package dev.itzgamesgd.moreminecrafts.mixin;
import dev.itzgamesgd.moreminecrafts.progression.T1Supply;
import net.minecraft.world.entity.npc.villager.*;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(AbstractVillager.class)
abstract class T1VillagerMixin {
    @Inject(method="getOffers",at=@At("RETURN"))
    private void moreminecrafts$normalize(CallbackInfoReturnable<MerchantOffers> cir) {
        if((Object)this instanceof Villager villager)T1Supply.normalizeTrades(villager,cir.getReturnValue());
    }
}
