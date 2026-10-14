package dev.itzgamesgd.moreminecrafts.progression;

/** Pure left-button gesture policy shared by the canvas and headless interaction tests. */
public final class ProgressionCanvasInput {
    private ProgressionCanvasInput() {}

    public static final class Gesture {
        private static final double DRAG_THRESHOLD = 4.0;
        private boolean active;
        private boolean dragged;
        private int button;
        private double startX;
        private double startY;
        private String startTarget;

        public void begin(int button, double x, double y, String target) {
            active = button == 0;
            dragged = false;
            this.button = button;
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

        /** Returns a target only for an unmoved, matching left click; always consumes the gesture. */
        public String release(int button, double x, double y, String releaseTarget, boolean doubleClick) {
            if (!active) return null;
            boolean moved = Math.hypot(x - startX, y - startY) >= DRAG_THRESHOLD;
            String target = this.button == 0 && button == 0 && !doubleClick && !dragged && !moved
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
