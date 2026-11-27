package dev.itzgamesgd.moreminecrafts.client;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.progression.AutoFarmMenu;
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
import java.nio.file.Path;

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
            uiJourneyStage = 1;
            return;
        }
        if (PHYSICAL_SELECTOR_TEST && physicalSelectorStage > 0 && !physicalSelectorDone) {
            advancePhysicalSelectorTest(client);
            if (!physicalSelectorDone) return;
        }
        if (uiCaptureName != null) return;
        switch (uiJourneyStage) {
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
                if (screen instanceof ProgressionSelectionScreen
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

