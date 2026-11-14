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
 * Cloud Slime P2-E1-R2 (approved concept A, cloud creature): static model and texture only.
 *
 * <p>Same two-pass structure as the vanilla slime: a cutout model (core, eyes, exterior cloud masses), then an
 * ordered translucent membrane cube. Opaque geometry writes depth before the membrane blends, so clouds hide the
 * membrane they cover without sorting artifacts. Size scaling, the vanilla landing squish and the shadow are inherited
 * unchanged from {@link AbstractCubeMobRenderer}, so every cloud part scales and squishes with the body.
 *
 * <p>Hierarchy kept for later phases: {@code body} (both passes) for squash/stretch, and {@code clouds} with one
 * child per cluster for cloud motion. Neither is animated here.
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

    /**
     * Model space: the membrane spans x/z -4..4 and y 16..24 (y grows downward; 24 is the ground).
     * Concept A: an opaque pale-blue core gives the body substance under the membrane, cyan eyes sit just in front of
     * the membrane face, and white cloud masses grow around the outside. No cloud or eye face is coplanar with a
     * membrane face, and the lower clouds stop 0.25 px above the ground plane.
     */
    static LayerDefinition createInnerLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition body = mesh.getRoot().addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        body.addOrReplaceChild("core", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-3.0F, 17.0F, -3.0F, 6.0F, 6.0F, 6.0F), PartPose.ZERO);
        body.addOrReplaceChild("right_eye", CubeListBuilder.create()
                .texOffs(40, 0).addBox(-2.5F, 19.25F, -4.3F, 1.25F, 2.25F, 0.25F), PartPose.ZERO);
        body.addOrReplaceChild("left_eye", CubeListBuilder.create()
                .texOffs(40, 0).addBox(1.25F, 19.25F, -4.3F, 1.25F, 2.25F, 0.25F), PartPose.ZERO);
        PartDefinition clouds = body.addOrReplaceChild("clouds", CubeListBuilder.create(), PartPose.ZERO);
        // Large fluffy mass over the top: two offset slabs overhanging the edges, a stepped dome and a crown puff,
        // plus two short lumps hanging over the front edge above the eyes.
        clouds.addOrReplaceChild("cloud_top", CubeListBuilder.create()
                .texOffs(0, 32).addBox(-4.75F, 14.5F, -4.5F, 7.25F, 2.0F, 8.0F)
                .texOffs(0, 32).addBox(-2.0F, 15.0F, -3.5F, 6.75F, 1.75F, 8.25F)
                .texOffs(0, 32).addBox(-3.5F, 13.0F, -3.0F, 5.0F, 1.5F, 5.75F)
                .texOffs(0, 32).addBox(0.5F, 13.5F, -2.25F, 3.25F, 1.5F, 5.0F)
                .texOffs(0, 32).addBox(-1.75F, 12.0F, -1.5F, 2.5F, 1.0F, 3.0F)
                .texOffs(0, 32).addBox(-4.25F, 13.75F, 0.5F, 1.75F, 0.75F, 2.5F)
                .texOffs(0, 32).addBox(2.75F, 14.25F, -3.75F, 1.5F, 0.75F, 2.25F)
                // Puffs spilling over the front edge and down two upper corners.
                .texOffs(0, 32).addBox(-4.75F, 16.5F, -4.75F, 1.5F, 1.5F, 2.0F)
                .texOffs(0, 32).addBox(-2.75F, 16.5F, -4.6F, 1.75F, 0.5F, 1.0F)
                .texOffs(0, 32).addBox(1.75F, 16.5F, -4.75F, 2.25F, 0.75F, 1.25F)
                .texOffs(0, 32).addBox(3.25F, 16.5F, 1.5F, 1.5F, 1.25F, 3.25F), PartPose.ZERO);
        clouds.addOrReplaceChild("cloud_right", CubeListBuilder.create()
                .texOffs(0, 48).addBox(3.5F, 17.5F, -2.25F, 1.75F, 2.0F, 3.75F)
                .texOffs(0, 48).addBox(4.75F, 18.0F, -1.5F, 1.25F, 1.25F, 2.0F)
                .texOffs(0, 48).addBox(3.5F, 19.5F, -1.75F, 1.25F, 0.75F, 2.5F), PartPose.ZERO);
        clouds.addOrReplaceChild("cloud_left", CubeListBuilder.create()
                .texOffs(0, 48).addBox(-5.75F, 19.5F, -1.0F, 2.25F, 2.0F, 3.5F)
                .texOffs(0, 48).addBox(-6.5F, 20.0F, -0.25F, 1.0F, 1.25F, 1.75F)
                .texOffs(0, 48).addBox(-5.25F, 21.5F, -0.5F, 1.75F, 0.75F, 2.25F), PartPose.ZERO);
        clouds.addOrReplaceChild("cloud_back", CubeListBuilder.create()
                .texOffs(0, 48).addBox(-1.25F, 18.25F, 3.5F, 3.75F, 2.25F, 1.75F)
                .texOffs(0, 48).addBox(-0.25F, 18.75F, 5.0F, 1.75F, 1.25F, 0.75F), PartPose.ZERO);
        clouds.addOrReplaceChild("cloud_lower", CubeListBuilder.create()
                .texOffs(0, 48).addBox(-5.0F, 22.0F, -4.75F, 2.75F, 1.75F, 2.25F)
                .texOffs(0, 48).addBox(-5.5F, 22.75F, -3.25F, 1.0F, 1.0F, 1.25F)
                .texOffs(0, 48).addBox(2.0F, 22.25F, 1.5F, 3.25F, 1.5F, 3.25F)
                .texOffs(0, 48).addBox(-4.5F, 22.5F, 3.0F, 2.5F, 1.25F, 2.0F), PartPose.ZERO);
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
