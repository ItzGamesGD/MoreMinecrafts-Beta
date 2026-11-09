package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

final class ProgressionCanvasInputTest {
    @Test void onlyAStationaryLeftClickOnTheSameNodeReturnsAPurchaseTarget() {
        var gesture = new ProgressionCanvasInput.Gesture();
        gesture.begin(true, 40, 50, "research-a");
        assertEquals("research-a", gesture.release(true, 41, 50, "research-a", false));
    }

    @Test void draggingFromANodeNeverReturnsAPurchaseTarget() {
        var gesture = new ProgressionCanvasInput.Gesture();
        gesture.begin(true, 40, 50, "research-a");
        assertFalse(gesture.dragged(44, 50));
        assertNull(gesture.release(true, 44, 50, "research-a", false));
    }

    @Test void targetMismatchDoubleClickAndNonLeftButtonNeverPurchase() {
        var mismatch = new ProgressionCanvasInput.Gesture();
        mismatch.begin(true, 40, 50, "research-a");
        assertNull(mismatch.release(true, 41, 50, "research-b", false));
        var doubleClick = new ProgressionCanvasInput.Gesture();
        doubleClick.begin(true, 40, 50, "research-a");
        assertNull(doubleClick.release(true, 40, 50, "research-a", true));
        var secondary = new ProgressionCanvasInput.Gesture();
        secondary.begin(false, 40, 50, "research-a");
        assertNull(secondary.release(false, 40, 50, "research-a", false));
    }

    @Test void aPrimaryPressReleasedWithAnotherButtonNeverPurchases() {
        var gesture = new ProgressionCanvasInput.Gesture();
        gesture.begin(true, 40, 50, "research-a");
        assertNull(gesture.release(false, 40, 50, "research-a", false));
        assertNull(gesture.release(true, 40, 50, "research-a", false), "the gesture is consumed by the first release");
    }

    @Test void anEmptySpaceDragIsDistinguishedFromAnEmptySpaceClick() {
        var gesture = new ProgressionCanvasInput.Gesture();
        gesture.begin(true, 40, 50, null);
        assertTrue(gesture.dragged(45, 50));
        assertNull(gesture.release(true, 45, 50, null, false));
    }
}
