package dev.itzgamesgd.moreminecrafts.client;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.mob.pyramid.CryptArcher;
import dev.itzgamesgd.moreminecrafts.mob.pyramid.PyramidMobs;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HuskRenderer;
import net.minecraft.client.renderer.entity.AbstractSkeletonRenderer;
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

    /** Vanilla skeleton model, armour layers and bow animation; the archer is an AbstractSkeleton (see CryptArcher). */
    private static final class CryptArcherRenderer extends AbstractSkeletonRenderer<CryptArcher, SkeletonRenderState> {
        private static final Identifier TEXTURE = texture("crypt_archer");

        CryptArcherRenderer(EntityRendererProvider.Context context) {
            super(context, ModelLayers.SKELETON, ModelLayers.SKELETON_ARMOR);
        }

        @Override
        public Identifier getTextureLocation(SkeletonRenderState state) {
            return TEXTURE;
        }

        @Override
        public SkeletonRenderState createRenderState() {
            return new SkeletonRenderState();
        }
    }

    static void register() {
        EntityRendererRegistry.register(PyramidMobs.BURIED_MUMMY, c -> husk(c, "buried_mummy"));
        EntityRendererRegistry.register(PyramidMobs.CRYPT_ARCHER, CryptArcherRenderer::new);
        EntityRendererRegistry.register(PyramidMobs.FUNERAL_SERVANT, c -> zombie(c, "funeral_servant"));
        EntityRendererRegistry.register(PyramidMobs.GILDED_MUMMY, c -> husk(c, "gilded_mummy"));
    }
}
