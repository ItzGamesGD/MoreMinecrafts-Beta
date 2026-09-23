package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.zombie.BombZombieEffects;
import dev.itzgamesgd.moreminecrafts.zombie.ZombieVariant;
import dev.itzgamesgd.moreminecrafts.zombie.ZombieVariantRules;
import dev.itzgamesgd.moreminecrafts.zombie.ZombieVariantState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Zombie.class)
abstract class ZombieMixin implements ZombieVariantState {
    @Unique private static final String VARIANT_TAG = "MoreMinecraftsZombieVariant";
    @Unique private ZombieVariant moreminecrafts$variant = ZombieVariant.NORMAL;
    @Unique private boolean moreminecrafts$classified;
    @Unique private long moreminecrafts$leapReadyTick;
    @Unique private int moreminecrafts$warningTicks;
    @Unique private boolean moreminecrafts$leaping;
    @Unique private boolean moreminecrafts$leftGround;
    @Unique private long moreminecrafts$fallImmuneUntil;
    @Unique private int moreminecrafts$recoveryTicks;
    @Unique private boolean moreminecrafts$bombScheduled;

    @Override
    public ZombieVariant moreminecrafts$getVariant() {
        return moreminecrafts$variant;
    }

    @Override
    public void moreminecrafts$setVariant(ZombieVariant variant) {
        moreminecrafts$variant = variant;
        moreminecrafts$classified = true;
        if (variant == ZombieVariant.BOMB) {
            Zombie zombie = (Zombie) (Object) this;
            zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.TNT));
            zombie.setDropChance(EquipmentSlot.HEAD, 0.0F);
        }
    }

    @Override
    public boolean moreminecrafts$hasLeapFallImmunity(long gameTick) {
        return moreminecrafts$variant == ZombieVariant.LEAP && moreminecrafts$leaping
                && gameTick <= moreminecrafts$fallImmuneUntil;
    }

    @Inject(method = "finalizeSpawn", at = @At("RETURN"))
    private void moreminecrafts$selectNaturalVariant(ServerLevelAccessor level, DifficultyInstance difficulty,
                                                      EntitySpawnReason reason, SpawnGroupData groupData,
                                                      CallbackInfoReturnable<SpawnGroupData> cir) {
        Zombie zombie = (Zombie) (Object) this;
        if (zombie.getType() != EntityTypes.ZOMBIE) {
            return;
        }
        ZombieVariant existing = moreminecrafts$classified ? moreminecrafts$variant : null;
        ZombieVariant selected = ZombieVariantRules.select(reason == EntitySpawnReason.NATURAL,
                existing, zombie.getRandom().nextDouble(), zombie.getRandom().nextDouble());
        moreminecrafts$setVariant(selected);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void moreminecrafts$saveVariant(ValueOutput output, CallbackInfo ci) {
        if (moreminecrafts$classified) {
            output.putString(VARIANT_TAG, moreminecrafts$variant.name());
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void moreminecrafts$loadVariant(ValueInput input, CallbackInfo ci) {
        input.getString(VARIANT_TAG).ifPresent(name -> moreminecrafts$setVariant(ZombieVariant.fromSavedName(name)));
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void moreminecrafts$tickLeap(CallbackInfo ci) {
        Zombie zombie = (Zombie) (Object) this;
        if (!(zombie.level() instanceof ServerLevel level)) {
            return;
        }
        if (moreminecrafts$variant == ZombieVariant.BOMB && zombie.deathTime == 1 && !moreminecrafts$bombScheduled) {
            moreminecrafts$bombScheduled = true;
            BombZombieEffects.schedule(level, zombie.position());
        }
        if (moreminecrafts$variant != ZombieVariant.LEAP) {
            return;
        }
        if (!zombie.isAlive() || zombie.isRemoved()) {
            moreminecrafts$clearLeap();
            return;
        }
        if (moreminecrafts$recoveryTicks > 0) {
            moreminecrafts$recoveryTicks--;
            return;
        }
        if (moreminecrafts$leaping) {
            if (level.getGameTime() > moreminecrafts$fallImmuneUntil
                    || zombie.isInWater() || zombie.isInLava() || zombie.isPassenger()) {
                moreminecrafts$clearLeap();
                moreminecrafts$recoveryTicks = 10;
            } else if (!zombie.onGround()) {
                moreminecrafts$leftGround = true;
            } else if (moreminecrafts$leftGround) {
                moreminecrafts$land(level, zombie);
            }
            return;
        }
        if (moreminecrafts$warningTicks > 0) {
            moreminecrafts$warningTicks--;
            if (moreminecrafts$warningTicks == 0) {
                moreminecrafts$launchIfValid(level, zombie);
            } else if (moreminecrafts$warningTicks % 2 == 0) {
                level.sendParticles(ParticleTypes.SMOKE, zombie.getX(), zombie.getY() + 1.0D, zombie.getZ(),
                        2, 0.25D, 0.25D, 0.25D, 0.01D);
            }
            return;
        }
        ServerPlayer target = moreminecrafts$playerTarget(zombie);
        if (target != null && moreminecrafts$validLaunch(zombie, target, level.getGameTime())) {
            moreminecrafts$warningTicks = 10;
            level.playSound(null, zombie.getX(), zombie.getY(), zombie.getZ(), SoundEvents.ZOMBIE_AMBIENT,
                    SoundSource.HOSTILE, 0.8F, 1.25F);
        }
    }

    @Unique
    private void moreminecrafts$launchIfValid(ServerLevel level, Zombie zombie) {
        ServerPlayer target = moreminecrafts$playerTarget(zombie);
        if (target == null || !moreminecrafts$validLaunch(zombie, target, level.getGameTime())) {
            return;
        }
        Vec3 horizontal = target.position().subtract(zombie.position()).multiply(1.0D, 0.0D, 1.0D);
        if (horizontal.lengthSqr() < 1.0E-6D) {
            return;
        }
        Vec3 velocity = horizontal.normalize().scale(1.1D);
        zombie.setDeltaMovement(velocity.x, 0.65D, velocity.z);
        moreminecrafts$leaping = true;
        moreminecrafts$leftGround = false;
        moreminecrafts$fallImmuneUntil = level.getGameTime() + 40;
        moreminecrafts$leapReadyTick = ZombieVariantRules.nextLeapTick(level.getGameTime());
    }

    @Unique
    private boolean moreminecrafts$validLaunch(Zombie zombie, ServerPlayer target, long tick) {
        double distance = zombie.distanceTo(target);
        return ZombieVariantRules.canTriggerLeap(distance, tick, moreminecrafts$leapReadyTick)
                && zombie.onGround() && !zombie.isInWater() && !zombie.isInLava()
                && !zombie.onClimbable() && !zombie.isPassenger()
                && zombie.level().noCollision(zombie, zombie.getBoundingBox().move(0.0D, 1.0D, 0.0D));
    }

    @Unique
    private static ServerPlayer moreminecrafts$playerTarget(Zombie zombie) {
        return zombie.getTarget() instanceof ServerPlayer player && player.isAlive() ? player : null;
    }

    @Unique
    private void moreminecrafts$land(ServerLevel level, Zombie zombie) {
        Vec3 center = zombie.position();
        level.sendParticles(ParticleTypes.POOF, center.x, center.y + 0.1D, center.z,
                15, 0.7D, 0.1D, 0.7D, 0.05D);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.HOSTILE, 0.45F, 1.5F);
        AABB area = zombie.getBoundingBox().inflate(1.8D);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, area, ServerPlayer::isAlive)) {
            if (player.position().distanceTo(center) <= 1.8D) {
                player.hurtServer(level, level.damageSources().mobAttack(zombie), ZombieVariantRules.LEAP_DAMAGE);
                BombZombieEffects.knockAway(player, center, 0.4D);
            }
        }
        moreminecrafts$leaping = false;
        moreminecrafts$leftGround = false;
        moreminecrafts$fallImmuneUntil = 0;
        moreminecrafts$recoveryTicks = 10;
    }

    @Unique
    private void moreminecrafts$clearLeap() {
        moreminecrafts$warningTicks = 0;
        moreminecrafts$leaping = false;
        moreminecrafts$leftGround = false;
        moreminecrafts$fallImmuneUntil = 0;
    }
}
