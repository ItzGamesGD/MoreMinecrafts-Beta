package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.skeleton.SkeletonKingRules;
import dev.itzgamesgd.moreminecrafts.skeleton.SkeletonKingRules.Ability;
import dev.itzgamesgd.moreminecrafts.skeleton.SkeletonKingState;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Skeleton.class)
abstract class SkeletonMixin implements SkeletonKingState {
    @Unique private static final Component KING_NAME = Component.literal("Skeleton King");
    @Unique private boolean moreminecrafts$king;
    @Unique private UUID moreminecrafts$guardOwner;
    @Unique private ServerBossEvent moreminecrafts$bossBar;
    @Unique private long moreminecrafts$barrageReady;
    @Unique private long moreminecrafts$guardsReady;
    @Unique private long moreminecrafts$curseReady;
    @Unique private int moreminecrafts$scheduleCursor;
    @Unique private Ability moreminecrafts$activeAbility;
    @Unique private int moreminecrafts$telegraphTicks;
    @Unique private ServerPlayer moreminecrafts$abilityTarget;
    @Unique private final List<Arrow> moreminecrafts$activeArrows = new ArrayList<>();
    @Unique private boolean moreminecrafts$cleaned;

    @Override
    public boolean moreminecrafts$isSkeletonKing() {
        return moreminecrafts$king;
    }

    @Override
    public void moreminecrafts$makeSkeletonKing() {
        if (moreminecrafts$king) return;
        moreminecrafts$king = true;
        Skeleton skeleton = (Skeleton) (Object) this;
        skeleton.setCustomName(KING_NAME);
        skeleton.setCustomNameVisible(true);
        skeleton.setPersistenceRequired();
        skeleton.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
        skeleton.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        skeleton.setDropChance(EquipmentSlot.HEAD, 0.0F);
        skeleton.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        moreminecrafts$setAttribute(skeleton, Attributes.MAX_HEALTH, 200.0D);
        moreminecrafts$setAttribute(skeleton, Attributes.ARMOR, 8.0D);
        moreminecrafts$setAttribute(skeleton, Attributes.MOVEMENT_SPEED, 0.22D);
        moreminecrafts$setAttribute(skeleton, Attributes.SCALE, 1.25D);
        skeleton.setHealth(skeleton.getMaxHealth());
        moreminecrafts$resetEncounterClock();
    }

    @Override
    public UUID moreminecrafts$getGuardOwner() {
        return moreminecrafts$guardOwner;
    }

