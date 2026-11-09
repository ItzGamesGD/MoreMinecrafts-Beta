package dev.itzgamesgd.moreminecrafts.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.mob.sky.SkyNormalMobs;
import dev.itzgamesgd.moreminecrafts.mob.sky.SkyguardArcher;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.monster.skeleton.SkeletonModel;
import net.minecraft.client.renderer.entity.AbstractSkeletonRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.SkeletonRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;

/**
 * P2-C1 Skyguard Archer (Concept B, Celestial Patrol Archer): a Steve-proportioned humanoid on the vanilla skeleton
 * renderer, so the skeleton bow-aim pose and vanilla bow item rendering are kept unchanged.
 *
 * <p>Texture layout follows the 64x64 player skin format. The leg-overlay regions are repurposed for the cape and
 * shoulder guards; the unused corner at (56, 16) holds the hood laurel.
 */
final class SkyguardArcherRenderer extends AbstractSkeletonRenderer<SkyguardArcher, SkeletonRenderState> {
    static final ModelLayerLocation LAYER = new ModelLayerLocation(ModIds.id("skyguard_archer"), "main");
    static final Identifier TEXTURE = ModIds.id("textures/entity/skyguard_archer.png");

    SkyguardArcherRenderer(EntityRendererProvider.Context context) {
        super(context, ModelLayers.SKELETON_ARMOR, new ArcherModel(context.bakeLayer(LAYER)));
    }

    static void register() {
        ModelLayerRegistry.registerModelLayer(LAYER, SkyguardArcherRenderer::createLayer);
        EntityRendererRegistry.register(SkyNormalMobs.SKYGUARD_ARCHER, SkyguardArcherRenderer::new);
    }

    static LayerDefinition createLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();
        CubeDeformation overlay = new CubeDeformation(0.25F);

        // Head + hood (vanilla hat layer at 32,0) + small gold laurel on the hood brow.
        PartDefinition head = root.getChild("head");
        head.addOrReplaceChild("laurel", CubeListBuilder.create().texOffs(56, 16)
                .addBox(-1.5F, -7.9F, -5.0F, 3.0F, 2.0F, 1.0F), PartPose.ZERO);

        // Body + layered tunic overlay + thin static cape (7x17x1 UV, widened to 9 and thinned to 0.3).
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16)
                .addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F), PartPose.ZERO);
        body.addOrReplaceChild("tunic", CubeListBuilder.create().texOffs(16, 32)
                .addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, overlay), PartPose.ZERO);
        body.addOrReplaceChild("cape", CubeListBuilder.create().texOffs(0, 32)
                        .addBox(-3.5F, 0.0F, 2.45F, 7.0F, 17.0F, 1.0F, new CubeDeformation(1.0F, 0.0F, -0.35F)),
                PartPose.rotation(0.06F, 0.0F, 0.0F));

        // Steve-width arms; the left arm and leg use their own skin regions.
        PartDefinition rightArm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16)
                .addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(-5.0F, 2.0F, 0.0F));
        rightArm.addOrReplaceChild("right_sleeve", CubeListBuilder.create().texOffs(40, 32)
                .addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, overlay), PartPose.ZERO);
        rightArm.addOrReplaceChild("right_pauldron", CubeListBuilder.create().texOffs(0, 50)
                .addBox(-3.4F, -2.0F, -2.0F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.6F)), PartPose.ZERO);
        PartDefinition leftArm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(32, 48)
                .addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(5.0F, 2.0F, 0.0F));
        leftArm.addOrReplaceChild("left_sleeve", CubeListBuilder.create().texOffs(48, 48)
                .addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, overlay), PartPose.ZERO);
        leftArm.addOrReplaceChild("left_pauldron", CubeListBuilder.create().texOffs(0, 57)
                .addBox(-0.6F, -2.0F, -2.0F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.6F)), PartPose.ZERO);
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(16, 48)
                .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public Identifier getTextureLocation(SkeletonRenderState state) {
        return TEXTURE;
    }

    @Override
    public SkeletonRenderState createRenderState() {
        return new SkeletonRenderState();
    }

    private int lastProbeTick = -1;
    private static final boolean CAPTURE = "1".equals(System.getenv("MOREMINECRAFTS_ARCHER_CAPTURE"));

    @Override
    public void extractRenderState(SkyguardArcher entity, SkeletonRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        if (CAPTURE && entity.tickCount % 10 == 0 && entity.tickCount != lastProbeTick) {
            lastProbeTick = entity.tickCount;
            dev.itzgamesgd.moreminecrafts.MoreMinecrafts.LOGGER.info(
                    "ARCHER_POSE entity={} tick={} aggressive={} holdingBow={} rightPose={}",
                    entity.getId(), entity.tickCount, state.isAggressive, state.isHoldingBow, state.rightArmPose);
        }
    }

    /** Skeleton bow-aim animation with humanoid hand placement for 4 px arms. */
    static final class ArcherModel extends SkeletonModel<SkeletonRenderState> {
        ArcherModel(ModelPart root) {
            super(root);
        }

        @Override
        public void translateToHand(SkeletonRenderState state, HumanoidArm arm, PoseStack poseStack) {
            // SkeletonModel shifts the item 1 px inward for its 2 px arms; 4 px arms use the humanoid placement.
            root().translateAndRotate(poseStack);
            getArm(arm).translateAndRotate(poseStack);
        }
    }
}
