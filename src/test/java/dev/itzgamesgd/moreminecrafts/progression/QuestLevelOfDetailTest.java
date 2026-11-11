package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.itzgamesgd.moreminecrafts.progression.QuestLevelOfDetail.Level;
import org.junit.jupiter.api.Test;

class QuestLevelOfDetailTest {
    @Test
    void zoomCrossesEachThresholdInOrder() {
        Level level = Level.SUB_BRANCH;
        level = QuestLevelOfDetail.next(level, 0.10);
        assertEquals(Level.CHAPTER_INDEX, level);
        level = QuestLevelOfDetail.next(level, 0.35);
        assertEquals(Level.SUB_BRANCH, level);
        level = QuestLevelOfDetail.next(level, 0.80);
        assertEquals(Level.QUEST, level);
    }

    @Test
    void hysteresisStopsFlickerNearEachThreshold() {
        // Oscillating inside a band must not change level, whichever side the camera started on.
        for (double zoom : new double[] {0.23, 0.25, 0.27, 0.29}) {
            assertEquals(Level.CHAPTER_INDEX, QuestLevelOfDetail.next(Level.CHAPTER_INDEX, zoom), "index at " + zoom);
            assertEquals(Level.SUB_BRANCH, QuestLevelOfDetail.next(Level.SUB_BRANCH, zoom), "sub at " + zoom);
        }
        for (double zoom : new double[] {0.49, 0.52, 0.54}) {
            assertEquals(Level.QUEST, QuestLevelOfDetail.next(Level.QUEST, zoom), "quest at " + zoom);
            assertEquals(Level.SUB_BRANCH, QuestLevelOfDetail.next(Level.SUB_BRANCH, zoom), "sub at " + zoom);
        }
    }

    @Test
    void oscillationInsideABandNeverChangesLevel() {
        Level index = Level.CHAPTER_INDEX;
        Level quest = Level.QUEST;
        for (int i = 0; i <= 400; i++) {
            double wiggle = Math.sin(i * 0.05);
            index = QuestLevelOfDetail.next(index, 0.26 + 0.03 * wiggle);   // stays within 0.23..0.29
            quest = QuestLevelOfDetail.next(quest, 0.515 + 0.02 * wiggle);  // stays within 0.495..0.535
            assertEquals(Level.CHAPTER_INDEX, index, "index flickered at step " + i);
            assertEquals(Level.QUEST, quest, "quest flickered at step " + i);
        }
    }

    @Test
    void monotoneSweepCrossesEachBoundaryExactlyOnce() {
        Level level = Level.CHAPTER_INDEX;
        int changes = 0;
        for (int i = 0; i <= 1000; i++) {
            Level next = QuestLevelOfDetail.next(level, 0.1 + 0.9 * i / 1000.0);
            if (next != level) changes++;
            level = next;
        }
        assertEquals(2, changes, "rising sweep should cross index->sub and sub->quest once each");
        assertEquals(Level.QUEST, level);
    }

    @Test
    void selectingAChapterSummaryZoomsIntoItsSubBranches() {
        assertEquals(Level.SUB_BRANCH, QuestLevelOfDetail.next(Level.CHAPTER_INDEX, QuestLevelOfDetail.focusZoom(Level.SUB_BRANCH)));
        assertEquals(Level.QUEST, QuestLevelOfDetail.next(Level.SUB_BRANCH, QuestLevelOfDetail.focusZoom(Level.QUEST)));
    }
}
