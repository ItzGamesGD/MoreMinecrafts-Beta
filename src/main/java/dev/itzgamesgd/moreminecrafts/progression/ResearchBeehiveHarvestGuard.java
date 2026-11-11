package dev.itzgamesgd.moreminecrafts.progression;

import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Validates the post-harvest state without reading a property from a replaced block. */
public final class ResearchBeehiveHarvestGuard {
    private ResearchBeehiveHarvestGuard() {}

    public static boolean succeeded(BlockState before, BlockState after, boolean consumed) {
        return consumed
                && before.hasProperty(BeehiveBlock.HONEY_LEVEL)
                && before.getValue(BeehiveBlock.HONEY_LEVEL) == 5
                && after.is(before.getBlock())
                && after.hasProperty(BeehiveBlock.HONEY_LEVEL)
                && after.getValue(BeehiveBlock.HONEY_LEVEL) == 0;
    }
}
