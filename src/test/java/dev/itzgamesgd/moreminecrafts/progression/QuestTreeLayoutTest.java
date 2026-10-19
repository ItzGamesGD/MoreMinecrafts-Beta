package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import org.junit.jupiter.api.Test;

class QuestTreeLayoutTest {
    @Test
    void threeQuestsShareOneCompactRow() {
        var points = QuestTreeLayout.grid(3);
        assertEquals(3, points.size());
        assertEquals(1, points.stream().map(QuestTreeLayout.Point::y).distinct().count());
        assertEquals(QuestTreeLayout.SPACING, points.get(1).x() - points.get(0).x());
        assertEquals(QuestTreeLayout.SPACING, points.get(2).x() - points.get(1).x());
        // 26px frames never touch at the minimum spacing.
        assertTrue(QuestTreeLayout.SPACING >= 26 + 16);
    }

    @Test
    void largerCatalogsWrapIntoRowsWithoutOverlap() {
        var points = QuestTreeLayout.grid(10);
        assertEquals(10, points.size());
        assertEquals(QuestTreeLayout.MAX_COLUMNS, points.stream().mapToInt(QuestTreeLayout.Point::column).max().getAsInt() + 1);
        assertEquals(3, points.stream().mapToInt(QuestTreeLayout.Point::row).max().getAsInt() + 1);
        var seen = new HashSet<String>();
        for (var point : points) assertTrue(seen.add(point.x() + "," + point.y()));
    }

    @Test
    void emptyAndSingleCountsAreStable() {
        assertTrue(QuestTreeLayout.grid(0).isEmpty());
        assertEquals(1, QuestTreeLayout.grid(1).size());
        assertEquals(1, QuestTreeLayout.columnsFor(0));
    }
}
