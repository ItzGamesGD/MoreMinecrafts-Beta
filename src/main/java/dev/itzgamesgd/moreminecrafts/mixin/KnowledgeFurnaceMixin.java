package dev.itzgamesgd.moreminecrafts.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.itzgamesgd.moreminecrafts.progression.SmeltingOutputProvenance;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Records the exact input/output pair after vanilla has accepted a completed cooking result. */
@Mixin(AbstractFurnaceBlockEntity.class)
abstract class KnowledgeFurnaceMixin {
    @WrapOperation(method = "serverTick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;burn(Lnet/minecraft/core/NonNullList;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)V"))
    private static void moreminecrafts$rememberCompletedOutput(NonNullList<ItemStack> items, ItemStack input,
            ItemStack result, Operation<Void> original,
            @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) AbstractFurnaceBlockEntity furnace) {
        ItemStack consumedInput = input.copy();
        ItemStack producedOutput = result.copy();
        original.call(items, input, result);
        SmeltingOutputProvenance.record(furnace, consumedInput, producedOutput);
    }
}
