package dev.itzgamesgd.moreminecrafts.progression;

import java.util.List;

/** Pure tooltip placement for the quest canvas, kept free of client types so it can be unit tested. */
public final class QuestTooltipPlacement {
    private QuestTooltipPlacement() {}

    /**
     * Picks the placement that keeps every node uncovered and offers the most room: below, above, then beside the node. The height is
     * trimmed to the room available (the list scrolls), but never below {@code minUseful}. Returns {x, y, height, clear} where clear is 1 when no node is covered.
     */
    public static int[] place(ProgressionCanvasGeometry.Rect node, List<ProgressionCanvasGeometry.Rect> allFrames,
                                      int width, int wantedHeight, int minUseful, ProgressionCanvasGeometry.Rect bounds, int margin) {
        int left = bounds.x(), top = bounds.y(), right = bounds.right(), bottom = bounds.bottom();
        final int gap = 4;
        int centeredX = Math.max(left + margin, Math.min(node.x() + node.width() / 2 - width / 2, right - width - margin));
        int below = bottom - margin - (node.bottom() + gap);
        int above = node.y() - gap - (top + margin);
        int hBelow = Math.min(wantedHeight, below), hAbove = Math.min(wantedHeight, above);
        int hSide = Math.min(wantedHeight, bounds.height() - margin * 2);
        int sideY = Math.max(top + margin, Math.min(node.y() + node.height() / 2 - 14, bottom - hSide - margin));
        int[][] candidates = {
                {centeredX, node.bottom() + gap, hBelow},
                {centeredX, node.y() - gap - hAbove, hAbove},
                {node.right() + gap, sideY, hSide},
                {node.x() - gap - width, sideY, hSide}};
        int[] best = null, bestFit = null;
        long bestScore = Long.MAX_VALUE;
        for (int[] c : candidates) {
            boolean fits = c[2] >= minUseful && c[0] >= left + margin && c[0] + width <= right - margin
                    && c[1] >= top + margin && c[1] + c[2] <= bottom - margin;
            long overlap = 0;
            for (var frame : allFrames) {
                long w = Math.max(0, Math.min(c[0] + width, frame.right()) - Math.max(c[0], frame.x()));
                long h = Math.max(0, Math.min(c[1] + c[2], frame.bottom()) - Math.max(c[1], frame.y()));
                overlap += w * h;
            }
            if (fits && overlap == 0 && (bestFit == null || c[2] > bestFit[2])) bestFit = new int[] {c[0], c[1], c[2], 1};
            long score = overlap * 4 + (fits ? 0 : 1_000_000L) + (wantedHeight - c[2]);
            if (score < bestScore) { bestScore = score; best = c; }
        }
        if (bestFit != null) return bestFit;
        int x = Math.max(left + margin, Math.min(best[0], right - width - margin));
        int h = Math.max(Math.min(minUseful, bounds.height() - margin * 2), best[2]);
        int y = Math.max(top + margin, Math.min(best[1], bottom - h - margin));
        return new int[] {x, y, h, 0};
    }

    /**
     * Collision-aware placement for large graphs. Tries twelve candidates (below, above, right and left of the anchor, each
     * centred or aligned to either end of it) and returns the best by, in order: fitting inside {@code bounds} with at least
     * {@code minUseful} height, leaving the anchor uncovered, leaving the selected node uncovered, covering the fewest visible
     * node frames, covering the fewest sub-branch captions, the tallest height, then the candidate order (which keeps the
     * familiar below-centred placement when the rest is equal). Returns {x, y, height, nodesCovered, captionsCovered, anchorCovered}.
     * The result depends only on geometry, never on the pointer or the scroll position, so it cannot flicker.
     */
    public static int[] choose(ProgressionCanvasGeometry.Rect anchor, ProgressionCanvasGeometry.Rect selected,
                               List<ProgressionCanvasGeometry.Rect> nodeFrames, List<ProgressionCanvasGeometry.Rect> captions,
                               int width, int wantedHeight, int minUseful, ProgressionCanvasGeometry.Rect bounds, int margin) {
        final int gap = 4;
        int left = bounds.x() + margin, right = bounds.right() - margin, top = bounds.y() + margin, bottom = bounds.bottom() - margin;
        int[] bestCandidate = null;
        long bestScore = Long.MAX_VALUE;
        int[][] shapes = candidates(anchor, width, wantedHeight, bounds, margin, gap);
        for (int i = 0; i < shapes.length; i++) {
            int[] c = shapes[i];
            boolean fits = c[2] >= minUseful && c[0] >= left && c[0] + width <= right && c[1] >= top && c[1] + c[2] <= bottom;
            var box = new ProgressionCanvasGeometry.Rect(c[0], c[1], width, Math.max(0, c[2]));
            int nodes = 0;
            for (var frame : nodeFrames) if (intersects(box, frame) && intersects(bounds, frame)) nodes++;
            int captionsHit = 0;
            for (var caption : captions) if (intersects(box, caption)) captionsHit++;
            int anchorHit = intersects(box, anchor) ? 1 : 0;
            int selectedHit = selected != null && intersects(box, selected) ? 1 : 0;
            long score = (fits ? 0L : 1L << 40) + anchorHit * (1L << 36) + selectedHit * (1L << 34)
                    + nodes * (1L << 24) + captionsHit * (1L << 16) + (wantedHeight - c[2]) * 8L + i;
            if (score < bestScore) {
                bestScore = score;
                bestCandidate = new int[] {c[0], c[1], c[2], nodes, captionsHit, anchorHit};
            }
        }
        return bestCandidate;
    }

    /** Candidate {x, y, height} boxes in preference order. Each box is clamped inside {@code bounds}. */
    private static int[][] candidates(ProgressionCanvasGeometry.Rect a, int width, int wanted, ProgressionCanvasGeometry.Rect bounds,
                                      int margin, int gap) {
        int left = bounds.x() + margin, right = bounds.right() - margin, top = bounds.y() + margin, bottom = bounds.bottom() - margin;
        int[] xs = {clamp(a.x() + a.width() / 2 - width / 2, left, right - width), clamp(a.x(), left, right - width),
                clamp(a.right() - width, left, right - width)};
        List<int[]> out = new java.util.ArrayList<>();
        int belowH = Math.min(wanted, bottom - (a.bottom() + gap));
        int aboveH = Math.min(wanted, a.y() - gap - top);
        for (int x : xs) out.add(new int[] {x, a.bottom() + gap, belowH});
        for (int x : xs) out.add(new int[] {x, a.y() - gap - aboveH, aboveH});
        int sideH = Math.min(wanted, bottom - top);
        int[] ys = {clamp(a.y(), top, bottom - sideH), clamp(a.y() + a.height() / 2 - sideH / 2, top, bottom - sideH),
                clamp(a.bottom() - sideH, top, bottom - sideH)};
        for (int y : ys) out.add(new int[] {a.right() + gap, y, sideH});
        for (int y : ys) out.add(new int[] {a.x() - gap - width, y, sideH});
        return out.toArray(new int[0][]);
    }

    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(v, Math.max(lo, hi))); }

    private static boolean intersects(ProgressionCanvasGeometry.Rect a, ProgressionCanvasGeometry.Rect b) {
        return a.x() < b.right() && a.right() > b.x() && a.y() < b.bottom() && a.bottom() > b.y();
    }
}
