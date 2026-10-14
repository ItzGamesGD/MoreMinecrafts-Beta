package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

final class ProgressionCanvasInputTest {
    @Test void onlyAStationaryLeftClickOnTheSameNodeReturnsAPurchaseTarget() {
        var gesture = new ProgressionCanvasInput.Gesture();
        gesture.begin(0, 40, 50, "research-a");
        assertEquals("research-a", gesture.release(0, 41, 50, "research-a", false));
    }

    @Test void draggingFromANodeNeverReturnsAPurchaseTarget() {
        var gesture = new ProgressionCanvasInput.Gesture();
        gesture.begin(0, 40, 50, "research-a");
        assertFalse(gesture.dragged(44, 50));
        assertNull(gesture.release(0, 44, 50, "research-a", false));
    }

    @Test void targetMismatchDoubleClickAndNonLeftButtonNeverPurchase() {
        var mismatch = new ProgressionCanvasInput.Gesture();
        mismatch.begin(0, 40, 50, "research-a");
        assertNull(mismatch.release(0, 41, 50, "research-b", false));
        var doubleClick = new ProgressionCanvasInput.Gesture();
        doubleClick.begin(0, 40, 50, "research-a");
        assertNull(doubleClick.release(0, 40, 50, "research-a", true));
        var right = new ProgressionCanvasInput.Gesture();
        right.begin(1, 40, 50, "research-a");
        assertNull(right.release(1, 40, 50, "research-a", false));
    }

    @Test void anEmptySpaceDragIsDistinguishedFromAnEmptySpaceClick() {
        var gesture = new ProgressionCanvasInput.Gesture();
        gesture.begin(0, 40, 50, null);
        assertTrue(gesture.dragged(45, 50));
        assertNull(gesture.release(0, 45, 50, null, false));
    }
}
