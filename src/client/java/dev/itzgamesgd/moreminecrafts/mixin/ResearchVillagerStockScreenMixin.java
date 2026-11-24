package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.client.ProgressionClientApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Displays current/maximum trade stock in MerchantScreen once `v3.research.53` L1+ is owned. */
@Mixin(AbstractContainerScreen.class)
abstract class ResearchVillagerStockScreenMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void moreminecrafts$renderTradeStock(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                                 float delta, CallbackInfo ci) {
        if (!((Object) this instanceof MerchantScreen)) return;
        if (ProgressionClientApi.researchLevel("v3.research.53") <= 0) return;
        @SuppressWarnings("unchecked")
        MerchantMenu menu = ((AbstractContainerScreen<MerchantMenu>) (Object) this).getMenu();
        MerchantOffers offers = menu.getOffers();
        if (offers.isEmpty()) return;

        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) this;
        MerchantScreenAccessor merchantAccessor = (MerchantScreenAccessor) this;
        int shopItem = merchantAccessor.moreminecrafts$getShopItem();
        int scrollOff = merchantAccessor.moreminecrafts$getScrollOff();
        int leftPos = accessor.moreminecrafts$getLeftPos();
        int topPos = accessor.moreminecrafts$getTopPos();

        int hoveredIndex = -1;
        for (int row = 0; row < 7; row++) {
            int offerIndex = scrollOff + row;
            if (offerIndex < 0 || offerIndex >= offers.size()) break;
            MerchantOffer offer = offers.get(offerIndex);
            int maxUses = Math.max(1, offer.getMaxUses());
            int remaining = Math.max(0, maxUses - offer.getUses());

            int bx = leftPos + 5;
            int by = topPos + 16 + row * 20;
            if (mouseX >= bx && mouseX < bx + 88 && mouseY >= by && mouseY < by + 20) {
                hoveredIndex = offerIndex;
            }

            int barX = bx + 2;
            int barY = by + 17;
            int barW = 84;
            int fillW = (int) Math.round(barW * ((double) remaining / maxUses));
            int color = remaining == 0 ? 0xFFC62828 : (remaining * 3 <= maxUses ? 0xFFF9A825 : 0xFF2E7D32);
            graphics.fill(barX, barY, barX + barW, barY + 2, 0xFF333333);
            if (fillW > 0) graphics.fill(barX, barY, barX + fillW, barY + 2, color);
        }

        int displayIndex = hoveredIndex >= 0 ? hoveredIndex : shopItem;
        if (displayIndex >= 0 && displayIndex < offers.size()) {
            MerchantOffer active = offers.get(displayIndex);
            int maxUses = Math.max(0, active.getMaxUses());
            int remaining = Math.max(0, maxUses - active.getUses());
            Font font = Minecraft.getInstance().font;
            int color = remaining == 0 ? 0xFFAA2222 : 0xFF404040;
            graphics.text(font, Component.translatable("moreminecrafts.ui.villager.stock", remaining, maxUses),
                    leftPos + 108, topPos + 60, color);
        }
    }
}
