package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.bounty.ContractTrades;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds the approved contract stock after vanilla has produced its offers. The hook is idempotent and server-only
 * ({@code getOffers} throws on the client), and it also tops up villagers saved before contracts existed.
 */
@Mixin(AbstractVillager.class)
abstract class ContractTradeStockMixin {
    @Inject(method = "getOffers()Lnet/minecraft/world/item/trading/MerchantOffers;", at = @At("RETURN"))
    private void moreminecrafts$contractStock(CallbackInfoReturnable<MerchantOffers> cir) {
        AbstractVillager self = (AbstractVillager) (Object) this;
        MerchantOffers offers = cir.getReturnValue();
        if (offers == null) return;
        if (self instanceof Villager villager) {
            var profession = villager.getVillagerData().profession().unwrapKey();
            if (profession.isPresent())
                ContractTrades.ensureVillager(profession.get().identifier().toString(),
                        villager.getVillagerData().level(), offers);
        } else if (self instanceof WanderingTrader) {
            ContractTrades.ensureWandering(offers, self.getRandom());
        }
    }
}
