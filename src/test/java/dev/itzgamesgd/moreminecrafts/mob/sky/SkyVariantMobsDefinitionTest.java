package dev.itzgamesgd.moreminecrafts.mob.sky;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/** Group B sky-island vanilla variants: data, identity separation and texture contract. */
class SkyVariantMobsDefinitionTest {
    private static final Path RES = Path.of("src/main/resources");
    private static final Path JAVA = Path.of("src/main/java/dev/itzgamesgd/moreminecrafts");
    private static final List<String> IDS =
            List.of("weathered_zombie", "sky_archer", "rock_creeper", "pale_enderman", "azure_slime");
    private static final Map<String, String> VANILLA = Map.of("weathered_zombie", "zombie", "sky_archer", "skeleton",
            "rock_creeper", "creeper", "pale_enderman", "enderman", "azure_slime", "slime");

    private static JsonObject json(String relative) throws Exception {
        return JsonParser.parseString(Files.readString(RES.resolve(relative))).getAsJsonObject();
    }

    private static Set<String> tag(String path) throws Exception {
        Set<String> out = new HashSet<>();
        for (JsonElement e : json(path).getAsJsonArray("values")) out.add(e.getAsString());
        return out;
    }

    private static Set<String> registered(String javaFile, String pattern) throws Exception {
        Matcher m = Pattern.compile(pattern).matcher(Files.readString(JAVA.resolve(javaFile)));
        Set<String> ids = new HashSet<>();
        while (m.find()) ids.add(m.group(1));
        return ids;
    }

    @Test
    void identifiersAreSeparateFromGroupCAndTheFortyEntryCatalog() throws Exception {
        Set<String> groupB = registered("mob/sky/SkyVariantMobs.java", "register\\(\"([a-z0-9_]+)\"");
        Set<String> groupC = registered("mob/sky/SkyNormalMobs.java", "register\\(\"([a-z0-9_]+)\"");
        Set<String> catalog = registered("mob/CustomMobCatalog.java", "type\\(\"([a-z0-9_]+)\"");
        assertEquals(Set.copyOf(IDS), groupB);
        assertEquals(40, catalog.size(), "the approved forty-entry catalog is untouched");
        for (String id : IDS) {
            assertFalse(groupC.contains(id), "no group C collision: " + id);
            assertFalse(catalog.contains(id), "no catalog collision: " + id);
        }
        assertTrue(groupC.contains("skyguard_archer") && !groupB.contains("skyguard_archer"),
                "R1: group B Sky Archer is not the group C Skyguard Archer");
    }

    @Test
    void namesLootAndMarkerTag() throws Exception {
        JsonObject en = json("assets/moreminecrafts/lang/en_us.json");
        JsonObject ko = json("assets/moreminecrafts/lang/ko_kr.json");
        Map<String, String> english = Map.of("weathered_zombie", "Weathered Zombie", "sky_archer", "Sky Archer",
                "rock_creeper", "Rock Creeper", "pale_enderman", "Pale Enderman", "azure_slime", "Azure Slime");
        Set<String> marker = tag("data/moreminecrafts/tags/entity_type/sky_variant_mobs.json");
        for (String id : IDS) {
            assertEquals(english.get(id), en.get("entity.moreminecrafts." + id).getAsString());
            assertTrue(ko.has("entity.moreminecrafts." + id), "ko_kr name: " + id);
            assertTrue(marker.contains("moreminecrafts:" + id));
            JsonObject loot = json("data/moreminecrafts/loot_table/entities/" + id + ".json");
            JsonArray pools = loot.getAsJsonArray("pools");
            JsonObject base = pools.get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject();
            assertEquals("minecraft:loot_table", base.get("type").getAsString());
            assertEquals("minecraft:entities/" + VANILLA.get(id), base.get("value").getAsString(), "vanilla alias " + id);
            assertEquals("moreminecrafts:entities/" + id, loot.get("random_sequence").getAsString());
        }
        assertBonus("weathered_zombie", Set.of("minecraft:iron_nugget"), 0, 1);
        assertBonus("sky_archer", Set.of("minecraft:feather"), 0, 1);
        assertBonus("rock_creeper", Set.of("minecraft:cobblestone", "minecraft:andesite"), 0, 2);
        assertEquals(1, json("data/moreminecrafts/loot_table/entities/pale_enderman.json").getAsJsonArray("pools").size());
        assertEquals(1, json("data/moreminecrafts/loot_table/entities/azure_slime.json").getAsJsonArray("pools").size());
    }

