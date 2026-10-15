package dev.itzgamesgd.moreminecrafts.progression;

import net.minecraft.server.level.ServerPlayer;

/** Applies Survival & Expedition only to the specific vanilla sprint/swim call sites. */
public final class ResearchMovementExhaustion {
    private ResearchMovementExhaustion() {}

    public static float scale(ServerPlayer player, float exhaustion) {
        int level = ResearchRewards.level(player, "v3.research.55", "sprint_swim_exhaustion");
        return (float) (exhaustion * ResearchParallelBranchRules.sprintSwimExhaustionMultiplier(level));
    }
}
