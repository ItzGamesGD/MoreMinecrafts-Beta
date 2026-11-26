package dev.itzgamesgd.moreminecrafts.mixin;

import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MerchantScreen.class)
public interface MerchantScreenAccessor {
    @Accessor("shopItem")
    int moreminecrafts$getShopItem();

    @Accessor("scrollOff")
    int moreminecrafts$getScrollOff();
}
