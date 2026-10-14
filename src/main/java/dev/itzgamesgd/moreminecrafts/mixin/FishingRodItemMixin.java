package dev.itzgamesgd.moreminecrafts.mixin;
import dev.itzgamesgd.moreminecrafts.fishing.FishingChallenges;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(FishingRodItem.class)
public abstract class FishingRodItemMixin {
 @Inject(method="use",at=@At("HEAD"),cancellable=true)
 private void moreminecrafts$specialUse(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir){
  if(player instanceof ServerPlayer server){
   if(FishingChallenges.active(server)){FishingChallenges.confirm(server);cir.setReturnValue(InteractionResult.SUCCESS);return;}
  }
 }
}
