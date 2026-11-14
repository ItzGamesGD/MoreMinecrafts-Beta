package dev.itzgamesgd.moreminecrafts.test.crystalguardian;

/**
 * Pure (Minecraft-free) pose solver for the Ancient Crystal Guardian POC. Rotations are model-space radians as used by vanilla
 * {@code ModelPart}: for a limb hanging along +Y, a positive X rotation swings its end BACKWARD (+Z; the model front is -Z).
 * Every call is stateless and returns ABSOLUTE rotations relative to the rest pose, so nothing can accumulate between frames or
 * between repeated animation runs.
 *
 * <p>Ownership rule (conflict prevention): while the arm swing is active it owns {@code armRight}, {@code forearmRight} and
 * {@code torsoYaw}; the walk keeps driving the legs, shins, left arm and forearm. The two are cross-faded over the first and last
 * part of the swing so the hand-over has no pop.
 */
public final class GuardianPoseMath {
    private GuardianPoseMath() {}

    /** Geometry (model pixels) read from the converted model: hip height and thigh/shin lengths to the sole. */
    public static final float HIP_HEIGHT = 27.0F, THIGH_LENGTH = 12.0F, SHIN_LENGTH = 15.0F;

    public static final float WALK_PHASE_PER_LIMB_SWING = 0.55F;
    public static final float LEG_AMPLITUDE = 0.70F, ARM_AMPLITUDE = 0.50F, KNEE_FLEX = 0.85F, ELBOW_FLEX = 0.60F, TORSO_TWIST = 0.05F;

    /** Arm swing timeline, seconds. */
    public static final float WINDUP_END = 0.55F, STRIKE_END = 0.85F, HOLD_END = 0.97F, SWING_SECONDS = 1.55F;
    public static final int SWING_TICKS = (int) Math.ceil(SWING_SECONDS * 20.0F);
    private static final float BLEND_IN = 0.18F, BLEND_OUT = 0.30F;

    public static final float ARM_WINDUP = 1.35F, ARM_STRIKE = -1.95F;
    public static final float FOREARM_WINDUP = -0.90F, FOREARM_STRIKE = -0.12F;
    public static final float YAW_WINDUP = -0.32F, YAW_STRIKE = 0.36F;

