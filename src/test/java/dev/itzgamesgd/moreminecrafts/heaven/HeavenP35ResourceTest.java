package dev.itzgamesgd.moreminecrafts.heaven;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/** P3.5 static checks: giant rotation resources, isolated heaven jigsaw pools, G4 tag, texture-proposal isolation, authority record. */
class HeavenP35ResourceTest {
    private static final Path MAIN = Path.of("src/main/resources");
    private static final Path GT = Path.of("src/gametest/resources");
    private static final List<String> GIANTS = List.of("heaven_giant_stem", "heaven_giant_leaf", "heaven_giant_bloom");
    private static final String[] FACINGS = {"north", "east", "south", "west"};

    private static JsonObject json(Path p) throws IOException { return JsonParser.parseString(Files.readString(p)).getAsJsonObject(); }

    @Test
    void giantFamiliesRotateByReusingTheirModels() throws IOException {
        for (String f : GIANTS) {
            JsonObject t = json(MAIN.resolve("data/moreminecrafts/heaven/flora/" + f + ".json"));
            assertTrue(t.get("facing").getAsBoolean(), f + " has facing");
            int n = t.get("count").getAsInt();
            JsonObject v = json(MAIN.resolve("assets/moreminecrafts/blockstates/" + f + ".json")).getAsJsonObject("variants");
            assertEquals(n * 4, v.size(), f + " variants = parts x 4");
            for (int i = 0; i < n; i++) {
                String model = null;
                for (int k = 0; k < 4; k++) {
                    JsonObject e = v.getAsJsonObject("facing=" + FACINGS[k] + ",part=" + i);
                    assertNotNull(e, f + " part " + i + " " + FACINGS[k]);
                    assertEquals(90 * k, e.has("y") ? e.get("y").getAsInt() : 0, f + " y rotation follows facing (same convention as the medium flowers)");
                    if (model == null) model = e.get("model").getAsString(); else assertEquals(model, e.get("model").getAsString(), "one model per part");
                }
                assertEquals("moreminecrafts:block/heaven/" + f + "/p_%03d".formatted(i), model);
            }
        }
    }

    @Test
    void washedAwayTagListsExactlyTheSmallHeavenPlants() throws IOException {
        JsonObject j = json(MAIN.resolve("data/minecraft/tags/block/washed_away_by_fluids.json"));
        assertFalse(j.get("replace").getAsBoolean());
        Set<String> v = new HashSet<>(); j.getAsJsonArray("values").forEach(e -> v.add(e.getAsString()));
        Set<String> expected = new HashSet<>();
        for (String c : List.of("red", "yellow", "white", "pink", "blue", "violet")) expected.add("moreminecrafts:heaven_flower_" + c);
        expected.addAll(List.of("moreminecrafts:heaven_short_grass", "moreminecrafts:heaven_tall_grass", "moreminecrafts:heaven_petals"));
        assertEquals(expected, v, "G4: the nine small heaven plants, nothing else");

    }

    // ---------------------------------------------------------------- isolated heaven jigsaw pools

    private record Tpl(Map<List<Integer>, String> blocks, Map<List<Integer>, Map<String, Object>> nbt) {}

