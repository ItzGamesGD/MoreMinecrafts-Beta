package dev.itzgamesgd.moreminecrafts.fishing;

import net.minecraft.util.RandomSource;

public final class FishingTiming {
    public static final int TIMEOUT_TICKS = 160;
    private static final double[] HALF_CYCLE_TICKS = {0, 36, 30, 25, 20, 16};
    private FishingTiming() {}

    public enum Zone {
        LIGHT_GREEN(100), YELLOW(45), ORANGE(15), RED(0);
        private final int capturePercent;
        Zone(int capturePercent) { this.capturePercent = capturePercent; }
        public int capturePercent() { return capturePercent; }
    }

    public static double oneWaySeconds(int difficulty) { return halfCycleTicks(difficulty) / 20.0; }
    public static int halfCycleTicks(int difficulty) {
        if (difficulty < 1 || difficulty > 5) throw new IllegalArgumentException("Difficulty must be 1-5");
        return (int) HALF_CYCLE_TICKS[difficulty];
    }
    public static double position(long elapsedTicks, int difficulty) {
        int half = halfCycleTicks(difficulty);
        long phase = Math.floorMod(elapsedTicks, half * 2L);
        return phase <= half ? phase / (double) half : (half * 2L - phase) / (double) half;
    }
    public static Zone zone(double position) {
        double distance = Math.abs(position - 0.5);
        if (distance <= 0.08) return Zone.LIGHT_GREEN;
        if (distance <= 0.18) return Zone.YELLOW;
        if (distance <= 0.32) return Zone.ORANGE;
        return Zone.RED;
    }
    public static boolean captures(Zone zone, RandomSource random) {
        return zone.capturePercent == 100 || (zone.capturePercent > 0 && random.nextInt(100) < zone.capturePercent);
    }
    public static boolean timedOut(long elapsedTicks) { return elapsedTicks >= TIMEOUT_TICKS; }
}
