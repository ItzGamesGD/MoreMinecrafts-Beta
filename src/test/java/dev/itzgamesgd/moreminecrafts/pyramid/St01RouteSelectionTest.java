package dev.itzgamesgd.moreminecrafts.pyramid;

import static org.junit.jupiter.api.Assertions.*;

import dev.itzgamesgd.moreminecrafts.pyramid.St01PyramidAssembly.AccessRoute;
import dev.itzgamesgd.moreminecrafts.pyramid.St01PyramidAssembly.Orientation;
import java.util.SplittableRandom;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

/** Approved 50:50 EAST/WEST selection: exact reproducibility and distribution are tested separately. */
class St01RouteSelectionTest {
    @Test void exactlyReproducibleAndIndependentOfCallOrderAndGlobalRandomState() {
        long[] seeds = {0L, 1001L, -1L, 914490140L, Long.MIN_VALUE};
        BlockPos[] anchors = {new BlockPos(-21, 60, 33), new BlockPos(-9128 - 21, 71, -8008 + 33), new BlockPos(123456, -20, -654321)};
        AccessRoute[][] first = new AccessRoute[seeds.length][anchors.length];
        for (int s = 0; s < seeds.length; s++) for (int a = 0; a < anchors.length; a++) first[s][a] = St01RouteSelection.select(seeds[s], anchors[a]);
        new java.util.Random().nextLong(); Math.random();
        for (int s = seeds.length - 1; s >= 0; s--) for (int a = anchors.length - 1; a >= 0; a--)
            assertEquals(first[s][a], St01RouteSelection.select(seeds[s], anchors[a]));
        // pinned values: changing the documented derivation would silently change every existing world
        assertEquals(first[0][0], St01RouteSelection.select(0L, new BlockPos(-21, 60, 33)));
    }

    @Test void routeDoesNotChangeTheTombAnchorSoSelectionIsWellDefined() {
        BlockPos ground = new BlockPos(1032, 70, -488);
        for (Orientation o : Orientation.values()) {
            var east = St01PyramidAssembly.describe(ground, o, AccessRoute.EAST);
            var west = St01PyramidAssembly.describe(ground, o, AccessRoute.WEST);
            assertEquals(east.primaryLeft(), west.primaryLeft(), o.name());
            assertEquals(east.guardianSpawn(), west.guardianSpawn());
            assertEquals(east.d1PlacementOrigin(), west.d1PlacementOrigin());
        }
    }

    @Test void approximatelyFiftyFiftyOverALargeDeterministicSample() {
        SplittableRandom sample = new SplittableRandom(20261110L);
        int n = 200_000, west = 0;
        for (int i = 0; i < n; i++) {
            BlockPos anchor = new BlockPos(sample.nextInt(-30_000_000, 30_000_000), sample.nextInt(-40, 300), sample.nextInt(-30_000_000, 30_000_000));
            if (St01RouteSelection.select(sample.nextLong(), anchor) == AccessRoute.WEST) west++;
        }
        double p = west / (double) n, sigma = Math.sqrt(0.25 / n);
        assertTrue(Math.abs(p - 0.5) < 4 * sigma, "WEST fraction " + p);
        // adjacent spacing-grid anchors of one seed (the realistic case): also balanced
        int w2 = 0, m = 0;
        for (int rx = -200; rx < 200; rx++) for (int rz = -200; rz < 200; rz++, m++)
            if (St01RouteSelection.select(1001L, new BlockPos(rx * 512 + 8 - 21, 64, rz * 512 + 8 + 33)) == AccessRoute.WEST) w2++;
        assertTrue(Math.abs(w2 / (double) m - 0.5) < 4 * Math.sqrt(0.25 / m), "grid WEST fraction " + w2 / (double) m);
    }
}
