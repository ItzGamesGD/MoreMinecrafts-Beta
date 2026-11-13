package dev.itzgamesgd.moreminecrafts.st01d7.mixin;

import dev.itzgamesgd.moreminecrafts.st01d7.St01D7Placeholders;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.storage.loot.LootContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** ST-01 P8-D7 experiment: an ST-01 map trade generated while preparation is not ready yields a placeholder in its slot. */
@Mixin(VillagerTrade.class)
abstract class St01D7VillagerTradeMixin {
    @Inject(method = "getOffer", at = @At("HEAD"), cancellable = true)
    private void moreminecrafts$st01d7Placeholder(LootContext context, CallbackInfoReturnable<MerchantOffer> cir) {
        MerchantOffer placeholder = St01D7Placeholders.intercept((VillagerTrade) (Object) this, context);
        if (placeholder != null) cir.setReturnValue(placeholder);
    }
}
