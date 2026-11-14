package dev.itzgamesgd.moreminecrafts.pyramid.entrance;

import org.jetbrains.annotations.Nullable;

/**
 * ST-01 secret entrance runtime parameters.
 *
 * <p>Owner-approved production defaults (ST-01 P11, 2026-11-08):
 * <ul>
 *   <li><b>O-1</b>: {@code fallbackTicks = 600L} (30.0 s at 20 TPS)</li>
 *   <li><b>O-3</b>: fallback timer starts immediately when valid Tomb Guardian victory is recorded</li>
 *   <li><b>O-4</b>: {@code arrivalRadius = 1.5D} blocks (horizontal {@code dx, dz} distance to stop point center)</li>
 * </ul>
 *
 * <p>{@link #PRODUCTION} is installed by default so the production runtime progresses registered ST-01
 * instances without test scaffolding. Tests that require custom timing, radius, or disabled runtime
 * behavior may install {@link #testOnly(long, double)} or {@code null} and restore {@link #PRODUCTION}
 * via {@link #resetToProduction()}.
 */
public record EntranceConfig(long fallbackTicks, double arrivalRadius, int maxSpawnAttempts, boolean respawnAfterRestart, boolean testOnly) {
    /** Owner-approved O-1 fallback opening delay: 600 game ticks (30.0 s). */
    public static final long APPROVED_FALLBACK_TICKS = 600L;
    /** Owner-approved O-4 horizontal arrival radius around the stop point center: 1.5 blocks. */
    public static final double APPROVED_ARRIVAL_RADIUS = 1.5D;
    /** Approved maximum spawn attempts per victory lifecycle. */
    public static final int APPROVED_MAX_SPAWN_ATTEMPTS = 1;

    /** Owner-approved ST-01 P11 production configuration. */
    public static final EntranceConfig PRODUCTION = new EntranceConfig(
            APPROVED_FALLBACK_TICKS,
            APPROVED_ARRIVAL_RADIUS,
            APPROVED_MAX_SPAWN_ATTEMPTS,
            false,
            false);

    private static volatile EntranceConfig current = PRODUCTION;

    @Nullable public static EntranceConfig current() { return current; }
    public static void install(@Nullable EntranceConfig config) { current = config; }
    public static void resetToProduction() { current = PRODUCTION; }

    /** TEST-ONLY values; separated from {@link #PRODUCTION} via {@code testOnly = true}. */
    public static EntranceConfig testOnly(long fallbackTicks, double arrivalRadius) {
        return new EntranceConfig(fallbackTicks, arrivalRadius, APPROVED_MAX_SPAWN_ATTEMPTS, false, true);
    }

    public EntranceConfig withRespawnAfterRestart(boolean v) { return new EntranceConfig(fallbackTicks, arrivalRadius, maxSpawnAttempts, v, testOnly); }
}
