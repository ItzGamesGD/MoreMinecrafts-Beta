package dev.itzgamesgd.moreminecrafts.enchantment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class EnchantmentMathTest {
    @Test void veinMiningLevelTwoHalvesAndRoundsUp() {
        assertEquals(3, EnchantmentMath.veinDurability(5, 2));
        assertEquals(5, EnchantmentMath.veinDurability(5, 1));
        assertEquals(1, EnchantmentMath.veinDurability(1, 2));
    }
    @Test void veinMiningIgnoresUnbreakingBookOptionWhileLumberjackStillUsesIt() {
        int withoutOption = EnchantmentMath.miningOperationDurability(7, 2, false, 2, 0);
        int withOption = EnchantmentMath.miningOperationDurability(7, 2, false, 2, 3);
        assertEquals(4, withoutOption);
        assertEquals(withoutOption, withOption);
        assertEquals(7, EnchantmentMath.miningOperationDurability(7, 1, false, 2, 3));
        assertEquals(6, EnchantmentMath.miningOperationDurability(7, 1, true, 0, 3));
    }
    @Test void lumberjackVerticalSpanIsInclusiveAndCappedAtThirtyFour() {
        assertEquals(true, EnchantmentMath.withinTreeHeight(10, 43));
        assertEquals(false, EnchantmentMath.withinTreeHeight(10, 44));
    }
    @Test void veinMiningUsesAUniqueChunkLimitRatherThanABlockLimit() {
        assertEquals(4, EnchantmentMath.MAX_VEIN_CHUNKS);
    }
    @Test void lumberjackAppliesUnbreakingOnceArithmetically() {
        assertEquals(10, EnchantmentMath.lumberDurability(10, 0));
        assertEquals(5, EnchantmentMath.lumberDurability(10, 1));
        assertEquals(4, EnchantmentMath.lumberDurability(10, 2));
    }
    @Test void lumberjackAcceptsOrdinaryOneByOneTrunk() {
        assertTrue(EnchantmentMath.isValidTreeStructure(tree(0, 0, 0, 5)));
    }
    @Test void lumberjackAcceptsCompleteTwoByTwoTrunk() {
        Set<BlockPos> logs = new HashSet<>();
        for (int y = 0; y < 8; y++) for (int x = 0; x < 2; x++) for (int z = 0; z < 2; z++)
            logs.add(new BlockPos(x, y, z));
        assertTrue(EnchantmentMath.isValidTreeStructure(logs));
    }
    @Test void lumberjackAcceptsBranchesOutsideTheTrunkFootprint() {
        Set<BlockPos> logs = tree(0, 0, 0, 8);
        logs.add(new BlockPos(1, 5, 0));
        logs.add(new BlockPos(2, 5, 0));
        logs.add(new BlockPos(3, 5, 0));
        assertTrue(EnchantmentMath.isValidTreeStructure(logs));
    }
    @Test void lumberjackAcceptsMaximumThirtyFourBlockTrunk() {
        assertTrue(EnchantmentMath.isValidTreeStructure(tree(0, 0, 0, 34)));
    }
    @Test void lumberjackRejectsTrunkWiderThanTwoByTwo() {
        Set<BlockPos> logs = new HashSet<>();
        for (int y = 0; y < 6; y++) for (int x = 0; x < 3; x++) for (int z = 0; z < 3; z++)
            logs.add(new BlockPos(x, y, z));
        assertFalse(EnchantmentMath.isValidTreeStructure(logs));
    }
    @Test void lumberjackRejectsTrunkTallerThanThirtyFourWithoutPartialEligibility() {
        Set<BlockPos> logs = tree(0, 0, 0, 35);
        assertFalse(EnchantmentMath.isValidTreeStructure(logs));
        // Validation is applied to the complete set: a valid initial segment cannot authorize
        // partial chain-felling when the full connected trunk exceeds the activation bound.
        assertTrue(EnchantmentMath.isValidTreeStructure(tree(0, 0, 0, 34)));
    }
    @Test void lumberjackRejectsRepresentativeArtificialLogMass() {
        Set<BlockPos> logs = tree(0, 0, 0, 8);
        for (int y = 4; y <= 6; y++) for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++)
            logs.add(new BlockPos(x, y, z));
        assertFalse(EnchantmentMath.isValidTreeStructure(logs));
    }
    @Test void lumberjackRejectsAnAttachedArtificialLogWall() {
        Set<BlockPos> logs = tree(0, 0, 0, 9);
        for (int y = 5; y <= 6; y++) for (int x = 1; x <= 8; x++)
            logs.add(new BlockPos(x, y, 0));
        assertFalse(EnchantmentMath.isValidTreeStructure(logs));
    }
    @Test void lumberjackRejectsASecondLongVerticalLogColumnAsTrunk() {
        Set<BlockPos> logs = tree(0, 0, 0, 9);
        for (int y = 5; y <= 8; y++) logs.add(new BlockPos(1, y, 0));
        assertFalse(EnchantmentMath.isValidTreeStructure(logs));
    }
    private static Set<BlockPos> tree(int x, int z, int startY, int height) {
        Set<BlockPos> logs = new HashSet<>();
        for (int y = startY; y < startY + height; y++) logs.add(new BlockPos(x, y, z));
        return logs;
    }
    @Test void jetBootChargesAreLevelBounded() {
        assertEquals(0, EnchantmentMath.airJumps(0));
        assertEquals(3, EnchantmentMath.airJumps(9));
    }
}
