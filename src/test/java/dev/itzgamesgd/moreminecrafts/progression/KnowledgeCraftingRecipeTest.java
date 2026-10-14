package dev.itzgamesgd.moreminecrafts.progression;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Contract test for all 13 human-approved knowledge-gated recipe families. */
final class KnowledgeCraftingRecipeTest {
    private static final Path ROOT =
            Path.of("src/main/resources/data/moreminecrafts/recipe/knowledge");
    private static final Set<Integer> APPROVED =
            Set.of(4, 14, 15, 17, 18, 19, 22, 25, 30, 42, 44, 57, 60);

    private static JsonObject json(Path file) throws IOException {
        try (var reader = Files.newBufferedReader(file)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    @Test void exactlyThirteenApprovedKnowledgeNumbersHaveRecipes() throws IOException {
        assertTrue(Files.isDirectory(ROOT), "Knowledge recipes folder must exist");
        var ids = new HashSet<Integer>();
        var names = new HashSet<String>();
        try (var files = Files.list(ROOT)) {
            for (var file : files.filter(p -> p.toString().endsWith(".json")).toList()) {
                String stem = file.getFileName().toString().replace(".json", "");
                assertTrue(stem.startsWith("knowledge") && stem.length() > 11);
                ids.add(Integer.parseInt(stem.substring(9, 11)));
                assertTrue(names.add(stem), "Duplicate recipe file basename");
            }
        }
        assertEquals(APPROVED, ids);
        assertEquals(35, names.size(), "12 stair variants + 12 trapdoors + 11 others");
    }

    @Test void allShapedRecipesUseValidVanillaSpacesAndIdenticalRowLengths() throws IOException {
        try (var files = Files.list(ROOT)) {
            for (var path : files.filter(p -> p.toString().endsWith(".json")).toList()) {
                JsonObject data = json(path);
                if (!data.get("type").getAsString().equals("minecraft:crafting_shaped")) continue;
                JsonArray rows = data.getAsJsonArray("pattern");
                int length = rows.get(0).getAsString().length();
                assertTrue(length >= 1 && length <= 3);
                Set<Character> used = new HashSet<>();
                for (var row : rows) {
                    String text = row.getAsString();
                    assertEquals(length, text.length(), path + ": mismatched width");
                    assertFalse(text.contains("."), path + ": dots are NOT empty slots in 26.3");
                    for (char c : text.toCharArray()) if (c != ' ') used.add(c);
                }
                var key = data.getAsJsonObject("key");
                Set<Character> available = new HashSet<>();
                for (var e : key.entrySet()) available.add(e.getKey().charAt(0));
                assertEquals(available, used, path + ": key/pattern symbol mismatch");
            }
        }
    }

    @Test void stairsUseExactUserApprovedStaggeredFourPlankLayout() throws IOException {
        try (var files = Files.list(ROOT)) {
            var stairs = files.filter(p -> p.toString().contains("knowledge04_")).toList();
            assertEquals(12, stairs.size());
            for (Path path : stairs) {
                JsonObject data = json(path);
                assertEquals("PP ", data.getAsJsonArray("pattern").get(0).getAsString());
                assertEquals(" PP", data.getAsJsonArray("pattern").get(1).getAsString());
                assertEquals(4, data.getAsJsonObject("result").get("count").getAsInt());
            }
        }
    }

    @Test void breadUsesTwoAdjacentWheatAndVanillaShapeTranslation() throws IOException {
        var bread = json(ROOT.resolve("knowledge30_bread.json"));
        assertEquals("WW", bread.getAsJsonArray("pattern").get(0).getAsString());
        assertEquals("minecraft:bread", bread.getAsJsonObject("result").get("id").getAsString());
    }

    @Test void originalSavedKnowledgeStateDoesNotRequirePurchases() {
        ResearchProgressState empty = ResearchProgressState.empty();
        assertEquals(0, empty.get("knowledge.57"));
        assertEquals(0, empty.get("research.wall_jump"));
        ResearchProgressState saved = empty.with("knowledge.57", 1);
        assertEquals(1, saved.get("knowledge.57"));
        assertEquals(0, saved.get("research.wall_jump"));
        assertEquals(0, empty.get("knowledge.57"), "Persistent snapshots must be immutable");
    }
}
