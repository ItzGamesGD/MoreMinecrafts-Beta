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
}
