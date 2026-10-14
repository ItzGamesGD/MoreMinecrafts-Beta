package dev.itzgamesgd.moreminecrafts.alchemy;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

final class ShockMobEffect extends MobEffect {
    // CSS sky blue. This native effect color also tints the vanilla potion liquid.
    static final int SKY_BLUE = 0x87CEEB;

    ShockMobEffect() {
        super(MobEffectCategory.HARMFUL, SKY_BLUE);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int remainingTicks, int amplifier) {
        return ShockTiming.shouldPulse(remainingTicks);
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (!entity.isAlive()) return false;

        entity.hurt(level.damageSources().magic(), ShockTiming.PULSE_DAMAGE);
        if (entity.isAlive()) {
            entity.addEffect(new MobEffectInstance(
                    ModAlchemy.SHOCK_ROOT,
                    ShockTiming.ROOT_DURATION_TICKS,
                    0,
                    false,
                    false,
                    false));
        }
        return true;
    }
}
