package dev.itzgamesgd.moreminecrafts.progression;

import java.util.Comparator;
import java.util.List;

/** Pure logical-pixel geometry and camera policy shared by the progression tree screens. */
public final class ProgressionCanvasGeometry {
    public static final double MIN_ZOOM = 0.1;
    public static final double MAX_ZOOM = 1.5;
    public static final double LEGIBLE_ZOOM = 0.75;
    public static final int MIN_VISIBLE_PIXELS = 32;

    private ProgressionCanvasGeometry() {}

    public record Rect(int x, int y, int width, int height) {
        public Rect {
            if (width < 0 || height < 0) throw new IllegalArgumentException("Negative rectangle size");
        }
        public int right() { return x + width; }
        public int bottom() { return y + height; }
        public boolean contains(double px, double py) {
            return px >= x && py >= y && px < right() && py < bottom();
        }
    }

    public record ResponsiveLayout(boolean compact, Rect tabs, Rect viewport, Rect details, Rect footer) {}
    public record Camera(double panX, double panY, double zoom) {
        public Camera {
            if (!Double.isFinite(panX) || !Double.isFinite(panY) || !Double.isFinite(zoom)
                    || zoom < MIN_ZOOM || zoom > MAX_ZOOM)
                throw new IllegalArgumentException("Invalid progression camera");
        }
    }
    public record NodePoint(String id, String branch, double x, double y, int branchOrder, int nodeOrder) {}
    public enum Direction { LEFT, RIGHT, UP, DOWN }

    public static ResponsiveLayout responsiveLayout(int width, int height) {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("Viewport dimensions must be positive");
        boolean compact = width < 640 || height < 360;
        int margin = compact ? 8 : 20;
        int headerHeight = compact ? 22 : 38;
        int tabHeight = compact ? 22 : 30;
        int footerHeight = compact ? 26 : 34;
        int availableWidth = Math.max(0, width - margin * 2);
        int tabY = margin + headerHeight;
        Rect tabs = new Rect(margin, tabY, availableWidth, Math.min(tabHeight, Math.max(0, height - tabY)));
        int viewportY = tabs.bottom();
        int viewportBottom = Math.max(viewportY, height - margin - footerHeight);
        int viewportHeight = viewportBottom - viewportY;
        Rect footer = new Rect(margin, height - margin - footerHeight, availableWidth, footerHeight);
        if (compact) return new ResponsiveLayout(true, tabs,
                new Rect(margin, viewportY, availableWidth, viewportHeight), new Rect(0, 0, 0, 0), footer);

        int gap = 16;
        int canvasWidth = Math.max(0, (availableWidth - gap) * 62 / 100);
        int detailsX = margin + canvasWidth + gap;
        int detailsWidth = Math.max(0, width - margin - detailsX);
        return new ResponsiveLayout(false, tabs,
                new Rect(margin, viewportY, canvasWidth, viewportHeight),
                new Rect(detailsX, viewportY, detailsWidth, viewportHeight), footer);
    }

    public static Camera initialCamera(Rect viewport, double canvasWidth, double canvasHeight,
                                       double selectedX, double selectedY, boolean knowledge) {
        double fit = fitScale(viewport, canvasWidth, canvasHeight);
        int nodeSize = knowledge ? ProgressionNodeLayoutCatalog.KNOWLEDGE_NODE_SIZE
                : ProgressionNodeLayoutCatalog.RESEARCH_NODE_SIZE;
        if (fit >= LEGIBLE_ZOOM && nodeSize * fit >= nodeSize * LEGIBLE_ZOOM)
            return centered(viewport, canvasWidth / 2.0, canvasHeight / 2.0, fit);
        return centered(viewport, selectedX, selectedY, LEGIBLE_ZOOM);
    }

    public static Camera fitAll(Rect viewport, double canvasWidth, double canvasHeight) {
        return centered(viewport, canvasWidth / 2.0, canvasHeight / 2.0, fitScale(viewport, canvasWidth, canvasHeight));
    }

    public static Camera focusSelected(Camera current, Rect viewport, double selectedX, double selectedY) {
        return centered(viewport, selectedX, selectedY, Math.max(LEGIBLE_ZOOM, current.zoom()));
    }

    private static Camera centered(Rect viewport, double worldX, double worldY, double zoom) {
        double scale = clamp(zoom, MIN_ZOOM, MAX_ZOOM);
        return new Camera(worldX - viewport.width() / (2.0 * scale),
                worldY - viewport.height() / (2.0 * scale), scale);
    }

