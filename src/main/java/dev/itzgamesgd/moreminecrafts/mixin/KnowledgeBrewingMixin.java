package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.progression.BrewingOutputProvenance;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.injection.At;

/** Captures only the output mutation made by vanilla's successful brew operation. */
@Mixin(BrewingStandBlockEntity.class)
abstract class KnowledgeBrewingMixin {
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
