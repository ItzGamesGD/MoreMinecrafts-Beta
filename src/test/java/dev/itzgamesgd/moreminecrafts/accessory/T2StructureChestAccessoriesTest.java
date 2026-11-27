package dev.itzgamesgd.moreminecrafts.accessory;

import static org.junit.jupiter.api.Assertions.*;

import dev.itzgamesgd.moreminecrafts.accessory.T2StructureChestAccessories.PoolSpec;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** D13-03-I01 exact table/weight/chance model; source-level checks for preservation and single registration. */
class T2StructureChestAccessoriesTest {
    private static PoolSpec pool(String table) {
        List<PoolSpec> found = T2StructureChestAccessories.POOLS.stream().filter(p -> p.lootTable().equals(table)).toList();
        assertEquals(1, found.size(), table);
        return found.get(0);
    }

    @Test void exactlyFourApprovedTablesAreTargeted() {
        assertEquals(java.util.Set.of("minecraft:chests/nether_bridge", "minecraft:chests/shipwreck_treasure",
                        "minecraft:chests/ancient_city", "minecraft:chests/bastion_treasure"),
                T2StructureChestAccessories.POOLS.stream().map(PoolSpec::lootTable).collect(Collectors.toSet()));
        assertEquals(4, T2StructureChestAccessories.POOLS.size());
    }

    @Test void chancesAndSharedPoolItemsMatchQ01ToQ10() {
        PoolSpec nether = pool("minecraft:chests/nether_bridge");
        assertEquals(0.05D, nether.chance(), 1e-12);
        assertEquals(List.of(AccessoryDefinition.EMBER_RING, AccessoryDefinition.SOUL_CHAIN), nether.items());
        assertEquals(0.025D, nether.chancePerItem(), 1e-12);

        PoolSpec city = pool("minecraft:chests/ancient_city");
        assertEquals(0.05D, city.chance(), 1e-12);
        assertEquals(List.of(AccessoryDefinition.RING_OF_LIFE, AccessoryDefinition.PHANTOM_MIRROR), city.items());
        assertEquals(0.025D, city.chancePerItem(), 1e-12);

        PoolSpec ship = pool("minecraft:chests/shipwreck_treasure");
        assertEquals(0.05D, ship.chance(), 1e-12);
        assertEquals(List.of(AccessoryDefinition.WHISPER_OF_THE_SEA), ship.items());

        PoolSpec bastion = pool("minecraft:chests/bastion_treasure");
        assertEquals(0.15D, bastion.chance(), 1e-12);
        assertEquals(List.of(AccessoryDefinition.BLOOD_COVENANT), bastion.items());
    }

    @Test void sixDistinctAccessoriesNoneSharedAcrossPools() {
        var all = T2StructureChestAccessories.POOLS.stream().flatMap(p -> p.items().stream()).toList();
        assertEquals(6, all.size());
        assertEquals(6, new HashSet<>(all).size());
    }

    @Test void sourceBuildsOneAdditivePoolPerTableWithEqualWeightsAndNoOtherTables() throws Exception {
        String s = Files.readString(Path.of("src/main/java/dev/itzgamesgd/moreminecrafts/accessory/T2StructureChestAccessories.java"));
        assertEquals(1, s.split("table\\.withPool\\(", -1).length - 1, "single pool per matched table");
        assertTrue(s.contains(".setWeight(1)"));
        assertFalse(s.contains("table.pools"));
        for (String forbidden : new String[]{"pyramid", "monument", "tomb_guardian", "sky_islands", "trial", "ancient_city_ice_box", "bastion_other"})
            assertFalse(s.contains(forbidden), forbidden);
    }

    @Test void vanillaTablesNotOverriddenAndRegisteredExactlyOnce() throws Exception {
        for (String t : new String[]{"nether_bridge", "shipwreck_treasure", "ancient_city", "bastion_treasure"})
            assertFalse(Files.exists(Path.of("src/main/resources/data/minecraft/loot_table/chests/" + t + ".json")), t);
        String main = Files.readString(Path.of("src/main/java/dev/itzgamesgd/moreminecrafts/MoreMinecrafts.java"));
        assertEquals(1, main.split("T2StructureChestAccessories\\.initialize\\(\\)", -1).length - 1);
        assertEquals(1, main.split("RelicCatalystSources\\.initialize\\(\\)", -1).length - 1);
    }

    @Test void catalystAndAncientHeartLogicUntouched() throws Exception {
        String heart = Files.readString(Path.of("src/main/java/dev/itzgamesgd/moreminecrafts/progression/T2CoreAcquisition.java"));
        assertTrue(heart.contains("ANCIENT_HEART, 0.03F"));
        String cat = Files.readString(Path.of("src/main/java/dev/itzgamesgd/moreminecrafts/accessory/RelicCatalystSources.java"));
        assertTrue(cat.contains("CATALYST_CHANCE = 0.05D"));
    }
}
