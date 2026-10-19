package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Player-facing quest text must never fall back to a registry id. */
class QuestLocalizationTest {
    private static final String ENTITY_PREFIX = "entity.moreminecrafts.";

    private static JsonObject lang(String code) throws IOException {
        try (var reader = new InputStreamReader(QuestLocalizationTest.class.getResourceAsStream(
                "/assets/moreminecrafts/lang/" + code + ".json"), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static Set<String> entityKeys(JsonObject lang) {
        Set<String> keys = new TreeSet<>();
        for (String key : lang.keySet()) if (key.startsWith(ENTITY_PREFIX)) keys.add(key);
        return keys;
    }

    /** Every custom entity type registered in CustomMobCatalog. */
    private static Set<String> registeredMobPaths() throws IOException {
        String source = Files.readString(Path.of("src/main/java/dev/itzgamesgd/moreminecrafts/mob/CustomMobCatalog.java"));
        Matcher matcher = Pattern.compile("= type\\(\"([a-z_]+)\"").matcher(source);
        Set<String> paths = new TreeSet<>();
        while (matcher.find()) paths.add(matcher.group(1));
        return paths;
    }

    @Test
    void koreanAndEnglishDefineTheSameCustomEntityNames() throws IOException {
        assertEquals(entityKeys(lang("en_us")), entityKeys(lang("ko_kr")));
    }

    @Test
    void everyRegisteredCustomMobHasAFinishedNameInBothLanguages() throws IOException {
        Set<String> paths = registeredMobPaths();
        assertTrue(paths.size() >= 40, "registry parse found " + paths.size());
        for (String code : List.of("en_us", "ko_kr")) {
            JsonObject lang = lang(code);
            for (String path : paths) {
                String key = ENTITY_PREFIX + path;
                assertTrue(lang.has(key), code + " missing " + key);
                String name = lang.get(key).getAsString();
                assertFalse(name.isBlank(), code + " blank " + key);
                assertFalse(name.contains("moreminecrafts:") || name.contains("_"), code + " raw id in " + key);
            }
        }
        JsonObject ko = lang("ko_kr");
        for (String path : paths) {
            assertTrue(ko.get(ENTITY_PREFIX + path).getAsString().codePoints().anyMatch(c -> c >= 0xAC00 && c <= 0xD7A3),
                    "Korean name is not Korean: " + path);
        }
    }

    @Test
    void everyQuestTargetEntityResolvesInBothLanguages() throws IOException {
        List<String> ids = new ArrayList<>();
        for (var quest : XpQuestCatalogData.catalog().quests()) {
            if (!quest.tracker().kind().startsWith("stat_killed")) continue;
            for (String entry : quest.tracker().entries()) {
                String[] parts = entry.split("\\|");
                for (int i = entry.contains("|") ? 1 : 0; i < parts.length; i++) ids.add(parts[i]);
            }
        }
        assertFalse(ids.isEmpty());
        for (String code : List.of("en_us", "ko_kr")) {
            JsonObject lang = lang(code);
            for (String id : ids) {
                if (!id.startsWith("moreminecrafts:")) continue;
                assertTrue(lang.has("entity." + id.replace(':', '.')), code + " missing quest target name " + id);
            }
        }
    }

    @Test
    void questTooltipUiKeysExistInBothLanguages() throws IOException {
        for (String code : List.of("en_us", "ko_kr")) {
            JsonObject lang = lang(code);
            for (String key : List.of("moreminecrafts.ui.quest.tooltip.title", "moreminecrafts.ui.quest.tooltip.choose",
                    "moreminecrafts.ui.quest.group.candidates", "moreminecrafts.ui.quest.unknown_entity",
                    "moreminecrafts.ui.quest.reward", "moreminecrafts.ui.quest.group.desert",
                    "moreminecrafts.ui.quest.group.swamp", "moreminecrafts.ui.quest.group.snowy",
                    "moreminecrafts.ui.quest.group.fungal", "moreminecrafts.ui.quest.group.wild_slime")) {
                assertTrue(lang.has(key), code + " missing " + key);
            }
        }
    }
}
