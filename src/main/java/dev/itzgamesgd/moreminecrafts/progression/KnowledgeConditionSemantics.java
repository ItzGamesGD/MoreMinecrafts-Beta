package dev.itzgamesgd.moreminecrafts.progression;

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
}
