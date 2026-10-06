package dev.itzgamesgd.moreminecrafts.mob;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class FeralWolf extends Wolf {
    public FeralWolf(EntityType<? extends Wolf> type, Level level) { super(type, level); }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.15, true));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }
}
