package dev.itzgamesgd.moreminecrafts.progression;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

/**
 * Reads persisted evidence for one tracker: vanilla statistics, Knowledge distinct-observation
 * keys, uncapped quest counters and owned research. Every source here is a durable server
 * record, so the same evaluation serves live progress and retroactive reconciliation.
 */
final class XpQuestEvaluator {
    private final ServerPlayer player;
    private final ResearchProgressState state;
    private final Map<String, Long> memo = new HashMap<>();

    XpQuestEvaluator(ServerPlayer player, ResearchProgressState state) {
        this.player = player;
        this.state = state;
    }

    long value(XpQuestCatalogData.Tracker tracker) { return value(tracker, Integer.MAX_VALUE); }

    long value(XpQuestCatalogData.Tracker tracker, int target) {
        return switch (tracker.kind()) {
            case "counter" -> (baseline(tracker.seedCondition()) + QuestRules.counter(state, tracker.key())) / tracker.divisor();
            case "counter_each" -> tracker.entries().stream().filter(k -> QuestRules.counter(state, k) > 0).count();
            case "counter_gated" -> gatedSum(tracker, target);
            case "counter_sum" -> tracker.entries().stream().mapToLong(k -> QuestRules.counter(state, k)).sum();
            case "placement" -> baseline(XpQuestHooks.BLOCK_ITEMS_USED_BASELINE)
                    + QuestRules.counter(state, XpQuestHooks.BLOCKS_PLACED);
            case "distinct_set" -> tracker.entries().isEmpty() ? QuestRules.distinctCount(state, tracker.key())
                    : tracker.entries().stream().filter(e -> state.get(QuestRules.seenKey(tracker.key(), e)) != 0).count();
            case "distinct_groups" -> tracker.entries().stream().filter(group -> java.util.Arrays.stream(group.split("\\|"))
                    .anyMatch(e -> state.get(QuestRules.seenKey(tracker.key(), e)) != 0)).count();
            case "stat_crafted_distinct" -> tracker.entries().stream().filter(id -> {
                var item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(id));
                return item.isPresent() && stats().getValue(Stats.ITEM_CRAFTED.get(item.get())) > 0;
            }).count();
            case "stat_custom" -> customStats(tracker) / tracker.divisor();
            case "stat_block_items_used" -> memo("block_items_used", () -> blockItemsUsed(false));
            case "stat_block_items_distinct" -> memo("block_items_distinct", () -> blockItemsUsed(true));
            case "stat_killed" -> killed(tracker);
            case "stat_killed_each" -> killedEach(tracker);
            case "stat_killed_groups_each" -> tracker.entries().stream().filter(group -> java.util.Arrays.stream(group.split("\\|"))
                    .skip(1).anyMatch(id -> killedType(id) > 0)).count();
            case "stat_killed_monsters" -> memo("monsters:" + tracker.key(), () -> monsters(tracker.key(), false));
            case "stat_killed_monsters_distinct" -> memo("monster_types:" + tracker.key(), () -> monsters(tracker.key(), true));
            case "stat_mined" -> mined(tracker);
            case "knowledge_seen" -> knowledgeSeen(tracker);
            case "knowledge_count" -> knowledgeCount("resolved".equals(tracker.key()));
            default -> XpQuestResearchValue.value(state, tracker);
        };
    }

    /** Baseline captured once by XpQuestHooks; absent or blank means no recognised history. */
    private long baseline(String key) {
        return key == null || key.isBlank() ? 0 : Math.max(0, state.get(XpQuestHooks.baselineKey(key)));
    }

    private long memo(String key, java.util.function.LongSupplier supplier) {
        return memo.computeIfAbsent(key, ignored -> supplier.getAsLong());
    }

    private ServerStatsCounter stats() { return player.getStats(); }

    private long customStats(XpQuestCatalogData.Tracker tracker) {
        long sum = 0;
        for (String id : tracker.entries()) {
            // StatType.get resolves by registered value, so use the registry's own Identifier instance.
            Identifier registered = BuiltInRegistries.CUSTOM_STAT.getValue(Identifier.parse(id));
            if (registered != null) sum += stats().getValue(Stats.CUSTOM.get(registered));
        }
        return sum;
    }

    /** Successful placements: 26.3 ItemStack.useOn awards ITEM_USED only when the use succeeds. */
    private long blockItemsUsed(boolean distinct) {
        long sum = 0;
        for (Item item : BuiltInRegistries.ITEM) {
            if (!(item instanceof BlockItem)) continue;
            int used = stats().getValue(Stats.ITEM_USED.get(item));
            if (used > 0) sum += distinct ? 1 : used;
        }
        return sum;
    }

    private long killed(XpQuestCatalogData.Tracker tracker) {
        long sum = 0;
        for (String id : tracker.entries()) {
            var type = BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(id));
            if (type.isPresent()) sum += stats().getValue(Stats.ENTITY_KILLED.get(type.get()));
        }
        return sum;
    }

    /** Distinct entity types of the list with at least one credited kill. */
    private long killedEach(XpQuestCatalogData.Tracker tracker) {
        long types = 0;
        for (String id : tracker.entries()) {
            var type = BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(id));
            if (type.isPresent() && stats().getValue(Stats.ENTITY_KILLED.get(type.get())) > 0) types++;
        }
        return types;
    }

    private int killedType(String id) {
        var type = BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(id));
        return type.map(entityType -> stats().getValue(Stats.ENTITY_KILLED.get(entityType))).orElse(0);
    }

    /** Sum of the counters, but the quest cannot complete until every listed counter is at least one. */
    private long gatedSum(XpQuestCatalogData.Tracker tracker, int target) {
        long sum = tracker.entries().stream().mapToLong(k -> QuestRules.counter(state, k)).sum();
        boolean all = tracker.entries().stream().allMatch(k -> QuestRules.counter(state, k) > 0);
        return all ? sum : Math.min(sum, Math.max(0, (long) target - 1));
    }

    private long monsters(String namespace, boolean distinct) {
        long sum = 0;
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (type.getCategory() != MobCategory.MONSTER) continue;
            if (!namespace.isEmpty() && !BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace().equals(namespace)) continue;
            int kills = stats().getValue(Stats.ENTITY_KILLED.get(type));
            if (kills > 0) sum += distinct ? 1 : kills;
        }
        return sum;
    }

    private long mined(XpQuestCatalogData.Tracker tracker) {
        long sum = 0;
        for (String id : tracker.entries()) {
            var block = BuiltInRegistries.BLOCK.getOptional(Identifier.parse(id));
            if (block.isPresent()) sum += stats().getValue(Stats.BLOCK_MINED.get(block.get()));
        }
        return sum;
    }

    /** Uncapped Knowledge observation keys (recordDistinctCondition always writes them). */
    private long knowledgeSeen(XpQuestCatalogData.Tracker tracker) {
        if (!tracker.entries().isEmpty()) {
            return tracker.entries().stream()
                    .filter(e -> KnowledgeConditionSemantics.alreadyObserved(state.counters(), tracker.key(), e))
                    .count();
        }
        String prefix = "condition." + tracker.key() + ".seen.";
        Set<String> seen = new HashSet<>();
        state.counters().forEach((k, v) -> { if (v != 0 && k.startsWith(prefix)) seen.add(k); });
        return seen.size();
    }

    private long knowledgeCount(boolean resolvedOnly) {
        return KnowledgeCatalog.entries().stream()
                .filter(e -> !resolvedOnly || !e.unresolved())
                .filter(e -> state.hasKnowledge(e.number()))
                .count();
    }
}
