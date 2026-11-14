package dev.itzgamesgd.moreminecrafts.mob.skyflyer;

/**
 * Pure state machine for the Storm Hunter loop (design section 3): circle, wind shot, dive, rise, then circle again.
 * It holds no Minecraft state so the order, timing and reset rules can be unit tested; {@link StormHunter} feeds it
 * the facts it cannot know (whether the dive connected or reached the target, and the current height).
 */
public final class HunterPattern {
    public enum Phase { CIRCLE, SHOOT, DIVE, RISE }

    private Phase phase = Phase.CIRCLE;
    private int ticksInPhase;

    public Phase phase() { return phase; }

    public int ticksInPhase() { return ticksInPhase; }

    /** True exactly once on the tick the wind shot should be released. */
    public boolean shotDue() { return phase == Phase.SHOOT && ticksInPhase == 0; }

    private void enter(Phase next) {
        phase = next;
        ticksInPhase = 0;
    }

    /**
     * Advances one tick.
     *
     * @param diveFinished the dive connected with the target (or reached it); ends the dive immediately
     * @param risenEnough  the hunter is at or above its rise height; ends the rise
     */
    public void tick(boolean diveFinished, boolean risenEnough) {
        ticksInPhase++;
        switch (phase) {
            case CIRCLE -> {
                if (ticksInPhase >= SkyFlyerTuning.HUNTER_CIRCLE_TICKS) {
                    enter(Phase.SHOOT);
                }
            }
            case SHOOT -> {
                if (ticksInPhase >= SkyFlyerTuning.HUNTER_SHOT_AFTERGLOW_TICKS) enter(Phase.DIVE);
            }
            case DIVE -> {
                if (diveFinished || ticksInPhase >= SkyFlyerTuning.HUNTER_DIVE_TIMEOUT_TICKS) enter(Phase.RISE);
            }
            case RISE -> {
                if (risenEnough || ticksInPhase >= SkyFlyerTuning.HUNTER_RISE_TIMEOUT_TICKS) enter(Phase.CIRCLE);
            }
        }
    }

    /** Called by the entity when the target is lost; the next engagement starts from a fresh circle. */
    public void reset() {
        enter(Phase.CIRCLE);
    }
}
