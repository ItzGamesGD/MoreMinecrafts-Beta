package dev.itzgamesgd.moreminecrafts.skytower;

/** Shared, irreversible combat ledger. Rewards remain owned by vanilla vault/spawner data.
 * Calls are server-thread only. An adapter must supply real vanilla trial-cycle completion,
 * never mob absence, chunk unload, or player departure. No such adapter is guessed here.
 */
public final class SkyTowerProgress {
    private int completedMask;
    public SkyTowerProgress(int completedMask) {
        if (completedMask < 0 || completedMask > 0xffff) throw new IllegalArgumentException("invalid completion mask");
        this.completedMask = completedMask;
    }
    public int completedMask() { return completedMask; }
    public boolean completeVanillaTrial(int spawnerIndex) {
        if (spawnerIndex < 0 || spawnerIndex >= 16) throw new IllegalArgumentException("invalid spawner anchor");
        int bit = 1 << spawnerIndex;
        if ((completedMask & bit) != 0) return false;
        completedMask |= bit;
        return true;
    }
    public boolean floorCleared(int floor) {
        if (floor < 0 || floor >= 4) throw new IllegalArgumentException("invalid floor");
        int quorum = 15 << (floor * 4);
        return (completedMask & quorum) == quorum;
    }
    /** Combat fact only; sanctum arrival / overall-content completion is a separate undecided policy. */
    public boolean allFloorsCleared() { return completedMask == 0xffff; }
}
