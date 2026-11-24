package dev.itzgamesgd.moreminecrafts.heaven;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonArray;
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
 * P3.8: production heaven templates (meadow + forest) preserve the originals; the option-D pool change is implemented but inactive;
 * the live worldgen files are byte-identical to the pre-P3.8 baseline; static closure of the repointed pool graph.
 */
class HeavenP38ResourceTest {
    private static final String NS = "moreminecrafts";
    private static final Path RES = Path.of("src/main/resources");
    private static final Path DATA = RES.resolve("data/moreminecrafts");
    private static final Path PENDING = RES.resolve("heaven_worldgen_pending/data/moreminecrafts/worldgen/template_pool/sky_islands");
    private static final Path REPORT = Path.of("docs/design/heaven_sky_island_p3_8_2026-11-21/data/heaven_production.json");
    private static final List<String> THEMES = List.of("meadow", "forest");
    /** P3.12: themes whose 3 live pools are switched to the heaven templates (pending_pools.py apply). Updated step by step with the approved rollout. */
    static final Set<String> TRANSITIONED = Set.of("meadow", "forest");
    private static final List<String> NAMES = List.of("large_a", "large_b", "large_c", "large_d", "large_e", "plain_medium_a", "plain_medium_b", "plain_medium_c",
            "plain_medium_d", "plain_medium_e", "plain_small_a", "plain_small_b", "plain_small_c", "plain_small_d", "ruins", "satellite_a", "satellite_b", "temple", "tower");
    private static final String GRASS = NS + ":heaven_grass_block", LOAM = NS + ":heaven_loam", STONE = NS + ":heaven_cream_stone", PALE = NS + ":heaven_cream_stone_pale";
    private static final Map<String, Map<String, String>> SLOT = Map.of(
            "meadow", Map.of("minecraft:grass_block", GRASS, "minecraft:dirt", LOAM, "minecraft:coarse_dirt", LOAM, "minecraft:stone", STONE, "minecraft:andesite", PALE, "minecraft:tuff", PALE),
            "forest", Map.of("minecraft:grass_block", GRASS, "minecraft:dirt", LOAM, "minecraft:coarse_dirt", LOAM, "minecraft:podzol", LOAM, "minecraft:stone", STONE,
                    "minecraft:andesite", PALE, "minecraft:tuff", PALE, "minecraft:mossy_cobblestone", PALE));
    private static final List<String> VEG_KEYS = List.of("leaves", "short_grass", "tall_grass", "fern", "poppy", "daisy", "bluet", "allium", "dandelion", "cornflower", "tulip",
            "dead_bush", "_log", "sapling", "mushroom", "vine", "sweet_berry", "moss_carpet", "flower", "bush", "petals");
    private static final Set<String> HEAVEN_TERRAIN = Set.of(GRASS, LOAM, STONE, PALE);
    private static final Map<String, int[]> DIR = Map.of("north", new int[]{0, 0, -1}, "south", new int[]{0, 0, 1}, "east", new int[]{1, 0, 0}, "west", new int[]{-1, 0, 0});

    private record State(String id, Map<String, String> props) {}
    private record Pos(int x, int y, int z) { Pos add(int dx, int dy, int dz) { return new Pos(x + dx, y + dy, z + dz); } }
    private record Tpl(int[] size, Map<Pos, State> blocks, Map<Pos, Map<String, Object>> nbt) {}

