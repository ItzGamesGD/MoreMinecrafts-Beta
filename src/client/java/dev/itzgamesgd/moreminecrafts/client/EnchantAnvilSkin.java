package dev.itzgamesgd.moreminecrafts.client;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Shared drawing for the enchanting and anvil skin (owner-approved 2026-11-27). The textures under
 * {@code textures/gui/enchant_anvil/} are authored at 2 art px per GUI px, so every draw maps a w x h GUI px area
 * to a 2w x 2h art px texture. Layout, slot coordinates and logic stay with the screens.
 */
final class EnchantAnvilSkin {
    static final String DIR = "textures/gui/enchant_anvil/";
    static final int TEXT = 0xFFE6DAF2;
    static final int TEXT_DIM = 0xFFB9A6D4;
    static final int TEXT_GOLD = 0xFFF6CA5C;
    static final int TEXT_OFF = 0xFF766C74;
    static final int TEXT_BAD = 0xFFFF7A7A;

    private EnchantAnvilSkin() {}

    static Identifier tex(String name) { return ModIds.id(DIR + name + ".png"); }

    /** Draws a whole texture (2 art px per GUI px) into a w x h GUI px area. */
    static void blit(GuiGraphicsExtractor g, String name, int x, int y, int w, int h) {
        g.blit(RenderPipelines.GUI_TEXTURED, tex(name), x, y, 0, 0, w, h, w * 2, h * 2, w * 2, h * 2);
    }

    /** A w x h GUI px piece taken from the top-left of a repeating tile (tileW x tileH art px). */
    private static void blitTile(GuiGraphicsExtractor g, String name, int x, int y, int w, int h, int tileW, int tileH) {
        g.blit(RenderPipelines.GUI_TEXTURED, tex(name), x, y, 0, 0, w, h, w * 2, h * 2, tileW, tileH);
    }

    /** A 1 GUI px high connector made of 4 GUI px tiles. */
    static void horizontal(GuiGraphicsExtractor g, String name, int x, int y, int length) {
        for (int dx = 0; dx < length; dx += 4) blitTile(g, name, x + dx, y, Math.min(4, length - dx), 1, 8, 2);
    }

    /** A 1 GUI px wide connector made of 4 GUI px tiles. */
    static void vertical(GuiGraphicsExtractor g, String name, int x, int y, int length) {
        for (int dy = 0; dy < length; dy += 4) blitTile(g, name, x, y + dy, 1, Math.min(4, length - dy), 2, 8);
    }
}
