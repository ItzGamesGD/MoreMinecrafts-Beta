package dev.itzgamesgd.moreminecrafts.client;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
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

import java.nio.file.Files;
import java.nio.file.Path;

public final class VisualValidationHarness {
    private static final String OPT_IN_ENV = "MOREMINECRAFTS_VISUAL_VALIDATION";
    private static final String SERVER_ENV = "MOREMINECRAFTS_VISUAL_SERVER_ADDRESS";
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
            uiJourneyStarted = true;
            client.gui.setScreen(new ProgressionSelectionScreen());
            queueUiScreenshot("progression-selection.png");
            uiJourneyStage = 1;
            return;
        }
        if (uiCaptureName != null || !uiCaptureComplete && uiJourneyStage > 0) return;
        switch (uiJourneyStage) {
            case 1 -> {
                if (!uiCaptureComplete) return;
                uiCaptureComplete = false;
                clickSelectionChoice(client, 2);
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
                for (int page = 0; page < 7; page++) clickResearchNext(client);
                clickResearchRow(client, 1);
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
                clickResearchPurchase(client);
                uiJourneyStage = 5;
            }
            case 5 -> {
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
                uiJourneyStage = 6;
                MoreMinecrafts.LOGGER.info(
                        "CLIENT_UI_PURCHASE_VERIFIED result=PURCHASED research=nearby_chest_ingredients level=1 xp=1100 chest=8 iron_blocks=4 paper=64"
                );
            }
            case 6 -> {
                if (!uiCaptureComplete) return;
                uiCaptureComplete = false;
                MoreMinecrafts.LOGGER.info(
                        "CLIENT_INTERACTION_PATH synthetic Screen button callbacks: selector -> knowledge -> research -> purchase"
                );
                uiJourneyStage = 7;
            }
            default -> { }
        }
    }

    private static void clickSelectionChoice(Minecraft client, int choice) {
        clickWidget(client, choice - 1);
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

    private static void clickResearchNext(Minecraft client) {
        clickWidget(client, 9);
        if (!(client.gui.screen() instanceof ProgressionBrowseScreen browse)
                || browse.mode() != ProgressionBrowseScreen.Mode.RESEARCH) {
            uiJourneyFailed = true;
            MoreMinecrafts.LOGGER.error("CLIENT_UI_INTERACTION_FAILED research page navigation changed screen");
        }
    }

    private static void clickResearchRow(Minecraft client, int row) {
        clickWidget(client, 1 + row);
        if (!(client.gui.screen() instanceof ProgressionBrowseScreen browse) || browse.validationSelection() != 50) {
            uiJourneyFailed = true;
            MoreMinecrafts.LOGGER.error("CLIENT_UI_INTERACTION_FAILED purchasable research row was not selected");
        }
    }

    private static void clickResearchPurchase(Minecraft client) {
        clickWidget(client, 10);
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

