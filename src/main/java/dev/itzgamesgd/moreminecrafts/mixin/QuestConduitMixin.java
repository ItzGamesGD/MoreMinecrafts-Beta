package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.progression.XpQuestHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ConduitBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Credits nearby server players when a Conduit transitions to active. */
@Mixin(ConduitBlockEntity.class)
abstract class QuestConduitMixin {
    @Unique
    private static final ThreadLocal<Boolean> MOREMINECRAFTS$WAS_ACTIVE = ThreadLocal.withInitial(() -> false);

    @Inject(method = "serverTick", at = @At("HEAD"))
    private static void moreminecrafts$captureConduitBeforeTick(Level level, BlockPos pos, BlockState state,
            ConduitBlockEntity blockEntity, CallbackInfo ci) {
        MOREMINECRAFTS$WAS_ACTIVE.set(blockEntity.isActive());
    }

    @Inject(method = "serverTick", at = @At("RETURN"))
    private static void moreminecrafts$creditConduitActivation(Level level, BlockPos pos, BlockState state,
            ConduitBlockEntity blockEntity, CallbackInfo ci) {
        boolean wasActive = MOREMINECRAFTS$WAS_ACTIVE.get();
        MOREMINECRAFTS$WAS_ACTIVE.remove();
        if (!(level instanceof ServerLevel serverLevel) || wasActive || !blockEntity.isActive()) return;
        double cx = pos.getX() + 0.5D;
        double cy = pos.getY() + 0.5D;
        double cz = pos.getZ() + 0.5D;
        double maxDistSq = 96.0D * 96.0D;
        for (ServerPlayer player : serverLevel.players()) {
            if (player.distanceToSqr(cx, cy, cz) <= maxDistSq) {
                XpQuestHooks.conduitActivated(player);
            }
        }
    }
}
