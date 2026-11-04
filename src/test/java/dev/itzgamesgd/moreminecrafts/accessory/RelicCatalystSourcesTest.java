package dev.itzgamesgd.moreminecrafts.accessory;

import static dev.itzgamesgd.moreminecrafts.accessory.RelicCatalystSources.Source.ANCIENT_CITY;
import static dev.itzgamesgd.moreminecrafts.accessory.RelicCatalystSources.Source.BASTION_TREASURE;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

/** D13-02 probability and assignment model with fixed seeds and controlled rolls (no retry-until-success). */
class RelicCatalystSourcesTest {
    @Test void ancientCityChanceIsOneMinusPointNineFiveToTheN() {
        assertEquals(0.05D, RelicCatalystSources.instanceChance(ANCIENT_CITY, 1), 1e-12);
        assertEquals(1 - Math.pow(0.95D, 13), RelicCatalystSources.instanceChance(ANCIENT_CITY, 13), 1e-12);
        assertEquals(0.0D, RelicCatalystSources.instanceChance(ANCIENT_CITY, 0));
    }

    @Test void bastionTreasureRoomIsFivePercentPerStructureRegardlessOfCoreChestCount() {
        assertEquals(0.05D, RelicCatalystSources.instanceChance(BASTION_TREASURE, 1), 1e-12);
        assertEquals(0.05D, RelicCatalystSources.instanceChance(BASTION_TREASURE, 2), 1e-12);
        assertEquals(0.0D, RelicCatalystSources.instanceChance(BASTION_TREASURE, 0));
    }

    @Test void controlledRollBoundariesAndSinglePick() {
        double chance = RelicCatalystSources.instanceChance(ANCIENT_CITY, 4);
        assertEquals(2, RelicCatalystSources.select(ANCIENT_CITY, 4, () -> chance - 1e-9, n -> { assertEquals(4, n); return 2; }));
        assertEquals(-1, RelicCatalystSources.select(ANCIENT_CITY, 4, () -> chance, n -> { fail("no pick on failure"); return 0; }));
        assertEquals(-1, RelicCatalystSources.select(ANCIENT_CITY, 4, () -> 0.999D, n -> { fail("no pick on failure"); return 0; }));
        assertEquals(-1, RelicCatalystSources.select(ANCIENT_CITY, 0, () -> 0.0D, n -> 0));
        assertEquals(-1, RelicCatalystSources.select(ANCIENT_CITY, 3, () -> 0.0D, n -> n), "out-of-range pick is rejected");
    }

    @Test void assignmentIsStableAcrossRecomputation() {
        for (long salt = 0; salt < 200; salt++) {
            ChunkPos start = new ChunkPos((int) (salt * 31 - 2000), (int) (salt * 17 + 900));
            int first = RelicCatalystSources.assignedIndex(42L, start, ANCIENT_CITY, 9);
            assertEquals(first, RelicCatalystSources.assignedIndex(42L, start, ANCIENT_CITY, 9), "restart-equivalent recompute");
            assertTrue(first >= -1 && first < 9);
        }
    }

    @Test void fixedSeedPopulationMatchesApprovedRatesAndPicksUniformly() {
        final int cities = 40_000, chests = 8;
        int hits = 0;
        int[] perChest = new int[chests];
        for (int i = 0; i < cities; i++) {
            int index = RelicCatalystSources.assignedIndex(1234567L, new ChunkPos(i % 400 - 200, i / 400 - 50), ANCIENT_CITY, chests);
            if (index >= 0) { hits++; perChest[index]++; }
        }
        double expected = 1 - Math.pow(0.95D, chests);
        assertEquals(expected, hits / (double) cities, 0.01D);
        for (int count : perChest) assertEquals(hits / (double) chests, count, hits * 0.15D / chests * 3);

        int bastionHits = 0;
        for (int i = 0; i < cities; i++)
            if (RelicCatalystSources.assignedIndex(99L, new ChunkPos(i % 400 - 200, i / 400 - 50), BASTION_TREASURE, 2) >= 0) bastionHits++;
        assertEquals(0.05D, bastionHits / (double) cities, 0.006D);
    }

    @Test void catalystIsAnAdditivePoolAndNeverReplacesVanillaTables() throws Exception {
        for (String table : new String[]{"ancient_city", "ancient_city_ice_box", "bastion_treasure", "bastion_other"})
            assertFalse(Files.exists(Path.of("src/main/resources/data/minecraft/loot_table/chests/" + table + ".json")),
                    table + " must not be overridden");
        String source = Files.readString(Path.of("src/main/java/dev/itzgamesgd/moreminecrafts/accessory/RelicCatalystSources.java"));
        assertTrue(source.contains("table.withPool("));
        assertFalse(source.contains("table.pools"));
        assertFalse(source.contains("minecraft:chests/ancient_city_ice_box"));
        assertFalse(source.contains("sky_islands"));
        assertFalse(source.contains("tomb_guardian"));
    }
}
