package dev.itzgamesgd.moreminecrafts.client.st01;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.st01.GoldGuideGolem;
import net.minecraft.client.model.animal.golem.IronGolemModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.IronGolemRenderState;
import net.minecraft.resources.Identifier;

/**
 * Reuses the vanilla iron golem model, layer and walk animation with a gold texture. Render size, hitbox and path size all follow the entity's
 * SCALE attribute (single source of truth), so a small golem is small everywhere. Final look/size are unapproved (O-6, O-12).
 */
public final class GoldGuideGolemRenderer extends MobRenderer<GoldGuideGolem, IronGolemRenderState, IronGolemModel> {
    private static final Identifier TEXTURE = ModIds.id("textures/entity/gold_guide_golem/gold_guide_golem.png");

    public GoldGuideGolemRenderer(EntityRendererProvider.Context context) {
        super(context, new IronGolemModel(context.bakeLayer(ModelLayers.IRON_GOLEM)), 0.7F);
    }

    @Override public Identifier getTextureLocation(IronGolemRenderState state) { return TEXTURE; }
    @Override public IronGolemRenderState createRenderState() { return new IronGolemRenderState(); }
}
