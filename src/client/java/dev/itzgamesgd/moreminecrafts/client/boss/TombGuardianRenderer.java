package dev.itzgamesgd.moreminecrafts.client.boss;

import dev.itzgamesgd.moreminecrafts.boss.TombGuardian;
import dev.itzgamesgd.moreminecrafts.boss.TombGuardianTuning;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.monster.warden.WardenModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/**
 * Separate custom renderer for the Tomb Guardian: reuses the vanilla Warden model anatomy and keyframe
 * animations (the vanilla Warden renderer is untouched), adds armor cuboids (helmet, chest plate, pauldrons),
 * scales the body to the guardian hitbox and draws two emissive red eyes that brighten when enraged.
 */
public final class TombGuardianRenderer extends MobRenderer<TombGuardian, TombGuardianRenderState, WardenModel> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ModIds.id("tomb_guardian"), "main");
    private static final Identifier TEXTURE = ModIds.id("textures/entity/tomb_guardian/tomb_guardian.png");
    private static final Identifier EYES = ModIds.id("textures/entity/tomb_guardian/tomb_guardian_eyes.png");
    private static final Identifier ENRAGED_EYES = ModIds.id("textures/entity/tomb_guardian/tomb_guardian_eyes_enraged.png");

    public TombGuardianRenderer(EntityRendererProvider.Context context) {
        super(context, new WardenModel(context.bakeLayer(LAYER)), 0.9F * TombGuardianTuning.MODEL_SCALE);
        addLayer(new GuardianEyes(this, EYES, false));
        addLayer(new GuardianEyes(this, ENRAGED_EYES, true));
    }

    public static void registerLayer() {
        ModelLayerRegistry.registerModelLayer(LAYER, TombGuardianRenderer::createArmoredLayer);
    }

    /** Vanilla Warden geometry with armor cuboids in the free texture area, scaled to the guardian size. */
    static LayerDefinition createArmoredLayer() {
        return WardenModel.createBodyLayer().apply(mesh -> {
            PartDefinition body = mesh.getRoot().getChild("bone").getChild("body");
            body.getChild("head").addOrReplaceChild("helmet",
                    CubeListBuilder.create().texOffs(0, 96).addBox(-9.0F, -17.0F, -6.0F, 18.0F, 5.0F, 12.0F), PartPose.ZERO);
            body.addOrReplaceChild("chest_plate",
                    CubeListBuilder.create().texOffs(0, 114).addBox(-9.5F, -12.0F, -5.0F, 19.0F, 10.0F, 1.0F), PartPose.ZERO);
            body.getChild("right_arm").addOrReplaceChild("right_pauldron",
                    CubeListBuilder.create().texOffs(60, 96).addBox(-5.0F, -1.0F, -5.0F, 10.0F, 6.0F, 10.0F), PartPose.ZERO);
            body.getChild("left_arm").addOrReplaceChild("left_pauldron",
                    CubeListBuilder.create().texOffs(60, 96).mirror().addBox(-5.0F, -1.0F, -5.0F, 10.0F, 6.0F, 10.0F), PartPose.ZERO);
            return mesh;
        }).apply(MeshTransformer.scaling(TombGuardianTuning.MODEL_SCALE));
    }

    @Override
    public TombGuardianRenderState createRenderState() {
        return new TombGuardianRenderState();
    }

    @Override
    public void extractRenderState(TombGuardian guardian, TombGuardianRenderState state, float partialTick) {
        super.extractRenderState(guardian, state, partialTick);
        state.enraged = guardian.isEnraged();
        state.emergeAnimationState.copyFrom(guardian.emergeAnimationState);
        state.attackAnimationState.copyFrom(guardian.attackAnimationState);
        state.roarAnimationState.copyFrom(guardian.slamAnimationState);
        state.sonicBoomAnimationState.copyFrom(guardian.meleeWindupAnimationState);
        state.sniffAnimationState.copyFrom(guardian.statueAnimationState);
    }

    @Override
    public Identifier getTextureLocation(TombGuardianRenderState state) {
        return TEXTURE;
    }

    /** Emissive eye layer (same submission as the vanilla eyes layer); one texture per enrage state. */
    private static final class GuardianEyes extends RenderLayer<TombGuardianRenderState, WardenModel> {
        private static final int FULL_BRIGHT = 15728640;
        private final RenderType renderType;
        private final boolean enraged;

        GuardianEyes(TombGuardianRenderer parent, Identifier texture, boolean enraged) {
            super(parent);
            this.renderType = RenderTypes.eyes(texture);
            this.enraged = enraged;
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, TombGuardianRenderState state,
                           float yRot, float xRot) {
            if (state.enraged != enraged) return;
            collector.order(1).submitModel(getParentModel(), state, poseStack, renderType, FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, state.outlineColor);
        }
    }
}
