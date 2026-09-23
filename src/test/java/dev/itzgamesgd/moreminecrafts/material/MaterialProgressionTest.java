package dev.itzgamesgd.moreminecrafts.material;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MaterialProgressionTest {
    @Test void definesExactlyTheFiveApprovedFamiliesInProgressionOrder() {
        assertEquals(java.util.List.of("titanium", "tungsten", "mythril", "etherium", "adamantium"),
                MaterialProgression.ORDERED.stream().map(MaterialProgression.Definition::name).toList());
        for (int i = 1; i < MaterialProgression.ORDERED.size(); i++) {
            assertTrue(MaterialProgression.ORDERED.get(i - 1).progressionTier()
                    <= MaterialProgression.ORDERED.get(i).progressionTier());
        }
    }

    @Test void placementBoundsAndRareVeinCapsRemainExplicit() {
        assertEquals(-16, MaterialProgression.TITANIUM.minimumY());
        assertEquals(80, MaterialProgression.TITANIUM.maximumY());
        assertEquals(-48, MaterialProgression.MYTHRIL.peakY());
        assertEquals(6, MaterialProgression.MYTHRIL.veinSize());
        assertEquals(6, MaterialProgression.ETHERIUM.veinSize());
        assertTrue(MaterialProgression.MYTHRIL.attemptsPerChunk() < MaterialProgression.TITANIUM.attemptsPerChunk());
    }
}
