package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.enchantment.EnchantmentRuntime;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.BlockTags;
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
        List<ItemStack> drops = cir.getReturnValue().stream()
                .map(stack -> EnchantmentRuntime.consumeReplantDrop(level, pos, entity,
                        EnchantmentRuntime.autoSmeltDrop(level, pos, tool, stack)))
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        ServerPlayer player = entity instanceof ServerPlayer serverPlayer
                ? serverPlayer : dev.itzgamesgd.moreminecrafts.progression.ResearchMiningContext.currentPlayer();
        if (player != null && player.gameMode.getGameModeForPlayer() == net.minecraft.world.level.GameType.SURVIVAL
                && state.is(BlockTags.LOGS)) {
            int paidLevel = dev.itzgamesgd.moreminecrafts.progression.ResearchRewards.level(
                    player, "v3.research.02", "lumbering_extra_logs");
            int cap = dev.itzgamesgd.moreminecrafts.progression.Research23AEffectRules.maximumExtraLogs(paidLevel);
            if (cap > 0) {
                int extra = dev.itzgamesgd.moreminecrafts.progression.Research23AEffectRules.rollExtraLogs(
                        paidLevel, player.getRandom().nextFloat(), player.getRandom().nextInt(cap));
                if (extra > 0 && !state.getBlock().asItem().getDefaultInstance().isEmpty()) {
                    ItemStack bonus = EnchantmentRuntime.autoSmeltDrop(level, pos, tool,
                            new ItemStack(state.getBlock().asItem(), extra));
                    if (!bonus.isEmpty()) drops.add(bonus);
                }
            }
        }
        cir.setReturnValue(drops);
    }
}
