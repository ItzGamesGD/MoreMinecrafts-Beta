package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/**
 * The enchanting and anvil skin (owner-approved 2026-11-27) is authored at 2 art px per GUI px. Every texture the
 * screens draw must exist with exactly twice the GUI size of the area it is drawn into, otherwise it would be
 * stretched. Panel frames keep an 8 GUI px (16 art px) thickness so they never overlap content that starts at 8.
 */
final class EnchantAnvilSkinResourceTest {
    private static final String DIR = "/assets/moreminecrafts/textures/gui/enchant_anvil/";

    private static Map<String, int[]> expected() {
        Map<String, int[]> m = new LinkedHashMap<>();
        for (String n : List.of("enchant_panel_frame", "enchant_panel_overlay")) m.put(n, new int[]{660, 736});
        for (String n : List.of("anvil_panel_frame", "anvil_panel_overlay")) m.put(n, new int[]{352, 332});
        for (String s : List.of("normal", "hover", "pressed", "selected")) {
            m.put("anvil_tab_112_" + s, new int[]{112, 40});
            m.put("anvil_tab_160_" + s, new int[]{160, 40});
            m.put("anvil_tab_224_" + s, new int[]{224, 40});
        }
        for (String s : List.of("normal", "hover", "pressed", "disabled")) {
            m.put("enhance_btn_" + s, new int[]{140, 40});
            m.put("reset_btn_" + s, new int[]{180, 36});
            m.put("apply_btn_" + s, new int[]{440, 36});
            m.put("arrow_left_" + s, new int[]{40, 36});
            m.put("arrow_right_" + s, new int[]{40, 36});
        }
        for (String s : List.of("normal", "hover", "pressed", "selected", "disabled")) m.put("option_row_" + s, new int[]{628, 40});
        for (String n : List.of("slot_normal", "slot_hover", "slot_disabled", "slot_equipment_normal", "slot_equipment_hover",
                "slot_book_normal", "slot_book_hover", "node_locked", "node_available", "node_owned", "node_selected"))
            m.put(n, new int[]{36, 36});
        m.put("cost_panel_a", new int[]{152, 74});
        m.put("cost_panel_b", new int[]{236, 34});
        for (String s : List.of("normal", "active")) {
            m.put("connector_h_" + s, new int[]{8, 2});
            m.put("connector_v_" + s, new int[]{2, 8});
        }
        m.put("divider", new int[]{628, 2});
        m.put("plus", new int[]{20, 20});
        m.put("output_arrow", new int[]{44, 30});
        m.put("output_error", new int[]{56, 42});
        return m;
    }

    private static BufferedImage read(String name) throws Exception {
        var stream = EnchantAnvilSkinResourceTest.class.getResourceAsStream(DIR + name + ".png");
        assertNotNull(stream, "missing skin texture " + name);
        try (stream) {
            return ImageIO.read(stream);
        }
    }

    @Test void everyTextureExistsWithTwiceTheGuiSize() throws Exception {
        for (var entry : expected().entrySet()) {
            BufferedImage image = read(entry.getKey());
            assertEquals(entry.getValue()[0], image.getWidth(), entry.getKey() + " width");
            assertEquals(entry.getValue()[1], image.getHeight(), entry.getKey() + " height");
            assertTrue(image.getColorModel().hasAlpha(), entry.getKey() + " keeps an alpha channel");
        }
    }

    @Test void statesOfOneControlShareTheSameSilhouette() throws Exception {
        Map<String, List<String>> families = new LinkedHashMap<>();
        for (String p : List.of("anvil_tab_112", "anvil_tab_160", "anvil_tab_224"))
            families.put(p, List.of("normal", "hover", "pressed", "selected"));
        for (String p : List.of("enhance_btn", "reset_btn", "apply_btn", "arrow_left", "arrow_right"))
            families.put(p, List.of("normal", "hover", "pressed", "disabled"));
        families.put("option_row", List.of("normal", "hover", "pressed", "selected", "disabled"));
        for (var family : families.entrySet()) {
            BufferedImage base = read(family.getKey() + "_" + family.getValue().get(0));
            for (String state : family.getValue()) {
                BufferedImage image = read(family.getKey() + "_" + state);
                for (int y = 0; y < base.getHeight(); y++)
                    for (int x = 0; x < base.getWidth(); x++)
                        assertEquals(base.getRGB(x, y) >>> 24, image.getRGB(x, y) >>> 24,
                                family.getKey() + "_" + state + " alpha at " + x + "," + y);
            }
        }
    }

    @Test void panelFramesAreFlatInsideTheEightGuiPixelBorder() throws Exception {
        for (String name : List.of("enchant_panel_frame", "anvil_panel_frame")) {
            BufferedImage image = read(name);
            int flat = 0xFF221538;
            for (int y = 16; y < image.getHeight() - 16; y++)
                for (int x = 16; x < image.getWidth() - 16; x++)
                    assertEquals(flat, image.getRGB(x, y), name + " centre at " + x + "," + y);
        }
    }
}
