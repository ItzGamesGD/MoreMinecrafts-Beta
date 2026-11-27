package dev.itzgamesgd.moreminecrafts.client;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import java.nio.file.Files;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.resources.Identifier;

/** Native framebuffer capture, following the repository's successful 26.3 client harness. */
public final class ArenaVisualValidationHarness {
    private static int startupTicks;
    private static int readyTicks;
    private static int renderedFrames;
    private static boolean connecting;
    private static boolean joined;
    private static boolean capture;

    private ArenaVisualValidationHarness() {}

    public static void register() {
        if (!"1".equals(System.getenv("MOREMINECRAFTS_ARENA_VISUAL"))) return;
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.level == null) {
                if (!connecting && client.gui.screen() != null && ++startupTicks >= 60) {
                    connecting = true;
                    String address = System.getenv().getOrDefault("MOREMINECRAFTS_ARENA_SERVER", "127.0.0.1:25565");
                    ConnectScreen.startConnecting(client.gui.screen(), client, ServerAddress.parseString(address),
                            new ServerData("Arena validation", address, ServerData.Type.OTHER), false, null);
                }
                return;
            }
            if (!joined) {
                joined = true;
                MoreMinecrafts.LOGGER.info("ARENA_CLIENT_JOINED");
            }
            if (client.gui.screen() != null) return;
            readyTicks++;
            client.player.setYRot(Float.parseFloat(System.getenv().getOrDefault("ARENA_CAMERA_YAW", "0")));
            client.player.setXRot(Float.parseFloat(System.getenv().getOrDefault("ARENA_CAMERA_PITCH", "20")));
        });
        HudElementRegistry.attachElementAfter(VanillaHudElements.CHAT, Identifier.parse("moreminecrafts:arena_validation_frame"),
                (graphics, delta) -> {
                    var client = Minecraft.getInstance();
                    if (capture || readyTicks < 600 || client.player == null || client.gui.screen() != null || ++renderedFrames < 30) return;
                    capture = true;
                    String name = System.getenv().getOrDefault("ARENA_SCREENSHOT_NAME", "arena-overview.png");
                    var path = client.gameDirectory.toPath().resolve("screenshots").resolve(name);
                    Screenshot.grab(client.gameDirectory, name, client.gameRenderer.mainRenderTarget(), 1, message -> {
                        try {
                            if (!Files.isRegularFile(path) || Files.size(path) == 0) throw new IllegalStateException(message.getString());
                            MoreMinecrafts.LOGGER.info("ARENA_CAPTURE_WRITTEN path={} bytes={}", path, Files.size(path));
                        } catch (Exception error) {
                            MoreMinecrafts.LOGGER.error("ARENA_CAPTURE_FAILED", error);
                        }
                    });
                });
    }
}
