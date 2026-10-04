package dev.itzgamesgd.moreminecrafts.progression;

import com.google.gson.JsonParser;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EnhancementStoneRecipeTest {
    @Test void craftingRecipeUsesTheConfirmedThreeByThreeRecipeAndYield() throws Exception {
        try (InputStream stream = getClass().getClassLoader()
                .getResourceAsStream("data/moreminecrafts/recipe/enhancement_stone.json")) {
            assertNotNull(stream);
            var recipe = JsonParser.parseString(new String(stream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
            assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
            assertEquals(List.of("RDR", "DLD", "RDR"),
                    List.of(recipe.getAsJsonArray("pattern").get(0).getAsString(),
                            recipe.getAsJsonArray("pattern").get(1).getAsString(),
                            recipe.getAsJsonArray("pattern").get(2).getAsString()));
            var key = recipe.getAsJsonObject("key");
            assertEquals("minecraft:redstone", key.get("R").getAsString());
            assertEquals("minecraft:diamond", key.get("D").getAsString());
            assertEquals("minecraft:lapis_lazuli", key.get("L").getAsString());
            assertEquals("moreminecrafts:enhancement_stone", recipe.getAsJsonObject("result").get("id").getAsString());
            assertEquals(16, recipe.getAsJsonObject("result").get("count").getAsInt());
            assertFalse(recipe.has("ingredients"));
        }
    }
}
