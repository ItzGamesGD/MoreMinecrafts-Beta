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

    private static final ProgressionCanvasGeometry.Rect LARGE = new ProgressionCanvasGeometry.Rect(0, 0, 400, 240);

    @Test
    void chooseNeverPlacesTheTooltipOutsideTheViewport() {
        for (int x = 0; x <= 380; x += 19) for (int y = 0; y <= 220; y += 17) {
            var anchor = new ProgressionCanvasGeometry.Rect(x, y, 14, 14);
            int[] p = QuestTooltipPlacement.choose(anchor, null, List.of(anchor), List.of(), 150, 120, 40, LARGE, 6);
            assertTrue(p[0] >= 6 && p[1] >= 6 && p[0] + 150 <= 400 - 6 && p[1] + p[2] <= 240 - 6, "outside at " + x + "," + y);
        }
    }

    @Test
    void chooseAvoidsCoveringTheAnchorWhenAClearPlacementExists() {
        var anchor = new ProgressionCanvasGeometry.Rect(200, 120, 14, 14);
        int[] p = QuestTooltipPlacement.choose(anchor, null, List.of(anchor), List.of(), 120, 100, 40, LARGE, 6);
        assertEquals(0, p[5], "the anchor must not be covered");
        assertEquals(0, p[3], "no node should be covered when the canvas is otherwise empty");
    }

    @Test
    void chooseMinimisesCoveredNodesInADenseField() {
        // In a dense field the chosen box must report exactly the nodes it covers, and never more than the default
        // below-centred placement would cover.
        var anchor = new ProgressionCanvasGeometry.Rect(200, 120, 14, 14);
        List<ProgressionCanvasGeometry.Rect> field = new java.util.ArrayList<>();
        for (int x = 0; x < 400; x += 20) for (int y = 0; y < 240; y += 20) field.add(new ProgressionCanvasGeometry.Rect(x, y, 14, 14));
        int[] best = QuestTooltipPlacement.choose(anchor, null, field, List.of(), 160, 120, 40, LARGE, 6);
        var box = new ProgressionCanvasGeometry.Rect(best[0], best[1], 160, best[2]);
        assertEquals(countCovered(box, field), best[3], "reported node count must match the returned box");
        var below = new ProgressionCanvasGeometry.Rect(best[0], anchor.bottom() + 4, 160, 120);
        assertTrue(best[3] <= countCovered(below, field) || best[2] < 40, "worse than the default below placement");
    }

    @Test
    void chooseIsDeterministicForTheSameGeometry() {
        var anchor = new ProgressionCanvasGeometry.Rect(150, 60, 14, 14);
        var field = List.of(anchor, new ProgressionCanvasGeometry.Rect(50, 60, 14, 14));
        int[] a = QuestTooltipPlacement.choose(anchor, null, field, List.of(), 150, 120, 40, LARGE, 6);
        int[] b = QuestTooltipPlacement.choose(anchor, null, field, List.of(), 150, 120, 40, LARGE, 6);
        assertEquals(java.util.Arrays.toString(a), java.util.Arrays.toString(b));
    }

    private static int countCovered(ProgressionCanvasGeometry.Rect box, List<ProgressionCanvasGeometry.Rect> field) {
        int n = 0;
        for (var f : field) if (overlaps(box, f)) n++;
        return n;
    }
}
