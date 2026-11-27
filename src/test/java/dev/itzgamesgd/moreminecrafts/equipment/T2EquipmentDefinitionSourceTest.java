package dev.itzgamesgd.moreminecrafts.equipment;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Source guard: the six T2 elemental items are Unbreakable and define no finite durability of their own. */
class T2EquipmentDefinitionSourceTest {
    @Test void sixT2ItemsAreUnbreakableWithoutFiniteDurabilityDefinitions() throws Exception {
        String source = Files.readString(Path.of("src/main/java/dev/itzgamesgd/moreminecrafts/registry/ModItems.java"));
        for (String key : List.of("FLAME_SWORD_KEY", "FLOWING_WATER_SWORD_KEY", "WIND_SWORD_KEY",
                "WIND_ARCHERS_BOW_KEY", "EARTH_MACE_KEY", "LIGHTNING_SWORD_KEY")) {
            int end = source.indexOf(".setId(" + key + ")");
            assertTrue(end > 0, key);
            String definition = source.substring(source.lastIndexOf("Registry.register(", end), end);
            assertTrue(definition.contains("DataComponents.UNBREAKABLE"), key + " must be Unbreakable");
            assertFalse(definition.contains(".durability("), key + " must not define finite durability");
        }
        // The deferred placeholder is deliberately outside this wave.
        int staff = source.indexOf(".setId(DARK_ENERGY_STAFF_KEY)");
        assertFalse(source.substring(source.lastIndexOf("Registry.register(", staff), staff).contains("UNBREAKABLE"));
    }
}
