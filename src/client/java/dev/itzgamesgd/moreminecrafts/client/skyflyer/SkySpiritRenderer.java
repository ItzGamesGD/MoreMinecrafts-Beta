package dev.itzgamesgd.moreminecrafts.client.skyflyer;

import dev.itzgamesgd.moreminecrafts.mob.skyflyer.SkySpirit;
import net.minecraft.client.model.animal.allay.AllayModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.AllayRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

/** Vanilla Allay model and flight animation for the Sky Spirit; only the texture comes from {@link SkyFlyerTextures}. */
public final class SkySpiritRenderer extends MobRenderer<SkySpirit, AllayRenderState, AllayModel> {
    public SkySpiritRenderer(EntityRendererProvider.Context context) {
        super(context, new AllayModel(context.bakeLayer(ModelLayers.ALLAY)), 0.4F);
    }

    @Override
    public Identifier getTextureLocation(AllayRenderState state) {
        return SkyFlyerTextures.SKY_SPIRIT;
    }

    @Override
    public AllayRenderState createRenderState() {
        return new AllayRenderState();
    }

    @Override
    public void extractRenderState(SkySpirit entity, AllayRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.isDancing = false;
        state.isSpinning = false;
        state.spinningProgress = 0.0F;
        state.holdingAnimationProgress = 0.0F;
    }

    /** Same glow as the vanilla Allay. */
    @Override
    protected int getBlockLightLevel(SkySpirit entity, BlockPos pos) {
        return 15;
    }
}
