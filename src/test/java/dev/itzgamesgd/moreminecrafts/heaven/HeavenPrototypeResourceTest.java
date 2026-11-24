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

/**
 * Heaven sky island P1 prototype: resource and template integrity, read from the packaged resources.
 * Visual quality is NOT verified here (see the client capture and the P1 report).
 */
class HeavenPrototypeResourceTest {
    private static final String NS = "moreminecrafts";
    private static final String PROTO = "data/moreminecrafts/structure/heaven_prototype/plain_medium_d_v5.nbt";
    private static final String ORIGINAL = "data/moreminecrafts/structure/sky_islands/meadow/plain_medium_d.nbt";
    private static final String FLORA = NS + ":heaven_flora_prototype";
    private static final Map<String, String> SLOT = Map.of(
            "minecraft:grass_block", NS + ":heaven_grass_block",
            "minecraft:dirt", NS + ":heaven_loam", "minecraft:coarse_dirt", NS + ":heaven_loam",
            "minecraft:stone", NS + ":heaven_cream_stone",
            "minecraft:andesite", NS + ":heaven_cream_stone_pale", "minecraft:tuff", NS + ":heaven_cream_stone_pale");
    private static final List<String> NATURAL = List.of("heaven_grass_block", "heaven_loam", "heaven_cream_stone", "heaven_cream_stone_pale");

    // ---- resource helpers -----------------------------------------------------------------------------------------

    private static byte[] bytes(String path) throws IOException {
        try (InputStream in = HeavenPrototypeResourceTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(in, "missing resource " + path);
            return in.readAllBytes();
        }
    }

    private static boolean exists(String path) {
        return HeavenPrototypeResourceTest.class.getClassLoader().getResource(path) != null;
    }

