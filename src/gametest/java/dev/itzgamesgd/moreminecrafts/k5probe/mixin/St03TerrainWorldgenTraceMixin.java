package dev.itzgamesgd.moreminecrafts.k5probe.mixin;

import dev.itzgamesgd.moreminecrafts.pillagerhq.St03TerrainWriteTrace;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldGenRegion.class)
abstract class St03TerrainWorldgenTraceMixin {
    @Inject(method="setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",at=@At("HEAD"))
    private void moreminecrafts$traceSt03Foundation(BlockPos pos,BlockState state,int flags,int limit,CallbackInfoReturnable<Boolean> cir) {
        St03TerrainWriteTrace.observe("WorldGenRegion",pos,state);
    }
}
