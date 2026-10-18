package dev.itzgamesgd.moreminecrafts.progression;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;

/**
 * Registration and evidence-based reconciliation for XP Quests. Only quests registered here are
 * evaluated. Production registration is gated per quest by the catalog's {@code registered} flag,
 * which authority allows only after that quest's own automated test passes.
 *
 * <p>This class never grants XP and never writes {@code quest.claimed}; claims stay in
 * {@link QuestProgressService}.</p>
 */
public final class XpQuestTracking {
    public static final int SCHEMA_VERSION = 1;
    static final String SCHEMA_KEY = "quest.schema";
    private static final int REFRESH_TICKS = 100;
    private static final Map<String, XpQuestCatalogData.Spec> ACTIVE = new LinkedHashMap<>();
    private static final java.util.Set<String> FAILED = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private XpQuestTracking() {}

    /** Call after {@link KnowledgeRuntime#initialize()} so the join reconciliation sees Knowledge's own refresh. */
    public static void initialize() {
        XpQuestHooks.initialize(); // baselines are captured before the reconciliation join listener runs
        QuestStructureEntry.initialize();
        for (XpQuestCatalogData.Spec spec : XpQuestCatalogData.catalog().quests())
            if (spec.registered()) register(spec);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> reconcile(handler.player));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % REFRESH_TICKS != 0) return;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) reconcile(player);
        });
    }

    static synchronized void register(XpQuestCatalogData.Spec spec) {
        if (ACTIVE.containsKey(spec.id())) return;
        if (!"NOW".equals(spec.status()) || !spec.tracker().evaluable())
            throw new IllegalArgumentException("Only NOW quests with an implemented tracker can register: " + spec.id());
        QuestCatalog.register(spec.definition());
        ACTIVE.put(spec.id(), spec);
    }

    static synchronized Collection<XpQuestCatalogData.Spec> active() { return java.util.List.copyOf(ACTIVE.values()); }

    /** Adds to an uncapped counter on the server thread; called by event hooks (Q1-B onward). */
    public static void addCounter(ServerPlayer player, String key, long amount) {
        ResearchProgressState before = KnowledgeRuntime.state(player);
        ResearchProgressState after = QuestRules.addCounter(before, key, amount);
        if (after != before) KnowledgeRuntime.updateState(player, after);
    }

    /** Re-evaluates every active quest from persisted evidence and writes at most one state update. */
    public static void reconcile(ServerPlayer player) {
        ResearchProgressState before = KnowledgeRuntime.state(player);
        ResearchProgressState next = reconcile(player, before, active());
        if (!next.equals(before)) KnowledgeRuntime.updateState(player, next);
    }

    static ResearchProgressState reconcile(ServerPlayer player, ResearchProgressState state,
                                           Collection<XpQuestCatalogData.Spec> specs) {
        if (specs.isEmpty()) return state; // nothing registered: leave every save untouched
        // Q1-B: history enters through per-key baselines captured once (XpQuestHooks), not by seeding.
        ResearchProgressState next = migrate(state);
        XpQuestEvaluator evaluator = new XpQuestEvaluator(player, next);
        for (XpQuestCatalogData.Spec spec : specs) {
            QuestDefinition quest = QuestCatalog.byId(spec.id());
            if (quest == null) continue;
            long observed;
            try {
                observed = evaluator.value(spec.tracker());
            } catch (RuntimeException failure) {
                // Fail closed for this quest only: no progress, and joins/ticks keep working.
                if (FAILED.add(spec.id()))
                    dev.itzgamesgd.moreminecrafts.MoreMinecrafts.LOGGER.error("XP Quest tracker failed for {}", spec.id(), failure);
                continue;
            }
            next = QuestRules.raiseProgress(next, quest, observed);
        }
        return next;
    }

    /** Schema 0 → 1 adds no transformation: earlier saves have no XP Quest keys to convert. */
    static ResearchProgressState migrate(ResearchProgressState state) {
        int schema = state.get(SCHEMA_KEY);
        if (schema >= SCHEMA_VERSION) return state;
        return state.with(SCHEMA_KEY, SCHEMA_VERSION);
    }
}
