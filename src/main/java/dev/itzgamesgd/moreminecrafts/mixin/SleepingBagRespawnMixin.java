package dev.itzgamesgd.moreminecrafts.mixin;

import com.mojang.datafixers.util.Either;
import dev.itzgamesgd.moreminecrafts.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.AbstractBedBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
abstract class SleepingBagRespawnMixin {
    @Unique private ServerPlayer.RespawnConfig moreminecrafts$previousRespawn;
    @Unique private boolean moreminecrafts$sleepingBag;

    @Inject(method = "startSleepInBed", at = @At("HEAD"))
    private void moreminecrafts$captureRespawn(AbstractBedBlock bed, BlockState state, BedRule rule, BlockPos pos,
            CallbackInfoReturnable<Either<Player.BedSleepingProblem, Unit>> cir) {
        moreminecrafts$sleepingBag = state.is(ModBlocks.EXPLORATION_SLEEPING_BAG);
        if (moreminecrafts$sleepingBag) moreminecrafts$previousRespawn = ((ServerPlayer)(Object)this).getRespawnConfig();
    }

    @Inject(method = "startSleepInBed", at = @At("RETURN"))
    private void moreminecrafts$restoreRespawn(AbstractBedBlock bed, BlockState state, BedRule rule, BlockPos pos,
            CallbackInfoReturnable<Either<Player.BedSleepingProblem, Unit>> cir) {
        if (moreminecrafts$sleepingBag) ((ServerPlayer)(Object)this).setRespawnPosition(moreminecrafts$previousRespawn, false);
        moreminecrafts$sleepingBag = false;
    }
}
