package dev.itzgamesgd.moreminecrafts.progression;

/** Persistent attribution and brew-speed carry state stored on BrewingStandBlockEntity. */
public interface BrewingResearchStateAccess {
    BlockEntityResearchAttribution.State moreminecrafts$getResearchAttribution();
    void moreminecrafts$setResearchAttribution(BlockEntityResearchAttribution.State state);
    double moreminecrafts$getBrewProgressRemainder();
    void moreminecrafts$setBrewProgressRemainder(double remainder);
}
