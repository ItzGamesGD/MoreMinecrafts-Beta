package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.progression.BrewingOutputProvenance;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

/** Captures only the output mutation made by vanilla's successful brew operation. */
@Mixin(BrewingStandBlockEntity.class)
abstract class KnowledgeBrewingMixin {
    @Inject(method = "setItem", at = @At("HEAD"))
    private void moreminecrafts$discardReplacedOutput(int slot, ItemStack stack, CallbackInfo ci) {
        if (slot >= 0 && slot <= 2)
            BrewingOutputProvenance.discard((BrewingStandBlockEntity) (Object) this, slot);
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void moreminecrafts$saveBrewProvenance(ValueOutput output, CallbackInfo ci) {
        BrewingStandBlockEntity stand = (BrewingStandBlockEntity) (Object) this;
        var pending = BrewingOutputProvenance.save(stand);
        if (!pending.isEmpty()) {
            output.putInt("MoreMinecraftsBrewProvenanceVersion", BrewingOutputProvenance.DATA_VERSION);
            output.store("MoreMinecraftsBrewProvenance", BrewingOutputProvenance.CODEC, pending);
        }
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void moreminecrafts$loadBrewProvenance(ValueInput input, CallbackInfo ci) {
        BrewingStandBlockEntity stand = (BrewingStandBlockEntity) (Object) this;
        if (input.getIntOr("MoreMinecraftsBrewProvenanceVersion", 0) == BrewingOutputProvenance.DATA_VERSION)
            input.read("MoreMinecraftsBrewProvenance", BrewingOutputProvenance.CODEC)
                    .ifPresent(pending -> BrewingOutputProvenance.load(stand, pending));
    }

    @WrapOperation(method = "serverTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/entity/BrewingStandBlockEntity;doBrew(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BrewingStandBlockEntity;)V"))
    private static void moreminecrafts$recordSuccessfulBrew(ServerLevel level, BlockPos pos,
            BrewingStandBlockEntity stand, Operation<Void> original) {
        ItemStack[] before = new ItemStack[3];
        for (int slot = 0; slot < 3; slot++) before[slot] = stand.getItem(slot).copy();
        original.call(level, pos, stand);
        ItemStack[] after = new ItemStack[3];
        for (int slot = 0; slot < 3; slot++) after[slot] = stand.getItem(slot).copy();
        BrewingOutputProvenance.completed(level, pos, stand, before, after);
    }
}
