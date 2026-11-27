package dev.itzgamesgd.moreminecrafts.progression;

import net.minecraft.server.level.ServerPlayer;

/** Applies V3 XP Acquisition to the player who collects an orb. */
public final class ResearchXpAcquisition {
    private static final String FRACTION_KEY = "research.xp_fraction.v3.research.64";
    private ResearchXpAcquisition() {}

    public static int adjustedAmount(ServerPlayer player, int vanillaXp) {
        if (vanillaXp <= 0) return vanillaXp;
        ResearchProgressState state = KnowledgeRuntime.state(player);
        int level = ResearchRewards.level(player, "v3.research.64", "xp_acquisition");
        ResearchXpAcquisitionRules.Award award = ResearchXpAcquisitionRules.apply(
                vanillaXp, level, state.get(FRACTION_KEY));
        if (level > 0 && award.remainder() != state.get(FRACTION_KEY))
            KnowledgeRuntime.updateState(player, state.with(FRACTION_KEY, award.remainder()));
        return award.totalXp();
    }
}
