package dev.itzgamesgd.moreminecrafts.client;

import dev.itzgamesgd.moreminecrafts.accessory.AccessoryEnhancementOperation;
import dev.itzgamesgd.moreminecrafts.accessory.AnvilCatalystUiLayout;
import dev.itzgamesgd.moreminecrafts.enhancement.EnhancementOperation;
import dev.itzgamesgd.moreminecrafts.progression.T1AnvilModes;
import dev.itzgamesgd.moreminecrafts.progression.T1Balance;
import dev.itzgamesgd.moreminecrafts.progression.RawXp;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.Slot;

import java.text.NumberFormat;
import java.util.Locale;

/** Keeps the vanilla Anvil panel and slots, adding stable mode tabs and an in-place Enhance action. */
public final class T1AnvilScreen extends AnvilScreen {
    private static final int SHORT_COLOR = EnchantAnvilSkin.TEXT_BAD;
    private static final Identifier ERROR_SPRITE = Identifier.withDefaultNamespace("container/anvil/error");
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
        String[] tabTextures = {"anvil_tab_112", "anvil_tab_160", "anvil_tab_224"};   // named by native art width
        int x = leftPos - 36;
        for (int i = 0; i < 3; i++) {
            final int index = i;
            modes[i] = addRenderableWidget(new EnchantSkinButton(x, topPos - 24, widths[i], 20,
                    Component.translatable("menu.moreminecrafts." + keys[i]), b -> request(index),
                    EnchantSkinButton.Look.plain(tabTextures[i], "selected")));
            x += widths[i];
        }
        action = addRenderableWidget(new EnchantSkinButton(leftPos + 101, topPos + 42, 70, 20, Component.empty(),
                b -> request(3), EnchantSkinButton.Look.plain("enhance_btn", "disabled")));
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
            action.setX(leftPos + AnvilCatalystUiLayout.ACTION_X);
            action.setY(topPos + AnvilCatalystUiLayout.ACTION_Y);
            action.setSize(AnvilCatalystUiLayout.ACTION_WIDTH, AnvilCatalystUiLayout.ACTION_HEIGHT);
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
        // The skin covers the whole vanilla panel (slots, arrow, rename field and error icon included) and redraws what
        // each mode needs at the unchanged vanilla coordinates.
        EnchantAnvilSkin.blit(g, "anvil_panel_frame", leftPos, topPos, 176, 166);
        EnchantAnvilSkin.blit(g, "anvil_panel_overlay", leftPos, topPos, 176, 166);
        int catalystIndex = ((T1AnvilModes) menu).moreminecrafts$catalystSlotIndex();
        for (int i = 0; i < menu.slots.size(); i++) {
            if (mode() == 0 && i == 2) continue;                 // no result slot in Enhancement mode
            if (mode() != 0 && i == catalystIndex) continue;     // the catalyst slot only exists in Enhancement mode
            Slot slot = menu.getSlot(i);
            String look = i == 0 ? "slot_equipment_normal" : i == 1 && mode() == 2 ? "slot_book_normal" : "slot_normal";
            EnchantAnvilSkin.blit(g, look, leftPos + slot.x - 1, topPos + slot.y - 1, 18, 18);
        }
        EnchantAnvilSkin.blit(g, "plus", leftPos + 55, topPos + 50, 10, 10);
        if (mode() == 0) {
            // The two cost lines use the space of the rename field and the old result arrow.
            EnchantAnvilSkin.blit(g, "cost_panel_a", leftPos + 99, topPos + 34, 76, 37);
            EnchantAnvilSkin.blit(g, "cost_panel_b", leftPos + 50, topPos + 19, 118, 17);
            return;
        }
        EnchantAnvilSkin.blit(g, "output_arrow", leftPos + 99, topPos + 45, 22, 15);
        if ((menu.getSlot(0).hasItem() || menu.getSlot(1).hasItem()) && !menu.getSlot(2).hasItem()) {
            g.blitSprite(RenderPipelines.GUI_TEXTURED, ERROR_SPRITE, leftPos + 99, topPos + 45, 28, 21);
        }
        if (mode() == 1) {
            // Rename field background (the vanilla sprite sits under the covered area).
            g.fill(leftPos + 59, topPos + 20, leftPos + 169, topPos + 36, 0xFF986A24);
            g.fill(leftPos + 60, topPos + 21, leftPos + 168, topPos + 35, 0xFF120A20);
        }
    }

    @Override
    protected void extractSlot(GuiGraphicsExtractor g, Slot slot, int mouseX, int mouseY) {
        if (mode() == 0 && slot == menu.getSlot(2)) return;
        if (mode() != 0 && slot == menu.getSlot(((T1AnvilModes) menu).moreminecrafts$catalystSlotIndex())) return;
        super.extractSlot(g, slot, mouseX, mouseY);
    }

    /** Vanilla repair labels redrawn in the skin's light colours (the vanilla ones are dark text for a grey panel). */
    private void extractRepairLabels(GuiGraphicsExtractor g) {
        g.text(font, title, titleLabelX, titleLabelY, EnchantAnvilSkin.TEXT, false);
        g.text(font, Component.translatable("container.inventory"), inventoryLabelX, inventoryLabelY, EnchantAnvilSkin.TEXT, false);
        int cost = menu.getCost();
        if (cost <= 0 || minecraft.player == null) return;
        Component label = null;
        int color = 0xFF80FF20;
        if (cost >= 40 && !minecraft.player.getAbilities().instabuild) {
            label = Component.translatable("container.repair.expensive");
            color = 0xFFFF6060;
        } else if (menu.getSlot(2).hasItem()) {
            label = Component.translatable("container.repair.cost", cost);
            if (!menu.getSlot(2).mayPickup(minecraft.player)) color = 0xFFFF6060;
        }
        if (label != null) {
            int x = imageWidth - 8 - font.width(label) - 2;
            g.fill(x - 2, 67, imageWidth - 8, 79, 0x4F000000);
            g.text(font, label, x, 69, color, true);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (mode() == 1) {
            extractRepairLabels(g);
            return;
        }
        g.text(font, Component.translatable("container.inventory"), 8, 72, EnchantAnvilSkin.TEXT, false);
        if (mode() == 0) {
            var equipment = menu.getSlot(0).getItem();
            boolean accessory = AccessoryEnhancementOperation.isAccessory(equipment);
            int current = accessory ? AccessoryEnhancementOperation.level(equipment) : EnhancementOperation.level(equipment);
            var accessoryRequirement = accessory ? AccessoryEnhancementOperation.nextRequirement(equipment) : java.util.Optional.<dev.itzgamesgd.moreminecrafts.accessory.AccessoryEnhancementRules.Plan>empty();
            var equipmentRequirement = accessory ? java.util.Optional.<EnhancementOperation.Result>empty() : EnhancementOperation.requirement(equipment);
            int target = accessory ? accessoryRequirement.map(dev.itzgamesgd.moreminecrafts.accessory.AccessoryEnhancementRules.Plan::targetLevel).orElse(current)
                    : equipmentRequirement.map(EnhancementOperation.Result::targetLevel).orElse(current);
            int xp = accessory ? accessoryRequirement.map(dev.itzgamesgd.moreminecrafts.accessory.AccessoryEnhancementRules.Plan::rawExperienceCost).orElse(0)
                    : equipmentRequirement.map(EnhancementOperation.Result::rawXpCost).orElse(0);
            int stone = accessory ? accessoryRequirement.map(dev.itzgamesgd.moreminecrafts.accessory.AccessoryEnhancementRules.Plan::stoneCost).orElse(0)
                    : equipmentRequirement.map(EnhancementOperation.Result::materialCost).orElse(0);
            int catalyst = accessory ? accessoryRequirement.map(dev.itzgamesgd.moreminecrafts.accessory.AccessoryEnhancementRules.Plan::catalystCost).orElse(0) : 0;
            int maximum = accessory ? AccessoryEnhancementOperation.maximumLevel(equipment) : 20;
            boolean hasRequirement = accessory ? accessoryRequirement.isPresent() : equipmentRequirement.isPresent();
            var stoneStack = menu.getSlot(1).getItem();
            boolean stoneOk = accessory ? stoneStack.is(dev.itzgamesgd.moreminecrafts.registry.ModItems.ENHANCEMENT_STONE)
                    : EnhancementOperation.isEnhancementMaterial(stoneStack);
            boolean xpOk = minecraft.player != null && RawXp.canPay(minecraft.player, xp);
            // Insufficient cost lines are drawn red (vanilla anvil convention); the Enhance tooltip lists the exact amounts.
            int costColor = xpOk && stoneOk && stoneStack.getCount() >= stone ? EnchantAnvilSkin.TEXT : SHORT_COLOR;
            var catalystStack = accessory ? menu.getSlot(((T1AnvilModes) menu).moreminecrafts$catalystSlotIndex()).getItem()
                    : net.minecraft.world.item.ItemStack.EMPTY;
            int catalystColor = catalystStack.is(dev.itzgamesgd.moreminecrafts.registry.ModItems.RELIC_CATALYST)
                    && catalystStack.getCount() >= catalyst ? EnchantAnvilSkin.TEXT : SHORT_COLOR;
            if (!equipment.isEmpty() && current >= maximum) {
                g.text(font, Component.literal("+" + current + " → ").append(Component.translatable("menu.moreminecrafts.maximum")), 54, 10, EnchantAnvilSkin.TEXT, false);
            } else if (hasRequirement) {
                // Empty or non-enhanceable input shows no step; the Enhance button tooltip explains what to insert.
                g.text(font, Component.literal("+" + current + " → +" + target), 54, 10, EnchantAnvilSkin.TEXT, false);
                if (accessory) {
                    // The three costs do not fit one line in the panel, so the catalyst count wraps to its own line.
                    g.text(font, Component.translatable("menu.moreminecrafts.accessory_enhancement_cost_line_compact",
                            NumberFormat.getIntegerInstance(Locale.ROOT).format(xp), stone), 54, 23, costColor, false);
                    if (catalyst > 0) {
                        g.text(font, Component.translatable("menu.moreminecrafts.accessory_enhancement_catalyst_line", catalyst),
                                54, 38, catalystColor, false);
                    }
                } else {
                    g.text(font, Component.translatable("menu.moreminecrafts.enhancement_cost_line",
                            NumberFormat.getIntegerInstance(Locale.ROOT).format(xp), stone), 54, 23, costColor, false);
                }
            }
        } else {
            g.text(font, Component.translatable("menu.moreminecrafts.xp_amount",
                    NumberFormat.getIntegerInstance(Locale.ROOT).format(menu.getCost())),
                    115, 10, EnchantAnvilSkin.TEXT, false);
        }
    }
}
