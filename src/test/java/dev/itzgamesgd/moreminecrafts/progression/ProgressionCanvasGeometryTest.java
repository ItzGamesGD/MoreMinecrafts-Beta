package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

final class ProgressionCanvasGeometryTest {
    @Test void compactLogicalViewportIsCanvasFirstAndKeepsNavigationAndFooterReachable() {
        var compact = ProgressionCanvasGeometry.responsiveLayout(320, 180);
        assertTrue(compact.compact());
        assertEquals(0, compact.details().width());
        assertTrue(compact.viewport().width() >= 300);
        assertTrue(compact.viewport().height() >= 80);
        assertTrue(compact.tabs().height() >= 20);
        assertTrue(compact.footer().height() >= 20);
        assertTrue(compact.viewport().y() >= compact.tabs().bottom());
        assertTrue(compact.footer().y() >= compact.viewport().bottom());
    }

    @Test void normalViewportRetainsCanvasAndDetailsRegions() {
        var normal = ProgressionCanvasGeometry.responsiveLayout(900, 520);
        assertFalse(normal.compact());
        assertTrue(normal.viewport().width() > normal.details().width());
        assertTrue(normal.details().width() >= 240);
        assertTrue(normal.viewport().right() <= normal.details().x());
    }

    @Test void initialCameraUsesFitOnlyWhenNodeRemainsReadableOtherwiseFocusesSelection() {
        var viewport = new ProgressionCanvasGeometry.Rect(0, 0, 400, 300);
        var small = ProgressionCanvasGeometry.initialCamera(viewport, 480, 240, 160, 120, false);
        assertEquals(0.833333, small.zoom(), 0.001);
        var large = ProgressionCanvasGeometry.initialCamera(viewport, 1200, 900, 920, 680, false);
        assertEquals(0.75, large.zoom(), 0.0001);
        assertEquals(920, ProgressionCanvasGeometry.screenToWorldX(large, viewport.x(), 200), 0.001);
        assertEquals(680, ProgressionCanvasGeometry.screenToWorldY(large, viewport.y(), 150), 0.001);
        var knowledge = ProgressionCanvasGeometry.initialCamera(viewport, 1200, 900, 600, 400, true);
        assertEquals(0.75, knowledge.zoom(), 0.0001);
    }

    @Test void fitAllAndFocusSelectedHaveExplicitScaleBehavior() {
        var viewport = new ProgressionCanvasGeometry.Rect(0, 0, 320, 180);
        var fit = ProgressionCanvasGeometry.fitAll(viewport, 1200, 900);
        assertTrue(fit.zoom() < 0.75);
        assertTrue(fit.zoom() >= ProgressionCanvasGeometry.MIN_ZOOM);
        var focused = ProgressionCanvasGeometry.focusSelected(fit, viewport, 800, 600);
        assertEquals(0.75, focused.zoom(), 0.0001);
        assertEquals(800, ProgressionCanvasGeometry.screenToWorldX(focused, 0, 160), 0.001);
        assertEquals(600, ProgressionCanvasGeometry.screenToWorldY(focused, 0, 90), 0.001);
    }

    @Test void cursorAnchoredZoomPreservesWorldPointAndStaysWithinLimits() {
        var viewport = new ProgressionCanvasGeometry.Rect(20, 30, 300, 200);
        var start = new ProgressionCanvasGeometry.Camera(100, 80, 0.75);
        double beforeX = ProgressionCanvasGeometry.screenToWorldX(start, viewport.x(), 170);
        double beforeY = ProgressionCanvasGeometry.screenToWorldY(start, viewport.y(), 110);
        var zoomed = ProgressionCanvasGeometry.zoomAt(start, viewport, 170, 110, 3);
        assertEquals(beforeX, ProgressionCanvasGeometry.screenToWorldX(zoomed, viewport.x(), 170), 0.000001);
        assertEquals(beforeY, ProgressionCanvasGeometry.screenToWorldY(zoomed, viewport.y(), 110), 0.000001);
        assertEquals(0.75 * Math.pow(1.1, 3), zoomed.zoom(), 0.000001);
        assertEquals(ProgressionCanvasGeometry.MAX_ZOOM,
                ProgressionCanvasGeometry.zoomAt(start, viewport, 170, 110, 20).zoom(), 0.000001);
        assertEquals(ProgressionCanvasGeometry.MIN_ZOOM,
                ProgressionCanvasGeometry.zoomAt(start, viewport, 170, 110, -100).zoom(), 0.000001);
    }

    @Test void panClampKeepsPartOfCanvasVisibleAndDirectionalNavigationIsDeterministic() {
        var viewport = new ProgressionCanvasGeometry.Rect(0, 0, 100, 80);
        var clamped = ProgressionCanvasGeometry.clampPan(
                new ProgressionCanvasGeometry.Camera(900, 900, 1.0), viewport, 300, 200);
        assertTrue(clamped.panX() <= 300 - ProgressionCanvasGeometry.MIN_VISIBLE_PIXELS);
        assertTrue(clamped.panY() <= 200 - ProgressionCanvasGeometry.MIN_VISIBLE_PIXELS);
        var nodes = List.of(new ProgressionCanvasGeometry.NodePoint("a", "one", 50, 50, 0, 0),
                new ProgressionCanvasGeometry.NodePoint("b", "one", 160, 55, 1, 0),
                new ProgressionCanvasGeometry.NodePoint("c", "one", 52, 150, 0, 1),
                new ProgressionCanvasGeometry.NodePoint("d", "two", 270, 60, 2, 0));
        assertEquals("b", ProgressionCanvasGeometry.nearestDirectional(nodes, "a",
                ProgressionCanvasGeometry.Direction.RIGHT).id());
        assertEquals("c", ProgressionCanvasGeometry.nearestDirectional(nodes, "a",
                ProgressionCanvasGeometry.Direction.DOWN).id());
        assertEquals("d", ProgressionCanvasGeometry.branchFirst(nodes, "two").id());
        assertNull(ProgressionCanvasGeometry.nearestDirectional(nodes, "d",
                ProgressionCanvasGeometry.Direction.RIGHT));
    }
}
