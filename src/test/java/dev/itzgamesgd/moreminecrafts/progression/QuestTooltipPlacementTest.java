package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class QuestTooltipPlacementTest {
    private static final ProgressionCanvasGeometry.Rect VIEW = new ProgressionCanvasGeometry.Rect(0, 0, 640, 360);

    @Test
    void tooltipStaysInsideTheViewportAtEveryEdge() {
        var node = new ProgressionCanvasGeometry.Rect(0, 150, 14, 14);
        int[] p = QuestTooltipPlacement.place(node, List.of(node), 200, 120, 60, VIEW, 6);
        assertTrue(p[0] >= 6 && p[1] >= 6 && p[0] + 200 <= 640 - 6 && p[1] + p[2] <= 360 - 6, "outside viewport");
    }

    @Test
    void tooltipAvoidsCoveringAnyNodeWhenARoomyPlacementExists() {
        var node = new ProgressionCanvasGeometry.Rect(300, 160, 14, 14);
        int[] p = QuestTooltipPlacement.place(node, List.of(node), 110, 90, 40, VIEW, 6);
        assertEquals(1, p[3], "a clear placement should be chosen");
        var placed = new ProgressionCanvasGeometry.Rect(p[0], p[1], 110, p[2]);
        assertTrue(!overlaps(placed, node), "tooltip covers the selected node");
    }

    @Test
    void heightNeverDropsBelowTheMinimumUsefulHeight() {
        var node = new ProgressionCanvasGeometry.Rect(300, 160, 14, 14);
        int[] p = QuestTooltipPlacement.place(node, List.of(node), 110, 300, 50, VIEW, 6);
        assertTrue(p[2] >= 50 || p[2] == 300, "height " + p[2] + " below the minimum useful height");
    }

    private static boolean overlaps(ProgressionCanvasGeometry.Rect a, ProgressionCanvasGeometry.Rect b) {
        return a.x() < b.right() && a.right() > b.x() && a.y() < b.bottom() && a.bottom() > b.y();
    }
}
