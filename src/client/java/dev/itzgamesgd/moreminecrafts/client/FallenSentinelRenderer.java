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
import net.minecraft.world.entity.HumanoidArm;

/**
 * Fallen Sentinel: the Blockbench model (scripts/bbmodel-capture/convert.py) on the vanilla husk/zombie renderer path,
 * so idle, walking, head tracking, the raised arm pose, attack swing, hurt tint and death roll are inherited.
 *
 * <p>The Blood Seraph Greatsword is drawn in the right hand by {@link HeldWeaponLayer} (render only: the entity's
 * equipment and damage are unchanged).
 */
final class FallenSentinelRenderer extends AbstractZombieRenderer<FallenSentinel, ZombieRenderState, ZombieModel<ZombieRenderState>> {
    static final Identifier TEXTURE = ModIds.id("textures/entity/bb_fallen_sentinel.png");
    static final Identifier WEAPON_TEXTURE = ModIds.id("textures/entity/bb_fallen_sentinel_weapon.png");

    FallenSentinelRenderer(EntityRendererProvider.Context context) {
        super(context, new SentinelModel(), new SentinelModel(),
                ArmorModelSet.bake(ModelLayers.HUSK_ARMOR, context.getModelSet(), ZombieModel::new),
                ArmorModelSet.bake(ModelLayers.HUSK_BABY_ARMOR, context.getModelSet(), BabyZombieModel::new));
        addLayer(new HeldWeaponLayer<>(this,
                BbModelParts.buildWeapon("fallen_sentinel_weapon", "fallen_sentinel", "right_arm"), WEAPON_TEXTURE, HumanoidArm.RIGHT));
    }

    static void register() {
        EntityRendererRegistry.register(SkyNormalMobs.FALLEN_SENTINEL, FallenSentinelRenderer::new);
    }

    @Override public Identifier getTextureLocation(ZombieRenderState state) { return TEXTURE; }

    @Override public ZombieRenderState createRenderState() { return new ZombieRenderState(); }


    static final class SentinelModel extends ZombieModel<ZombieRenderState> {
        SentinelModel() {
            super(BbModelParts.build("fallen_sentinel", true));
        }
    }
}
