package dev.itzgamesgd.moreminecrafts.client;

import dev.itzgamesgd.moreminecrafts.mob.sky.SkyNormalMobs;
import dev.itzgamesgd.moreminecrafts.mob.sky.SkyguardArcher;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AbstractSkeletonRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HuskRenderer;
import net.minecraft.client.renderer.entity.SilverfishRenderer;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.client.renderer.entity.state.SkeletonRenderState;
import net.minecraft.resources.Identifier;

/**
 * Placeholder renderers for the five Sky Realm normal mobs. They reuse the exact vanilla renderers and textures so the
 * server-side entities are safe to track on a client; custom models, animations and final textures are out of P1 scope.
 */
final class SkyMobRenderers {
    private SkyMobRenderers() {}

    static void register() {
        CloudWraithRenderer.register();
        EntityRendererRegistry.register(SkyNormalMobs.FALLEN_SENTINEL, HuskRenderer::new);
        EntityRendererRegistry.register(SkyNormalMobs.CRYSTAL_BUG, SilverfishRenderer::new);
        EntityRendererRegistry.register(SkyNormalMobs.CLOUD_SLIME, SlimeRenderer::new);
        EntityRendererRegistry.register(SkyNormalMobs.SKYGUARD_ARCHER, SkyMobRenderers::archer);
    }

    private static AbstractSkeletonRenderer<SkyguardArcher, SkeletonRenderState> archer(
            EntityRendererProvider.Context context) {
        Identifier texture = Identifier.withDefaultNamespace("textures/entity/skeleton/skeleton.png");
        return new AbstractSkeletonRenderer<>(context, ModelLayers.SKELETON, ModelLayers.SKELETON_ARMOR) {
            @Override
            public Identifier getTextureLocation(SkeletonRenderState state) {
                return texture;
            }

            @Override
            public SkeletonRenderState createRenderState() {
                return new SkeletonRenderState();
            }
        };
    }
}