    public record Pose(float legRight, float legLeft, float shinRight, float shinLeft,
                       float armRight, float armLeft, float forearmRight, float forearmLeft,
                       float torsoYaw, float rootDrop) {
        public static final Pose REST = new Pose(0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    /** Walk cycle from vanilla-style limb swing position and amount (amount clamped to [0,1]). */
    public static Pose walk(float limbSwing, float limbSwingAmount) {
        float a = Math.max(0.0F, Math.min(1.0F, limbSwingAmount));
        if (a <= 1.0E-4F) return Pose.REST;
        float phase = limbSwing * WALK_PHASE_PER_LIMB_SWING;
        float c = (float) Math.cos(phase), s = (float) Math.sin(phase);
        float legR = c * LEG_AMPLITUDE * a, legL = -c * LEG_AMPLITUDE * a;
        // Knee flexes while the leg swings forward (its X rotation is decreasing).
        float shinR = KNEE_FLEX * Math.max(0.0F, s) * a, shinL = KNEE_FLEX * Math.max(0.0F, -s) * a;
        // Arms oppose the same-side leg; elbow bends while the arm swings forward.
        float armR = -c * ARM_AMPLITUDE * a, armL = c * ARM_AMPLITUDE * a;
        float foreR = -ELBOW_FLEX * Math.max(0.0F, -s) * a, foreL = -ELBOW_FLEX * Math.max(0.0F, s) * a;
        float yaw = c * TORSO_TWIST * a;
        return new Pose(legR, legL, shinR, shinL, armR, armL, foreR, foreL, yaw, rootDrop(legR, shinR, legL, shinL));
    }

    /**
     * How far (model px) the root must drop so the lower foot stays planted: the sole height of each leg from forward kinematics,
     * minimum of both (never negative). Keeps feet on the ground instead of floating when the legs bend.
     */
    static float rootDrop(float legR, float shinR, float legL, float shinL) {
        float hr = soleHeight(legR, shinR), hl = soleHeight(legL, shinL);
        return Math.max(0.0F, Math.min(hr, hl));
    }

    private static float soleHeight(float thighRot, float shinRot) {
        return HIP_HEIGHT - (THIGH_LENGTH * (float) Math.cos(thighRot) + SHIN_LENGTH * (float) Math.cos(thighRot + shinRot));
    }

    /** Right-arm swing key pose (absolute), independent of walking. {@code t} in seconds; 0 outside the animation. */
    public static float[] swingKey(float t) {
        if (t <= 0.0F || t >= SWING_SECONDS) return new float[] {0, 0, 0};
        float arm, fore, yaw;
        if (t < WINDUP_END) {
            float k = easeOut(t / WINDUP_END);
            arm = ARM_WINDUP * k; fore = FOREARM_WINDUP * k; yaw = YAW_WINDUP * k;
        } else if (t < STRIKE_END) {
            float k = easeIn((t - WINDUP_END) / (STRIKE_END - WINDUP_END));
            arm = lerp(ARM_WINDUP, ARM_STRIKE, k);
            // Forearm trails the shoulder: it unfolds over the second half of the strike (whip).
            fore = lerp(FOREARM_WINDUP, FOREARM_STRIKE, smooth(Math.max(0.0F, (k - 0.35F) / 0.65F)));
            yaw = lerp(YAW_WINDUP, YAW_STRIKE, easeOut(k));
        } else if (t < HOLD_END) {
            float k = (t - STRIKE_END) / (HOLD_END - STRIKE_END);
            arm = ARM_STRIKE + 0.06F * k; fore = FOREARM_STRIKE; yaw = YAW_STRIKE;
        } else {
            float k = smooth((t - HOLD_END) / (SWING_SECONDS - HOLD_END));
            arm = lerp(ARM_STRIKE + 0.06F, 0.0F, k); fore = lerp(FOREARM_STRIKE, 0.0F, k); yaw = lerp(YAW_STRIKE, 0.0F, k);
        }
        return new float[] {arm, fore, yaw};
    }

    /** Weight [0,1] of the swing over the walk at time t. */
    public static float swingWeight(float t) {
        if (t <= 0.0F || t >= SWING_SECONDS) return 0.0F;
        float in = smooth(Math.min(1.0F, t / BLEND_IN));
        float out = smooth(Math.min(1.0F, (SWING_SECONDS - t) / BLEND_OUT));
        return Math.min(in, out);
    }

    /** Final pose. {@code swingSeconds < 0} (or beyond the length) means no swing. */
    public static Pose compose(float limbSwing, float limbSwingAmount, float swingSeconds) {
        Pose w = walk(limbSwing, limbSwingAmount);
        float weight = swingWeight(swingSeconds);
        if (weight <= 0.0F) return w;
        float[] k = swingKey(swingSeconds);
        return new Pose(w.legRight, w.legLeft, w.shinRight, w.shinLeft,
                lerp(w.armRight, k[0], weight), w.armLeft, lerp(w.forearmRight, k[1], weight), w.forearmLeft,
                lerp(w.torsoYaw, k[2], weight), w.rootDrop);
    }

    private static float lerp(float a, float b, float k) { return a + (b - a) * k; }
    private static float smooth(float k) { k = Math.max(0.0F, Math.min(1.0F, k)); return k * k * (3.0F - 2.0F * k); }
    private static float easeOut(float k) { k = Math.max(0.0F, Math.min(1.0F, k)); return 1.0F - (1.0F - k) * (1.0F - k); }
    private static float easeIn(float k) { k = Math.max(0.0F, Math.min(1.0F, k)); return k * k; }
}
