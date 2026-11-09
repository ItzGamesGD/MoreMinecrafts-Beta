package dev.itzgamesgd.moreminecrafts.client;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;

/**
 * Opt-in evidence for OS-level input validation: logs every mouse press the progression screens receive, so the X11
 * harness can prove that one XTest press reached Minecraft exactly once with the expected SDL button.
 */
final class PhysicalInputLog {
    static final boolean ENABLED = "1".equals(System.getenv("MOREMINECRAFTS_VISUAL_SELECTOR_PHYSICAL"));

    private PhysicalInputLog() {}

    static void press(String screen, MouseButtonEvent event) {
        if (!ENABLED) return;
        var window = Minecraft.getInstance().getWindow();
        MoreMinecrafts.LOGGER.info("SELECTOR_MOUSE_EVENT screen={} x={} y={} button={} scale={} window={}x{}",
                screen, event.x(), event.y(), event.button(), window.getGuiScale(), window.getWidth(), window.getHeight());
    }
}
