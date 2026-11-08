package dev.itzgamesgd.moreminecrafts.st01;

import org.jetbrains.annotations.Nullable;

/**
 * Tunable, UNAPPROVED entrance parameters. There is deliberately NO production default: with no config installed the runtime is disabled
 * (it registers nothing that progresses). O-1 (fallback time) is not approved; the arrival radius (O-4) is not approved either.
 * Tests install explicitly TEST-ONLY values.
 */
public record EntranceConfig(long fallbackTicks, double arrivalRadius, int maxSpawnAttempts, boolean respawnAfterRestart, boolean testOnly) {
    private static volatile EntranceConfig current;

    @Nullable public static EntranceConfig current() { return current; }
    public static void install(@Nullable EntranceConfig config) { current = config; }

    /** TEST-ONLY values; never a default and never to be shipped. */
    public static EntranceConfig testOnly(long fallbackTicks, double arrivalRadius) {
        return new EntranceConfig(fallbackTicks, arrivalRadius, 1, false, true);
    }

    public EntranceConfig withRespawnAfterRestart(boolean v) { return new EntranceConfig(fallbackTicks, arrivalRadius, maxSpawnAttempts, v, testOnly); }
}
