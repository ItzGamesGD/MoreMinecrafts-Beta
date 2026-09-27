package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.enhancement.EnhancementOperation;
import dev.itzgamesgd.moreminecrafts.enhancement.EnhancementEffect;
import dev.itzgamesgd.moreminecrafts.enhancement.EnhancementEffects;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.BiConsumer;

@Mixin(ItemStack.class)
abstract class ItemStackMixin {
    @Inject(
            method = "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlot;Ljava/util/function/BiConsumer;)V",
            at = @At("RETURN")
    )
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

    @Inject(
            method = "getDestroySpeed(Lnet/minecraft/world/level/block/state/BlockState;)F",
            at = @At("RETURN"),
            cancellable = true
    )
    private void moreminecrafts$applyEnhancementToolEfficiency(
            BlockState state,
            CallbackInfoReturnable<Float> callbackInfo
    ) {
        float vanillaSpeed = callbackInfo.getReturnValue();
        // A vanilla speed of one is the fallback for blocks this stack is not effective against.
        // Only extend the tool's native effective-block result; do not make every block mineable
        // as though it were a preferred tool target.
        if (vanillaSpeed <= 1.0F) {
            return;
        }

        double bonus = EnhancementEffects.value(
                (ItemStack) (Object) this,
                EnhancementEffect.Target.TOOL_EFFICIENCY
        );
        if (bonus > 0.0D) {
            callbackInfo.setReturnValue(vanillaSpeed + (float) bonus);
        }
    }
}
