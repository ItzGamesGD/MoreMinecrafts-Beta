package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class QuestTreeLayoutTest {
    private static QuestTreeStructure.Chapter chapter(String id, int count) {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) ids.add(id + "." + i);
        return QuestTreeStructure.Chapter.of(id, "key." + id, ids);
    }

    @Test
    void currentSliceIsOneEcologyLaneWithNoInventedDependencies() {
        var structure = QuestTreeStructure.current();
        assertEquals(1, structure.chapters().size());
        assertEquals(3, structure.allNodeIds().size());
        assertTrue(structure.edges().isEmpty(), "no prerequisite exists between the current quests");
        assertTrue(structure.validate().isEmpty());
        var result = QuestTreeLayout.layout(structure);
        assertEquals(1, result.nodes().stream().map(QuestTreeLayout.Placed::y).distinct().count());
        assertEquals(QuestTreeLayout.NODE_SPACING, result.nodes().get(1).x() - result.nodes().get(0).x());
        assertTrue(result.width() >= QuestTreeLayout.MIN_WIDTH);
    }

    @Test
    void nodesStartRightOfTheLabelGutterAndStayInsideTheirLane() {
        var result = QuestTreeLayout.layout(new QuestTreeStructure(List.of(chapter("a", 8)), List.of()));
        var lane = result.lanes().get(0);
        for (var node : result.nodes()) {
            assertTrue(node.x() >= QuestTreeLayout.GUTTER + 26);
            assertTrue(node.y() - 13 >= lane.top() && node.y() + 13 <= lane.bottom());
            assertTrue(node.x() + 13 <= result.width());
        }
    }

    @Test
    void manyChaptersStackWithoutOverlapAndGrowTheCanvas() {
        var structure = new QuestTreeStructure(List.of(chapter("a", 3), chapter("b", 14), chapter("c", 1)), List.of());
        var result = QuestTreeLayout.layout(structure);
        assertEquals(3, result.lanes().size());
        for (int i = 1; i < result.lanes().size(); i++)
            assertTrue(result.lanes().get(i).top() >= result.lanes().get(i - 1).bottom() + QuestTreeLayout.LANE_GAP);
        assertTrue(result.height() > result.lanes().get(2).bottom());
        var seen = new HashSet<String>();
        for (var node : result.nodes()) assertTrue(seen.add(node.x() + "," + node.y()), "overlap at " + node);
        assertEquals(18, result.nodes().size());
        assertTrue(result.nodes().stream().filter(n -> n.chapterId().equals("b")).mapToInt(QuestTreeLayout.Placed::y)
                .distinct().count() > 1, "wide chapters wrap into rows");
    }

    @Test
    void emptyStructureStillProducesACanvas() {
        var result = QuestTreeLayout.layout(new QuestTreeStructure(List.of(), List.of()));
        assertTrue(result.nodes().isEmpty());
        assertTrue(result.width() > 0 && result.height() > 0);
    }

    @Test
    void validationRejectsUnknownSelfAndCyclicDependencies() {
        var chapters = List.of(chapter("a", 3));
        assertFalse(new QuestTreeStructure(chapters, List.of(new QuestTreeStructure.Edge("a.0", "zzz"))).validate().isEmpty());
        assertFalse(new QuestTreeStructure(chapters, List.of(new QuestTreeStructure.Edge("a.0", "a.0"))).validate().isEmpty());
        assertFalse(new QuestTreeStructure(chapters, List.of(new QuestTreeStructure.Edge("a.0", "a.1"),
                new QuestTreeStructure.Edge("a.1", "a.2"), new QuestTreeStructure.Edge("a.2", "a.0"))).validate().isEmpty());
        assertTrue(new QuestTreeStructure(chapters, List.of(new QuestTreeStructure.Edge("a.0", "a.1"),
                new QuestTreeStructure.Edge("a.1", "a.2"))).validate().isEmpty());
    }
}
