package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.enhancement.EnhancementOperation;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BiConsumer;

@Mixin(ItemStack.class)
abstract class ItemStackMixin {
    @Inject(method = "forEachModifier", at = @At("RETURN"))
    private void moreminecrafts$applyEnhancementAttackDamage(
            EquipmentSlot slot,
            BiConsumer<Holder<Attribute>, AttributeModifier> consumer,
            CallbackInfo callbackInfo
    ) {
        if (slot != EquipmentSlot.MAINHAND) {
            return;
        }

        double bonus = EnhancementOperation.attackDamageBonus((ItemStack) (Object) this);
        if (bonus > 0.0D) {
            consumer.accept(
                    Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(
                            ModIds.id("enhancement_attack_damage"),
                            bonus,
                            AttributeModifier.Operation.ADD_VALUE
                    )
            );
        }
    }
}
