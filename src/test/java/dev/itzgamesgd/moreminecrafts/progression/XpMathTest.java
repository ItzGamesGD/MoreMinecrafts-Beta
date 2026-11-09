package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class XpMathTest {
    @Test void cumulativeTotalsAreTheSumOfVanillaPerLevelRequirements() {
        long running = 0;
        for (int level = 0; level < 5_000; level++) {
            assertEquals(running, XpMath.totalForLevel(level), "total at level " + level);
            running += XpMath.xpNeededForNextLevel(level);
        }
    }

    @Test void vanillaRequirementBoundariesMatchMinecraft26_3() {
        assertEquals(7, XpMath.xpNeededForNextLevel(0));
        assertEquals(35, XpMath.xpNeededForNextLevel(14));
        assertEquals(37, XpMath.xpNeededForNextLevel(15));
        assertEquals(107, XpMath.xpNeededForNextLevel(29));
        assertEquals(112, XpMath.xpNeededForNextLevel(30));
        assertEquals(315, XpMath.totalForLevel(15));
        assertEquals(352, XpMath.totalForLevel(16));
        assertEquals(1_395, XpMath.totalForLevel(30));
        assertEquals(1_507, XpMath.totalForLevel(31));
    }

    @Test void splitRoundTripsEveryTotalUpToOneMillion() {
        for (int total = 0; total <= 1_000_000; total++) {
            XpMath.Split split = XpMath.split(total);
            assertTrue(split.points() >= 0 && split.points() < XpMath.xpNeededForNextLevel(split.level()),
                    "points inside bar at " + total);
            assertEquals(total, XpMath.totalForLevel(split.level()) + split.points(), "round trip " + total);
        }
    }

    @Test void splitClampsNegativeAndOverflowingTotals() {
        assertEquals(new XpMath.Split(0, 0), XpMath.split(-50));
        XpMath.Split max = XpMath.split(Long.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, XpMath.totalForLevel(max.level()) + max.points());
    }
}
