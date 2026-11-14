package dev.itzgamesgd.moreminecrafts.st01;

/**
 * Gold guide golem parameters.
 *
 * <p>Owner-approved O-6 production settings (ST-01 P11, 2026-11-08):
 * <ul>
 *   <li>{@link #SCALE} = {@code 0.5D} (50% of vanilla Iron Golem dimensions: {@code 0.7w x 1.35h}, keeping path node at {@code 1x1x2})</li>
 *   <li>{@link #MOVEMENT_SPEED} = {@code 0.25D}</li>
 * </ul>
 *
 * <p>{@link #WAIT_DISTANCE} remains a provisional candidate value from P7/P8 design notes.
 */
public final class GoldGuideGolemTuning {
    private GoldGuideGolemTuning() {}

    /** Vanilla iron golem base size (the model is reused; the SCALE attribute scales render, hitbox and path node together). */
    public static final float BASE_WIDTH = 1.4F;
    public static final float BASE_HEIGHT = 2.7F;
    /** APPROVED (O-6, ST-01 P11): 0.5 scale (0.7w x 1.35h). */
    public static final double SCALE = 0.5D;
    /** APPROVED (O-6, ST-01 P11): 0.25 movement speed attribute base value. */
    public static final double MOVEMENT_SPEED = 0.25D;
    /** Distance beyond which the golem waits for its leader. CANDIDATE (unapproved). */
    public static final double WAIT_DISTANCE = 10.0D;
}
