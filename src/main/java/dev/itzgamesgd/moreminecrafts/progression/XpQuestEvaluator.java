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

    long value(XpQuestCatalogData.Tracker tracker) {
        return switch (tracker.kind()) {
            case "counter" -> QuestRules.counter(state, tracker.key());
            case "stat_custom" -> customStats(tracker) / tracker.divisor();
            case "stat_block_items_used" -> memo("block_items_used", () -> blockItemsUsed(false));
            case "stat_block_items_distinct" -> memo("block_items_distinct", () -> blockItemsUsed(true));
            case "stat_killed" -> killed(tracker);
            case "stat_killed_monsters" -> memo("monsters:" + tracker.key(), () -> monsters(tracker.key(), false));
            case "stat_killed_monsters_distinct" -> memo("monster_types:" + tracker.key(), () -> monsters(tracker.key(), true));
            case "stat_mined" -> mined(tracker);
            case "knowledge_seen" -> knowledgeSeen(tracker);
            case "knowledge_count" -> knowledgeCount("resolved".equals(tracker.key()));
            default -> XpQuestResearchValue.value(state, tracker);
        };
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
