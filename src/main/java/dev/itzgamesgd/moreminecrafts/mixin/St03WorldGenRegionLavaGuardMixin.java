package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.pillagerhq.worldgen.St03ExteriorFeatureGuard;
import dev.itzgamesgd.moreminecrafts.pillagerhq.worldgen.St03SupportLavaGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ST-03 world-generation guards on region writes: non-solid blocks in the support band become cobblestone
 * (St03SupportLavaGuard), and feature writes inside the headquarters above the foundation are dropped
 * (St03ExteriorFeatureGuard).
 */
@Mixin(WorldGenRegion.class)
abstract class St03WorldGenRegionLavaGuardMixin {
    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At("HEAD"), cancellable = true)
    private void moreminecrafts$replaceSt03SupportLava(BlockPos pos, BlockState state, int updateFlags, int updateLimit,
            CallbackInfoReturnable<Boolean> cir) {
        WorldGenRegion region = (WorldGenRegion) (Object) this;
        if (St03ExteriorFeatureGuard.blocksExteriorWrite(region, pos, state)) {
            cir.setReturnValue(false);
            return;
        }
        if (St03SupportLavaGuard.replacesNonSolidSupport(region, pos, state)) {
            // Re-enters this hook with cobblestone, which is not lava, so the write goes through normally.
            cir.setReturnValue(region.setBlock(pos, Blocks.COBBLESTONE.defaultBlockState(), updateFlags, updateLimit));
        }
    }
}
