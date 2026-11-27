package dev.itzgamesgd.moreminecrafts.boss;

import static dev.itzgamesgd.moreminecrafts.boss.TombGuardianTuning.*;

/** Pure, server-side hit rules for the Tomb Guardian's four patterns and the Royal Judgment Earthwave. */
public final class TombGuardianCombat {
    private TombGuardianCombat() {}

    public enum Attack { NONE, SWEEP, SLAM, SPIKES, SUMMON, ULTIMATE }

    /** One-way enrage at or below 50% health (spec 4.1: the Ultimate triggers at this threshold). */
    public static boolean shouldEnrage(float health, float maxHealth) {
        return maxHealth > 0.0F && health > 0.0F && health <= maxHealth * ENRAGE_HEALTH_FRACTION;
    }

    /** Ticks from smash impact until the front has passed the full radius of a wave. */
    public static int waveTravelTicks(double radius) {
        return (int) Math.ceil((radius + WAVE_THICKNESS / 2.0D) / WAVE_SPEED);
    }

    /** Wave front radius {@code ticksSinceImpact} ticks after impact, capped at {@code radius}. */
    public static double waveFront(int ticksSinceImpact, double radius) {
        return Math.min(radius, ticksSinceImpact * WAVE_SPEED);
    }

    /**
     * Whether a target is struck by a moving wave as its front sweeps from {@code previousFront} to {@code front}
     * this tick. Only the band of the ring hurts; a target whose feet are above the jump clearance hops over it.
     */
    public static boolean waveHits(double horizontalDistance, double previousFront, double front, double feetAboveFloor,
                                   double radius) {
        if (feetAboveFloor >= WAVE_JUMP_CLEARANCE || feetAboveFloor < -WAVE_FLOOR_TOLERANCE) return false;
        double half = WAVE_THICKNESS / 2.0D;
        double inner = Math.max(0.0D, previousFront - half);
        double outer = Math.min(radius + half, front + half);
        return horizontalDistance >= inner && horizontalDistance <= outer;
    }

    /**
     * Sweep test (spec 4.1, P1-BUG-01 "1-A"): a victim is hit when it lies inside the forward 120 degree fan
     * within the sweep reach and within the vertical tolerance. Forward is the guardian's yaw at the strike frame.
     *
     * @param yawDegrees the guardian's body yaw at the strike frame, in Minecraft convention
     * @param dx         victim x minus guardian x
     * @param dz         victim z minus guardian z
     * @param dy         victim feet y minus guardian feet y
     */
    public static boolean sweepHits(double yawDegrees, double dx, double dz, double dy) {
        if (Math.abs(dy) > SWEEP_VERTICAL_TOLERANCE) return false;
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance > SWEEP_REACH) return false;
        if (distance < 1.0E-6D) return true;
        double radians = Math.toRadians(yawDegrees);
        double forwardX = -Math.sin(radians);
        double forwardZ = Math.cos(radians);
        double cosAngle = (dx * forwardX + dz * forwardZ) / distance;
        return cosAngle >= Math.cos(Math.toRadians(SWEEP_ARC_DEGREES / 2.0D));
    }

    /** Whether a target stands inside a spike column when it rises (spec: radius 1.5, height 4.0). */
    public static boolean spikeHits(double dx, double dz, double feetAboveFloor) {
        if (feetAboveFloor < -1.0D || feetAboveFloor > SPIKE_HEIGHT) return false;
        return dx * dx + dz * dz <= SPIKE_RADIUS * SPIKE_RADIUS;
    }

    /** Radius of ultimate strike {@code index} (0, 1, 2). */
    public static double ultimateRadius(int index) {
        return switch (index) {
            case 0 -> ULTIMATE_HIT_ONE_RADIUS;
            case 1 -> ULTIMATE_HIT_TWO_RADIUS;
            default -> ULTIMATE_HIT_THREE_RADIUS;
        };
    }

    /** Damage of ultimate strike {@code index} (0, 1, 2). */
    public static float ultimateDamage(int index) {
        return switch (index) {
            case 0 -> ULTIMATE_HIT_ONE_DAMAGE;
            case 1 -> ULTIMATE_HIT_TWO_DAMAGE;
            default -> ULTIMATE_HIT_THREE_DAMAGE;
        };
    }
}
