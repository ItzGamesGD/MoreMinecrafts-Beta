package dev.itzgamesgd.moreminecrafts.pillagerhq.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import org.junit.jupiter.api.Test;

class St03PlacementManifestTest {
    @Test
    void loadsAll34PlacementsInOrder() {
        var list = St03PlacementManifest.placements();
        assertEquals(34, list.size());
        int prevOrder = 0;
        for (var p : list) {
            assertTrue(p.module().order() >= prevOrder);
            prevOrder = p.module().order();
        }
    }

    @Test
    void allRotationsStayInside112x112Bounds() {
        BlockPos origin = new BlockPos(100, 64, 200);
        for (Rotation rot : Rotation.values()) {
            for (var p : St03PlacementManifest.placements()) {
                BlockPos pos = p.worldPlacePos(origin, rot);
                int dx = pos.getX() - origin.getX();
                int dz = pos.getZ() - origin.getZ();
                assertTrue(dx >= 0 && dx < 112, "dx out of bounds for " + p.placementId() + " rot=" + rot + ": " + dx);
                assertTrue(dz >= 0 && dz < 112, "dz out of bounds for " + p.placementId() + " rot=" + rot + ": " + dz);
            }
        }
    }
}
