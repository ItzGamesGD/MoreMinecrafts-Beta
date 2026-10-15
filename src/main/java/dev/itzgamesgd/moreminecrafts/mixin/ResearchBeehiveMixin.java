package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.progression.ResearchContentRules;
import dev.itzgamesgd.moreminecrafts.progression.ResearchRewards;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adds at most one bonus comb after a successful vanilla honeycomb harvest. */
@Mixin(BeehiveBlock.class)
abstract class ResearchBeehiveMixin {
    @Inject(method = "useItemOn", at = @At("RETURN"))
    private void moreminecrafts$bonusHoneycomb(ItemStack held, BlockState before, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit,
            CallbackInfoReturnable<InteractionResult> result) {
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)
                || !held.is(Items.SHEARS) || !result.getReturnValue().consumesAction()
                || before.getValue(BeehiveBlock.HONEY_LEVEL) < 5
                || serverLevel.getBlockState(pos).getValue(BeehiveBlock.HONEY_LEVEL) != 0) return;
        int researchLevel = ResearchRewards.level(serverPlayer,
                "v3.research.18", "beekeeping_extra_honeycomb");
        double chance = ResearchContentRules.honeycombChance(researchLevel);
        if (ResearchContentRules.extraItemSucceeds(serverPlayer.getRandom().nextDouble(), chance)) {
            serverLevel.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(serverLevel,
                    pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    new ItemStack(Items.HONEYCOMB)));
        }
    }
}
