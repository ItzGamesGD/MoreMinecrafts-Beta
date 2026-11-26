package dev.itzgamesgd.moreminecrafts.client.boss;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;

/**
 * Minimal attack keyframe corrections for the Tomb Guardian (spec 3: "model replacement plus partial attack
 * keyframe correction"). The skeleton is the vanilla Warden's, so these only add rotation on top of the vanilla pose
 * of the same bones; emerge, walk and look keep using the vanilla Warden animation.
 *
 * <p>Each animation is timed to its server windup (20 ticks = 1.0 s) so the strike pose lands on the tick that the
 * server resolves the hit, and ends back at the rest pose:
 * sweep 0.75 s, slam 1.3 s, spikes 1.0 s, summon 1.5 s, ultimate 2.0 s then strikes at 2.0 s, 3.0 s and 4.0 s.
 *
 * <p>Rotation convention (ModelPart, rotation order X then Y then Z, +Y down, front = -Z): a negative X rotation of an
 * arm raises it forwards, a positive Y rotation turns a forward-pointing arm to the model's right (-X).
 */
final class TombGuardianAnimations {
    private TombGuardianAnimations() {}

    private static Keyframe rot(float t, float x, float y, float z) {
        return new Keyframe(t, KeyframeAnimations.degreeVec(x, y, z), AnimationChannel.Interpolations.LINEAR);
    }

    private static AnimationChannel rotation(Keyframe... frames) {
        return new AnimationChannel(AnimationChannel.Targets.ROTATION, frames);
    }

    /** Right arm cocks out to the right, then sweeps across the front (the 120 degree fan); the torso_bone twists with it. */
    static final AnimationDefinition SWEEP = AnimationDefinition.Builder.withLength(1.0F)
            .addAnimation("arm_upper_right_bone", rotation(
                    rot(0.0F, 0, 0, 0), rot(0.55F, -80, 70, 0), rot(0.62F, -80, 72, 0),
                    rot(0.75F, -85, -62, 0), rot(0.9F, -60, -70, 0), rot(1.0F, 0, 0, 0)))
            .addAnimation("arm_upper_left_bone", rotation(
                    rot(0.0F, 0, 0, 0), rot(0.55F, 12, 0, -14), rot(0.75F, -10, 0, -6), rot(1.0F, 0, 0, 0)))
            .addAnimation("torso_bone", rotation(
                    rot(0.0F, 0, 0, 0), rot(0.55F, 0, 22, 0), rot(0.75F, 0, -25, 0), rot(0.9F, 0, -18, 0), rot(1.0F, 0, 0, 0)))
            .build();

    /** Both arms are raised overhead, then driven down onto the floor at 1.3 s with the torso_bone leaning into the blow. */
    static final AnimationDefinition SLAM = AnimationDefinition.Builder.withLength(2.2F)
            .addAnimation("arm_upper_right_bone", rotation(
                    rot(0.0F, 0, 0, 0), rot(1.0F, -165, 0, 8), rot(1.3F, -35, 0, 4), rot(1.6F, -35, 0, 4), rot(2.2F, 0, 0, 0)))
            .addAnimation("arm_upper_left_bone", rotation(
                    rot(0.0F, 0, 0, 0), rot(1.0F, -165, 0, -8), rot(1.3F, -35, 0, -4), rot(1.6F, -35, 0, -4), rot(2.2F, 0, 0, 0)))
            .addAnimation("torso_bone", rotation(
                    rot(0.0F, 0, 0, 0), rot(1.0F, -10, 0, 0), rot(1.3F, 18, 0, 0), rot(1.6F, 18, 0, 0), rot(2.2F, 0, 0, 0)))
            .build();

