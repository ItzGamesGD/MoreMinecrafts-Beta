package dev.itzgamesgd.moreminecrafts.worldgen;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;
import org.junit.jupiter.api.Test;

/** Task A resource checks: approved sky island structures, rewards and the untouched vanilla trial content. */
class SkyIslandsResourceTest {
    private static final String FEATHER = "moreminecrafts:sky_feather";
    private static final String GALE_SHARD = "moreminecrafts:gale_shard";
    private static final String CHEST = "moreminecrafts:chests/sky_islands/chest";
    private static final String VAULT = "moreminecrafts:chests/sky_islands/vault";
    private static final String VAULT_TOP = "moreminecrafts:chests/sky_islands/vault_tower_top";

    private static byte[] bytes(String path) throws IOException {
        try (InputStream stream = SkyIslandsResourceTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(stream, "missing resource " + path);
            return stream.readAllBytes();
        }
    }

    private static JsonObject json(String path) throws IOException {
        return JsonParser.parseString(new String(bytes(path), StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static boolean exists(String path) {
        return SkyIslandsResourceTest.class.getClassLoader().getResource(path) != null;
    }

    private static JsonObject lootTable(String id) throws IOException {
        return json("data/moreminecrafts/loot_table/" + id.substring("moreminecrafts:".length()) + ".json");
    }

    /** Every pool that can emit the given item directly (not through a nested table). */
    private static List<JsonObject> poolsEmitting(JsonObject table, String item) {
        List<JsonObject> pools = new ArrayList<>();
        for (JsonElement pool : table.getAsJsonArray("pools")) {
            for (JsonElement entry : pool.getAsJsonObject().getAsJsonArray("entries")) {
                JsonObject e = entry.getAsJsonObject();
                if (e.has("name") && e.get("name").getAsString().equals(item)) pools.add(pool.getAsJsonObject());
            }
        }
        return pools;
    }

    private static void assertSingleChanceRoll(JsonObject table, String item, double chance, String label) {
        List<JsonObject> pools = poolsEmitting(table, item);
        assertEquals(1, pools.size(), label + ": exactly one pool may emit " + item);
        JsonObject pool = pools.getFirst();
        assertEquals(1, pool.get("rolls").getAsInt(), label + ": one draw per container");
        assertEquals("minecraft:random_chance", pool.getAsJsonObject("condition").get("type").getAsString(), label);
        assertEquals(chance, pool.getAsJsonObject("condition").get("chance").getAsDouble(), 1e-12, label);
        assertEquals(1, pool.getAsJsonArray("entries").size(), label + ": the pool holds only the reward");
        JsonObject entry = pool.getAsJsonArray("entries").get(0).getAsJsonObject();
        assertEquals(1, entry.getAsJsonArray("modifier").size(), label + ": only a count modifier");
        JsonObject count = entry.getAsJsonArray("modifier").get(0).getAsJsonObject();
        assertEquals("minecraft:set_count", count.get("type").getAsString(), label);
        assertEquals(1, count.get("count").getAsInt(), label + ": exactly one item");
        assertFalse(entry.has("weight") && entry.get("weight").getAsInt() != 1, label);
    }

    @Test void chestFeatherFivePercentExactlyOne() throws Exception {
        JsonObject chest = lootTable(CHEST);
        assertSingleChanceRoll(chest, FEATHER, 0.05, "chest feather");
        assertTrue(poolsEmitting(chest, GALE_SHARD).isEmpty(), "ordinary chests never carry Gale Shards");
    }

    @Test void vaultsFeatherFifteenAndGaleShardFivePercentIndependently() throws Exception {
        for (String id : List.of(VAULT, VAULT_TOP)) {
            JsonObject table = lootTable(id);
            assertSingleChanceRoll(table, FEATHER, 0.15, id + " feather");
            assertSingleChanceRoll(table, GALE_SHARD, 0.05, id + " gale shard");
            assertNotSame(poolsEmitting(table, FEATHER).getFirst(), poolsEmitting(table, GALE_SHARD).getFirst(),
                    id + ": feather and shard are separate pools, so each is an independent draw");
        }
    }

    @Test void crownVaultRaisesOnlyNonFeatherRareWeights() throws Exception {
        JsonObject normal = lootTable(VAULT);
        JsonObject top = lootTable(VAULT_TOP);
        Map<String, Integer> normalWeights = firstPoolWeights(normal);
        Map<String, Integer> topWeights = firstPoolWeights(top);
        assertTrue(topWeights.get("minecraft:chests/trial_chambers/reward_rare") > normalWeights.get("minecraft:chests/trial_chambers/reward_rare"));
        assertTrue(topWeights.get("moreminecrafts:chests/sky_islands/materials") > normalWeights.get("moreminecrafts:chests/sky_islands/materials"));
        // The crown must not make Sky Feathers or Gale Shards more likely than an ordinary vault.
        assertEquals(poolsEmitting(normal, FEATHER).getFirst().getAsJsonObject("condition"), poolsEmitting(top, FEATHER).getFirst().getAsJsonObject("condition"));
        assertEquals(poolsEmitting(normal, GALE_SHARD).getFirst().getAsJsonObject("condition"), poolsEmitting(top, GALE_SHARD).getFirst().getAsJsonObject("condition"));
    }

    private static Map<String, Integer> firstPoolWeights(JsonObject table) {
        Map<String, Integer> weights = new HashMap<>();
        for (JsonElement entry : table.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries"))
            weights.put(entry.getAsJsonObject().get("value").getAsString(), entry.getAsJsonObject().get("weight").getAsInt());
        return weights;
    }

    @Test void skyRewardTablesReferenceRegisteredItemsOnly() throws Exception {
        String registry = new String(bytes("data/moreminecrafts/loot_table/chests/sky_islands/materials.json"), StandardCharsets.UTF_8);
        for (String item : List.of("moreminecrafts:mythril", "moreminecrafts:titanium_ingot", "moreminecrafts:adamantium_ingot"))
            assertTrue(registry.contains(item), item);
        // the item ids exist in the mod's item-model resources (registered items)
        for (String item : List.of("sky_feather", "gale_shard", "mythril", "titanium_ingot", "adamantium_ingot"))
            assertTrue(exists("assets/moreminecrafts/items/" + item + ".json"), "item definition " + item);
    }

    @Test void vanillaTrialContentIsNotOverridden() {
        // Checked against the mod's own source tree: the class path also holds the vanilla jar, which legitimately has these files.
        for (String path : List.of(
                "loot_table/chests/trial_chambers/reward.json",
                "loot_table/chests/trial_chambers/reward_ominous.json",
                "loot_table/chests/trial_chambers/reward_rare.json",
                "worldgen/structure/trial_chambers.json",
                "worldgen/structure_set/trial_chambers.json",
                "trial_spawner/trial_chamber/melee/zombie/normal.json"))
            assertFalse(java.nio.file.Files.exists(java.nio.file.Path.of("src/main/resources/data/minecraft/" + path)),
                    "vanilla trial data must not be replaced: " + path);
    }

    @Test void skyTrialSpawnersOnlyAddTheApprovedShulker() throws Exception {
        for (String mode : List.of("normal", "ominous")) {
            JsonObject config = json("data/moreminecrafts/trial_spawner/sky_islands/shulker/" + mode + ".json");
            JsonArray potentials = config.getAsJsonArray("spawn_potentials");
            assertEquals(1, potentials.size());
            assertEquals("minecraft:shulker", potentials.get(0).getAsJsonObject().getAsJsonObject("data").getAsJsonObject("entity").get("id").getAsString());
        }
    }

    @Test void structuresPoolsSetsAndTagsAreWired() throws Exception {
        List<String> structures = List.of("sky_island_mountain_snowy", "sky_island_mountain_rocky", "sky_island_mountain_meadow",
                "sky_island_snowy", "sky_island_meadow", "sky_island_forest", "sky_island_desert", "sky_island_badlands");
        for (String id : structures) {
            JsonObject structure = json("data/moreminecrafts/worldgen/structure/" + id + ".json");
            assertEquals(id.startsWith("sky_island_mountain_") ? "moreminecrafts:adaptive_jigsaw" : "minecraft:jigsaw", structure.get("type").getAsString());
            if (id.startsWith("sky_island_mountain_")) { assertEquals(18, structure.get("minimum_offset").getAsInt()); assertEquals(70, structure.get("maximum_offset").getAsInt()); assertFalse(structure.has("start_height")); }
            assertEquals("none", structure.get("terrain_adaptation").getAsString(), "islands float: no terrain adaptation");
            assertTrue(exists("data/moreminecrafts/tags/worldgen/biome/has_structure/" + id + ".json"), id);
            String start = structure.get("start_pool").getAsString().substring("moreminecrafts:".length());
            JsonObject pool = json("data/moreminecrafts/worldgen/template_pool/" + start + ".json");
            for (JsonElement element : pool.getAsJsonArray("elements")) {
                JsonObject e = element.getAsJsonObject().getAsJsonObject("element");
                if (e.has("location")) assertTrue(exists("data/moreminecrafts/structure/" + e.get("location").getAsString().substring("moreminecrafts:".length()) + ".nbt"));
            }
        }
        JsonObject mountain = json("data/moreminecrafts/worldgen/structure_set/sky_islands_mountain.json");
        JsonObject general = json("data/moreminecrafts/worldgen/structure_set/sky_islands_general.json");
        // Mountains are denser than the rest of the Overworld, but neither set places an island in every region.
        assertTrue(mountain.getAsJsonObject("placement").get("spacing").getAsInt() < general.getAsJsonObject("placement").get("spacing").getAsInt());
        assertTrue(mountain.getAsJsonObject("placement").get("frequency").getAsDouble() < 1.0);
        assertTrue(general.getAsJsonObject("placement").get("frequency").getAsDouble() < 1.0);
        assertEquals(3, mountain.getAsJsonArray("structures").size());
        assertEquals(5, general.getAsJsonArray("structures").size());
    }

    @Test void t2WindEquipmentMaterialsAreObtainableFromSkyContentAndStillUnlockTheirRecipes() throws Exception {
        JsonObject sword = json("data/moreminecrafts/recipe/wind_sword.json");
        JsonObject bow = json("data/moreminecrafts/recipe/wind_archers_bow.json");
        assertTrue(sword.getAsJsonObject("key").toString().contains(FEATHER), "Wind Sword consumes Sky Feathers");
        assertTrue(bow.getAsJsonObject("key").toString().contains(FEATHER) && bow.getAsJsonObject("key").toString().contains(GALE_SHARD),
                "Wind Bow consumes Sky Feathers and a Gale Shard");
        for (String table : List.of(CHEST, VAULT, VAULT_TOP))
            assertFalse(poolsEmitting(lootTable(table), FEATHER).isEmpty(), table + " supplies Sky Feathers");
        for (String table : List.of(VAULT, VAULT_TOP))
            assertFalse(poolsEmitting(lootTable(table), GALE_SHARD).isEmpty(), table + " supplies Gale Shards");
        JsonObject unlock = json("data/moreminecrafts/advancement/recipes/equipment/wind_archers_bow.json");
        assertTrue(unlock.getAsJsonObject("criteria").has("has_gale_shard"), "obtaining a Gale Shard keeps unlocking the Wind Bow recipe");
    }

    // ---- structure NBT ----------------------------------------------------------------------------------------------

    private record Template(int[] size, List<String> palette, List<Block> blocks) {
        long count(String id) { return blocks.stream().filter(b -> palette.get(b.state()).equals(id)).count(); }
        List<Block> of(String id) { return blocks.stream().filter(b -> palette.get(b.state()).equals(id)).toList(); }
    }

    private record Block(int x, int y, int z, int state, Map<String, Object> nbt) {}

    @SuppressWarnings("unchecked")
    private static Template template(String path) throws IOException {
        try (DataInputStream in = new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(bytes(path))))) {
            assertEquals(10, in.readByte());
            in.readUTF();
            Map<String, Object> root = (Map<String, Object>) readPayload(in, 10);
            List<Integer> size = (List<Integer>) root.get("size");
            List<String> palette = new ArrayList<>();
            for (Object state : (List<Object>) root.get("palette")) palette.add((String) ((Map<String, Object>) state).get("id"));
            List<Block> blocks = new ArrayList<>();
            for (Object entry : (List<Object>) root.get("blocks")) {
                Map<String, Object> e = (Map<String, Object>) entry;
                List<Integer> pos = (List<Integer>) e.get("pos");
                blocks.add(new Block(pos.get(0), pos.get(1), pos.get(2), (Integer) e.get("state"), (Map<String, Object>) e.get("nbt")));
            }
            return new Template(new int[]{size.get(0), size.get(1), size.get(2)}, palette, blocks);
        }
    }

    private static Object readPayload(DataInputStream in, int type) throws IOException {
        switch (type) {
            case 1: return in.readByte();
            case 2: return in.readShort();
            case 3: return in.readInt();
            case 4: return in.readLong();
            case 5: return in.readFloat();
            case 6: return in.readDouble();
            case 8: return in.readUTF();
            case 7: { int n = in.readInt(); byte[] b = new byte[n]; in.readFully(b); return b; }
            case 9: {
                int inner = in.readByte();
                int n = in.readInt();
                List<Object> list = new ArrayList<>();
                for (int i = 0; i < n; i++) list.add(readPayload(in, inner));
                return list;
            }
            case 10: {
                Map<String, Object> map = new TreeMap<>();
                for (int inner = in.readByte(); inner != 0; inner = in.readByte()) map.put(in.readUTF(), readPayload(in, inner));
                return map;
            }
            case 11: { int n = in.readInt(); int[] a = new int[n]; for (int i = 0; i < n; i++) a[i] = in.readInt(); return a; }
            case 12: { int n = in.readInt(); long[] a = new long[n]; for (int i = 0; i < n; i++) a[i] = in.readLong(); return a; }
            default: throw new IOException("unsupported NBT tag " + type);
        }
    }

    @Test void floatingRuinsHaveFourVariantsEachWithSpawnerVaultAndVisibleChests() throws Exception {
        for (String name : List.of("ruins_a", "ruins_b", "ruins_c", "ruins_d")) {
            Template t = template("data/moreminecrafts/structure/sky_islands/" + name + ".nbt");
            assertEquals(1, t.count("minecraft:trial_spawner"), name);
            assertEquals(1, t.count("minecraft:vault"), name);
            assertEquals(2, t.count("minecraft:chest"), name);
            assertEquals(1, t.count("minecraft:jigsaw"), name + ": sits on an island through exactly one socket");
        }
    }

    @Test void towerHasFiveToSevenFloorsWithOneOrTwoSpawnersEach() throws Exception {
        for (int floors = 5; floors <= 7; floors++) {
            Template t = template("data/moreminecrafts/structure/sky_islands/tower_" + floors + ".nbt");
            Map<Integer, Integer> spawners = new TreeMap<>(), vaults = new TreeMap<>(), chests = new TreeMap<>();
            for (Block b : t.of("minecraft:trial_spawner")) spawners.merge(b.y() / 7, 1, Integer::sum);
            for (Block b : t.of("minecraft:vault")) vaults.merge(b.y() / 7, 1, Integer::sum);
            for (Block b : t.of("minecraft:chest")) chests.merge(b.y() / 7, 1, Integer::sum);
            assertEquals(floors, spawners.size(), "tower_" + floors);
            for (int f = 0; f < floors; f++) {
                assertTrue(spawners.get(f) >= 1 && spawners.get(f) <= 2, "floor " + f + " spawners");
                assertTrue(vaults.get(f) >= 1 && chests.get(f) >= 1, "floor " + f + " has a vault and a chest");
            }
            assertTrue(vaults.get(floors - 1) > vaults.get(1) && chests.get(floors - 1) > chests.get(1), "crown holds the most containers");
            for (Block vault : t.of("minecraft:vault")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> config = (Map<String, Object>) vault.nbt().get("config");
                assertEquals(vault.y() / 7 == floors - 1 ? VAULT_TOP : VAULT, config.get("loot_table"));
            }
        }
    }

    @Test void templeIsLargeSingleStoreyWithArenaSpawnersVaultsAndChests() throws Exception {
        Template t = template("data/moreminecrafts/structure/sky_islands/temple.nbt");
        assertTrue(t.size()[0] >= 41 && t.size()[2] >= 21);
        assertTrue(t.count("minecraft:trial_spawner") >= 2 && t.count("minecraft:vault") >= 1 && t.count("minecraft:chest") >= 1);
        for (Block chest : t.of("minecraft:chest")) assertEquals(CHEST, chest.nbt().get("LootTable"));
        for (Block spawner : t.of("minecraft:trial_spawner")) {
            String config = (String) spawner.nbt().get("normal_config");
            assertTrue(config.startsWith("minecraft:trial_chamber/") || config.equals("moreminecrafts:sky_islands/shulker/normal"), config);
        }
    }

    private static final List<String> STYLES = List.of("snowy", "meadow", "forest", "badlands", "desert", "rocky");
    private static final List<String> SMALL = List.of("plain_small_a", "plain_small_b", "plain_small_c", "plain_small_d");
    private static final List<String> MEDIUM = List.of("plain_medium_a", "plain_medium_b", "plain_medium_c", "plain_medium_d", "plain_medium_e");
    private static final List<String> LARGE = List.of("large_a", "large_b", "large_c", "large_d", "large_e");
    private static final List<String> TOWER_LARGE = List.of("large_d", "large_e");

    @Test void everyIslandTemplateLoadsAndOnlyStructureIslandsCarryASocket() throws Exception {
        List<String> names = new ArrayList<>(List.of("ruins", "tower", "temple", "satellite_a", "satellite_b"));
        names.addAll(SMALL); names.addAll(MEDIUM); names.addAll(LARGE);
        for (String style : STYLES) {
            for (String name : names) {
                Template t = template("data/moreminecrafts/structure/sky_islands/" + style + "/" + name + ".nbt");
                boolean hasSocket = t.of("minecraft:jigsaw").stream().anyMatch(b -> String.valueOf(b.nbt().get("pool")).endsWith("_top"));
                if (name.equals("tower")) {
                    assertEquals(List.of(58, 97, 49), Arrays.stream(t.size()).boxed().toList(), style + "/tower composite bounds");
                    assertEquals(1, t.of("minecraft:jigsaw").size(), style + "/tower has one preserved root connector");
                    assertEquals("minecraft:empty", t.of("minecraft:jigsaw").getFirst().nbt().get("pool"), style + "/tower terminates legacy child socket");
                    assertEquals(16, t.count("minecraft:trial_spawner"), style + "/tower approved trial spawners");
                    assertEquals(8, t.count("minecraft:vault"), style + "/tower approved vaults");
                } else {
                    assertEquals(List.of("ruins", "temple").contains(name) || TOWER_LARGE.contains(name), hasSocket, style + "/" + name);
                }
            }
        }
    }

    @Test void startPoolsHoldSizeFamiliesInFourThreeThreeWithEqualWeightsAndNoDirectTower() throws Exception {
        for (String style : STYLES) {
            JsonObject pool = json("data/moreminecrafts/worldgen/template_pool/sky_islands/" + style + "/start.json");
            Map<String, Integer> weights = new HashMap<>();
            for (JsonElement element : pool.getAsJsonArray("elements")) {
                JsonObject e = element.getAsJsonObject();
                String location = e.getAsJsonObject("element").get("location").getAsString();
                weights.put(location.substring(location.lastIndexOf('/') + 1), e.get("weight").getAsInt());
            }
            assertFalse(weights.containsKey("tower"), style + ": the composite tower is never a start element");
            for (List<String> family : List.of(SMALL, MEDIUM, LARGE)) {
                assertEquals(1, family.stream().map(weights::get).distinct().count(), style + " equal weights inside " + family.getFirst());
            }
            int small = SMALL.stream().mapToInt(weights::get).sum(), medium = MEDIUM.stream().mapToInt(weights::get).sum(),
                    large = LARGE.stream().mapToInt(weights::get).sum();
            assertEquals(List.of(40, 30, 30), List.of(small, medium, large), style + " family totals (4:3:3)");
            assertEquals(6, weights.get("ruins")); assertEquals(2, weights.get("temple"));
            assertEquals(SMALL.size() + MEDIUM.size() + LARGE.size() + 2, weights.size(), style);
            JsonObject child = json("data/moreminecrafts/worldgen/template_pool/sky_islands/" + style + "/tower_top.json");
            assertEquals(1, child.getAsJsonArray("elements").size());
            // P3.12: the approved transition repoints meadow / forest to the heaven templates (same names, same pool structure); other themes keep the original tower
            String towerPath = java.util.Set.of("meadow", "forest").contains(style) ? "sky_islands/heaven/" + style + "/tower" : "sky_islands/" + style + "/tower";
            assertEquals("moreminecrafts:" + towerPath,
                    child.getAsJsonArray("elements").get(0).getAsJsonObject().getAsJsonObject("element").get("location").getAsString());
        }
    }

    @Test void islandFamiliesKeepTheirFootprintRanges() throws Exception {
        for (String style : STYLES) {
            for (String n : SMALL) {
                int[] s = template("data/moreminecrafts/structure/sky_islands/" + style + "/" + n + ".nbt").size();
                assertTrue(s[0] >= 23 && s[0] <= 27 && s[2] >= 23 && s[2] <= 27, style + "/" + n + " " + Arrays.toString(s));
            }
            for (String n : MEDIUM) {
                int[] s = template("data/moreminecrafts/structure/sky_islands/" + style + "/" + n + ".nbt").size();
                assertTrue(s[0] >= 43 && s[0] <= 47 && s[2] >= 33 && s[2] <= 37, style + "/" + n + " " + Arrays.toString(s));
            }
            for (String n : LARGE) {
                int[] s = template("data/moreminecrafts/structure/sky_islands/" + style + "/" + n + ".nbt").size();
                assertTrue(s[0] >= 96 && s[0] <= 160 && s[2] >= 96 && s[2] <= 160, style + "/" + n + " " + Arrays.toString(s));
            }
        }
    }

    /** The tower child (58 x 97 x 49, connector at local 36,16,24) must lie inside the large parent, on flat solid plateau. */
    @Test void towerSocketsExistOnlyOnLargeIslandsAndTheChildFitsWithGroundUnderTheTower() throws Exception {
        for (String style : STYLES) {
            for (String name : SMALL) assertEquals(0, towerSockets(style, name).size(), style + "/" + name);
            for (String name : MEDIUM) assertEquals(0, towerSockets(style, name).size(), style + "/" + name);
            for (String name : LARGE) {
                List<Block> sockets = towerSockets(style, name);
                assertEquals(TOWER_LARGE.contains(name) ? 1 : 0, sockets.size(), style + "/" + name);
                if (sockets.isEmpty()) continue;
                Template t = template("data/moreminecrafts/structure/sky_islands/" + style + "/" + name + ".nbt");
                Block socket = sockets.getFirst();
                int ox = socket.x() - 36, oy = socket.y() + 1 - 16, oz = socket.z() - 24;
                assertTrue(ox >= 0 && oy >= 0 && oz >= 0 && ox + 58 <= t.size()[0] && oy + 97 <= t.size()[1] && oz + 49 <= t.size()[2],
                        style + "/" + name + " tower child inside parent box");
                java.util.Set<Long> ground = new java.util.HashSet<>();
                for (Block b : t.blocks()) {
                    String id = t.palette().get(b.state());
                    if (b.y() == socket.y() + 1 && !id.equals("minecraft:air") && !id.equals("minecraft:water")) ground.add(((long) b.x() << 32) | b.z());
                }
                for (int x = ox; x < ox + 50; x++) for (int z = oz; z < oz + 49; z++)
                    assertTrue(ground.contains(((long) x << 32) | z), style + "/" + name + " plateau ground under tower at " + x + "," + z);
            }
        }
    }

    private static List<Block> towerSockets(String style, String name) throws IOException {
        Template t = template("data/moreminecrafts/structure/sky_islands/" + style + "/" + name + ".nbt");
        return t.of("minecraft:jigsaw").stream()
                .filter(b -> String.valueOf(b.nbt().get("pool")).equals("moreminecrafts:sky_islands/" + style + "/tower_top")).toList();
    }
}
