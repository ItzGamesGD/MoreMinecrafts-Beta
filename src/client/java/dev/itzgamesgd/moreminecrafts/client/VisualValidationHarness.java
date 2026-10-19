package dev.itzgamesgd.moreminecrafts.client;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.progression.AutoFarmMenu;
import dev.itzgamesgd.moreminecrafts.progression.ProgressionCanvasGeometry;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;

import java.nio.file.Files;
import com.mojang.blaze3d.platform.InputConstants;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.client.input.KeyEvent;

public final class VisualValidationHarness {
    private static final String OPT_IN_ENV = "MOREMINECRAFTS_VISUAL_VALIDATION";
    private static final String SERVER_ENV = "MOREMINECRAFTS_VISUAL_SERVER_ADDRESS";
    private static final boolean PHYSICAL_SELECTOR_TEST = "1".equals(System.getenv("MOREMINECRAFTS_VISUAL_SELECTOR_PHYSICAL"));
    private static final boolean ENABLED = "1".equals(System.getenv(OPT_IN_ENV));
    private static final String SCREENSHOT_NAME = "visual-world.png";
    private static boolean connectionRequested;
    private static boolean playerJoinedSignalEmitted;
    private static boolean readySignalEmitted;
    private static volatile boolean frameReady;
    private static volatile boolean captureRequested;
    private static volatile boolean captureFinished;
    private static int startupTicks;
    private static int readyTicks;
    private static int renderedFramesAfterReady;
    private static boolean uiJourneyStarted;
    private static boolean uiJourneyFailed;
    private static int uiJourneyStage;
    private static int uiFrames;
    private static String uiCaptureName;
    private static volatile boolean uiCaptureComplete;
    private static int purchaseXpBefore;
    private static int purchaseChestBefore;
    private static int purchaseIronBlocksBefore;
    private static int purchasePaperBefore;
    private static int physicalSelectorStage;
    private static boolean physicalSelectorDone;

    private VisualValidationHarness() {
    }

