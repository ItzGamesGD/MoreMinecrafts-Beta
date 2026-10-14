package dev.itzgamesgd.moreminecrafts.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.itzgamesgd.moreminecrafts.progression.SmeltingOutputProvenance;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Records the exact input/output pair after vanilla has accepted a completed cooking result. */
@Mixin(AbstractFurnaceBlockEntity.class)
abstract class KnowledgeFurnaceMixin {
    @Inject(method = "setItem", at = @At("HEAD"))
    private void moreminecrafts$discardReplacedOutput(int slot, ItemStack stack, CallbackInfo ci) {
        if (slot == 2)
            SmeltingOutputProvenance.discard((AbstractFurnaceBlockEntity) (Object) this);
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void moreminecrafts$saveSmeltProvenance(ValueOutput output, CallbackInfo ci) {
        AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity) (Object) this;
        var pending = SmeltingOutputProvenance.save(furnace);
        if (!pending.isEmpty()) {
            output.putInt("MoreMinecraftsSmeltProvenanceVersion", SmeltingOutputProvenance.DATA_VERSION);
            output.store("MoreMinecraftsSmeltProvenance", SmeltingOutputProvenance.CODEC, pending);
        }
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void moreminecrafts$loadSmeltProvenance(ValueInput input, CallbackInfo ci) {
        AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity) (Object) this;
        if (input.getIntOr("MoreMinecraftsSmeltProvenanceVersion", 0) == SmeltingOutputProvenance.DATA_VERSION)
            input.read("MoreMinecraftsSmeltProvenance", SmeltingOutputProvenance.CODEC)
                    .ifPresent(pending -> SmeltingOutputProvenance.load(furnace, pending));
    }

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
