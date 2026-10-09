package dev.itzgamesgd.moreminecrafts.client;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

public final class VisualValidationHarness {
    private static final String OPT_IN_ENV = "MOREMINECRAFTS_VISUAL_VALIDATION";
    private static final boolean ENABLED = "1".equals(System.getenv(OPT_IN_ENV));
    private static boolean readySignalEmitted;
    private static int readyTicks;

    private VisualValidationHarness() {
    }

    public static void register() {
        if (!ENABLED) {
            return;
        }

        MoreMinecrafts.LOGGER.info("Visual-validation test hook enabled by {}", OPT_IN_ENV);
        ClientTickEvents.END_CLIENT_TICK.register(VisualValidationHarness::onClientTick);
    }

    private static void onClientTick(Minecraft client) {
        if (readySignalEmitted || client.player == null || client.level == null
                || client.gui.screen() != null) {
            return;
        }

        if (++readyTicks < 120) {
            return;
        }

        // The test server fixes the spawn and scene. Orient the real client camera toward it.
        client.player.setYRot(180.0F);
        client.player.setXRot(8.0F);
        readySignalEmitted = true;
        MoreMinecrafts.LOGGER.info(
                "VISUAL_VALIDATION_WORLD_READY dimension={} player=({}, {}, {}) ticks={}",
                client.level.dimension(),
                client.player.getX(),
                client.player.getY(),
                client.player.getZ(),
                readyTicks
        );
    }
}
