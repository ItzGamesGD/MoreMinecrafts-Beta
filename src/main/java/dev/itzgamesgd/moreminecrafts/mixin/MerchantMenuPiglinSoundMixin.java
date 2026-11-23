package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.trade.PiglinMerchant;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.Merchant;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vanilla casts the Merchant to Entity when shift-click trading; PiglinMerchant is an adapter, not an Entity. */
@Mixin(MerchantMenu.class)
abstract class MerchantMenuPiglinSoundMixin {
    @Shadow @Final private Merchant trader;

    @Inject(method = "playTradeSound", at = @At("HEAD"), cancellable = true)
    private void moreminecrafts$piglinTradeSound(CallbackInfo ci) {
        if (trader instanceof PiglinMerchant merchant) {
            if (!merchant.isClientSide()) {
                var piglin = merchant.piglin();
                piglin.level().playSound(null, piglin.getX(), piglin.getY(), piglin.getZ(),
                        merchant.getNotifyTradeSound(), SoundSource.NEUTRAL, 1.0F, 1.0F);
            }
            ci.cancel();
        }
    }
}
