package dev.itzgamesgd.moreminecrafts.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.network.chat.Component;

/** Four-card entry point. The fourth destination is intentionally visible but disabled. */
public final class ProgressionSelectionScreen extends Screen {
    private static final int[] ACCENTS = {0xFF78945B, 0xFF579CC3, 0xFF9A65C3, 0xFF626262};
    private static final String[] TITLES = {
            "moreminecrafts.ui.selector.advancements", "moreminecrafts.ui.selector.knowledge",
            "moreminecrafts.ui.selector.research", "moreminecrafts.ui.selector.quests"};
    private static final String[] DESCRIPTIONS = {
            "moreminecrafts.ui.selector.advancements.description", "moreminecrafts.ui.selector.knowledge.description",
            "moreminecrafts.ui.selector.research.description", "moreminecrafts.ui.selector.quests.description"};

    public ProgressionSelectionScreen() { super(Component.translatable("moreminecrafts.ui.selector.title")); }

    @Override protected void init() {
        int panelWidth = Math.min(width - 28, 1100);
        int panelHeight = Math.min(height - 24, 610);
        int panelX = (width - panelWidth) / 2;
        int panelY = (height - panelHeight) / 2;
        int gap = Math.max(8, panelWidth / 110);
        int cardsWidth = panelWidth - 36;
        int cardWidth = (cardsWidth - gap * 3) / 4;
        int cardY = panelY + 58;
        int cardHeight = panelHeight - 128;
        for (int index = 0; index < 4; index++) {
            final int card = index;
            int x = panelX + 18 + index * (cardWidth + gap);
            int buttonY = cardY + cardHeight * 52 / 100;
            Button button = Button.builder(Component.translatable(TITLES[index]), b -> open(card))
                    .bounds(x + 8, buttonY, cardWidth - 16, 34).build();
            button.active = index != 3;
            addRenderableWidget(button);
        }
        addRenderableWidget(Button.builder(Component.translatable("moreminecrafts.ui.close"), b -> onClose())
                .bounds(panelX + panelWidth / 2 - 70, panelY + panelHeight - 48, 140, 28).build());
    }

    private void open(int card) {
        if (minecraft == null || minecraft.player == null) return;
        switch (card) {
            case 0 -> minecraft.gui.setScreen(new AdvancementsScreen(minecraft.player.connection.getAdvancements()));
            case 1 -> minecraft.gui.setScreen(new ProgressionBrowseScreen(ProgressionBrowseScreen.Mode.KNOWLEDGE));
            case 2 -> minecraft.gui.setScreen(new ProgressionBrowseScreen(ProgressionBrowseScreen.Mode.RESEARCH));
            default -> { /* XP Quest is a disabled future destination. */ }
        }
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        int pw = Math.min(width - 28, 1100), ph = Math.min(height - 24, 610);
        int x = (width - pw) / 2, y = (height - ph) / 2;
        int gap = Math.max(8, pw / 110), cw = (pw - 36 - gap * 3) / 4;
        int cy = y + 58, ch = ph - 128;
        frame(g, x, y, pw, ph, 0xFFE1E1DE);
        g.fill(x + 7, y + 7, x + pw - 7, y + 52, 0xFFCACAC6);
        g.text(font, Component.translatable("moreminecrafts.ui.selector.title"), x + pw / 2,
                y + 11, 0xFF151515, true);
        g.text(font, Component.translatable("moreminecrafts.ui.selector.subtitle"), x + pw / 2,
                y + 33, 0xFF303030, true);
        g.fill(x + 12, cy - 9, x + pw - 12, cy + ch + 7, 0xFF171717);
        for (int i = 0; i < 4; i++) {
            int cx = x + 18 + i * (cw + gap);
            int accent = ACCENTS[i];
            int fill = i == 3 ? 0xFF262626 : (i == 0 ? 0xFF183024 : i == 1 ? 0xFF172B38 : 0xFF281D35);
            cardFrame(g, cx, cy, cw, ch, accent, fill);
            drawIcon(g, i, cx + cw / 2, cy + ch * 28 / 100);
            drawWrapped(g, Component.translatable(DESCRIPTIONS[i]), cx + cw / 2, cy + ch * 70 / 100,
                    cw - 24, i == 3 ? 0xFF9A9A9A : 0xFFE5E5E5, 3);
            if (i == 3) g.text(font, Component.translatable("moreminecrafts.ui.selector.coming_soon"),
                    cx + cw / 2, cy + ch - 12, 0xFF929292, true);
        }
        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    private static void frame(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + h, 0xFF202020);
        g.fill(x + 3, y + 3, x + w - 3, y + h - 5, color);
        g.fill(x + 8, y + 8, x + w - 8, y + 12, 0xFFB7B7B3);
    }

