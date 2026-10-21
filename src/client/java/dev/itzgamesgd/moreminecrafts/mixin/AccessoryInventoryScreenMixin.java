package dev.itzgamesgd.moreminecrafts.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
abstract class AccessoryInventoryScreenMixin {
    private static final int BASE_WIDTH = 176;
    private static final int EXTENDED_WIDTH = 284;

    @Shadow protected int imageWidth;
    @Shadow protected int leftPos;
    @Shadow protected int topPos;

    @Inject(method = "init", at = @At("HEAD"))
    private void moreminecrafts$extendInventoryForAccessoryColumn(CallbackInfo ci) {
        imageWidth = EXTENDED_WIDTH;
    }

    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void moreminecrafts$drawAccessoryColumn(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                                     float delta, CallbackInfo ci) {
        int x = leftPos + BASE_WIDTH;
        int right = leftPos + EXTENDED_WIDTH;
        int bottom = topPos + 166;
        graphics.fill(x, topPos, right, bottom, 0xFF9A9A9A);
        graphics.fill(x + 1, topPos + 1, right - 1, bottom - 1, 0xFFC6C6C6);
        graphics.fill(x, topPos, x + 1, bottom, 0xFF555555);
        graphics.fill(x + 2, topPos + 2, right - 2, topPos + 3, 0xFFE8E8E8);
        graphics.text(net.minecraft.client.Minecraft.getInstance().font,
                Component.translatable("menu.moreminecrafts.accessories"), x + 5, topPos + 5, 0xFF303030, false);

        for (int slot = 0; slot < 5; slot++) {
            int slotX = leftPos + 178;
            int slotY = topPos + 25 + slot * 27;
            graphics.fill(slotX - 1, slotY - 1, slotX + 17, slotY + 17, 0xFF282828);
            graphics.fill(slotX, slotY, slotX + 16, slotY + 16, 0xFF777774);
            graphics.fill(slotX + 1, slotY + 1, slotX + 15, slotY + 15, 0xFF858582);
            drawCategory(graphics, slot, leftPos + 200, slotY + 1);
        }
    }

    private static void drawCategory(GuiGraphicsExtractor graphics, int slot, int x, int y) {
        var font = net.minecraft.client.Minecraft.getInstance().font;
        switch (slot) {
            case 0 -> graphics.text(font, Component.translatable("accessory.category.combat"), x, y + 4, 0xFF303030, false);
            case 1 -> {
                graphics.text(font, Component.translatable("accessory.category.defense"), x, y, 0xFF303030, false);
                graphics.text(font, Component.translatable("accessory.category.survival"), x, y + 9, 0xFF303030, false);
            }
            case 2 -> {
                graphics.text(font, Component.translatable("accessory.category.support"), x, y, 0xFF303030, false);
                graphics.text(font, Component.translatable("accessory.category.recovery"), x, y + 9, 0xFF303030, false);
            }
            case 3 -> {
                graphics.text(font, Component.translatable("accessory.category.exploration"), x, y, 0xFF303030, false);
                graphics.text(font, Component.translatable("accessory.category.mobility"), x, y + 9, 0xFF303030, false);
            }
            case 4 -> {
                graphics.text(font, Component.translatable("accessory.category.lifestyle"), x, y, 0xFF303030, false);
                graphics.text(font, Component.translatable("accessory.category.gathering"), x, y + 9, 0xFF303030, false);
            }
            default -> throw new IndexOutOfBoundsException("Accessory slot " + slot);
        }
    }
}
