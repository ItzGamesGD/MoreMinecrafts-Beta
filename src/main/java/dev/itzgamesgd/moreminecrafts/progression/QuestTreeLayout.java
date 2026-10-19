package dev.itzgamesgd.moreminecrafts.progression;

import java.util.ArrayList;
import java.util.List;

/**
 * Swimlane layout for the XP Quest tree. Each chapter is a horizontal lane: a label gutter on the left,
 * then its nodes in row-major order (progression reads left to right, so future dependency edges run
 * parent to child). Lanes stack vertically and the canvas grows with the graph; positions are
 * presentation-only world coordinates.
 */
public final class QuestTreeLayout {
    public static final int GUTTER = 128;
    public static final int NODE_SPACING = 88;
    public static final int ROW_SPACING = 72;
    public static final int FIRST_NODE_OFFSET = 56;
    public static final int MAX_COLUMNS = 6;
    public static final int LANE_PADDING_Y = 34;
    public static final int LANE_MIN_HEIGHT = 112;
    public static final int LANE_GAP = 10;
    public static final int TRAILING = 32;
    public static final int MARGIN_Y = 20;
    public static final int MIN_WIDTH = 440;

    public record Placed(String id, String chapterId, int laneIndex, int index, int x, int y) {}
    public record Lane(String chapterId, String titleKey, int top, int height, int nodeCount) {
        public int bottom() { return top + height; }
    }
    public record Result(List<Placed> nodes, List<Lane> lanes, int width, int height) {}

    private QuestTreeLayout() {}

    public static Result layout(QuestTreeStructure structure) {
        List<Placed> placed = new ArrayList<>();
        List<Lane> lanes = new ArrayList<>();
        int top = MARGIN_Y, width = MIN_WIDTH;
        for (int laneIndex = 0; laneIndex < structure.chapters().size(); laneIndex++) {
            var chapter = structure.chapters().get(laneIndex);
            int count = chapter.nodeIds().size();
            int columns = Math.max(1, Math.min(count, MAX_COLUMNS));
            int rows = Math.max(1, (count + columns - 1) / columns);
            int height = Math.max(LANE_MIN_HEIGHT, LANE_PADDING_Y * 2 + (rows - 1) * ROW_SPACING);
            int firstY = top + height / 2 - (rows - 1) * ROW_SPACING / 2;
            for (int i = 0; i < count; i++) {
                placed.add(new Placed(chapter.nodeIds().get(i), chapter.id(), laneIndex, i,
                        GUTTER + FIRST_NODE_OFFSET + NODE_SPACING * (i % columns), firstY + ROW_SPACING * (i / columns)));
            }
            width = Math.max(width, GUTTER + FIRST_NODE_OFFSET + NODE_SPACING * (columns - 1) + FIRST_NODE_OFFSET / 2 + TRAILING);
            lanes.add(new Lane(chapter.id(), chapter.titleKey(), top, height, count));
            top += height + LANE_GAP;
        }
        int height = lanes.isEmpty() ? MARGIN_Y * 2 : lanes.get(lanes.size() - 1).bottom() + MARGIN_Y;
        return new Result(List.copyOf(placed), List.copyOf(lanes), width, height);
    }
}
