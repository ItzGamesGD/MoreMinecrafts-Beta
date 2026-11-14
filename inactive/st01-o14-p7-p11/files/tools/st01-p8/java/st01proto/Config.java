package st01proto;

/** Timer length is UNAPPROVED (O-1). Production defaults are deliberately unset; the coordinator refuses to run without an explicit TEST-ONLY value. */
public record Config(long timerTicks, int maxSpawnAttempts, boolean respawnAfterRestart, boolean testOnly) {
    public static Config productionDefaults() { return new Config(-1, 1, false, false); }
    /** TEST-ONLY value; never a default. */
    public static Config testOnly(long timerTicks) { return new Config(timerTicks, 1, false, true); }
    public Config withRespawn(boolean v) { return new Config(timerTicks, maxSpawnAttempts, v, testOnly); }
    public Config withAttempts(int n) { return new Config(timerTicks, n, respawnAfterRestart, testOnly); }
}
