package dev.itzgamesgd.moreminecrafts.client;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.mob.sky.CloudWraith;
import dev.itzgamesgd.moreminecrafts.mob.sky.SkyNormalMobs;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/** B1 prototype: a humanoid core with separately articulated cloud fragments. */
final class CloudWraithRenderer extends HumanoidMobRenderer<CloudWraith, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
    private static final ModelLayerLocation LAYER = new ModelLayerLocation(ModIds.id("cloud_wraith"), "main");
    private static final Identifier TEXTURE = ModIds.id("textures/entity/cloud_wraith.png");

    CloudWraithRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<HumanoidRenderState>(context.bakeLayer(LAYER)), 0.5F);
    }

    static void register() {
        ModelLayerRegistry.registerModelLayer(LAYER, CloudWraithRenderer::createLayer);
        EntityRendererRegistry.register(SkyNormalMobs.CLOUD_WRAITH, CloudWraithRenderer::new);
    }

    private static LayerDefinition createLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        var root = mesh.getRoot();
        root.getChild("right_arm").addOrReplaceChild("wisp_right", CubeListBuilder.create().texOffs(0, 32)
                .addBox(-3.0F, 7.0F, -2.0F, 3.0F, 6.0F, 4.0F), PartPose.ZERO);
        root.getChild("left_arm").addOrReplaceChild("wisp_left", CubeListBuilder.create().texOffs(0, 32)
                .addBox(0.0F, 8.0F, -1.5F, 3.0F, 5.0F, 3.0F), PartPose.ZERO);
        root.getChild("body").addOrReplaceChild("lower_cloud", CubeListBuilder.create().texOffs(16, 32)
                .addBox(-4.5F, 9.0F, -2.5F, 9.0F, 5.0F, 5.0F)
                .texOffs(16, 42).addBox(-3.0F, 14.0F, -2.0F, 6.0F, 4.0F, 4.0F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override public HumanoidRenderState createRenderState() { return new HumanoidRenderState(); }
    @Override public Identifier getTextureLocation(HumanoidRenderState state) { return TEXTURE; }
}
