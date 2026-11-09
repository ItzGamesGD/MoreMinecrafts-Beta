package dev.itzgamesgd.moreminecrafts.client;

import dev.itzgamesgd.moreminecrafts.mob.sky.SkyNormalMobs;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.HuskRenderer;
import net.minecraft.client.renderer.entity.SilverfishRenderer;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.client.renderer.entity.ZombieRenderer;

/**
 * Renderers for the five Sky Realm normal mobs (Cloud Wraith P2-B and Skyguard Archer P2-C1 are custom).
 * The remaining placeholders reuse the exact vanilla renderers and textures so the
 * server-side entities are safe to track on a client; custom models, animations and final textures are out of P1 scope.
 */
final class SkyMobRenderers {
    private SkyMobRenderers() {}

    static void register() {
        CloudWraithRenderer.register();
        EntityRendererRegistry.register(SkyNormalMobs.FALLEN_SENTINEL, HuskRenderer::new);
        EntityRendererRegistry.register(SkyNormalMobs.CRYSTAL_BUG, SilverfishRenderer::new);
        EntityRendererRegistry.register(SkyNormalMobs.CLOUD_SLIME, SlimeRenderer::new);
        SkyguardArcherRenderer.register();
    }
}
