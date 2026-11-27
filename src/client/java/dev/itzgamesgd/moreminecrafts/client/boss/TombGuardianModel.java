package dev.itzgamesgd.moreminecrafts.client.boss;

import dev.itzgamesgd.moreminecrafts.client.model.BoneModelLoader;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Converted Blockbench colossus model for the Tomb Guardian. Rest pose is reset each frame via {@code super.setupAnim},
 * then head look, walk cycle, emergence lift, and the five attack keyframe animations are layered on the mapped bones.
 */
final class TombGuardianModel extends EntityModel<TombGuardianRenderState> {
    private final ModelPart rootBone;
    private final ModelPart head;
    private final ModelPart armRight;
    private final ModelPart armLeft;
    private final ModelPart legRight;
    private final ModelPart legLeft;

    private final KeyframeAnimation sweep;
    private final KeyframeAnimation slam;
    private final KeyframeAnimation spikes;
    private final KeyframeAnimation summon;
    private final KeyframeAnimation ultimate;

    TombGuardianModel(BoneModelLoader.Loaded loaded) {
        super(loaded.root());
        this.rootBone = loaded.require("root");
        this.head = loaded.require("head_bone");
        this.armRight = loaded.require("arm_upper_right_bone");
        this.armLeft = loaded.require("arm_upper_left_bone");
        this.legRight = loaded.require("leg_upper_right_bone");
        this.legLeft = loaded.require("leg_upper_left_bone");

        ModelPart root = loaded.root();
        this.sweep = TombGuardianAnimations.SWEEP.bake(root);
        this.slam = TombGuardianAnimations.SLAM.bake(root);
        this.spikes = TombGuardianAnimations.SPIKES.bake(root);
        this.summon = TombGuardianAnimations.SUMMON.bake(root);
        this.ultimate = TombGuardianAnimations.ULTIMATE.bake(root);
    }

    @Override
    public void setupAnim(TombGuardianRenderState state) {
        super.setupAnim(state);
        head.yRot += state.yRot * ((float) Math.PI / 180.0F);
        head.xRot += state.xRot * ((float) Math.PI / 180.0F);

        float walkPos = state.walkAnimationPos;
        float walkSpeed = state.walkAnimationSpeed;
        float legSwing = Mth.cos(walkPos * 0.5F) * 0.7F * walkSpeed;
        legRight.xRot += legSwing;
        legLeft.xRot -= legSwing;
        armRight.xRot -= legSwing * 0.5F;
        armLeft.xRot += legSwing * 0.5F;

        if (state.emergeAnimationState.isStarted()) {
            float t = state.emergeAnimationState.getTimeInMillis(state.ageInTicks) / 1000.0F;
            float progress = Mth.clamp(t / 4.0F, 0.0F, 1.0F);
            rootBone.y += Mth.lerp(progress, 68.0F, 0.0F);
        }

        sweep.apply(state.sweepAnimationState, state.ageInTicks);
        slam.apply(state.slamAnimationState, state.ageInTicks);
        spikes.apply(state.spikeAnimationState, state.ageInTicks);
        summon.apply(state.summonAnimationState, state.ageInTicks);
        ultimate.apply(state.ultimateAnimationState, state.ageInTicks);
    }
}
