package dev.itzgamesgd.moreminecrafts.mixin;
import dev.itzgamesgd.moreminecrafts.progression.T1AnvilModes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(AbstractContainerMenu.class)
abstract class AnvilButtonsMixin {
    @Inject(method="removed",at=@At("TAIL"))
    private void moreminecrafts$returnCatalyst(Player player,CallbackInfo ci){
        if((Object)this instanceof T1AnvilModes modes)modes.moreminecrafts$returnCatalyst(player);
    }

    @Inject(method="clickMenuButton",at=@At("HEAD"),cancellable=true)
    private void moreminecrafts$modeButton(Player player,int action,CallbackInfoReturnable<Boolean> cir){
        if((Object)this instanceof T1AnvilModes modes)cir.setReturnValue(modes.moreminecrafts$button(player,action));
    }
}
