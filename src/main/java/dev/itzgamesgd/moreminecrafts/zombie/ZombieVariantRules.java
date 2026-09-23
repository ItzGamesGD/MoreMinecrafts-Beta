package dev.itzgamesgd.moreminecrafts.zombie;

/** Pure, deterministic rules shared by the two Stage 8A zombie variants. */
public final class ZombieVariantRules {
    public static final double CUSTOM_CHANCE = 0.15D;
    public static final double BOMB_RADIUS = 3.0D;
    public static final float BOMB_DAMAGE = 4.0F;
    public static final double LEAP_MIN_DISTANCE = 3.5D;
    public static final double LEAP_MAX_DISTANCE = 8.0D;
    public static final int LEAP_COOLDOWN_TICKS = 120;
    public static final float LEAP_DAMAGE = 2.0F;

    private ZombieVariantRules() {
    }

    public static ZombieVariant select(boolean natural, ZombieVariant existing,
                                       double customRoll, double variantRoll) {
        if (existing != null) {
            return existing;
        }
        if (!natural || customRoll >= CUSTOM_CHANCE) {
            return ZombieVariant.NORMAL;
        }
        return variantRoll < 0.5D ? ZombieVariant.BOMB : ZombieVariant.LEAP;
    }

    public static float bombDamage(double distance) {
        if (distance > BOMB_RADIUS) {
            return 0.0F;
        }
        double falloff = Math.max(0.3D, 1.0D - (distance / BOMB_RADIUS) * 0.7D);
        return (float) (BOMB_DAMAGE * falloff);
    }

    public static boolean canTriggerLeap(double distance, long gameTick, long readyTick) {
        return distance >= LEAP_MIN_DISTANCE && distance <= LEAP_MAX_DISTANCE && gameTick >= readyTick;
    }

    public static long nextLeapTick(long launchTick) {
        return launchTick + LEAP_COOLDOWN_TICKS;
    }
}
