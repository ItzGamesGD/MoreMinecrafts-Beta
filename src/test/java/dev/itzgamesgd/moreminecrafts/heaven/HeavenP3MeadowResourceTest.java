package dev.itzgamesgd.moreminecrafts.heaven;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import org.junit.jupiter.api.Test;

/**
 * P3-D static checks of the 19 heaven meadow templates against their untouched originals: terrain shape, structure,
 * water and jigsaw preservation, palette conversion, vegetation rules, masks, worldgen isolation and the P3 tags.
 */
class HeavenP3MeadowResourceTest {
    private static final String NS = "moreminecrafts";
    private static final Path RES = Path.of("src/main/resources/data/moreminecrafts/structure");
    private static final Path REPORT = Path.of("docs/design/heaven_sky_island_p3_2026-11-20/data/meadow_p3.json");
    private static final List<String> NAMES = List.of("large_a", "large_b", "large_c", "large_d", "large_e", "plain_medium_a", "plain_medium_b",
            "plain_medium_c", "plain_medium_d", "plain_medium_e", "plain_small_a", "plain_small_b", "plain_small_c", "plain_small_d",
            "ruins", "satellite_a", "satellite_b", "temple", "tower");
    private static final Map<String, String> SLOT = Map.of("minecraft:grass_block", NS + ":heaven_grass_block", "minecraft:dirt", NS + ":heaven_loam",
            "minecraft:coarse_dirt", NS + ":heaven_loam", "minecraft:stone", NS + ":heaven_cream_stone",
            "minecraft:andesite", NS + ":heaven_cream_stone_pale", "minecraft:tuff", NS + ":heaven_cream_stone_pale");
    private static final Set<String> VANILLA_VEG = Set.of("minecraft:oak_leaves", "minecraft:oak_log", "minecraft:short_grass", "minecraft:tall_grass",
            "minecraft:dandelion", "minecraft:poppy", "minecraft:allium", "minecraft:cornflower", "minecraft:azure_bluet", "minecraft:oxeye_daisy");
    private static final Set<String> HEAVEN_TERRAIN = new HashSet<>(SLOT.values());
    private static final String GRASS = NS + ":heaven_grass_block", LOAM = NS + ":heaven_loam";
    private static final Set<String> GROUND = Set.of(NS + ":heaven_flower_red", NS + ":heaven_flower_yellow", NS + ":heaven_flower_white",
            NS + ":heaven_flower_pink", NS + ":heaven_flower_blue", NS + ":heaven_flower_violet", NS + ":heaven_short_grass", NS + ":heaven_tall_grass", NS + ":heaven_petals");

    private record State(String id, Map<String, String> props) {}
    private record Pos(int x, int y, int z) { Pos add(int dx, int dy, int dz) { return new Pos(x + dx, y + dy, z + dz); } }
    private record Template(int[] size, Map<Pos, State> blocks, Map<Pos, Map<String, Object>> nbt) {}

