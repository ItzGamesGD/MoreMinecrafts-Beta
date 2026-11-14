package dev.itzgamesgd.moreminecrafts.client.skyflyer;

import dev.itzgamesgd.moreminecrafts.mob.skyflyer.SkyFlyers;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

/** Renderer registration for the Sky Flyers; independent of {@code SkyMobRenderers} (group C). */
public final class SkyFlyerRenderers {
    private SkyFlyerRenderers() {}

    public static void register() {
        EntityRendererRegistry.register(SkyFlyers.SKY_SPIRIT, SkySpiritRenderer::new);
        EntityRendererRegistry.register(SkyFlyers.STORM_HUNTER, StormHunterRenderer::new);
    }
}