    private static void cardFrame(GuiGraphicsExtractor g, int x, int y, int w, int h, int accent, int fill) {
        g.fill(x, y, x + w, y + h, 0xFF080808);
        g.fill(x + 3, y + 3, x + w - 3, y + h - 3, accent);
        g.fill(x + 7, y + 7, x + w - 7, y + h - 7, fill);
        g.fill(x + 9, y + 9, x + w - 9, y + 12, accent);
    }

    private void drawWrapped(GuiGraphicsExtractor g, Component text, int centerX, int y, int maxWidth, int color, int maxLines) {
        String[] words = text.getString().split(" ");
        String line = "";
        int lines = 0;
        for (String word : words) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && font.width(Component.literal(candidate)) > maxWidth) {
                g.text(font, Component.literal(line), centerX, y + lines * 10, color, true);
                lines++;
                line = word;
                if (lines >= maxLines) return;
            } else line = candidate;
        }
        if (!line.isEmpty() && lines < maxLines) g.text(font, Component.literal(line), centerX, y + lines * 10, color, true);
    }

    /** Pixel-sized vanilla-inspired symbols keep the cards legible without external art assets. */
    private static void drawIcon(GuiGraphicsExtractor g, int type, int centerX, int centerY) {
        int x = centerX - 43, y = centerY - 37;
        if (type == 0) {
            g.fill(x + 8, y + 2, x + 78, y + 45, 0xFF6C91AA);
            g.fill(x + 4, y + 39, x + 82, y + 66, 0xFF42633C);
            g.fill(x + 19, y + 49, x + 67, y + 76, 0xFF795136);
            g.fill(x + 19, y + 45, x + 67, y + 54, 0xFF78A34A);
        } else if (type == 1) {
            g.fill(x + 5, y + 16, x + 42, y + 63, 0xFFD7C18B);
            g.fill(x + 44, y + 16, x + 81, y + 63, 0xFFF1E6C5);
            g.fill(x + 41, y + 18, x + 45, y + 63, 0xFF54402A);
            for (int row = 0; row < 4; row++) {
                g.fill(x + 12, y + 25 + row * 8, x + 36, y + 27 + row * 8, 0xFF8C826D);
                g.fill(x + 51, y + 25 + row * 8, x + 75, y + 27 + row * 8, 0xFF8C826D);
            }
        } else if (type == 2) {
            g.fill(x + 18, y + 9, x + 66, y + 30, 0xFF9C7750);
            g.fill(x + 12, y + 27, x + 72, y + 71, 0xFF76553A);
            g.fill(x + 20, y + 34, x + 32, y + 48, 0xFFD6BF80);
            g.fill(x + 48, y + 34, x + 60, y + 48, 0xFFD6BF80);
            g.fill(x + 37, y + 45, x + 47, y + 69, 0xFF352719);
            g.fill(x + 18, y + 31, x + 66, y + 34, 0xFF40302A);
        } else {
            g.fill(x + 16, y + 17, x + 70, y + 71, 0xFF505050);
            g.fill(x + 23, y + 24, x + 63, y + 64, 0xFF292929);
            g.fill(x + 31, y + 42, x + 55, y + 64, 0xFFAEAEAE);
            g.fill(x + 35, y + 32, x + 51, y + 46, 0xFFAEAEAE);
            g.fill(x + 39, y + 53, x + 47, y + 59, 0xFF303030);
        }
    }
}
