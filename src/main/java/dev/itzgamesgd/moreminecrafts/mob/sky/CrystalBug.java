package dev.itzgamesgd.moreminecrafts.mob.sky;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.level.Level;

/**
 * 수정 벌레 (Q10-Q14, Q25, D2, D3): Silverfish melee AI, 24 HP / 4 attack, 2.5x scale, arthropod.
 *
 * <p>D2 removes the wake-up-friends goal (which converts infested blocks into extra silverfish) and D3 removes the
 * merge-with-stone goal (which hides it in a block and creates an infested block). The group "alert others" retaliation
 * on the vanilla target selector is kept: it summons nothing.
 */
public final class CrystalBug extends Silverfish {
    public CrystalBug(EntityType<? extends Silverfish> type, Level level) {
        super(type, level);
        refreshDimensions(); // apply the SCALE attribute to the collision box from the first tick (Q25)
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        EntitySpawnReason reason, SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data);
        SkyMobTraits.applyFixedLoadout(this, ItemStack.EMPTY); // Q55
        return result;
    }

    @Override
    public boolean wantsToPickUp(ServerLevel level, ItemStack stack) {
        return false;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.removeAllGoals(goal -> {
            String name = goal.getClass().getName();
            return name.endsWith("SilverfishWakeUpFriendsGoal") || name.endsWith("SilverfishMergeWithStoneGoal");
        });
        // D3 removes only hiding in stone. The vanilla merge goal is also the Silverfish's only idle stroll
        // (RandomStrollGoal, speed 1.0, interval 10, priority 5); keep that ordinary wandering (Q84).
        goalSelector.addGoal(5, new RandomStrollGoal(this, 1.0, 10));
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
