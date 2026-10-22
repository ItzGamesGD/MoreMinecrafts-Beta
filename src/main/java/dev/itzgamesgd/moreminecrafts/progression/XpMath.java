package dev.itzgamesgd.moreminecrafts.progression;

/**
 * Pure vanilla experience curve, using the same per-level requirement as
 * {@code Player.getXpNeededForNextLevel} in Minecraft 26.3. Integer-only, so totals convert to
 * (level, points) and back without the float drift of {@code giveExperiencePoints}.
 */
public final class XpMath {
    /** Highest level whose cumulative total still fits in an int. */
    public static final int MAX_LEVEL = 21_863;

    public record Split(int level, int points) {}

    private XpMath() {}

    public static int xpNeededForNextLevel(int level) {
        if (level >= 30) return 112 + (level - 30) * 9;
        return level >= 15 ? 37 + (level - 15) * 5 : 7 + level * 2;
    }

    /** Cumulative raw XP needed to reach {@code level} with an empty bar. */
    public static long totalForLevel(int level) {
        long l = Math.max(0, level);
        if (l <= 16) return l * l + 6 * l;
        if (l <= 31) return (5 * l * l - 81 * l + 720) / 2;
        return (9 * l * l - 325 * l + 4440) / 2;
    }

    /** Splits a raw total into the highest reachable level and the leftover points on its bar. */
    public static Split split(long total) {
        long clamped = Math.max(0, Math.min(total, Integer.MAX_VALUE));
        int low = 0, high = MAX_LEVEL;
        while (low < high) {
            int mid = (low + high + 1) >>> 1;
            if (totalForLevel(mid) <= clamped) low = mid; else high = mid - 1;
        }
        return new Split(low, (int) (clamped - totalForLevel(low)));
    }
}
