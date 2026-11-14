package dev.itzgamesgd.moreminecrafts.mob.skyflyer;

/** Pure end-of-chase rule for the Sky Spirit: the chase ends when sight is lost for long enough or the attacker is far. */
public final class SpiritPursuit {
    private SpiritPursuit() {}

    /**
     * @param targetValid     the attacker still exists, is alive and may still be attacked
     * @param distanceSqr     squared distance to the attacker
     * @param unseenTicks     consecutive ticks without line of sight
     */
    public static boolean shouldDisengage(boolean targetValid, double distanceSqr, int unseenTicks) {
        double max = SkyFlyerTuning.SPIRIT_DISENGAGE_DISTANCE;
        return !targetValid || distanceSqr > max * max || unseenTicks >= SkyFlyerTuning.SPIRIT_LOST_SIGHT_TICKS;
    }
}
