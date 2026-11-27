package dev.itzgamesgd.moreminecrafts.progression;

import net.minecraft.server.level.ServerPlayer;

/** Server-authoritative progress and one-time raw-XP claims. */
public final class QuestProgressService {
    /** Appended values keep existing client ordinal mapping stable. */
    public enum ClaimResult { CLAIMED, UNKNOWN_QUEST, INCOMPLETE, ALREADY_CLAIMED, UNAVAILABLE, INVALID_STATE }
    /** GameTest seam: runs after the claimed flag is recorded and before the XP grant. */
    static volatile Runnable claimFailureInjectionForTests;
    private QuestProgressService() {}

    public static void recordProgress(ServerPlayer player, String questId, int amount) {
        QuestDefinition quest = QuestCatalog.byId(questId);
        if (quest == null || amount <= 0) return;
        ResearchProgressState before = KnowledgeRuntime.state(player);
        ResearchProgressState after = QuestRules.addProgress(before, quest, amount);
        if (after != before) KnowledgeRuntime.updateState(player, after);
    }

    /**
     * Claim-once transaction on the server thread: check, record claimed, grant exact raw XP.
     * If the grant fails, the claimed flag and all XP fields are restored before returning.
     */
    public static ClaimResult claim(ServerPlayer player, String questId) {
        QuestDefinition quest = QuestCatalog.byId(questId);
        if (quest == null) return ClaimResult.UNKNOWN_QUEST;
        if (!player.isAlive() || player.hasDisconnected()) return ClaimResult.UNAVAILABLE;
        var server = player.level().getServer();
        if (server == null || !server.isSameThread()) return ClaimResult.INVALID_STATE;
        ResearchProgressState before = KnowledgeRuntime.state(player);
        if (before.questClaimed(questId)) return ClaimResult.ALREADY_CLAIMED;
        if (!QuestRules.canClaim(before, quest)) return ClaimResult.INCOMPLETE;
        RawXp.Snapshot xpBefore = RawXp.Snapshot.of(player);
        // Mark first on the single server thread; duplicate client requests observe the claim.
        KnowledgeRuntime.updateState(player, QuestRules.claim(before, quest));
        try {
            Runnable injection = claimFailureInjectionForTests;
            if (injection != null) injection.run();
            RawXp.grant(player, quest.rewardRawXp());
            return ClaimResult.CLAIMED;
        } catch (RuntimeException failure) {
            xpBefore.restore(player);
            KnowledgeRuntime.updateState(player, before);
            return ClaimResult.INVALID_STATE;
        }
    }
}
