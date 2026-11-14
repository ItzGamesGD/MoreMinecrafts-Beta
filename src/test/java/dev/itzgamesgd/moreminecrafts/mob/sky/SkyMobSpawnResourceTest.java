package dev.itzgamesgd.moreminecrafts.mob.sky;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.itzgamesgd.moreminecrafts.skytower.SkyTowerTrialSpawners;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

/** SKY-MOB §15.6 data contract: sky-island spawn list (P1/P2) and Sky Tower combinations (P4-P7, C2). */
class SkyMobSpawnResourceTest {
    private static final Path DATA = Path.of("src/main/resources/data/moreminecrafts");
    private static final Set<String> GROUP_C = Set.of("moreminecrafts:cloud_wraith", "moreminecrafts:skyguard_archer",
            "moreminecrafts:crystal_bug", "moreminecrafts:cloud_slime", "moreminecrafts:fallen_sentinel");
    private static final Map<String, List<String>> COMBOS = Map.of(
            "a", List.of("cloud_wraith", "cloud_slime"),
            "b", List.of("cloud_wraith", "skyguard_archer"),
            "c", List.of("crystal_bug", "skyguard_archer"),
            "d", List.of("fallen_sentinel", "cloud_wraith", "crystal_bug"),
            "e", List.of("fallen_sentinel", "skyguard_archer", "cloud_slime"));

    private static JsonObject json(Path path) throws Exception {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }

    private static int[] count(JsonElement count) {
        if (count.isJsonPrimitive()) return new int[]{count.getAsInt(), count.getAsInt()};
        JsonObject u = count.getAsJsonObject();
        assertEquals("minecraft:uniform", u.get("type").getAsString());
        return new int[]{u.get("min_inclusive").getAsInt(), u.get("max_inclusive").getAsInt()};
    }

    @Test void everySkyIslandUsesTheApprovedMonsterList() throws Exception {
        Map<String, int[]> expected = new LinkedHashMap<>();
        expected.put("moreminecrafts:cloud_wraith", new int[]{24, 1, 2});
        expected.put("moreminecrafts:skyguard_archer", new int[]{18, 1, 2});
        expected.put("moreminecrafts:crystal_bug", new int[]{20, 1, 1});
        expected.put("moreminecrafts:cloud_slime", new int[]{24, 1, 2});
        expected.put("moreminecrafts:fallen_sentinel", new int[]{14, 1, 1});
        expected.put("minecraft:zombie", new int[]{10, 4, 4});
        expected.put("minecraft:skeleton", new int[]{10, 4, 4});
        expected.put("minecraft:spider", new int[]{10, 4, 4});
        int structures = 0;
        try (var files = Files.list(DATA.resolve("worldgen/structure"))) {
            for (Path p : files.filter(f -> f.getFileName().toString().startsWith("sky_island_")).toList()) {
                structures++;
                JsonObject overrides = json(p).getAsJsonObject("spawn_overrides");
                assertEquals(Set.of("monster"), overrides.keySet(), p + ": only the monster category is overridden");
                JsonObject monster = overrides.getAsJsonObject("monster");
                assertEquals("piece", monster.get("bounding_box").getAsString(), p + ": R1 saved piece boxes");
                Map<String, int[]> actual = new LinkedHashMap<>();
                for (JsonElement e : monster.getAsJsonArray("spawns")) {
                    JsonObject o = e.getAsJsonObject();
                    int[] c = count(o.get("count"));
                    assertNull(actual.put(o.get("type").getAsString(), new int[]{o.get("weight").getAsInt(), c[0], c[1]}));
                }
                // Group B variants (2026-11-14 owner task; weights fixed 2026-11-14 follow-up): all 10, every island type.
                Map<String, int[]> withGroupB = new LinkedHashMap<>(expected);
                for (String variant : List.of("weathered_zombie", "sky_archer", "rock_creeper", "pale_enderman", "azure_slime"))
                    withGroupB.put("moreminecrafts:" + variant, new int[]{10, 1, 2});
                assertEquals(withGroupB.keySet(), actual.keySet(),
                        p + ": Q31 list (no vanilla creeper/enderman/witch) plus the five group B variants");
                for (var entry : withGroupB.entrySet())
                    assertArrayEquals(entry.getValue(), actual.get(entry.getKey()), p + " " + entry.getKey());
            }
        }
        assertEquals(8, structures);
    }

