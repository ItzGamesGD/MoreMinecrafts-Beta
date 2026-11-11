package dev.itzgamesgd.moreminecrafts.client;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.mob.sky.FallenSentinel;
import dev.itzgamesgd.moreminecrafts.mob.sky.SkyNormalMobs;
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
import net.minecraft.client.model.monster.zombie.BabyZombieModel;
import net.minecraft.client.model.monster.zombie.ZombieModel;
import net.minecraft.client.renderer.entity.AbstractZombieRenderer;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.resources.Identifier;

/**
 * P2-F1 Fallen Sentinel, approved concept D "Blood Seraph": closed black-steel helm with no eyes, a floating segmented
 * crimson halo, layered black wings, heavy plate with dark-gold trim and a crimson tabard. Static model and texture.
 *
 * <p>This is the vanilla husk renderer path with only the model and texture replaced: {@code HuskRenderer} is a
 * {@code ZombieRenderer} on the {@code AbstractZombieRenderer}, so idle, walking, head tracking, the raised zombie arm
 * pose and attack swing, hurt tint, death roll, water-conversion shake and the husk armour layer are all inherited.
 *
 * <p>The halo and wings are children of {@code body}, so they keep the torso frame (they do not tilt with the head) and
 * follow every body rotation. The only added animation code makes the front and back tabard panels follow the leading
 * and trailing leg so the cloth does not cut through the legs while walking.
 *
 * <p>Every texOffs below is registered in scripts/fallen-sentinel-validation/texture.py, which asserts no overlaps.
 */
final class FallenSentinelRenderer extends AbstractZombieRenderer<FallenSentinel, ZombieRenderState, ZombieModel<ZombieRenderState>> {
    static final ModelLayerLocation LAYER = new ModelLayerLocation(ModIds.id("fallen_sentinel"), "main");
    static final Identifier TEXTURE = ModIds.id("textures/entity/fallen_sentinel.png");

    FallenSentinelRenderer(EntityRendererProvider.Context context) {
        super(context, new SentinelModel(context.bakeLayer(LAYER)), new SentinelModel(context.bakeLayer(LAYER)),
                ArmorModelSet.bake(ModelLayers.HUSK_ARMOR, context.getModelSet(), ZombieModel::new),
                ArmorModelSet.bake(ModelLayers.HUSK_BABY_ARMOR, context.getModelSet(), BabyZombieModel::new));
    }

    static void register() {
        ModelLayerRegistry.registerModelLayer(LAYER, FallenSentinelRenderer::createLayer);
        EntityRendererRegistry.register(SkyNormalMobs.FALLEN_SENTINEL, FallenSentinelRenderer::new);
    }

    @Override public Identifier getTextureLocation(ZombieRenderState state) { return TEXTURE; }

    @Override public ZombieRenderState createRenderState() { return new ZombieRenderState(); }

    // Wing arm: an 11 px bone raised 0.5 rad from the shoulder blade; feathers hang from points along it.
    private static final float WING_ARM_LENGTH = 11.0F;
    private static final float WING_ARM_RAISE = 0.5F;
    /** Backward sweep of each wing; kept small so the spread reads from the front as well as from behind. */
    private static final float WING_SWEEP = 0.12F;
    // Layer depth behind the wing pivot (local +z points backwards): long feathers, coverts, short scapulars.
    private static final float LAYER_LONG = 0.0F;
    private static final float LAYER_MID = 1.1F;
    private static final float LAYER_SHORT = 2.2F;
    /** {fraction along the wing arm, feather length, outward fan angle, layer depth}. */
    private static final float[][] FEATHERS = {
            {0.12F, 15, 0.04F, LAYER_LONG}, {0.27F, 15, 0.10F, LAYER_LONG}, {0.42F, 15, 0.17F, LAYER_LONG},
            {0.57F, 14, 0.25F, LAYER_LONG}, {0.72F, 13, 0.33F, LAYER_LONG}, {0.86F, 12, 0.41F, LAYER_LONG},
            {1.00F, 11, 0.50F, LAYER_LONG},
            {0.20F, 10, 0.08F, LAYER_MID}, {0.40F, 10, 0.17F, LAYER_MID}, {0.60F, 9, 0.27F, LAYER_MID},
            {0.80F, 8, 0.37F, LAYER_MID}, {0.98F, 7, 0.48F, LAYER_MID},
            {0.10F, 6, 0.05F, LAYER_SHORT}, {0.35F, 6, 0.15F, LAYER_SHORT}, {0.60F, 5, 0.26F, LAYER_SHORT},
            {0.85F, 5, 0.37F, LAYER_SHORT},
    };
    /** Plumes continuing the upper edge outward past the wing tip: {fraction, length, outward tilt, layer depth}. */
    private static final float[][] PLUMES = { {1.00F, 7, 1.05F, LAYER_MID}, {0.90F, 6, 0.75F, LAYER_LONG} };
    private static final int[] FEATHER_LENGTHS = {15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5};

