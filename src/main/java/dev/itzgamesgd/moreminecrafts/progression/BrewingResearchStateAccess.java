package dev.itzgamesgd.moreminecrafts.progression;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

/** Duck interface on BrewingStandBlockEntity for `.59`/`.60`/`.66` attribution and `.69` 9+9 automation queues. */
public interface BrewingResearchStateAccess {
    int AUTO_QUEUE_SIZE = 9;

    BlockEntityResearchAttribution.State moreminecrafts$getResearchAttribution();
    void moreminecrafts$setResearchAttribution(BlockEntityResearchAttribution.State state);
    double moreminecrafts$getBrewProgressRemainder();
    void moreminecrafts$setBrewProgressRemainder(double remainder);

    boolean moreminecrafts$isAutoProcessEnabled();
    void moreminecrafts$setAutoProcessEnabled(boolean enabled);
    NonNullList<ItemStack> moreminecrafts$getInputQueue();
    NonNullList<ItemStack> moreminecrafts$getOutputBuffer();
}
