package dev.itzgamesgd.moreminecrafts.boss;

/**
 * Tomb Guardian combat numbers. Every value here is the PROVISIONAL proposal recorded in
 * docs/PYRAMID_DESIGN_PREP_2026-10-25.md ("NUMBERS REMAIN PROVISIONAL despite encounter design lock");
 * only the 40% enrage threshold and the Ancient Heart 50% x1 drop (SKY_ISLANDS_T2_CONTEXT §18.4) are
 * owner-approved. Values marked HOW are implementation choices needed to realise an approved behaviour
 * where the documents give no number; they are reported for owner review, not locked authority.
 */
public final class TombGuardianTuning {
    private TombGuardianTuning() {}

    // Attributes.
    public static final double MAX_HEALTH = 200.0D;
    public static final double MELEE_DAMAGE = 10.0D;
    public static final double KNOCKBACK_RESISTANCE = 0.8D;
    /** Sight detection ~24 blocks (Balance V1). */
    public static final double FOLLOW_RANGE = 24.0D;
    /** Balance V1: "movement speed slower than zombie" (zombie 0.23). */
    public static final double MOVEMENT_SPEED = 0.20D;

    // Model / hitbox: ~4-5 block tall armored Warden-derived model; Warden 0.9 x 2.9 scaled by 1.5.
    public static final float MODEL_SCALE = 1.5F;
    public static final float WIDTH = 0.9F * MODEL_SCALE;
    public static final float HEIGHT = 2.9F * MODEL_SCALE;

    // Encounter awakening: "~3-5 seconds as an initial target" (approved royal chest unlock gate).
    public static final int AWAKENING_TICKS = 80;

    // Heavy melee fist: 10 HP with a 0.65 s telegraph; melee interval ~2.2 s, enraged ~1.9 s.
    public static final int MELEE_WINDUP_TICKS = 13;
    public static final int ATTACK_INTERVAL_TICKS = 44;
    public static final int ENRAGED_ATTACK_INTERVAL_TICKS = 38;
    /** HOW: reach measured from the guardian's edge, vanilla-style melee reach for a 1.35-wide body. */
    public static final double MELEE_REACH = 2.0D;

    // Ground smash shockwave: 12 HP, 1.3 s windup (1.1 s enraged), radius 6, ~0.8 thickness, ~9 s cooldown (8 s enraged).
    public static final float SLAM_DAMAGE = 12.0F;
    public static final int SLAM_WINDUP_TICKS = 26;
    public static final int ENRAGED_SLAM_WINDUP_TICKS = 22;
    public static final int SLAM_COOLDOWN_TICKS = 180;
    public static final int ENRAGED_SLAM_COOLDOWN_TICKS = 160;
    public static final double WAVE_RADIUS = 6.0D;
    public static final double WAVE_THICKNESS = 0.8D;
    /** HOW: outward travel speed of the wave front, blocks per tick (reaches 6 blocks in 12 ticks). */
    public static final double WAVE_SPEED = 0.5D;
    /** HOW: feet this far above the smash floor are airborne and jump over the wave. */
    public static final double WAVE_JUMP_CLEARANCE = 0.6D;
    /** HOW: feet this far below the smash floor (steps, slabs) are still on the arena floor. */
    public static final double WAVE_FLOOR_TOLERANCE = 1.0D;

    // Stone Eruption (inscribed statue): 8 HP, radius ~1.5, warning ~1.2 s, cooldown ~11 s.
    public static final float STATUE_DAMAGE = 8.0F;
    public static final double STATUE_RADIUS = 1.5D;
    public static final int STATUE_WARNING_TICKS = 24;
    public static final int STATUE_COOLDOWN_TICKS = 220;
    /** HOW: visible height of the risen statue and of its bounded damage column. */
    public static final double STATUE_HEIGHT = 4.0D;
    /** HOW: how long the risen statue stays visible before it sinks away. */
    public static final int STATUE_VISIBLE_TICKS = 40;

    // Enrage at <= 40% HP (owner approved): movement x1.15, faster cadence; no new attacks, no wider wave.
    public static final float ENRAGE_HEALTH_FRACTION = 0.40F;
    public static final double ENRAGED_SPEED_MULTIPLIER = 1.15D;
}