    private static void assertBonus(String id, Set<String> items, int min, int max) throws Exception {
        JsonArray pools = json("data/moreminecrafts/loot_table/entities/" + id + ".json").getAsJsonArray("pools");
        assertEquals(2, pools.size(), id + ": vanilla pool + one bonus pool");
        Set<String> seen = new HashSet<>();
        for (JsonElement e : pools.get(1).getAsJsonObject().getAsJsonArray("entries")) {
            JsonObject entry = e.getAsJsonObject();
            seen.add(entry.get("name").getAsString());
            JsonObject count = entry.getAsJsonArray("modifier").get(0).getAsJsonObject().getAsJsonObject("count");
            assertEquals(min, count.get("min").getAsInt(), id);
            assertEquals(max, count.get("max").getAsInt(), id);
        }
        assertEquals(items, seen, id + " bonus items");
    }

    @Test
    void vanillaFamilyTagsAndFallImmunity() throws Exception {
        String t = "data/minecraft/tags/entity_type/";
        for (String undead : List.of("weathered_zombie", "sky_archer")) {
            for (String vanillaTag : List.of("undead", "burn_in_daylight", "sensitive_to_smite"))
                assertTrue(tag(t + vanillaTag + ".json").contains("moreminecrafts:" + undead), undead + " in " + vanillaTag);
        }
        assertTrue(tag(t + "zombies.json").contains("moreminecrafts:weathered_zombie"));
        assertTrue(tag(t + "skeletons.json").contains("moreminecrafts:sky_archer"));
        assertTrue(tag(t + "fall_damage_immune.json").contains("moreminecrafts:azure_slime"));
        for (String notUndead : List.of("rock_creeper", "pale_enderman", "azure_slime"))
            assertFalse(tag(t + "undead.json").contains("moreminecrafts:" + notUndead));
        assertFalse(tag(t + "freeze_immune_entity_types.json").contains("moreminecrafts:sky_archer"),
                "only the Stray conversion is blocked; freezing stays vanilla");
    }

    @Test
    void neverAddedToBiomeSpawnLists() throws Exception {
        String source = Files.readString(JAVA.resolve("mob/sky/SkyVariantMobs.java"));
        assertFalse(source.contains("BiomeModifications"), "§15.6 P2: sky-island-only isolated list");
        assertTrue(source.contains("SkyIslandRegion.isIsland") && source.contains("CustomMobCatalog.skyIslandSurface"));
    }

    @Test
    void provisionalMultipliersAreRecorded() throws Exception {
        String stats = Files.readString(JAVA.resolve("mob/CustomMobStats.java"));
        assertTrue(stats.contains("case \"weathered_zombie\" -> { health = 1.10; speed = 0.95; damage = 1.05; }"));
        assertTrue(stats.contains("case \"sky_archer\" -> { health = 0.95; speed = 1.05; }"));
        assertTrue(stats.contains("case \"rock_creeper\" -> { health = 1.15; speed = 0.95; }"));
        assertTrue(stats.contains("case \"pale_enderman\" -> { health = 0.90; speed = 1.05; }"));
        assertTrue(stats.contains("type == SkyVariantMobs.WEATHERED_ZOMBIE"), "exact EntityType identity dispatch");
        assertEquals(0.85F, SkyVariantMobs.ROCK_CREEPER_EXPLOSION_MULTIPLIER);
        assertEquals(1.25D, SkyVariantMobs.AZURE_SLIME_JUMP_MULTIPLIER);
    }

    @Test
    void texturesUseVanillaSizedLayouts() throws Exception {
        Map<String, int[]> expected = Map.of("weathered_zombie", new int[]{64, 64},
                "weathered_zombie_baby", new int[]{64, 64}, "sky_archer", new int[]{64, 32},
                "rock_creeper", new int[]{64, 32}, "pale_enderman", new int[]{64, 32},
                "pale_enderman_eyes", new int[]{64, 32}, "azure_slime", new int[]{64, 32});
        Path dir = RES.resolve("assets/moreminecrafts/textures/entity/sky_variants");
        Set<String> actual = new HashSet<>();
        try (var files = Files.list(dir)) {
            files.forEach(f -> actual.add(f.getFileName().toString().replace(".png", "")));
        }
        assertEquals(expected.keySet(), actual, "every routed renderer texture, nothing unused");
        for (var entry : expected.entrySet()) {
            var image = ImageIO.read(dir.resolve(entry.getKey() + ".png").toFile());
            assertEquals(entry.getValue()[0], image.getWidth(), entry.getKey());
            assertEquals(entry.getValue()[1], image.getHeight(), entry.getKey());
        }
    }
}
