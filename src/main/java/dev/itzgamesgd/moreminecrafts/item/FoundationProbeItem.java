package dev.itzgamesgd.moreminecrafts.item;

import dev.itzgamesgd.moreminecrafts.ability.AbilityActivation;
import dev.itzgamesgd.moreminecrafts.ability.FoundationProbeAbility;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public final class FoundationProbeItem extends Item {
    public FoundationProbeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) {
            var result = AbilityActivation.tryActivate(
                    serverPlayer,
                    player.getItemInHand(hand),
                    FoundationProbeAbility.INSTANCE
            );
            if (result == AbilityActivation.Result.ON_COOLDOWN) {
                serverPlayer.displayClientMessage(Component.literal("Foundation ability is cooling down"), true);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
