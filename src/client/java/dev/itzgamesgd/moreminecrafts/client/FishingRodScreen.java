package dev.itzgamesgd.moreminecrafts.client;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.fishing.FishingEquipmentItem;
import dev.itzgamesgd.moreminecrafts.fishing.FishingRodMenu;
import dev.itzgamesgd.moreminecrafts.fishing.ModuleDurability;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;

/** The server's three module slots remain the source of truth for click, drag, swap and shift-click. */
public final class FishingRodScreen extends AbstractContainerScreen<FishingRodMenu> {
    private static final int[] CARD_X = {80, 155, 230};
    private static final String[] FAMILIES = {"reel", "crankbait", "minnow"};
    private static final int CARD_Y = 8;

    public FishingRodScreen(FishingRodMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 330, 270);
        titleLabelX = 10;
        titleLabelY = 8;
        inventoryLabelX = 82;
        inventoryLabelY = 178;
    }

    private static Identifier texture(String name) {
        return ModIds.id("textures/gui/fishing/equipment/" + name + ".png");
    }

    /** Scale at render time so the approved PNG stays byte-for-byte unchanged. */
    private static void picture(GuiGraphicsExtractor graphics, String name, int x, int y,
                                int width, int height, int nativeWidth, int nativeHeight) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(width / (float) nativeWidth, height / (float) nativeHeight);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture(name), 0, 0, 0, 0,
                nativeWidth, nativeHeight, nativeWidth, nativeHeight);
        graphics.pose().popMatrix();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);
        int x = leftPos, y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xEE17212C);
        picture(graphics, "rod_panel", x + 8, y + CARD_Y, 68, 91, 1086, 1448);
        picture(graphics, "reel_card_empty", x + CARD_X[0], y + CARD_Y, 70, 93, 1086, 1448);
        picture(graphics, "crankbait_card_empty", x + CARD_X[1], y + CARD_Y, 70, 93, 1086, 1448);
        picture(graphics, "minnow_card_empty", x + CARD_X[2], y + CARD_Y, 70, 93, 1086, 1448);
        picture(graphics, "equipment_effects_panel", x + 8, y + 103, 228, 76, 2172, 724);
        picture(graphics, "close_button", x + 306, y + 4, 18, 18, 1254, 1254);
        for (int i = 0; i < 3; i++) {
            ItemStack stack = menu.getSlot(i).getItem();
            if (stack.getItem() instanceof FishingEquipmentItem item) {
                picture(graphics, "tier_" + switch (item.tier()) {
                    case 1 -> "I";
                    case 2 -> "II";
                    default -> "III";
                }, x + CARD_X[i] + 54, y + CARD_Y + 59, 11, 11,
                        switch (item.tier()) { case 1 -> 115; case 2 -> 118; default -> 119; }, 116);
                int max = stack.getMaxDamage();
                int remaining = ModuleDurability.remaining(stack);
                float ratio = max == 0 ? 0 : Math.clamp(remaining / (float) max, 0f, 1f);
                int barX = x + CARD_X[i] + 10, barY = y + CARD_Y + 72;
                int fill = Math.round(50 * ratio);
                int color = ratio < .33f ? 0xFFE34A45 : ratio < .66f ? 0xFFE9B13C : 0xFF60C675;
                if (fill > 0) graphics.fill(barX + 2, barY + 6, barX + 2 + fill, barY + 10, color);
                picture(graphics, "durability_frame", barX, barY, 54, 18, 2172, 724);
            }
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, 17, 14, 0xFFFFFFFF, true);
        graphics.text(font, Component.translatable("container.inventory"),
                inventoryLabelX, inventoryLabelY, 0xFFFFFFFF, false);
        for (int i = 0; i < 3; i++) {
            int x = CARD_X[i];
            graphics.text(font, Component.translatable("menu.moreminecrafts.fishing." + FAMILIES[i]),
                    x + 8, CARD_Y + 16, 0xFFFFFFFF, true);
        }
        // This row is informational: the rod's weight formula is not yet defined.
        for (int i = 0; i < 4; i++) {
            String key = i == 0 ? "rod" : FAMILIES[i - 1];
            graphics.text(font, Component.translatable("menu.moreminecrafts.fishing.effect." + key),
                    40, 127 + i * 12, 0xFFDBE6E9, false);
        }
        ItemStack rod = minecraft.player.getMainHandItem().getItem() instanceof FishingRodItem
                ? minecraft.player.getMainHandItem() : minecraft.player.getOffhandItem();
        if (rod.getItem() instanceof FishingRodItem) enlargedItem(graphics, rod, 28, 42, 2f);
        for (int i = 0; i < 3; i++) {
            ItemStack stack = menu.getSlot(i).getItem();
            if (!stack.isEmpty()) enlargedItem(graphics, stack, CARD_X[i] + 22, 35, 2f);
        }
    }

    private static void enlargedItem(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y, float scale) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        graphics.item(stack, 0, 0);
        graphics.pose().popMatrix();
    }

    /**
     * Route the full visible card rectangle through normal container handling.
     * The server-owned slot/menu remains authoritative for clicks and quick-craft drags.
     */
    private MouseButtonEvent cardSlotEvent(MouseButtonEvent event) {
        double cardX = event.x() - leftPos;
        double cardY = event.y() - topPos;
        for (int i = 0; i < CARD_X.length; i++) {
            if (cardX >= CARD_X[i] && cardX < CARD_X[i] + 70
                    && cardY >= CARD_Y && cardY < CARD_Y + 93) {
                return new MouseButtonEvent(leftPos + 107 + i * 75 + 8,
                        topPos + 45 + 8, event.buttonInfo());
            }
        }
        return event;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        return super.mouseDragged(cardSlotEvent(event), dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return super.mouseReleased(cardSlotEvent(event));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT
                && event.x() >= leftPos + 306 && event.x() < leftPos + 324
                && event.y() >= topPos + 4 && event.y() < topPos + 22) {
            onClose();
            return true;
        }
        return super.mouseClicked(cardSlotEvent(event), doubleClick);
    }
}
