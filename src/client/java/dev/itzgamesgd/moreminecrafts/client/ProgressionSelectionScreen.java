package dev.itzgamesgd.moreminecrafts.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.network.chat.Component;

/** Provisional functional selector for the approved three progression destinations. */
public final class ProgressionSelectionScreen extends Screen {
    public ProgressionSelectionScreen() { super(Component.literal("MoreMinecrafts")); }

    @Override protected void init() {
        int x = (width - 190) / 2;
        int y = height / 2 - 48;
        addRenderableWidget(Button.builder(Component.literal("발전 과제"), b -> {
            if (minecraft != null && minecraft.player != null)
                minecraft.gui.setScreen(new AdvancementsScreen(minecraft.player.connection.getAdvancements()));
        }).bounds(x, y, 190, 20).build());
        addRenderableWidget(Button.builder(Component.literal("지식"), b ->
                minecraft.gui.setScreen(new ProgressionBrowseScreen(ProgressionBrowseScreen.Mode.KNOWLEDGE))).bounds(x, y + 26, 190, 20).build());
        addRenderableWidget(Button.builder(Component.literal("연구"), b ->
                minecraft.gui.setScreen(new ProgressionBrowseScreen(ProgressionBrowseScreen.Mode.RESEARCH))).bounds(x, y + 52, 190, 20).build());
    }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int x = (width - 220) / 2, y = height / 2 - 82;
        graphics.fill(x, y, x + 220, y + 152, 0xEE172129);
        graphics.text(font, Component.literal("MoreMinecrafts"), x + 8, y + 8, 0xFFFFFFFF);
        graphics.text(font, Component.literal("Progression access (provisional)"), x + 8, y + 22, 0xFFB8C8D0);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }
}
