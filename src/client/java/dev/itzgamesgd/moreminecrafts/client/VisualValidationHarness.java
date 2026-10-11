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
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
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
    private static int purchaseResearchIndex = -1;
    private static String previousActionBeforeCancel;
    private static int previousActionResultBeforeCancel;
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
     * Opt-in synthetic widget interaction for headless client validation. It runs the same
     * Screen click callbacks as a mouse input and sends the final purchase through the normal
     * client/server payload; production clients never register this route.
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
            MoreMinecrafts.LOGGER.info("CLIENT_UI_ICON_MAPPING_VALIDATED count=157 fallback=minecraft:book");
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
                if (!uiCaptureComplete) return;
                uiCaptureComplete = false;
                if (PHYSICAL_SELECTOR_TEST && physicalSelectorStage == 0) {
                    physicalSelectorStage = 1;
                    announcePhysicalSelectorTarget(client, "knowledge");
                    return;
                }
                clickSelectionChoice(client, 2);
                if (!(client.gui.screen() instanceof ProgressionBrowseScreen browse)
                        || !browse.validationVisitAllDisciplines()
                        || !browse.validationExerciseCanvasControls()
                        || !browse.validationKeyboardNodeFocus()) {
                    uiJourneyFailed = true;
                    MoreMinecrafts.LOGGER.error("CLIENT_UI_NAVIGATION_FAILED Knowledge discipline tabs, branch navigation, drag pan, wheel zoom, or keyboard focus");
                    return;
                }
                if (browse.validationCompact()) {
                    browse.validationOpenDetails();
                    queueUiScreenshot("knowledge-compact-details.png");
                    uiJourneyStage = 11;
                } else {
                    queueUiScreenshot("knowledge-screen.png");
                    uiJourneyStage = 2;
                }
            }
            case 11 -> {
                if (!uiCaptureComplete) return;
                uiCaptureComplete = false;
                if (client.gui.screen() instanceof ProgressionBrowseScreen browse) browse.validationCloseDetails();
                queueUiScreenshot("knowledge-screen.png");
                uiJourneyStage = 2;
            }
            case 2 -> {
                if (!uiCaptureComplete) return;
                uiCaptureComplete = false;
                clickProgressionBack(client);
                clickSelectionChoice(client, 3);
                queueUiScreenshot("research-screen.png");
                uiJourneyStage = 3;
            }
            case 3 -> {
                if (!uiCaptureComplete) return;
                uiCaptureComplete = false;
                if (!(client.gui.screen() instanceof ProgressionBrowseScreen initialResearch)
                        || !initialResearch.validationVisitAllDisciplines()
                        || !initialResearch.validationExerciseCanvasControls()
                        || !initialResearch.validationKeyboardNodeFocus()) {
                    uiJourneyFailed = true;
                    MoreMinecrafts.LOGGER.error("CLIENT_UI_NAVIGATION_FAILED Research discipline tabs, branch navigation, drag pan, wheel zoom, or keyboard focus");
                    return;
                }
                clickDiscipline(client, "생산공학");
                var rows = ProgressionClientApi.research("production");
                purchaseResearchIndex = -1;
                for (int index = 0; index < rows.size(); index++) {
                    if (rows.get(index).id().equals("nearby_chest_ingredients")) {
                        purchaseResearchIndex = index;
                        break;
                    }
                }
                if (purchaseResearchIndex < 0) {
                    uiJourneyFailed = true;
                    MoreMinecrafts.LOGGER.error("CLIENT_UI_INTERACTION_FAILED purchasable research entry is missing");
                    return;
                }
                if (!(client.gui.screen() instanceof ProgressionBrowseScreen browse)
                        || !browse.validationClickNode("nearby_chest_ingredients")
                        || !"nearby_chest_ingredients".equals(browse.validationSelectedNodeId())) {
                    uiJourneyFailed = true;
                    MoreMinecrafts.LOGGER.error("CLIENT_UI_INTERACTION_FAILED node canvas did not select target research");
                    return;
                }
                browse.validationFitAll();
                queueUiScreenshot("research-fit-all.png");
                uiJourneyStage = 31;
            }
            case 31 -> {
                if (!uiCaptureComplete) return;
                uiCaptureComplete = false;
                if (!(client.gui.screen() instanceof ProgressionBrowseScreen browse)) return;
                browse.validationFocusSelected();
                if (!browse.validationSelectedCentered()) {
                    uiJourneyFailed = true;
                    MoreMinecrafts.LOGGER.error("CLIENT_UI_INTERACTION_FAILED focus selected did not recenter node");
                    return;
                }
                if (browse.validationCompact()) {
                    browse.validationOpenDetails();
                    queueUiScreenshot("research-compact-details.png");
                    uiJourneyStage = 30;
                } else {
                    queueUiScreenshot("research-purchasable-entry.png");
                    uiJourneyStage = 4;
                }
            }
            case 30 -> {
                if (!uiCaptureComplete) return;
                uiCaptureComplete = false;
                if (client.gui.screen() instanceof ProgressionBrowseScreen browse) browse.validationCloseDetails();
                queueUiScreenshot("research-purchasable-entry.png");
                uiJourneyStage = 4;
            }
            case 4 -> {
                if (!uiCaptureComplete) return;
                uiCaptureComplete = false;
                purchaseXpBefore = dev.itzgamesgd.moreminecrafts.progression.RawXp.available(client.player);
                purchaseChestBefore = itemCount(client, "minecraft:chest");
                purchaseIronBlocksBefore = itemCount(client, "minecraft:iron_block");
                purchasePaperBefore = itemCount(client, "minecraft:paper");
                previousActionBeforeCancel = ProgressionClientApi.lastAction();
                previousActionResultBeforeCancel = ProgressionClientApi.lastActionResult();
                if (client.gui.screen() instanceof ProgressionBrowseScreen browse) {
                    if (!browse.validationActivatePurchaseWithKeyboard()) {
                        uiJourneyFailed = true;
                        MoreMinecrafts.LOGGER.error("CLIENT_UI_KEYBOARD_FAILED focused purchase activation did not open confirmation");
                        return;
                    }
                    if (client.gui.screen() instanceof ResearchPurchaseConfirmScreen) {
                        ResearchPurchaseConfirmScreen confirmation = (ResearchPurchaseConfirmScreen) client.gui.screen();
                        if (confirmation.validationCompact()) confirmation.validationScrollBody();
                        queueUiScreenshot(confirmation.validationCompact()
                                ? "research-purchase-confirmation-compact-scrolled.png"
                                : "research-purchase-confirmation.png");
                        MoreMinecrafts.LOGGER.info("CLIENT_UI_PURCHASE_CONFIRMATION_OPENED id=nearby_chest_ingredients");
                    } else {
                        uiJourneyFailed = true;
                        MoreMinecrafts.LOGGER.error("CLIENT_UI_INTERACTION_FAILED purchase confirmation did not open");
                    }
                } else {
                    uiJourneyFailed = true;
                    MoreMinecrafts.LOGGER.error("CLIENT_UI_INTERACTION_FAILED purchase screen disappeared");
                }
                uiJourneyStage = 5;
            }
            case 5 -> {
                if (!uiCaptureComplete) return;
                uiCaptureComplete = false;
                if (!(client.gui.screen() instanceof ResearchPurchaseConfirmScreen)) {
                    uiJourneyFailed = true;
                    MoreMinecrafts.LOGGER.error("CLIENT_UI_INTERACTION_FAILED confirmation screen disappeared before cancel");
                    return;
                }
                clickWidget(client, 1);
                if (!(client.gui.screen() instanceof ProgressionBrowseScreen)
                        || ProgressionClientApi.lastActionResult() != previousActionResultBeforeCancel
                        || !ProgressionClientApi.lastAction().equals(previousActionBeforeCancel)
                        || purchaseXpBefore != dev.itzgamesgd.moreminecrafts.progression.RawXp.available(client.player)
                        || purchaseChestBefore != itemCount(client, "minecraft:chest")
                        || purchaseIronBlocksBefore != itemCount(client, "minecraft:iron_block")
                        || purchasePaperBefore != itemCount(client, "minecraft:paper")) {
                    uiJourneyFailed = true;
                    MoreMinecrafts.LOGGER.error("CLIENT_UI_PURCHASE_CANCEL_FAILED cancellation changed action or resources");
                    return;
                }
                queueUiScreenshot("research-purchase-cancelled.png");
                MoreMinecrafts.LOGGER.info("CLIENT_UI_PURCHASE_CANCEL_VERIFIED resources_unchanged=true");
                uiJourneyStage = 6;
            }
            case 6 -> {
                if (!uiCaptureComplete) return;
                uiCaptureComplete = false;
                if (client.gui.screen() instanceof ProgressionBrowseScreen researchScreen) {
                    clickWidgetWithLabel(client, "연구 구매", "Purchase research", "구매", "Buy");
                    if (!(client.gui.screen() instanceof ResearchPurchaseConfirmScreen)) {
                        uiJourneyFailed = true;
                        MoreMinecrafts.LOGGER.error("CLIENT_UI_INTERACTION_FAILED confirmation did not reopen");
                        return;
                    }
                    clickWidget(client, 0);
                    MoreMinecrafts.LOGGER.info("CLIENT_UI_PURCHASE_CONFIRMED id=nearby_chest_ingredients");
                } else {
                    uiJourneyFailed = true;
                    MoreMinecrafts.LOGGER.error("CLIENT_UI_INTERACTION_FAILED purchase screen disappeared after cancel");
                    return;
                }
                uiJourneyStage = 7;
            }
            case 7 -> {
                int expectedResult = dev.itzgamesgd.moreminecrafts.progression.ResearchPurchaseService.Result.PURCHASED.ordinal();
                if (!"research:nearby_chest_ingredients".equals(ProgressionClientApi.lastAction())
                        || ProgressionClientApi.lastActionResult() != expectedResult) return;
                var row = ProgressionClientApi.research(null).stream()
                        .filter(entry -> entry.id().equals("nearby_chest_ingredients")).findFirst().orElse(null);
                if (row == null || row.level() != 1) return;
                int xpSpent = purchaseXpBefore - dev.itzgamesgd.moreminecrafts.progression.RawXp.available(client.player);
                int chestsSpent = purchaseChestBefore - itemCount(client, "minecraft:chest");
                int ironBlocksSpent = purchaseIronBlocksBefore - itemCount(client, "minecraft:iron_block");
                int paperSpent = purchasePaperBefore - itemCount(client, "minecraft:paper");
                if (xpSpent != 1100 || chestsSpent != 8 || ironBlocksSpent != 4 || paperSpent != 64) {
                    uiJourneyFailed = true;
                    MoreMinecrafts.LOGGER.error(
                            "CLIENT_UI_PURCHASE_FAILED xp={} chest={} iron_blocks={} paper={}",
                            xpSpent, chestsSpent, ironBlocksSpent, paperSpent
                    );
                    return;
                }
                queueUiScreenshot("research-purchase-state.png");
                uiJourneyStage = 8;
                MoreMinecrafts.LOGGER.info(
                        "CLIENT_UI_PURCHASE_VERIFIED result=PURCHASED research=nearby_chest_ingredients level=1 xp=1100 chest=8 iron_blocks=4 paper=64"
                );
            }
            case 8 -> {
                if (!uiCaptureComplete) return;
                uiCaptureComplete = false;
                var inventory = client.player.getInventory();
                var menu = new AutoFarmMenu(1, inventory, new SimpleContainer(50));
                client.gui.setScreen(new AutoFarmScreen(menu, inventory, Component.translatable("container.moreminecrafts.auto_farm")));
                queueUiScreenshot("auto-farm-screen.png");
                MoreMinecrafts.LOGGER.info("CLIENT_UI_RENDER_FIXTURE screen=auto_farm backend=real_menu empty_container=synthetic");
                uiJourneyStage = 9;
            }
            case 9 -> {
                if (!uiCaptureComplete) return;
                uiCaptureComplete = false;
                MoreMinecrafts.LOGGER.info(
                        "CLIENT_INTERACTION_PATH synthetic Screen button callbacks: selector -> knowledge -> research -> purchase"
                );
                MoreMinecrafts.LOGGER.info("CLIENT_INTERACTION_PATH auto_farm=render_only_synthetic_empty_container");
                uiJourneyStage = 10;
            }
            default -> { }
        }
    }

    private static void advancePhysicalSelectorTest(Minecraft client) {
        Screen screen = client.gui.screen();
        switch (physicalSelectorStage) {
            case 1 -> {
                if (screen instanceof ProgressionBrowseScreen browse
                        && browse.mode() == ProgressionBrowseScreen.Mode.KNOWLEDGE) {
                    verifyPhysicalSelectorTarget("knowledge");
                    openSelectorForPhysicalTest(client, 2, "research");
                }
            }
            case 2 -> {
                if (screen instanceof ProgressionBrowseScreen browse
                        && browse.mode() == ProgressionBrowseScreen.Mode.RESEARCH) {
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
            if (!(client.gui.screen() instanceof ProgressionBrowseScreen browse)
                    || browse.mode() != (choice == 2 ? ProgressionBrowseScreen.Mode.KNOWLEDGE : ProgressionBrowseScreen.Mode.RESEARCH)) {
                uiJourneyFailed = true;
                MoreMinecrafts.LOGGER.error("CLIENT_UI_INTERACTION_FAILED selector choice={} did not open expected screen", choice);
            }
        }
    }

    private static void clickProgressionBack(Minecraft client) {
        clickWidget(client, 0);
        if (!(client.gui.screen() instanceof ProgressionSelectionScreen)) {
            uiJourneyFailed = true;
            MoreMinecrafts.LOGGER.error("CLIENT_UI_INTERACTION_FAILED back button did not return to selector");
        }
    }

    private static void clickResearchPurchase(Minecraft client) {
        clickWidget(client, 10);
    }

    private static void clickDiscipline(Minecraft client, String label) {
        // CI clients use en_us by default; accept both the approved Korean label and its
        // localized English equivalent while activating the actual visible tab widget.
        clickWidgetWithLabel(client, label, "Production Engineering");
    }

    private static void clickWidgetWithLabel(Minecraft client, String... labels) {
        Screen screen = client.gui.screen();
        if (screen == null) { uiJourneyFailed = true; return; }
        for (var child : screen.children()) {
            if (child instanceof Button button && java.util.Arrays.stream(labels)
                    .anyMatch(label -> button.getMessage().getString().equals(label))) {
                button.onPress(new MouseButtonEvent(button.getX() + button.getWidth() / 2.0,
                        button.getY() + button.getHeight() / 2.0, new MouseButtonInfo(0, 0)));
                MoreMinecrafts.LOGGER.info("CLIENT_UI_VISIBLE_BUTTON_ACTIVATED label={}", button.getMessage().getString());
                return;
            }
        }
        uiJourneyFailed = true;
        MoreMinecrafts.LOGGER.error("CLIENT_UI_INTERACTION_FAILED visible button is missing: {}", String.join(" / ", labels));
    }

    private static void clickWidget(Minecraft client, int index) {
        Screen screen = client.gui.screen();
        if (screen == null || index < 0 || index >= screen.children().size()
                || !(screen.children().get(index) instanceof AbstractWidget widget)
                || !widget.active || !widget.visible) {
            uiJourneyFailed = true;
            MoreMinecrafts.LOGGER.error("CLIENT_UI_INTERACTION_FAILED widget={} screen={}", index, screen);
            return;
        }
        if (!(widget instanceof Button button)) {
            uiJourneyFailed = true;
            MoreMinecrafts.LOGGER.error("CLIENT_UI_INTERACTION_FAILED widget={} is not a button", index);
            return;
        }
        button.onPress(new MouseButtonEvent(button.getX() + button.getWidth() / 2.0,
                button.getY() + button.getHeight() / 2.0, new MouseButtonInfo(0, 0)));
        MoreMinecrafts.LOGGER.info("CLIENT_UI_WIDGET_ACTIVATED index={} label={} screen={}", index,
                button.getMessage().getString(), screen.getClass().getSimpleName());
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

