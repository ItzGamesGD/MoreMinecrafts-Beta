package dev.itzgamesgd.moreminecrafts.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.mob.sky.SkyNormalMobs;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.AbstractCubeMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.SlimeRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.cubemob.Slime;

/**
 * Cloud Slime P2-E1 (approved concept C, celestial spirit): static model and texture only.
 *
 * <p>Same two-pass structure as the vanilla slime: an inner cutout model holding layered voxel clouds and the eyes,
 * then an ordered translucent membrane cube. Inner geometry is opaque, so it writes depth before the membrane blends
 * over it, and every inner box keeps at least 0.25 px clearance from the membrane to avoid z-fighting. Size scaling,
 * the vanilla landing squish and the shadow are inherited unchanged from {@link AbstractCubeMobRenderer}.
 *
 * <p>Hierarchy kept for later phases: {@code body} (membrane and inner both) for jump deformation, and
 * {@code clouds} with one child per cloud for internal cloud movement. Neither is animated here.
 */
final class CloudSlimeRenderer extends AbstractCubeMobRenderer<Slime, SlimeRenderState, CloudSlimeRenderer.InnerModel> {
    static final ModelLayerLocation INNER = new ModelLayerLocation(ModIds.id("cloud_slime"), "main");
    static final ModelLayerLocation OUTER = new ModelLayerLocation(ModIds.id("cloud_slime"), "outer");
    static final Identifier TEXTURE = ModIds.id("textures/entity/cloud_slime.png");

    CloudSlimeRenderer(EntityRendererProvider.Context context) {
        super(context, new InnerModel(context.bakeLayer(INNER)));
        addLayer(new MembraneLayer(this, new MembraneModel(context.bakeLayer(OUTER))));
    }

    static void register() {
        ModelLayerRegistry.registerModelLayer(INNER, CloudSlimeRenderer::createInnerLayer);
        ModelLayerRegistry.registerModelLayer(OUTER, CloudSlimeRenderer::createOuterLayer);
        EntityRendererRegistry.register(SkyNormalMobs.CLOUD_SLIME, CloudSlimeRenderer::new);
    }

    /** Model space: the membrane spans x/z -4..4 and y 16..24 (y grows downward; 24 is the ground). */
    static LayerDefinition createInnerLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition body = mesh.getRoot().addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition clouds = body.addOrReplaceChild("clouds", CubeListBuilder.create(), PartPose.ZERO);
        // Three depth layers (back, middle, front) plus a small high wisp, so parallax shows between views.
        clouds.addOrReplaceChild("cloud_back", CubeListBuilder.create()
                .texOffs(0, 32).addBox(-3.0F, 18.25F, 1.5F, 5.0F, 1.5F, 1.75F)
                .texOffs(16, 32).addBox(-2.0F, 17.25F, 1.75F, 2.5F, 1.0F, 1.25F)
                .texOffs(16, 32).addBox(1.0F, 17.75F, 2.0F, 1.5F, 0.5F, 1.0F), PartPose.ZERO);
        clouds.addOrReplaceChild("cloud_middle", CubeListBuilder.create()
                .texOffs(0, 40).addBox(-1.0F, 20.25F, -0.75F, 4.25F, 1.25F, 1.5F)
                .texOffs(16, 40).addBox(0.0F, 19.25F, -0.5F, 2.0F, 1.0F, 1.0F)
                .texOffs(16, 40).addBox(-2.0F, 20.75F, -0.5F, 1.0F, 0.75F, 1.0F), PartPose.ZERO);
        clouds.addOrReplaceChild("cloud_front", CubeListBuilder.create()
                .texOffs(0, 48).addBox(-3.25F, 22.0F, -2.75F, 3.75F, 1.25F, 1.0F)
                .texOffs(16, 48).addBox(-2.5F, 21.25F, -2.5F, 1.75F, 0.75F, 0.75F)
                .texOffs(16, 48).addBox(1.25F, 22.25F, -2.25F, 2.0F, 1.0F, 1.0F), PartPose.ZERO);
        clouds.addOrReplaceChild("cloud_wisp", CubeListBuilder.create()
                .texOffs(0, 56).addBox(1.25F, 16.75F, -2.0F, 2.0F, 0.75F, 1.0F)
                .texOffs(0, 56).addBox(-3.25F, 20.5F, 2.25F, 1.5F, 0.75F, 1.0F), PartPose.ZERO);
        body.addOrReplaceChild("right_eye", CubeListBuilder.create()
                .texOffs(40, 0).addBox(-2.25F, 18.5F, -3.75F, 1.0F, 2.0F, 0.5F), PartPose.ZERO);
        body.addOrReplaceChild("left_eye", CubeListBuilder.create()
                .texOffs(40, 0).addBox(1.25F, 18.5F, -3.75F, 1.0F, 2.0F, 0.5F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    static LayerDefinition createOuterLayer() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO)
                .addOrReplaceChild("membrane", CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-4.0F, 16.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override protected void scale(SlimeRenderState state, PoseStack pose) {
        downscaleSlightly(pose); // Same as SlimeRenderer: keeps the membrane off coplanar neighbouring blocks.
        super.scale(state, pose);
    }

    @Override public Identifier getTextureLocation(SlimeRenderState state) { return TEXTURE; }

    @Override public SlimeRenderState createRenderState() { return new SlimeRenderState(); }

    static final class InnerModel extends EntityModel<SlimeRenderState> {
        InnerModel(ModelPart root) { super(root); }
    }

    static final class MembraneModel extends EntityModel<SlimeRenderState> {
        MembraneModel(ModelPart root) { super(root); }
    }

    /** Mirrors vanilla SlimeOuterLayer (invisibility and glowing-outline branches) with the Cloud Slime texture. */
    private static final class MembraneLayer extends RenderLayer<SlimeRenderState, InnerModel> {
        private final MembraneModel model;

        MembraneLayer(CloudSlimeRenderer parent, MembraneModel model) {
            super(parent);
            this.model = model;
        }

        @Override
        public void submit(PoseStack pose, SubmitNodeCollector collector, int light,
                           SlimeRenderState state, float yRot, float xRot) {
            boolean glowingInvisible = state.appearsGlowing() && state.isInvisible;
            if (state.isInvisible && !glowingInvisible) return;
            // Culled so each face blends exactly once regardless of face submission order.
            RenderType type = glowingInvisible ? RenderTypes.outline(TEXTURE) : RenderTypes.entityTranslucentCull(TEXTURE);
            collector.order(1).submitModel(model, state, pose, type, light,
                    LivingEntityRenderer.getOverlayCoords(state, 0.0F), state.outlineColor);
        }
    }
}
