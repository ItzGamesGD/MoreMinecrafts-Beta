package dev.itzgamesgd.moreminecrafts.client;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.mob.pyramid.PyramidMobs;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HuskRenderer;
import net.minecraft.client.renderer.entity.SkeletonRenderer;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.client.renderer.entity.state.SkeletonRenderState;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.resources.Identifier;

/** Texture-only adapters for the pyramid variants: vanilla renderers, model layers and animations. */
final class PyramidMobRenderers {
    private PyramidMobRenderers() {}

    static Identifier texture(String name) {
        return ModIds.id("textures/entity/pyramid/" + name + ".png");
    }

    private static HuskRenderer husk(EntityRendererProvider.Context context, String name) {
        Identifier adult = texture(name);
        Identifier baby = texture(name + "_baby");
        return new HuskRenderer(context) {
            @Override
            public Identifier getTextureLocation(ZombieRenderState state) {
                return state.isBaby ? baby : adult;
            }
        };
    }

    private static ZombieRenderer zombie(EntityRendererProvider.Context context, String name) {
        Identifier adult = texture(name);
        Identifier baby = texture(name + "_baby");
        return new ZombieRenderer(context) {
            @Override
            public Identifier getTextureLocation(ZombieRenderState state) {
                return state.isBaby ? baby : adult;
            }
        };
    }

    private static SkeletonRenderer skeleton(EntityRendererProvider.Context context, String name) {
        Identifier adult = texture(name);
        return new SkeletonRenderer(context) {
            @Override
            public Identifier getTextureLocation(SkeletonRenderState state) {
                return adult;
            }
        };
    }

    static void register() {
        EntityRendererRegistry.register(PyramidMobs.BURIED_MUMMY, c -> husk(c, "buried_mummy"));
        EntityRendererRegistry.register(PyramidMobs.CRYPT_ARCHER, c -> skeleton(c, "crypt_archer"));
        EntityRendererRegistry.register(PyramidMobs.FUNERAL_SERVANT, c -> zombie(c, "funeral_servant"));
        EntityRendererRegistry.register(PyramidMobs.GILDED_MUMMY, c -> husk(c, "gilded_mummy"));
    }
}
