package dev.itzgamesgd.moreminecrafts.client;

import dev.itzgamesgd.moreminecrafts.bounty.ContractData;
import dev.itzgamesgd.moreminecrafts.bounty.ContractService;
import dev.itzgamesgd.moreminecrafts.bounty.ContractStacks;
import dev.itzgamesgd.moreminecrafts.network.ContractActionPayload;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Single vertical contract screen: tier and status, objectives, live delivery holdings, fixed rewards, then
 * accept/complete/abandon. The contract is re-read from the live inventory every frame, and every action is
 * validated again by the server.
 */
public final class ContractScreen extends Screen {
    private static final int PANEL_WIDTH = 260;
    private static final int PANEL_HEIGHT = 240;
    private static final int PANEL_COLOR = 0xFFC6C6C6;
    private static final int BORDER_COLOR = 0xFF373737;
    private static final int TEXT_COLOR = 0xFF404040;
    private static final int DONE_COLOR = 0xFF2E7D32;
    private static final int SHORT_COLOR = 0xFF8B1A1A;
    private static final int WARNING_COLOR = 0xFF8B1A1A;
    private static final int BUTTON_WIDTH = 72;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 6;

    private final String contractId;
    private boolean confirmingAbandon;
    private Button accept;
    private Button complete;
    private Button abandon;
    private Button confirmAbandon;
    private Button cancelAbandon;

    public ContractScreen(String contractId) {
        super(Component.translatable("item.moreminecrafts.bounty_contract"));
        this.contractId = contractId;
    }

    @Override
    protected void init() {
        int row = top() + PANEL_HEIGHT - 28;
        int centerX = width / 2;
        int threeRowX = centerX - (BUTTON_WIDTH * 3 + BUTTON_GAP * 2) / 2;
        accept = button("moreminecrafts.contract.button.accept", threeRowX, row, b -> send("ACCEPT"));
        complete = button("moreminecrafts.contract.button.complete", threeRowX + BUTTON_WIDTH + BUTTON_GAP, row,
                b -> send("COMPLETE"));
        abandon = button("moreminecrafts.contract.button.abandon", threeRowX + (BUTTON_WIDTH + BUTTON_GAP) * 2, row,
                b -> confirmingAbandon = true);
        confirmAbandon = button("moreminecrafts.contract.button.confirm_abandon", centerX - BUTTON_WIDTH - BUTTON_GAP / 2,
                row, b -> {
                    send("ABANDON");
                    onClose();
                });
        cancelAbandon = button("moreminecrafts.contract.button.cancel", centerX + BUTTON_GAP / 2, row,
                b -> confirmingAbandon = false);
    }

