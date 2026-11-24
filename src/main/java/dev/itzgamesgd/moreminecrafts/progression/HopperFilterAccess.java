package dev.itzgamesgd.moreminecrafts.progression;

import net.minecraft.server.level.ServerLevel;

/** Duck interface applied to HopperBlockEntity for persistent `hopper_filter` state and attribution. */
public interface HopperFilterAccess {
    HopperFilterState moreminecrafts$getFilterState();
    void moreminecrafts$setFilterState(HopperFilterState state);
    BlockEntityResearchAttribution.State moreminecrafts$getResearchAttribution();
    void moreminecrafts$setResearchAttribution(BlockEntityResearchAttribution.State state);
    boolean moreminecrafts$isFilterActive(ServerLevel level);
}
