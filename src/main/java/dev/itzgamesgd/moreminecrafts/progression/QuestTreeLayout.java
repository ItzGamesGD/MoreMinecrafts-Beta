package dev.itzgamesgd.moreminecrafts.progression;

import java.util.ArrayList;
import java.util.List;

/**
 * Lane layout for the XP Quest tree. Each chapter is a horizontal lane with a label gutter on the left.
 *
 * <p>Small graphs (at most {@link #LARGE_GRAPH_NODES} quests) keep the original geometry, so the production
 * three-node slice is unchanged. Large graphs reserve a caption band above every sub-branch and start each
 * sub-branch on a new row, so captions can never sit on a node. Positions are presentation-only world coordinates.
 */
public final class QuestTreeLayout {
    /** Graphs larger than this use the large-graph layout and semantic zoom. */
    public static final int LARGE_GRAPH_NODES = 24;
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
    /** Caption band above a sub-branch's first row (large graphs only). No node frame reaches into it. */
    public static final int CAPTION_HEIGHT = 26;
    /** Gap between consecutive sub-branches in one lane (large graphs only). */
    public static final int SUB_GAP = 14;

    public record Placed(String id, String chapterId, String subBranchId, int laneIndex, int index, int x, int y) {}

    public record Lane(String chapterId, String titleKey, int top, int height, int nodeCount) {
        public int bottom() { return top + height; }
    }

    /**
     * Bounds of one sub-branch. {@code y} is the top of its caption band (or of its first row for small graphs),
     * and {@code firstRowY} is the centre of its first row of nodes.
     */
    public record SubBranchBounds(String id, String chapterId, String titleKey, int x, int y, int width, int height,
                                  int firstRowY, int rows, int columns, int nodeCount) {
        public int bottom() { return y + height; }
        public int right() { return x + width; }
    }

    public record Result(List<Placed> nodes, List<Lane> lanes, List<SubBranchBounds> subBranches, int width, int height,
                         boolean large) {}

    private QuestTreeLayout() {}

    public static Result layout(QuestTreeStructure structure) {
        boolean large = structure.allNodeIds().size() > LARGE_GRAPH_NODES;
        return large ? layoutLarge(structure) : layoutSmall(structure);
    }

    /** The original geometry: one row per chapter-wide grid, centred vertically in its lane. */
    private static Result layoutSmall(QuestTreeStructure structure) {
        List<Placed> placed = new ArrayList<>();
        List<Lane> lanes = new ArrayList<>();
        List<SubBranchBounds> subs = new ArrayList<>();
        int top = MARGIN_Y, width = MIN_WIDTH;
        for (int laneIndex = 0; laneIndex < structure.chapters().size(); laneIndex++) {
            var chapter = structure.chapters().get(laneIndex);
            List<String> ids = chapter.nodeIds();
            int count = ids.size();
            int columns = Math.max(1, Math.min(count, MAX_COLUMNS));
            int rows = Math.max(1, (count + columns - 1) / columns);
            int height = Math.max(LANE_MIN_HEIGHT, LANE_PADDING_Y * 2 + (rows - 1) * ROW_SPACING);
            int firstY = top + height / 2 - (rows - 1) * ROW_SPACING / 2;
            String subId = chapter.subBranches().isEmpty() ? chapter.id() : chapter.subBranches().get(0).id();
            for (int i = 0; i < count; i++) {
                String sub = subOf(chapter, ids.get(i), subId);
                placed.add(new Placed(ids.get(i), chapter.id(), sub, laneIndex, i,
                        GUTTER + FIRST_NODE_OFFSET + NODE_SPACING * (i % columns), firstY + ROW_SPACING * (i / columns)));
            }
            width = Math.max(width, GUTTER + FIRST_NODE_OFFSET + NODE_SPACING * (columns - 1) + FIRST_NODE_OFFSET / 2 + TRAILING);
            lanes.add(new Lane(chapter.id(), chapter.titleKey(), top, height, count));
            for (var sub : chapter.subBranches()) {
                int subColumns = Math.max(1, Math.min(sub.nodeIds().size(), MAX_COLUMNS));
                int subRows = Math.max(1, (sub.nodeIds().size() + subColumns - 1) / subColumns);
                subs.add(new SubBranchBounds(sub.id(), chapter.id(), sub.titleKey(), GUTTER, top, width - GUTTER, height,
                        firstY, subRows, subColumns, sub.nodeIds().size()));
            }
            top += height + LANE_GAP;
        }
        int height = lanes.isEmpty() ? MARGIN_Y * 2 : lanes.get(lanes.size() - 1).bottom() + MARGIN_Y;
        return new Result(List.copyOf(placed), List.copyOf(lanes), List.copyOf(subs), width, height, false);
    }

    /** Sub-branches stack inside a lane, each with a caption band and its own rows. */
    private static Result layoutLarge(QuestTreeStructure structure) {
        List<Placed> placed = new ArrayList<>();
        List<Lane> lanes = new ArrayList<>();
        List<SubBranchBounds> subs = new ArrayList<>();
        int top = MARGIN_Y, width = MIN_WIDTH;
        for (int laneIndex = 0; laneIndex < structure.chapters().size(); laneIndex++) {
            var chapter = structure.chapters().get(laneIndex);
            int cursor = top + LANE_PADDING_Y;
            int laneNodes = 0;
            for (var sub : chapter.subBranches()) {
                List<String> ids = sub.nodeIds();
                int count = ids.size();
                if (count == 0) continue;
                int columns = Math.min(count, MAX_COLUMNS);
                int rows = (count + columns - 1) / columns;
                int captionTop = cursor;
                int firstRowY = captionTop + CAPTION_HEIGHT + ROW_SPACING / 2;
                for (int i = 0; i < count; i++) {
                    placed.add(new Placed(ids.get(i), chapter.id(), sub.id(), laneIndex, laneNodes + i,
                            GUTTER + FIRST_NODE_OFFSET + NODE_SPACING * (i % columns),
                            firstRowY + ROW_SPACING * (i / columns)));
                }
                int subHeight = CAPTION_HEIGHT + rows * ROW_SPACING;
                subs.add(new SubBranchBounds(sub.id(), chapter.id(), sub.titleKey(), GUTTER, captionTop,
                        FIRST_NODE_OFFSET + NODE_SPACING * (columns - 1) + FIRST_NODE_OFFSET, subHeight,
                        firstRowY, rows, columns, count));
                width = Math.max(width, GUTTER + FIRST_NODE_OFFSET + NODE_SPACING * (columns - 1) + FIRST_NODE_OFFSET / 2 + TRAILING);
                cursor = captionTop + subHeight + SUB_GAP;
                laneNodes += count;
            }
            int laneHeight = Math.max(LANE_MIN_HEIGHT, cursor - SUB_GAP + LANE_PADDING_Y - top);
            lanes.add(new Lane(chapter.id(), chapter.titleKey(), top, laneHeight, laneNodes));
            top += laneHeight + LANE_GAP;
        }
        int height = lanes.isEmpty() ? MARGIN_Y * 2 : lanes.get(lanes.size() - 1).bottom() + MARGIN_Y;
        return new Result(List.copyOf(placed), List.copyOf(lanes), List.copyOf(subs), width, height, true);
    }

    private static String subOf(QuestTreeStructure.Chapter chapter, String id, String fallback) {
        for (var sub : chapter.subBranches()) if (sub.nodeIds().contains(id)) return sub.id();
        return fallback;
    }
}
