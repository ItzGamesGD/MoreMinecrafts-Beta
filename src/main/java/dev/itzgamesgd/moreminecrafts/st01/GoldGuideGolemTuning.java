package dev.itzgamesgd.moreminecrafts.st01;

/**
 * Candidate golem parameters from the P8 technical window. NONE of these is approved (O-6): size, speed and spawn are open decisions and are
 * kept in one place so they can be changed or overridden without touching behaviour. SCALE 0.5 keeps the path node at 1x1x2 and stays clear of
 * the cover cut at the nearest stop zone (limit 0.714).
 */
public final class GoldGuideGolemTuning {
    private GoldGuideGolemTuning() {}

    /** Vanilla iron golem base size (the model is reused; the SCALE attribute scales render, hitbox and path node together). */
    public static final float BASE_WIDTH = 1.4F;
    public static final float BASE_HEIGHT = 2.7F;
    /** CANDIDATE (unapproved). */
    public static final double SCALE = 0.5D;
    /** CANDIDATE (unapproved), blocks/tick attribute value. */
    public static final double MOVEMENT_SPEED = 0.25D;
    /** Distance beyond which the golem waits for its leader. CANDIDATE (unapproved). */
    public static final double WAIT_DISTANCE = 10.0D;
}
