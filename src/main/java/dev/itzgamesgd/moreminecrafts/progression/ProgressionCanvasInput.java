package dev.itzgamesgd.moreminecrafts.progression;

/**
 * Pure primary-button gesture policy shared by the canvas and headless interaction tests. The caller decides which
 * device button is primary (Minecraft 26.3: {@code InputConstants.MOUSE_BUTTON_LEFT}, SDL numbering).
 */
public final class ProgressionCanvasInput {
    private ProgressionCanvasInput() {}

    public static final class Gesture {
        private static final double DRAG_THRESHOLD = 4.0;
        private boolean active;
        private boolean dragged;
        private double startX;
        private double startY;
        private String startTarget;

        public void begin(boolean primary, double x, double y, String target) {
            active = primary;
            dragged = false;
            startX = x;
            startY = y;
            startTarget = target;
        }

        /** Returns true once an empty-space gesture has crossed the pan threshold. */
        public boolean dragged(double x, double y) {
            if (!active) return false;
            if (Math.hypot(x - startX, y - startY) >= DRAG_THRESHOLD) dragged = true;
            return dragged && startTarget == null;
        }

        /** Returns a target only for an unmoved, matching primary click; always consumes the gesture. */
        public String release(boolean primary, double x, double y, String releaseTarget, boolean doubleClick) {
            if (!active) return null;
            boolean moved = Math.hypot(x - startX, y - startY) >= DRAG_THRESHOLD;
            String target = primary && !doubleClick && !dragged && !moved
                    && startTarget != null && startTarget.equals(releaseTarget) ? startTarget : null;
            active = false;
            dragged = false;
            startTarget = null;
            return target;
        }

        public void cancel() {
            active = false;
            dragged = false;
            startTarget = null;
        }
    }
}
