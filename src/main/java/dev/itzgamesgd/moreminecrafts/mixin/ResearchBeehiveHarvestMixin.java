package dev.itzgamesgd.moreminecrafts.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.itzgamesgd.moreminecrafts.progression.Research23AEffectRules;
import dev.itzgamesgd.moreminecrafts.progression.ResearchRewards;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Adds one separate honeycomb drop to the single vanilla honeycomb harvest activation. */
@Mixin(BeehiveBlock.class)
abstract class ResearchBeehiveHarvestMixin {
    @WrapOperation(method = "useItemOn",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/BeehiveBlock;dropHoneycomb(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"))
    private void moreminecrafts$bonusHoneycomb(Level level, BlockPos pos,
            Operation<Void> original, @Local(argsOnly = true) Player player) {
        original.call(level, pos);
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        int paidLevel = ResearchRewards.level(serverPlayer, "v3.research.18", "beekeeping_honeycomb");
        if (Research23AEffectRules.shouldGrantBonusHoneycomb(paidLevel, serverPlayer.getRandom().nextFloat()))
            net.minecraft.world.level.block.Block.popResource(level, pos, new ItemStack(Items.HONEYCOMB));
    }
}
