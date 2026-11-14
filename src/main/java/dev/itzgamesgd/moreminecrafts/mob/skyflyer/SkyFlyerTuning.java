package dev.itzgamesgd.moreminecrafts.mob.skyflyer;

/**
 * PROVISIONAL tuning for the Sky Spirit and Storm Hunter. The design record specifies neither health, damage, speed,
 * cooldowns nor pursuit distances, so each value below is the matching vanilla base value (Allay for the spirit,
 * Phantom plus the vanilla Breeze wind charge for the hunter) or a plain number chosen only to make the approved
 * behaviour observable. Every one of these awaits an owner balance decision; none is design authority.
 */
public final class SkyFlyerTuning {
    // ---- Sky Spirit (Allay base) -------------------------------------------------------------------------
    /** PROVISIONAL: vanilla Allay max health (20) is taken from Allay.createAttributes(); not restated here. */
    /** PROVISIONAL: preferred hover distance to the attacker while retaliating (blocks). */
    public static final double SPIRIT_STANDOFF_DISTANCE = 6.0D;
    /** PROVISIONAL: the spirit stops chasing when the attacker is farther than this (vanilla Allay follow range 48 is the cap). */
    public static final double SPIRIT_DISENGAGE_DISTANCE = 32.0D;
    /** PROVISIONAL: ticks without line of sight after which the chase ends (5 s). */
    public static final int SPIRIT_LOST_SIGHT_TICKS = 100;
    /** PROVISIONAL: ticks between wind shots (vanilla Breeze shoots about every 40 ticks). */
    public static final int SPIRIT_SHOT_COOLDOWN_TICKS = 40;
    /** PROVISIONAL: wind shots must start from at least this close to firing range (blocks). */
    public static final double SPIRIT_FIRE_RANGE = 16.0D;
    /** PROVISIONAL: flight speed multiplier while chasing (Allay default control speed 1.0 with 0.1 flying speed). */
    public static final double SPIRIT_CHASE_SPEED = 1.0D;

    // ---- Storm Hunter (Phantom base) -----------------------------------------------------------------------
    /** PROVISIONAL: vanilla Phantom max health 20 and attack damage 2 at size 0 (measured). Taken from vanilla, not restated. */
    /** PROVISIONAL: orbit radius and height above the target while circling (blocks), vanilla Phantom uses 20 / 10-ish. */
    public static final double HUNTER_CIRCLE_RADIUS = 12.0D;
    public static final double HUNTER_CIRCLE_HEIGHT = 10.0D;
    /** PROVISIONAL: orbit angular speed (radians per tick); about one lap per 6 s. */
    public static final double HUNTER_CIRCLE_RATE = 0.0524D;
    /** PROVISIONAL: ticks spent circling before the first wind shot (3 s). */
    public static final int HUNTER_CIRCLE_TICKS = 60;
    /** PROVISIONAL: ticks between wind shot and dive (wind-up), and dive time-out (vanilla sweep gives up after ~2 s). */
    public static final int HUNTER_SHOT_AFTERGLOW_TICKS = 10;
    public static final int HUNTER_DIVE_TIMEOUT_TICKS = 40;
    /** PROVISIONAL: height above the target the hunter climbs to before re-entering the circle; reached or timed out. */
    public static final double HUNTER_RISE_HEIGHT = 10.0D;
    public static final int HUNTER_RISE_TIMEOUT_TICKS = 60;
    /** PROVISIONAL: target acquisition range (vanilla Phantom attack range, 64). */
    public static final double HUNTER_TARGET_RANGE = 64.0D;
    /** PROVISIONAL: movement speeds (vanilla Phantom move control uses 0.1 flying speed x 1.0 / 1.8 for dive). */
    public static final double HUNTER_CIRCLE_SPEED = 0.1D;
    public static final double HUNTER_DIVE_SPEED = 0.18D;
    /** PROVISIONAL: wind projectile launch speed and inaccuracy; the projectile itself is the vanilla Breeze wind charge. */
    public static final float WIND_SHOT_SPEED = 1.0F;
    public static final float WIND_SHOT_INACCURACY = 1.0F;

    private SkyFlyerTuning() {}
}
