package dev.itzgamesgd.moreminecrafts.heaven;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import org.junit.jupiter.api.Test;

/** Heaven sky island P2: reusable vegetation resources and the separate P2 island (resources only, no visuals). */
class HeavenP2ResourceTest {
    private static final String NS = "moreminecrafts";
    private static final String ORIGINAL = "data/moreminecrafts/structure/sky_islands/meadow/plain_medium_d.nbt";
    private static final String P1 = "data/moreminecrafts/structure/heaven_prototype/plain_medium_d_v5.nbt";
    private static final String P2 = "data/moreminecrafts/structure/heaven_prototype/plain_medium_d_v5_p2.nbt";
    private static final List<String> FAMILIES = List.of(
            "heaven_giant_stem", "heaven_giant_leaf", "heaven_giant_bloom", "heaven_lavender", "heaven_hydrangea", "heaven_phlox",
            "heaven_flower_red", "heaven_flower_yellow", "heaven_flower_white", "heaven_flower_pink", "heaven_flower_blue", "heaven_flower_violet",
            "heaven_short_grass", "heaven_tall_grass", "heaven_vine", "heaven_wisteria", "heaven_petals");
    private static final Set<String> NATURAL = Set.of(NS + ":heaven_grass_block", NS + ":heaven_loam", NS + ":heaven_cream_stone", NS + ":heaven_cream_stone_pale");
    private static final Set<String> VANILLA_REUSED = Set.of("minecraft:oak_log", "minecraft:cherry_leaves", "minecraft:water");
    private static final Set<String> GIANT = Set.of(NS + ":heaven_giant_stem", NS + ":heaven_giant_leaf", NS + ":heaven_giant_bloom");

    private static byte[] bytes(String path) throws IOException {
        try (InputStream in = HeavenP2ResourceTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(in, "missing resource " + path);
            return in.readAllBytes();
        }
    }

    private static boolean exists(String path) { return HeavenP2ResourceTest.class.getClassLoader().getResource(path) != null; }

