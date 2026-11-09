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
 * <p>The 64x64 texture is a custom atlas: the head, hood, body, arm and leg keep the player-skin positions (left limbs
 * are mirrored, as in vanilla) and the extra armour parts are packed into the rest. The layout, with an overlap check,
 * lives in scripts/skyguard-archer-validation/texture.py.
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

    private static CubeDeformation grow(float all) {
        return new CubeDeformation(all);
    }

    /**
     * Humanoid base (vanilla Steve proportions, vanilla mirrored left limbs) plus layered detail boxes. Every UV
     * offset below is registered in scripts/skyguard-archer-validation/texture.py, which asserts that none overlap.
     */
    static LayerDefinition createLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();

        // ---- hood: inflated cowl, raised gold rim, rear bulge and a three-leaf laurel sprig --------------------
        PartDefinition hat = root.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(32, 0)
                .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, grow(0.75F)), PartPose.ZERO);
        hat.addOrReplaceChild("hood_back", CubeListBuilder.create().texOffs(40, 57)
                .addBox(-3.0F, -7.0F, 3.8F, 6.0F, 5.0F, 2.0F, grow(0.2F)), PartPose.ZERO);
        hat.addOrReplaceChild("rim_top", CubeListBuilder.create().texOffs(16, 58)
                .addBox(-4.0F, -8.0F, -5.0F, 8.0F, 1.0F, 1.0F, new CubeDeformation(0.75F, 0.25F, 0.4F)), PartPose.ZERO);
        hat.addOrReplaceChild("rim_right", CubeListBuilder.create().texOffs(10, 55)
                .addBox(-4.65F, -7.0F, -5.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.1F, 0.2F, 0.4F)), PartPose.ZERO);
        hat.addOrReplaceChild("rim_left", CubeListBuilder.create().texOffs(10, 55).mirror()
                .addBox(3.65F, -7.0F, -5.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.1F, 0.2F, 0.4F)), PartPose.ZERO);
        hat.addOrReplaceChild("laurel_center", CubeListBuilder.create().texOffs(56, 16)
                .addBox(-1.0F, -5.0F, -0.5F, 2.0F, 5.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -8.4F, -4.9F, 0.12F, 0.0F, 0.0F));
        hat.addOrReplaceChild("laurel_right", CubeListBuilder.create().texOffs(56, 22)
                .addBox(-1.0F, -4.0F, -0.5F, 2.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(-0.8F, -8.4F, -4.95F, 0.1F, 0.0F, -0.42F));
        hat.addOrReplaceChild("laurel_left", CubeListBuilder.create().texOffs(56, 22).mirror()
                .addBox(-1.0F, -4.0F, -0.5F, 2.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(0.8F, -8.4F, -4.95F, 0.1F, 0.0F, 0.42F));

        // ---- torso: chest plates, belt, hem, scarf collar and a static three-panel cape ---------------------------
        PartDefinition body = root.getChild("body");
        body.addOrReplaceChild("chest_plate", CubeListBuilder.create().texOffs(16, 32)
                .addBox(-4.0F, 0.0F, -2.0F, 8.0F, 4.0F, 4.0F, grow(0.4F)), PartPose.ZERO);
        body.addOrReplaceChild("belt", CubeListBuilder.create().texOffs(16, 40)
                .addBox(-4.0F, 8.0F, -2.0F, 8.0F, 2.0F, 4.0F, grow(0.5F)), PartPose.ZERO);
        body.addOrReplaceChild("hem", CubeListBuilder.create().texOffs(16, 52)
                .addBox(-4.0F, 10.0F, -2.0F, 8.0F, 2.0F, 4.0F, grow(0.65F)), PartPose.ZERO);
        body.addOrReplaceChild("collar", CubeListBuilder.create().texOffs(16, 46)
                .addBox(-4.0F, -1.0F, -2.0F, 8.0F, 2.0F, 4.0F, grow(1.1F)), PartPose.ZERO);
        // The cape hangs from the shoulders, flares 0.12 rad away from the legs and ends 2 px above the ground.
        body.addOrReplaceChild("cape", CubeListBuilder.create().texOffs(0, 32)
                .addBox(-3.5F, 0.0F, 0.0F, 7.0F, 22.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 1.0F, 2.95F, 0.12F, 0.0F, 0.0F));
        // Flaps sit just behind the centre panel and fan outwards so the cape widens towards the hem.
        body.addOrReplaceChild("cape_flap_right", CubeListBuilder.create().texOffs(56, 32)
                .addBox(-3.0F, 0.0F, 0.0F, 3.0F, 20.0F, 1.0F), PartPose.offsetAndRotation(0.5F, 1.2F, 2.85F, 0.12F, 0.0F, 0.2F));
        body.addOrReplaceChild("cape_flap_left", CubeListBuilder.create().texOffs(56, 32).mirror()
                .addBox(0.0F, 0.0F, 0.0F, 3.0F, 20.0F, 1.0F), PartPose.offsetAndRotation(-0.5F, 1.2F, 2.85F, 0.12F, 0.0F, -0.2F));

        // ---- arms: three stacked shoulder plates (each projects less than the one above) and a bracer ------------
        PartDefinition rightArm = root.getChild("right_arm");
        rightArm.addOrReplaceChild("pauldron_top", CubeListBuilder.create().texOffs(40, 40)
                .addBox(-3.0F, -2.0F, -2.0F, 4.0F, 1.0F, 4.0F, grow(0.9F)), PartPose.ZERO);
        rightArm.addOrReplaceChild("pauldron_mid", CubeListBuilder.create().texOffs(40, 45)
                .addBox(-3.0F, -1.0F, -2.0F, 4.0F, 1.0F, 4.0F, grow(0.7F)), PartPose.ZERO);
        rightArm.addOrReplaceChild("pauldron_low", CubeListBuilder.create().texOffs(40, 40)
                .addBox(-3.0F, 0.0F, -2.0F, 4.0F, 1.0F, 4.0F, grow(0.45F)), PartPose.ZERO);
        rightArm.addOrReplaceChild("bracer", CubeListBuilder.create().texOffs(40, 32)
                .addBox(-3.0F, 4.0F, -2.0F, 4.0F, 4.0F, 4.0F, grow(0.3F)), PartPose.ZERO);
        PartDefinition leftArm = root.getChild("left_arm");
        leftArm.addOrReplaceChild("pauldron_top", CubeListBuilder.create().texOffs(40, 40).mirror()
                .addBox(-1.0F, -2.0F, -2.0F, 4.0F, 1.0F, 4.0F, grow(0.9F)), PartPose.ZERO);
        leftArm.addOrReplaceChild("pauldron_mid", CubeListBuilder.create().texOffs(40, 45).mirror()
                .addBox(-1.0F, -1.0F, -2.0F, 4.0F, 1.0F, 4.0F, grow(0.7F)), PartPose.ZERO);
        leftArm.addOrReplaceChild("pauldron_low", CubeListBuilder.create().texOffs(40, 40).mirror()
                .addBox(-1.0F, 0.0F, -2.0F, 4.0F, 1.0F, 4.0F, grow(0.45F)), PartPose.ZERO);
        leftArm.addOrReplaceChild("bracer", CubeListBuilder.create().texOffs(40, 32).mirror()
                .addBox(-1.0F, 4.0F, -2.0F, 4.0F, 4.0F, 4.0F, grow(0.3F)), PartPose.ZERO);

        // ---- legs: boot cuffs and a split tabard that swings with each leg (no clipping when walking) -------------
        for (boolean left : new boolean[] {false, true}) {
            PartDefinition leg = root.getChild(left ? "left_leg" : "right_leg");
            CubeListBuilder cuff = CubeListBuilder.create().texOffs(40, 50);
            CubeListBuilder tabard = CubeListBuilder.create().texOffs(0, 55);
            if (left) {
                cuff.mirror();
                tabard.mirror();
            }
            leg.addOrReplaceChild("boot_cuff", cuff.addBox(-2.0F, 6.0F, -2.0F, 4.0F, 3.0F, 4.0F, grow(0.4F)), PartPose.ZERO);
            // Different front depths keep the two overlapping halves from z-fighting at the centre seam.
            leg.addOrReplaceChild("tabard", tabard.addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 1.0F,
                    new CubeDeformation(0.5F, 0.3F, left ? 0.6F : 0.55F)), PartPose.ZERO);
        }
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
