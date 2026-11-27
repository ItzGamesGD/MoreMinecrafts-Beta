package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.progression.T1AnvilModes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Routes catalyst shift-clicks only for the anvil menu that owns the additional slot. */
@Mixin(ItemCombinerMenu.class)
abstract class AnvilCatalystQuickMoveMixin {
    @Inject(method="quickMoveStack",at=@At("HEAD"),cancellable=true)
    private void moreminecrafts$quickMoveCatalyst(Player player,int slotIndex,CallbackInfoReturnable<ItemStack> cir){
        if((Object)this instanceof T1AnvilModes modes){
            ItemStack result=modes.moreminecrafts$quickMoveCatalyst(player,slotIndex);
            if(result!=null)cir.setReturnValue(result);
        }
    }
}