    @SuppressWarnings("unchecked")
    private static Template template(Path path) throws IOException {
        try (DataInputStream in = new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(Files.readAllBytes(path))))) {
            assertEquals(10, in.readByte()); in.readUTF();
            Map<String, Object> root = (Map<String, Object>) payload(in, 10);
            List<Integer> size = (List<Integer>) root.get("size");
            List<State> palette = new ArrayList<>();
            for (Object o : (List<Object>) root.get("palette")) {
                Map<String, Object> s = (Map<String, Object>) o; Map<String, String> props = new TreeMap<>();
                if (s.get("properties") != null) ((Map<String, Object>) s.get("properties")).forEach((k, v) -> props.put(k, String.valueOf(v)));
                palette.add(new State((String) s.get("id"), props));
            }
            Map<Pos, State> blocks = new HashMap<>(); Map<Pos, Map<String, Object>> nbt = new HashMap<>();
            for (Object o : (List<Object>) root.get("blocks")) {
                Map<String, Object> e = (Map<String, Object>) o; List<Integer> p = (List<Integer>) e.get("pos");
                Pos pos = new Pos(p.get(0), p.get(1), p.get(2));
                assertNull(blocks.put(pos, palette.get((Integer) e.get("state"))), "duplicate position " + p);
                if (e.get("nbt") != null) nbt.put(pos, (Map<String, Object>) e.get("nbt"));
            }
            return new Template(new int[]{size.get(0), size.get(1), size.get(2)}, blocks, nbt);
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

    private static Template original(String n) throws IOException { return template(RES.resolve("sky_islands/meadow/" + n + ".nbt")); }
    private static Template heaven(String n) throws IOException { return template(RES.resolve("heaven_p3/meadow/" + n + ".nbt")); }
    private static JsonObject report() throws IOException { return JsonParser.parseString(Files.readString(REPORT)).getAsJsonObject(); }

    @Test
    void originalsAreUntouchedSinceGeneration() throws Exception {
        JsonObject r = report();
        for (String n : NAMES) {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(RES.resolve("sky_islands/meadow/" + n + ".nbt")));
            assertEquals(r.getAsJsonObject(n).get("source_sha256").getAsString(), HexFormat.of().formatHex(d), n + " original unchanged");
        }
    }

    @Test
    void terrainStructureWaterAndJigsawsArePreserved() throws IOException {
        for (String n : NAMES) {
            Template o = original(n), h = heaven(n);
            assertArrayEquals(o.size(), h.size(), n + " template size (island size) unchanged");
            int kept = 0;
            for (var e : o.blocks().entrySet()) {
                State os = e.getValue(); State hs = h.blocks().get(e.getKey());
                if (VANILLA_VEG.contains(os.id())) continue;             // replaced by heaven vegetation (or air)
                assertNotNull(hs, n + " lost " + os + " at " + e.getKey());
                if (SLOT.containsKey(os.id())) {
                    if (hs.equals(os)) { kept++; continue; }              // terrain-id block that belongs to a building
                    assertEquals(SLOT.get(os.id()), hs.id(), n + " terrain slot at " + e.getKey());
                } else if (os.id().equals("minecraft:jigsaw")) {
                    assertEquals(os, hs, n + " jigsaw orientation at " + e.getKey());
                    Map<String, Object> on = new TreeMap<>(o.nbt().get(e.getKey())), hn = new TreeMap<>(h.nbt().get(e.getKey()));
                    String fs = (String) on.remove("final_state"), hfs = (String) hn.remove("final_state");
                    assertEquals(on, hn, n + " jigsaw NBT (pool, name, target, joint, priorities) at " + e.getKey());
                    assertEquals(switch (fs) { case "minecraft:grass_block" -> GRASS; case "minecraft:dirt" -> LOAM; default -> fs; }, hfs, n + " final_state");
                } else {
                    assertEquals(os, hs, n + " structure / water block at " + e.getKey());
                }
            }
            for (var e : h.blocks().entrySet()) {
                State os = o.blocks().get(e.getKey());
                if (os == null || VANILLA_VEG.contains(os.id()))
                    assertFalse(HEAVEN_TERRAIN.contains(e.getValue().id()) || e.getValue().id().equals("minecraft:jigsaw"), n + " new non-vegetation block " + e);
            }
            assertEquals(n.equals("tower") ? 3312 : 0, kept, n + " terrain-id blocks kept as building (Sky Tower andesite walls only)");
            assertEquals(0, h.blocks().values().stream().filter(s -> VANILLA_VEG.contains(s.id()) && !s.id().equals("minecraft:oak_log")).count(), n + " no vanilla vegetation left");
        }
    }

    @Test
    void vegetationFollowsTheP3SurvivalRules() throws IOException {
        Map<String, int[]> dir = Map.of("north", new int[]{0, 0, -1}, "south", new int[]{0, 0, 1}, "east", new int[]{1, 0, 0}, "west", new int[]{-1, 0, 0});
        Set<String> sturdy = new HashSet<>(HEAVEN_TERRAIN);
        sturdy.addAll(Set.of("minecraft:oak_log", "minecraft:cherry_leaves", NS + ":heaven_white_blossom_leaves"));
        for (String n : NAMES) {
            Template h = heaven(n);
            java.util.function.Function<Pos, String> at = p -> h.blocks().containsKey(p) ? h.blocks().get(p).id() : "minecraft:air";
            for (var e : h.blocks().entrySet()) {
                Pos p = e.getKey(); State s = e.getValue(); String below = at.apply(p.add(0, -1, 0)), above = at.apply(p.add(0, 1, 0));
                if (GROUND.contains(s.id())) assertTrue(below.equals(GRASS) || below.equals(LOAM), n + " " + s + " at " + p + " stands on " + below);
                if (s.id().endsWith("_leaves")) assertEquals("true", s.props().get("persistent"), n + " leaves never decay");
                if (s.id().equals(NS + ":heaven_vine") || s.id().equals(NS + ":heaven_wisteria")) {
                    int[] d = dir.get(s.props().get("facing"));
                    boolean ok = HEAVEN_TERRAIN.contains(at.apply(p.add(d[0], d[1], d[2]))) || above.equals(s.id())
                            || (s.id().endsWith("wisteria") && sturdy.contains(above));
                    assertTrue(ok, n + " " + s + " at " + p + " has nothing to hang on");
                }
            }
        }
    }

    @Test
    void giantFlowersAreCompleteAndCountedInTheReport() throws IOException {
        JsonObject r = report();
        for (String n : NAMES) {
            JsonObject g = r.getAsJsonObject(n).getAsJsonObject("giants");
            int placed = g.get("placed").getAsInt();
            assertTrue(placed <= g.get("trial_formula_requested").getAsInt(), n + " never more giants than the F10 trial formula asks");
            Template h = heaven(n);
            long stems = h.blocks().values().stream().filter(s -> s.id().equals(NS + ":heaven_giant_stem")).count();
            assertEquals(placed == 0, stems == 0, n + " giant stems iff giants placed");
            for (JsonElement pe : g.getAsJsonArray("positions")) {
                JsonArray a = pe.getAsJsonArray();
                Pos root = new Pos(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt());
                assertEquals(GRASS, h.blocks().get(root.add(0, -1, 0)).id(), n + " giant root on heaven grass");
            }
        }
    }

    @Test
    void p3TemplatesAreIsolatedFromWorldgen() throws IOException {
        try (Stream<Path> files = Files.walk(Path.of("src/main/resources/data"))) {
            for (Path f : files.filter(p -> p.toString().endsWith(".json")).toList())
                assertFalse(Files.readString(f).contains("heaven_p3/"), "heaven P3 template referenced by " + f);
        }
    }

    @Test
    void plantSupportTagsAreIndividualNeverDirt() throws IOException {
        Path tags = Path.of("src/main/resources/data/minecraft/tags/block");
        assertFalse(Files.exists(tags.resolve("dirt.json")), "#minecraft:dirt is never extended (F2)");
        assertFalse(Files.exists(tags.resolve("grass_blocks.json")), "#minecraft:grass_blocks is never extended (F2)");
        for (String t : List.of("supports_vegetation", "supports_sugar_cane", "turns_into_dirt_path", "turns_into_farmland", "cannot_replace_below_tree_trunk")) {
            JsonObject j = JsonParser.parseString(Files.readString(tags.resolve(t + ".json"))).getAsJsonObject();
            assertFalse(j.get("replace").getAsBoolean(), t + " appends");
            Set<String> v = new HashSet<>(); j.getAsJsonArray("values").forEach(x -> v.add(x.getAsString()));
            assertEquals(Set.of(GRASS, LOAM), v, t + " lists exactly heaven grass and loam");
        }
    }
}
