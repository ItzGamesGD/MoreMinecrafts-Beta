package dev.itzgamesgd.moreminecrafts.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.slime.SlimeModel;
import net.minecraft.client.model.monster.zombie.BabyDrownedModel;
import net.minecraft.client.model.monster.zombie.DrownedModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.*;
import net.minecraft.client.renderer.entity.state.*;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.spider.Spider;

/** Texture-only adapters: each factory retains its exact vanilla renderer and model layers. */
final class CustomMobRenderers {
    private CustomMobRenderers() {}

    private static Identifier texture(String id) {
        return ModIds.id("textures/entity/mobs/" + id + ".png");
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

    static WitherSkeletonRenderer witherSkeleton(EntityRendererProvider.Context context, String id) {
        Identifier base = texture(id);
        return new WitherSkeletonRenderer(context) {
            @Override
            public Identifier getTextureLocation(SkeletonRenderState state) {
                return base;
            }
        };
    }

    static SilverfishRenderer silverfish(EntityRendererProvider.Context context, String id) {
        Identifier base = texture(id);
        return new SilverfishRenderer(context) {
            @Override
            public Identifier getTextureLocation(LivingEntityRenderState state) {
                return base;
            }
        };
    }

    static EndermiteRenderer endermite(EntityRendererProvider.Context context, String id) {
        Identifier base = texture(id);
        return new EndermiteRenderer(context) {
            @Override
            public Identifier getTextureLocation(LivingEntityRenderState state) {
                return base;
            }
        };
    }

    static MagmaCubeRenderer magmaCube(EntityRendererProvider.Context context, String id) {
        Identifier base = texture(id);
        return new MagmaCubeRenderer(context) {
            @Override
            public Identifier getTextureLocation(SlimeRenderState state) {
                return base;
            }
        };
    }

    static PillagerRenderer pillager(EntityRendererProvider.Context context, String id) {
        Identifier base = texture(id);
        return new PillagerRenderer(context) {
            @Override
            public Identifier getTextureLocation(IllagerRenderState state) {
                return base;
            }
        };
    }

    static VindicatorRenderer vindicator(EntityRendererProvider.Context context, String id) {
        Identifier base = texture(id);
        return new VindicatorRenderer(context) {
            @Override
            public Identifier getTextureLocation(IllagerRenderState state) {
                return base;
            }
        };
    }

    static BatRenderer bat(EntityRendererProvider.Context context, String id) {
        Identifier base = texture(id);
        return new BatRenderer(context) {
            @Override
            public Identifier getTextureLocation(BatRenderState state) {
                return base;
            }
        };
    }

    static CreeperRenderer creeper(EntityRendererProvider.Context context, String id) {
        Identifier base = texture(id);
        Identifier armor = texture(id + "_armor");
        return new CreeperRenderer(context) {
            {
                layers.replaceAll(layer -> layer instanceof CreeperPowerLayer
                        ? new CreeperPowerLayer(this, context.getModelSet()) {
                            @Override
                            protected Identifier getTextureLocation() { return armor; }
                        } : layer);
            }

            @Override
            public Identifier getTextureLocation(CreeperRenderState state) {
                return base;
            }
        };
    }

    static PiglinRenderer piglin(EntityRendererProvider.Context context, String id) {
        Identifier base = texture(id);
        Identifier baby = texture(id + "_baby");
        return new PiglinRenderer(context, ModelLayers.PIGLIN, ModelLayers.PIGLIN_BABY,
                ModelLayers.PIGLIN_ARMOR, ModelLayers.PIGLIN_BABY_ARMOR) {
            @Override
            public Identifier getTextureLocation(PiglinRenderState state) {
                return state.isBaby ? baby : base;
            }
        };
    }

    static StrayRenderer stray(EntityRendererProvider.Context context, String id) {
        Identifier base = texture(id);
        return new StrayRenderer(context) {
            {
                layers.replaceAll(layer -> layer instanceof SkeletonClothingLayer
                        ? new SkeletonClothingLayer<>(this, context.getModelSet(),
                                ModelLayers.STRAY_OUTER_LAYER, texture(id + "_overlay")) : layer);
            }

            @Override
            public Identifier getTextureLocation(SkeletonRenderState state) {
                return base;
            }
        };
    }

    static SpiderRenderer<Spider> spider(EntityRendererProvider.Context context, String id) {
        Identifier base = texture(id);
        RenderType eyes = RenderTypes.eyes(texture(id + "_eyes"));
        return new SpiderRenderer<Spider>(context) {
            {
                layers.replaceAll(layer -> layer instanceof SpiderEyesLayer
                        ? new SpiderEyesLayer<>(this) {
                            @Override
                            public RenderType renderType() { return eyes; }
                        } : layer);
            }

            @Override
            public Identifier getTextureLocation(LivingEntityRenderState state) {
                return base;
            }
        };
    }

    static CaveSpiderRenderer caveSpider(EntityRendererProvider.Context context, String id) {
        Identifier base = texture(id);
        RenderType eyes = RenderTypes.eyes(texture(id + "_eyes"));
        return new CaveSpiderRenderer(context) {
            {
                layers.replaceAll(layer -> layer instanceof SpiderEyesLayer
                        ? new SpiderEyesLayer<>(this) {
                            @Override
                            public RenderType renderType() { return eyes; }
                        } : layer);
            }

            @Override
            public Identifier getTextureLocation(LivingEntityRenderState state) {
                return base;
            }
        };
    }

    static PhantomRenderer phantom(EntityRendererProvider.Context context, String id) {
        Identifier base = texture(id);
        RenderType eyes = RenderTypes.eyes(texture(id + "_eyes"));
        return new PhantomRenderer(context) {
            {
                layers.replaceAll(layer -> layer instanceof PhantomEyesLayer
                        ? new PhantomEyesLayer(this) {
                            @Override
                            public RenderType renderType() { return eyes; }
                        } : layer);
            }

            @Override
            public Identifier getTextureLocation(PhantomRenderState state) {
                return base;
            }
        };
    }

    static BeeRenderer bee(EntityRendererProvider.Context context, String id) {
        // Match vanilla's angry/nectar selection for both independently mapped age textures.
        Identifier[] textures = new Identifier[8];
        for (int i = 0; i < textures.length; i++) {
            textures[i] = texture(id + ((i & 1) != 0 ? "_angry" : "")
                    + ((i & 2) != 0 ? "_nectar" : "") + ((i & 4) != 0 ? "_baby" : ""));
        }
        return new BeeRenderer(context) {
            @Override
            public Identifier getTextureLocation(BeeRenderState state) {
                return textures[(state.isAngry ? 1 : 0) | (state.hasNectar ? 2 : 0) | (state.isBaby ? 4 : 0)];
            }
        };
    }

    static WolfRenderer wolf(EntityRendererProvider.Context context, String id) {
        Identifier[] textures = {texture(id), texture(id + "_tame"), texture(id + "_angry"),
                texture(id + "_baby"), texture(id + "_tame_baby"), texture(id + "_angry_baby")};
        return new WolfRenderer(context) {
            @Override
            public void extractRenderState(Wolf wolf, WolfRenderState state, float partialTick) {
                super.extractRenderState(wolf, state, partialTick);
                // Vanilla prioritizes tame, then anger; leave collar, armor and wet shading intact.
                state.texture = textures[(state.isBaby ? 3 : 0) + (wolf.isTame() ? 1 : wolf.isAngry() ? 2 : 0)];
            }
        };
    }

    static DrownedRenderer drowned(EntityRendererProvider.Context context, String id) {
        Identifier base = texture(id);
        Identifier baby = texture(id + "_baby");
        Identifier outer = texture(id + "_outer_layer");
        Identifier babyOuter = texture(id + "_outer_layer_baby");
        return new DrownedRenderer(context) {
            {
                layers.replaceAll(layer -> layer instanceof DrownedOuterLayer
                        ? new RenderLayer<ZombieRenderState, DrownedModel>(this) {
                            private final DrownedModel adultModel = new DrownedModel(
                                    context.bakeLayer(ModelLayers.DROWNED_OUTER_LAYER));
                            private final DrownedModel babyModel = new BabyDrownedModel(
                                    context.bakeLayer(ModelLayers.DROWNED_BABY_OUTER_LAYER));

                            @Override
                            public void submit(PoseStack pose, SubmitNodeCollector collector, int light,
                                               ZombieRenderState state, float yRot, float xRot) {
                                // Same models, cutout submission, tint and order as 26.3 DrownedOuterLayer.
                                coloredCutoutModelCopyLayerRender(state.isBaby ? babyModel : adultModel,
                                        state.isBaby ? babyOuter : outer, pose, collector, light, state, -1, 1);
                            }
                        } : layer);
            }

            @Override
            public Identifier getTextureLocation(ZombieRenderState state) {
                return state.isBaby ? baby : base;
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
                                // Vanilla hardcodes slime.png here; retain its translucency and outline branch.
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
