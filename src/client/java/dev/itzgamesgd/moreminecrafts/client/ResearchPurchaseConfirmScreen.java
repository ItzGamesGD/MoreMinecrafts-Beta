package dev.itzgamesgd.moreminecrafts.client;

import dev.itzgamesgd.moreminecrafts.progression.ProgressionScreenData;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Explicit second step before a client request is sent to the authoritative server. */
public final class ResearchPurchaseConfirmScreen extends Screen {
    private final ProgressionBrowseScreen parent;
    private final ProgressionScreenData.ResearchRow row;
    private boolean submitted;

    public ResearchPurchaseConfirmScreen(ProgressionBrowseScreen parent, ProgressionScreenData.ResearchRow row) {
        super(Component.translatable("moreminecrafts.ui.purchase.confirm.title"));
        this.parent = parent;
        this.row = row;
    }

    @Override protected void init() {
        int cx = width / 2;
        int cy = height / 2;
        addRenderableWidget(Button.builder(Component.translatable("moreminecrafts.ui.purchase.confirm"), b -> confirm())
                .bounds(cx - 108, cy + 86, 104, 24).build());
        addRenderableWidget(Button.builder(Component.translatable("moreminecrafts.ui.cancel"), b -> cancel())
                .bounds(cx + 4, cy + 86, 104, 24).build());
    }

    private void confirm() {
        if (submitted || row.nextCost() == null || !row.prerequisitesDefined()
                || row.availability() != dev.itzgamesgd.moreminecrafts.progression.ResearchPurchaseRules.Result.PURCHASED)
            return;
        submitted = true;
        ProgressionClientApi.purchase(row.id(), row.level() + 1);
        minecraft.gui.setScreen(parent);
    }

    private void cancel() {
        if (minecraft != null) minecraft.gui.setScreen(parent);
    }

    /** Test hook invokes the same button callback and retains the one-request guard. */
    public void validationConfirm() { confirm(); }
    public void validationCancel() { cancel(); }

    @Override public void onClose() { cancel(); }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        int w = Math.min(width - 40, 460), h = 250;
        int x = (width - w) / 2, y = (height - h) / 2;
        g.fill(x, y, x + w, y + h, 0xFF171717);
        g.fill(x + 3, y + 3, x + w - 3, y + h - 3, 0xFFD8D8D5);
        g.fill(x + 8, y + 8, x + w - 8, y + 38, 0xFFB6B6B2);
        g.text(font, Component.translatable("moreminecrafts.ui.purchase.confirm.title"), x + w / 2, y + 18, 0xFF161616, true);
        g.text(font, Component.translatable(row.titleKey()), x + 16, y + 52, 0xFF222222);
        g.text(font, Component.translatable("moreminecrafts.ui.purchase.confirm.level", row.level() + 1, row.maximumLevel()),
                x + 16, y + 71, 0xFF333333);
        if (row.nextCost() != null) {
            g.text(font, Component.translatable("moreminecrafts.ui.cost_xp", row.nextCost().rawXp()), x + 16, y + 91, 0xFF333333);
            int yy = y + 110;
            for (var cost : row.nextCost().materials()) {
                if (yy > y + h - 72) break;
                String itemName = Component.translatable("item." + cost.itemId().replace(':', '.')).getString();
                String costKey = cost.compressedUnits() > 0
                        ? "moreminecrafts.ui.cost_material_compressed" : "moreminecrafts.ui.cost_material";
                g.text(font, cost.compressedUnits() > 0
                        ? Component.translatable(costKey, itemName, cost.count(), cost.compressedUnits())
                        : Component.translatable(costKey, itemName, cost.count()), x + 16, yy, 0xFF424242);
                yy += 14;
            }
        }
        super.extractRenderState(g, mouseX, mouseY, delta);
    }
}
