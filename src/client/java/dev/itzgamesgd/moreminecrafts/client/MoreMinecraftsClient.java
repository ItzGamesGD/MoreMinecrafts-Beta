package dev.itzgamesgd.moreminecrafts.client;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import dev.itzgamesgd.moreminecrafts.enhancement.EnhancementOperation;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public final class MoreMinecraftsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
            int level = EnhancementOperation.level(stack);
            if (level > 0) {
                lines.add(Component.translatable("item.moreminecrafts.enhancement_level", level)
                        .withStyle(ChatFormatting.AQUA));
            }
        });
        MoreMinecrafts.LOGGER.info("MoreMinecrafts client initialization complete.");
    }
}