    private static int featherU(int length) {
        for (int k = 0; k < FEATHER_LENGTHS.length; k++) if (FEATHER_LENGTHS[k] == length) return 8 * k;
        throw new IllegalArgumentException("no feather texture for length " + length);
    }

    static LayerDefinition createLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();

        // ---- closed helm: no face, eyes or eye holes; a solid faceplate with one gold rib --------------------------
        PartDefinition head = root.getChild("head");
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        head.addOrReplaceChild("crest", CubeListBuilder.create().texOffs(32, 0)
                .addBox(-0.5F, -9.5F, -4.25F, 1, 2, 9), PartPose.ZERO);
        head.addOrReplaceChild("faceplate", CubeListBuilder.create().texOffs(52, 0)
                .addBox(-3.0F, -7.0F, -4.75F, 6, 6, 1), PartPose.ZERO);
        head.addOrReplaceChild("right_cheek", CubeListBuilder.create().texOffs(66, 0)
                .addBox(-4.5F, -6.0F, -4.5F, 1, 5, 4), PartPose.ZERO);
        head.addOrReplaceChild("left_cheek", CubeListBuilder.create().texOffs(66, 0).mirror()
                .addBox(3.5F, -6.0F, -4.5F, 1, 5, 4), PartPose.ZERO);
        head.addOrReplaceChild("brow", CubeListBuilder.create().texOffs(76, 0)
                .addBox(-4.5F, -8.0F, -4.5F, 9, 2, 1), PartPose.ZERO);
        head.addOrReplaceChild("neck_guard", CubeListBuilder.create().texOffs(76, 4)
                .addBox(-4.0F, -3.0F, 3.5F, 8, 3, 1), PartPose.ZERO);

        // ---- torso plate and cloth ----------------------------------------------------------------------------------
        PartDefinition body = root.getChild("body");
        body.addOrReplaceChild("collar", CubeListBuilder.create().texOffs(96, 16)
                .addBox(-4.5F, -1.0F, -2.5F, 9, 2, 5, new CubeDeformation(0.1F)), PartPose.ZERO);
        body.addOrReplaceChild("breastplate", CubeListBuilder.create().texOffs(56, 16)
                .addBox(-4.5F, 0.5F, -3.25F, 9, 7, 1), PartPose.ZERO);
        body.addOrReplaceChild("fauld", CubeListBuilder.create().texOffs(56, 25)
                .addBox(-4.0F, 7.5F, -2.75F, 8, 2, 1), PartPose.ZERO);
        body.addOrReplaceChild("backplate", CubeListBuilder.create().texOffs(76, 16)
                .addBox(-4.5F, 0.5F, 2.25F, 9, 9, 1), PartPose.ZERO);
        body.addOrReplaceChild("belt", CubeListBuilder.create().texOffs(96, 24)
                .addBox(-4.5F, 9.5F, -2.5F, 9, 2, 5, new CubeDeformation(0.05F)), PartPose.ZERO);
        body.addOrReplaceChild("tabard_front", CubeListBuilder.create().texOffs(0, 44)
                .addBox(-2.5F, 0.0F, -0.5F, 5, 10, 1), PartPose.offset(0.0F, 11.0F, -2.75F));
        body.addOrReplaceChild("tabard_back", CubeListBuilder.create().texOffs(0, 44)
                .addBox(-2.5F, 0.0F, -0.5F, 5, 10, 1), PartPose.offset(0.0F, 11.0F, 2.75F));

