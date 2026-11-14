package dev.itzgamesgd.moreminecrafts.client;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.mob.pyramid.PyramidMobs;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.HuskRenderer;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.client.renderer.entity.SkeletonRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.client.renderer.entity.state.SkeletonRenderState;
import net.minecraft.resources.Identifier;

final class PyramidMobRenderers {
    private PyramidMobRenderers() {}
    private static Identifier texture(String name, boolean baby) {
        return ModIds.id("textures/entity/pyramid/" + name + (baby ? "_baby" : "") + ".png");
    }
    private static HuskRenderer husk(EntityRendererProvider.Context context, String name) {
        return new HuskRenderer(context) {
            @Override public Identifier getTextureLocation(ZombieRenderState state) { return texture(name, state.isBaby); }
        };
    }
    static void register() {
        EntityRendererRegistry.register(PyramidMobs.BURIED_MUMMY, c -> husk(c, "buried_mummy"));
        EntityRendererRegistry.register(PyramidMobs.GILDED_MUMMY, c -> husk(c, "gilded_mummy"));
        EntityRendererRegistry.register(PyramidMobs.FUNERAL_SERVANT, c -> new ZombieRenderer(c) {
            @Override public Identifier getTextureLocation(ZombieRenderState state) { return texture("funeral_servant", state.isBaby); }
        });
        EntityRendererRegistry.register(PyramidMobs.CRYPT_ARCHER, c -> new SkeletonRenderer(c) {
            @Override public Identifier getTextureLocation(SkeletonRenderState state) { return texture("crypt_archer", false); }
        });
    }
}
