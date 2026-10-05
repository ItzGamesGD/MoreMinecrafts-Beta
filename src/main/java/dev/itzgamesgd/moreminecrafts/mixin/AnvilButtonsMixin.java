package dev.itzgamesgd.moreminecrafts.mixin;
import dev.itzgamesgd.moreminecrafts.progression.T1AnvilModes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(AbstractContainerMenu.class)
abstract class AnvilButtonsMixin {
    @Inject(method="clickMenuButton",at=@At("HEAD"),cancellable=true)
    private void moreminecrafts$modeButton(Player player,int action,CallbackInfoReturnable<Boolean> cir){
        if((Object)this instanceof T1AnvilModes modes)cir.setReturnValue(modes.moreminecrafts$button(player,action));
    }
}
