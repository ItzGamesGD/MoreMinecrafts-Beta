package dev.itzgamesgd.moreminecrafts.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** One native-tooltip row pairing a real inventory item icon with its localized quantities. */
final class ProgressionMaterialTooltipComponent implements ClientTooltipComponent {
    private final ItemStack icon;
    private final Component label;

    ProgressionMaterialTooltipComponent(ItemStack icon, Component label) {
        this.icon = icon.copy();
        this.label = label;
    }

    @Override public int getHeight(Font font) { return 18; }
    @Override public int getWidth(Font font) { return 20 + font.width(label); }

    @Override public void extractImage(Font font, int x, int y, int width, int height, GuiGraphicsExtractor graphics) {
        graphics.item(icon, x, y + 1);
        graphics.text(font, label, x + 20, y + 5, 0xFFFFFFFF);
    }
}
