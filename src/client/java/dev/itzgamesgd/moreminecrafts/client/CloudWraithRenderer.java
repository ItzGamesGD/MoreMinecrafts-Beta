package dev.itzgamesgd.moreminecrafts.client;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import dev.itzgamesgd.moreminecrafts.mob.sky.CloudWraith;
import dev.itzgamesgd.moreminecrafts.mob.sky.SkyNormalMobs;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.monster.zombie.ZombieModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/** B1 geometry animated with the vanilla zombie attack state and client-only wisps. */
final class CloudWraithRenderer extends HumanoidMobRenderer<CloudWraith, ZombieRenderState, HumanoidModel<ZombieRenderState>> {
    private static final ModelLayerLocation LAYER = new ModelLayerLocation(ModIds.id("cloud_wraith"), "main");
    private static final Identifier TEXTURE = ModIds.id("textures/entity/cloud_wraith.png");

    CloudWraithRenderer(EntityRendererProvider.Context context) {
        super(context, new WraithModel(context.bakeLayer(LAYER)), 0.5F);
    }

    static void register() {
        ModelLayerRegistry.registerModelLayer(LAYER, CloudWraithRenderer::createLayer);
        EntityRendererRegistry.register(SkyNormalMobs.CLOUD_WRAITH, CloudWraithRenderer::new);
    }

    private static LayerDefinition createLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        var root = mesh.getRoot();
        // Give the left limbs their own skin regions, allowing asymmetric cutouts.
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(32, 48)
                .addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(16, 48)
                .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override public ZombieRenderState createRenderState() { return new ZombieRenderState(); }

    private int lastProbeId = -1;
    private int lastProbeTick = -1;

    @Override public void extractRenderState(CloudWraith entity, ZombieRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        // AbstractZombieRenderer normally supplies this field; keep its vanilla behaviour.
        state.isAggressive = entity.isAggressive();
        if ("1".equals(System.getenv("MOREMINECRAFTS_WRAITH_CAPTURE"))
                && (entity.getId() != lastProbeId || entity.tickCount != lastProbeTick)) {
            lastProbeId = entity.getId();
            lastProbeTick = entity.tickCount;
            MoreMinecrafts.LOGGER.info("WRAITH_ANIM entity={} tick={} age={} swing={} aggressive={} walk={} hurt={} death={}",
                    entity.getId(), entity.tickCount, state.ageInTicks, state.swingAnimation, state.isAggressive,
                    state.walkAnimationSpeed, state.hasRedOverlay, state.deathTime);
        }
    }

    @Override public Identifier getTextureLocation(ZombieRenderState state) { return TEXTURE; }

    @Override protected RenderType getRenderType(ZombieRenderState state, boolean bodyVisible,
                                                  boolean forceTransparent, boolean appearGlowing) {
        // Keep vanilla invisibility and outline handling; alpha is supplied by the texture only.
        return bodyVisible ? RenderTypes.entityTranslucentCull(TEXTURE)
                : super.getRenderType(state, false, forceTransparent, appearGlowing);
    }

    private static final class WraithModel extends ZombieModel<ZombieRenderState> {
        private final ModelPart root;
        private final float restY;

        WraithModel(ModelPart root) {
            super(root);
            this.root = root;
            this.restY = root.y;
        }

        @Override public void setupAnim(ZombieRenderState state) {
            super.setupAnim(state);
            if (state.deathTime > 0.0F) {
                // The renderer supplies the normal death roll; no ghost motion after death.
                root.y = restY;
                rightArm.xRot = leftArm.xRot = -(float) Math.PI / 2.25F;
                rightArm.yRot = -0.1F;
                leftArm.yRot = 0.1F;
                rightArm.zRot = leftArm.zRot = 0.0F;
                rightLeg.xRot = leftLeg.xRot = 0.0F;
                rightLeg.yRot = leftLeg.yRot = 0.0F;
                rightLeg.zRot = leftLeg.zRot = 0.0F;
                return;
            }
            float phase = state.ageInTicks;
            // 2.4 model pixels = 0.15 blocks, centered above the collision surface.
            root.y = restY - 2.4F - 2.4F * Mth.sin(phase * 0.075F);
            // Vanilla already bobs both arms. A small phase offset is suppressed during a swing.
            if (state.swingAnimation <= 0.0F) {
                rightArm.xRot += 0.025F * Mth.sin(phase * 0.055F);
                leftArm.xRot += 0.025F * Mth.sin(phase * 0.055F + 1.1F);
            }
            rightLeg.zRot += 0.055F * Mth.sin(phase * 0.065F + 0.8F);
            leftLeg.zRot += 0.055F * Mth.sin(phase * 0.065F + 2.0F);
            // Knockback can produce a very large vanilla walk swing; keep the wisps restrained.
            rightLeg.xRot = Mth.clamp(rightLeg.xRot + 0.035F * Mth.sin(phase * 0.055F + 1.3F), -0.45F, 0.45F);
            leftLeg.xRot = Mth.clamp(leftLeg.xRot + 0.035F * Mth.sin(phase * 0.055F + 2.5F), -0.45F, 0.45F);
        }
    }
}
