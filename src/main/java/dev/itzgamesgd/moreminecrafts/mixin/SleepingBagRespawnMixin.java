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
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
abstract class SleepingBagRespawnMixin {
    @Unique private boolean moreminecrafts$sleepingBag;

    @Inject(method = "startSleepInBed", at = @At("HEAD"))
    private void moreminecrafts$identifySleepingBag(AbstractBedBlock bed, BlockState state, BedRule rule, BlockPos pos,
            CallbackInfoReturnable<Either<Player.BedSleepingProblem, Unit>> cir) {
        moreminecrafts$sleepingBag = state.is(ModBlocks.EXPLORATION_SLEEPING_BAG);
    }

    @Redirect(method = "startSleepInBed", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;setRespawnPosition(Lnet/minecraft/server/level/ServerPlayer$RespawnConfig;Z)V"))
    private void moreminecrafts$preventSleepingBagRespawn(ServerPlayer player, ServerPlayer.RespawnConfig config,
                                                           boolean forced) {
        if (!moreminecrafts$sleepingBag) player.setRespawnPosition(config, forced);
    }

    @Inject(method = "startSleepInBed", at = @At("RETURN"))
    private void moreminecrafts$clearSleepingBagFlag(AbstractBedBlock bed, BlockState state, BedRule rule, BlockPos pos,
            CallbackInfoReturnable<Either<Player.BedSleepingProblem, Unit>> cir) {
        moreminecrafts$sleepingBag = false;
    }
}
