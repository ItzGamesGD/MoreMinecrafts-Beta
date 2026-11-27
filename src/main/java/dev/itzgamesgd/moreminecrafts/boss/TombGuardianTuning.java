package dev.itzgamesgd.moreminecrafts.boss;

/**
 * Tomb Guardian combat numbers, synchronised to the owner-confirmed 2026-11-14 redesign
 * (docs/design/st01_tomb_guardian/2026-11-14_ST01_왕릉수호자_설계_확정_및_PYRAMID_DESIGN_PREP_정정명세.md).
 * Values without a "HOW" marker are spec values. Values marked HOW are implementation choices for mechanics the
 * spec describes without a number; they are listed for owner review and are not confirmed authority.
 */
public final class TombGuardianTuning {
    private TombGuardianTuning() {}

    // Attributes (spec 4.0).
    public static final double MAX_HEALTH = 300.0D;
    public static final double ARMOR = 12.0D;
    public static final double MOVEMENT_SPEED = 0.21D;
    /** Enrage speed is 0.24 (spec 4.0). Applied as an additive modifier of the difference. */
    public static final double ENRAGED_MOVEMENT_SPEED = 0.24D;
    /** HOW: not stated by the spec; kept at the existing value. */
    public static final double KNOCKBACK_RESISTANCE = 0.8D;
    /** HOW: not stated by the spec; kept at the existing value. */
    public static final double FOLLOW_RANGE = 24.0D;
    /** HOW: base attack attribute is unused by the attacks, which carry their own spec damage. */
    public static final double MELEE_DAMAGE = 10.0D;

    // Model and hitbox (spec 2): 1.35 wide x 4.35 tall, eye height 3.6975.
    public static final float MODEL_SCALE = 1.5F;
    public static final float WIDTH = 1.35F;
    public static final float HEIGHT = 4.35F;
    public static final float EYE_HEIGHT = 3.6975F;

    // Emergence warm-up (spec 2): 80 ticks, invulnerable during the animation.
    public static final int AWAKENING_TICKS = 80;

    // Enrage at or below 50% health (spec 4.1 Ultimate condition).
    public static final float ENRAGE_HEALTH_FRACTION = 0.5F;

    // Pattern 2: Sweep (spec 4.1). Hit test is a 120 degree forward fan, reach 2.5, vertical tolerance +-1.5.
    public static final int SWEEP_WINDUP_TICKS = 15;
    public static final int SWEEP_COOLDOWN_TICKS = 50;
    public static final double SWEEP_REACH = 2.5D;
    public static final double SWEEP_ARC_DEGREES = 120.0D;
    public static final double SWEEP_VERTICAL_TOLERANCE = 1.5D;
    public static final float SWEEP_DAMAGE = 9.0F;

    // Pattern 1: Ground Slam (spec 4.1).
    public static final int SLAM_WINDUP_TICKS = 26;
    public static final int SLAM_COOLDOWN_TICKS = 180;
    public static final double WAVE_RADIUS = 6.0D;
    public static final float SLAM_DAMAGE = 12.0F;
    /** HOW: thickness of the moving shockwave band. */
    public static final double WAVE_THICKNESS = 0.8D;
    /** HOW: outward travel speed of the wave front, blocks per tick. */
    public static final double WAVE_SPEED = 0.5D;
    /** Spec 4.2 / 4.1: feet at least this high above the floor are airborne and jump over the wave. */
    public static final double WAVE_JUMP_CLEARANCE = 0.6D;
    /** HOW: feet this far below the floor (steps, slabs) still count as the arena floor. */
    public static final double WAVE_FLOOR_TOLERANCE = 1.0D;

    // Pattern 3: Desert Call / Tomb Spikes (spec 4.1).
    public static final int SPIKE_WINDUP_TICKS = 20;
    public static final int SPIKE_COOLDOWN_TICKS = 140;
    public static final double SPIKE_RADIUS = 1.5D;
    public static final double SPIKE_HEIGHT = 4.0D;
    public static final float SPIKE_DAMAGE = 8.0F;
    /** HOW: how long the risen spikes stay visible before they sink away. */
    public static final int SPIKE_VISIBLE_TICKS = 40;
    /** HOW: upward speed given to a victim lifted by a spike (spec says "airborne" without a value). */
    public static final double SPIKE_AIRBORNE_LIFT = 0.5D;

    // Pattern 4: Royal Guard Summon (spec 4.1). Mummy zombies x2 plus a tomb skeleton x1, at most 3 alive.
    public static final int SUMMON_WINDUP_TICKS = 30;
    public static final int SUMMON_COOLDOWN_TICKS = 400;
    public static final int SUMMON_MAX_ALIVE = 3;
    public static final int SUMMON_MUMMY_COUNT = 2;
    public static final int SUMMON_SKELETON_COUNT = 1;
    /** HOW: distance from the guardian at which summons appear. */
    public static final double SUMMON_RING_RADIUS = 3.0D;

    // Ultimate: Royal Judgment Earthwave, U1 (spec 4.1). Three consecutive strikes.
    public static final int ULTIMATE_WINDUP_TICKS = 40;
    public static final int ULTIMATE_COOLDOWN_TICKS = 500;
    public static final double ULTIMATE_HIT_ONE_RADIUS = 8.0D;
    public static final double ULTIMATE_HIT_TWO_RADIUS = 12.0D;
    /** HOW: the spec says "full-range" for the third strike without a radius; the largest ring is used. */
    public static final double ULTIMATE_HIT_THREE_RADIUS = 12.0D;
    public static final float ULTIMATE_HIT_ONE_DAMAGE = 10.0F;
    public static final float ULTIMATE_HIT_TWO_DAMAGE = 10.0F;
    public static final float ULTIMATE_HIT_THREE_DAMAGE = 12.0F;
    /** HOW: ticks between the three strikes; the spec gives no interval. */
    public static final int ULTIMATE_HIT_INTERVAL_TICKS = 20;

    // Encounter: the bound guardian despawns after no living player stays within FOLLOW_RANGE for this long (P1-BUG-02).
    public static final int NO_SURVIVOR_RESET_TICKS = 100;

    /** Tag placed on a guardian spawned by the royal encounter. Only tagged guardians may drop the reward (P1-BUG-03). */
    public static final String ENCOUNTER_BOUND_TAG = "EncounterBound";
}
