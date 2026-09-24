package dev.itzgamesgd.moreminecrafts.alchemy;

/** Deterministic timing contract for the Shock status effect. */
public final class ShockTiming {
    public static final int DURATION_TICKS = 1_200;
    public static final int PULSE_INTERVAL_TICKS = 320;
    public static final int ROOT_DURATION_TICKS = 20;
    public static final float PULSE_DAMAGE = 1.0F;

    private ShockTiming() {}

    /**
     * Uses the persisted vanilla effect duration as the clock. Reapplying a full-duration
     * effect therefore restarts the clock rather than creating another pulse sequence.
     */
    public static boolean shouldPulse(int remainingTicks) {
        int elapsedTicks = DURATION_TICKS - remainingTicks;
        return elapsedTicks > 0 && elapsedTicks % PULSE_INTERVAL_TICKS == 0;
    }
}
