package dev.itzgamesgd.moreminecrafts.skytower;

import static org.junit.jupiter.api.Assertions.assertEquals;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import org.junit.jupiter.api.Test;

class SkyTowerWorldgenRegistrationTest {
    @Test
    void compositePieceOriginRestoresApprovedTowerOriginForEveryRotation() {
        var piece = new BlockPos(-32, 96, 48);
        Rotation[] rotations = {Rotation.NONE, Rotation.CLOCKWISE_90, Rotation.CLOCKWISE_180,
                Rotation.COUNTERCLOCKWISE_90};
        for (int quarter = 0; quarter < rotations.length; quarter++) {
            assertEquals(new BlockPos(-32, 113, 48),
                    SkyTowerWorldgenRegistration.towerOrigin(piece, rotations[quarter]));
            assertEquals(quarter, SkyTowerWorldgenRegistration.rotationIndex(rotations[quarter]));
        }
    }

    @Test
    void restoredRotationMapsSpawnerAnchorsToAllFourWorldOrientations() {
        int[][] expected = {{117, 217}, {83, 217}, {83, 183}, {117, 183}};
        for (int rotation = 0; rotation < 4; rotation++) {
            var site = new SkyTowerLayout.Site("minecraft:overworld", 100, 70, 200, rotation);
            var spawner = site.spawner(0);
            assertEquals(expected[rotation][0], spawner.x());
            assertEquals(82, spawner.y());
            assertEquals(expected[rotation][1], spawner.z());
        }
    }
}
