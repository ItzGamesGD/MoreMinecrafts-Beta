package dev.itzgamesgd.moreminecrafts.test;

import dev.itzgamesgd.moreminecrafts.fishing.FishingEquipmentFamily;
import dev.itzgamesgd.moreminecrafts.fishing.FishingEquipmentTier;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class LiveTestCommandTest {
    @Test void fishingKitRequestsOnlyAuthoritativeEquipmentTiersForEveryFamily() {
        Map<FishingEquipmentFamily, List<Integer>> requested = new EnumMap<>(FishingEquipmentFamily.class);
        LiveTestCommand.forEachFishingEquipment((family, tier) ->
                requested.computeIfAbsent(family, ignored -> new ArrayList<>()).add(tier));

        List<Integer> expected = IntStream.rangeClosed(FishingEquipmentTier.MIN, FishingEquipmentTier.MAX)
                .boxed().toList();
        assertEquals(List.of(1, 2, 3), expected);
        assertEquals(FishingEquipmentFamily.values().length, requested.size());
        for (FishingEquipmentFamily family : FishingEquipmentFamily.values()) {
            assertEquals(expected, requested.get(family), family.name());
            assertFalse(requested.get(family).contains(4), family.name());
            assertFalse(requested.get(family).contains(5), family.name());
            requested.get(family).forEach(FishingEquipmentTier::requireValid);
        }
    }
}
