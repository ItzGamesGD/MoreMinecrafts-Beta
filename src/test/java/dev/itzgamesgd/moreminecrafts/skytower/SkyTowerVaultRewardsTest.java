package dev.itzgamesgd.moreminecrafts.skytower;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class SkyTowerVaultRewardsTest {
    private static SkyTowerLayout.Site site(int x, int y, int z, int rotation) {
        return new SkyTowerLayout.Site("minecraft:overworld", x, y, z, rotation);
    }

    @Test
    void normalVaultLootTableIdentifiersAreCorrectAndDistinct() {
        assertEquals("moreminecrafts:chests/sky_islands/vault",
                SkyTowerVaultRewards.NORMAL_VAULT_LOOT.toString());
        assertEquals("moreminecrafts:chests/sky_islands/vault_tower_top",
                SkyTowerVaultRewards.ROOFTOP_VAULT_LOOT.toString());
        assertNotEquals(SkyTowerVaultRewards.NORMAL_VAULT_LOOT_KEY,
                SkyTowerVaultRewards.ROOFTOP_VAULT_LOOT_KEY);
    }

    @Test
    void eachSiteHasExactlyEightNormalVaultsAcrossFourFloors() {
        var s = site(0, 65, 0, 0);
        Set<SkyTowerLayout.Point> points = new HashSet<>();
        for (int i = 0; i < 8; i++) {
            var pt = s.vault(i);
            assertTrue(points.add(pt), "vault position must be unique: " + pt);
        }
        assertEquals(8, points.size());

        // Floor 0: relative Y = 11 (world Y = 76)
        assertEquals(new SkyTowerLayout.Point(16, 76, 15), s.vault(0));
        assertEquals(new SkyTowerLayout.Point(16, 76, 33), s.vault(1));
        // Floor 1: relative Y = 22 (world Y = 87)
        assertEquals(new SkyTowerLayout.Point(16, 87, 15), s.vault(2));
        assertEquals(new SkyTowerLayout.Point(16, 87, 33), s.vault(3));
        // Floor 2: relative Y = 34 (world Y = 99)
        assertEquals(new SkyTowerLayout.Point(16, 99, 15), s.vault(4));
        assertEquals(new SkyTowerLayout.Point(16, 99, 33), s.vault(5));
        // Floor 3: relative Y = 48 (world Y = 113)
        assertEquals(new SkyTowerLayout.Point(16, 113, 15), s.vault(6));
        assertEquals(new SkyTowerLayout.Point(16, 113, 33), s.vault(7));
    }

    @Test
    void normalVaultIndicesMatchAcrossRotations() {
        for (int r = 0; r < 4; r++) {
            var s = site(100, 65, 100, r);
            for (int i = 0; i < 8; i++) {
                var pt = s.vault(i);
                BlockPos pos = new BlockPos(pt.x(), pt.y(), pt.z());
                assertEquals(i, SkyTowerVaultRewards.normalVaultIndex(s, pos));
            }
            // Non-vault position returns -1
            BlockPos randomPos = new BlockPos(100, 65, 100);
            assertEquals(-1, SkyTowerVaultRewards.normalVaultIndex(s, randomPos));
        }
    }

    @Test
    void rooftopVaultCoordinatesMatchApprovedLocationAcrossRotations() {
        // Approved coordinate: local (24, 65, 11) relative to tower origin
        // Rotation 0: (x + 24, y + 65, z + 11)
        var s0 = site(0, 65, 0, 0);
        assertEquals(new SkyTowerLayout.Point(24, 130, 11), s0.rooftopVault());

        // Rotation 1 (CW 90): X = -11, Z = 24 -> (x - 11, y + 65, z + 24)
        var s1 = site(0, 65, 0, 1);
        assertEquals(new SkyTowerLayout.Point(-11, 130, 24), s1.rooftopVault());

        // Rotation 2 (CW 180): X = -24, Z = -11 -> (x - 24, y + 65, z - 11)
        var s2 = site(0, 65, 0, 2);
        assertEquals(new SkyTowerLayout.Point(-24, 130, -11), s2.rooftopVault());

        // Rotation 3 (CCW 90): X = 11, Z = -24 -> (x + 11, y + 65, z - 24)
        var s3 = site(0, 65, 0, 3);
        assertEquals(new SkyTowerLayout.Point(11, 130, -24), s3.rooftopVault());
    }

    @Test
    void rooftopVaultFacingTransformsWithStructureRotation() {
        // Local facing is NORTH; rotated clockwise by rotation * 90 degrees
        assertEquals(net.minecraft.core.Direction.NORTH, SkyTowerVaultRewards.rooftopVaultFacing(0));
        assertEquals(net.minecraft.core.Direction.EAST, SkyTowerVaultRewards.rooftopVaultFacing(1));
        assertEquals(net.minecraft.core.Direction.SOUTH, SkyTowerVaultRewards.rooftopVaultFacing(2));
        assertEquals(net.minecraft.core.Direction.WEST, SkyTowerVaultRewards.rooftopVaultFacing(3));
    }

    @Test
    void rooftopVaultDoesNotCollideWithNormalVaultsOrSpawnersOrBarriers() {
        for (int r = 0; r < 4; r++) {
            var s = site(100, 65, 100, r);
            var rooftop = s.rooftopVault();

            // Check disjoint from all 8 normal vaults
            for (int i = 0; i < 8; i++) {
                assertNotEquals(rooftop, s.vault(i), "rooftop vault must not collide with normal vault " + i);
            }

            // Check disjoint from all 16 trial spawners
            for (int i = 0; i < 16; i++) {
                assertNotEquals(rooftop, s.spawner(i), "rooftop vault must not collide with spawner " + i);
            }

            // Check disjoint from all 72 barrier cells
            for (int f = 0; f < 4; f++) {
                for (int c = 0; c < 18; c++) {
                    assertNotEquals(rooftop, s.barrier(f, c), "rooftop vault must not collide with barrier " + f + "/" + c);
                }
            }
        }
    }

    @Test
    void rooftopVaultClassificationIsDistinctFromNormalVaults() {
        var s = site(0, 65, 0, 0);
        var rooftopPt = s.rooftopVault();
        BlockPos rooftopPos = new BlockPos(rooftopPt.x(), rooftopPt.y(), rooftopPt.z());

        // Rooftop vault is not a normal vault
        assertEquals(-1, SkyTowerVaultRewards.normalVaultIndex(s, rooftopPos));

        // Normal vaults are not rooftop vaults
        for (int i = 0; i < 8; i++) {
            var normalPt = s.vault(i);
            assertNotEquals(rooftopPt, normalPt);
        }
    }
}
