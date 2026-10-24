package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.*;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;

class ResearchBeehiveHarvestGuardTest {
    @Test
    void onlyConsumedFullHiveShearsHarvestWithSameBlockIsEligible() {
        var full = Blocks.BEEHIVE.defaultBlockState().setValue(BeehiveBlock.HONEY_LEVEL, 5);
        var empty = full.setValue(BeehiveBlock.HONEY_LEVEL, 0);
        assertTrue(ResearchBeehiveHarvestGuard.succeeded(full, empty, true));
        assertFalse(ResearchBeehiveHarvestGuard.succeeded(full, empty, false));
        assertFalse(ResearchBeehiveHarvestGuard.succeeded(full.setValue(BeehiveBlock.HONEY_LEVEL, 4), empty, true));
        assertFalse(ResearchBeehiveHarvestGuard.succeeded(full, full, true));
        assertFalse(ResearchBeehiveHarvestGuard.succeeded(full, Blocks.AIR.defaultBlockState(), true),
                "replaced block must not attempt to read a missing honey property");
        assertFalse(ResearchBeehiveHarvestGuard.succeeded(full, Blocks.HONEY_BLOCK.defaultBlockState(), true));
    }
}
