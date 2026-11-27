package dev.itzgamesgd.moreminecrafts.client;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.mob.sky.CrystalBug;
import dev.itzgamesgd.moreminecrafts.mob.sky.SkyNormalMobs;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Crystal Bug: the Blockbench model (scripts/bbmodel-capture/convert.py). Bones are head, thorax, abdomen and six legs.
 * The animation is the earlier D2 behaviour reduced to these bones: idle breathing, walking undulation with alternating
 * legs, a head lunge on the melee swing, hurt recoil and a settling death. The entity's SCALE attribute (2.5)
 * enlarges the render.
 */
final class CrystalBugRenderer extends MobRenderer<CrystalBug, CrystalBugRenderer.CrystalBugRenderState, CrystalBugRenderer.CrystalBugModel> {
    static final Identifier TEXTURE = ModIds.id("textures/entity/bb_crystal_bug.png");

    CrystalBugRenderer(EntityRendererProvider.Context context) {
        super(context, new CrystalBugModel(), 0.3F);
    }

    static void register() {
        EntityRendererRegistry.register(SkyNormalMobs.CRYSTAL_BUG, CrystalBugRenderer::new);
    }

    /** No vanilla death roll: the death is a low collapse in the model instead of a 180-degree flip. */
    @Override
    protected float getFlipDegrees() {
        return 0.0F;
    }

    @Override
    public Identifier getTextureLocation(CrystalBugRenderState state) {
        return TEXTURE;
    }

    @Override
    public CrystalBugRenderState createRenderState() {
        return new CrystalBugRenderState();
    }

    @Override
    public void extractRenderState(CrystalBug entity, CrystalBugRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.swing = entity.getSwingAnimation(partialTick);
        state.hurtProgress = entity.hurtTime > 0 ? Math.max(0.0F, entity.hurtTime - partialTick) / 10.0F : 0.0F;
    }

    static final class CrystalBugRenderState extends LivingEntityRenderState {
        /** Vanilla melee swing progress, 0..1. */
        float swing;
        /** Remaining hurt flash, 1 at the hit down to 0. */
        float hurtProgress;
    }

    static final class CrystalBugModel extends EntityModel<CrystalBugRenderState> {
        private static final String[] LEGS = {"fl", "fr", "ml", "mr", "bl", "br"};
        private final ModelPart head;
        private final ModelPart thorax;
        private final ModelPart abdomen;
        private final ModelPart[] legs = new ModelPart[LEGS.length];

        CrystalBugModel() {
            this(BbModelParts.build("crystal_bug", false));
        }

        private CrystalBugModel(ModelPart root) {
            super(root);
            head = root.getChild("head");
            thorax = root.getChild("thorax");
            abdomen = root.getChild("abdomen");
            for (int i = 0; i < LEGS.length; i++) legs[i] = root.getChild("leg_" + LEGS[i]);
        }

        @Override
        public void setupAnim(CrystalBugRenderState state) {
            super.setupAnim(state);
            resetPose();
            float t = state.ageInTicks;
            float walk = Math.min(1.0F, state.walkAnimationSpeed * 2.5F);
            float pos = state.walkAnimationPos;
            float lunge = Mth.sin(state.swing * Mth.PI);
            float hurt = state.hurtProgress * state.hurtProgress;
            float death = Mth.clamp(state.deathTime / 20.0F, 0.0F, 1.0F);
            float settle = death * (2.0F - death);
            float alive = 1.0F - settle;

            // Idle breathing and walking undulation: the abdomen trails the thorax with a lateral wave.
            thorax.y += Mth.sin(t * 0.09F) * 0.18F * alive;
            abdomen.y += Mth.sin(t * 0.09F - 0.55F) * 0.18F * alive;
            abdomen.yRot += Mth.sin(t * 0.05F) * 0.02F * alive + Mth.cos(pos * 0.9F - 1.4F) * 0.12F * walk * alive;
            thorax.yRot += Mth.cos(pos * 0.9F) * 0.05F * walk * alive;
            // Attack lunge and hurt recoil on the head.
            head.z -= 2.0F * lunge;
            head.xRot += 0.18F * lunge - 0.22F * hurt + 0.12F * settle;
            head.z += 0.8F * hurt;
            // Death: the body sinks onto the ground.
            thorax.y += 1.5F * settle;
            abdomen.y += 1.5F * settle;
            head.y += 1.5F * settle;

            for (int i = 0; i < LEGS.length; i++) {
                boolean left = LEGS[i].endsWith("l");
                float gait = pos * 1.1F + (i / 2) * Mth.PI * 0.66F + (left ? 0.0F : Mth.PI);
                float swing = Mth.cos(gait) * 0.55F * walk * alive;
                float lift = Math.max(0.0F, Mth.sin(gait)) * 0.25F * walk * alive;
                float side = left ? -1.0F : 1.0F;
                legs[i].xRot += swing;
                legs[i].zRot += side * (lift + 0.9F * settle - 0.2F * hurt);
            }
        }
    }
}
