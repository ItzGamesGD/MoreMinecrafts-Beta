package dev.itzgamesgd.moreminecrafts.client;

import dev.itzgamesgd.moreminecrafts.accessory.AccessoryEnhancementOperation;
import dev.itzgamesgd.moreminecrafts.accessory.AnvilCatalystLayout;
import dev.itzgamesgd.moreminecrafts.enhancement.EnhancementOperation;
import dev.itzgamesgd.moreminecrafts.progression.T1AnvilModes;
import dev.itzgamesgd.moreminecrafts.progression.T1Balance;
import dev.itzgamesgd.moreminecrafts.progression.XpLevels;
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
            boolean inline = AnvilCatalystLayout.active() == AnvilCatalystLayout.INLINE_RIGHT;
            action.setX(leftPos + (inline ? 122 : 101));
            action.setY(topPos + 42);
            action.setSize(inline ? 49 : 70, 20);
            action.setMessage(Component.translatable("menu.moreminecrafts.enhancement"));
            var equipment = menu.getSlot(0).getItem();
            var material = menu.getSlot(1).getItem();
            if (AccessoryEnhancementOperation.isAccessory(equipment)) {
                var requirement = AccessoryEnhancementOperation.nextRequirement(equipment);
                var catalyst = menu.getSlot(((T1AnvilModes) menu).moreminecrafts$catalystSlotIndex()).getItem();
                action.active = requirement.isPresent()
                        && material.is(dev.itzgamesgd.moreminecrafts.registry.ModItems.ENHANCEMENT_STONE)
                        && material.getCount() >= requirement.get().stoneCost()
                        && (requirement.get().catalystCost() == 0
                            || catalyst.is(dev.itzgamesgd.moreminecrafts.registry.ModItems.RELIC_CATALYST)
                                && catalyst.getCount() >= requirement.get().catalystCost())
                        && RawXp.available(minecraft.player) >= requirement.get().rawExperienceCost();
                if (requirement.isPresent()) {
                    var state = requirement.get();
                    action.setTooltip(Tooltip.create(Component.translatable("menu.moreminecrafts.accessory_enhancement_requirement",
                            state.targetLevel() - 1, state.targetLevel(), state.rawExperienceCost(),
                            state.stoneCost(), state.catalystCost())));
                } else {
                    action.setTooltip(Tooltip.create(Component.translatable("menu.moreminecrafts.enhancement_maximum")));
                }
            } else {
                var requirement = EnhancementOperation.requirement(equipment);
                boolean enoughMaterial = requirement.isPresent()
                        && EnhancementOperation.isEnhancementMaterial(material)
                        && material.getCount() >= requirement.get().materialCost();
                action.active = requirement.isPresent() && enoughMaterial
                        && XpLevels.canPay(minecraft.player, requirement.get().experienceLevelCost());
                if (requirement.isPresent()) {
                    var state = requirement.get();
                    action.setTooltip(Tooltip.create(Component.translatable("menu.moreminecrafts.enhancement_requirement",
                            state.targetLevel() - 1, state.targetLevel(), state.experienceLevelCost(), state.materialCost())));
                } else if (!equipment.isEmpty() && EnhancementOperation.level(equipment) >= 20) {
                    action.setTooltip(Tooltip.create(Component.translatable("menu.moreminecrafts.enhancement_maximum")));
                } else {
                    action.setTooltip(Tooltip.create(Component.translatable("menu.moreminecrafts.enhancement_unavailable")));
                }
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
            drawComparisonCatalystCell(g);
        }
    }

    /** CURRENT draws no cell (as shipped); the comparison layouts sit inside the panel and need one. */
    private void drawComparisonCatalystCell(GuiGraphicsExtractor g) {
        if (AnvilCatalystLayout.active() == AnvilCatalystLayout.CURRENT) return;
        int x = leftPos + AnvilCatalystLayout.active().x - 1;
        int y = topPos + AnvilCatalystLayout.active().y - 1;
        g.fill(x, y, x + 18, y + 18, 0xFF8B8B8B);
        g.fill(x, y, x + 17, y + 1, 0xFF373737);
        g.fill(x, y, x + 1, y + 17, 0xFF373737);
        g.fill(x + 1, y + 17, x + 18, y + 18, 0xFFFFFFFF);
        g.fill(x + 17, y + 1, x + 18, y + 18, 0xFFFFFFFF);
    }

    @Override
    protected void extractSlot(GuiGraphicsExtractor g, Slot slot, int mouseX, int mouseY) {
        if (mode() == 0 && slot == menu.getSlot(2)) return;
        if (mode() != 0 && slot == menu.getSlot(((T1AnvilModes) menu).moreminecrafts$catalystSlotIndex())) return;
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
            var equipment = menu.getSlot(0).getItem();
            boolean accessory = AccessoryEnhancementOperation.isAccessory(equipment);
            int current = accessory ? AccessoryEnhancementOperation.level(equipment) : EnhancementOperation.level(equipment);
            var accessoryRequirement = accessory ? AccessoryEnhancementOperation.nextRequirement(equipment) : java.util.Optional.<dev.itzgamesgd.moreminecrafts.accessory.AccessoryEnhancementRules.Plan>empty();
            var equipmentRequirement = accessory ? java.util.Optional.<EnhancementOperation.Result>empty() : EnhancementOperation.requirement(equipment);
            int target = accessory ? accessoryRequirement.map(dev.itzgamesgd.moreminecrafts.accessory.AccessoryEnhancementRules.Plan::targetLevel).orElse(current)
                    : equipmentRequirement.map(EnhancementOperation.Result::targetLevel).orElse(current);
            int xp = accessory ? accessoryRequirement.map(dev.itzgamesgd.moreminecrafts.accessory.AccessoryEnhancementRules.Plan::rawExperienceCost).orElse(0)
                    : equipmentRequirement.map(EnhancementOperation.Result::experienceLevelCost).orElse(0);
            int stone = accessory ? accessoryRequirement.map(dev.itzgamesgd.moreminecrafts.accessory.AccessoryEnhancementRules.Plan::stoneCost).orElse(0)
                    : equipmentRequirement.map(EnhancementOperation.Result::materialCost).orElse(0);
            int catalyst = accessory ? accessoryRequirement.map(dev.itzgamesgd.moreminecrafts.accessory.AccessoryEnhancementRules.Plan::catalystCost).orElse(0) : 0;
            int maximum = accessory ? 10 : 20;
            if (!equipment.isEmpty() && current >= maximum) {
                g.text(font, Component.literal("+" + current + " → ").append(Component.translatable("menu.moreminecrafts.maximum")), 62, 6, 0xFF303030, false);
            } else {
                g.text(font, Component.literal("+" + current + " → +" + target), 62, 6, 0xFF303030, false);
                if (accessory && AnvilCatalystLayout.active() != AnvilCatalystLayout.CURRENT) {
                    // The single-line cost string is wider than the panel; the comparison layouts wrap the catalyst.
                    g.text(font, Component.translatable("menu.moreminecrafts.accessory_enhancement_cost_line_compact",
                            NumberFormat.getIntegerInstance(Locale.ROOT).format(xp), stone), 62, 18, 0xFF303030, false);
                    if (catalyst > 0) {
                        g.text(font, Component.translatable("menu.moreminecrafts.accessory_enhancement_catalyst_line", catalyst),
                                62, 28, 0xFF303030, false);
                    }
                } else if (accessory) {
                    g.text(font, Component.translatable("menu.moreminecrafts.accessory_enhancement_cost_line",
                            NumberFormat.getIntegerInstance(Locale.ROOT).format(xp), stone, catalyst), 62, 18, 0xFF303030, false);
                } else {
                    g.text(font, Component.translatable("menu.moreminecrafts.enhancement_cost_line",
                            NumberFormat.getIntegerInstance(Locale.ROOT).format(xp), stone), 62, 18, 0xFF303030, false);
                }
            }
        } else {
            g.text(font, Component.translatable("menu.moreminecrafts.xp_amount",
                    NumberFormat.getIntegerInstance(Locale.ROOT).format(menu.getCost())),
                    115, 6, 0xFF303030, false);
        }
    }
}
