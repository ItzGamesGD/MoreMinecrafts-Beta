package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Guards the two quest-canvas defects repaired in #126 and #129. Runtime behaviour is checked by the native
 * harness (stale-tooltip and keyboard-tooltip regression steps); these guards catch an accidental revert in source.
 */
class QuestDefectRegressionGuardTest {
    private static final Path CLIENT = Path.of("src/client/java/dev/itzgamesgd/moreminecrafts/client");

    @Test
    void keyboardFocusOpensTheSelectionTooltip() throws IOException {
        String screen = Files.readString(CLIENT.resolve("ProgressionBrowseScreen.java"));
        int keyPressed = screen.indexOf("public boolean keyPressed(KeyEvent event)");
        int end = screen.indexOf("@Override public void onClose()", keyPressed);
        String body = keyPressed >= 0 && end > keyPressed ? screen.substring(keyPressed, end) : screen.substring(keyPressed);
        assertTrue(body.contains("canvas.setSelectionTooltip(true)"), "keyboard focus no longer opens the selection tooltip");
    }

    @Test
    void anUndrawnTooltipReleasesItsHitRectangle() throws IOException {
        String canvas = Files.readString(CLIENT.resolve("ProgressionTreeCanvas.java"));
        assertTrue(canvas.contains("if (hoveredId == null) tooltipWidth = 0;"),
                "an undrawn tooltip no longer clears its hit rectangle");
    }
}