    private static JsonObject json(String path) throws IOException {
        return JsonParser.parseString(new String(bytes(path), StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private record State(String id, Map<String, String> props) {}
    private record Pos(int x, int y, int z) {}
    private record Template(int[] size, Map<Pos, State> blocks, int dataVersion) {}

    @SuppressWarnings("unchecked")
    private static Template template(String path) throws IOException {
        try (DataInputStream in = new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(bytes(path))))) {
            assertEquals(10, in.readByte()); in.readUTF();
            Map<String, Object> root = (Map<String, Object>) payload(in, 10);
            List<Integer> size = (List<Integer>) root.get("size");
            List<State> palette = new ArrayList<>();
            for (Object o : (List<Object>) root.get("palette")) {
                Map<String, Object> s = (Map<String, Object>) o; Map<String, String> props = new TreeMap<>();
                if (s.get("properties") != null) ((Map<String, Object>) s.get("properties")).forEach((k, v) -> props.put(k, String.valueOf(v)));
                palette.add(new State((String) s.get("id"), props));
            }
            Map<Pos, State> blocks = new HashMap<>();
            for (Object o : (List<Object>) root.get("blocks")) {
                Map<String, Object> e = (Map<String, Object>) o; List<Integer> p = (List<Integer>) e.get("pos");
                assertNull(blocks.put(new Pos(p.get(0), p.get(1), p.get(2)), palette.get((Integer) e.get("state"))), "duplicate position " + p);
            }
            return new Template(new int[]{size.get(0), size.get(1), size.get(2)}, blocks, (Integer) root.get("DataVersion"));
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
            case 10: { Map<String, Object> m = new LinkedHashMap<>(); for (int t = in.readByte(); t != 0; t = in.readByte()) m.put(in.readUTF(), payload(in, t)); return m; }
            case 11: { int n = in.readInt(); List<Integer> l = new ArrayList<>(n); for (int i = 0; i < n; i++) l.add(in.readInt()); return l; }
            case 12: { int n = in.readInt(); long[] l = new long[n]; for (int i = 0; i < n; i++) l[i] = in.readLong(); return l; }
            default: throw new IOException("unknown NBT tag " + type);
        }
    }

    private static JsonObject table(String family) throws IOException { return json("data/moreminecrafts/heaven/flora/" + family + ".json"); }

    @Test
    void everyFamilyHasCompleteResources() throws IOException {
        Set<String> textures = new HashSet<>();
        int models = 0;
        for (String f : FAMILIES) {
            JsonObject t = table(f);
            int count = t.get("count").getAsInt(); boolean facing = t.get("facing").getAsBoolean();
            assertEquals(count, t.getAsJsonArray("parts").size(), f + " table size");
            JsonObject variants = json("assets/moreminecrafts/blockstates/" + f + ".json").getAsJsonObject("variants");
            assertEquals(count * (facing ? 4 : 1), variants.size(), f + " blockstate variants = parts x facings");
            Set<String> seenModels = new HashSet<>();
            for (var v : variants.entrySet()) {
                String model = v.getValue().getAsJsonObject().get("model").getAsString();
                seenModels.add(model);
                JsonObject m = json("assets/moreminecrafts/models/" + model.substring(model.indexOf(':') + 1) + ".json");
                assertFalse(m.getAsJsonArray("elements").isEmpty(), model + " has geometry");
                for (JsonElement el : m.getAsJsonArray("elements"))
                    for (String k : List.of("from", "to"))
                        for (JsonElement c : el.getAsJsonObject().getAsJsonArray(k))
                            assertTrue(c.getAsDouble() >= -16 && c.getAsDouble() <= 32, model + " element inside the model limits");
                m.getAsJsonObject("textures").entrySet().forEach(e -> textures.add(e.getValue().getAsString()));
            }
            assertEquals(count, seenModels.size(), f + ": one model per part, shared by its four facings");
            models += count;
        }
        for (String tex : textures)
            assertTrue(exists("assets/moreminecrafts/textures/" + tex.substring(tex.indexOf(':') + 1) + ".png"), "texture " + tex);
        assertTrue(exists("assets/moreminecrafts/blockstates/heaven_white_blossom_leaves.json"));
        assertTrue(exists("assets/moreminecrafts/textures/block/heaven/heaven_white_blossom_leaves.png"));
        JsonObject manifest = json("data/moreminecrafts/heaven/manifest_p2.json");
        assertEquals(models + 1, manifest.getAsJsonObject("p2").getAsJsonObject("resources").get("models").getAsInt(), "manifest model count");
        assertTrue(models + 1 < manifest.getAsJsonObject("p1").get("models").getAsInt(), "P2 uses fewer models than P1");
    }

    @Test
    void p2IslandKeepsTerrainAndNaturalPalette() throws IOException {
        Template original = template(ORIGINAL), p1 = template(P1), p2 = template(P2);
        assertArrayEquals(original.size(), p2.size()); assertEquals(original.dataVersion(), p2.dataVersion());
        int terrain = 0;
        for (var e : p1.blocks().entrySet()) {
            String id = e.getValue().id();
            if (NATURAL.contains(id) || id.equals("minecraft:water")) {
                assertEquals(e.getValue(), p2.blocks().get(e.getKey()), "P1 terrain/water block kept at " + e.getKey());
                if (NATURAL.contains(id)) terrain++;
            }
        }
        assertEquals(6272, terrain, "terrain blocks of plain_medium_d");
        assertEquals(terrain, p2.blocks().values().stream().filter(s -> NATURAL.contains(s.id())).count(), "no terrain added in P2");
        for (State s : p2.blocks().values()) {
            boolean ok = NATURAL.contains(s.id()) || VANILLA_REUSED.contains(s.id()) || s.id().equals(NS + ":heaven_white_blossom_leaves")
                    || FAMILIES.contains(s.id().substring(s.id().indexOf(':') + 1)) && s.id().startsWith(NS + ":");
            assertTrue(ok, "unexpected P2 palette state " + s);
        }
        // original terrain positions too (P1 itself was verified against the original in HeavenPrototypeResourceTest)
        for (var e : original.blocks().entrySet())
            if (e.getValue().id().matches("minecraft:(grass_block|dirt|coarse_dirt|stone|andesite|tuff)"))
                assertTrue(NATURAL.contains(p2.blocks().get(e.getKey()).id()), "terrain at " + e.getKey());
    }

    @Test
    void p2PartIndicesAndFacingsAreValid() throws IOException {
        Template p2 = template(P2);
        Map<String, Integer> counts = new HashMap<>();
        for (String f : FAMILIES) counts.put(NS + ":" + f, table(f).get("count").getAsInt());
        for (var e : p2.blocks().entrySet()) {
            State s = e.getValue(); Integer n = counts.get(s.id());
            if (n == null) continue;
            if (n > 1) { int part = Integer.parseInt(s.props().get("part")); assertTrue(part >= 0 && part < n, s + " part"); }
            else assertFalse(s.props().containsKey("part"), s + " single-model family has no part property");
        }
        // vanilla reuse: leaves never decay, petals carry a real amount
        assertTrue(p2.blocks().values().stream().filter(s -> s.id().endsWith("_leaves")).allMatch(s -> "true".equals(s.props().get("persistent"))));
        assertTrue(p2.blocks().values().stream().noneMatch(s -> s.id().equals("minecraft:pink_petals")),
                "vanilla pink_petals cannot survive on heaven grass (plant-support tag not approved): heaven_petals is used instead");
    }

    @Test
    void giantFlowersAreExactAndKeepV5Heights() throws IOException {
        Template p2 = template(P2);
        JsonObject manifest = json("data/moreminecrafts/heaven/manifest_p2.json");
        JsonObject giant = manifest.getAsJsonObject("voxel_comparison").getAsJsonObject("giant");
        assertEquals(1.0, giant.get("iou").getAsDouble(), "giant flowers are voxel-identical to V5");
        Map<String, JsonArray> parts = new HashMap<>();
        for (String g : GIANT) parts.put(g, table(g.substring(NS.length() + 1)).getAsJsonArray("parts"));
        Map<String, Double> expected = Map.of("starlotus", 9.25, "lily", 11.5, "daffodil", 10.5);
        for (JsonElement el : manifest.getAsJsonArray("giants")) {
            JsonObject g = el.getAsJsonObject();
            JsonArray base = g.getAsJsonArray("base"), bb = g.getAsJsonArray("bbox_blocks");
            int bx = base.get(0).getAsInt(), by = base.get(1).getAsInt(), bz = base.get(2).getAsInt();
            double top = 0;
            for (var e : p2.blocks().entrySet()) {
                Pos p = e.getKey(); State s = e.getValue();
                if (!GIANT.contains(s.id()) || p.y() < by) continue;
                double cx = p.x() + 0.5 - (bx + 0.5), cz = p.z() + 0.5 - (bz + 0.5);
                if (cx < bb.get(0).getAsDouble() - 0.5 || cx > bb.get(3).getAsDouble() + 0.5 || cz < bb.get(2).getAsDouble() - 0.5 || cz > bb.get(5).getAsDouble() + 0.5) continue;
                for (JsonElement box : parts.get(s.id()).get(Integer.parseInt(s.props().get("part"))).getAsJsonObject().getAsJsonArray("outline"))
                    top = Math.max(top, p.y() - by + box.getAsJsonArray().get(4).getAsDouble() / 16.0);
            }
            String sp = g.get("species").getAsString();
            assertEquals(expected.get(sp), top, 1e-6, sp + " keeps the V5 original height");
            System.out.println("HEAVEN_P2_GIANT " + sp + " top=" + top);
        }
    }

    @Test
    void speciesStructuresAreStandaloneAndReusable() throws IOException {
        JsonObject structures = json("data/moreminecrafts/heaven/manifest_p2.json").getAsJsonObject("p2").getAsJsonObject("structures");
        for (String name : List.of("giant_starlotus", "giant_lily", "giant_daffodil", "tree_blossom", "tree_white"))
            assertTrue(structures.has(name), "structure " + name);
        for (var e : structures.entrySet()) {
            Template t = template("data/moreminecrafts/structure/heaven_flora/" + e.getKey() + ".nbt");
            JsonObject info = e.getValue().getAsJsonObject();
            assertEquals(info.get("blocks").getAsInt(), t.blocks().size(), e.getKey() + " block count");
            for (Pos p : t.blocks().keySet())
                assertTrue(p.x() < t.size()[0] && p.y() < t.size()[1] && p.z() < t.size()[2], e.getKey() + " inside its size");
            JsonArray pivot = info.getAsJsonArray("pivot");
            for (int a = 0; a < 3; a++) assertTrue(pivot.get(a).getAsInt() >= 0 && pivot.get(a).getAsInt() < t.size()[a], e.getKey() + " pivot inside");
        }
    }

    @Test
    void p1ArtifactsArePreserved() throws Exception {
        JsonObject p1 = json("data/moreminecrafts/heaven/manifest_p2.json").getAsJsonObject("p1");
        assertEquals(p1.get("p1_island_sha256").getAsString(), HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes(P1))), "P1 island unchanged");
        assertEquals(1241, json("data/moreminecrafts/heaven_prototype/flora_parts.json").get("count").getAsInt(), "P1 part table unchanged");
        assertTrue(exists("assets/moreminecrafts/blockstates/heaven_flora_prototype.json"));
    }

    @Test
    void jigsawLinksCanBeRestoredInP2() throws IOException {
        Template original = template(ORIGINAL), p2 = template(P2);
        int jigsaws = 0;
        for (var e : original.blocks().entrySet()) {
            if (!e.getValue().id().equals("minecraft:jigsaw")) continue;
            jigsaws++;
            Pos j = e.getKey();
            assertNull(p2.blocks().get(j), "jigsaw position " + j + " is free in P2");
            for (var b : p2.blocks().entrySet()) {
                String id = b.getValue().id();
                if (NATURAL.contains(id) || id.equals("minecraft:water")) continue;
                assertTrue(Math.hypot(b.getKey().x() - j.x(), b.getKey().z() - j.z()) > 4, "vegetation within 4 columns of jigsaw " + j + ": " + b.getKey());
            }
        }
        assertEquals(2, jigsaws, "plain_medium_d has two satellite jigsaws");
    }

    @Test
    void p2IslandStaysWalkable() throws IOException {
        Template p2 = template(P2);
        Map<String, JsonArray> parts = new HashMap<>();
        for (String g : GIANT) parts.put(g, table(g.substring(NS.length() + 1)).getAsJsonArray("parts"));
        Map<Long, Integer> surface = new HashMap<>();
        for (var e : p2.blocks().entrySet())
            if (NATURAL.contains(e.getValue().id()) || e.getValue().id().equals("minecraft:water"))
                surface.merge((long) e.getKey().x() << 32 | e.getKey().z(), e.getKey().y(), Math::max);
        int grass = 0, walk = 0;
        for (var c : surface.entrySet()) {
            int x = (int) (c.getKey() >> 32), z = c.getKey().intValue(), y = c.getValue();
            if (!p2.blocks().get(new Pos(x, y, z)).id().equals(NS + ":heaven_grass_block")) continue;
            grass++;
            boolean blocked = false;
            for (int dy = 1; dy <= 2; dy++) {
                State s = p2.blocks().get(new Pos(x, y + dy, z));
                if (s == null) continue;
                if (s.id().equals("minecraft:oak_log") || s.id().endsWith("_leaves")) blocked = true;     // full collision blocks
                else if (GIANT.contains(s.id()) && !parts.get(s.id()).get(Integer.parseInt(s.props().get("part"))).getAsJsonObject().getAsJsonArray("collision").isEmpty()) blocked = true;
            }
            if (!blocked) walk++;
        }
        double share = walk / (double) grass;
        System.out.println("HEAVEN_P2_WALKABLE " + walk + "/" + grass + " = " + share);
        assertTrue(share >= 0.95, "P2 keeps at least 95% of the grass surface walkable: " + share);
    }

    @Test
    void p2IsIsolatedFromWorldgen() throws IOException {
        try (Stream<Path> files = Files.walk(Path.of("src/main/resources/data"))) {
            for (Path f : files.filter(p -> p.toString().endsWith(".json")).toList()) {
                String norm = f.toString().replace('\\', '/');
                if (norm.contains("/heaven_prototype/") || norm.contains("/heaven/")) continue;
                String text = Files.readString(f);
                assertFalse(text.contains("heaven_prototype/") || text.contains("heaven_flora/"), "P2 structure referenced by " + f);
            }
        }
    }
}
