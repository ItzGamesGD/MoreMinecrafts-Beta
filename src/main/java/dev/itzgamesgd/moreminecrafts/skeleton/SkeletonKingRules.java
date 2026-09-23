package dev.itzgamesgd.moreminecrafts.skeleton;

/** Pure tuning and scheduling rules for the one concrete Skeleton King encounter. */
public final class SkeletonKingRules {
    public enum Ability { BARRAGE, GUARDS, CURSE }

    public static final String IDENTITY_SAVE_KEY = "MoreMinecraftsSkeletonKing";
    public static final String GUARD_OWNER_SAVE_KEY = "MoreMinecraftsSkeletonKingGuardOwner";
    public static final int BARRAGE_COOLDOWN = 120;
    public static final int GUARD_COOLDOWN = 200;
    public static final int CURSE_COOLDOWN = 160;
    public static final int BARRAGE_ARROWS = 8;
    public static final double BARRAGE_SPREAD_DEGREES = 30.0D;
    public static final int GUARDS_PER_CAST = 3;
    public static final int MAX_LIVING_GUARDS = 6;
    public static final double PARTICIPATION_RANGE = 32.0D;
    public static final double CURSE_RANGE = 12.0D;

    private SkeletonKingRules() {
    }

    public static boolean ready(long tick, long readyTick) {
        return tick >= readyTick;
    }

    public static long nextReady(Ability ability, long tick) {
        return tick + switch (ability) {
            case BARRAGE -> BARRAGE_COOLDOWN;
            case GUARDS -> GUARD_COOLDOWN;
            case CURSE -> CURSE_COOLDOWN;
        };
    }

    public static Ability select(int cursor, long tick, long barrageReady, long guardsReady,
                                 long curseReady, boolean targetValid, int livingGuards,
                                 boolean cursePlayerNearby) {
        if (!targetValid) return null;
        Ability[] order = Ability.values();
        for (int offset = 0; offset < order.length; offset++) {
            Ability candidate = order[Math.floorMod(cursor + offset, order.length)];
            if (candidate == Ability.BARRAGE && ready(tick, barrageReady)) return candidate;
            if (candidate == Ability.GUARDS && livingGuards <= MAX_LIVING_GUARDS - GUARDS_PER_CAST
                    && ready(tick, guardsReady)) return candidate;
            if (candidate == Ability.CURSE && cursePlayerNearby && ready(tick, curseReady)) return candidate;
        }
        return null;
    }

    public static double barrageYawOffset(int arrowIndex) {
        if (arrowIndex < 0 || arrowIndex >= BARRAGE_ARROWS) throw new IllegalArgumentException("arrowIndex");
        return -BARRAGE_SPREAD_DEGREES / 2.0D
                + arrowIndex * BARRAGE_SPREAD_DEGREES / (BARRAGE_ARROWS - 1);
    }

    public static boolean eligiblePlayer(double distance, boolean alive, boolean spectator) {
        return alive && !spectator && distance <= CURSE_RANGE;
    }

    public static boolean shouldCleanup(boolean king, boolean destroyed) {
        return king && destroyed;
    }
}
