package dev.itzgamesgd.moreminecrafts.equipment;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * Lightweight fallback for the approved parachute: a consumable Slow Falling I
 * application, with no new gliding physics, deployed model, or custom animation.
 * Item creation is independent from knowledge-gated recipe unlock.
 */
public final class ParachuteItem extends Item {
    public static final int SLOW_FALL_TICKS = 200;

    public ParachuteItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.onGround()) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, SLOW_FALL_TICKS, 0));
        if (!player.hasInfiniteMaterials()) player.getItemInHand(hand).shrink(1);
        return InteractionResult.SUCCESS_SERVER;
    }
}
