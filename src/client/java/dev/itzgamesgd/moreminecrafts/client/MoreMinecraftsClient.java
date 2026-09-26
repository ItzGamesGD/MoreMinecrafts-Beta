package dev.itzgamesgd.moreminecrafts.client;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import dev.itzgamesgd.moreminecrafts.ability.EquipmentSkillInput;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.enhancement.EnhancementOperation;
import dev.itzgamesgd.moreminecrafts.network.EquipmentSkillPayload;
import dev.itzgamesgd.moreminecrafts.network.FishingChallengePayload;
import dev.itzgamesgd.moreminecrafts.fishing.FishItem;
import dev.itzgamesgd.moreminecrafts.fishing.FishingGrade;
import dev.itzgamesgd.moreminecrafts.fishing.FishingTiming;
import dev.itzgamesgd.moreminecrafts.registry.ModDataComponents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.Component;

public final class MoreMinecraftsClient implements ClientModInitializer {
    private static long fishingChallengeStart=-1;
    private static int fishingDifficulty=1;
    private static final KeyMapping.Category EQUIPMENT_CATEGORY =
            KeyMapping.Category.register(ModIds.id("equipment"));
    private static final KeyMapping EQUIPMENT_SKILL = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.moreminecrafts.equipment_skill",
            InputConstants.Type.KEYBOARD,
            InputConstants.KEY_F,
            EQUIPMENT_CATEGORY
    ));

    public static boolean fishingChallengeActive() { return fishingChallengeStart >= 0; }

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(FishingChallengePayload.TYPE,(payload,context)-> context.client().execute(() ->
                { fishingChallengeStart=payload.active() ? payload.startTick() : -1; fishingDifficulty=payload.difficulty(); }));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> fishingChallengeStart = -1);
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                ModIds.id("fishing_timing"), (graphics, delta) -> {
            var client=net.minecraft.client.Minecraft.getInstance();
            if(fishingChallengeStart<0 || client.level==null)return;
            int width=160,height=10,x=(graphics.guiWidth()-width)/2,y=graphics.guiHeight()-65;
            graphics.fill(x,y,x+width,y+height,0xCC9B2020);
            fillZone(graphics, x, y, width, height, .18, .32, 0xCCE87822);
            fillZone(graphics, x, y, width, height, .32, .42, 0xCCE8CF22);
            fillZone(graphics, x, y, width, height, .42, .58, 0xCC62C878);
            fillZone(graphics, x, y, width, height, .58, .68, 0xCCE8CF22);
            fillZone(graphics, x, y, width, height, .68, .82, 0xCCE87822);
            double position=FishingTiming.position(client.level.getGameTime()-fishingChallengeStart, fishingDifficulty);
            int marker=x+(int)(position*width);
            graphics.fill(marker-1,y-3,marker+2,y+height+3,0xFFFFFFFF);
        });

        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
            if(stack.getItem() instanceof FishItem fish){
                int grade=stack.getOrDefault(ModDataComponents.FISH_GRADE,1);
                int length=stack.getOrDefault(ModDataComponents.FISH_LENGTH_TENTHS_CM,fish.species().lengthDistribution().minimum());
                int weight=stack.getOrDefault(ModDataComponents.FISH_WEIGHT_TENTHS_KG,fish.species().weightDistribution().minimum());
                lines.add(Component.translatable("tooltip.moreminecrafts.fish.grade",FishingGrade.roman(grade)).withStyle(ChatFormatting.GOLD));
                lines.add(Component.translatable("tooltip.moreminecrafts.fish.length",formatTenths(length)).withStyle(ChatFormatting.GRAY));
                lines.add(Component.translatable("tooltip.moreminecrafts.fish.weight",formatTenths(weight)).withStyle(ChatFormatting.GRAY));
            }
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

    private static String formatTenths(int value) { return "%d.%d".formatted(value / 10, Math.abs(value % 10)); }
    private static void fillZone(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int x, int y, int width, int height,
                                 double from, double to, int color) {
        graphics.fill(x + (int) (width * from), y, x + (int) (width * to), y + height, color);
    }
}
