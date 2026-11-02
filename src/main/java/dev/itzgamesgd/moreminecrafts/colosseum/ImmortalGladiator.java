package dev.itzgamesgd.moreminecrafts.colosseum;

import java.util.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;

/** Dedicated R7 boss. Attack identities/HP/basic damage are approved; exact cadence, reach and special damage are provisional tuning. */
public final class ImmortalGladiator extends Monster {
    private static final EntityDataAccessor<Integer> POSE=SynchedEntityData.defineId(ImmortalGladiator.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> SHIELD_ACTIVE=SynchedEntityData.defineId(ImmortalGladiator.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> PHASE_TWO=SynchedEntityData.defineId(ImmortalGladiator.class,EntityDataSerializers.BOOLEAN);
    private final ServerBossEvent bar=new ServerBossEvent(UUID.randomUUID(),Component.translatable("entity.moreminecrafts.immortal_gladiator"),BossEvent.BossBarColor.RED,BossEvent.BossBarOverlay.PROGRESS);
    private GladiatorCombat combat=new GladiatorCombat();
    private UUID attemptId;
    private String arenaId="";
    private int awakening=40, pursuitSample;
    private double lastDistance=Double.MAX_VALUE;
    private final Set<UUID> chargeHits=new HashSet<>();

    public ImmortalGladiator(EntityType<? extends ImmortalGladiator> type,Level level) { super(type,level); setPersistenceRequired(); }
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,300).add(Attributes.ATTACK_DAMAGE,10)
                .add(Attributes.MOVEMENT_SPEED,.28).add(Attributes.FOLLOW_RANGE,64).add(Attributes.KNOCKBACK_RESISTANCE,.6).add(Attributes.STEP_HEIGHT,1);
    }
    public static EntityType<ImmortalGladiator> entityType() { return GladiatorEntities.TYPE; }
    @Override protected void defineSynchedData(SynchedEntityData.Builder b) { super.defineSynchedData(b); b.define(POSE,0); b.define(PHASE_TWO,false); b.define(SHIELD_ACTIVE,true); }
    @Override protected void registerGoals() { } // All targeting/cadence is arena-authoritative, no unrelated target goals.
    public int attackPose() { return entityData.get(POSE); }
    public boolean phaseTwo() { return entityData.get(PHASE_TWO); }
    public boolean shieldActive() {return entityData.get(SHIELD_ACTIVE);}
    UUID attemptId() { return attemptId; }
    String combatSnapshot() {return ArenaController.GSON.toJson(combat)+":"+awakening+":"+ArenaController.GSON.toJson(chargeHits.stream().map(UUID::toString).sorted().toList());}
    void bind(String arena,UUID attempt,int maxHp) {
        arenaId=arena;attemptId=attempt;
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(maxHp);setHealth(maxHp);
        setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_SWORD));setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.SHIELD));
        setCustomName(Component.translatable("entity.moreminecrafts.immortal_gladiator"));
    }
    private ArenaController controller() {
        if(!(level() instanceof ServerLevel l) || attemptId==null) return null;
        ArenaController c=ColosseumRuntime.data(l.getServer()).controller(arenaId);
        return c!=null && c.attempt()!=null && attemptId.equals(c.attempt().attemptId) ? c:null;
    }
    @Override public void tick() {
        if(level().isClientSide()) {super.tick();return;}
        ServerLevel l=(ServerLevel)level(); ArenaController c=controller();
        if(c==null) {bar.removeAllPlayers();setDeltaMovement(Vec3.ZERO);return;}
        if(c.attempt().ended() && !c.quarantined()) {bar.removeAllPlayers();discard();return;}
        if(ColosseumRuntime.combatFrozen(this)) {bar.removeAllPlayers();setDeltaMovement(Vec3.ZERO);return;}
        super.tick(); if(!isAlive()) {bar.removeAllPlayers();return;}
        var site=ColosseumRuntime.data(l.getServer()).sites().get(arenaId);
        for(ServerPlayer p:l.players()) {
            boolean valid=valid(p,c,site); if(valid)bar.addPlayer(p);else bar.removePlayer(p);
        }
        for(ServerPlayer p:new ArrayList<>(bar.getPlayers())) if(!l.players().contains(p))bar.removePlayer(p);
        bar.setProgress(getHealth()/getMaxHealth());
        if(awakening>0) {awakening--; getNavigation().stop(); particles(l,ParticleTypes.ENCHANT,16);return;}
        combat.tick();
        if(combat.updatePhase(getHealth(),getMaxHealth())) {
            entityData.set(PHASE_TWO,true);setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);
            l.playSound(null,blockPosition(),SoundEvents.RAVAGER_ROAR,SoundSource.HOSTILE,1,.8F);
            particles(l,ParticleTypes.CRIT,24);
        }
        entityData.set(SHIELD_ACTIVE,combat.canBlock());
        entityData.set(POSE,combat.attack.ordinal());
        ServerPlayer target=l.players().stream().filter(p->valid(p,c,site)).min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        setTarget(target); if(target==null || combat.recovery>0) {getNavigation().stop();return;}
        getLookControl().setLookAt(target,30,30);
        if(combat.attack!=GladiatorCombat.Attack.NONE) {attackTick(l,c,target);return;}
        double dist=distanceToSqr(target);
        if(++pursuitSample>=20) {
            if(dist>16 && (getNavigation().isDone() || dist>=lastDistance-.1))combat.failedPursuit();
            lastDistance=dist;pursuitSample=0;
        }
        if(combat.obstacleReady()) { begin(target,GladiatorCombat.Attack.OBSTACLE);combat.beginObstacleCharge();return; }
        if(dist<=16) {
            GladiatorCombat.Attack type=switch(combat.attackIndex++%4) {
                case 1->GladiatorCombat.Attack.SWEEP; case 2->GladiatorCombat.Attack.LUNGE; default->GladiatorCombat.Attack.COMBO;
            }; begin(target,type);
        } else if(dist<400 && combat.attackIndex%4==3 && getSensing().hasLineOfSight(target)) {combat.attackIndex++;begin(target,GladiatorCombat.Attack.CHARGE);}
        else getNavigation().moveTo(target,1.0);
    }
    private boolean valid(ServerPlayer p,ArenaController c,ColosseumData.Site site) {
        var a=c.attempt();var m=a.member(p.getUUID());
        return m!=null && !m.dead && p.isAlive() && !p.isSpectator() && !p.isCreative() && site.geometry().contains(p.blockPosition().getX(),p.blockPosition().getY(),p.blockPosition().getZ());
    }
    private void begin(ServerPlayer target,GladiatorCombat.Attack type) {
        combat.attack=type;combat.windup=type==GladiatorCombat.Attack.OBSTACLE?50:type==GladiatorCombat.Attack.CHARGE?30:combat.phaseTwo?10:16;
        combat.attackTicks=0;combat.comboLeft=type==GladiatorCombat.Attack.COMBO?(combat.phaseTwo?4:2):type==GladiatorCombat.Attack.COUNTER?2:1;
        combat.targetX=target.getX();combat.targetY=target.getY();combat.targetZ=target.getZ();
        Vec3 dir=target.position().subtract(position()).normalize();combat.dirX=dir.x;combat.dirZ=dir.z;chargeHits.clear();getNavigation().stop();
    }
    private void attackTick(ServerLevel l,ArenaController c,ServerPlayer target) {
        if(combat.windup>0) {
            combat.windup--;getNavigation().stop();
            if(combat.windup%4==0) {
                particles(l,ParticleTypes.CRIT,3);
                for(int i=1;i<=8;i++) l.sendParticles(ParticleTypes.END_ROD,getX()+combat.dirX*i,getY()+.15,getZ()+combat.dirZ*i,1,0,0,0,0);
            }
            return;
        }
        var attack=combat.attack;
        boolean charge=attack==GladiatorCombat.Attack.CHARGE || attack==GladiatorCombat.Attack.OBSTACLE;
        if(charge) {
            setDeltaMovement(combat.dirX*.7,getDeltaMovement().y,combat.dirZ*.7);
            if(attack==GladiatorCombat.Attack.OBSTACLE) ArenaTerrain.breakObstacles(l,c,getBoundingBox().inflate(.3));
            for(ServerPlayer p:l.players()) if(valid(p,c,ColosseumRuntime.data(l.getServer()).sites().get(arenaId)) && p.getBoundingBox().intersects(getBoundingBox().inflate(.8)) && chargeHits.add(p.getUUID())) strike(l,p,true);
            if(++combat.attackTicks<16 && !horizontalCollision)return;
        } else {
            if(attack==GladiatorCombat.Attack.LUNGE)setDeltaMovement(combat.dirX*.45,getDeltaMovement().y,combat.dirZ*.45);
            swing(InteractionHand.MAIN_HAND,net.minecraft.world.item.component.SwingAnimation.DEFAULT);
            if(attack==GladiatorCombat.Attack.COUNTER && combat.comboLeft==2) {
                if(distanceToSqr(target)<=16)target.knockback(.6,getX()-target.getX(),getZ()-target.getZ(),damageSources().mobAttack(this),0);
            } else if(attack==GladiatorCombat.Attack.SWEEP) {
                var site=ColosseumRuntime.data(l.getServer()).sites().get(arenaId);
                for(ServerPlayer p:l.players()) if(valid(p,c,site) && distanceToSqr(p)<=25 && getSensing().hasLineOfSight(p))strike(l,p,false);
            } else if(distanceToSqr(target)<=20 && getSensing().hasLineOfSight(target))strike(l,target,false);
            if(--combat.comboLeft>0) {combat.windup=combat.phaseTwo?8:14;return;}
        }
        combat.attack=GladiatorCombat.Attack.NONE;combat.recovery=combat.phaseTwo?14:26;setDeltaMovement(0,getDeltaMovement().y,0);
    }
    private void strike(ServerLevel l,ServerPlayer p,boolean heavy) {
        boolean blocking=p.isBlocking();
        // Special damage is deliberately the approved 10 baseline until live tuning; no invented bonus.
        if(p.hurtServer(l,damageSources().mobAttack(this),10)) {
            p.knockback(heavy?.8:.35,getX()-p.getX(),getZ()-p.getZ(),damageSources().mobAttack(this),10);
            combat.retaliated();
        }
        if(heavy && blocking) {p.getCooldowns().addCooldown(new ItemStack(Items.SHIELD),100);p.stopUsingItem();}
    }
    @Override public boolean hurtServer(ServerLevel l,DamageSource source,float amount) {
        if(ColosseumRuntime.combatFrozen(this))return false;
        if(source.getEntity() instanceof ServerPlayer p && combat.canBlock() && source.getSourcePosition()!=null) {
            Vec3 to=source.getSourcePosition().subtract(position()).normalize();
            if(to.dot(getLookAngle())>.25 && combat.block(p.getMainHandItem().is(ItemTags.AXES))) {
                l.playSound(null,blockPosition(),SoundEvents.SHIELD_BLOCK.value(),SoundSource.HOSTILE,1,1);
                entityData.set(SHIELD_ACTIVE,combat.canBlock());
                if(combat.canBlock() && combat.attack==GladiatorCombat.Attack.NONE)begin(p,GladiatorCombat.Attack.COUNTER);
                return false;
            }
        }
        boolean hurt=super.hurtServer(l,source,amount);
        if(hurt && source.getEntity() instanceof ServerPlayer)combat.attackedWithoutRetaliation();
        return hurt;
    }
    private void particles(ServerLevel l,net.minecraft.core.particles.SimpleParticleType type,int count) {l.sendParticles(type,getX(),getY()+2,getZ(),count,.6,1,.6,.02);}
    @Override public boolean shouldDropExperience() {return false;} // No separately approved direct XP reward.
    @Override protected void dropEquipment(ServerLevel l) { } // Sword/shield are encounter equipment, never collectible loot.
    @Override protected void dropCustomDeathLoot(ServerLevel l,DamageSource s,boolean hit) { }
    @Override public void remove(Entity.RemovalReason reason) {bar.removeAllPlayers();super.remove(reason);}
    @Override public boolean removeWhenFarAway(double distance) {return false;}
    @Override protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);out.putString("ST05Arena",arenaId);
        if(attemptId!=null)out.putString("ST05Attempt",attemptId.toString());
        out.putString("ST05ChargeHits",ArenaController.GSON.toJson(chargeHits.stream().map(UUID::toString).sorted().toList()));
        out.putString("ST05Combat",ArenaController.GSON.toJson(combat));out.putInt("ST05Awakening",awakening);
        out.putInt("ST05PursuitSample",pursuitSample);out.putDouble("ST05LastDistance",lastDistance);
    }
    @Override protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);arenaId=in.getStringOr("ST05Arena","");
        try {attemptId=UUID.fromString(in.getStringOr("ST05Attempt",""));combat=ArenaController.GSON.fromJson(in.getStringOr("ST05Combat","{}"),GladiatorCombat.class);}
        catch(RuntimeException e) {combat=new GladiatorCombat();attemptId=null;}
        if(combat==null || combat.attack==null) {combat=new GladiatorCombat();attemptId=null;}
        try {for(String hit:ArenaController.GSON.fromJson(in.getStringOr("ST05ChargeHits","[]"),String[].class))chargeHits.add(UUID.fromString(hit));}catch(RuntimeException e){attemptId=null;}
        awakening=in.getIntOr("ST05Awakening",40);pursuitSample=in.getIntOr("ST05PursuitSample",0);lastDistance=in.getDoubleOr("ST05LastDistance",Double.MAX_VALUE);
        entityData.set(PHASE_TWO,combat.phaseTwo);entityData.set(SHIELD_ACTIVE,combat.canBlock());
        entityData.set(POSE,combat.attack.ordinal());
    }
}
