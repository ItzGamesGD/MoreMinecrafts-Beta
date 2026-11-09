package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.junit.jupiter.api.Test;

/** D3: sub-branch rows, caption bands and bounds for large graphs. */
class QuestLargeLayoutTest {
    private static QuestTreeStructure sample() {
        // Sizes chosen to force multi-row sub-branches, a 1-node sub-branch and a 30+ node sub-branch.
        int[][] sizes = {{30, 1, 7}, {4, 9}};
        List<QuestTreeStructure.Chapter> chapters = new ArrayList<>();
        int next = 0;
        for (int c = 0; c < sizes.length; c++) {
            List<QuestTreeStructure.SubBranch> subs = new ArrayList<>();
            for (int s = 0; s < sizes[c].length; s++) {
                List<String> ids = new ArrayList<>();
                for (int i = 0; i < sizes[c][s]; i++) ids.add("q" + (next++));
                subs.add(new QuestTreeStructure.SubBranch("c" + c + ".s" + s, "k", ids));
            }
            chapters.add(new QuestTreeStructure.Chapter("c" + c, "k" + c, subs));
        }
        return new QuestTreeStructure(chapters, List.of());
    }

    @Test
    void largeGraphsUseTheCaptionLayoutAndSmallGraphsKeepTheOriginal() {
        assertTrue(QuestTreeLayout.layout(sample()).large());
        assertFalse(QuestTreeLayout.layout(QuestTreeStructure.current()).large());
    }

    @Test
    void eachSubBranchStartsOnANewRowBelowThePreviousOne() {
        var result = QuestTreeLayout.layout(sample());
        Map<String, Integer> maxY = new HashMap<>();
        Map<String, Integer> minY = new HashMap<>();
        for (var node : result.nodes()) {
            maxY.merge(node.subBranchId(), node.y(), Math::max);
            minY.merge(node.subBranchId(), node.y(), Math::min);
        }
        var subs = result.subBranches();
        for (int i = 1; i < subs.size(); i++) {
            var previous = subs.get(i - 1);
            var current = subs.get(i);
            if (!previous.chapterId().equals(current.chapterId())) continue;
            assertTrue(current.y() >= previous.bottom(), "sub-branch " + current.id() + " overlaps the previous one");
            assertTrue(minY.get(current.id()) > maxY.get(previous.id()), "rows of " + current.id() + " are not separate");
        }
    }

    @Test
    void captionBandsNeverContainANodeFrame() {
        var result = QuestTreeLayout.layout(sample());
        for (var sub : result.subBranches()) {
            int captionBottom = sub.y() + QuestTreeLayout.CAPTION_HEIGHT;
            for (var node : result.nodes()) {
                if (!node.subBranchId().equals(sub.id())) continue;
                assertTrue(node.y() - 13 >= captionBottom, "node " + node.id() + " reaches into the caption of " + sub.id());
            }
        }
    }

    @Test
    void everyNodeLiesInsideItsOwnSubBranchBounds() {
        var result = QuestTreeLayout.layout(sample());
        Map<String, QuestTreeLayout.SubBranchBounds> byId = new HashMap<>();
        for (var sub : result.subBranches()) byId.put(sub.id(), sub);
        for (var node : result.nodes()) {
            var sub = byId.get(node.subBranchId());
            assertTrue(node.x() >= sub.x() && node.x() <= sub.right(), node.id() + " outside its sub-branch horizontally");
            assertTrue(node.y() - 13 >= sub.y() && node.y() + 13 <= sub.bottom(), node.id() + " outside its sub-branch vertically");
        }
    }

    @Test
    void presentationOrderAndSubBranchMembershipArePreserved() {
        var structure = sample();
        var result = QuestTreeLayout.layout(structure);
        assertEquals(structure.allNodeIds(), result.nodes().stream().map(QuestTreeLayout.Placed::id).toList());
        for (var node : result.nodes()) assertFalse(node.subBranchId().isEmpty());
        assertEquals(structure.allNodeIds().size(), new java.util.HashSet<>(result.nodes().stream().map(QuestTreeLayout.Placed::id).toList()).size());
    }

    @Test
    void canvasBoundsCoverEverySubBranch() {
        var result = QuestTreeLayout.layout(sample());
        for (var sub : result.subBranches()) {
            assertTrue(sub.right() <= result.width(), sub.id() + " extends past the canvas width");
            assertTrue(sub.bottom() <= result.height(), sub.id() + " extends past the canvas height");
        }
    }
}
