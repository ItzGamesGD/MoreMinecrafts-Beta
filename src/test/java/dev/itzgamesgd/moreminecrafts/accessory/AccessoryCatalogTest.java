package dev.itzgamesgd.moreminecrafts.accessory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AccessoryCatalogTest {
    @Test
    void finalizedRosterHasTwelveUniqueRegistryIdsAndOneSlotPerCategory() {
        assertEquals(12, AccessoryDefinition.ALL.size());
        assertEquals(12, AccessoryDefinition.ALL.stream().map(AccessoryDefinition::registryId).distinct().count());

        Map<AccessoryCategory, Integer> counts = new EnumMap<>(AccessoryCategory.class);
        for (AccessoryDefinition definition : AccessoryDefinition.ALL) {
            assertTrue(definition.registryId().matches("[a-z0-9_]+"));
            assertTrue(definition.translationKey().startsWith("item.moreminecrafts."));
            counts.merge(definition.category(), 1, Integer::sum);
        }
        for (AccessoryCategory category : AccessoryCategory.values())
            assertTrue(counts.getOrDefault(category, 0) >= 1, "No accessory assigned to " + category);
        assertEquals(5, AccessoryCategory.values().length);
        assertEquals(6, counts.get(AccessoryCategory.COMBAT));
        assertEquals(2, counts.get(AccessoryCategory.DEFENSE_SURVIVAL));
        assertEquals(1, counts.get(AccessoryCategory.SUPPORT_RECOVERY));
        assertEquals(2, counts.get(AccessoryCategory.EXPLORATION_MOBILITY));
        assertEquals(1, counts.get(AccessoryCategory.LIFESTYLE_GATHERING));
    }

    @Test
    void onlyCombatCategoryAllowsManualActivation() {
        for (AccessoryCategory category : AccessoryCategory.values())
            assertEquals(category == AccessoryCategory.COMBAT, category.allowsManualActivation());
        assertTrue(AccessoryCategory.COMBAT.allowsManualActivation());
        assertFalse(AccessoryCategory.SUPPORT_RECOVERY.allowsManualActivation());
    }

    @Test
    void loadoutHasFiveCategorySlotsRejectsDuplicateCategoryAndLimitsActivationToCombat() {
        AccessoryLoadout loadout = AccessoryLoadout.empty();
        assertEquals(5, loadout.slotCount());
        assertTrue(loadout.equip(AccessoryDefinition.STAR_COVENANT));
        assertFalse(loadout.equip(AccessoryDefinition.PHANTOM_MIRROR));
        assertTrue(loadout.equip(AccessoryDefinition.EMBER_RING));
        assertTrue(loadout.canManuallyActivate(AccessoryDefinition.STAR_COVENANT));
        assertFalse(loadout.canManuallyActivate(AccessoryDefinition.EMBER_RING));
        assertEquals(2, loadout.equipped().size());
    }
}
