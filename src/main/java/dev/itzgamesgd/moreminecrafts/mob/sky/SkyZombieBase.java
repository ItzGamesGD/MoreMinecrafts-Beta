package dev.itzgamesgd.moreminecrafts.mob.sky;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/** Zombie-line behaviour shared by Cloud Wraith and Fallen Sentinel. Everything unlisted stays vanilla Zombie. */
abstract class SkyZombieBase extends Zombie {
    protected SkyZombieBase(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
    }

    /** D1: no baby variant, including NBT-loaded and command-set values. */
    @Override
    public void setBaby(boolean baby) {
        super.setBaby(false);
    }

    /** D1: hand vanilla an adult, jockey-free group so no baby (and no chicken jockey) path runs. Q55: fixed loadout. */
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        EntitySpawnReason reason, SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, new ZombieGroupData(false, false));
        super.setBaby(false);
        SkyMobTraits.applyFixedLoadout(this, ItemStack.EMPTY);
        return result;
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        // Q55: no random equipment.
    }

    @Override
    public boolean wantsToPickUp(ServerLevel level, ItemStack stack) {
        return false;
    }

    /** Q36: no sunlight burning (the type is deliberately absent from #minecraft:burn_in_daylight). */
    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        return SkyMobTraits.isImmuneDamage(source) || super.isInvulnerableTo(level, source);
    }

    @Override
    public boolean canFreeze() {
        return false;
    }
}
