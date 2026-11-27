package dev.itzgamesgd.moreminecrafts.client;

import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * Button drawn with the enchanting/anvil skin. Every state texture shares one silhouette, so a hover or press never
 * moves the control. {@code Look.inactive} names the state used when the button is inactive: tabs show their
 * {@code selected} look (the selected tab is the inactive one), other buttons show {@code disabled}.
 */
final class EnchantSkinButton extends Button {
    record Look(String prefix, String inactive, int sliceTexW, int sliceTexH, int sliceCap, boolean label) {
        static Look plain(String prefix, String inactive) { return new Look(prefix, inactive, 0, 0, 0, true); }

        /** A look whose texture already carries the glyph (page arrows): the message is narrated but not drawn. */
        static Look glyph(String prefix) { return new Look(prefix, "disabled", 0, 0, 0, false); }

        /** Horizontally three-sliced look; {@code capArt} is the fixed cap width in art px on each side. */
        static Look sliced(String prefix, String inactive, int texW, int texH, int capArt) {
            return new Look(prefix, inactive, texW, texH, capArt, true);
        }

        boolean isSliced() { return sliceTexW > 0; }
    }

    private final Look look;
    private BooleanSupplier selectedWhen = () -> false;
    private String forcedState;

    EnchantSkinButton(int x, int y, int width, int height, Component message, OnPress onPress, Look look) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.look = look;
    }

    EnchantSkinButton selectedWhen(BooleanSupplier selected) {
        this.selectedWhen = selected;
        return this;
    }

    /** For nodes: the screen decides the base state; hovering an active node shows the selected look. */
    void forcedState(String state) { this.forcedState = state; }

    private String state() {
        if (forcedState != null) return active && isHoveredOrFocused() ? "selected" : forcedState;
        if (selectedWhen.getAsBoolean()) return "selected";
        if (!active) return look.inactive();
        if (isHovered() && Minecraft.getInstance().mouseHandler.isLeftPressed()) return "pressed";
        return isHoveredOrFocused() ? "hover" : "normal";
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        String state = state();
        String name = look.prefix() + "_" + state;
        if (look.isSliced()) {
            ResearchSheetScene.nineSlice(graphics, EnchantAnvilSkin.tex(name), look.sliceTexW(), look.sliceTexH(),
                    look.sliceCap(), look.sliceCap() / 2, getX(), getY(), getWidth(), getHeight(), false);
        } else {
            EnchantAnvilSkin.blit(graphics, name, getX(), getY(), getWidth(), getHeight());
        }
        String label = getMessage().getString();
        if (label.isEmpty() || !look.label()) return;
        var font = Minecraft.getInstance().font;
        int room = getWidth() - 8;
        if (font.width(label) > room) label = font.plainSubstrByWidth(label, Math.max(0, room - font.width("…"))) + "…";
        boolean lit = selectedWhen.getAsBoolean() || state.equals("selected") || state.equals("hover");
        int color = !active && !state.equals("selected") ? EnchantAnvilSkin.TEXT_OFF
                : lit ? EnchantAnvilSkin.TEXT_GOLD : EnchantAnvilSkin.TEXT;
        graphics.text(font, label, getX() + (getWidth() - font.width(label)) / 2, getY() + (getHeight() - 8) / 2, color, true);
    }
}
