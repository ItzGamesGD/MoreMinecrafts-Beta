package dev.itzgamesgd.moreminecrafts.mixin;
import dev.itzgamesgd.moreminecrafts.progression.T1Supply;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(MerchantContainer.class)
abstract class T1SmithUpgradeMixin {
    @Shadow @Final private Merchant merchant;
    @Inject(method="updateSellItem",at=@At("RETURN"))
    private void moreminecrafts$preserveEquipment(CallbackInfo ci){
        MerchantContainer container=(MerchantContainer)(Object)this;
        if(!(merchant instanceof Villager villager)||!T1Supply.isSmith(villager))return;
        var offer=container.getActiveOffer();ItemStack output=container.getItem(2);
        if(offer==null||output.isEmpty()||!offer.getBaseCostA().is(output.getItem()))return;
        ItemStack input=container.getItem(0).is(output.getItem())?container.getItem(0):container.getItem(1);
        if(input.is(output.getItem())&&!input.isDamaged()){
            ItemStack upgraded=T1Supply.smithUpgrade(input,output).orElse(ItemStack.EMPTY);
            container.setItem(2,upgraded);
        }
    }
}