    private static boolean isVeg(String id) { return VEG_KEYS.stream().anyMatch(k -> id.substring(id.indexOf(':') + 1).contains(k)); }
    private static String sha(Path p) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(p))); }
    private static JsonObject json(Path p) throws IOException { return JsonParser.parseString(Files.readString(p)).getAsJsonObject(); }
    private static Path orig(String t, String n) { return DATA.resolve("structure/sky_islands/" + t + "/" + n + ".nbt"); }
    private static Path heaven(String t, String n) { return DATA.resolve("structure/sky_islands/heaven/" + t + "/" + n + ".nbt"); }

    @SuppressWarnings("unchecked")
    private static Tpl template(Path path) throws IOException {
        try (DataInputStream in = new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(Files.readAllBytes(path))))) {
            in.readByte(); in.readUTF();
            Map<String, Object> root = (Map<String, Object>) payload(in, 10);
            List<Integer> size = (List<Integer>) root.get("size"); List<State> pal = new ArrayList<>();
            for (Object o : (List<Object>) root.get("palette")) {
                Map<String, Object> s = (Map<String, Object>) o; Map<String, String> props = new TreeMap<>();
                if (s.get("properties") != null) ((Map<String, Object>) s.get("properties")).forEach((k, v) -> props.put(k, String.valueOf(v)));
                pal.add(new State((String) s.get("id"), props));
            }
            Map<Pos, State> b = new HashMap<>(); Map<Pos, Map<String, Object>> n = new HashMap<>();
            for (Object o : (List<Object>) root.get("blocks")) {
                Map<String, Object> e = (Map<String, Object>) o; List<Integer> p = (List<Integer>) e.get("pos"); Pos pos = new Pos(p.get(0), p.get(1), p.get(2));
                assertNull(b.put(pos, pal.get((Integer) e.get("state"))), "duplicate position");
                if (e.get("nbt") != null) n.put(pos, (Map<String, Object>) e.get("nbt"));
            }
            return new Tpl(new int[]{size.get(0), size.get(1), size.get(2)}, b, n);
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
    void liveWorldgenIsByteIdenticalToTheBaselineAndTheD_ChangeIsPending() throws Exception {
        Map<String, String> expected = new TreeMap<>();
        for (String line : Files.readAllLines(Path.of("tools/heaven-p38/worldgen_baseline.sha256"))) { String[] f = line.split("  ", 2); expected.put(f[1], f[0]); }
        Map<String, String> actual = new TreeMap<>();
        for (Path root : List.of(DATA.resolve("worldgen"), DATA.resolve("tags/worldgen")))
            try (Stream<Path> s = Files.walk(root)) { for (Path p : s.filter(x -> x.toString().endsWith(".json")).toList()) actual.put(p.toString().replace('\\', '/'), sha(p)); }
        for (String t : TRANSITIONED) for (String p : List.of("start", "satellite", "tower_top")) {      // the only approved live changes: the 3 pools of each transitioned theme == pending
            String key = "src/main/resources/data/moreminecrafts/worldgen/template_pool/sky_islands/" + t + "/" + p + ".json";
            assertNotNull(expected.put(key, sha(PENDING.resolve(t + "/" + p + ".json"))), key + " is in the baseline");
        }
        assertEquals(expected, actual, "structure IDs, sets, other pools, biome tags (probabilities, mob spawn overrides) unchanged; only the approved pools switched");
        // the pending change is outside data/, differs from the live pools only by the element locations, and reverts exactly
        for (String t : THEMES) for (String p : List.of("start", "satellite", "tower_top")) {
            String live = Files.readString(DATA.resolve("worldgen/template_pool/sky_islands/" + t + "/" + p + ".json"));
            String pending = Files.readString(PENDING.resolve(t + "/" + p + ".json"));
            if (TRANSITIONED.contains(t)) { assertEquals(pending, live, t + "/" + p + " live pool == pending (transitioned)"); live = pending.replace("sky_islands/heaven/" + t + "/", "sky_islands/" + t + "/"); }
            else assertFalse(live.contains("heaven"), t + "/" + p + " live pool is not repointed");
            assertEquals(live.replace("\"location\": \"moreminecrafts:sky_islands/" + t + "/", "\"location\": \"moreminecrafts:sky_islands/heaven/" + t + "/"), pending, t + "/" + p + " pending = live with repointed locations");
            assertEquals(live, pending.replace("sky_islands/heaven/" + t + "/", "sky_islands/" + t + "/"), t + "/" + p + " reverts exactly");
            JsonArray a = JsonParser.parseString(live).getAsJsonObject().getAsJsonArray("elements"), b = JsonParser.parseString(pending).getAsJsonObject().getAsJsonArray("elements");
            assertEquals(a.size(), b.size());
            for (int i = 0; i < a.size(); i++) assertEquals(a.get(i).getAsJsonObject().get("weight"), b.get(i).getAsJsonObject().get("weight"), "weights unchanged");
        }
    }

    @Test
    void originalsAreUntouchedAndProductionMeadowEqualsTheP3Templates() throws Exception {
        JsonObject r = json(REPORT);
        for (String t : THEMES) for (String n : NAMES) {
            assertEquals(r.getAsJsonObject(t).getAsJsonObject(n).get("source_sha256").getAsString(), sha(orig(t, n)), t + "/" + n + " original unchanged");
            assertEquals(r.getAsJsonObject(t).getAsJsonObject(n).get("output_sha256").getAsString(), sha(heaven(t, n)), t + "/" + n + " production template matches the generator report");
            if (t.equals("meadow")) assertEquals(sha(DATA.resolve("structure/heaven_p3/meadow/" + n + ".nbt")), sha(heaven(t, n)), "meadow production template = the preserved P3 template");
        }
    }

    @Test
    void productionTemplatesPreserveShapeStructureWaterAndJigsaws() throws Exception {
        for (String t : THEMES) for (String n : NAMES) {
            Tpl o = template(orig(t, n)), h = template(heaven(t, n)); Map<String, String> slot = SLOT.get(t); int kept = 0;
            assertArrayEquals(o.size(), h.size(), t + "/" + n + " size");
            for (var e : o.blocks().entrySet()) {
                State os = e.getValue(), hs = h.blocks().get(e.getKey());
                if (isVeg(os.id())) continue;
                assertNotNull(hs, t + "/" + n + " lost " + os + " at " + e.getKey());
                if (slot.containsKey(os.id())) { if (hs.equals(os)) kept++; else assertEquals(slot.get(os.id()), hs.id(), t + "/" + n + " terrain slot at " + e.getKey()); }
                else if (os.id().equals("minecraft:jigsaw")) {
                    assertEquals(os, hs, "jigsaw orientation");
                    Map<String, Object> on = new TreeMap<>(o.nbt().get(e.getKey())), hn = new TreeMap<>(h.nbt().get(e.getKey()));
                    String fs = (String) on.remove("final_state"), hfs = (String) hn.remove("final_state");
                    assertEquals(on, hn, t + "/" + n + " jigsaw NBT incl. pool value (option D keeps the pool IDs)");
                    assertEquals(switch (fs) { case "minecraft:grass_block" -> GRASS; case "minecraft:dirt" -> LOAM; default -> fs; }, hfs, "final_state");
                } else assertEquals(os, hs, t + "/" + n + " structure / water block at " + e.getKey());
            }
            for (var e : h.blocks().entrySet()) { State os = o.blocks().get(e.getKey()); if (os == null || isVeg(os.id())) assertFalse(HEAVEN_TERRAIN.contains(e.getValue().id()) || e.getValue().id().equals("minecraft:jigsaw"), "new non-vegetation block"); }
            assertEquals(n.equals("tower") ? 3312 : 0, kept, t + "/" + n + " terrain-id blocks kept as building (Sky Tower andesite walls only)");
            long leftover = h.blocks().values().stream().filter(s -> s.id().startsWith("minecraft:") && isVeg(s.id()) && !s.id().equals("minecraft:oak_log") && !s.id().equals("minecraft:cherry_leaves")).count();
            assertEquals(0, leftover, t + "/" + n + " no vanilla vegetation left (oak log / cherry leaves are the V5 tree blocks)");
        }
    }

    @Test
    void productionVegetationFollowsTheSurvivalRulesAndForestIsDenser() throws Exception {
        Set<String> sturdy = new HashSet<>(HEAVEN_TERRAIN); sturdy.addAll(Set.of("minecraft:oak_log", "minecraft:cherry_leaves", NS + ":heaven_white_blossom_leaves"));
        Set<String> ground = Set.of(NS + ":heaven_flower_red", NS + ":heaven_flower_yellow", NS + ":heaven_flower_white", NS + ":heaven_flower_pink", NS + ":heaven_flower_blue",
                NS + ":heaven_flower_violet", NS + ":heaven_short_grass", NS + ":heaven_tall_grass", NS + ":heaven_petals");
        Map<String, Integer> logs = new HashMap<>();
        for (String t : THEMES) for (String n : NAMES) {
            Tpl h = template(heaven(t, n));
            java.util.function.Function<Pos, String> at = p -> h.blocks().containsKey(p) ? h.blocks().get(p).id() : "minecraft:air";
            for (var e : h.blocks().entrySet()) {
                Pos p = e.getKey(); State s = e.getValue(); String below = at.apply(p.add(0, -1, 0)), above = at.apply(p.add(0, 1, 0));
                if (ground.contains(s.id())) assertTrue(below.equals(GRASS) || below.equals(LOAM), t + "/" + n + " ground plant support at " + p);
                if (s.id().endsWith("_leaves")) assertEquals("true", s.props().get("persistent"), "leaves never decay");
                if (s.id().equals("minecraft:oak_log")) logs.merge(t, 1, Integer::sum);
                if (s.id().equals(NS + ":heaven_vine") || s.id().equals(NS + ":heaven_wisteria")) {
                    int[] d = DIR.get(s.props().get("facing"));
                    assertTrue(HEAVEN_TERRAIN.contains(at.apply(p.add(d[0], d[1], d[2]))) || above.equals(s.id()) || (s.id().endsWith("wisteria") && sturdy.contains(above)), t + "/" + n + " hanging support at " + p);
                }
            }
        }
        assertTrue(logs.get("forest") > 2 * logs.get("meadow"), "forest strengthened density: " + logs);
    }

    @Test
    void pendingPoolGraphIsClosedOverHeavenTemplatesAndSharedPieces() throws Exception {
        for (String t : THEMES) {
            Set<String> seen = new HashSet<>(), reached = new TreeSet<>(); Deque<String> q = new ArrayDeque<>(List.of(NS + ":sky_islands/" + t + "/start"));
            while (!q.isEmpty()) {
                String pid = q.pop(); if (!seen.add(pid) || pid.equals("minecraft:empty")) continue;
                String sub = pid.substring(pid.indexOf(':') + 1).replaceFirst("^sky_islands/", "");
                Path pool = sub.startsWith(t + "/") ? PENDING.resolve(sub + ".json") : DATA.resolve("worldgen/template_pool/sky_islands/" + sub + ".json");
                assertTrue(Files.exists(pool), "pool resolves: " + pid);
                for (var e : json(pool).getAsJsonArray("elements")) {
                    var el = e.getAsJsonObject().getAsJsonObject("element"); if (!el.has("location")) continue;
                    String loc = el.get("location").getAsString(); reached.add(loc);
                    Tpl tp = template(DATA.resolve("structure/" + loc.substring(loc.indexOf(':') + 1) + ".nbt"));
                    for (var n : tp.nbt().values()) if ("minecraft:jigsaw".equals(n.get("id"))) q.push((String) n.get("pool"));
                }
            }
            for (String loc : reached) assertFalse(loc.matches(NS + ":sky_islands/" + t + "/.*"), "no vanilla " + t + " island template is reachable: " + loc);
            long heaven = reached.stream().filter(l -> l.startsWith(NS + ":sky_islands/heaven/" + t + "/")).count();
            assertEquals(NAMES.size(), heaven, t + ": all 19 heaven templates reachable (start pool + satellites + tower)");
            assertTrue(reached.stream().anyMatch(l -> l.matches(NS + ":sky_islands/(ruins_[a-d]|temple)")), "shared functional pieces stay in the graph");
        }
    }

    @Test
    void habitatPathPrefixAndWorldgenReferencesOnlyInTheApprovedPools() throws Exception {
        for (String t : THEMES) for (String n : NAMES) assertTrue(Files.exists(heaven(t, n)) && heaven(t, n).toString().replace('\\', '/').contains("structure/sky_islands/heaven/"), "template under sky_islands/");
        try (Stream<Path> files = Files.walk(RES.resolve("data"))) {
            for (Path f : files.filter(p -> p.toString().endsWith(".json")).toList()) {
                String text = Files.readString(f);
                String path = f.toString().replace('\\', '/');
                boolean approvedPool = TRANSITIONED.stream().anyMatch(t -> path.endsWith("worldgen/template_pool/sky_islands/" + t + "/start.json") || path.endsWith("worldgen/template_pool/sky_islands/" + t + "/satellite.json") || path.endsWith("worldgen/template_pool/sky_islands/" + t + "/tower_top.json"));
                assertEquals(approvedPool, text.contains("sky_islands/heaven/") && !path.contains("/heaven/"), "only the approved (transitioned) pools reference the production heaven templates: " + f);
            }
        }
    }

    @Test
    void authorityRecordsTheApprovedStructureDecisionAndTheDeferrals() throws Exception {
        String a = Files.readString(Path.of("docs/design/SKY_ISLANDS_T2_CONTEXT_2026-10-27.md"));
        for (String k : List.of("### 24.5", "H1 = D안", "초원(meadow)과 forest 우선", "나머지 4테마", "sky_islands/heaven/", "H4 = A안", "807기둥당 나무 6그루", "별도 오너 승인 전까지 보류",
                "sky_island_mountain_meadow", "F7(거대 꽃 드롭), G2(거대 꽃 최종 밀도)"))
            assertTrue(a.contains(k), "authority §24.5 records " + k);
        assertTrue(Files.readString(Path.of("docs/CONTENT_AUTHORITY_INDEX.md")).contains("#245-2026-11-21"), "index routes to §24.5");
    }
}