    private static double fitScale(Rect viewport, double canvasWidth, double canvasHeight) {
        if (canvasWidth <= 0 || canvasHeight <= 0) throw new IllegalArgumentException("Canvas bounds must be positive");
        return clamp(Math.min(viewport.width() / canvasWidth, viewport.height() / canvasHeight), MIN_ZOOM, MAX_ZOOM);
    }

    public static Camera zoomAt(Camera camera, Rect viewport, double screenX, double screenY, int wheelSteps) {
        if (wheelSteps == 0) return camera;
        double worldX = screenToWorldX(camera, viewport.x(), screenX);
        double worldY = screenToWorldY(camera, viewport.y(), screenY);
        double zoom = clamp(camera.zoom() * Math.pow(1.1, wheelSteps), MIN_ZOOM, MAX_ZOOM);
        double panX = worldX - (screenX - viewport.x()) / zoom;
        double panY = worldY - (screenY - viewport.y()) / zoom;
        return new Camera(panX, panY, zoom);
    }

    public static Camera panBy(Camera camera, double screenDeltaX, double screenDeltaY) {
        return new Camera(camera.panX() - screenDeltaX / camera.zoom(),
                camera.panY() - screenDeltaY / camera.zoom(), camera.zoom());
    }

    public static Camera clampPan(Camera camera, Rect viewport, double canvasWidth, double canvasHeight) {
        double visible = MIN_VISIBLE_PIXELS / camera.zoom();
        double minX = visible - viewport.width() / camera.zoom();
        double maxX = Math.max(minX, canvasWidth - visible);
        double minY = visible - viewport.height() / camera.zoom();
        double maxY = Math.max(minY, canvasHeight - visible);
        return new Camera(clamp(camera.panX(), minX, maxX), clamp(camera.panY(), minY, maxY), camera.zoom());
    }

    public static double screenToWorldX(Camera camera, int viewportOriginX, double screenX) {
        return camera.panX() + (screenX - viewportOriginX) / camera.zoom();
    }
    public static double screenToWorldY(Camera camera, int viewportOriginY, double screenY) {
        return camera.panY() + (screenY - viewportOriginY) / camera.zoom();
    }
    public static double worldToScreenX(Camera camera, int viewportOriginX, double worldX) {
        return viewportOriginX + (worldX - camera.panX()) * camera.zoom();
    }
    public static double worldToScreenY(Camera camera, int viewportOriginY, double worldY) {
        return viewportOriginY + (worldY - camera.panY()) * camera.zoom();
    }

    public static NodePoint branchFirst(List<NodePoint> nodes, String branch) {
        return nodes.stream().filter(node -> node.branch().equals(branch))
                .min(Comparator.comparingInt(NodePoint::branchOrder).thenComparingInt(NodePoint::nodeOrder)
                        .thenComparing(NodePoint::id)).orElse(null);
    }

    public static NodePoint nearestDirectional(List<NodePoint> nodes, String selectedId, Direction direction) {
        NodePoint selected = nodes.stream().filter(node -> node.id().equals(selectedId)).findFirst().orElse(null);
        if (selected == null) return null;
        return nodes.stream().filter(node -> !node.id().equals(selectedId))
                .filter(node -> isInDirection(selected, node, direction))
                .min(Comparator.comparingDouble((NodePoint node) -> directionalScore(selected, node, direction))
                        .thenComparing(NodePoint::id)).orElse(null);
    }

    public static NodePoint previousOrNext(List<NodePoint> nodes, String selectedId, int delta) {
        if (nodes.isEmpty() || delta == 0) return null;
        List<NodePoint> ordered = nodes.stream().sorted(Comparator.comparingInt(NodePoint::branchOrder)
                .thenComparingInt(NodePoint::nodeOrder).thenComparing(NodePoint::id)).toList();
        int current = -1;
        for (int i = 0; i < ordered.size(); i++) if (ordered.get(i).id().equals(selectedId)) current = i;
        if (current < 0) return ordered.get(0);
        return ordered.get(Math.floorMod(current + Integer.signum(delta), ordered.size()));
    }

    private static boolean isInDirection(NodePoint from, NodePoint to, Direction direction) {
        return switch (direction) {
            case LEFT -> to.x() < from.x();
            case RIGHT -> to.x() > from.x();
            case UP -> to.y() < from.y();
            case DOWN -> to.y() > from.y();
        };
    }

    private static double directionalScore(NodePoint from, NodePoint to, Direction direction) {
        double primary = switch (direction) {
            case LEFT, RIGHT -> Math.abs(to.x() - from.x());
            case UP, DOWN -> Math.abs(to.y() - from.y());
        };
        double secondary = switch (direction) {
            case LEFT, RIGHT -> Math.abs(to.y() - from.y());
            case UP, DOWN -> Math.abs(to.x() - from.x());
        };
        return primary + secondary * 1.5;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
