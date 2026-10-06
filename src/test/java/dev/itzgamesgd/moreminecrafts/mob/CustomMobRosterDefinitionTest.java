package dev.itzgamesgd.moreminecrafts.mob;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class CustomMobRosterDefinitionTest {
    @Test
    void allFortyTypesHaveUniqueLocalizedNamesAndTextureBacklogEntries() throws Exception {
        String catalog = Files.readString(Path.of("src/main/java/dev/itzgamesgd/moreminecrafts/mob/CustomMobCatalog.java"));
        String language = Files.readString(Path.of("src/main/resources/assets/moreminecrafts/lang/en_us.json"));
        String backlog = Files.readString(Path.of("docs/CUSTOM_MOB_RESOURCE_BACKLOG.md"));
        Matcher matcher = Pattern.compile("type\\(\"([a-z0-9_]+)\"").matcher(catalog);
        Set<String> ids = new HashSet<>();
        while (matcher.find()) {
            String id = matcher.group(1);
            assertTrue(ids.add(id), "Duplicate entity identifier: " + id);
            assertTrue(language.contains("\"entity.moreminecrafts." + id + "\""),
                    "Missing localized entity name: " + id);
            assertTrue(backlog.contains("textures/entity/mobs/" + id + ".png"),
                    "Missing per-mob texture backlog path: " + id);
            Path lootPath = Path.of("src/main/resources/data/moreminecrafts/loot_table/entities/" + id + ".json");
            assertTrue(Files.isRegularFile(lootPath), "Missing vanilla-family loot alias: " + id);
            assertTrue(Files.readString(lootPath).contains("minecraft:entities/"),
                    "Custom mob loot must preserve its vanilla base-family drops: " + id);
        }
        assertEquals(40, ids.size(), "The implementation must retain the approved forty-entry roster");
        String stats = Files.readString(Path.of("src/main/java/dev/itzgamesgd/moreminecrafts/mob/CustomMobStats.java"));
        assertTrue(stats.contains("type == CustomMobCatalog.SWAMP_ZOMBIE")
                        && stats.contains("type == CustomMobCatalog.CAVE_CREEPER"),
                "Custom stat profiles must be scoped to exact MoreMinecrafts EntityType identities");
        assertTrue(!stats.contains("getKey(mob.getType()).getPath()"),
                "Custom stat profiles must never dispatch on an un-namespaced registry path");
        assertTrue(stats.contains("!APPLIED.add(mob)"),
                "Custom stat application must explicitly guard against repeated finalization");
        assertTrue(!stats.contains("Attributes.SCALE"),
                "Entity size must have one source of truth; stat finalization must not rescale sized EntityTypes");
        assertTrue(catalog.contains("Slime.checkSlimeSpawnRules")
                        && catalog.contains("MagmaCube.checkMagmaCubeSpawnRules")
                        && catalog.contains("Drowned.checkDrownedSpawnRules")
                        && catalog.contains("Piglin.checkPiglinSpawnRules")
                        && catalog.contains("Wolf.checkWolfSpawnRules")
                        && catalog.contains("Bat.checkBatSpawnRules")
                        && catalog.contains("Animal.checkAnimalSpawnRules"),
                "Natural spawn predicates must preserve family-specific vanilla rules where available");
        assertTrue(catalog.contains("spec.type.getCategory()"),
                "Biome spawn registration must use each EntityType's family population category");
        for (String tag : Set.of("undead", "zombies", "skeletons", "raiders", "illager", "arthropod",
                "sensitive_to_smite", "sensitive_to_bane_of_arthropods", "aquatic", "beehive_inhabitors",
                "burn_in_daylight", "inverted_healing_and_harm", "wither_friends", "immune_to_oozing",
                "frog_food", "axolotl_always_hostiles", "axolotl_hunt_targets", "immune_to_infested",
                "no_anger_from_wind_charge", "powder_snow_walkable_mobs", "fall_damage_immune",
                "freeze_hurts_extra_types", "non_controlling_rider")) {
            assertTrue(Files.isRegularFile(Path.of("src/main/resources/data/minecraft/tags/entity_type/" + tag + ".json")),
                    "Missing vanilla semantic tag extension: " + tag);
        }
        assertTrue(Files.readString(Path.of("src/main/resources/data/moreminecrafts/loot_table/entities/bandit.json"))
                        .contains("\"minecraft:entities/pillager\""),
                "Bandit must preserve Pillager-family loot rather than Vindicator loot");
        assertTrue(catalog.contains("new SmallCaveSpider") && catalog.contains("new SwampCaveSpider"),
                "Small and Swamp Spider must use their explicit Cave Spider-family behavior classes");
        assertTrue(!catalog.contains("WILD_PIGLIN = type(\"wild_piglin\", Piglin::new, 0.6f, 1.95f, true)"),
                "Wild Piglin must not gain non-vanilla fire immunity");
        for (String biomeTag : Set.of("snowy_habitat", "fungal_habitat", "desert_habitat", "wild_slime_habitat")) {
            assertTrue(Files.isRegularFile(Path.of("src/main/resources/data/moreminecrafts/tags/worldgen/biome/" + biomeTag + ".json")),
                    "Missing approved habitat biome tag: " + biomeTag);
        }
        assertTrue(catalog.contains("add(FROZEN_ZOMBIE, Zombie.createAttributes(), SNOWY_HABITAT")
                        && catalog.contains("add(FUNGAL_ZOMBIE, Zombie.createAttributes(), FUNGAL_HABITAT")
                        && catalog.contains("add(DESERT_SPIDER, Spider.createAttributes(), DESERT_HABITAT")
                        && catalog.contains("add(ICE_SPIDER, Spider.createAttributes(), SNOWY_HABITAT")
                        && catalog.contains("add(WILD_SLIME, net.minecraft.world.entity.monster.Monster.createMonsterAttributes(), WILD_SLIME_HABITAT")
                        && catalog.contains("add(SAND_SLIME, net.minecraft.world.entity.monster.Monster.createMonsterAttributes(), DESERT_HABITAT"),
                "Approved biome identities must not regress to mountain/jungle/badlands shortcuts");
        assertTrue(Files.readString(Path.of("docs/VANILLA_MODEL_CUSTOM_MOB_ROSTER.md"))
                .contains("VANILLA_MODEL_CUSTOM_MOB_ROSTER_IMPLEMENTATION.md"));
    }
}