    private Button button(String key, int x, int y, Button.OnPress onPress) {
        return addRenderableWidget(Button.builder(Component.translatable(key), onPress)
                .bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        ContractData data = current();
        if (data == null) {
            onClose();
            return;
        }
        int left = left();
        int top = top();
        int right = left + PANEL_WIDTH;
        int centerX = width / 2;
        Map<String, Integer> holdings = ContractService.quantities(minecraft.player.getInventory());
        boolean ready = data.ready(holdings);

        graphics.fill(0, 0, width, height, 0x99000000);
        graphics.fill(left - 1, top - 1, right + 1, top + PANEL_HEIGHT + 1, BORDER_COLOR);
        graphics.fill(left, top, right, top + PANEL_HEIGHT, PANEL_COLOR);
        graphics.centeredText(font, Component.translatable("moreminecrafts.contract.tier." + lower(data.tier())),
                centerX, top + 8, TEXT_COLOR);
        graphics.centeredText(font, Component.translatable("moreminecrafts.contract.status." + lower(data.status())),
                centerX, top + 22, TEXT_COLOR);

        int x = left + 10;
        int y = top + 40;
        graphics.text(font, Component.translatable("moreminecrafts.contract.section.objectives"), x, y, TEXT_COLOR);
        y += 12;
        boolean hasDelivery = false;
        for (ContractData.Objective objective : data.objectives()) {
            Component label = objective.category() == ContractData.Category.DELIVERY
                    ? Component.translatable("moreminecrafts.contract.objective_need", categoryName(objective),
                    targetName(objective), objective.required())
                    : Component.translatable("moreminecrafts.contract.objective_progress", categoryName(objective),
                    targetName(objective), objective.progress(), objective.required());
            graphics.text(font, label, x, y, objective.progress() >= objective.required() ? DONE_COLOR : TEXT_COLOR);
            y += 12;
            hasDelivery |= objective.category() == ContractData.Category.DELIVERY;
        }

        if (hasDelivery) {
            y += 6;
            graphics.text(font, Component.translatable("moreminecrafts.contract.section.holdings"), x, y, TEXT_COLOR);
            y += 12;
            for (ContractData.Objective objective : data.objectives()) {
                if (objective.category() != ContractData.Category.DELIVERY) continue;
                int have = holdings.getOrDefault(objective.target(), 0);
                graphics.text(font, Component.translatable("moreminecrafts.contract.holding", targetName(objective),
                                have, objective.required()), x, y, have >= objective.required() ? DONE_COLOR : SHORT_COLOR);
                y += 12;
            }
        }

        y += 6;
        graphics.text(font, Component.translatable("moreminecrafts.contract.section.rewards"), x, y, TEXT_COLOR);
        y += 12;
        graphics.text(font, Component.translatable("moreminecrafts.contract.reward.emeralds", data.reward().emeralds()),
                x + 20, y + 4, TEXT_COLOR);
        graphics.item(new ItemStack(Items.EMERALD), x, y);
        y += 18;
        ItemStack rewardStack = new ItemStack(itemOf(data.reward().item()));
        graphics.item(rewardStack, x, y);
        graphics.text(font, Component.translatable("moreminecrafts.contract.reward.item",
                rewardStack.getHoverName(), data.reward().count()), x + 20, y + 4, TEXT_COLOR);

        if (confirmingAbandon)
            graphics.centeredText(font, Component.translatable("moreminecrafts.contract.abandon_warning"),
                    centerX, top + PANEL_HEIGHT - 44, WARNING_COLOR);

        boolean accepted = data.status() == ContractData.Status.ACCEPTED;
        boolean offered = data.status() == ContractData.Status.OFFERED;
        accept.visible = offered && !confirmingAbandon;
        complete.visible = accepted && !confirmingAbandon;
        complete.active = ready;
        abandon.visible = !confirmingAbandon;
        confirmAbandon.visible = confirmingAbandon;
        cancelAbandon.visible = confirmingAbandon;

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    /** Sends the action for the exact slot this screen shows; the server refuses it if that slot changed. */
    private void send(String action) {
        int slot = slotOf();
        if (slot >= 0) ClientPlayNetworking.send(new ContractActionPayload(contractId, action, slot));
    }

    private ContractData current() {
        if (minecraft.player == null) return null;
        int slot = slotOf();
        return slot < 0 ? null : ContractStacks.read(minecraft.player.getInventory().getItem(slot));
    }

    private int slotOf() {
        if (minecraft.player == null) return -1;
        Inventory inventory = minecraft.player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ContractData data = ContractStacks.read(inventory.getItem(slot));
            if (data != null && data.id().equals(contractId)) return slot;
        }
        return -1;
    }

    private int left() { return (width - PANEL_WIDTH) / 2; }

    private int top() { return (height - PANEL_HEIGHT) / 2; }

    private static String lower(Enum<?> value) { return value.name().toLowerCase(Locale.ROOT); }

    private static Component categoryName(ContractData.Objective objective) {
        return Component.translatable("moreminecrafts.contract.category." + lower(objective.category()));
    }

    private static Component targetName(ContractData.Objective objective) {
        Identifier id = Identifier.tryParse(objective.target());
        if (id == null) return Component.literal(objective.target());
        return switch (objective.category()) {
            case DELIVERY -> new ItemStack(BuiltInRegistries.ITEM.getValue(id)).getHoverName();
            case COMBAT -> BuiltInRegistries.ENTITY_TYPE.getValue(id).getDescription();
            case EXPLORATION -> Component.translatableWithFallback(
                    dev.itzgamesgd.moreminecrafts.bounty.ContractCatalog.structureNameKey(objective.target()), objective.target());
            default -> Component.literal(objective.target());
        };
    }

    private static Item itemOf(String id) {
        return BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
    }
}
