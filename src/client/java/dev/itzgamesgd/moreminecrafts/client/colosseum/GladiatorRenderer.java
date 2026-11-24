package dev.itzgamesgd.moreminecrafts.client.colosseum;

import dev.itzgamesgd.moreminecrafts.client.model.BoneModelLoader;
import dev.itzgamesgd.moreminecrafts.colosseum.ImmortalGladiator;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Custom Blockbench model renderer for the Immortal Gladiator (ST-05). Loaded from
 * {@code assets/moreminecrafts/entity_models/immortal_gladiator.json} at 1.0x scale (the .bbmodel is already 3.52m
 * tall, matching the 1.9x 1.14x3.42 hitbox).
 */
public final class GladiatorRenderer extends MobRenderer<ImmortalGladiator, GladiatorRenderer.State, GladiatorRenderer.Model> {
    public static final Identifier MODEL = ModIds.id("entity_models/immortal_gladiator.json");
    private static final Identifier TEXTURE = ModIds.id("textures/entity/immortal_gladiator.png");

    public static final class State extends LivingEntityRenderState {
        int pose;
        boolean phaseTwo;
        boolean shieldActive;
    }

    public GladiatorRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model(BoneModelLoader.load(ctx.getResourceManager(), MODEL)), 1.0F);
    }

    public static void register() {
        EntityRendererRegistry.register(ImmortalGladiator.entityType(), GladiatorRenderer::new);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ImmortalGladiator b, State s, float t) {
        super.extractRenderState(b, s, t);
        s.pose = b.attackPose();
        s.phaseTwo = b.phaseTwo();
        s.shieldActive = b.shieldActive();
    }

    @Override
    public Identifier getTextureLocation(State s) {
        return TEXTURE;
    }

    public static final class Model extends EntityModel<State> {
        private final ModelPart torso;
        private final ModelPart head;
        private final ModelPart armUpperRight;
        private final ModelPart armLowerRight;
        private final ModelPart armUpperLeft;
        private final ModelPart armLowerLeft;
        private final ModelPart shield;
        private final ModelPart legUpperRight;
        private final ModelPart legLowerRight;
        private final ModelPart legUpperLeft;
        private final ModelPart legLowerLeft;
        private final ModelPart cape;

        Model(BoneModelLoader.Loaded loaded) {
            super(loaded.root());
            this.torso = loaded.require("torso_bone");
            this.head = loaded.require("head_bone");
            this.armUpperRight = loaded.require("arm_upper_right_bone");
            this.armLowerRight = loaded.require("arm_lower_right_bone");
            this.armUpperLeft = loaded.require("arm_upper_left_bone");
            this.armLowerLeft = loaded.require("arm_lower_left_bone");
            this.shield = loaded.require("shield_bone");
            this.legUpperRight = loaded.require("leg_upper_right_bone");
            this.legLowerRight = loaded.require("leg_lower_right_bone");
            this.legUpperLeft = loaded.require("leg_upper_left_bone");
            this.legLowerLeft = loaded.require("leg_lower_left_bone");
            this.cape = loaded.require("cape_bone");
        }

        @Override
        public void setupAnim(State s) {
            super.setupAnim(s);
            shield.visible = s.shieldActive && !s.phaseTwo;

            head.yRot += s.yRot * ((float) Math.PI / 180.0F);
            head.xRot += s.xRot * ((float) Math.PI / 180.0F);

            float walkPos = s.walkAnimationPos;
            float walkSpeed = s.walkAnimationSpeed;
            float legSwing = Mth.cos(walkPos * 0.6662F) * 0.85F * walkSpeed;
            legUpperRight.xRot += legSwing;
            legUpperLeft.xRot -= legSwing;
            legLowerRight.xRot += Math.max(0.0F, -legSwing) * 0.4F;
            legLowerLeft.xRot += Math.max(0.0F, legSwing) * 0.4F;
            armUpperRight.xRot -= legSwing * 0.45F;
            armUpperLeft.xRot += legSwing * 0.45F;
            cape.xRot += 0.1F + walkSpeed * 0.35F;

            if (s.shieldActive && !s.phaseTwo) {
                armUpperLeft.xRot = -0.85F;
                armUpperLeft.yRot = 0.45F;
                armLowerLeft.xRot = -0.35F;
            }

            // GladiatorCombat.Attack ordinals: 0=NONE, 1=COMBO, 2=LUNGE, 3=SWEEP, 4=CHARGE, 5=OBSTACLE, 6=COUNTER
            switch (s.pose) {
                case 1 -> { // COMBO
                    armUpperRight.xRot = -1.75F;
                    armLowerRight.xRot = -0.45F;
                    torso.yRot += 0.2F;
                }
                case 2 -> { // LUNGE
                    armUpperRight.xRot = -1.45F;
                    armLowerRight.xRot = -0.1F;
                    torso.xRot += 0.18F;
                }
                case 3 -> { // SWEEP
                    armUpperRight.xRot = -1.2F;
                    armUpperRight.zRot = -0.95F;
                    torso.yRot -= 0.35F;
                }
                case 4, 5 -> { // CHARGE / OBSTACLE
                    armUpperRight.xRot = -2.25F;
                    armUpperRight.zRot = -0.2F;
                    torso.xRot += 0.25F;
                }
                case 6 -> { // COUNTER
                    armUpperRight.xRot = -1.9F;
                    armUpperLeft.xRot = -1.1F;
                    armUpperLeft.yRot = 0.6F;
                }
                default -> {}
            }
        }
    }
}
