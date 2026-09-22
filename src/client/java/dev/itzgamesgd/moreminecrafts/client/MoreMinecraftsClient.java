package dev.itzgamesgd.moreminecrafts.client;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import dev.itzgamesgd.moreminecrafts.ability.EquipmentSkillInput;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.enhancement.EnhancementOperation;
import dev.itzgamesgd.moreminecrafts.network.EquipmentSkillPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.Component;

public final class MoreMinecraftsClient implements ClientModInitializer {
    private static final KeyMapping.Category EQUIPMENT_CATEGORY =
            KeyMapping.Category.register(ModIds.id("equipment"));
    private static final KeyMapping EQUIPMENT_SKILL = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.moreminecrafts.equipment_skill",
            InputConstants.Type.KEYBOARD,
            InputConstants.KEY_F,
            EQUIPMENT_CATEGORY
    ));

    @Override
    public void onInitializeClient() {
        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
            int level = EnhancementOperation.level(stack);
            if (level > 0) {
                lines.add(Component.translatable("item.moreminecrafts.enhancement_level", level)
                        .withStyle(ChatFormatting.AQUA));
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (EQUIPMENT_SKILL.consumeClick()) {
                if (client.player == null || client.gui.screen() != null
                        || !ClientPlayNetworking.canSend(EquipmentSkillPayload.TYPE)) {
                    continue;
                }

                EquipmentSkillInput input = client.hasShiftDown()
                        ? EquipmentSkillInput.SECONDARY
                        : EquipmentSkillInput.PRIMARY;
                ClientPlayNetworking.send(new EquipmentSkillPayload(input));
            }
        });

        MoreMinecrafts.LOGGER.info("MoreMinecrafts client initialization complete.");
    }
}
