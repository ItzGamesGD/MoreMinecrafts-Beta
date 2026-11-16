package dev.itzgamesgd.moreminecrafts.pillagerhq;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class St03GarrisonSpecTest {
    @Test
    void garrisonHasExact40DefendersAnd2Prisoners() {
        assertEquals(40, St03GarrisonSpec.SLOTS.size());
        assertEquals(2, St03GarrisonSpec.PRISONER_SLOTS.size());

        Map<St03GarrisonSpec.Role, Integer> counts = new EnumMap<>(St03GarrisonSpec.Role.class);
        for (int i = 0; i < St03GarrisonSpec.SLOTS.size(); i++) {
            var slot = St03GarrisonSpec.SLOTS.get(i);
            assertEquals(i, slot.index());
            counts.merge(slot.role(), 1, Integer::sum);
        }

        assertEquals(1, counts.get(St03GarrisonSpec.Role.PILLAGER_COMMANDER));
        assertEquals(1, counts.get(St03GarrisonSpec.Role.PILLAGER_BEASTMASTER));
        assertEquals(2, counts.get(St03GarrisonSpec.Role.ARMORY_GUARD));
        assertEquals(1, counts.get(St03GarrisonSpec.Role.PILLAGER_DRILLMASTER));
        assertEquals(1, counts.get(St03GarrisonSpec.Role.RAVAGER));
        assertEquals(9, counts.get(St03GarrisonSpec.Role.BANDIT));
        assertEquals(8, counts.get(St03GarrisonSpec.Role.BANDIT_WARRIOR));
        assertEquals(12, counts.get(St03GarrisonSpec.Role.BANDIT_ARCHER));
        assertEquals(5, counts.get(St03GarrisonSpec.Role.WANDERING_RAIDER));
    }
}
