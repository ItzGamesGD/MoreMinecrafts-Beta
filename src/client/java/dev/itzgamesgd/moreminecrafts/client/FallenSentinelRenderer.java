package dev.itzgamesgd.moreminecrafts.client;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.mob.sky.FallenSentinel;
import dev.itzgamesgd.moreminecrafts.mob.sky.SkyNormalMobs;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.zombie.BabyZombieModel;
import net.minecraft.client.model.monster.zombie.ZombieModel;
import net.minecraft.client.renderer.entity.AbstractZombieRenderer;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Fallen Sentinel: the Blockbench model (scripts/bbmodel-capture/convert.py) on the vanilla husk/zombie renderer path,
 * so idle, walking, head tracking, the raised arm pose, attack swing, hurt tint and death roll are inherited.
 *
 * <p>The Blood Seraph Greatsword is drawn through the vanilla held-item path: a render-only iron sword stack whose
 * item model is the greatsword, so hand placement and arm animation are exactly vanilla's. The entity's equipment and
 * damage are unchanged.
 */
final class FallenSentinelRenderer extends AbstractZombieRenderer<FallenSentinel, ZombieRenderState, ZombieModel<ZombieRenderState>> {
    static final Identifier TEXTURE = ModIds.id("textures/entity/bb_fallen_sentinel.png");
    /** Created lazily: item components are not bound yet while renderers register. */
    private ItemStack heldSword;

    FallenSentinelRenderer(EntityRendererProvider.Context context) {
        super(context, new SentinelModel(), new SentinelModel(),
                ArmorModelSet.bake(ModelLayers.HUSK_ARMOR, context.getModelSet(), ZombieModel::new),
                ArmorModelSet.bake(ModelLayers.HUSK_BABY_ARMOR, context.getModelSet(), BabyZombieModel::new));
    }

    static void register() {
        EntityRendererRegistry.register(SkyNormalMobs.FALLEN_SENTINEL, FallenSentinelRenderer::new);
    }

    @Override public Identifier getTextureLocation(ZombieRenderState state) { return TEXTURE; }

    @Override public ZombieRenderState createRenderState() { return new ZombieRenderState(); }

    @Override
    public void extractRenderState(FallenSentinel entity, ZombieRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        if (state.rightHandItemState.isEmpty()) {
            if (heldSword == null) {
                heldSword = new ItemStack(Items.IRON_SWORD);
                heldSword.set(DataComponents.ITEM_MODEL, ModIds.id("fallen_sentinel_weapon"));
            }
            state.rightHandItemStack = heldSword;
            itemModelResolver.updateForLiving(state.rightHandItemState, heldSword,
                    ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, entity);
        }
    }


    static final class SentinelModel extends ZombieModel<ZombieRenderState> {
        SentinelModel() {
            super(BbModelParts.build("fallen_sentinel", true));
        }
    }
}
