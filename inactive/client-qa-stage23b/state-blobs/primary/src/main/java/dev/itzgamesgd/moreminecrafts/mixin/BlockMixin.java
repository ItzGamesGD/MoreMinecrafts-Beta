package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.enchantment.EnchantmentRuntime;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Restricts Auto Smelt to vanilla loot from the exact mined block, after Fortune has run. */
@Mixin(Block.class)
abstract class BlockMixin {
    @Inject(method = "getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemInstance;)Ljava/util/List;",
            at = @At("RETURN"), cancellable = true)
    private static void moreminecrafts$smeltMinedBlockDrops(BlockState state, ServerLevel level, BlockPos pos,
            BlockEntity blockEntity, Entity entity, ItemInstance tool, CallbackInfoReturnable<List<ItemStack>> cir) {
        List<ItemStack> drops = cir.getReturnValue();
        if (entity instanceof ServerPlayer player) {
            drops = dev.itzgamesgd.moreminecrafts.progression.ResearchContentRuntime
                    .applySeedHarvestResearch(player, state, drops);
            // Lumbering extra logs enter the one enchantment pass alongside vanilla drops.
            drops = dev.itzgamesgd.moreminecrafts.progression.ResearchContentRuntime
                    .applyLumberingResearch(player, state, drops);
        }
        drops = drops.stream()
                .map(stack -> EnchantmentRuntime.consumeReplantDrop(level, pos, entity,
                        EnchantmentRuntime.autoSmeltDrop(level, pos, tool, stack))).toList();
        if (entity instanceof ServerPlayer player) {
            // Ore Mining must continue to copy the *processed* Fortune/Silk/Auto Smelt result.
            drops = dev.itzgamesgd.moreminecrafts.progression.ResearchContentRuntime
                    .applyOreResearch(player, state, drops);
        }
        cir.setReturnValue(drops);
    }
}
