package dev.itzgamesgd.moreminecrafts.special.flame;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class FlameSwordItem extends Item {
    public FlameSwordItem(Properties properties) {
        super(properties);
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof ServerPlayer serverPlayer) {
            FlameSwordEffects.splash(serverPlayer, target);
            ServerLevel level = serverPlayer.level();
            level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        super.hurtEnemy(stack, target, attacker);
    }
}
