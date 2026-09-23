package dev.itzgamesgd.moreminecrafts.mixin;
import dev.itzgamesgd.moreminecrafts.fishing.FishingChallenges;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(FishingHook.class)
public abstract class FishingHookMixin {
 @Shadow private boolean biting;
 @Inject(method="retrieve",at=@At("HEAD"),cancellable=true)
 private void moreminecrafts$startTiming(ItemStack rod, CallbackInfoReturnable<Integer> cir){FishingHook self=(FishingHook)(Object)this;if(biting&&self.getOwner() instanceof ServerPlayer player){FishingChallenges.start(player,self,rod);cir.setReturnValue(1);}}
}
