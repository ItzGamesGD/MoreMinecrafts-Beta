package dev.itzgamesgd.moreminecrafts.spidercolony;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Persistent vanilla-spider-derived elite. Nest ID is optional for manually summoned instances. */
public final class MotherSpider extends Spider {
    // Juvenile tuning is PROVISIONAL: design specifies only smaller/weaker.
    static final double JUVENILE_SCALE_PROVISIONAL=0.5, JUVENILE_HEALTH_PROVISIONAL=8, JUVENILE_DAMAGE_PROVISIONAL=2;
    // Owner-specified base ranges; PROVISIONAL pending live balance validation.
    static final int STRING_MIN_PROVISIONAL=4, STRING_MAX_PROVISIONAL=8;
    static final int EYE_MIN_PROVISIONAL=1, EYE_MAX_PROVISIONAL=3;
    static final int FERMENTED_MIN_PROVISIONAL=1, FERMENTED_MAX_PROVISIONAL=2;
    static final int WEB_MIN_PROVISIONAL=4, WEB_MAX_PROVISIONAL=8;
    static final int LOOTING_MAX_BONUS_PER_LEVEL_PROVISIONAL=1;
    static final float VOICE_PITCH_FACTOR_PROVISIONAL=0.8F;
    private int cooldown=SummonPolicy.INTERVAL_TICKS;
    private String nest="";
    private net.minecraft.core.BlockPos home;
    public MotherSpider(EntityType<? extends Spider> type,Level level) {super(type,level);setPersistenceRequired();}
    public static AttributeSupplier.Builder attributes() {return Spider.createAttributes().add(Attributes.MAX_HEALTH,60).add(Attributes.ATTACK_DAMAGE,6);}
    @Override public float getVoicePitch() {return super.getVoicePitch()*VOICE_PITCH_FACTOR_PROVISIONAL;}
    @Override public void tick() {
        super.tick();
        if(!(level() instanceof ServerLevel level) || !isAlive()) return;
        ColonyLedger data=SpiderColonyEntities.data(level);
        if(home==null)home=blockPosition();
        if(!nest.isEmpty() && !data.bind(nest,getUUID())) {discard();return;}
        var target=getTarget();
        if(target==null || !target.isAlive()) {
            if(tickCount%40==0 && distanceToSqr(home.getX()+0.5,home.getY(),home.getZ()+0.5)>16)
                getNavigation().moveTo(home.getX()+0.5,home.getY(),home.getZ()+0.5,1.0);
            return;
        }
        if(--cooldown>0)return;
        cooldown=SummonPolicy.INTERVAL_TICKS;
        int count=SummonPolicy.allowed(true,data.childCount(getUUID()));
        for(int i=0;i<count;i++) spawnJuvenile(level,target,data);
    }
    private void spawnJuvenile(ServerLevel level,LivingEntity target,ColonyLedger data) {
        for(int attempt=0;attempt<8;attempt++) {
            Spider child=SpiderColonyEntities.JUVENILE.create(level,EntitySpawnReason.MOB_SUMMONED);
            if(child==null)return;
            double x=getX()+(random.nextDouble()-0.5)*6, z=getZ()+(random.nextDouble()-0.5)*6;
            child.setPos(x,getY(),z);
            child.getAttribute(Attributes.SCALE).setBaseValue(JUVENILE_SCALE_PROVISIONAL);
            child.getAttribute(Attributes.MAX_HEALTH).setBaseValue(JUVENILE_HEALTH_PROVISIONAL);
            child.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(JUVENILE_DAMAGE_PROVISIONAL);
            child.setHealth((float)JUVENILE_HEALTH_PROVISIONAL);
            var floor=child.blockPosition().below();
            if(!level.noCollision(child) || !level.getFluidState(child.blockPosition()).isEmpty()
                    || !level.getBlockState(floor).isFaceSturdy(level,floor,net.minecraft.core.Direction.UP))continue;
            child.setTarget(target); child.addTag("mm_spider_juvenile"); child.addTag("mm_spider_owner_"+getUUID());
            data.addChild(getUUID(),child.getUUID());
            if(!level.addFreshEntity(child))data.removeChild(child.getUUID());
            else level.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF,x,getY()+0.3,z,4,0.15,0.15,0.15,0.01);
            return;
        }
    }
    @Override protected int getBaseExperienceReward(ServerLevel level) { return 40; }
    @Override protected void dropCustomDeathLoot(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, boolean playerKill) {
        super.dropCustomDeathLoot(level,source,playerKill);
        int looting=0;
        if(source.getEntity() instanceof LivingEntity attacker) {
            var ench=level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING);
            looting=net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentLevel(ench,attacker);
        }
        dropRange(level,net.minecraft.world.item.Items.STRING,STRING_MIN_PROVISIONAL,STRING_MAX_PROVISIONAL,looting);
        dropRange(level,net.minecraft.world.item.Items.SPIDER_EYE,EYE_MIN_PROVISIONAL,EYE_MAX_PROVISIONAL,looting);
        dropRange(level,net.minecraft.world.item.Items.FERMENTED_SPIDER_EYE,FERMENTED_MIN_PROVISIONAL,FERMENTED_MAX_PROVISIONAL,looting);
        dropRange(level,net.minecraft.world.item.Items.COBWEB,WEB_MIN_PROVISIONAL,WEB_MAX_PROVISIONAL,looting);
        spawnAtLocation(level,dev.itzgamesgd.moreminecrafts.registry.ModItems.COLONY_CRYSTAL);
    }
    private void dropRange(ServerLevel level,net.minecraft.world.level.ItemLike item,int min,int max,int looting) {
        int amount=min+random.nextInt(max-min+1)+(looting>0?random.nextInt(looting*LOOTING_MAX_BONUS_PER_LEVEL_PROVISIONAL+1):0);
        spawnAtLocation(level,new net.minecraft.world.item.ItemStack(item,amount));
    }
    public boolean bindNest(ServerLevel level,String id) {
        if(id==null || id.isBlank() || (!nest.isEmpty() && !nest.equals(id)))return false;
        if(!SpiderColonyEntities.data(level).bind(id,getUUID()))return false;
        nest=id; if(home==null)home=blockPosition(); return true;
    }
    void recordDefeat(ServerLevel level) {if(!nest.isEmpty())SpiderColonyEntities.data(level).defeat(nest,getUUID());}
    @Override protected void addAdditionalSaveData(ValueOutput out) {super.addAdditionalSaveData(out);out.putInt("colony_cooldown",cooldown);out.putString("colony_nest",nest);out.storeNullable("colony_home",net.minecraft.core.BlockPos.CODEC,home);}
    @Override protected void readAdditionalSaveData(ValueInput in) {super.readAdditionalSaveData(in);cooldown=in.getIntOr("colony_cooldown",SummonPolicy.INTERVAL_TICKS);nest=in.getStringOr("colony_nest","");home=in.read("colony_home",net.minecraft.core.BlockPos.CODEC).orElse(null);}
}
