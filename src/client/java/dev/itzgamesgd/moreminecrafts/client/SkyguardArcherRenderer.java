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
 * skeleton bow-aim pose is kept. The Celestial Longbow is drawn in the left hand by {@link HeldWeaponLayer}; the
 * vanilla bow item is not drawn.
 */
final class SkyguardArcherRenderer extends AbstractSkeletonRenderer<SkyguardArcher, SkeletonRenderState> {
    static final Identifier TEXTURE = ModIds.id("textures/entity/bb_skyguard_archer.png");
    static final Identifier WEAPON_TEXTURE = ModIds.id("textures/entity/bb_skyguard_archer_weapon.png");

    SkyguardArcherRenderer(EntityRendererProvider.Context context) {
        super(context, ModelLayers.SKELETON_ARMOR, new ArcherModel());
        addLayer(new HeldWeaponLayer<>(this,
                BbModelParts.buildWeapon("skyguard_archer_weapon", "skyguard_archer", "left_arm"), WEAPON_TEXTURE, HumanoidArm.LEFT));
    }

    static void register() {
        EntityRendererRegistry.register(SkyNormalMobs.SKYGUARD_ARCHER, SkyguardArcherRenderer::new);
    }

    @Override
    public Identifier getTextureLocation(SkeletonRenderState state) {
        return TEXTURE;
    }

    @Override
    public void extractRenderState(SkyguardArcher entity, SkeletonRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        // The longbow is drawn by HeldWeaponLayer, so the vanilla bow item from the entity is not drawn.
        state.rightHandItemState.clear();
        state.leftHandItemState.clear();
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
