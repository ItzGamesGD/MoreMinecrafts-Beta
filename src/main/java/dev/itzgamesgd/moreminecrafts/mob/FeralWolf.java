package dev.itzgamesgd.moreminecrafts.mob;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NonTameRandomTargetGoal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class FeralWolf extends Wolf implements Enemy {
    private static final double HEALTH_MULTIPLIER = 1.15;

    public FeralWolf(EntityType<? extends Wolf> type, Level level) { super(type, level); }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return !isTame() && distance > 128.0D;
    }

    @Override
    public void setTame(boolean isTame, boolean includeSideEffects) {
        super.setTame(isTame, includeSideEffects);
        if (isTame) {
            setTarget(null);
            getNavigation().stop();
        }
    }

    @Override
    protected void applyTamingSideEffects() {
        super.applyTamingSideEffects();
        var maxHealth = getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(maxHealth.getBaseValue() * HEALTH_MULTIPLIER);
            if (isTame()) {
                setHealth(getMaxHealth());
            } else if (getHealth() > getMaxHealth()) {
                setHealth(getMaxHealth());
            }
        }
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.15, true));
        targetSelector.addGoal(2, new NonTameRandomTargetGoal<Player>(this, Player.class, true, null) {
            @Override
            public boolean canContinueToUse() {
                return !FeralWolf.this.isTame() && super.canContinueToUse();
            }
        });
    }
}
