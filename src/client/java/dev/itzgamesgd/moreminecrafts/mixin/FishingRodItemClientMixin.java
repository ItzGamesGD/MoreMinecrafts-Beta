package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.client.MoreMinecraftsClient;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FishingRodItem.class, priority = 1100)
public abstract class FishingRodItemClientMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void moreminecrafts$keepLocalHookDuringChallenge(
            Level level, Player player, InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> result) {
        if (level.isClientSide() && MoreMinecraftsClient.fishingChallengeActive())
            result.setReturnValue(InteractionResult.SUCCESS);
    }
}
