package dev.itzgamesgd.moreminecrafts.client;

import dev.itzgamesgd.moreminecrafts.progression.ProgressionCanvasGeometry;
import dev.itzgamesgd.moreminecrafts.progression.ProgressionScreenData;
import dev.itzgamesgd.moreminecrafts.progression.ResearchPurchaseRules;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

/** Existing explicit confirmation step, responsive to compact GUI-scale viewports. */
public final class ResearchPurchaseConfirmScreen extends Screen {
    private final ProgressionBrowseScreen parent;
    private final ProgressionScreenData.ResearchRow row;
    private boolean submitted;
    private boolean compact;
    private int bodyScroll;

    public ResearchPurchaseConfirmScreen(ProgressionBrowseScreen parent, ProgressionScreenData.ResearchRow row) {
        super(Component.translatable("moreminecrafts.ui.purchase.confirm.title"));
        this.parent = parent;
        this.row = row;
    }

    @Override protected void init() {
        compact = ProgressionCanvasGeometry.responsiveLayout(width, height).compact();
        int confirmWidth = compact ? Math.min(132, (width - 28) / 2) : 104;
        int cancelWidth = compact ? Math.min(132, (width - 28) / 2) : 104;
        int y = compact ? height - 8 - 24 : height / 2 + 86;
        int gap = 8;
        int total = confirmWidth + cancelWidth + gap;
        int x = (width - total) / 2;
        addRenderableWidget(Button.builder(Component.translatable("moreminecrafts.ui.purchase.confirm"), b -> confirm())
                .bounds(x, y, confirmWidth, 24).build());
        addRenderableWidget(Button.builder(Component.translatable("moreminecrafts.ui.cancel"), b -> cancel())
                .bounds(x + confirmWidth + gap, y, cancelWidth, 24).build());
    }

    private void confirm() {
        if (submitted || row.nextCost() == null || !row.prerequisitesDefined()
                || row.availability() != ResearchPurchaseRules.Result.PURCHASED) return;
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
    public boolean validationCompact() { return compact; }

    @Override public void onClose() { cancel(); }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (compact) {
            bodyScroll = Math.max(0, Math.min(1200, bodyScroll + (int) Math.signum(verticalAmount) * 18));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override public boolean keyPressed(KeyEvent event) {
        if (compact && event.isUp()) { bodyScroll = Math.max(0, bodyScroll - 18); return true; }
        if (compact && event.isDown()) { bodyScroll = Math.min(1200, bodyScroll + 18); return true; }
        return super.keyPressed(event);
    }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int x = compact ? 8 : (width - Math.min(width - 40, 460)) / 2;
        int y = compact ? 8 : (height - 250) / 2;
        int w = compact ? width - 16 : Math.min(width - 40, 460);
        int h = compact ? height - 16 : 250;
        graphics.fill(0, 0, width, height, 0xFF272727);
        graphics.fill(x, y, x + w, y + h, 0xFF171717);
        graphics.fill(x + 3, y + 3, x + w - 3, y + h - 3, 0xFFD8D8D5);
        graphics.fill(x + 8, y + 8, x + w - 8, y + 36, 0xFFB6B6B2);
        graphics.centeredText(font, Component.translatable("moreminecrafts.ui.purchase.confirm.title"), x + w / 2, y + 17, 0xFF161616);
        int bodyTop = y + 44;
        int bodyBottom = compact ? y + h - 40 : y + h - 20;
        graphics.enableScissor(x + 8, bodyTop, x + w - 8, bodyBottom);
        try {
            int yy = bodyTop + 4 - bodyScroll;
            graphics.text(font, Component.translatable(row.titleKey()), x + 16, yy, 0xFF222222); yy += 21;
            graphics.text(font, Component.translatable("moreminecrafts.ui.purchase.confirm.level", row.level() + 1, row.maximumLevel()),
                    x + 16, yy, 0xFF333333); yy += 20;
            if (row.nextCost() != null) {
                graphics.text(font, Component.translatable("moreminecrafts.ui.cost_xp", row.nextCost().rawXp()), x + 16, yy, 0xFF333333);
                yy += 20;
                for (var cost : row.nextCost().materials()) {
                    String itemName = Component.translatable("item." + cost.itemId().replace(':', '.')).getString();
                    String key = cost.compressedUnits() > 0
                            ? "moreminecrafts.ui.cost_material_compressed" : "moreminecrafts.ui.cost_material";
                    Component text = cost.compressedUnits() > 0
                            ? Component.translatable(key, itemName, cost.count(), cost.compressedUnits())
                            : Component.translatable(key, itemName, cost.count());
                    graphics.text(font, text, x + 16, yy, 0xFF424242);
                    yy += 17;
                }
            }
        } finally {
            graphics.disableScissor();
        }
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }
}
