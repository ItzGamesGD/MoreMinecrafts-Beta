package dev.itzgamesgd.moreminecrafts.special.flame;

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
        }
        super.hurtEnemy(stack, target, attacker);
    }
}
