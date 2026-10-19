package dev.itzgamesgd.moreminecrafts.progression;

import java.util.ArrayList;
import java.util.List;

/**
 * Compact row-major grid for the XP Quest tree. Positions are presentation-only world coordinates:
 * the grid implies no prerequisite between neighbouring quests.
 */
public final class QuestTreeLayout {
    /** World-unit distance between neighbouring node centres (a 26px frame leaves a clear gap). */
    public static final int SPACING = 64;
    public static final int ORIGIN_X = 96;
    public static final int ORIGIN_Y = 104;
    public static final int MAX_COLUMNS = 4;
    /** Padding between the outermost node centres and the drawn group panel. */
    public static final int GROUP_PADDING_X = 32;
    public static final int GROUP_PADDING_Y = 24;
    public static final int GROUP_LABEL_HEIGHT = 12;

    public record Point(int x, int y, int column, int row) {}

    private QuestTreeLayout() {}

    public static int columnsFor(int count) {
        return Math.max(1, Math.min(count, MAX_COLUMNS));
    }

    public static List<Point> grid(int count) {
        int columns = columnsFor(count);
        List<Point> points = new ArrayList<>(Math.max(0, count));
        for (int i = 0; i < count; i++) {
            int column = i % columns, row = i / columns;
            points.add(new Point(ORIGIN_X + SPACING * column, ORIGIN_Y + SPACING * row, column, row));
        }
        return List.copyOf(points);
    }
}
