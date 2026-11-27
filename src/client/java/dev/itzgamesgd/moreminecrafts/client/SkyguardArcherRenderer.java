package dev.itzgamesgd.moreminecrafts.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.mob.sky.SkyNormalMobs;
import dev.itzgamesgd.moreminecrafts.mob.sky.SkyguardArcher;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.skeleton.SkeletonModel;
import net.minecraft.client.renderer.entity.AbstractSkeletonRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.SkeletonRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;

/**
 * Skyguard Archer: the Blockbench model (scripts/bbmodel-capture/convert.py) on the vanilla skeleton renderer, so the
 * skeleton bow-aim pose and the vanilla bow item are kept. The model has no weapon of its own: the archer holds the
 * default vanilla bow (a temporary stand-in until a custom bow model is attached).
 */
final class SkyguardArcherRenderer extends AbstractSkeletonRenderer<SkyguardArcher, SkeletonRenderState> {
    static final Identifier TEXTURE = ModIds.id("textures/entity/bb_skyguard_archer.png");

    SkyguardArcherRenderer(EntityRendererProvider.Context context) {
        super(context, ModelLayers.SKELETON_ARMOR, new ArcherModel());
    }

    static void register() {
        EntityRendererRegistry.register(SkyNormalMobs.SKYGUARD_ARCHER, SkyguardArcherRenderer::new);
    }

    @Override
    public Identifier getTextureLocation(SkeletonRenderState state) {
        return TEXTURE;
    }

    @Override
    public SkeletonRenderState createRenderState() {
        return new SkeletonRenderState();
    }

    /** Skeleton bow-aim animation with humanoid hand placement for 4 px arms. */
    static final class ArcherModel extends SkeletonModel<SkeletonRenderState> {
        ArcherModel() {
            super(BbModelParts.build("skyguard_archer", true));
        }

        @Override
        public void translateToHand(SkeletonRenderState state, HumanoidArm arm, PoseStack poseStack) {
            // SkeletonModel shifts the item 1 px inward for its 2 px arms; 4 px arms use the humanoid placement.
            root().translateAndRotate(poseStack);
            getArm(arm).translateAndRotate(poseStack);
        }
    }
}
