package dev.itzgamesgd.moreminecrafts.accessory;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Source/resource-level guards. The roll itself is vanilla LootItemRandomChanceCondition (chance 0.20F, one roll,
 * count exactly 1) evaluated once per entity death, so there is no custom RNG, per-player roll or event handler to
 * inject; GameTest verification of an actual kill is NOT RUN.
 */
class TombGuardianGravityHeartTest {
    private static String read(String p) throws Exception { return Files.readString(Path.of(p)); }
    private static final String SRC = "src/main/java/dev/itzgamesgd/moreminecrafts/accessory/TombGuardianGravityHeart.java";

    @Test void twentyPercentCountOneOnTombGuardianTableOnly() throws Exception {
        assertEquals(0.20F, TombGuardianGravityHeart.CHANCE);
        assertEquals(1, TombGuardianGravityHeart.COUNT);
        assertEquals("moreminecrafts:entities/tomb_guardian", TombGuardianGravityHeart.LOOT_TABLE);
        String s = read(SRC);
        assertEquals(1, s.split("table\\.withPool\\(", -1).length - 1);
        assertTrue(s.contains("HEART_OF_GRAVITY"));
        assertTrue(s.contains("randomChance(CHANCE)"));
        assertTrue(s.contains("setRolls(Holder.direct(new ConstantValue(1)))"));
    }

    @Test void sharedWorldDropWithNoPerPlayerOrEventLogic() throws Exception {
        String s = read(SRC);
        for (String f : new String[]{"ServerPlayer", "AFTER_DEATH", "getInventory", "spawnAtLocation", "ItemEntity", "summon"})
            assertFalse(s.contains(f), f);
    }

    @Test void independentFromAncientHeartPoolWhichStaysUnchanged() throws Exception {
        // The separate pool carries its own condition, so both pools can succeed on the same death.
        String json = read("src/main/resources/data/moreminecrafts/loot_table/entities/tomb_guardian.json");
        assertFalse(json.contains("heart_of_gravity"), "gravity pool is not merged into the Ancient Heart pool");
        assertTrue(json.contains("moreminecrafts:ancient_heart"));
        assertTrue(json.contains("0.5"));
    }

    @Test void registeredOnceAndOtherRulesUntouched() throws Exception {
        String main = read("src/main/java/dev/itzgamesgd/moreminecrafts/MoreMinecrafts.java");
        assertEquals(1, main.split("TombGuardianGravityHeart\\.initialize\\(\\)", -1).length - 1);
        assertTrue(read("src/main/java/dev/itzgamesgd/moreminecrafts/progression/T2CoreAcquisition.java").contains("ANCIENT_HEART, 0.03F"));
        assertTrue(read("src/main/java/dev/itzgamesgd/moreminecrafts/accessory/RelicCatalystSources.java").contains("CATALYST_CHANCE = 0.05D"));
    }
}
