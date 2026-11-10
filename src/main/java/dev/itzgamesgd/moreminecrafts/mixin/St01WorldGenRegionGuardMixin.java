package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01WorldgenGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** ST-01 P8-C2-B: foreign world-generation writes cannot replace structure-owned pyramid blocks (see St01WorldgenGuard). */
@Mixin(WorldGenRegion.class)
abstract class St01WorldGenRegionGuardMixin {
    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At("HEAD"), cancellable = true)
    private void moreminecrafts$guardSt01Pyramid(BlockPos pos, BlockState state, int updateFlags, int updateLimit,
            CallbackInfoReturnable<Boolean> cir) {
        if (St01WorldgenGuard.refuses((WorldGenRegion) (Object) this, pos)) cir.setReturnValue(false);
    }
}
