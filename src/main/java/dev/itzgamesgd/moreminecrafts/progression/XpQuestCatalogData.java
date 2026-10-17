package dev.itzgamesgd.moreminecrafts.progression;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The approved 200-quest XP ledger and each quest's tracker, loaded from
 * {@code data/moreminecrafts/xp_quest/catalog.json}. Pure data: no Minecraft classes, so the
 * ledger invariants are checked by plain JUnit as well as at server start.
 */
public final class XpQuestCatalogData {
    public static final String RESOURCE = "/data/moreminecrafts/xp_quest/catalog.json";
    public static final Set<String> STATUSES = Set.of("NOW", "VERIFY", "FUTURE");
    public static final Set<String> TIERS = Set.of("EARLY", "EARLY_MID", "MID", "MID_LATE", "LATE", "ENDGAME");
    /** Tracker kinds Q1-A can evaluate; {@code pending} quests get their hooks in Q1-B and later. */
    public static final Set<String> EVALUABLE_KINDS = Set.of("counter", "stat_custom", "stat_block_items_used",
            "stat_block_items_distinct", "stat_killed", "stat_killed_monsters", "stat_killed_monsters_distinct",
            "stat_mined", "knowledge_seen", "knowledge_count", "research_nodes", "research_roots", "research_tier",
            "research_breadth", "research_discipline_complete", "research_value",
            "counter_sum", "placement", "distinct_set", "distinct_groups", "stat_crafted_distinct");

    public record Tracker(String kind, String key, List<String> entries, int divisor, String seedCondition) {
        public Tracker {
            entries = entries == null ? List.of() : List.copyOf(entries);
            divisor = divisor <= 0 ? 1 : divisor;
            key = key == null ? "" : key;
        }
        public boolean evaluable() { return EVALUABLE_KINDS.contains(kind); }
    }

    public record Spec(String id, String tab, String tier, String status, int rewardRawXp, int target,
                       Tracker tracker, boolean registered) {
        public QuestDefinition definition() {
            String base = "moreminecrafts." + id;
            return new QuestDefinition(id, tab, base + ".title", base + ".description", base + ".objective",
                    target, rewardRawXp);
        }
    }

    public record Catalog(int schemaVersion, int approvedActiveTotal, int approvedFutureTotal, int approvedTotal,
                          List<Spec> quests) {
        public Catalog { quests = List.copyOf(quests); }
        public int total(String status) {
            return quests.stream().filter(q -> q.status().equals(status)).mapToInt(Spec::rewardRawXp).sum();
        }
    }

    private static volatile Catalog loaded;

    private XpQuestCatalogData() {}

    public static Catalog catalog() {
        Catalog current = loaded;
        if (current == null) {
            var stream = XpQuestCatalogData.class.getResourceAsStream(RESOURCE);
            if (stream == null) throw new IllegalStateException("Missing XP Quest catalog " + RESOURCE);
            try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                current = parse(reader);
            } catch (java.io.IOException failure) {
                throw new IllegalStateException("Unreadable XP Quest catalog", failure);
            }
            loaded = current;
        }
        return current;
    }

    public static Catalog parse(Reader reader) {
        Catalog catalog = new Gson().fromJson(reader, Catalog.class);
        List<String> problems = validate(catalog);
        if (!problems.isEmpty()) throw new IllegalStateException("Invalid XP Quest catalog: " + problems);
        return catalog;
    }

    /** Ledger and registration invariants. An empty list means the catalog is valid. */
    public static List<String> validate(Catalog catalog) {
        List<String> problems = new ArrayList<>();
        if (catalog == null || catalog.quests() == null) return List.of("catalog missing");
        if (catalog.schemaVersion() != 1) problems.add("schemaVersion " + catalog.schemaVersion());
        if (catalog.quests().size() != 200) problems.add("quest count " + catalog.quests().size());
        Set<String> ids = new HashSet<>();
        for (Spec q : catalog.quests()) {
            if (q.id() == null || !q.id().matches("xpq\\.[a-z]+\\.[a-z0-9_]+")) problems.add("bad id " + q.id());
            else if (!ids.add(q.id())) problems.add("duplicate id " + q.id());
            if (!STATUSES.contains(q.status())) problems.add(q.id() + " status " + q.status());
            if (!TIERS.contains(q.tier())) problems.add(q.id() + " tier " + q.tier());
            if (q.rewardRawXp() <= 0 || q.rewardRawXp() % 50 != 0) problems.add(q.id() + " reward " + q.rewardRawXp());
            if (q.target() <= 0) problems.add(q.id() + " target " + q.target());
            if (q.tracker() == null || q.tracker().kind() == null) problems.add(q.id() + " tracker missing");
            // Authority: VERIFY/FUTURE are never registered; a NOW quest needs an implemented tracker.
            if (q.registered() && !"NOW".equals(q.status())) problems.add(q.id() + " registered while " + q.status());
            if (q.registered() && (q.tracker() == null || !q.tracker().evaluable()))
                problems.add(q.id() + " registered without an implemented tracker");
            if (q.tracker() != null && "counter".equals(q.tracker().kind()) && q.tracker().key().isBlank())
                problems.add(q.id() + " counter without key");
        }
        int active = catalog.total("NOW") + catalog.total("VERIFY");
        int future = catalog.total("FUTURE");
        if (active != catalog.approvedActiveTotal() || active != 180_000) problems.add("active ledger " + active);
        if (future != catalog.approvedFutureTotal() || future != 29_100) problems.add("future ledger " + future);
        if (active + future != catalog.approvedTotal() || active + future != 209_100) problems.add("total ledger " + (active + future));
        return problems;
    }
}
