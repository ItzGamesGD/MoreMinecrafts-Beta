package dev.itzgamesgd.moreminecrafts.mob;

import dev.itzgamesgd.moreminecrafts.mixin.WolfMixinAccessors;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NonTameRandomTargetGoal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.animal.wolf.WolfSoundVariants;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;

public final class FeralWolf extends Wolf implements Enemy {
    private static final double HEALTH_MULTIPLIER = 1.15;
    private boolean deferTamingHealthProfile;

    public FeralWolf(EntityType<? extends Wolf> type, Level level) { super(type, level); }

    @Override
    public FeralWolf getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        FeralWolf child = (FeralWolf) CustomMobCatalog.FERAL_WOLF.create(level, EntitySpawnReason.BREEDING);
        if (child == null) return null;

        WolfMixinAccessors childAccess = (WolfMixinAccessors) (Object) child;
        if (otherParent instanceof Wolf otherWolf) {
            WolfMixinAccessors selfAccess = (WolfMixinAccessors) (Object) this;
            WolfMixinAccessors otherAccess = (WolfMixinAccessors) (Object) otherWolf;
            childAccess.moreminecrafts$invokeSetVariant(getRandom().nextBoolean()
                    ? selfAccess.moreminecrafts$invokeGetVariant() : otherAccess.moreminecrafts$invokeGetVariant());
            if (isTame()) {
                child.setOwnerReference(getOwnerReference());
                child.deferTamingHealthProfile = true;
                try {
                    child.setTame(true, true);
                } finally {
                    child.deferTamingHealthProfile = false;
                }
                childAccess.moreminecrafts$invokeSetCollarColor(
                        DyeColor.getMixedColor(level, getCollarColor(), otherWolf.getCollarColor()));
            }
        }
        childAccess.moreminecrafts$invokeSetSoundVariant(
                WolfSoundVariants.pickRandomSoundVariant(registryAccess(), getRandom()));
        CustomMobStats.apply(child);
        return child;
    }

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
            if (!deferTamingHealthProfile) {
                maxHealth.setBaseValue(maxHealth.getBaseValue() * HEALTH_MULTIPLIER);
            }
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
