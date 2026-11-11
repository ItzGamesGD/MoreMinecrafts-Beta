package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.pyramid.RoyalChestRuntime;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerExplosion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Registered royal chests are never part of an explosion's destroyed block set (D13-03-I06 Q38). */
@Mixin(ServerExplosion.class)
abstract class RoyalChestExplosionMixin {
    @Shadow @Final private ServerLevel level;

    @Inject(method = "calculateExplodedPositions", at = @At("RETURN"), cancellable = true)
    private void moreminecrafts$sparRoyalChests(CallbackInfoReturnable<List<BlockPos>> cir) {
        List<BlockPos> positions = cir.getReturnValue();
        if (positions == null || positions.isEmpty()) return;
        List<BlockPos> kept = new ArrayList<>(positions.size());
        for (BlockPos pos : positions) if (!RoyalChestRuntime.isGuarded(level, pos)) kept.add(pos);
        if (kept.size() != positions.size()) cir.setReturnValue(kept);
    }
}
