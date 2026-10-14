package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

final class ProgressionCanvasGeometryTest {
    @Test void compactLogicalViewportKeepsNavigationAndUsesAllSpaceForTheCanvas() {
        var compact = ProgressionCanvasGeometry.responsiveLayout(320, 180);
        assertTrue(compact.compact());
        assertTrue(compact.viewport().width() >= 300);
        assertTrue(compact.viewport().height() >= 80);
        assertTrue(compact.tabs().height() >= 20);
        assertTrue(compact.viewport().y() >= compact.tabs().bottom());
        assertEquals(180 - 8, compact.viewport().bottom());
    }

    @Test void normalViewportUsesTheFullCanvasAndRemovesTheDetailsAndFooterColumns() {
        var normal = ProgressionCanvasGeometry.responsiveLayout(900, 520);
        assertFalse(normal.compact());
        assertEquals(900 - 40, normal.viewport().width());
        assertEquals(520 - 20, normal.viewport().bottom());
        var scaleTwoBoundary = ProgressionCanvasGeometry.responsiveLayout(640, 360);
        assertFalse(scaleTwoBoundary.compact());
        assertEquals(640 - 40, scaleTwoBoundary.viewport().width());
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

    @Test void frameBoundsMatchTheRenderedSquareAtEveryZoomAndExcludeTheFirstOutsidePixel() {
        int[] expectedSizes = {14, 26, 33, 39};
        double[] zooms = {ProgressionCanvasGeometry.MIN_ZOOM, 1.0, 1.25, ProgressionCanvasGeometry.MAX_ZOOM};
        for (int i = 0; i < zooms.length; i++) {
            assertEquals(expectedSizes[i], ProgressionCanvasGeometry.nodeFrameSize(zooms[i]));
            var bounds = ProgressionCanvasGeometry.nodeFrameBounds(101, 77, zooms[i]);
            assertEquals(expectedSizes[i], bounds.width());
            assertEquals(expectedSizes[i], bounds.height());
            assertTrue(bounds.contains(bounds.x(), bounds.y()));
            assertTrue(bounds.contains(bounds.right() - 1, bounds.bottom() - 1));
            assertFalse(bounds.contains(bounds.x() - 1, bounds.y()));
            assertFalse(bounds.contains(bounds.right(), bounds.bottom() - 1));
            assertFalse(bounds.contains(bounds.x(), bounds.bottom()));
        }
    }

    @Test void frameHitCoordinatesStayLogicalAndExactAtGuiScalesTwoThreeAndFour() {
        for (int scale = 2; scale <= 4; scale++) {
            int guiWidth = (int) Math.ceil(1280.0 / scale);
            int guiHeight = (int) Math.ceil(720.0 / scale);
            var layout = ProgressionCanvasGeometry.responsiveLayout(guiWidth, guiHeight);
            int centerX = layout.viewport().x() + layout.viewport().width() / 2;
            int centerY = layout.viewport().y() + layout.viewport().height() / 2;
            var frame = ProgressionCanvasGeometry.nodeFrameBounds(centerX, centerY, 1.0);
            assertTrue(layout.viewport().contains(frame.x(), frame.y()));
            assertTrue(layout.viewport().contains(frame.right() - 1, frame.bottom() - 1));
            assertFalse(frame.contains(frame.right(), centerY));
        }
    }

    @Test void frameHitBoundsFollowCursorZoomAndCanvasPan() {
        var viewport = new ProgressionCanvasGeometry.Rect(20, 30, 300, 200);
        var camera = new ProgressionCanvasGeometry.Camera(100, 80, 1.0);
        for (var transform : List.of(camera,
                ProgressionCanvasGeometry.zoomAt(camera, viewport, 140, 110, 4),
                ProgressionCanvasGeometry.panBy(camera, 23, -17))) {
            int centerX = (int) Math.round(ProgressionCanvasGeometry.worldToScreenX(transform, viewport.x(), 240));
            int centerY = (int) Math.round(ProgressionCanvasGeometry.worldToScreenY(transform, viewport.y(), 180));
            var frame = ProgressionCanvasGeometry.nodeFrameBounds(centerX, centerY, transform.zoom());
            assertTrue(frame.contains(centerX, centerY));
            assertFalse(frame.contains(frame.right(), centerY));
        }
    }

    @Test void adjacentRenderedFramesDoNotClaimTheGapBetweenThem() {
        var left = ProgressionCanvasGeometry.nodeFrameBounds(50, 50, 1.0);
        var right = ProgressionCanvasGeometry.nodeFrameBounds(80, 50, 1.0);
        assertEquals(4, right.x() - left.right());
        for (int x = left.right(); x < right.x(); x++) assertFalse(left.contains(x, 50) || right.contains(x, 50));
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
