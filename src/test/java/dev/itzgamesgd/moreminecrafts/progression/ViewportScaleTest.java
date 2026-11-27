package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ViewportScaleTest {
    @Test
    void fixedGuiFitsLogicalViewportsProducedByMultipleGuiScales() {
        int[][] cases = {
                {854, 480, 1}, {854, 480, 2}, {1280, 720, 2}, {1280, 720, 3}, {640, 360, 1}
        };
        for (int[] viewport : cases) {
            double logicalWidth = Math.ceil(viewport[0] / (double) viewport[2]);
            double logicalHeight = Math.ceil(viewport[1] / (double) viewport[2]);
            ViewportScale fit = ViewportScale.fit(logicalWidth, logicalHeight, 330, 360, 8);
            assertTrue(fit.scale() > 0 && fit.scale() <= 1, "valid scale for " + java.util.Arrays.toString(viewport));
            assertTrue(fit.left() >= 8 - 1.0E-8 && fit.top() >= 8 - 1.0E-8);
            assertTrue(fit.left() + 330 * fit.scale() <= logicalWidth - 8 + 1.0E-8);
            assertTrue(fit.top() + 360 * fit.scale() <= logicalHeight - 8 + 1.0E-8);
        }
    }

    @Test
    void pointerMappingRoundTripsForSlotAndControlCoordinates() {
        ViewportScale fit = ViewportScale.fit(427, 240, 330, 360, 8);
        for (double x : new double[]{12, 108, 286, 322}) {
            double screen = fit.screenX(20 + x, 20);
            assertEquals(20 + x, fit.contentX(screen, 20), 1.0E-9);
        }
        for (double y : new double[]{42, 86, 246, 270, 342}) {
            double screen = fit.screenY(10 + y, 10);
            assertEquals(10 + y, fit.contentY(screen, 10), 1.0E-9);
        }
    }
}
