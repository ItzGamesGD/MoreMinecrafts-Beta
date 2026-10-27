package dev.itzgamesgd.moreminecrafts.bounty;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

/** BC-04: the contract screen names every generatable exploration target instead of showing its registry ID. */
class ContractStructureNamesTest {
    private static JsonObject lang(String code) throws IOException {
        var stream = ContractStructureNamesTest.class.getClassLoader()
                .getResourceAsStream("assets/moreminecrafts/lang/" + code + ".json");
        assertNotNull(stream, code);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    @Test void everyExplorationTargetHasANameInEnglishAndKorean() throws IOException {
        for (String code : List.of("en_us", "ko_kr")) {
            JsonObject json = lang(code);
            for (String structure : ContractCatalog.EXPLORATION_STRUCTURES) {
                String key = ContractCatalog.structureNameKey(structure);
                assertTrue(json.has(key), code + " is missing " + key);
                String name = json.get(key).getAsString();
                assertFalse(name.isBlank() || name.contains(":"), code + " " + key + " is not a readable name: " + name);
            }
        }
    }

    @Test void keyFollowsTheRegistryId() {
        assertEquals("moreminecrafts.contract.structure.minecraft.trial_chambers",
                ContractCatalog.structureNameKey("minecraft:trial_chambers"));
    }
}
