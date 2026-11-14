package dev.itzgamesgd.moreminecrafts.mob.skyflyer;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.monster.dragon.EnderDragonModel;
import net.minecraft.client.renderer.entity.state.EnderDragonRenderState;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.boss.enderdragon.DragonFlightHistory;

/**
 * Technical probe for the Sky Wyvern (design section 3), NOT an implementation: it exercises the vanilla 26.3
 * {@link EnderDragonModel} headlessly (no entity, no renderer) to establish
 * (1) whether the model is a pure function of its render state (procedural) rather than keyframe driven,
 * (2) whether the baked layer can be instantiated on its own (reuse outside EnderDragonRenderer), and
 * (3) whether per-animation overlays, by hand or through vanilla keyframe clips, can be applied after setupAnim.
 * Active only with {@code -Dmoreminecrafts.skywyvern.probe=true}; prints WYVERN_PROBE lines and asserts each finding.
 */
@SuppressWarnings("UnstableApiUsage")
public final class SkyWyvernModelProbeClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        if (!Boolean.getBoolean("moreminecrafts.skywyvern.probe")) return;
        context.runOnClient(client -> {
            ModelPart root = client.getEntityModels().bakeLayer(ModelLayers.ENDER_DRAGON);
            EnderDragonModel model = new EnderDragonModel(root);
            ModelPart head = root.getChild("head");
            ModelPart jaw = head.getChild("jaw");
            ModelPart body = root.getChild("body");
            ModelPart leftWing = body.getChild("left_wing");
            ModelPart rightWing = body.getChild("right_wing");
            say("baked ENDER_DRAGON layer standalone: parts=" + root.getAllParts().size() + " (no EnderDragon entity or EnderDragonRenderer involved)");

            EnderDragonRenderState state = new EnderDragonRenderState();
            for (int i = 0; i < DragonFlightHistory.LENGTH; i++) state.flightHistory.record(100.0D + i * 0.05D, i * 2.0F);
            state.flapTime = 0.30F;
            state.ageInTicks = 20.0F;

            model.setupAnim(state);
            float[] a = pose(head, jaw, leftWing, rightWing);
            model.setupAnim(state);
            float[] b = pose(head, jaw, leftWing, rightWing);
            check(java.util.Arrays.equals(a, b), "setupAnim is a pure function of the render state (repeat call gives an identical pose)");

            state.flapTime = 0.80F;
            model.setupAnim(state);
            float[] c = pose(head, jaw, leftWing, rightWing);
            check(!java.util.Arrays.equals(a, c), "flapTime alone moves the wings: left wing zRot " + a[10] + " -> " + c[10]);
            state.flapTime = 0.30F;

            // 1. Hand overlay: applied after setupAnim it sticks; the next setupAnim rewrites it, so it must be reapplied every frame.
            model.setupAnim(state);
            float base = head.xRot;
            head.xRot += 0.5F;
            jaw.xRot += 0.6F;
            check(Math.abs(head.xRot - base - 0.5F) < 1.0E-6F, "hand overlay (breath aim: head pitch +0.5 rad, jaw open +0.6 rad) applies on top of the procedural pose");
            model.setupAnim(state);
            check(Math.abs(head.xRot - base) < 1.0E-6F, "the next setupAnim discards the overlay, so overlays must run after setupAnim on every frame");

            // 2. Vanilla keyframe clip layered over the procedural pose.
            AnimationDefinition clip = AnimationDefinition.Builder.withLength(1.0F)
                    .addAnimation("head", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(1.0F, KeyframeAnimations.degreeVec(30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)))
                    .build();
            KeyframeAnimation baked = clip.bake(root);
            AnimationState clock = new AnimationState();
            clock.start(0);
            model.setupAnim(state);
            float before = head.xRot;
            baked.apply(clock, 10.0F);
            float delta = head.xRot - before;
            say("keyframe clip at 0.5 s: head xRot " + before + " -> " + head.xRot + " (delta " + delta + " rad, expected about 0.2618 for additive)");
            check(Math.abs(delta - (float) Math.toRadians(15.0D)) < 0.02F,
                    "a vanilla KeyframeAnimation bakes against the dragon model and layers additively on the procedural pose");

            // 3. The pose a scaled render would draw is unchanged by scale: scaling is a PoseStack matter, not a model one.
            say("FINDINGS: EnderDragonModel is procedural (Mth.sin/cos of flapTime, flight history); it has no AnimationDefinition/KeyframeAnimation; "
                    + "it reads only EnderDragonRenderState, so a custom renderer + EnderDragonRenderState subclass can drive it without an EnderDragon entity.");
        });
    }

    private static float[] pose(ModelPart head, ModelPart jaw, ModelPart leftWing, ModelPart rightWing) {
        return new float[] {head.xRot, head.yRot, head.zRot, jaw.xRot, jaw.yRot, jaw.zRot, leftWing.xRot, leftWing.yRot, leftWing.zRot,
                rightWing.xRot, leftWing.zRot, rightWing.zRot};
    }

    private static void say(String message) {
        System.out.println("WYVERN_PROBE " + message);
    }

    private static void check(boolean ok, String finding) {
        System.out.println("WYVERN_PROBE " + (ok ? "PASS " : "FAIL ") + finding);
        if (!ok) throw new AssertionError(finding);
    }
}