    public static void register() {
        if (!ENABLED) {
            return;
        }

        MoreMinecrafts.LOGGER.info("Visual-validation test hook enabled by {}", OPT_IN_ENV);
        ClientTickEvents.END_CLIENT_TICK.register(VisualValidationHarness::onClientTick);
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.CHAT,
                ModIds.id("visual_validation_frame"),
                (graphics, delta) -> onHudRender(Minecraft.getInstance())
        );
    }

    private static void onClientTick(Minecraft client) {
        if (captureFinished) {
            runUiJourney(client);
            return;
        }

        if (readySignalEmitted) {
            if (frameReady && !captureRequested) {
                requestNativeScreenshot(client);
                return;
            }
            if (++readyTicks > 1800) {
                failCapture(frameReady
                        ? "native screenshot callback did not complete within 90 seconds"
                        : "no rendered HUD frame within 90 seconds");
            }
            return;
        }

        if (client.player == null || client.level == null) {
            if (!connectionRequested && client.gui.screen() != null && ++startupTicks >= 60) {
                connectToTestServer(client);
            }
            return;
        }

        if (!playerJoinedSignalEmitted) {
            playerJoinedSignalEmitted = true;
            MoreMinecrafts.LOGGER.info("VISUAL_VALIDATION_PLAYER_JOINED");
        }

        if (client.gui.screen() != null || ++readyTicks < 120) {
            return;
        }

        // The test server fixes the spawn and scene. Orient the real client camera toward it.
        client.player.setYRot(180.0F);
        client.player.setXRot(8.0F);
        readySignalEmitted = true;
        readyTicks = 0;
        MoreMinecrafts.LOGGER.info(
                "VISUAL_VALIDATION_WORLD_READY dimension={} player=({}, {}, {}) ticks={}",
                client.level.dimension(),
                client.player.getX(),
                client.player.getY(),
                client.player.getZ(),
                120
        );
    }

    private static void onHudRender(Minecraft client) {
        if (uiCaptureName != null && ++uiFrames >= 4) {
            captureUiScreenshot(client);
            return;
        }
        if (!readySignalEmitted || frameReady || captureRequested || captureFinished
                || client.player == null || client.level == null || client.gui.screen() != null) {
            return;
        }

        // This HUD hook runs after the world has rendered into the main target for this frame.
        if (++renderedFramesAfterReady < 30) {
            return;
        }

        frameReady = true;
        MoreMinecrafts.LOGGER.info(
                "VISUAL_VALIDATION_FRAME_READY milestone=HudElementRegistry.after(CHAT) rendered_frames={}",
                renderedFramesAfterReady
        );
    }

    /**
     * Synthetic Screen callback journey. These checks are explicitly distinct from X11 input,
     * which is only called physical when the shell harness observes the resulting screen event.
     */
    private static void runUiJourney(Minecraft client) {
        if (uiJourneyFailed || client.player == null || client.level == null) return;
        if (!uiJourneyStarted) {
            int missingIcons = ProgressionTreeCanvas.validateIconMappings();
            if (missingIcons != 0) {
                uiJourneyFailed = true;
                MoreMinecrafts.LOGGER.error("CLIENT_UI_ICON_MAPPING_FAILED missing={} of 157 configured node icons", missingIcons);
                return;
            }
            uiJourneyStarted = true;
            client.gui.setScreen(new ProgressionSelectionScreen());
            queueUiScreenshot("progression-selection.png");
            uiJourneyStage = 200;
            return;
        }
        if (PHYSICAL_SELECTOR_TEST && physicalSelectorStage > 0 && !physicalSelectorDone) {
            advancePhysicalSelectorTest(client);
            if (!physicalSelectorDone) return;
        }
        if (uiCaptureName != null) return;
        switch (uiJourneyStage) {
            case 200 -> {
                if (!takeCapture()) return;
                if (!(client.gui.screen() instanceof ProgressionSelectionScreen selector)
                        || !selector.validationHitboxesAligned()) {
                    failJourney("selector card hitboxes do not match the card bounds"); return;
                }
                MoreMinecrafts.LOGGER.info("SELECTOR_HITBOXES_ALIGNED gui_scale={} size={}x{}",
                        client.getWindow().getGuiScale(), client.getWindow().getGuiScaledWidth(),
                        client.getWindow().getGuiScaledHeight());
                if (!selector.validationQuestTextFits(net.minecraft.network.chat.Component.translatable("moreminecrafts.ui.selector.quests"),
                        net.minecraft.network.chat.Component.translatable("moreminecrafts.ui.selector.quests.description"))
                        || !selector.validationQuestTextFits(net.minecraft.network.chat.Component.literal("XP Quests"),
                        net.minecraft.network.chat.Component.literal("Special objectives that grant experience when completed."))) {
                    failJourney("XP Quest card text does not fit its boxes (active or English text)"); return;
                }
                MoreMinecrafts.LOGGER.info("SELECTOR_TEXT_FITS active_language_and_english=true");
                selector.validationSetHover("xp_quest");
                queueUiScreenshot("selector-xp-hover.png");
                uiJourneyStage = 201;
            }
            case 201 -> {
                if (!takeCapture()) return;
                if (!(client.gui.screen() instanceof ProgressionSelectionScreen selector)) {
                    failJourney("selector closed before the focus check"); return;
                }
                selector.validationSetHover(null);
                for (int i = 0; i < 4; i++) selector.validationKey(InputConstants.KEY_TAB, false);
                if (!"xp_quest".equals(selector.validationFocusedTarget())) {
                    failJourney("Tab x4 did not focus the XP Quest card"); return;
                }
                selector.validationKey(InputConstants.KEY_LEFT, false);
                if ("none".equals(selector.validationFocusedTarget()) || "xp_quest".equals(selector.validationFocusedTarget())) {
                    failJourney("Left arrow did not move focus off the XP Quest card"); return;
                }
                selector.validationKey(InputConstants.KEY_RIGHT, false);
                if (!"xp_quest".equals(selector.validationFocusedTarget())) {
                    failJourney("Left/Right arrows did not round-trip on the XP Quest card"); return;
                }
                queueUiScreenshot("selector-xp-focus.png");
                uiJourneyStage = 202;
            }
            case 202 -> {
                if (!takeCapture()) return;
                if (!(client.gui.screen() instanceof ProgressionSelectionScreen selector)
                        || !selector.validationKey(InputConstants.KEY_RETURN, false)
                        || !(client.gui.screen() instanceof ProgressionBrowseScreen quests)
                        || quests.mode() != ProgressionBrowseScreen.Mode.QUEST
                        || !quests.validationTreeReady()) {
                    failJourney("Enter on the focused XP Quest card did not open the quest tree"); return;
                }
                MoreMinecrafts.LOGGER.info("SELECTOR_KEYBOARD_VERIFIED target=xp_quest keys=Tab,Left,Right,Enter");
                queueUiScreenshot("selector-xp-quest-opened-by-keyboard.png");
                uiJourneyStage = 203;
            }
            case 203 -> {
                if (!takeCapture()) return;
                client.gui.setScreen(new ProgressionSelectionScreen());
                queueUiScreenshot("selector-normal-final.png");
                uiJourneyStage = 1;
            }
            case 1 -> {
                if (!takeCapture()) return;
                clickSelectionChoice(client, 2);
                if (!(client.gui.screen() instanceof ProgressionDisciplineSelectionScreen hub)
                        || hub.mode() != ProgressionBrowseScreen.Mode.KNOWLEDGE || !hub.validationVisitAllSectors()) {
                    failJourney("knowledge selector did not reach the seven-sector menu"); return;
                }
                queueUiScreenshot("knowledge-sector-selection.png");
                uiJourneyStage = 10;
            }
            case 10 -> {
                if (!takeCapture()) return;
                if (!(client.gui.screen() instanceof ProgressionDisciplineSelectionScreen hub)
                        || !hub.validationOpenSector("resources")
                        || !(client.gui.screen() instanceof ProgressionBrowseScreen browse)
                        || !browse.validationTreeReady() || !browse.validationExerciseCanvasControls()
                        || !browse.validationKeyboardNodeFocus() || !browse.validationOpenBranchMenu()) {
                    failJourney("knowledge tree navigation, pan, wheel zoom, or branch menu failed"); return;
                }
                queueUiScreenshot("knowledge-branch-menu.png");
                uiJourneyStage = 11;
            }
            case 11 -> {
                if (!takeCapture()) return;
                if (!(client.gui.screen() instanceof ProgressionBrowseScreen browse)) { failJourney("knowledge tree disappeared"); return; }
                if (!browse.validationClickBranchForNode("knowledge.1")
                        || !browse.validationHoverNode("knowledge.1")) { failJourney("knowledge direct branch selection or tooltip target missing"); return; }
                queueUiScreenshot("knowledge-node-tooltip.png");
                uiJourneyStage = 12;
            }
            case 12 -> {
                if (!takeCapture()) return;
                if (!(client.gui.screen() instanceof ProgressionBrowseScreen browse)) return;
                browse.validationClearHover();
                queueUiScreenshot("knowledge-screen.png");
                uiJourneyStage = 13;
            }
            case 13 -> {
                if (!takeCapture()) return;
                if (!(client.gui.screen() instanceof ProgressionBrowseScreen browse)) return;
                browse.onClose();
                if (client.gui.screen() instanceof ProgressionDisciplineSelectionScreen hub) hub.onClose();
                clickSelectionChoice(client, 3);
                if (!(client.gui.screen() instanceof ProgressionDisciplineSelectionScreen hub)
                        || hub.mode() != ProgressionBrowseScreen.Mode.RESEARCH || !hub.validationVisitAllSectors()) {
                    failJourney("research selector did not reach the seven-sector menu"); return;
                }
                queueUiScreenshot("research-sector-selection.png");
                uiJourneyStage = 3;
            }
            case 3 -> {
                if (!takeCapture()) return;
                if (!(client.gui.screen() instanceof ProgressionDisciplineSelectionScreen hub)
                        || !hub.validationOpenSector("production")
                        || !(client.gui.screen() instanceof ProgressionBrowseScreen browse)
                        || !browse.validationTreeReady() || !browse.validationExerciseCanvasControls()
                        || !browse.validationKeyboardNodeFocus()) {
                    failJourney("research tree navigation, pan, wheel zoom, or branch menu failed"); return;
                }
                queueUiScreenshot("research-screen.png");
                uiJourneyStage = 30;
            }
            case 30 -> {
                if (!takeCapture()) return;
                if (!(client.gui.screen() instanceof ProgressionBrowseScreen browse)
                        || !browse.validationOpenBranchMenu()) { failJourney("research branch list did not open"); return; }
                queueUiScreenshot("research-branch-menu.png");
                uiJourneyStage = 31;
            }
            case 31 -> {
                if (!takeCapture()) return;
                if (!(client.gui.screen() instanceof ProgressionBrowseScreen browse)) return;
                if (!browse.validationClickBranchForNode("nearby_chest_ingredients")
                        || !browse.validationHoverNode("nearby_chest_ingredients")) {
                    failJourney("purchasable research branch or tooltip target missing"); return;
                }
                queueUiScreenshot("research-node-tooltip.png");
                uiJourneyStage = 32;
            }
            case 32 -> {
                if (!takeCapture()) return;
                if (!(client.gui.screen() instanceof ProgressionBrowseScreen browse)) return;
                browse.validationClearHover();
                double beforeZoom = browse.validationCanvas().camera().zoom();
                var view = dev.itzgamesgd.moreminecrafts.progression.ProgressionCanvasGeometry.responsiveLayout(
                        client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight()).viewport();
                browse.mouseScrolled(view.x() + view.width() / 2.0, view.y() + view.height() / 2.0, 0, 1);
                if (browse.validationCanvas().camera().zoom() <= beforeZoom) { failJourney("wheel zoom did not increase tree zoom"); return; }
                queueUiScreenshot("research-zoomed.png");
                uiJourneyStage = 33;
            }
            case 33 -> {
                if (!takeCapture()) return;
                if (!(client.gui.screen() instanceof ProgressionBrowseScreen browse)) return;
                queueUiScreenshot("research-purchasable-entry.png");
                uiJourneyStage = 34;
            }
            case 34 -> {
                if (!takeCapture()) return;
                if (!(client.gui.screen() instanceof ProgressionBrowseScreen browse)) return;
                browse.validationFocusSelected();
                purchaseXpBefore = dev.itzgamesgd.moreminecrafts.progression.RawXp.available(client.player);
                purchaseChestBefore = itemCount(client, "minecraft:chest");
                purchaseIronBlocksBefore = itemCount(client, "minecraft:iron_block");
                purchasePaperBefore = itemCount(client, "minecraft:paper");
                boolean clickAccepted = browse.validationClickNode("nearby_chest_ingredients");
                boolean sameScreen = client.gui.screen() == browse;
                boolean purchasePending = ProgressionClientApi.purchasePending("nearby_chest_ingredients");
                var clickedRow = ProgressionClientApi.research(null).stream()
                        .filter(entry -> entry.id().equals("nearby_chest_ingredients")).findFirst().orElse(null);
                if (!clickAccepted || !sameScreen || !purchasePending) {
                    failJourney("one-click purchase failed: accepted=" + clickAccepted + " same_screen=" + sameScreen
                            + " pending=" + purchasePending + " availability="
                            + (clickedRow == null ? "missing" : clickedRow.availability())
                            + " level=" + (clickedRow == null ? -1 : clickedRow.level())); return;
                }
                MoreMinecrafts.LOGGER.info("CLIENT_UI_ONE_CLICK_SUBMITTED research=nearby_chest_ingredients input=synthetic_screen_callback");
                uiJourneyStage = 4;
            }
            case 4 -> {
                if (ProgressionClientApi.lastActionResult() < 0
                        || !"research:nearby_chest_ingredients".equals(ProgressionClientApi.lastAction())) return;
                int expected = dev.itzgamesgd.moreminecrafts.progression.ResearchPurchaseService.Result.PURCHASED.ordinal();
                var row = ProgressionClientApi.research(null).stream()
                        .filter(entry -> entry.id().equals("nearby_chest_ingredients")).findFirst().orElse(null);
                if (ProgressionClientApi.lastActionResult() != expected || row == null || row.level() != 1) return;
                int xpSpent = purchaseXpBefore - dev.itzgamesgd.moreminecrafts.progression.RawXp.available(client.player);
                int chestsSpent = purchaseChestBefore - itemCount(client, "minecraft:chest");
                int ironSpent = purchaseIronBlocksBefore - itemCount(client, "minecraft:iron_block");
                int paperSpent = purchasePaperBefore - itemCount(client, "minecraft:paper");
                if (xpSpent != 1100 || chestsSpent != 8 || ironSpent != 4 || paperSpent != 64) {
                    failJourney("one-click server purchase did not debit the expected existing costs"); return;
                }
                queueUiScreenshot("research-purchase-state.png");
                uiJourneyStage = 5;
                MoreMinecrafts.LOGGER.info("CLIENT_UI_ONE_CLICK_PURCHASE_VERIFIED result=PURCHASED xp=1100 chest=8 iron_blocks=4 paper=64");
            }
            case 5 -> {
                if (!takeCapture()) return;
                if (!(client.gui.screen() instanceof ProgressionBrowseScreen browse)) return;
                browse.onClose();
                if (!(client.gui.screen() instanceof ProgressionDisciplineSelectionScreen hub)
                        || !hub.validationOpenSector("combat")
                        || !(client.gui.screen() instanceof ProgressionBrowseScreen combat)
                        || !combat.validationHoverNode("combat_root")) {
                    failJourney("locked research tooltip target missing"); return;
                }
                queueUiScreenshot("research-unavailable-tooltip.png");
                uiJourneyStage = 6;
            }
            case 6 -> {
                if (!takeCapture()) return;
                if (!(client.gui.screen() instanceof ProgressionBrowseScreen combat)) return;
                String previousAction = ProgressionClientApi.lastAction();
                int previousResult = ProgressionClientApi.lastActionResult();
                int xpBefore = dev.itzgamesgd.moreminecrafts.progression.RawXp.available(client.player);
                combat.validationClickNode("combat_root");
                if (!previousAction.equals(ProgressionClientApi.lastAction())
                        || previousResult != ProgressionClientApi.lastActionResult()
                        || xpBefore != dev.itzgamesgd.moreminecrafts.progression.RawXp.available(client.player)) {
                    failJourney("ineligible research click submitted or spent resources"); return;
                }
                combat.onClose();
                if (client.gui.screen() instanceof ProgressionDisciplineSelectionScreen hub) hub.onClose();
                if (!(client.gui.screen() instanceof ProgressionSelectionScreen selector)
                        || !selector.validationClick("xp_quest")
                        || !(client.gui.screen() instanceof ProgressionBrowseScreen quests)
                        || quests.mode() != ProgressionBrowseScreen.Mode.QUEST
                        || !quests.validationTreeReady()
                        || quests.validationQuestTargetCount("xpq.cmb.bestiary_25") != 37
                        || quests.validationQuestGroupCount("xpq.cmb.habitats_5") != 5) {
                    failJourney("XP Quest selector did not open the three-node tree with all 37 bestiary targets"); return;
                }
                for (String id : QUEST_IDS) {
                    String text = quests.validationQuestTooltipText(id);
                    if (!quests.validationQuestNamesResolved(id) || text.contains("moreminecrafts:") || text.contains("minecraft:")
                            || text.contains(" (") && text.contains("_")) {
                        failJourney("quest " + id + " shows an unresolved or raw entity name"); return;
                    }
                }
                MoreMinecrafts.LOGGER.info("QUEST_NAMES_RESOLVED quests=3 bestiary_targets=37 lang={} raw_ids=0",
                        client.getLanguageManager().getSelected());
                if (!quests.validationAllNodesVisible()) {
                    failJourney("the compact quest tree does not fit the viewport"); return;
                }
                MoreMinecrafts.LOGGER.info("QUEST_TREE_LAYOUT gui_scale={} nodes=3 min_spacing_px={} all_visible=true",
                        client.getWindow().getGuiScale(), quests.validationMinNodeSpacingPx());
                double fill = quests.validationContentFill();
                MoreMinecrafts.LOGGER.info("QUEST_CANVAS_VIEW gui_scale={} zoom={} content_fill_ratio={}",
                        client.getWindow().getGuiScale(), String.format("%.2f", quests.validationZoomLevel()),
                        String.format("%.2f", fill));
                if (fill < 0.85) { failJourney("the initial quest view leaves the canvas under-filled (fill=" + fill + ")"); return; }
                quests.validationClearHover();
                queueUiScreenshot("xp-quest-tree-compact.png");
                uiJourneyStage = 58;
            }
            case 58 -> {
                if (!takeCapture()) return;
                if (!(client.gui.screen() instanceof ProgressionBrowseScreen quests)) { failJourney("quest screen closed"); return; }
                double before = quests.validationZoomLevel();
                quests.validationZoom(-4);
                if (quests.validationZoomLevel() >= before) { failJourney("zooming out did not change the quest canvas zoom"); return; }
                queueUiScreenshot("xp-quest-tree-zoomed-out.png");
                uiJourneyStage = 59;
            }
            case 59 -> {
                if (!takeCapture()) return;
                if (!(client.gui.screen() instanceof ProgressionBrowseScreen quests)) { failJourney("quest screen closed"); return; }
                quests.validationResetCamera();
                if (!quests.validationHoverNode("xpq.cmb.cave_dwellers")) {
                    failJourney("cave dwellers node was unavailable"); return;
                }
                queueUiScreenshot("xp-quest-tooltip-cave.png");
                uiJourneyStage = 60;
            }
            case 60 -> {
                if (!takeCapture()) return;
                if (!(client.gui.screen() instanceof ProgressionBrowseScreen quests)
                        || !checkQuestTooltip(client, quests, "cave_dwellers")
                        || !quests.validationHoverNode("xpq.cmb.bestiary_25")) {
                    failJourney("cave dwellers tooltip failed bounds/overlap checks"); return;
                }
                queueUiScreenshot("xp-quest-bestiary-tooltip.png");
                uiJourneyStage = 61;
            }
            case 61 -> {
                if (!takeCapture()) return;
                if (!(client.gui.screen() instanceof ProgressionBrowseScreen quests)
                        || !checkQuestTooltip(client, quests, "bestiary_25")) {
                    failJourney("bestiary tooltip failed bounds/overlap checks"); return;
                }
                quests.validationScrollQuestTooltip(12);
                if (quests.validationTooltipScroll() <= 0) {
                    failJourney("the full bestiary target list did not enable internal tooltip scrolling"); return;
                }
                int scrollBefore = quests.validationTooltipScroll();
                quests.validationKey(InputConstants.KEY_DOWN, false);
                if (quests.validationTooltipScroll() != scrollBefore) { failJourney("plain Down arrow scrolled the tooltip in production"); return; }
                quests.validationKey(InputConstants.KEY_DOWN, true);
                if (quests.validationTooltipScroll() <= scrollBefore) { failJourney("Shift+Down did not scroll the tooltip in production"); return; }
                MoreMinecrafts.LOGGER.info("PRODUCTION_KEYBOARD plain_arrow_scrolls=false shift_arrow_scrolls=true");
                queueUiScreenshot("xp-quest-bestiary-scrolled.png");
                uiJourneyStage = 62;
            }
            case 62 -> {
                if (!takeCapture()) return;
                if (!(client.gui.screen() instanceof ProgressionBrowseScreen quests)
                        || !quests.validationHoverNode("xpq.cmb.habitats_5")) {
                    failJourney("habitat quest tooltip target was unavailable"); return;
                }
                if (quests.validationTooltipScroll() != 0) { /* scroll resets per quest on the next render */ }
                queueUiScreenshot("xp-quest-habitats-tooltip.png");
                uiJourneyStage = 63;
            }
            case 63 -> {
                if (!takeCapture()) return;
                if (!(client.gui.screen() instanceof ProgressionBrowseScreen quests)
                        || quests.validationQuestGroupCount("xpq.cmb.habitats_5") != 5
                        || !checkQuestTooltip(client, quests, "habitats_5")
                        || quests.validationTooltipScroll() != 0
                        || !quests.validationHoverNode("xpq.cmb.bestiary_25")) {
                    failJourney("habitat tooltip failed checks or kept the previous quest's scroll"); return;
                }
                questEdgeIndex = 0;
                quests.validationPlaceNodeAtEdge(QUEST_EDGES[0]);
                queueUiScreenshot("xp-quest-tooltip-edge-" + QUEST_EDGES[0] + ".png");
                uiJourneyStage = 64;
            }
            case 64 -> {
                if (!takeCapture()) return;
                if (!(client.gui.screen() instanceof ProgressionBrowseScreen quests)
                        || !checkQuestTooltip(client, quests, "edge_" + QUEST_EDGES[questEdgeIndex])) {
                    failJourney("tooltip failed bounds/overlap checks at the " + QUEST_EDGES[questEdgeIndex] + " edge"); return;
                }
                if (++questEdgeIndex < QUEST_EDGES.length) {
                    quests.validationPlaceNodeAtEdge(QUEST_EDGES[questEdgeIndex]);
                    queueUiScreenshot("xp-quest-tooltip-edge-" + QUEST_EDGES[questEdgeIndex] + ".png");
                    return;
                }
                quests.validationResetCamera();
                quests.validationClearHover();
                String before = quests.validationSelectedId();
                quests.keyPressed(new KeyEvent(InputConstants.KEY_RIGHT, 0, 0));
                if (before.equals(quests.validationSelectedId())) {
                    failJourney("Right arrow did not move quest focus"); return;
                }
                // Let a frame render so the tooltip bounds used by the click are current, not the previous test's.
                queueUiScreenshot("xp-quest-keyboard-focus-tooltip.png");
                uiJourneyStage = 66;
            }
            case 66 -> {
                if (!takeCapture()) return;
                if (!(client.gui.screen() instanceof ProgressionBrowseScreen quests)
                        || !checkQuestTooltip(client, quests, "keyboard_focus")) {
                    failJourney("keyboard-focus tooltip failed checks"); return;
                }
                if (!quests.validationClickNode("xpq.cmb.habitats_5") || !"xpq.cmb.habitats_5".equals(quests.validationSelectedId())) {
                    failJourney("clicking the habitats node did not select it (selected=" + quests.validationSelectedId()
                            + " tooltip=" + quests.validationTooltipGeometry() + ")"); return;
                }
                MoreMinecrafts.LOGGER.info("QUEST_INTERACTION_VERIFIED keyboard_focus=true click_select=true scroll_reset=true");
                queueUiScreenshot("xp-quest-selected-tooltip.png");
                uiJourneyStage = 65;
            }
            case 65 -> {
                if (!takeCapture()) return;
                if (!(client.gui.screen() instanceof ProgressionBrowseScreen quests)
                        || !checkQuestTooltip(client, quests, "selected_habitats")) {
                    failJourney("selected-node tooltip failed checks"); return;
                }
                previewRowsBefore = ProgressionClientApi.quests(null).size();
                uiJourneyStage = 80;
            }
            case 80 -> {
                structurePreview = QuestStructurePreview.load(client.getLanguageManager().getSelected());
                if (structurePreview == null) {
                    MoreMinecrafts.LOGGER.info("STRUCTURE_PREVIEW_SKIPPED reason=not_configured");
                    uiJourneyStage = 99;
                    return;
                }
                var screen = ProgressionBrowseScreen.forStructurePreview(structurePreview);
                client.gui.setScreen(screen);
                if (!(client.gui.screen() instanceof ProgressionBrowseScreen quests) || !quests.validationIsPreview()
                        || quests.validationNodeStats()[0] != 200 || quests.validationChapterCount() != 15
                        || quests.validationPreviewSubBranches() != 52) {
                    failJourney("structure preview did not load 200 nodes, 15 chapters and 52 sub-branches"); return;
                }
                MoreMinecrafts.LOGGER.info("PREVIEW_OPEN nodes=200 chapters=15 sub_branches=52 lang={} gui_scale={}",
                        client.getLanguageManager().getSelected(), client.getWindow().getGuiScale());
                int iconsMissing = 0, iconsChecked = 0;
                for (String icon : structurePreview.iconItemIds()) {
                    iconsChecked++;
                    var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(icon));
                    if (item == null || item == net.minecraft.world.item.Items.AIR) iconsMissing++;
                }
                MoreMinecrafts.LOGGER.info("QUEST_ICONS checked={} missing_from_registry={}", iconsChecked, iconsMissing);
                if (iconsChecked != 200 || iconsMissing != 0) { failJourney("quest icons: " + iconsMissing + " missing of " + iconsChecked); return; }
                previewSteps = previewSteps();
                previewStepIndex = 0;
                previewPendingName = null;
                quests.validationResetRenderStats();
                uiJourneyStage = 81;
            }
            case 81 -> {
                if (previewPendingName != null && !takeCapture()) return;
                if (!(client.gui.screen() instanceof ProgressionBrowseScreen quests) || !quests.validationIsPreview()) {
                    failJourney("structure preview screen was closed"); return;
                }
                if (previewPendingName != null) {
                    if (!logPreviewView(client, quests, previewPendingName)) return;
                }
                if (previewStepIndex >= previewSteps.size()) {
                    client.gui.setScreen(null);
                    int rowsAfter = ProgressionClientApi.quests(null).size();
                    long registered = dev.itzgamesgd.moreminecrafts.progression.XpQuestCatalogData.catalog().quests().stream()
                            .filter(q -> q.registered()).count();
                    MoreMinecrafts.LOGGER.info("PREVIEW_ISOLATION client_quest_rows_before={} after={} catalog_registered={} claims_sent=0",
                            previewRowsBefore, rowsAfter, registered);
                    if (previewRowsBefore != rowsAfter) { failJourney("preview changed the client quest rows"); return; }
                    uiJourneyStage = 99;
                    return;
                }
                PreviewStep step = previewSteps.get(previewStepIndex++);
                quests.validationResetRenderStats();
                String failure = step.apply().apply(quests);
                if (failure != null) { failJourney("preview step " + step.name() + ": " + failure); return; }
                previewPendingName = step.name();
                queueUiScreenshot(step.name() + ".png");
            }
            case 99 -> {
                client.gui.setScreen(null);
                var inventory = client.player.getInventory();
                var menu = new AutoFarmMenu(1, inventory, new SimpleContainer(50));
                client.gui.setScreen(new AutoFarmScreen(menu, inventory, Component.translatable("container.moreminecrafts.auto_farm")));
                queueUiScreenshot("auto-farm-screen.png");
                uiJourneyStage = 7;
            }
            case 7 -> {
                if (!takeCapture()) return;
                MoreMinecrafts.LOGGER.info("CLIENT_INTERACTION_PATH synthetic Screen callbacks: selector -> both sector menus -> node hover -> one-click server purchase");
                MoreMinecrafts.LOGGER.info("CLIENT_INTERACTION_PATH auto_farm=render_only_synthetic_empty_container");
                uiJourneyStage = 8;
            }
            default -> { }
        }
    }

    private static final String[] QUEST_IDS = {"xpq.cmb.cave_dwellers", "xpq.cmb.bestiary_25", "xpq.cmb.habitats_5"};
    private static final String[] QUEST_EDGES = {"left", "right", "top", "bottom"};
    private static int questEdgeIndex;

    private static boolean checkQuestTooltip(Minecraft client, ProgressionBrowseScreen quests, String label) {
        String geometry = quests.validationTooltipGeometry();
        boolean within = quests.validationTooltipWithinScreen();
        boolean clears = quests.validationTooltipClearsNode();
        MoreMinecrafts.LOGGER.info("QUEST_TOOLTIP label={} gui_scale={} screen={}x{} geometry(x,y,w,h;clearsNode)={} within_screen={} scroll={}",
                label, client.getWindow().getGuiScale(), client.getWindow().getGuiScaledWidth(),
                client.getWindow().getGuiScaledHeight(), geometry, within, quests.validationTooltipScroll());
        return within && clears;
    }

    private static QuestStructurePreview structurePreview;
    private static List<PreviewStep> previewSteps;
    private static int previewStepIndex;
    private static String previewPendingName;
    private static int previewRowsBefore;

    private record PreviewStep(String name, java.util.function.Function<ProgressionBrowseScreen, String> apply) { }

    private static double[] laneCentre(ProgressionBrowseScreen q, String chapter) {
        String[] parts = q.validationLaneCenter(chapter).split(",");
        return new double[] {Double.parseDouble(parts[0]), Double.parseDouble(parts[1])};
    }

    private static java.util.function.Function<ProgressionBrowseScreen, String> viewLane(String chapter, double zoom, int hoverIndex) {
        return q -> {
            double[] c = laneCentre(q, chapter);
            q.validationViewAt(c[0], c[1], zoom);
            if (hoverIndex >= 0) q.validationHoverNode(q.validationQuestIdAt(hoverIndex)); else q.validationClearHover();
            return null;
        };
    }

    private static List<PreviewStep> previewSteps() {
        List<PreviewStep> steps = new java.util.ArrayList<>();
        steps.add(new PreviewStep("structure-preview-initial", q -> null));
        steps.add(new PreviewStep("structure-preview-overview", q -> { q.validationFitAll(); q.validationClearHover(); return null; }));
        steps.add(new PreviewStep("structure-preview-index-card-click", q -> {
            q.validationFitAll();
            q.validationClearHover();
            int[] card = q.validationChapterCardCentre("building");
            if (card == null) return "no chapter card for building at the index level";
            if (!q.validationClickAt(card[0], card[1])) return "chapter card click was not handled";
            return null;
        }));
        steps.add(new PreviewStep("structure-preview-medium-zoom-exploration", viewLane("exploration", 0.36, -1)));
        steps.add(new PreviewStep("structure-preview-mid-zoom", q -> {
            double[] c = laneCentre(q, "mining");
            q.validationViewAt(c[0], c[1], 0.3);
            return null;
        }));
        steps.add(new PreviewStep("structure-preview-dense-exploration", viewLane("exploration", 0.75, -1)));
        steps.add(new PreviewStep("structure-preview-dense-mining", viewLane("mining", 0.75, -1)));
        steps.add(new PreviewStep("structure-preview-sparse-ecology", viewLane("ecology", 0.75, -1)));
        steps.add(new PreviewStep("structure-preview-zoomed-in", viewLane("exploration", 1.5, -1)));
        steps.add(new PreviewStep("structure-preview-pan-farming", viewLane("farming", 0.75, -1)));
        steps.add(new PreviewStep("structure-preview-pan-bosses", q -> {
            double[] c = laneCentre(q, "bosses");
            q.validationViewAt(c[0], c[1], 0.75);
            // Drag far in each direction. The clamp keeps the view centre inside the canvas (a 32 px strip of canvas
            // always stays on screen, so the ends can show no nodes; that is expected, not a layout fault).
            q.validationPan(0, -100000);
            String low = panOverlapViolation(q);
            if (low != null) return low;
            q.validationPan(0, 100000);
            return panOverlapViolation(q);
        }));
        steps.add(new PreviewStep("structure-preview-tooltip-long-label", viewLane("exploration", 0.75, 3)));
        steps.add(new PreviewStep("structure-preview-tooltip-groups", viewLane("exploration", 0.75, 11)));
        steps.add(new PreviewStep("structure-preview-tooltip-scroll-list", viewLane("exploration", 0.75, 17)));
        steps.add(new PreviewStep("structure-preview-tooltip-scrolled", q -> {
            q.validationScrollQuestTooltip(15);
            return q.validationTooltipScroll() > 0 ? null : "the 40-entry list did not scroll";
        }));
        for (String edge : new String[] {"left", "right", "top", "bottom"}) {
            steps.add(new PreviewStep("structure-preview-tooltip-edge-" + edge, q -> {
                double[] c = laneCentre(q, "exploration");
                q.validationViewAt(c[0], c[1], 0.75);
                q.validationHoverNode(q.validationQuestIdAt(17));
                q.validationPlaceNodeAtEdge(edge);
                return null;
            }));
        }
        steps.add(new PreviewStep("structure-preview-keyboard-navigation", q -> {
            q.validationClearHover();
            double[] c = laneCentre(q, "exploration");
            q.validationViewAt(c[0], c[1], 0.75);
            if (!q.validationClickNode(q.validationQuestIdAt(3))) return "could not select the starting node";
            StringBuilder path = new StringBuilder(q.validationSelectedId());
            int[] keys = {InputConstants.KEY_RIGHT, InputConstants.KEY_RIGHT, InputConstants.KEY_DOWN, InputConstants.KEY_DOWN,
                    InputConstants.KEY_LEFT, InputConstants.KEY_UP};
            String[] names = {"Right", "Right", "Down", "Down", "Left", "Up"};
            int moved = 0;
            for (int i = 0; i < keys.length; i++) {
                String before = q.validationSelectedId();
                q.keyPressed(new KeyEvent(keys[i], 0, 0));
                boolean changed = !before.equals(q.validationSelectedId());
                if (changed) moved++;
                path.append(" -").append(names[i]).append(changed ? "-> " : "(no move)-> ").append(q.validationSelectedId());
                if (!q.validationSelectedInViewport()) return "selected node left the viewport after " + names[i];
            }
            MoreMinecrafts.LOGGER.info("PREVIEW_KEYBOARD moved={}/{} path={}", moved, keys.length, path);
            return moved >= 4 ? null : "keyboard navigation moved only " + moved + " of " + keys.length + " keys";
        }));
        steps.add(new PreviewStep("structure-preview-mouse-prepare-far-node", q -> {
            double[] c = laneCentre(q, "bosses");
            q.validationViewAt(c[0], c[1], 0.75);
            return null; // the tooltip left open by the keyboard step stays visible so its effect is captured
        }));
        steps.add(new PreviewStep("structure-preview-mouse-click-far-node", q -> {
            String target = q.validationQuestIdAt(199);
            boolean blocked = q.validationNodeUnderTooltip(target);
            boolean dismissed = false;
            if (blocked) dismissed = q.validationClickEmpty();
            boolean stillBlocked = q.validationNodeUnderTooltip(target);
            boolean clicked = q.validationClickNode(target);
            MoreMinecrafts.LOGGER.info("PREVIEW_MOUSE far_node_index=199 blocked_by_open_tooltip={} dismissed_with_empty_click={} still_blocked={} selected={}",
                    blocked, dismissed, stillBlocked, target.equals(q.validationSelectedId()));
            return clicked && target.equals(q.validationSelectedId()) ? null : "clicking the last node did not select it";
        }));
        steps.add(new PreviewStep("structure-preview-regression-tooltip-open", viewLane("exploration", 0.75, 17)));
        steps.add(new PreviewStep("structure-preview-regression-stale-tooltip", q -> {
            q.validationClearHover();
            // Click genuinely empty canvas. The top-left corner can sit inside the open tooltip, where a click is
            // correctly absorbed by the tooltip rather than dismissing it.
            int[] empty = q.validationEmptyPoint();
            if (empty == null) return "no empty canvas point available";
            q.validationClickAt(empty[0], empty[1]);
            return null;
        }));
        steps.add(new PreviewStep("structure-preview-regression-stale-click", q -> {
            q.validationClearHover();
            String target = q.validationQuestIdAt(17);
            if (!q.validationClickNode(target) || !target.equals(q.validationSelectedId()))
                return "a stale tooltip rectangle swallowed a click on a visible node";
            return null;
        }));
        steps.add(new PreviewStep("structure-preview-regression-keyboard-tooltip", q -> {
            q.validationClearHover();
            q.validationKey(InputConstants.KEY_RIGHT, false);
            return null;
        }));
        steps.add(new PreviewStep("structure-preview-zoom-roundtrip", q -> {
            double[] c = laneCentre(q, "farming");
            q.validationViewAt(c[0], c[1], 0.75);
            double[] before = q.validationCentreWorld();
            double zoomBefore = q.validationZoomLevel();
            q.validationZoom(3);
            q.validationZoom(-3);
            double[] after = q.validationCentreWorld();
            double drift = Math.hypot(before[0] - after[0], before[1] - after[1]);
            MoreMinecrafts.LOGGER.info("PREVIEW_ZOOM_STABILITY zoom_before={} zoom_after={} centre_drift_world_units={}",
                    String.format("%.3f", zoomBefore), String.format("%.3f", q.validationZoomLevel()), String.format("%.2f", drift));
            return drift <= 2.0 && Math.abs(zoomBefore - q.validationZoomLevel()) < 0.001 ? null : "zoom round trip drifted by " + drift;
        }));
        return steps;
    }

    /** clampPan keeps the canvas overlapping the viewport by at least MIN_VISIBLE_PIXELS in world units at both ends. */
    private static String panOverlapViolation(ProgressionBrowseScreen q) {
        double zoom = q.validationZoomValue();
        double half = q.validationViewportHeight() / (2.0 * zoom);
        double viewTop = q.validationCentreWorld()[1] - half;
        double viewBottom = viewTop + 2.0 * half;
        double minVisible = ProgressionCanvasGeometry.MIN_VISIBLE_PIXELS / zoom;
        double canvasHeight = q.validationCanvasHeight();
        if (viewTop > canvasHeight - minVisible + 0.5 || viewBottom < minVisible - 0.5)
            return "pan left less than " + ProgressionCanvasGeometry.MIN_VISIBLE_PIXELS + " px of the canvas on screen: view "
                    + viewTop + ".." + viewBottom + " over canvas 0.." + canvasHeight;
        return null;
    }

    private static boolean logPreviewView(Minecraft client, ProgressionBrowseScreen q, String name) {
        int[] n = q.validationNodeStats();
        String geometry = q.validationTooltipGeometry();
        boolean tooltip = !geometry.equals("none") && q.validationTooltipWithinScreen();
        int[] lod = q.validationLargeMetrics();
        int overlapping = q.validationOverlappingFrames();
        String level = q.validationLevel();
        MoreMinecrafts.LOGGER.info("PREVIEW_LOD name={} level={} zoom={} overlapping_frames={} label_overflows={} label_collisions={} labels_culled={} caption_collisions={} captions={} index_cards={} index_overflows={} markers={} quest_nodes={} tooltip_visible={}",
                name, level, String.format("%.3f", q.validationZoomValue()), overlapping, lod[0], lod[1], lod[2], lod[4], lod[3],
                lod[8], lod[5], lod[6], lod[7], q.validationTooltipVisible());
        if (!level.equals("CHAPTER_INDEX") && overlapping != 0) {
            failJourney("overlapping node frames at " + level + " in " + name + ": " + overlapping); return false;
        }
        if (lod[1] != 0) { failJourney("chapter labels collide in " + name); return false; }
        if (lod[4] != 0) { failJourney("sub-branch captions collide with markers or captions in " + name); return false; }
        if (name.endsWith("overview") && !level.equals("CHAPTER_INDEX")) { failJourney("overview did not use the chapter index"); return false; }
        if (name.contains("medium-zoom") && !level.equals("SUB_BRANCH")) { failJourney("medium zoom did not show sub-branches"); return false; }
        if (name.equals("structure-preview-zoomed-in") && !level.equals("QUEST")) { failJourney("close zoom did not show quest nodes"); return false; }
        if (name.equals("structure-preview-index-card-click") && !level.equals("SUB_BRANCH")) { failJourney("choosing a chapter card did not zoom into its sub-branches"); return false; }
        if (name.equals("structure-preview-regression-tooltip-open") && !q.validationTooltipVisible()) { failJourney("regression: keyboard/hover tooltip did not open"); return false; }
        if (name.equals("structure-preview-regression-stale-tooltip") && q.validationTooltipVisible()) { failJourney("regression: tooltip stayed open after an empty click"); return false; }
        if (name.equals("structure-preview-regression-keyboard-tooltip") && !q.validationTooltipVisible()) { failJourney("regression: keyboard focus did not open the tooltip"); return false; }
        MoreMinecrafts.LOGGER.info("PREVIEW_VIEW name={} gui_scale={} lang={} zoom={} nodes_total={} nodes_visible={} nodes_clipped={} overlapping_pairs={} tooltip_covered_nodes={} tooltip_in_viewport={} geometry(x,y,w,h;clears)={} render[{}]",
                name, client.getWindow().getGuiScale(), client.getLanguageManager().getSelected(),
                String.format("%.2f", q.validationZoomLevel()), n[0], n[1], n[2], n[3], q.validationTooltipCoveredNodes(),
                tooltip, geometry, q.validationRenderStats());
        if (name.contains("tooltip-edge") && !tooltip) {
            failJourney("tooltip left the viewport at " + name); return false;
        }
        return true;
    }

    private static boolean takeCapture() {
        if (!uiCaptureComplete) return false;
        uiCaptureComplete = false;
        return true;
    }

    private static void failJourney(String reason) {
        uiJourneyFailed = true;
        MoreMinecrafts.LOGGER.error("CLIENT_UI_INTERACTION_FAILED {}", reason);
    }

    private static void advancePhysicalSelectorTest(Minecraft client) {
        Screen screen = client.gui.screen();
        switch (physicalSelectorStage) {
            case 1 -> {
                if (screen instanceof ProgressionDisciplineSelectionScreen hub
                        && hub.mode() == ProgressionBrowseScreen.Mode.KNOWLEDGE) {
                    verifyPhysicalSelectorTarget("knowledge");
                    openSelectorForPhysicalTest(client, 2, "research");
                }
            }
            case 2 -> {
                if (screen instanceof ProgressionDisciplineSelectionScreen hub
                        && hub.mode() == ProgressionBrowseScreen.Mode.RESEARCH) {
                    verifyPhysicalSelectorTarget("research");
                    openSelectorForPhysicalTest(client, 3, "advancements");
                }
            }
            case 3 -> {
                if (screen instanceof net.minecraft.client.gui.screens.advancements.AdvancementsScreen) {
                    verifyPhysicalSelectorTarget("advancements");
                    openSelectorForPhysicalTest(client, 4, "xp_quest");
                }
            }
            case 4 -> {
                if (screen instanceof ProgressionBrowseScreen browse && browse.mode() == ProgressionBrowseScreen.Mode.QUEST
                        && "xp_quest".equals(ProgressionSelectionScreen.lastClickedTarget())) {
                    verifyPhysicalSelectorTarget("xp_quest");
                    openSelectorForPhysicalTest(client, 5, "close");
                }
            }
            case 5 -> {
                if (screen == null && "close".equals(ProgressionSelectionScreen.lastClickedTarget())) {
                    verifyPhysicalSelectorTarget("close");
                    physicalSelectorDone = true;
                    client.gui.setScreen(new ProgressionSelectionScreen());
                    uiCaptureComplete = true;
                    MoreMinecrafts.LOGGER.info("SELECTOR_PHYSICAL_TEST_COMPLETE gui_scale={}", client.getWindow().getGuiScale());
                }
            }
            default -> { }
        }
    }

    private static void openSelectorForPhysicalTest(Minecraft client, int nextStage, String target) {
        physicalSelectorStage = nextStage;
        client.gui.setScreen(new ProgressionSelectionScreen());
        announcePhysicalSelectorTarget(client, target);
    }

    private static void announcePhysicalSelectorTarget(Minecraft client, String target) {
        if (!(client.gui.screen() instanceof ProgressionSelectionScreen selector)) {
            uiJourneyFailed = true;
            MoreMinecrafts.LOGGER.error("SELECTOR_PHYSICAL_TEST_FAILED expected_selector target={}", target);
            return;
        }
        MoreMinecrafts.LOGGER.info("SELECTOR_PHYSICAL_READY target={} x={} y={} gui_scale={} resolution={}x{}",
                target, selector.validationPhysicalX(target), selector.validationPhysicalY(target),
                client.getWindow().getGuiScale(), client.getWindow().getWidth(), client.getWindow().getHeight());
    }

    private static void verifyPhysicalSelectorTarget(String target) {
        if (!target.equals(ProgressionSelectionScreen.lastClickedTarget())) {
            uiJourneyFailed = true;
            MoreMinecrafts.LOGGER.error("SELECTOR_PHYSICAL_TEST_FAILED target={} actual={}", target,
                    ProgressionSelectionScreen.lastClickedTarget());
            return;
        }
        MoreMinecrafts.LOGGER.info("SELECTOR_PHYSICAL_VERIFIED target={} input=xdotool", target);
    }

    private static void clickSelectionChoice(Minecraft client, int choice) {
        if (!(client.gui.screen() instanceof ProgressionSelectionScreen selector)
                || !selector.validationClick(choice == 2 ? "knowledge" : "research")) {
            uiJourneyFailed = true;
            MoreMinecrafts.LOGGER.error("CLIENT_UI_INTERACTION_FAILED selector choice={} was not hit", choice);
            return;
        }
        if (choice == 2 || choice == 3) {
            if (!(client.gui.screen() instanceof ProgressionDisciplineSelectionScreen hub)
                    || hub.mode() != (choice == 2 ? ProgressionBrowseScreen.Mode.KNOWLEDGE : ProgressionBrowseScreen.Mode.RESEARCH)) {
                uiJourneyFailed = true;
                MoreMinecrafts.LOGGER.error("CLIENT_UI_INTERACTION_FAILED selector choice={} did not open expected screen", choice);
            }
        }
    }

    private static int itemCount(Minecraft client, String itemId) {
        var item = BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(itemId));
        int total = 0;
        for (int slot = 0; slot < client.player.getInventory().getContainerSize(); slot++) {
            var stack = client.player.getInventory().getItem(slot);
            if (!stack.isEmpty() && stack.getItem() == item) total += stack.getCount();
        }
        return total;
    }

    private static void queueUiScreenshot(String name) {
        uiCaptureName = name;
        uiFrames = 0;
    }

    private static void captureUiScreenshot(Minecraft client) {
        String name = uiCaptureName;
        uiCaptureName = null;
        Path screenshot = client.gameDirectory.toPath().resolve("screenshots").resolve(name);
        try {
            Files.createDirectories(screenshot.getParent());
            Screenshot.grab(client.gameDirectory, name, client.gameRenderer.mainRenderTarget(), 1, message -> {
                if (!Files.isRegularFile(screenshot)) {
                    uiJourneyFailed = true;
                    MoreMinecrafts.LOGGER.error("CLIENT_UI_SCREENSHOT_FAILED name={} callback={}", name, message.getString());
                    return;
                }
                uiCaptureComplete = true;
                MoreMinecrafts.LOGGER.info("CLIENT_UI_SCREENSHOT_WRITTEN name={}", name);
            });
        } catch (Exception exception) {
            uiJourneyFailed = true;
            MoreMinecrafts.LOGGER.error("CLIENT_UI_SCREENSHOT_FAILED name={}", name, exception);
        }
    }

    private static void requestNativeScreenshot(Minecraft client) {
        captureRequested = true;
        Path screenshot = client.gameDirectory.toPath().resolve("screenshots").resolve(SCREENSHOT_NAME);
        MoreMinecrafts.LOGGER.info(
                "VISUAL_CAPTURE_REQUESTED path={} source=Minecraft.mainRenderTarget frame_milestone=HudElementRegistry.after(CHAT)",
                screenshot.toAbsolutePath()
        );

        try {
            Files.deleteIfExists(screenshot);
            Screenshot.grab(
                    client.gameDirectory,
                    SCREENSHOT_NAME,
                    client.gameRenderer.mainRenderTarget(),
                    1,
                    message -> {
                        try {
                            if (!Files.isRegularFile(screenshot)) {
                                failCapture("Minecraft screenshot callback completed without a non-empty PNG: "
                                        + message.getString());
                                return;
                            }
                            captureFinished = true;
                            MoreMinecrafts.LOGGER.info(
                                    "VISUAL_CAPTURE_WRITTEN path={} bytes={} callback={}",
                                    screenshot.toAbsolutePath(),
                                    Files.size(screenshot),
                                    message.getString()
                            );
                        } catch (Exception exception) {
                            failCapture("could not verify screenshot output: " + exception);
                        }
                    }
            );
        } catch (Exception exception) {
            failCapture("Minecraft Screenshot.grab threw: " + exception);
        }
    }

    private static void failCapture(String reason) {
        if (captureFinished) {
            return;
        }
        captureFinished = true;
        MoreMinecrafts.LOGGER.error("VISUAL_CAPTURE_FAILED reason={}", reason);
    }

    private static void connectToTestServer(Minecraft client) {
        String address = System.getenv(SERVER_ENV);
        if (address == null || address.isBlank()) {
            MoreMinecrafts.LOGGER.error("Visual-validation server address missing from {}", SERVER_ENV);
            return;
        }

        ServerData serverData = new ServerData(
                "MoreMinecrafts Visual Validation",
                address,
                ServerData.Type.OTHER
        );
        ConnectScreen.startConnecting(
                client.gui.screen(),
                client,
                ServerAddress.parseString(address),
                serverData,
                false,
                null
        );
        connectionRequested = true;
        MoreMinecrafts.LOGGER.info("Visual-validation client connecting to local test server");
    }
}

