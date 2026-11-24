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
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Skyguard Archer: the Blockbench model (scripts/bbmodel-capture/convert.py) on the vanilla skeleton renderer, so the
 * skeleton bow-aim pose is kept. The Celestial Longbow is drawn through the vanilla held-item path: a render-only
 * bow stack whose item model is the longbow, so hand placement is exactly vanilla's.
 */
final class SkyguardArcherRenderer extends AbstractSkeletonRenderer<SkyguardArcher, SkeletonRenderState> {
    static final Identifier TEXTURE = ModIds.id("textures/entity/bb_skyguard_archer.png");
    /** Created lazily: item components are not bound yet while renderers register. */
    private ItemStack heldBow;

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
    public void extractRenderState(SkyguardArcher entity, SkeletonRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        if (heldBow == null) {
            heldBow = new ItemStack(Items.BOW);
            heldBow.set(DataComponents.ITEM_MODEL, ModIds.id("skyguard_archer_weapon"));
        }
        state.rightHandItemStack = heldBow;
        itemModelResolver.updateForLiving(state.rightHandItemState, heldBow,
                ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, entity);
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
