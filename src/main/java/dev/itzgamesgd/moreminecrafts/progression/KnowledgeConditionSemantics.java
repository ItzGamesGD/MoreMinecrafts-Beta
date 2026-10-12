package dev.itzgamesgd.moreminecrafts.progression;

import java.util.Map;

/** Small deterministic predicates shared by Stage 2-B event adapters and their tests. */
public final class KnowledgeConditionSemantics {
    private KnowledgeConditionSemantics() {}

    public static boolean successfulServerAction(boolean serverSide, boolean consumesAction) {
        return serverSide && consumesAction;
    }

    public static boolean consumedItem(int before, int after) {
        return before > 0 && after < before;
    }

    public static boolean blastFurnaceUseQualifies(boolean playerCraftedBlastFurnace, boolean collectedOutput) {
        return playerCraftedBlastFurnace && collectedOutput;
    }

    public static String projectileKillCondition(boolean crossbowOrigin) {
        return crossbowOrigin ? "crossbow_hostile_kills" : "bow_hostile_kills";
    }

    public static String distinctObservationKey(String condition, String discoveredId) {
        return "condition." + condition + ".seen." + discoveredId.replace(':', '.');
    }

    public static boolean alreadyObserved(Map<String, Integer> counters, String condition, String discoveredId) {
        return counters.getOrDefault(distinctObservationKey(condition, discoveredId), 0) != 0;
    }
}
