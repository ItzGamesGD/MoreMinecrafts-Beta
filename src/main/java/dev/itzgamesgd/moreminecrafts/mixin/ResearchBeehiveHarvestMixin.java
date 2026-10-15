package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.progression.Research23AEffectRules;
import dev.itzgamesgd.moreminecrafts.progression.ResearchRewards;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adds one separate honeycomb drop after one successful vanilla shears harvest. */
@Mixin(BeehiveBlock.class)
abstract class ResearchBeehiveHarvestMixin {
    @Inject(method = "useItemOn", at = @At("RETURN"))
    private void moreminecrafts$bonusHoneycomb(ItemStack usedStack, BlockState initialState, Level level,
            BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit,
            CallbackInfoReturnable<?> callbackInfo) {
        if (!(player instanceof ServerPlayer serverPlayer) || !usedStack.is(Items.SHEARS)
                || initialState.getValue(BeehiveBlock.HONEY_LEVEL) < 5) return;
        BlockState after = level.getBlockState(pos);
        if (!after.is(initialState.getBlock()) || after.getValue(BeehiveBlock.HONEY_LEVEL) != 0) return;
        int paidLevel = ResearchRewards.level(serverPlayer, "v3.research.18", "beekeeping_honeycomb");
        if (Research23AEffectRules.shouldGrantBonusHoneycomb(paidLevel, serverPlayer.getRandom().nextFloat()))
            Block.popResource(level, pos, new ItemStack(Items.HONEYCOMB));
    }
}