        // ---- floating segmented halo, in the torso frame --------------------------------------------------------
        PartDefinition halo = body.addOrReplaceChild("halo", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, -11.0F, 0.5F, 0.12F, 0.0F, 0.0F));
        float radius = 5.0F;
        for (int k = 0; k < 8; k++) {
            if (k == 5) continue; // the broken gap at the rear left
            float angle = (float) Math.toRadians(22.5 + 45.0 * k);
            boolean shortSegment = k == 2;
            float length = shortSegment ? 2.0F : 3.0F;
            halo.addOrReplaceChild("halo_" + k, CubeListBuilder.create().texOffs(96, shortSegment ? 3 : 0)
                            .addBox(-length / 2.0F, -0.5F, -0.5F, (int) length, 1, 1, new CubeDeformation(0.15F)),
                    PartPose.offsetAndRotation(radius * (float) Math.sin(angle), (k % 2 == 0) ? 0.0F : -0.5F,
                            radius * (float) Math.cos(angle), 0.0F, angle, 0.0F));
        }

        // ---- layered wings from the shoulder blades ----------------------------------------------------------------
        addWing(body, true);
        addWing(body, false);

        // ---- shoulders and forearms ------------------------------------------------------------------------------
        addArmArmour(root.getChild("right_arm"), true);
        addArmArmour(root.getChild("left_arm"), false);
        addLegArmour(root.getChild("right_leg"), true);
        addLegArmour(root.getChild("left_leg"), false);
        return LayerDefinition.create(mesh, 128, 128);
    }

    private static void addWing(PartDefinition body, boolean right) {
        float side = right ? -1.0F : 1.0F;   // right wing extends to -x
        PartDefinition wing = body.addOrReplaceChild(right ? "right_wing" : "left_wing", CubeListBuilder.create()
                        .texOffs(44, 48).mirror(!right).addBox(right ? -4.0F : 0.0F, -2.0F, 0.0F, 4, 4, 3),
                PartPose.offsetAndRotation(2.0F * side, 1.5F, 2.75F, 0.0F, -WING_SWEEP * side, 0.0F));
        wing.addOrReplaceChild("arm", CubeListBuilder.create().texOffs(16, 48).mirror(!right)
                        .addBox(right ? -WING_ARM_LENGTH : 0.0F, -1.0F, 0.6F, (int) WING_ARM_LENGTH, 2, 3),
                PartPose.rotation(0.0F, 0.0F, -WING_ARM_RAISE * side));
        float tipX = side * WING_ARM_LENGTH * (float) Math.cos(WING_ARM_RAISE);
        float tipY = -WING_ARM_LENGTH * (float) Math.sin(WING_ARM_RAISE);
        for (int i = 0; i < FEATHERS.length; i++) {
            float[] f = FEATHERS[i];
            int length = (int) f[1];
            wing.addOrReplaceChild("feather_" + i, CubeListBuilder.create().texOffs(featherU(length), 64).mirror(!right)
                            .addBox(-1.5F, 0.0F, 0.0F, 3, length, 1),
                    PartPose.offsetAndRotation(tipX * f[0], tipY * f[0] + 0.5F, f[3], 0.0F, 0.0F, -f[2] * side));
        }
        for (int i = 0; i < PLUMES.length; i++) {
            float[] p = PLUMES[i];
            int length = (int) p[1];
            wing.addOrReplaceChild("plume_" + i, CubeListBuilder.create().texOffs(88 + 8 * i, 64).mirror(!right)
                            .addBox(-1.5F, -length, 0.0F, 3, length, 1),
                    PartPose.offsetAndRotation(tipX * p[0], tipY * p[0], p[3], 0.0F, 0.0F, p[2] * side));
        }
    }

    private static void addArmArmour(PartDefinition arm, boolean right) {
        arm.addOrReplaceChild("pauldron", CubeListBuilder.create().texOffs(0, 32).mirror(!right)
                .addBox(right ? -5.0F : -1.0F, -3.5F, -3.0F, 6, 3, 6), PartPose.ZERO);
        arm.addOrReplaceChild("pauldron_lower", CubeListBuilder.create().texOffs(24, 32).mirror(!right)
                .addBox(right ? -5.5F : 0.5F, -1.0F, -2.5F, 5, 2, 5), PartPose.ZERO);
        arm.addOrReplaceChild("pauldron_top", CubeListBuilder.create().texOffs(44, 32).mirror(!right)
                .addBox(right ? -4.0F : 0.0F, -5.0F, -2.0F, 4, 2, 4), PartPose.ZERO);
        arm.addOrReplaceChild("vambrace", CubeListBuilder.create().texOffs(60, 32).mirror(!right)
                .addBox(right ? -3.5F : -1.5F, 5.0F, -2.5F, 5, 5, 5), PartPose.ZERO);
    }

    private static void addLegArmour(PartDefinition leg, boolean right) {
        leg.addOrReplaceChild("thigh", CubeListBuilder.create().texOffs(80, 32).mirror(!right)
                .addBox(-2.5F, 0.0F, -2.5F, 5, 4, 5), PartPose.ZERO);
        leg.addOrReplaceChild("knee", CubeListBuilder.create().texOffs(100, 32).mirror(!right)
                .addBox(-2.0F, 4.5F, -2.75F, 4, 2, 1), PartPose.ZERO);
        leg.addOrReplaceChild("sabaton", CubeListBuilder.create().texOffs(100, 36).mirror(!right)
                .addBox(-2.5F, 10.0F, -3.0F, 5, 2, 5), PartPose.ZERO);
    }

    /** Vanilla zombie animation; the tabard panels only follow the leading (front) and trailing (back) leg. */
    static final class SentinelModel extends ZombieModel<ZombieRenderState> {
        private final ModelPart tabardFront;
        private final ModelPart tabardBack;

        SentinelModel(ModelPart root) {
            super(root);
            this.tabardFront = body.getChild("tabard_front");
            this.tabardBack = body.getChild("tabard_back");
        }

        @Override public void setupAnim(ZombieRenderState state) {
            super.setupAnim(state);
            tabardFront.xRot = Math.min(0.0F, Math.min(rightLeg.xRot, leftLeg.xRot));
            tabardBack.xRot = Math.max(0.0F, Math.max(rightLeg.xRot, leftLeg.xRot));
        }
    }
}
