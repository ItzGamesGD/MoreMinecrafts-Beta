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
        for (String tag : Set.of("undead", "zombies", "skeletons", "raiders", "illager", "arthropod",
                "sensitive_to_smite", "sensitive_to_bane_of_arthropods", "aquatic", "beehive_inhabitants",
                "burn_in_daylight", "inverted_healing_and_harm", "wither_friends", "immune_to_oozing",
                "frog_food", "axolotl_always_hostiles", "axolotl_hunt_targets", "immune_to_infested")) {
            assertTrue(Files.isRegularFile(Path.of("src/main/resources/data/minecraft/tags/entity_type/" + tag + ".json")),
                    "Missing vanilla semantic tag extension: " + tag);
        }
        assertTrue(Files.readString(Path.of("docs/VANILLA_MODEL_CUSTOM_MOB_ROSTER.md"))
                .contains("VANILLA_MODEL_CUSTOM_MOB_ROSTER_IMPLEMENTATION.md"));
    }
}
