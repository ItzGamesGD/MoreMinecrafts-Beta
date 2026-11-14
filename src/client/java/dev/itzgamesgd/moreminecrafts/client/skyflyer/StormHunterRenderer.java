package dev.itzgamesgd.moreminecrafts.client.skyflyer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.PhantomRenderer;
import net.minecraft.client.renderer.entity.state.PhantomRenderState;
import net.minecraft.resources.Identifier;

/** Vanilla Phantom model, wing animation and eye layer for the Storm Hunter; only the texture comes from {@link SkyFlyerTextures}. */
public final class StormHunterRenderer extends PhantomRenderer {
    public StormHunterRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public Identifier getTextureLocation(PhantomRenderState state) {
        return SkyFlyerTextures.STORM_HUNTER;
    }
}
