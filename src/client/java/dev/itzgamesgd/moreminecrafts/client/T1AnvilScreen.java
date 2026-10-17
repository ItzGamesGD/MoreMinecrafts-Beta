package dev.itzgamesgd.moreminecrafts.client;

import dev.itzgamesgd.moreminecrafts.enhancement.EnhancementOperation;
import dev.itzgamesgd.moreminecrafts.progression.T1AnvilModes;
import dev.itzgamesgd.moreminecrafts.progression.T1Balance;
import dev.itzgamesgd.moreminecrafts.progression.RawXp;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.Slot;

import java.text.NumberFormat;
import java.util.Locale;

/** Keeps the vanilla Anvil panel and slots, adding stable mode tabs and an in-place Enhance action. */
public final class T1AnvilScreen extends AnvilScreen {
    private Button action;
    private final Button[] modes = new Button[3];

    public T1AnvilScreen(AnvilMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    private int mode() {
        return ((T1AnvilModes) menu).moreminecrafts$mode();
    }

    private void request(int id) {
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    protected void subInit() {
        super.subInit();
        String[] keys = {"enhancement", "normal_anvil", "book_combine"};
        int[] widths = {56, 80, 112};
        int x = leftPos - 36;
        for (int i = 0; i < 3; i++) {
            final int index = i;
            modes[i] = addRenderableWidget(Button.builder(Component.translatable("menu.moreminecrafts." + keys[i]), b -> request(index))
                    .bounds(x, topPos - 24, widths[i], 20).build());
            x += widths[i];
        }
        action = addRenderableWidget(Button.builder(Component.empty(), b -> request(3))
                .bounds(leftPos + 101, topPos + 42, 70, 20).build());
        refresh();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        refresh();
    }

    private void refresh() {
        if (action == null) return;
        for (int i = 0; i < modes.length; i++) modes[i].active = mode() != i;
        for (var child : children()) if (child instanceof EditBox box) box.visible = mode() == 1;
        action.visible = mode() != 1;
        if (mode() == 0) {
            action.setX(leftPos + 101);
            action.setY(topPos + 42);
            action.setSize(70, 20);
            action.setMessage(Component.translatable("menu.moreminecrafts.enhancement"));
            var equipment = menu.getSlot(0).getItem();
            var material = menu.getSlot(1).getItem();
            var requirement = EnhancementOperation.requirement(equipment);
            boolean enoughMaterial = requirement.isPresent()
                    && EnhancementOperation.isEnhancementMaterial(material)
                    && material.getCount() >= requirement.get().materialCost();
            action.active = requirement.isPresent() && enoughMaterial
                    && RawXp.canPay(minecraft.player, requirement.get().rawXpCost());
            if (requirement.isPresent()) {
                var state = requirement.get();
                action.setTooltip(Tooltip.create(Component.translatable("menu.moreminecrafts.enhancement_requirement",
                        state.targetLevel() - 1, state.targetLevel(), state.rawXpCost(), state.materialCost())));
            } else if (!equipment.isEmpty() && EnhancementOperation.level(equipment) >= 20) {
                action.setTooltip(Tooltip.create(Component.translatable("menu.moreminecrafts.enhancement_maximum")));
            } else {
                action.setTooltip(Tooltip.create(Component.translatable("menu.moreminecrafts.enhancement_unavailable")));
            }
        } else {
            action.setX(leftPos + 5);
            action.setY(topPos + 22);
            action.setSize(166, 20);
            action.setMessage(Component.translatable("menu.moreminecrafts.book_combine"));
            action.active = menu.getSlot(2).hasItem() && menu.getSlot(2).mayPickup(minecraft.player);
            action.setTooltip(Tooltip.create(Component.translatable("menu.moreminecrafts.combine_action_tooltip")));
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractBackground(g, mouseX, mouseY, delta);
        if (mode() == 0) {
            // The result slot and arrow belong to the old material→output interaction. Keep only the two inputs.
            g.fill(leftPos + 99, topPos + 34, leftPos + 175, topPos + 71, 0xFFC6C6C6);
            // The vanilla rename field is inactive in Enhancement mode; use its space for the two cost lines.
            g.fill(leftPos + 55, topPos + 19, leftPos + 173, topPos + 35, 0xFFC6C6C6);
        }
    }

    @Override
    protected void extractSlot(GuiGraphicsExtractor g, Slot slot, int mouseX, int mouseY) {
        if (mode() == 0 && slot == menu.getSlot(2)) return;
        super.extractSlot(g, slot, mouseX, mouseY);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (mode() == 1) {
            super.extractLabels(g, mouseX, mouseY);
            return;
        }
        g.text(font, Component.translatable("container.inventory"), 8, 72, 0xFF404040, false);
        if (mode() == 0) {
            int current = EnhancementOperation.level(menu.getSlot(0).getItem());
            var requirement = EnhancementOperation.requirement(menu.getSlot(0).getItem());
            int target = requirement.map(EnhancementOperation.Result::targetLevel).orElse(current);
            int xp = requirement.map(EnhancementOperation.Result::rawXpCost).orElse(0);
            int stone = requirement.map(EnhancementOperation.Result::materialCost).orElse(0);
            if (!menu.getSlot(0).getItem().isEmpty() && current >= 20) {
                g.text(font, Component.literal("+" + current + " → ").append(Component.translatable("menu.moreminecrafts.maximum")), 62, 6, 0xFF303030, false);
            } else {
                g.text(font, Component.literal("+" + current + " → +" + target), 62, 6, 0xFF303030, false);
                g.text(font, Component.translatable("menu.moreminecrafts.enhancement_cost_line",
                        NumberFormat.getIntegerInstance(Locale.ROOT).format(xp), stone), 62, 18, 0xFF303030, false);
            }
        } else {
            g.text(font, Component.translatable("menu.moreminecrafts.xp_amount",
                    NumberFormat.getIntegerInstance(Locale.ROOT).format(menu.getCost())),
                    115, 6, 0xFF303030, false);
        }
    }
}
