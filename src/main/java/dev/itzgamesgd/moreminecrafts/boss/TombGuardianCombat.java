package dev.itzgamesgd.moreminecrafts.boss;

import static dev.itzgamesgd.moreminecrafts.boss.TombGuardianTuning.*;

/** Pure, server-side hit and cadence rules for the three Tomb Guardian attacks. */
public final class TombGuardianCombat {
    private TombGuardianCombat() {}

    public enum Attack { NONE, MELEE, SLAM, STATUE }

    /** Approved simple enrage: a single one-way phase entered at or below 40% of max health. */
    public static boolean shouldEnrage(float health, float maxHealth) {
        return maxHealth > 0.0F && health > 0.0F && health <= maxHealth * ENRAGE_HEALTH_FRACTION;
    }

    public static int attackInterval(boolean enraged) {
        return enraged ? ENRAGED_ATTACK_INTERVAL_TICKS : ATTACK_INTERVAL_TICKS;
    }

    public static int slamWindup(boolean enraged) {
        return enraged ? ENRAGED_SLAM_WINDUP_TICKS : SLAM_WINDUP_TICKS;
    }

    public static int slamCooldown(boolean enraged) {
        return enraged ? ENRAGED_SLAM_COOLDOWN_TICKS : SLAM_COOLDOWN_TICKS;
    }

    /** Ticks from smash impact until the wave front has passed the full radius. */
    public static int waveTravelTicks() {
        return (int) Math.ceil((WAVE_RADIUS + WAVE_THICKNESS / 2.0D) / WAVE_SPEED);
    }

    /** Wave front radius {@code ticksSinceImpact} ticks after the smash lands. */
    public static double waveFront(int ticksSinceImpact) {
        return Math.min(WAVE_RADIUS, ticksSinceImpact * WAVE_SPEED);
    }

    /**
     * Recovery after any attack resolves, derived from the melee cadence so that consecutive punches start
     * exactly one attack interval apart and the area attacks leave the same punish window.
     */
    public static int recoveryAfterAttack(boolean enraged) {
        return attackInterval(enraged) - MELEE_WINDUP_TICKS;
    }

    /**
     * Whether a target is struck by the outward wave as its front sweeps from {@code previousFront} to
     * {@code front} this tick. Only the band of the wave ring hurts; anything inside the ring after it has
     * passed, or outside its maximum radius, is safe, and a target whose feet are above the jump clearance
     * hops over it.
     */
    public static boolean waveHits(double horizontalDistance, double previousFront, double front, double feetAboveFloor) {
        if (feetAboveFloor >= WAVE_JUMP_CLEARANCE || feetAboveFloor < -WAVE_FLOOR_TOLERANCE) return false;
        double half = WAVE_THICKNESS / 2.0D;
        double inner = Math.max(0.0D, previousFront - half);
        double outer = Math.min(WAVE_RADIUS + half, front + half);
        return horizontalDistance >= inner && horizontalDistance <= outer;
    }

    /** Whether a target stands inside the fixed eruption column when the statue rises. */
    public static boolean statueHits(double dx, double dz, double feetAboveFloor) {
        if (feetAboveFloor < -1.0D || feetAboveFloor > STATUE_HEIGHT) return false;
        return dx * dx + dz * dz <= STATUE_RADIUS * STATUE_RADIUS;
    }
}
