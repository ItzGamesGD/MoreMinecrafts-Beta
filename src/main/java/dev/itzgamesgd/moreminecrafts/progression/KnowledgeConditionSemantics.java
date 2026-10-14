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
        // Keep the namespace separator. Both namespace and path may contain dots, so replacing
        // ':' with '.' is not injective (for example foo.bar:baz and foo:bar.baz).
        return "condition." + condition + ".seen." + discoveredId;
    }

    public static boolean alreadyObserved(Map<String, Integer> counters, String condition, String discoveredId) {
        if (counters.getOrDefault(distinctObservationKey(condition, discoveredId), 0) != 0) return true;
        // Older saves flattened ':' to '.', which can be ambiguous. Recognize that old key so
        // previously observed IDs do not gain credit again; new observations use the exact ID.
        return counters.getOrDefault(legacyDistinctObservationKey(condition, discoveredId), 0) != 0;
    }

    private static String legacyDistinctObservationKey(String condition, String discoveredId) {
        return "condition." + condition + ".seen." + discoveredId.replace(':', '.');
    }
}
