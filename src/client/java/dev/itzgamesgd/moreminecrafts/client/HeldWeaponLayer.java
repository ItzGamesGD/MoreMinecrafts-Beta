package dev.itzgamesgd.moreminecrafts.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;

/**
 * Draws a custom weapon model in the right hand of a humanoid mob, with its own texture.
 *
 * <p>The weapon root copies the right arm's pose every frame, so the weapon follows the vanilla arm animation (idle,
 * swing, raised arm, bow aim). The weapon's elements are positioned relative to that arm (see
 * {@link BbModelParts#buildWeapon}), so the grip pivot lands in the hand as authored in Blockbench.
 */
final class HeldWeaponLayer<S extends LivingEntityRenderState, M extends EntityModel<? super S>> extends RenderLayer<S, M> {
    private final ModelPart root;
    private final WeaponModel<S> model;
    private final RenderType renderType;

    HeldWeaponLayer(RenderLayerParent<S, M> parent, ModelPart root, Identifier texture) {
        super(parent);
        this.root = root;
        this.model = new WeaponModel<>(root);
        this.renderType = RenderTypes.entityCutoutCull(texture);
    }

    @Override
    public void submit(PoseStack pose, SubmitNodeCollector collector, int light, S state, float yRot, float xRot) {
        ModelPart arm = ((HumanoidModel<?>) getParentModel()).getArm(HumanoidArm.RIGHT);
        root.loadPose(arm.storePose());
        collector.submitModel(model, state, pose, renderType, light,
                LivingEntityRenderer.getOverlayCoords(state, 0.0F), -1);
    }

    /** Model wrapper for the weapon root; it is posed by the layer, never by {@code setupAnim}. */
    static final class WeaponModel<S extends EntityRenderState> extends EntityModel<S> {
        WeaponModel(ModelPart root) { super(root); }
    }
}
