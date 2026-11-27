package dev.itzgamesgd.moreminecrafts.progression;

/** Approved Stage 23-A values; durations use the existing nearest-tick rounding convention. */
public final class ResearchBatch2Rules {
    private ResearchBatch2Rules() {}
    public static boolean preventSpiderAction(int level, boolean spiderFamily) {
        return level > 0 && spiderFamily;
    }
    public static boolean preventEngagedTeleport(int level, boolean engaged) {
        return level > 0 && engaged;
    }
    public static int fuseDelay(int level) {
        return switch (Math.clamp(level, 0, 3)) { case 1 -> 6; case 2 -> 12; case 3 -> 20; default -> 0; };
    }
    public static int flightLifetime(int baseline, int level) {
        return ResearchPassiveMath.increasedDuration(baseline, Math.clamp(level, 0, 3) * 8);
    }
    public static int effectDuration(int baseline, int level, boolean beneficial, boolean harmful) {
        if (baseline <= 0 || level <= 0 || !beneficial && !harmful) return baseline;
        int percent = Math.clamp(level, 0, 3) * 5;
        if (beneficial) return ResearchPassiveMath.increasedDuration(baseline, percent);
        return Math.max(1, (int) Math.round(baseline * (100 - percent) / 100.0D));
    }
}
