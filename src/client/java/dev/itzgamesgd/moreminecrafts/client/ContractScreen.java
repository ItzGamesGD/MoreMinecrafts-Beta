package dev.itzgamesgd.moreminecrafts.client;

import dev.itzgamesgd.moreminecrafts.bounty.ContractData;
import dev.itzgamesgd.moreminecrafts.bounty.ContractService;
import dev.itzgamesgd.moreminecrafts.bounty.ContractStacks;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.network.ContractActionPayload;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Single vertical contract screen: tier and status, objectives, live delivery holdings, fixed rewards, then
 * accept/complete/abandon. The contract is re-read from the live inventory every frame, and every action is
 * validated again by the server. The board is the approved "의뢰 계약서" UI kit, drawn at its layout coordinates
 * (canvas 344 x 294 GUI px); text is drawn as real, translatable text on top of the kit sprites.
 */
public final class ContractScreen extends Screen {
    /** Kit canvas size (layout.json: screen 344 x 294). */
    private static final int PANEL_WIDTH = 344;
    private static final int PANEL_HEIGHT = 294;
    /** Kit board (frame) placement inside the canvas. */
    private static final int BOARD_X = 12;
    private static final int BOARD_Y = 14;
    private static final int BOARD_WIDTH = 320;
    private static final int BOARD_HEIGHT = 263;

    private static final int TITLE_COLOR = 0xFF40231B;
    private static final int HEADING_COLOR = 0xFFE2B570;
    private static final int TEXT_COLOR = 0xFFFAE4BB;
    private static final int DONE_COLOR = 0xFF7BD389;
    private static final int SHORT_COLOR = 0xFFFF7A7A;
    private static final int WARNING_COLOR = 0xFFFF7A7A;
    private static final int MAX_HOLDINGS = 3;
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
        int row = top() + 266;
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
        int centerX = width / 2;
        Map<String, Integer> holdings = ContractService.quantities(minecraft.player.getInventory());
        boolean ready = data.ready(holdings);

        graphics.fill(0, 0, width, height, 0x99000000);

        // Board and title banner from the kit.
        sprite(graphics, "frame", left + BOARD_X, top + BOARD_Y, BOARD_WIDTH, BOARD_HEIGHT);
        sprite(graphics, "title_banner", left + 59, top + 18, 226, 60);
        sprite(graphics, "wax_seal", left + 270, top + 4, 55, 78);
        graphics.centeredText(font, Component.translatable("moreminecrafts.contract.tier." + lower(data.tier())),
                left + 172, top + 30, TITLE_COLOR);
        graphics.centeredText(font, Component.translatable("moreminecrafts.contract.status." + lower(data.status())),
                left + 172, top + 50, TITLE_COLOR);

        // Objectives panel (kit: 35, 81, 274 x 72).
        sprite(graphics, "objectives_panel", left + 35, top + 81, 274, 72);
        heading(graphics, "moreminecrafts.contract.section.objectives", left, top, 87);
        int x = left + 52;
        int y = top + 106;
        for (ContractData.Objective objective : data.objectives()) {
            Component label = objective.category() == ContractData.Category.DELIVERY
                    ? Component.translatable("moreminecrafts.contract.objective_need", categoryName(objective),
                    targetName(objective), objective.required())
                    : Component.translatable("moreminecrafts.contract.objective_progress", categoryName(objective),
                    targetName(objective), objective.progress(), objective.required());
            int labelColor = objective.progress() >= objective.required() ? DONE_COLOR : TEXT_COLOR;
            for (var part : font.split(label, 228)) {
                graphics.text(font, part, x, y, labelColor);
                y += font.lineHeight + 3;
            }
        }

        // Holdings panel (kit: 35, 159, 274 x 44): one socket per delivery objective.
        sprite(graphics, "holdings_panel", left + 35, top + 159, 274, 44);
        heading(graphics, "moreminecrafts.contract.section.holdings", left, top, 163);
        int slot = 0;
        for (ContractData.Objective objective : data.objectives()) {
            if (objective.category() != ContractData.Category.DELIVERY || slot >= MAX_HOLDINGS) continue;
            int socketX = left + 54 + slot * 83;
            int have = holdings.getOrDefault(objective.target(), 0);
            int holdingColor = have >= objective.required() ? DONE_COLOR : SHORT_COLOR;
            sprite(graphics, "item_socket", socketX, top + 177, 23, 23);
            graphics.item(new ItemStack(itemOf(objective.target())), socketX + 3, top + 179);
            graphics.text(font, Component.literal(have + "/" + objective.required()), socketX + 28, top + 181,
                    holdingColor);
            slot++;
        }

        // Rewards panel (kit: 35, 209, 274 x 54).
        sprite(graphics, "rewards_panel", left + 35, top + 209, 274, 54);
        heading(graphics, "moreminecrafts.contract.section.rewards", left, top, 213);
        sprite(graphics, "reward_socket", left + 54, top + 227, 34, 32);
        ItemStack rewardStack = new ItemStack(itemOf(data.reward().item()));
        graphics.item(rewardStack, left + 57, top + 230);
        int rewardX = left + 98;
        graphics.text(font, Component.translatable("moreminecrafts.contract.reward.emeralds", data.reward().emeralds()),
                rewardX, top + 226, TEXT_COLOR);
        graphics.text(font, Component.translatable("moreminecrafts.contract.reward.item",
                rewardStack.getHoverName(), data.reward().count()), rewardX, top + 237, TEXT_COLOR);

        if (confirmingAbandon)
            graphics.centeredText(font, Component.translatable("moreminecrafts.contract.abandon_warning"),
                    centerX, top + 252, WARNING_COLOR);

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

    /** Heading text centred on the kit's paper rule pair (rules at x 54 and 202, 88 x 5). */
    private void heading(GuiGraphicsExtractor graphics, String key, int left, int top, int y) {
        int ruleY = top + y + 4;
        String ruleDark = y >= 163 ? "heading_rule_dark" : "heading_rule_paper";
        sprite(graphics, ruleDark, left + 54, ruleY, 88, 5);
        sprite(graphics, ruleDark + "_r", left + 202, ruleY, 88, 5);
        graphics.centeredText(font, Component.translatable(key), left + 172, top + y, HEADING_COLOR);
    }

    private static void sprite(GuiGraphicsExtractor graphics, String name, int x, int y, int width, int height) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ModIds.id("contract/" + name), x, y, width, height);
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
