package dev.itzgamesgd.moreminecrafts.mob.sky;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
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
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.removeAllGoals(goal -> {
            String name = goal.getClass().getName();
            return name.endsWith("SilverfishWakeUpFriendsGoal") || name.endsWith("SilverfishMergeWithStoneGoal");
        });
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