    @Test void layoutMatchesTheApprovedFloorPlan() {
        String[][] floors = {{"a", "a", "b", "a"}, {"b", "c", "c", "b"}, {"c", "d", "d", "e"}, {"d", "e", "e", "e"}};
        for (int i = 0; i < 16; i++) assertEquals(floors[i / 4][i % 4], SkyTowerTrialSpawners.combo(i), "spawner " + i);
        assertThrows(IllegalArgumentException.class, () -> SkyTowerTrialSpawners.combo(16));
    }

    @Test void combinationsUseOnlyGroupCWithEqualPerMobWeights() throws Exception {
        JsonObject vanillaNormal = JsonParser.parseString("""
                {"simultaneous_mobs":3.0,"simultaneous_mobs_added_per_player":0.5,"ticks_between_spawn":20}""").getAsJsonObject();
        for (var combo : COMBOS.entrySet()) {
            for (String mode : List.of("normal", "ominous")) {
                Path path = DATA.resolve("trial_spawner/sky_tower/combo_" + combo.getKey() + "/" + mode + ".json");
                JsonObject cfg = json(path);
                // T3: vanilla trial_chamber/melee/zombie budget, timing and per-player scaling are kept; nothing else is set.
                Set<String> keys = mode.equals("normal")
                        ? Set.of("simultaneous_mobs", "simultaneous_mobs_added_per_player", "spawn_potentials", "ticks_between_spawn")
                        : Set.of("loot_tables_to_eject", "simultaneous_mobs", "simultaneous_mobs_added_per_player", "spawn_potentials", "ticks_between_spawn");
                assertEquals(keys, cfg.keySet(), path.toString());
                for (String k : vanillaNormal.keySet()) assertEquals(vanillaNormal.get(k), cfg.get(k), path + " " + k);
                if (mode.equals("ominous")) {
                    JsonArray eject = cfg.getAsJsonArray("loot_tables_to_eject");
                    assertEquals(2, eject.size());
                    assertEquals("minecraft:spawners/ominous/trial_chamber/key", eject.get(0).getAsJsonObject().get("data").getAsString());
                    assertEquals(3, eject.get(0).getAsJsonObject().get("weight").getAsInt());
                    assertEquals("minecraft:spawners/ominous/trial_chamber/consumables", eject.get(1).getAsJsonObject().get("data").getAsString());
                    assertEquals(7, eject.get(1).getAsJsonObject().get("weight").getAsInt());
                }
                Map<String, Integer> perMob = new TreeMap<>();
                Map<Integer, Integer> slimeSizes = new TreeMap<>();
                for (JsonElement e : cfg.getAsJsonArray("spawn_potentials")) {
                    JsonObject data = e.getAsJsonObject().getAsJsonObject("data");
                    assertFalse(data.has("equipment"), path + ": O1 no random equipment for group C");
                    JsonObject entity = data.getAsJsonObject("entity");
                    String id = entity.get("id").getAsString();
                    assertTrue(GROUP_C.contains(id), path + ": T4 group C only, got " + id);
                    int weight = e.getAsJsonObject().get("weight").getAsInt();
                    perMob.merge(id, weight, Integer::sum);
                    if (id.equals("moreminecrafts:cloud_slime")) slimeSizes.put(entity.get("Size").getAsInt(), weight);
                    else assertEquals(Set.of("id"), entity.keySet(), path + ": vanilla finalizeSpawn path kept");
                }
                Map<String, Integer> equal = new TreeMap<>();
                for (String m : combo.getValue()) equal.put("moreminecrafts:" + m, 4);
                assertEquals(equal, perMob, path + ": Q86 equal weight per mob");
                if (combo.getValue().contains("cloud_slime"))
                    assertEquals(Map.of(1, 3, 3, 1), slimeSizes, path + ": P6 medium(size 2):large(size 4) = 3:1");
            }
        }
    }
}
