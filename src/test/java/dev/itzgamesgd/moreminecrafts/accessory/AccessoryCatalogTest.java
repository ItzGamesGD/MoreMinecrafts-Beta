package dev.itzgamesgd.moreminecrafts.accessory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class AccessoryCatalogTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void finalizedRosterHasTwelveUniqueIdsAndExactlyFiveCategories() {
        assertEquals(12, AccessoryDefinition.ALL.size());
        assertEquals(12, AccessoryDefinition.ALL.stream().map(AccessoryDefinition::registryId).distinct().count());
        assertEquals(5, AccessoryCategory.values().length);

        var counts = new EnumMap<AccessoryCategory, Integer>(AccessoryCategory.class);
        for (AccessoryDefinition definition : AccessoryDefinition.ALL) {
            assertTrue(definition.registryId().matches("[a-z0-9_]+"));
            assertTrue(definition.translationKey().startsWith("item.moreminecrafts."));
            counts.merge(definition.category(), 1, Integer::sum);
        }
        assertEquals(6, counts.get(AccessoryCategory.COMBAT));
        assertEquals(2, counts.get(AccessoryCategory.DEFENSE_SURVIVAL));
        assertEquals(1, counts.get(AccessoryCategory.SUPPORT_RECOVERY));
        assertEquals(2, counts.get(AccessoryCategory.EXPLORATION_MOBILITY));
        assertEquals(1, counts.get(AccessoryCategory.LIFESTYLE_GATHERING));
    }

    @Test
    void loadoutHasFixedFiveSlotsAndRejectsMalformedShape() {
        AccessoryLoadout empty = AccessoryLoadout.empty();
        assertEquals(5, AccessoryLoadout.SIZE);
        assertEquals(5, empty.stacks().size());
        assertEquals(0, empty.occupiedSlots());
        assertFalse(empty.canManuallyActivate(AccessoryCategory.COMBAT.ordinal()));
        assertThrows(IndexOutOfBoundsException.class, () -> empty.get(-1));
        assertThrows(IllegalArgumentException.class, () -> new AccessoryLoadout(List.of()));

        List<net.minecraft.world.item.ItemStack> malformed = new ArrayList<>(empty.stacks());
        malformed.add(net.minecraft.world.item.ItemStack.EMPTY);
        assertThrows(IllegalArgumentException.class, () -> new AccessoryLoadout(malformed));
    }
}
