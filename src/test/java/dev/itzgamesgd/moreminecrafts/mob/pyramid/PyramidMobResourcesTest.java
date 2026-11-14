package dev.itzgamesgd.moreminecrafts.mob.pyramid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/** Texture, loot, language and tag resources of the four ST-01 pyramid variants. */
class PyramidMobResourcesTest {
    private static final Path TEXTURES = Path.of("src/main/resources/assets/moreminecrafts/textures/entity/pyramid");
    private static final Path DATA = Path.of("src/main/resources/data");
    private static final List<String> IDS = List.of("buried_mummy", "crypt_archer", "funeral_servant", "gilded_mummy");
    private static final List<String> BABY_IDS = List.of("buried_mummy", "funeral_servant", "gilded_mummy");

    /** u, v, w, h, d of each UV-owning box of a vanilla 26.3 layer; base boxes first, then overlay boxes. */
    private static final int[][] ADULT_BASE = {{0, 0, 8, 8, 8}, {16, 16, 8, 12, 4}, {40, 16, 4, 12, 4}, {0, 16, 4, 12, 4}};
    private static final int[][] ADULT_OUTER = {{32, 0, 8, 8, 8}};
    private static final int[][] BABY_BASE = {{3, 3, 6, 6, 6}, {16, 16, 4, 5, 2}, {36, 16, 2, 5, 2}, {28, 16, 2, 5, 2},
            {8, 16, 2, 4, 2}, {0, 16, 2, 4, 2}};
    private static final int[][] BABY_OUTER = {{35, 3, 6, 6, 6}};

    private static Set<Long> cover(int[][] boxes) {
        Set<Long> out = new HashSet<>();
        for (int[] b : boxes) {
            int u = b[0], v = b[1], w = b[2], h = b[3], d = b[4];
            add(out, u + d, v, 2 * w, d);
            add(out, u, v + d, 2 * (w + d), h);
        }
        return out;
    }

    private static void add(Set<Long> out, int x0, int y0, int w, int h) {
        for (int x = x0; x < x0 + w; x++) for (int y = y0; y < y0 + h; y++) out.add(((long) x << 32) | y);
    }

    private static BufferedImage read(String name) throws Exception {
        Path p = TEXTURES.resolve(name + ".png");
        assertTrue(Files.isRegularFile(p), name);
        return ImageIO.read(p.toFile());
    }

    private static void checkLayout(String name, BufferedImage img, int[][] base, int[][] outer, boolean opaqueBase) {
        Set<Long> baseCover = cover(base);
        Set<Long> all = new HashSet<>(baseCover);
        all.addAll(cover(outer));
        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                int alpha = img.getRGB(x, y) >>> 24;
                long key = ((long) x << 32) | y;
                if (!all.contains(key)) {
                    assertEquals(0, alpha, name + ": pixel outside the vanilla UV layout at " + x + "," + y);
                }
                if (opaqueBase && baseCover.contains(key)) {
                    assertEquals(255, alpha, name + ": hole in an opaque base face at " + x + "," + y);
                }
            }
        }
    }

    @Test
    void adultTexturesFollowTheVanillaLayouts() throws Exception {
        for (String id : List.of("buried_mummy", "funeral_servant", "gilded_mummy")) {
            BufferedImage img = read(id);
            assertEquals(64, img.getWidth(), id);
            assertEquals(64, img.getHeight(), id);
            checkLayout(id, img, ADULT_BASE, ADULT_OUTER, true);
        }
        BufferedImage archer = read("crypt_archer");
        assertEquals(64, archer.getWidth());
        assertEquals(32, archer.getHeight());
        // skeleton: thin limbs; the ribcage is deliberately cut out like the vanilla skeleton body
        checkLayout("crypt_archer", archer, new int[][]{{0, 0, 8, 8, 8}, {16, 16, 8, 12, 4}, {40, 16, 2, 12, 2}, {0, 16, 2, 12, 2}},
                ADULT_OUTER, false);
        int skullOpaque = 0;
        for (int x = 8; x < 16; x++) for (int y = 8; y < 16; y++) if ((archer.getRGB(x, y) >>> 24) == 255) skullOpaque++;
        assertEquals(64, skullOpaque, "crypt_archer skull face is opaque");
    }

    @Test
    void babyTexturesFollowTheBabyZombieLayout() throws Exception {
        for (String id : BABY_IDS) {
            BufferedImage img = read(id + "_baby");
            assertEquals(64, img.getWidth(), id);
            assertEquals(64, img.getHeight(), id);
            checkLayout(id + "_baby", img, BABY_BASE, BABY_OUTER, true);
        }
        assertFalse(Files.exists(TEXTURES.resolve("crypt_archer_baby.png")), "skeletons have no baby form");
    }

    @Test
    void everyTextureIsDistinct() throws Exception {
        List<String> names = new ArrayList<>(IDS);
        BABY_IDS.forEach(id -> names.add(id + "_baby"));
        for (int a = 0; a < names.size(); a++) {
            for (int b = a + 1; b < names.size(); b++) {
                assertFalse(Files.mismatch(TEXTURES.resolve(names.get(a) + ".png"), TEXTURES.resolve(names.get(b) + ".png")) == -1,
                        names.get(a) + " vs " + names.get(b));
            }
        }
    }

    @Test
    void lootTablesAliasTheVanillaBaseFamily() throws Exception {
        Map<String, String> bases = Map.of("buried_mummy", "husk", "crypt_archer", "skeleton",
                "funeral_servant", "zombie", "gilded_mummy", "husk");
        for (var e : bases.entrySet()) {
            String json = Files.readString(DATA.resolve("moreminecrafts/loot_table/entities/" + e.getKey() + ".json"));
            assertTrue(json.contains("\"minecraft:entities/" + e.getValue() + "\""), e.getKey());
        }
    }

    @Test
    void tagsMatchTheVanillaBaseFamily() throws Exception {
        Path tags = DATA.resolve("minecraft/tags/entity_type");
        Map<String, List<String>> expected = Map.of(
                "zombies", List.of("buried_mummy", "funeral_servant", "gilded_mummy"),
                "skeletons", List.of("crypt_archer"),
                "burn_in_daylight", List.of("crypt_archer", "funeral_servant"),
                "no_anger_from_wind_charge", IDS);
        for (var e : expected.entrySet()) {
            String json = Files.readString(tags.resolve(e.getKey() + ".json"));
            for (String id : IDS) {
                assertEquals(e.getValue().contains(id), json.contains("\"moreminecrafts:" + id + "\""), e.getKey() + " " + id);
            }
        }
    }

    @Test
    void namesAreTranslated() throws Exception {
        String en = Files.readString(Path.of("src/main/resources/assets/moreminecrafts/lang/en_us.json"));
        String ko = Files.readString(Path.of("src/main/resources/assets/moreminecrafts/lang/ko_kr.json"));
        for (String id : IDS) {
            assertTrue(en.contains("\"entity.moreminecrafts." + id + "\""), id);
            assertTrue(ko.contains("\"entity.moreminecrafts." + id + "\""), id);
        }
    }
}