    private static JsonObject json(String path) throws IOException {
        return JsonParser.parseString(new String(bytes(path), StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private record State(String id, Map<String, String> props) {}
    private record Pos(int x, int y, int z) {}
    private record Template(int[] size, List<State> palette, Map<Pos, State> blocks, int dataVersion) {}

    @SuppressWarnings("unchecked")
    private static Template template(String path) throws IOException {
        try (DataInputStream in = new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(bytes(path))))) {
            assertEquals(10, in.readByte());
            in.readUTF();
            Map<String, Object> root = (Map<String, Object>) payload(in, 10);
            List<Integer> size = (List<Integer>) root.get("size");
            List<State> palette = new ArrayList<>();
            for (Object o : (List<Object>) root.get("palette")) {
                Map<String, Object> s = (Map<String, Object>) o;
                Map<String, String> props = new TreeMap<>();
                Object p = s.get("properties");
                if (p != null) ((Map<String, Object>) p).forEach((k, v) -> props.put(k, String.valueOf(v)));
                palette.add(new State((String) s.get("id"), props));
            }
            Map<Pos, State> blocks = new HashMap<>();
            for (Object o : (List<Object>) root.get("blocks")) {
                Map<String, Object> e = (Map<String, Object>) o;
                List<Integer> pos = (List<Integer>) e.get("pos");
                State prev = blocks.put(new Pos(pos.get(0), pos.get(1), pos.get(2)), palette.get((Integer) e.get("state")));
                assertNull(prev, "duplicate block position in " + path + ": " + pos);
            }
            return new Template(new int[]{size.get(0), size.get(1), size.get(2)}, palette, blocks, (Integer) root.get("DataVersion"));
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
            case 9: {
                int inner = in.readByte(); int n = in.readInt();
                List<Object> list = new ArrayList<>(n);
                for (int i = 0; i < n; i++) list.add(payload(in, inner));
                return list;
            }
            case 10: {
                Map<String, Object> map = new LinkedHashMap<>();
                for (int t = in.readByte(); t != 0; t = in.readByte()) map.put(in.readUTF(), payload(in, t));
                return map;
            }
            case 11: { int n = in.readInt(); List<Integer> l = new ArrayList<>(n); for (int i = 0; i < n; i++) l.add(in.readInt()); return l; }
            case 12: { int n = in.readInt(); long[] l = new long[n]; for (int i = 0; i < n; i++) l[i] = in.readLong(); return l; }
            default: throw new IOException("unknown NBT tag " + type);
        }
    }

    private static JsonArray parts() throws IOException {
        return json("data/moreminecrafts/heaven_prototype/flora_parts.json").getAsJsonArray("parts");
    }

    private static boolean isTerrain(String id) { return SLOT.containsKey(id); }

    // ---- tests ---------------------------------------------------------------------------------------------------

    @Test
    void naturalBlockResourcesAreComplete() throws IOException {
        for (String id : NATURAL) {
            JsonObject bs = json("assets/moreminecrafts/blockstates/" + id + ".json");
            // P3 (F1): heaven grass gained the vanilla `snowy` state; both variants keep the one P1 model
            JsonObject variants = bs.getAsJsonObject("variants");
            assertEquals(id.equals("heaven_grass_block") ? java.util.Set.of("snowy=false", "snowy=true") : java.util.Set.of(""), variants.keySet(), id + " variants");
            for (var v : variants.entrySet()) assertEquals(NS + ":block/" + id, v.getValue().getAsJsonObject().get("model").getAsString());
            JsonObject m = json("assets/moreminecrafts/models/block/" + id + ".json");
            for (var e : m.getAsJsonObject("textures").entrySet()) {
                String tex = e.getValue().getAsString();
                assertTrue(exists("assets/moreminecrafts/textures/" + tex.substring(tex.indexOf(':') + 1) + ".png"), id + " texture " + tex);
            }
            assertEquals(NS + ":block/" + id, json("assets/moreminecrafts/items/" + id + ".json").getAsJsonObject("model").get("model").getAsString());
            assertTrue(exists("data/moreminecrafts/loot_table/blocks/" + id + ".json"), "loot table " + id);
        }
        JsonObject grass = json("assets/moreminecrafts/models/block/heaven_grass_block.json");
        assertEquals("minecraft:block/cube_bottom_top", grass.get("parent").getAsString());
        assertEquals(NS + ":block/heaven_loam", grass.getAsJsonObject("textures").get("bottom").getAsString(), "grass bottom is the B loam");
        Set<String> shovel = new HashSet<>(), pickaxe = new HashSet<>();
        json("data/minecraft/tags/block/mineable/shovel.json").getAsJsonArray("values").forEach(v -> shovel.add(v.getAsString()));
        json("data/minecraft/tags/block/mineable/pickaxe.json").getAsJsonArray("values").forEach(v -> pickaxe.add(v.getAsString()));
        assertTrue(shovel.containsAll(List.of(NS + ":heaven_grass_block", NS + ":heaven_loam")));
        assertTrue(pickaxe.containsAll(List.of(NS + ":heaven_cream_stone", NS + ":heaven_cream_stone_pale")));
    }

    @Test
    void floraPartResourcesAreComplete() throws IOException {
        JsonArray parts = parts();
        JsonObject variants = json("assets/moreminecrafts/blockstates/heaven_flora_prototype.json").getAsJsonObject("variants");
        assertEquals(parts.size(), variants.size(), "one blockstate variant per part");
        Set<String> textures = new HashSet<>();
        for (int i = 0; i < parts.size(); i++) {
            String model = variants.getAsJsonObject("part=" + i).get("model").getAsString();
            JsonObject m = json("assets/moreminecrafts/models/" + model.substring(model.indexOf(':') + 1) + ".json");
            JsonArray elements = m.getAsJsonArray("elements");
            assertFalse(elements.isEmpty(), "part " + i + " has geometry");
            for (JsonElement el : elements) {
                JsonObject e = el.getAsJsonObject();
                for (int a = 0; a < 3; a++) {
                    double f = e.getAsJsonArray("from").get(a).getAsDouble(), t = e.getAsJsonArray("to").get(a).getAsDouble();
                    assertTrue(0 <= f && f < t && t <= 16, "element inside the block cell (part " + i + ")");
                }
            }
            m.getAsJsonObject("textures").entrySet().forEach(e -> textures.add(e.getValue().getAsString()));
            assertFalse(parts.get(i).getAsJsonObject().getAsJsonArray("outline").isEmpty(), "part " + i + " outline");
        }
        for (String tex : textures)
            assertTrue(exists("assets/moreminecrafts/textures/" + tex.substring(tex.indexOf(':') + 1) + ".png"), "texture " + tex);
    }

    @Test
    void terrainCoordinatesArePreservedAndRemapped() throws IOException {
        Template original = template(ORIGINAL), proto = template(PROTO);
        assertArrayEquals(original.size(), proto.size(), "same template size");
        assertEquals(original.dataVersion(), proto.dataVersion());
        int terrain = 0, water = 0;
        for (var e : original.blocks().entrySet()) {
            String id = e.getValue().id();
            if (isTerrain(id)) {
                terrain++;
                State s = proto.blocks().get(e.getKey());
                assertNotNull(s, "terrain block kept at " + e.getKey());
                assertEquals(SLOT.get(id), s.id(), "slot mapping at " + e.getKey());
            } else if (id.equals("minecraft:water")) {
                water++;
                assertEquals("minecraft:water", proto.blocks().get(e.getKey()).id(), "pond kept at " + e.getKey());
            }
        }
        assertEquals(6272, terrain, "terrain block count of plain_medium_d");
        long protoTerrain = proto.blocks().values().stream().filter(s -> s.id().startsWith(NS + ":heaven_") && !s.id().equals(FLORA)).count();
        assertEquals(terrain, protoTerrain, "no terrain added or removed");
        assertEquals(water, proto.blocks().values().stream().filter(s -> s.id().equals("minecraft:water")).count());
        for (State s : proto.palette())
            assertTrue(s.id().startsWith(NS + ":heaven_") || s.id().equals("minecraft:water"), "unexpected palette state " + s);
    }

    @Test
    void floraStaysInsideTheTemplateAndOffTheTerrain() throws IOException {
        Template proto = template(PROTO);
        int count = parts().size(); int flora = 0;
        for (var e : proto.blocks().entrySet()) {
            Pos p = e.getKey();
            assertTrue(p.x() >= 0 && p.y() >= 0 && p.z() >= 0 && p.x() < proto.size()[0] && p.y() < proto.size()[1] && p.z() < proto.size()[2], "inside template: " + p);
            if (!e.getValue().id().equals(FLORA)) continue;
            flora++;
            int part = Integer.parseInt(e.getValue().props().get("part"));
            assertTrue(part >= 0 && part < count, "valid part index " + part);
        }
        // satellite jigsaw links of the source template (west (0,17,18), east (44,8,18)): keep a 4-block column radius clear
        for (Pos p : proto.blocks().keySet()) {
            if (!proto.blocks().get(p).id().equals(FLORA)) continue;
            for (int[] j : new int[][]{{0, 18}, {44, 18}})
                assertTrue(Math.hypot(p.x() - j[0], p.z() - j[1]) > 4, "flora inside jigsaw clearance: " + p);
        }
        assertEquals(json("data/moreminecrafts/heaven_prototype/manifest.json").getAsJsonObject("nbt").get("flora_blocks").getAsInt(), flora);
    }

    @Test
    void giantFlowersKeepTheirDesignHeight() throws IOException {
        Template proto = template(PROTO);
        JsonArray parts = parts();
        JsonObject manifest = json("data/moreminecrafts/heaven_prototype/manifest.json");
        JsonArray giants = manifest.getAsJsonArray("giants");
        assertEquals(3, giants.size(), "V5 has three giant flowers");
        for (JsonElement g : giants) {
            JsonObject giant = g.getAsJsonObject();
            JsonArray base = giant.getAsJsonArray("base"), bb = giant.getAsJsonArray("bbox_blocks");
            int bx = base.get(0).getAsInt(), by = base.get(1).getAsInt(), bz = base.get(2).getAsInt();
            assertEquals(10, giant.get("design_height_blocks").getAsInt(), "giant flower reference height");
            double top = 0;
            for (var e : proto.blocks().entrySet()) {
                Pos p = e.getKey();
                if (!e.getValue().id().equals(FLORA) || p.y() < by) continue;
                // the giant's own footprint (cell centre inside its voxel bounding box)
                double cx = p.x() + 0.5 - (bx + 0.5), cz = p.z() + 0.5 - (bz + 0.5);
                if (cx < bb.get(0).getAsDouble() - 0.5 || cx > bb.get(3).getAsDouble() + 0.5 || cz < bb.get(2).getAsDouble() - 0.5 || cz > bb.get(5).getAsDouble() + 0.5) continue;
                JsonArray outline = parts.get(Integer.parseInt(e.getValue().props().get("part"))).getAsJsonObject().getAsJsonArray("outline");
                for (JsonElement box : outline) top = Math.max(top, p.y() - by + box.getAsJsonArray().get(4).getAsDouble() / 16.0);
            }
            double design = giant.get("voxel_top_blocks").getAsDouble();
            assertEquals(design, top, 1e-6, giant.get("species").getAsString() + " in-game top equals the V5 design voxel top");
            assertTrue(Math.abs(top - 10) <= 1.5, giant.get("species").getAsString() + " stays within 10 +/- 1.5 blocks: " + top);
            System.out.println("HEAVEN_GIANT " + giant.get("species").getAsString() + " top=" + top + " blocks above the grass");
        }
    }

    @Test
    void stemsAndTrunksLeaveTheIslandWalkable() throws IOException {
        Template proto = template(PROTO);
        JsonArray parts = parts();
        Map<Long, Integer> surface = new HashMap<>();
        for (var e : proto.blocks().entrySet()) {
            Pos p = e.getKey(); String id = e.getValue().id();
            if (id.equals(FLORA)) continue;
            surface.merge((long) p.x() << 32 | p.z(), p.y(), Math::max);
        }
        int grass = 0, walkable = 0;
        for (var col : surface.entrySet()) {
            int x = (int) (col.getKey() >> 32), z = (int) (long) col.getKey().intValue(), y = col.getValue();
            if (!proto.blocks().get(new Pos(x, y, z)).id().equals(NS + ":heaven_grass_block")) continue;
            grass++;
            boolean blocked = false;
            for (int dy = 1; dy <= 2; dy++) {
                State s = proto.blocks().get(new Pos(x, y + dy, z));
                if (s != null && s.id().equals(FLORA)
                        && !parts.get(Integer.parseInt(s.props().get("part"))).getAsJsonObject().getAsJsonArray("collision").isEmpty()) blocked = true;
            }
            if (!blocked) walkable++;
        }
        double share = walkable / (double) grass;
        System.out.println("HEAVEN_WALKABLE " + walkable + "/" + grass + " = " + share);
        assertEquals(807, grass, "grass surface columns of plain_medium_d");
        assertTrue(share >= 0.95, "at least 95% of the grass surface stays walkable: " + share);
    }

    @Test
    void prototypeIsIsolatedFromWorldgenAndTheOriginalIsUntouched() throws Exception {
        try (Stream<Path> files = Files.walk(Path.of("src/main/resources/data"))) {
            // every data JSON except the prototype's own manifest/part table (worldgen, pools, structures, sets, tags, ...)
            for (Path f : files.filter(p -> p.toString().endsWith(".json") && !p.toString().replace('\\', '/').contains("/heaven_prototype/")).toList()) {
                String text = Files.readString(f);
                assertFalse(text.contains("heaven_prototype/"), "prototype template referenced by " + f);
            }
        }
        JsonObject manifest = json("data/moreminecrafts/heaven_prototype/manifest.json");
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes(ORIGINAL));
        assertEquals(manifest.getAsJsonObject("nbt").get("source_sha256").getAsString(), HexFormat.of().formatHex(digest),
                "original plain_medium_d.nbt unchanged since the prototype was generated");
    }
}
