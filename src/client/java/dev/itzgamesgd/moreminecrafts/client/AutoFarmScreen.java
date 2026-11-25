package dev.itzgamesgd.moreminecrafts.client;

import dev.itzgamesgd.moreminecrafts.progression.AutoFarmMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Two 5×5 farm inventories in the light, vanilla-container visual language of the approved reference. */
public final class AutoFarmScreen extends AbstractContainerScreen<AutoFarmMenu> {
    public AutoFarmScreen(AutoFarmMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 222, 212);
        titleLabelX = 10; titleLabelY = 6; inventoryLabelX = 30; inventoryLabelY = 121;
    }

    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);
        int x = leftPos, y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF1B1B1B);
        graphics.fill(x + 3, y + 3, x + imageWidth - 3, y + imageHeight - 3, 0xFFC9C9C6);
        graphics.fill(x + 8, y + 8, x + imageWidth - 8, y + 19, 0xFFB5B5B2);
        // One continuous body panel: the player-inventory label sits between the farm grids and the inventory rows
        // instead of overlapping the seed grid, and the section labels stay clear of the title band.
        graphics.fill(x + 7, y + 20, x + imageWidth - 7, y + imageHeight - 8, 0xFFBFBFBC);
        graphics.fill(x + 110, y + 30, x + 112, y + 119, 0xFF8C8C89);
        for (int i = 0; i < 50; i++) {
            int local = i % 25;
            int cx = x + (i < 25 ? 10 : 122) + (local % 5) * 18;
            int cy = y + 32 + (local / 5) * 18;
            slot(graphics, cx, cy);
        }
        for (int i = 0; i < 36; i++) {
            int cx = x + 30 + (i % 9) * 18;
            int cy = y + (i < 27 ? 130 + (i / 9) * 18 : 188);
            slot(graphics, cx, cy);
        }
    }

    @Override protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, 12, 10, 0xFF171717, false);
        graphics.text(font, Component.translatable("menu.moreminecrafts.auto_farm.seeds"), 10, 21, 0xFF252525);
        graphics.text(font, Component.translatable("menu.moreminecrafts.auto_farm.output"), 122, 21, 0xFF252525);
        graphics.text(font, Component.translatable("container.inventory"), inventoryLabelX, inventoryLabelY, 0xFF252525);
    }

    private static void slot(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF282828);
        graphics.fill(x, y, x + 16, y + 16, 0xFF777774);
        graphics.fill(x + 1, y + 1, x + 15, y + 15, 0xFF858582);
    }
}
