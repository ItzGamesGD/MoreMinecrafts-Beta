package dev.itzgamesgd.moreminecrafts.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * Button drawn with the owner-approved research kit (normal / hover / pressed / disabled).
 * {@code kind} is the kit base name: back (with arrow), plain (back plate without the arrow), zoom_in, zoom_out, close.
 */
final class ResearchSheetButton extends Button {
    private final String kind;

    ResearchSheetButton(int x, int y, int width, int height, Component message, String kind, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.kind = kind;
    }

    private String state() {
        if (!active) return "disabled";
        if (isHovered() && Minecraft.getInstance().mouseHandler.isLeftPressed()) return "pressed";
        return isHoveredOrFocused() ? "hover" : "normal";
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        String state = state();
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        if (kind.equals("back") || kind.equals("plain")) {
            var tex = ResearchSheetScene.kit("button_back_" + state);
            // 160x40 art plate; the left 32 art px hold the arrow, so a plain plate reuses the right-hand border.
            int leftU = kind.equals("back") ? 0 : 152;
            int leftArt = kind.equals("back") ? 32 : 8;
            int leftGui = leftArt / 2;
            graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, tex, x, y, leftU, 0,
                    leftGui, h, leftArt, 40, 160, 40);
            graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, tex, x + leftGui, y, 40, 0,
                    Math.max(0, w - leftGui - 4), h, 80, 40, 160, 40);
            graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, tex, x + w - 4, y, 152, 0,
                    4, h, 8, 40, 160, 40);
            int textLeft = x + leftGui + 2;
            int color = active ? (state.equals("hover") ? 0xFFFFEAA6 : 0xFFECE0C4) : 0xFF7A7682;
            var font = Minecraft.getInstance().font;
            String label = getMessage().getString();
            int room = w - leftGui - 8;
            if (font.width(label) > room) label = font.plainSubstrByWidth(label, Math.max(0, room - font.width("…"))) + "…";
            graphics.text(font, label, textLeft + Math.max(0, (room - font.width(label)) / 2), y + (h - 8) / 2, color, true);
        } else {
            graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, ResearchSheetScene.kit("button_" + kind + "_" + state),
                    x, y, 0, 0, w, h, 40, 40, 40, 40);
        }
    }
}
