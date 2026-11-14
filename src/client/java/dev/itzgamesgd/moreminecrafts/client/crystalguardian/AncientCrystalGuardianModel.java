package dev.itzgamesgd.moreminecrafts.client.crystalguardian;

import dev.itzgamesgd.moreminecrafts.test.crystalguardian.GuardianPoseMath;
import java.util.Map;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;

/**
 * Converted Blockbench model. Poses come exclusively from {@link GuardianPoseMath} (stateless, absolute), applied on top of the
 * reset rest pose every frame, so rotations can never accumulate between frames or repeated animation runs.
 */
public final class AncientCrystalGuardianModel extends EntityModel<AncientCrystalGuardianRenderState> {
    private final Map<String, ModelPart> bones;
    private final ModelPart guardian, torso, armLeft, armRight, forearmLeft, forearmRight, legLeft, legRight, shinLeft, shinRight;

    AncientCrystalGuardianModel(BoneModelLoader.Loaded loaded) {
        super(loaded.root());
        this.bones = loaded.bones();
        this.guardian = bone("guardian");
        this.torso = bone("torso");
        this.armLeft = bone("arm_left");
        this.armRight = bone("arm_right");
        this.forearmLeft = bone("forearm_left");
        this.forearmRight = bone("forearm_right");
        this.legLeft = bone("leg_left");
        this.legRight = bone("leg_right");
        this.shinLeft = bone("shin_left");
        this.shinRight = bone("shin_right");
    }

    private ModelPart bone(String name) {
        ModelPart part = bones.get(name);
        if (part == null) throw new IllegalStateException("Converted model has no bone '" + name + "'");
        return part;
    }

    @Override
    public void setupAnim(AncientCrystalGuardianRenderState state) {
        super.setupAnim(state);   // resets every part to its rest pose
        float swingSeconds = state.swingAnimationState.isStarted() ? state.swingAnimationState.getTimeInMillis(state.ageInTicks) / 1000.0F : -1.0F;
        GuardianPoseMath.Pose pose = GuardianPoseMath.compose(state.walkAnimationPos, state.walkAnimationSpeed, swingSeconds);
        guardian.y += pose.rootDrop();
        torso.yRot += pose.torsoYaw();
        legRight.xRot += pose.legRight();
        legLeft.xRot += pose.legLeft();
        shinRight.xRot += pose.shinRight();
        shinLeft.xRot += pose.shinLeft();
        armRight.xRot += pose.armRight();
        armLeft.xRot += pose.armLeft();
        forearmRight.xRot += pose.forearmRight();
        forearmLeft.xRot += pose.forearmLeft();
    }

    /**
     * Verification hook (used by the real-client GameTest): the CURRENT pose of the animated parts, in the order
     * legRight, legLeft, shinRight, shinLeft, armRight, armLeft, forearmRight, forearmLeft, torsoYaw, guardianY (model px).
     */
    public float[] debugPose() {
        return new float[] {legRight.xRot, legLeft.xRot, shinRight.xRot, shinLeft.xRot, armRight.xRot, armLeft.xRot,
                forearmRight.xRot, forearmLeft.xRot, torso.yRot, guardian.y - guardian.getInitialPose().y()};
    }

    public static final String[] DEBUG_POSE_COLUMNS = {"legRight", "legLeft", "shinRight", "shinLeft", "armRight", "armLeft",
            "forearmRight", "forearmLeft", "torsoYaw", "guardianY"};
}
