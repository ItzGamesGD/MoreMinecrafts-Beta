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
    void rooftopVaultIsUnplacedInBaselineCandidate() {
        // Until human owner approves container type and placement coordinates,
        // no rooftop vault position is claimed.
        var s = site(0, 65, 0, 0);
        BlockPos altarCandidate = new BlockPos(24, 65 + 65, 10);
        assertFalse(SkyTowerVaultRewards.isRooftopVault(null, altarCandidate));
    }
}
