package dev.itzgamesgd.moreminecrafts.colosseum;

/**
 * The exactly-8 conditional Q109 candidates. They are recorded for completeness only: each stays out of the
 * active pool until its own technical and live gate passes, and this controller has no way to allocate them.
 */
public enum ColosseumConditionalCandidate {
    SPIDER(3),
    CAVE_SPIDER(2),
    SMALL_SPIDER(2),
    LARGE_SPIDER(5),
    SILVERFISH(1),
    LEAP_ZOMBIE(4),
    BOMB_ZOMBIE(4),
    MINING_GIANT(9);

    private final int provisionalThreat;

    ColosseumConditionalCandidate(int provisionalThreat) { this.provisionalThreat = provisionalThreat; }

    public int provisionalThreat() { return provisionalThreat; }

    /** No activation gate has passed; never true in this stage. */
    public boolean active() { return false; }
}
