package dev.itzgamesgd.moreminecrafts.progression;

/**
 * Semantic zoom for large quest graphs. Each level is entered and left at different zoom values (hysteresis),
 * so a camera hovering on a threshold never flickers between two levels.
 *
 * <ul>
 *   <li>{@link Level#CHAPTER_INDEX}: a readable chapter index; no node frames are drawn.</li>
 *   <li>{@link Level#SUB_BRANCH}: sub-branch boxes and captions, with simplified node markers.</li>
 *   <li>{@link Level#QUEST}: full quest nodes, their state and presentation edges.</li>
 * </ul>
 */
public final class QuestLevelOfDetail {
    public enum Level { CHAPTER_INDEX, SUB_BRANCH, QUEST }

    /** Falling below this zoom enters the chapter index. */
    public static final double INDEX_ENTER = 0.22;
    /** Rising above this zoom leaves the chapter index. */
    public static final double INDEX_EXIT = 0.30;
    /** Reaching this zoom enters full quest nodes. */
    public static final double QUEST_ENTER = 0.55;
    /** Falling below this zoom leaves full quest nodes. */
    public static final double QUEST_EXIT = 0.48;

    private QuestLevelOfDetail() {}

    public static Level next(Level current, double zoom) {
        return switch (current) {
            case CHAPTER_INDEX -> zoom >= INDEX_EXIT ? (zoom >= QUEST_ENTER ? Level.QUEST : Level.SUB_BRANCH) : Level.CHAPTER_INDEX;
            case SUB_BRANCH -> zoom < INDEX_ENTER ? Level.CHAPTER_INDEX : (zoom >= QUEST_ENTER ? Level.QUEST : Level.SUB_BRANCH);
            case QUEST -> zoom >= QUEST_EXIT ? Level.QUEST : (zoom < INDEX_ENTER ? Level.CHAPTER_INDEX : Level.SUB_BRANCH);
        };
    }

    /** The zoom a summary selection focuses on: inside the sub-branch band, away from both thresholds. */
    public static double focusZoom(Level target) {
        return switch (target) {
            case CHAPTER_INDEX -> INDEX_EXIT;
            case SUB_BRANCH -> (INDEX_EXIT + QUEST_EXIT) / 2.0;
            case QUEST -> QUEST_ENTER;
        };
    }
}
