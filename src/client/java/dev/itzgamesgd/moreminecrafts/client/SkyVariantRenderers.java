package dev.itzgamesgd.moreminecrafts.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.mob.sky.SkyVariantMobs;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.slime.SlimeModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.CreeperRenderer;
import net.minecraft.client.renderer.entity.EndermanRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.SkeletonRenderer;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.client.renderer.entity.layers.EnderEyesLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.layers.SlimeOuterLayer;
import net.minecraft.client.renderer.entity.state.CreeperRenderState;
import net.minecraft.client.renderer.entity.state.EndermanRenderState;
import net.minecraft.client.renderer.entity.state.SkeletonRenderState;
import net.minecraft.client.renderer.entity.state.SlimeRenderState;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/**
 * Group B sky-island vanilla variants: texture-only adapters that keep each exact vanilla renderer, model layers,
 * armor/held-item/carried-block layers and the vanilla creeper charge layer. Textures live under
 * {@code textures/entity/sky_variants/} so they stay separate from the forty-entry catalog folder.
 */
final class SkyVariantRenderers {
    private SkyVariantRenderers() {}

    static Identifier texture(String id) {
        return ModIds.id("textures/entity/sky_variants/" + id + ".png");
    }

    static void register() {
        EntityRendererRegistry.register(SkyVariantMobs.WEATHERED_ZOMBIE, context -> zombie(context, "weathered_zombie"));
        EntityRendererRegistry.register(SkyVariantMobs.SKY_ARCHER, context -> skeleton(context, "sky_archer"));
        EntityRendererRegistry.register(SkyVariantMobs.ROCK_CREEPER, context -> creeper(context, "rock_creeper"));
        EntityRendererRegistry.register(SkyVariantMobs.PALE_ENDERMAN, context -> enderman(context, "pale_enderman"));
        EntityRendererRegistry.register(SkyVariantMobs.AZURE_SLIME, context -> slime(context, "azure_slime"));
    }

    static ZombieRenderer zombie(EntityRendererProvider.Context context, String id) {
        Identifier base = texture(id);
        Identifier baby = texture(id + "_baby");
        return new ZombieRenderer(context) {
            @Override
            public Identifier getTextureLocation(ZombieRenderState state) {
                return state.isBaby ? baby : base;
            }
        };
    }

    static SkeletonRenderer skeleton(EntityRendererProvider.Context context, String id) {
        Identifier base = texture(id);
        return new SkeletonRenderer(context) {
            @Override
            public Identifier getTextureLocation(SkeletonRenderState state) {
                return base;
            }
        };
    }

    static CreeperRenderer creeper(EntityRendererProvider.Context context, String id) {
        Identifier base = texture(id);
        return new CreeperRenderer(context) {
            @Override
            public Identifier getTextureLocation(CreeperRenderState state) {
                return base;
            }
        };
    }

    static EndermanRenderer enderman(EntityRendererProvider.Context context, String id) {
        Identifier base = texture(id);
        RenderType eyes = RenderTypes.eyes(texture(id + "_eyes"));
        return new EndermanRenderer(context) {
            {
                layers.replaceAll(layer -> layer instanceof EnderEyesLayer
                        ? new EnderEyesLayer(this) {
                            @Override
                            public RenderType renderType() { return eyes; }
                        } : layer);
            }

            @Override
            public Identifier getTextureLocation(EndermanRenderState state) {
                return base;
            }
        };
    }

    static SlimeRenderer slime(EntityRendererProvider.Context context, String id) {
        Identifier base = texture(id);
        return new SlimeRenderer(context) {
            {
                layers.replaceAll(layer -> layer instanceof SlimeOuterLayer
                        ? new RenderLayer<SlimeRenderState, SlimeModel>(this) {
                            private final SlimeModel outerModel = new SlimeModel(
                                    context.bakeLayer(ModelLayers.SLIME_OUTER));

                            @Override
                            public void submit(PoseStack pose, SubmitNodeCollector collector, int light,
                                               SlimeRenderState state, float yRot, float xRot) {
                                // Same as CustomMobRenderers.slime: vanilla hardcodes slime.png in its outer layer.
                                boolean glowingInvisible = state.appearsGlowing() && state.isInvisible;
                                if (state.isInvisible && !glowingInvisible) return;
                                RenderType type = glowingInvisible ? RenderTypes.outline(base)
                                        : RenderTypes.entityTranslucent(base);
                                collector.order(1).submitModel(outerModel, state, pose, type, light,
                                        LivingEntityRenderer.getOverlayCoords(state, 0.0F), state.outlineColor);
                            }
                        } : layer);
            }

            @Override
            public Identifier getTextureLocation(SlimeRenderState state) {
                return base;
            }
        };
    }
}