    @Override
    public void moreminecrafts$setGuardOwner(UUID owner) {
        moreminecrafts$guardOwner = owner;
        Skeleton skeleton = (Skeleton) (Object) this;
        skeleton.setCustomName(Component.literal("Skeleton Guard"));
        skeleton.setCustomNameVisible(true);
        skeleton.setPersistenceRequired();
        skeleton.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        skeleton.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    @Override
    public void moreminecrafts$onKingUnload(ServerLevel level, boolean destroyed) {
        moreminecrafts$cleanup(level, destroyed);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void moreminecrafts$saveIdentity(ValueOutput output, CallbackInfo ci) {
        if (moreminecrafts$king) output.putBoolean(SkeletonKingRules.IDENTITY_SAVE_KEY, true);
        if (moreminecrafts$guardOwner != null) {
            output.putString(SkeletonKingRules.GUARD_OWNER_SAVE_KEY, moreminecrafts$guardOwner.toString());
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void moreminecrafts$loadIdentity(ValueInput input, CallbackInfo ci) {
        if (input.getBooleanOr(SkeletonKingRules.IDENTITY_SAVE_KEY, false)) moreminecrafts$makeSkeletonKing();
        input.getString(SkeletonKingRules.GUARD_OWNER_SAVE_KEY).ifPresent(value -> {
            try {
                moreminecrafts$setGuardOwner(UUID.fromString(value));
            } catch (IllegalArgumentException ignored) {
                moreminecrafts$guardOwner = null;
            }
        });
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void moreminecrafts$tickKing(CallbackInfo ci) {
        Skeleton skeleton = (Skeleton) (Object) this;
        if (!(skeleton.level() instanceof ServerLevel level)) return;
        if (!moreminecrafts$king) {
            if (moreminecrafts$guardOwner != null && skeleton.tickCount % 40 == 0
                    && level.getEntityInAnyDimension(moreminecrafts$guardOwner) == null) skeleton.discard();
            return;
        }
        if (!skeleton.isAlive() || skeleton.isRemoved()) {
            moreminecrafts$cleanup(level, true);
            return;
        }
        moreminecrafts$updateBossBar(level, skeleton);
        moreminecrafts$activeArrows.removeIf(Entity::isRemoved);

        if (moreminecrafts$activeAbility != null) {
            if (!moreminecrafts$validTarget(skeleton, moreminecrafts$abilityTarget)) {
                moreminecrafts$cancelTelegraph();
                return;
            }
            skeleton.getNavigation().stop();
            moreminecrafts$telegraph(level, skeleton);
            if (--moreminecrafts$telegraphTicks <= 0) moreminecrafts$resolveAbility(level, skeleton);
            return;
        }

        ServerPlayer target = moreminecrafts$playerTarget(skeleton);
        long tick = level.getGameTime();
        int guards = moreminecrafts$countLivingGuards(level, skeleton.getUUID());
        boolean curseCandidate = moreminecrafts$hasCursePlayer(level, skeleton);
        Ability selected = SkeletonKingRules.select(moreminecrafts$scheduleCursor, tick,
                moreminecrafts$barrageReady, moreminecrafts$guardsReady, moreminecrafts$curseReady,
                target != null, guards, curseCandidate);
        if (selected != null) moreminecrafts$beginAbility(level, skeleton, target, selected);
    }

    @Unique
    private void moreminecrafts$beginAbility(ServerLevel level, Skeleton skeleton, ServerPlayer target, Ability ability) {
        moreminecrafts$activeAbility = ability;
        moreminecrafts$abilityTarget = target;
        moreminecrafts$telegraphTicks = ability == Ability.BARRAGE ? 20 : 16;
        moreminecrafts$scheduleCursor = (ability.ordinal() + 1) % Ability.values().length;
        long tick = level.getGameTime();
        switch (ability) {
            case BARRAGE -> moreminecrafts$barrageReady = SkeletonKingRules.nextReady(ability, tick);
            case GUARDS -> moreminecrafts$guardsReady = SkeletonKingRules.nextReady(ability, tick);
            case CURSE -> moreminecrafts$curseReady = SkeletonKingRules.nextReady(ability, tick);
        }
        level.playSound(null, skeleton.getX(), skeleton.getY(), skeleton.getZ(), SoundEvents.SKELETON_AMBIENT,
                SoundSource.HOSTILE, 1.0F, ability == Ability.BARRAGE ? 0.65F : 0.85F);
    }

    @Unique
    private void moreminecrafts$telegraph(ServerLevel level, Skeleton skeleton) {
        if (moreminecrafts$telegraphTicks % 4 != 0) return;
        var particle = moreminecrafts$activeAbility == Ability.CURSE
                ? ParticleTypes.WITCH : moreminecrafts$activeAbility == Ability.GUARDS
                ? ParticleTypes.SOUL : ParticleTypes.CRIT;
        level.sendParticles(particle, skeleton.getX(), skeleton.getY() + 1.4D, skeleton.getZ(),
                5, 0.5D, 0.5D, 0.5D, 0.03D);
    }

    @Unique
    private void moreminecrafts$resolveAbility(ServerLevel level, Skeleton skeleton) {
        switch (moreminecrafts$activeAbility) {
            case BARRAGE -> moreminecrafts$fireBarrage(level, skeleton, moreminecrafts$abilityTarget);
            case GUARDS -> moreminecrafts$summonGuards(level, skeleton, moreminecrafts$abilityTarget);
            case CURSE -> moreminecrafts$cursePlayers(level, skeleton);
        }
        moreminecrafts$cancelTelegraph();
    }

    @Unique
    private void moreminecrafts$fireBarrage(ServerLevel level, Skeleton skeleton, ServerPlayer target) {
        Vec3 origin = skeleton.getEyePosition().add(0.0D, -0.15D, 0.0D);
        Vec3 aim = target.getEyePosition().subtract(origin).normalize();
        for (int i = 0; i < SkeletonKingRules.BARRAGE_ARROWS; i++) {
            double angle = Math.toRadians(SkeletonKingRules.barrageYawOffset(i));
            double x = aim.x * Math.cos(angle) - aim.z * Math.sin(angle);
            double z = aim.x * Math.sin(angle) + aim.z * Math.cos(angle);
            Arrow arrow = new Arrow(level, skeleton, new ItemStack(Items.ARROW), new ItemStack(Items.BOW));
            arrow.setPos(origin);
            arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
            arrow.shoot(x, aim.y, z, 1.6F, 0.0F);
            level.addFreshEntity(arrow);
            moreminecrafts$activeArrows.add(arrow);
        }
        level.playSound(null, skeleton.getX(), skeleton.getY(), skeleton.getZ(), SoundEvents.SKELETON_SHOOT,
                SoundSource.HOSTILE, 1.0F, 0.75F);
    }

    @Unique
    private void moreminecrafts$summonGuards(ServerLevel level, Skeleton king, ServerPlayer target) {
        int capacity = SkeletonKingRules.MAX_LIVING_GUARDS - moreminecrafts$countLivingGuards(level, king.getUUID());
        for (int i = 0; i < Math.min(SkeletonKingRules.GUARDS_PER_CAST, capacity); i++) {
            Skeleton guard = EntityTypes.SKELETON.create(level, EntitySpawnReason.MOB_SUMMONED);
            if (guard == null) continue;
            double angle = i * Math.PI * 2.0D / SkeletonKingRules.GUARDS_PER_CAST + king.getRandom().nextDouble() * 0.35D;
            guard.setPos(king.getX() + Math.cos(angle) * 4.0D, king.getY(), king.getZ() + Math.sin(angle) * 4.0D);
            ((SkeletonKingState) guard).moreminecrafts$setGuardOwner(king.getUUID());
            guard.setTarget(target);
            level.addFreshEntity(guard);
            level.sendParticles(ParticleTypes.SOUL, guard.getX(), guard.getY() + 1.0D, guard.getZ(),
                    8, 0.3D, 0.6D, 0.3D, 0.03D);
        }
    }

    @Unique
    private void moreminecrafts$cursePlayers(ServerLevel level, Skeleton skeleton) {
        for (ServerPlayer player : moreminecrafts$nearbyPlayers(level, skeleton, SkeletonKingRules.CURSE_RANGE)) {
            if (SkeletonKingRules.eligiblePlayer(skeleton.distanceTo(player), player.isAlive(), player.isSpectator())) {
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0), skeleton);
                level.sendParticles(ParticleTypes.WITCH, player.getX(), player.getY() + 1.0D, player.getZ(),
                        12, 0.4D, 0.6D, 0.4D, 0.02D);
            }
        }
        level.playSound(null, skeleton.getX(), skeleton.getY(), skeleton.getZ(), SoundEvents.ILLUSIONER_CAST_SPELL,
                SoundSource.HOSTILE, 1.0F, 0.8F);
    }

    @Unique
    private void moreminecrafts$updateBossBar(ServerLevel level, Skeleton skeleton) {
        if (moreminecrafts$bossBar == null) moreminecrafts$bossBar = new ServerBossEvent(
                skeleton.getUUID(), KING_NAME, BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS);
        moreminecrafts$bossBar.setProgress(Math.max(0.0F, skeleton.getHealth() / skeleton.getMaxHealth()));
        List<ServerPlayer> nearby = moreminecrafts$nearbyPlayers(level, skeleton, SkeletonKingRules.PARTICIPATION_RANGE);
        for (ServerPlayer viewer : List.copyOf(moreminecrafts$bossBar.getPlayers())) {
            if (!nearby.contains(viewer) || !viewer.isAlive() || viewer.isSpectator()) moreminecrafts$bossBar.removePlayer(viewer);
        }
        for (ServerPlayer player : nearby) {
            if (player.isAlive() && !player.isSpectator()) moreminecrafts$bossBar.addPlayer(player);
        }
        moreminecrafts$bossBar.setVisible(!moreminecrafts$bossBar.getPlayers().isEmpty());
    }

    @Unique
    private List<ServerPlayer> moreminecrafts$nearbyPlayers(ServerLevel level, Skeleton skeleton, double range) {
        AABB area = skeleton.getBoundingBox().inflate(range);
        return level.getEntitiesOfClass(ServerPlayer.class, area, player -> player.distanceToSqr(skeleton) <= range * range);
    }

    @Unique
    private boolean moreminecrafts$hasCursePlayer(ServerLevel level, Skeleton skeleton) {
        return moreminecrafts$nearbyPlayers(level, skeleton, SkeletonKingRules.CURSE_RANGE).stream()
                .anyMatch(player -> SkeletonKingRules.eligiblePlayer(skeleton.distanceTo(player), player.isAlive(), player.isSpectator()));
    }

    @Unique
    private ServerPlayer moreminecrafts$playerTarget(Skeleton skeleton) {
        return skeleton.getTarget() instanceof ServerPlayer player && moreminecrafts$validTarget(skeleton, player)
                ? player : null;
    }

    @Unique
    private boolean moreminecrafts$validTarget(Skeleton skeleton, ServerPlayer player) {
        return player != null && player.isAlive() && !player.isRemoved() && !player.isSpectator()
                && player.level() == skeleton.level()
                && skeleton.distanceToSqr(player) <= SkeletonKingRules.PARTICIPATION_RANGE * SkeletonKingRules.PARTICIPATION_RANGE;
    }

    @Unique
    private int moreminecrafts$countLivingGuards(ServerLevel level, UUID owner) {
        int count = 0;
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof Skeleton guard && guard.isAlive()
                    && owner.equals(((SkeletonKingState) guard).moreminecrafts$getGuardOwner())) count++;
        }
        return count;
    }

    @Unique
    private void moreminecrafts$cleanup(ServerLevel level, boolean removeGuards) {
        if (moreminecrafts$bossBar != null) {
            moreminecrafts$bossBar.removeAllPlayers();
            moreminecrafts$bossBar.setVisible(false);
        }
        moreminecrafts$cancelTelegraph();
        moreminecrafts$activeArrows.forEach(Entity::discard);
        moreminecrafts$activeArrows.clear();
        if (removeGuards && !moreminecrafts$cleaned) {
            moreminecrafts$cleaned = true;
            UUID owner = ((Skeleton) (Object) this).getUUID();
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof Skeleton guard
                        && owner.equals(((SkeletonKingState) guard).moreminecrafts$getGuardOwner())) guard.discard();
            }
        }
    }

    @Unique
    private void moreminecrafts$cancelTelegraph() {
        moreminecrafts$activeAbility = null;
        moreminecrafts$telegraphTicks = 0;
        moreminecrafts$abilityTarget = null;
    }

    @Unique
    private void moreminecrafts$resetEncounterClock() {
        Skeleton skeleton = (Skeleton) (Object) this;
        long tick = skeleton.level().getGameTime();
        moreminecrafts$barrageReady = tick + SkeletonKingRules.BARRAGE_COOLDOWN;
        moreminecrafts$guardsReady = tick + SkeletonKingRules.GUARD_COOLDOWN;
        moreminecrafts$curseReady = tick + SkeletonKingRules.CURSE_COOLDOWN;
        moreminecrafts$cancelTelegraph();
        moreminecrafts$cleaned = false;
    }

    @Unique
    private static void moreminecrafts$setAttribute(Skeleton skeleton,
            net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double value) {
        var instance = skeleton.getAttribute(attribute);
        if (instance != null) instance.setBaseValue(value);
    }
}
