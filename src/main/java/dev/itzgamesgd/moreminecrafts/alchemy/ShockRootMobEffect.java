package dev.itzgamesgd.moreminecrafts.alchemy;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

final class ShockRootMobEffect extends MobEffect {
    ShockRootMobEffect() {
        super(MobEffectCategory.HARMFUL, ShockMobEffect.SKY_BLUE);
        addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                ModIds.id("shock_root"),
                -1.0,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