    @SuppressWarnings("unchecked")
    private static Tpl template(Path path) throws IOException {
        try (DataInputStream in = new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(Files.readAllBytes(path))))) {
            in.readByte(); in.readUTF();
            Map<String, Object> root = (Map<String, Object>) payload(in, 10);
            List<String> pal = new ArrayList<>();
            for (Object o : (List<Object>) root.get("palette")) pal.add(o.toString());
            Map<List<Integer>, String> b = new HashMap<>(); Map<List<Integer>, Map<String, Object>> n = new HashMap<>();
            for (Object o : (List<Object>) root.get("blocks")) {
                Map<String, Object> e = (Map<String, Object>) o; List<Integer> p = (List<Integer>) e.get("pos");
                b.put(p, pal.get((Integer) e.get("state")));
                if (e.get("nbt") != null) n.put(p, (Map<String, Object>) e.get("nbt"));
            }
            return new Tpl(b, n);
        }
    }

    private static Object payload(DataInputStream in, int type) throws IOException {
        switch (type) {
            case 1: return in.readByte();
            case 2: return in.readShort();
            case 3: return in.readInt();
            case 4: return in.readLong();
            case 5: return in.readFloat();
            case 6: return in.readDouble();
            case 7: { byte[] b = new byte[in.readInt()]; in.readFully(b); return b; }
            case 8: return in.readUTF();
            case 9: { int inner = in.readByte(), n = in.readInt(); List<Object> l = new ArrayList<>(n); for (int i = 0; i < n; i++) l.add(payload(in, inner)); return l; }
            case 10: { Map<String, Object> m = new TreeMap<>(); for (int t = in.readByte(); t != 0; t = in.readByte()) m.put(in.readUTF(), payload(in, t)); return m; }
            case 11: { int n = in.readInt(); List<Integer> l = new ArrayList<>(n); for (int i = 0; i < n; i++) l.add(in.readInt()); return l; }
            case 12: { int n = in.readInt(); long[] l = new long[n]; for (int i = 0; i < n; i++) l[i] = in.readLong(); return l; }
            default: throw new IOException("unknown NBT tag " + type);
        }
    }

    @Test
    void linkedTemplatesDifferFromP3OnlyInTheRetargetedPools() throws IOException {
        Map<String, String> retarget = Map.of("moreminecrafts:sky_islands/meadow/satellite", "moreminecrafts-gametest:heaven_link/meadow/satellite",
                "moreminecrafts:sky_islands/meadow/tower_top", "moreminecrafts-gametest:heaven_link/meadow/tower_top");
        Path dir = GT.resolve("data/moreminecrafts-gametest/structure/heaven_link/meadow");
        List<Path> linked;
        try (Stream<Path> s = Files.list(dir)) { linked = s.sorted().toList(); }
        assertEquals(10, linked.size(), "templates with satellite / tower_top links");
        for (Path p : linked) {
            Tpl h = template(MAIN.resolve("data/moreminecrafts/structure/heaven_p3/meadow/" + p.getFileName()));
            Tpl l = template(p);
            assertEquals(h.blocks(), l.blocks(), p + " blocks and palette identical");
            assertEquals(h.nbt().keySet(), l.nbt().keySet(), p + " jigsaw positions identical");
            for (var e : h.nbt().entrySet()) {
                Map<String, Object> a = new TreeMap<>(e.getValue()), b = new TreeMap<>(l.nbt().get(e.getKey()));
                String pool = (String) a.remove("pool"), lpool = (String) b.remove("pool");
                assertEquals(a, b, p + " jigsaw NBT apart from pool");
                assertEquals(retarget.getOrDefault(pool, pool), lpool, p + " pool retarget");
            }
        }
    }

    @Test
    void testPoolsAndTexturePacksStayOutOfTheProductionResources() throws IOException {
        try (Stream<Path> files = Files.walk(MAIN)) {
            for (Path f : files.filter(Files::isRegularFile).filter(x -> x.toString().endsWith(".json")).toList()) {
                String t = Files.readString(f);
                assertFalse(t.contains("heaven_link") || t.contains("moreminecrafts-gametest") || t.contains("heaven_tex_t"), "production file references test-only resources: " + f);
            }
        }
        assertFalse(Files.exists(MAIN.resolve("data/moreminecrafts/worldgen/template_pool/heaven_p3")), "no production heaven pool yet (G7: worldgen integration deferred)");
        for (int t = 1; t <= 3; t++) {
            Path pack = GT.resolve("resourcepacks/heaven_tex_t" + t);
            assertTrue(Files.exists(pack.resolve("pack.mcmeta")), "pack T" + t);
            try (Stream<Path> s = Files.walk(pack.resolve("assets"))) {
                for (Path png : s.filter(x -> x.toString().endsWith(".png")).toList()) {
                    String name = png.getFileName().toString();
                    Path prod = MAIN.resolve("assets/moreminecrafts/textures/block/heaven/flora/" + name);
                    assertTrue(Files.exists(prod), "proposal replaces an existing flora texture: " + name);
                    BufferedImage a = ImageIO.read(png.toFile()), b = ImageIO.read(prod.toFile());
                    assertEquals(16, a.getWidth()); assertEquals(16, a.getHeight());
                    double[] ma = mean(a), mb = mean(b);
                    for (int c = 0; c < 3; c++) assertEquals(mb[c], ma[c], Math.max(4.0, mb[c] * 0.03), name + " T" + t + " keeps the V5 mean colour");
                }
            }
        }
    }

    private static double[] mean(BufferedImage img) {
        double[] m = new double[3];
        for (int y = 0; y < img.getHeight(); y++) for (int x = 0; x < img.getWidth(); x++) {
            int rgb = img.getRGB(x, y); m[0] += (rgb >> 16) & 255; m[1] += (rgb >> 8) & 255; m[2] += rgb & 255;
        }
        for (int c = 0; c < 3; c++) m[c] /= img.getWidth() * img.getHeight();
        return m;
    }

    @Test
    void authorityRecordsTheApprovedAndDeferredHeavenDecisions() throws IOException {
        String a = Files.readString(Path.of("docs/design/SKY_ISLANDS_T2_CONTEXT_2026-10-27.md"));
        assertTrue(a.contains("## 24. 2026-11-21 오너 확정 — 천국 하늘섬"), "authority section 24");
        for (String k : List.of("| F8 |", "| G2 | **보류**", "| G9 | **보류**", "월드젠 통합은 보류", "모든 식물성 블록이 불연성인 것은 아니다"))
            assertTrue(a.contains(k), "authority records " + k);
        assertTrue(Files.readString(Path.of("docs/CONTENT_AUTHORITY_INDEX.md")).contains("SKY_ISLANDS_T2_CONTEXT_2026-10-27.md §24"), "index routes to section 24");
    }
}