    /** A heavy stomp: one leg is lifted, the torso_bone rocks back, and the foot comes down at 1.0 s as the spikes erupt. */
    static final AnimationDefinition SPIKES = AnimationDefinition.Builder.withLength(1.5F)
            .addAnimation("leg_upper_right_bone", rotation(
                    rot(0.0F, 0, 0, 0), rot(0.5F, -55, 0, 0), rot(0.85F, -55, 0, 0), rot(1.0F, 0, 0, 0), rot(1.5F, 0, 0, 0)))
            .addAnimation("torso_bone", rotation(
                    rot(0.0F, 0, 0, 0), rot(0.5F, -6, 0, 0), rot(1.0F, 8, 0, 0), rot(1.4F, 0, 0, 0)))
            .addAnimation("arm_upper_right_bone", rotation(
                    rot(0.0F, 0, 0, 0), rot(0.5F, -10, 0, 14), rot(1.0F, -25, 0, 6), rot(1.5F, 0, 0, 0)))
            .addAnimation("arm_upper_left_bone", rotation(
                    rot(0.0F, 0, 0, 0), rot(0.5F, -10, 0, -14), rot(1.0F, -25, 0, -6), rot(1.5F, 0, 0, 0)))
            .build();

    /** The roar of the summon: head thrown back, arms spread wide while the guardians rise at 1.5 s. */
    static final AnimationDefinition SUMMON = AnimationDefinition.Builder.withLength(2.0F)
            .addAnimation("arm_upper_right_bone", rotation(
                    rot(0.0F, 0, 0, 0), rot(0.8F, -15, 0, 70), rot(1.5F, -15, 0, 76), rot(2.0F, 0, 0, 0)))
            .addAnimation("arm_upper_left_bone", rotation(
                    rot(0.0F, 0, 0, 0), rot(0.8F, -15, 0, -70), rot(1.5F, -15, 0, -76), rot(2.0F, 0, 0, 0)))
            .addAnimation("head_bone", rotation(
                    rot(0.0F, 0, 0, 0), rot(0.8F, -30, 0, 0), rot(1.5F, -30, 0, 0), rot(1.8F, 0, 0, 0)))
            .addAnimation("torso_bone", rotation(
                    rot(0.0F, 0, 0, 0), rot(0.8F, -8, 0, 0), rot(1.5F, -8, 0, 0), rot(2.0F, 0, 0, 0)))
            .build();

    /** Arms stay at full height for the 2.0 s windup, then three crushing blows at 2.0 s, 3.0 s and 4.0 s. */
    static final AnimationDefinition ULTIMATE = AnimationDefinition.Builder.withLength(4.6F)
            .addAnimation("arm_upper_right_bone", rotation(
                    rot(0.0F, 0, 0, 0), rot(1.7F, -170, 0, 6), rot(1.85F, -170, 0, 6), rot(2.0F, -30, 0, 3),
                    rot(2.2F, -30, 0, 3), rot(2.8F, -150, 0, 6), rot(3.0F, -30, 0, 3), rot(3.2F, -30, 0, 3),
                    rot(3.8F, -150, 0, 6), rot(4.0F, -30, 0, 3), rot(4.2F, -30, 0, 3), rot(4.6F, 0, 0, 0)))
            .addAnimation("arm_upper_left_bone", rotation(
                    rot(0.0F, 0, 0, 0), rot(1.7F, -170, 0, -6), rot(1.85F, -170, 0, -6), rot(2.0F, -30, 0, -3),
                    rot(2.2F, -30, 0, -3), rot(2.8F, -150, 0, -6), rot(3.0F, -30, 0, -3), rot(3.2F, -30, 0, -3),
                    rot(3.8F, -150, 0, -6), rot(4.0F, -30, 0, -3), rot(4.2F, -30, 0, -3), rot(4.6F, 0, 0, 0)))
            .addAnimation("torso_bone", rotation(
                    rot(0.0F, 0, 0, 0), rot(1.7F, -12, 0, 0), rot(1.85F, -12, 0, 0), rot(2.0F, 18, 0, 0),
                    rot(2.2F, 18, 0, 0), rot(2.8F, -8, 0, 0), rot(3.0F, 18, 0, 0), rot(3.2F, 18, 0, 0),
                    rot(3.8F, -8, 0, 0), rot(4.0F, 18, 0, 0), rot(4.2F, 18, 0, 0), rot(4.6F, 0, 0, 0)))
            .build();
}
