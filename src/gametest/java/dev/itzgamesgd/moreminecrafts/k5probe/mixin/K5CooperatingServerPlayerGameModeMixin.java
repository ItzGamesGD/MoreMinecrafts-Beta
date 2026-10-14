package dev.itzgamesgd.moreminecrafts.k5probe.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.itzgamesgd.moreminecrafts.k5probe.K5CooperatingMixinProbe;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;

/** Independent GameTest-only wrapper that audits composition with production break handling. */
@Mixin(ServerPlayerGameMode.class)
abstract class K5CooperatingServerPlayerGameModeMixin {
    @WrapMethod(method = "destroyBlock(Lnet/minecraft/core/BlockPos;)Z")
    private boolean moreminecrafts$observeBreak(BlockPos pos, Operation<Boolean> original) {
        K5CooperatingMixinProbe.blockBreakWrapped();
        return original.call(pos);
    }
}
