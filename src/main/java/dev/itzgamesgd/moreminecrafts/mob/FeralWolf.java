package dev.itzgamesgd.moreminecrafts.mob;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NonTameRandomTargetGoal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class FeralWolf extends Wolf {
    public FeralWolf(EntityType<? extends Wolf> type, Level level) { super(type, level); }

    @Override
    public void setTame(boolean isTame, boolean includeSideEffects) {
        super.setTame(isTame, includeSideEffects);
        if (isTame) {
            setTarget(null);
            getNavigation().stop();
        }
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.15, true));
        targetSelector.addGoal(2, new NonTameRandomTargetGoal<>(this, Player.class, true, null));
    }
}
