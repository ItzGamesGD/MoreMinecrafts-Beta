package dev.itzgamesgd.moreminecrafts.pillagerhq.worldgen;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class St03ExteriorFeatureGuardTest {
    private static final BlockPos ORIGIN = new BlockPos(1000, 70, -2000);

    @Test void footprintColumnsAreTheExact112x112Square() {
        assertTrue(St03StructureLookup.columnInFootprint(ORIGIN, new BlockPos(1000, 0, -2000)));
        assertTrue(St03StructureLookup.columnInFootprint(ORIGIN, new BlockPos(1111, 300, -1889)));
        assertFalse(St03StructureLookup.columnInFootprint(ORIGIN, new BlockPos(999, 70, -2000)));
        assertFalse(St03StructureLookup.columnInFootprint(ORIGIN, new BlockPos(1112, 70, -2000)));
        assertFalse(St03StructureLookup.columnInFootprint(ORIGIN, new BlockPos(1000, 70, -2001)));
        assertFalse(St03StructureLookup.columnInFootprint(ORIGIN, new BlockPos(1000, 70, -1888)));
    }

    @Test void guardedVolumeStartsAtTheFoundationLevel() {
        assertFalse(St03ExteriorFeatureGuard.inGuardedVolume(70, 69));
        assertTrue(St03ExteriorFeatureGuard.inGuardedVolume(70, 70));
        assertTrue(St03ExteriorFeatureGuard.inGuardedVolume(70, 70 + St03ExteriorFeatureGuard.GUARDED_HEIGHT - 1));
        assertFalse(St03ExteriorFeatureGuard.inGuardedVolume(70, 70 + St03ExteriorFeatureGuard.GUARDED_HEIGHT));
    }

    @Test void guardCoversTheTallestTreeAboveTheStructure() {
        // Structure modules are about 30 blocks tall; a giant spruce is at most ~40 above the ground.
        assertTrue(St03ExteriorFeatureGuard.GUARDED_HEIGHT >= 64);
    }
}
