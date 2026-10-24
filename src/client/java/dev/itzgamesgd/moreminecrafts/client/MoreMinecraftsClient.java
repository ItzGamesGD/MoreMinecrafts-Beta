package dev.itzgamesgd.moreminecrafts.client;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.enhancement.EnhancementOperation;
import dev.itzgamesgd.moreminecrafts.enhancement.EnhancementCatalog;
import dev.itzgamesgd.moreminecrafts.enhancement.EnhancementEffect;
import dev.itzgamesgd.moreminecrafts.enhancement.EnhancementEffects;
import dev.itzgamesgd.moreminecrafts.network.FishingChallengePayload;
import dev.itzgamesgd.moreminecrafts.network.MovementJumpPayload;
import dev.itzgamesgd.moreminecrafts.network.ProgressionSnapshotPayload;
import dev.itzgamesgd.moreminecrafts.network.ProgressionDeltaPayload;
import dev.itzgamesgd.moreminecrafts.network.ProgressionActionResultPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import dev.itzgamesgd.moreminecrafts.fishing.FishItem;
import dev.itzgamesgd.moreminecrafts.fishing.FishSpecies;
import dev.itzgamesgd.moreminecrafts.fishing.FishingGrade;
import dev.itzgamesgd.moreminecrafts.fishing.FishingTiming;
import dev.itzgamesgd.moreminecrafts.registry.ModDataComponents;
import dev.itzgamesgd.moreminecrafts.registry.ModMenuTypes;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import dev.itzgamesgd.moreminecrafts.mob.CustomMobCatalog;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public final class MoreMinecraftsClient implements ClientModInitializer {
    private static long fishingChallengeStart=-1;
    private static int fishingDifficulty=1;
    private static int fishingTechniqueLevel;
    private static FishSpecies fishingSpecies;
    private static boolean fishingCaptureAllowed;
    private static int fishingRequiredMinnowTier = 1;
    private static boolean jumpHeld;
    public static boolean fishingChallengeActive() { return fishingChallengeStart >= 0; }

    @Override
    public void onInitializeClient() {
        dev.itzgamesgd.moreminecrafts.bounty.ContractUiBridge.setOpener(contractId ->
                net.minecraft.client.Minecraft.getInstance().execute(() ->
                        net.minecraft.client.Minecraft.getInstance().gui.setScreen(new ContractScreen(contractId))));
        VisualValidationHarness.register();
        registerCustomMobRenderers();
        MenuScreens.register(ModMenuTypes.FISHING_ROD, FishingRodScreen::new);
        MenuScreens.register(ModMenuTypes.AUTO_FARM, AutoFarmScreen::new);
        MenuScreens.register(ModMenuTypes.ENCHANTING_PROGRESSION, EnchantingProgressionScreen::new);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean jump = client.options.keyJump.isDown();
            if (jump && !jumpHeld && client.getConnection() != null) {
                ClientPlayNetworking.send(MovementJumpPayload.INSTANCE);
            }
            jumpHeld = jump;
        });
        ClientPlayNetworking.registerGlobalReceiver(FishingChallengePayload.TYPE,(payload,context)-> context.client().execute(() ->
                {
                    fishingChallengeStart=payload.active() ? payload.startTick() : -1;
                    fishingDifficulty=payload.difficulty();
                    fishingTechniqueLevel=payload.active() ? payload.fishingTechniqueLevel() : 0;
                    fishingCaptureAllowed=payload.active() && payload.captureAllowed();
                    fishingRequiredMinnowTier=payload.requiredMinnowTier();
                    fishingSpecies=payload.active() && payload.speciesOrdinal() >= 0
                            && payload.speciesOrdinal() < FishSpecies.values().length
                            ? FishSpecies.values()[payload.speciesOrdinal()] : null;
                }));
        ClientPlayNetworking.registerGlobalReceiver(ProgressionSnapshotPayload.TYPE, (payload, context) ->
                context.client().execute(() -> ProgressionClientApi.replace(payload.revision(), payload.values())));
        ClientPlayNetworking.registerGlobalReceiver(ProgressionDeltaPayload.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    if (!ProgressionClientApi.apply(payload.revision(), payload.changes()))
                        ProgressionClientApi.requestSnapshot();
                }));
        ClientPlayNetworking.registerGlobalReceiver(ProgressionActionResultPayload.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    ProgressionClientApi.actionResult(payload.actionId(), payload.resultCode(), payload.revision());
                    // Resolve any missed/out-of-order progression deltas before refreshing visible data.
                    ProgressionClientApi.requestSnapshot();
                }));
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> ProgressionClientApi.requestSnapshot());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ProgressionClientApi.reset();
            fishingChallengeStart = -1;
            fishingSpecies = null;
            fishingCaptureAllowed = false;
            fishingRequiredMinnowTier = 1;
            fishingTechniqueLevel = 0;
            jumpHeld = false;
        });
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                ModIds.id("fishing_timing"), (graphics, delta) -> {
            var client=net.minecraft.client.Minecraft.getInstance();
            if(fishingChallengeStart<0 || client.level==null)return;
            int width=260,x=(graphics.guiWidth()-width)/2,y=graphics.guiHeight()-93;
            drawFishingLayer(graphics, "fishing_name_panel", x + 64, y - 28, 132, 34, 993, 256);
            drawFishingLayer(graphics, "fishing_gauge", x, y + 5, width, 37, 1960, 277);
            // The supplied gauge includes a centered example marker. Cover that strip at render
            // time with the neighboring green section, then move the separate approved marker.
            graphics.pose().pushMatrix();
            graphics.pose().translate(x + width * 1014f / 1960f, y + 5);
            graphics.pose().scale(width / 1960f, 37f / 277f);
            graphics.blit(RenderPipelines.GUI_TEXTURED,
                    fishingTexture("fishing_gauge"), 0, 0, 946, 0, 68, 277, 68, 277, 1960, 277);
            graphics.pose().popMatrix();
            int gaugeStart = x + 11;
            int gaugeWidth = width - 22;
            int greenWidth = (int) Math.round(gaugeWidth
                    * 2.0D * dev.itzgamesgd.moreminecrafts.progression.ResearchContentRules
                    .fishingGreenHalfWidth(fishingTechniqueLevel));
            int gaugeCenter = gaugeStart + gaugeWidth / 2;
            if (fishingTechniqueLevel > 0)
                graphics.fill(gaugeCenter - greenWidth / 2, y + 16,
                        gaugeCenter + greenWidth / 2, y + 29, 0x9952D875);
            double position=FishingTiming.position(client.level.getGameTime()-fishingChallengeStart, fishingDifficulty);
            int marker=x+11+(int)(position*(width-22));
            drawFishingMarker(graphics, marker - 8, y);
            drawFishingLayer(graphics, "fishing_help_panel", x + 68, y + 46, 124, 34, 997, 272);
            if (fishingSpecies != null) {
                Component name = Component.translatable("item.moreminecrafts." + fishingSpecies.id());
                graphics.text(client.font, name, x + (width-client.font.width(name))/2, y - 15,
                        0xFFFFFFFF, true);
                if (!fishingCaptureAllowed) {
                    Component required = Component.translatable("hud.moreminecrafts.fishing.minnow_required",
                            FishingGrade.roman(fishingRequiredMinnowTier));
                    graphics.text(client.font, required, x + (width-client.font.width(required))/2, y - 4,
                            0xFFFFD36A, true);
                }
            }
            Component help = Component.translatable("hud.moreminecrafts.fishing.stop");
            graphics.text(client.font, help, x + 116, y + 57, 0xFFFFFFFF, true);
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
                EnhancementCatalog.definition(stack.getItem()).ifPresent(definition -> {
                    for (EnhancementEffect effect : definition.profile().effects()) {
                        double value = EnhancementEffects.value(definition, level, effect.target());
                        if (value <= 0.0D) continue;
                        String formatted = effect.target() == EnhancementEffect.Target.DAMAGE_REDUCTION
                                ? "%+.1f%%".formatted(value * 100.0D) : "%+.2f".formatted(value);
                        lines.add(Component.translatable("tooltip.moreminecrafts.enhancement.effect."
                                + effect.target().name().toLowerCase(), formatted).withStyle(ChatFormatting.GRAY));
                    }
                    int reached = Math.min(level, definition.category().maximumLevel()) / 5;
                    lines.add(Component.translatable("tooltip.moreminecrafts.enhancement.milestones", reached, 4)
                            .withStyle(ChatFormatting.DARK_AQUA));
                });
            }
        });

        MoreMinecrafts.LOGGER.info("MoreMinecrafts client initialization complete.");
    }

    private static void registerCustomMobRenderers() {
        EntityRendererRegistry.register(CustomMobCatalog.SWAMP_ZOMBIE,
                context -> CustomMobRenderers.zombie(context, "swamp_zombie"));
        EntityRendererRegistry.register(CustomMobCatalog.CAVE_ZOMBIE,
                context -> CustomMobRenderers.zombie(context, "cave_zombie"));
        EntityRendererRegistry.register(CustomMobCatalog.LARGE_ZOMBIE,
                context -> CustomMobRenderers.zombie(context, "large_zombie"));
        EntityRendererRegistry.register(CustomMobCatalog.FROZEN_ZOMBIE,
                context -> CustomMobRenderers.zombie(context, "frozen_zombie"));
        EntityRendererRegistry.register(CustomMobCatalog.MOSSY_ZOMBIE,
                context -> CustomMobRenderers.zombie(context, "mossy_zombie"));
        EntityRendererRegistry.register(CustomMobCatalog.FUNGAL_ZOMBIE,
                context -> CustomMobRenderers.zombie(context, "fungal_zombie"));
        EntityRendererRegistry.register(CustomMobCatalog.MINER_ZOMBIE,
                context -> CustomMobRenderers.zombie(context, "miner_zombie"));
        EntityRendererRegistry.register(CustomMobCatalog.ROTTEN_SKELETON,
                context -> CustomMobRenderers.skeleton(context, "rotten_skeleton"));
        EntityRendererRegistry.register(CustomMobCatalog.CAVE_SKELETON,
                context -> CustomMobRenderers.skeleton(context, "cave_skeleton"));
        EntityRendererRegistry.register(CustomMobCatalog.ARMORED_SKELETON,
                context -> CustomMobRenderers.skeleton(context, "armored_skeleton"));
        EntityRendererRegistry.register(CustomMobCatalog.ASHEN_SKELETON,
                context -> CustomMobRenderers.skeleton(context, "ashen_skeleton"));
        EntityRendererRegistry.register(CustomMobCatalog.LESSER_WITHER_SKELETON,
                context -> CustomMobRenderers.witherSkeleton(context, "lesser_wither_skeleton"));
        EntityRendererRegistry.register(CustomMobCatalog.MOUNTAIN_SKELETON,
                context -> CustomMobRenderers.stray(context, "mountain_skeleton"));
        EntityRendererRegistry.register(CustomMobCatalog.MINER_SKELETON,
                context -> CustomMobRenderers.skeleton(context, "miner_skeleton"));
        EntityRendererRegistry.register(CustomMobCatalog.SMALL_SPIDER,
                context -> CustomMobRenderers.caveSpider(context, "small_spider"));
        EntityRendererRegistry.register(CustomMobCatalog.LARGE_SPIDER,
                context -> CustomMobRenderers.spider(context, "large_spider"));
        EntityRendererRegistry.register(CustomMobCatalog.FOREST_SPIDER,
                context -> CustomMobRenderers.spider(context, "forest_spider"));
        EntityRendererRegistry.register(CustomMobCatalog.DESERT_SPIDER,
                context -> CustomMobRenderers.spider(context, "desert_spider"));
        EntityRendererRegistry.register(CustomMobCatalog.SWAMP_SPIDER,
                context -> CustomMobRenderers.caveSpider(context, "swamp_spider"));
        EntityRendererRegistry.register(CustomMobCatalog.ICE_SPIDER,
                context -> CustomMobRenderers.spider(context, "ice_spider"));
        EntityRendererRegistry.register(CustomMobCatalog.CAVE_BUG,
                context -> CustomMobRenderers.silverfish(context, "cave_bug"));
        EntityRendererRegistry.register(CustomMobCatalog.DESERT_BUG,
                context -> CustomMobRenderers.silverfish(context, "desert_bug"));
        EntityRendererRegistry.register(CustomMobCatalog.END_BUG,
                context -> CustomMobRenderers.endermite(context, "end_bug"));
        EntityRendererRegistry.register(CustomMobCatalog.WILD_SLIME,
                context -> CustomMobRenderers.slime(context, "wild_slime"));
        EntityRendererRegistry.register(CustomMobCatalog.MUD_SLIME,
                context -> CustomMobRenderers.slime(context, "mud_slime"));
        EntityRendererRegistry.register(CustomMobCatalog.SAND_SLIME,
                context -> CustomMobRenderers.slime(context, "sand_slime"));
        EntityRendererRegistry.register(CustomMobCatalog.LESSER_MAGMA_CUBE,
                context -> CustomMobRenderers.magmaCube(context, "lesser_magma_cube"));
        EntityRendererRegistry.register(CustomMobCatalog.DROWNED_SAILOR,
                context -> CustomMobRenderers.drowned(context, "drowned_sailor"));
        EntityRendererRegistry.register(CustomMobCatalog.DEEP_SEA_DROWNED,
                context -> CustomMobRenderers.drowned(context, "deep_sea_drowned"));
        EntityRendererRegistry.register(CustomMobCatalog.RIVER_DROWNED,
                context -> CustomMobRenderers.drowned(context, "river_drowned"));
        EntityRendererRegistry.register(CustomMobCatalog.WILD_PIGLIN,
                context -> CustomMobRenderers.piglin(context, "wild_piglin"));
        EntityRendererRegistry.register(CustomMobCatalog.BANDIT,
                context -> CustomMobRenderers.pillager(context, "bandit"));
        EntityRendererRegistry.register(CustomMobCatalog.BANDIT_ARCHER,
                context -> CustomMobRenderers.pillager(context, "bandit_archer"));
        EntityRendererRegistry.register(CustomMobCatalog.BANDIT_WARRIOR,
                context -> CustomMobRenderers.vindicator(context, "bandit_warrior"));
        EntityRendererRegistry.register(CustomMobCatalog.WANDERING_RAIDER,
                context -> CustomMobRenderers.pillager(context, "wandering_raider"));
        EntityRendererRegistry.register(CustomMobCatalog.FERAL_WOLF,
                context -> CustomMobRenderers.wolf(context, "feral_wolf"));
        EntityRendererRegistry.register(CustomMobCatalog.CAVE_BAT,
                context -> CustomMobRenderers.bat(context, "cave_bat"));
        EntityRendererRegistry.register(CustomMobCatalog.GIANT_BEE,
                context -> CustomMobRenderers.bee(context, "giant_bee"));
        EntityRendererRegistry.register(CustomMobCatalog.LESSER_PHANTOM,
                context -> CustomMobRenderers.phantom(context, "lesser_phantom"));
        EntityRendererRegistry.register(CustomMobCatalog.CAVE_CREEPER,
                context -> CustomMobRenderers.creeper(context, "cave_creeper"));
    }

    private static String formatTenths(int value) { return "%d.%d".formatted(value / 10, Math.abs(value % 10)); }
    private static Identifier fishingTexture(String name) {
        return ModIds.id("textures/gui/fishing/minigame/" + name + ".png");
    }

    private static void drawFishingLayer(GuiGraphicsExtractor graphics, String name, int x, int y,
                                         int width, int height, int nativeWidth, int nativeHeight) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(width / (float) nativeWidth, height / (float) nativeHeight);
        graphics.blit(RenderPipelines.GUI_TEXTURED, fishingTexture(name), 0, 0, 0, 0,
                nativeWidth, nativeHeight, nativeWidth, nativeHeight);
        graphics.pose().popMatrix();
    }

    private static void drawFishingMarker(GuiGraphicsExtractor graphics, int x, int y) {
        // The approved marker PNG also contains a frame fragment on its left;
        // select its marker region at render time without changing the source PNG.
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(16f / 80f, 40f / 299f);
        graphics.blit(RenderPipelines.GUI_TEXTURED, fishingTexture("fishing_marker"),
                0, 0, 116, 0, 80, 299, 80, 299, 212, 299);
        graphics.pose().popMatrix();
    }
}
