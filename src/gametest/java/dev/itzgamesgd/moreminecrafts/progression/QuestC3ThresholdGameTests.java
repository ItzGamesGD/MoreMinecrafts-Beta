package dev.itzgamesgd.moreminecrafts.progression;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;

/**
 * Persisted-evidence threshold for every quest registered by Q1-C (those without a Q1-B style
 * {@code threshold_*} test): target - 1 is incomplete with exact progress, target completes, another player
 * has nothing. Source-specific positive, negative, attribution and persistence tests live in the
 * subsystem test classes; this proves the tracker, target and registration agree for each quest.
 */
public final class QuestC3ThresholdGameTests implements CustomTestMethodInvoker {
    private static Set<String> alreadyCovered() {
        Set<String> ids = new HashSet<>();
        for (Method m : XpQuestEventTrackingGameTests.class.getDeclaredMethods())
            if (m.getName().startsWith("threshold_")) {
                String rest = m.getName().substring("threshold_".length());
                int cut = rest.indexOf('_');
                ids.add("xpq." + rest.substring(0, cut) + "." + rest.substring(cut + 1));
            }
        return ids;
    }

    private static List<EntityType<?>> monsters(String namespace) {
        return BuiltInRegistries.ENTITY_TYPE.stream().filter(t -> t.getCategory() == MobCategory.MONSTER)
                .filter(t -> namespace.isEmpty() || BuiltInRegistries.ENTITY_TYPE.getKey(t).getNamespace().equals(namespace))
                .toList();
    }

    private static void clearKills(ServerPlayer player) {
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE)
            player.getStats().setValue(player, Stats.ENTITY_KILLED.get(type), 0);
    }

    /** Writes persisted evidence worth {@code amount} units of the quest target. */
    private static void inject(ServerPlayer player, XpQuestCatalogData.Spec spec, int amount) {
        XpQuestCatalogData.Tracker t = spec.tracker();
        ResearchProgressState s = KnowledgeRuntime.state(player);
        switch (t.kind()) {
            case "counter" -> s = s.with("quest.counter." + t.key(), amount * Math.max(1, t.divisor()));
            case "counter_each" -> { for (int i = 0; i < amount; i++) s = s.with("quest.counter." + t.entries().get(i), 1); }
            case "counter_sum" -> {
                for (String entry : t.entries()) s = s.with("quest.counter." + entry, 0);
                if (t.entries().size() >= 2) {
                    int first = amount / 2;
                    s = s.with("quest.counter." + t.entries().get(0), first)
                            .with("quest.counter." + t.entries().get(1), amount - first);
                } else if (!t.entries().isEmpty()) {
                    s = s.with("quest.counter." + t.entries().get(0), amount);
                }
            }
            case "counter_gated" -> {
                for (int i = 1; i < t.entries().size(); i++) s = s.with("quest.counter." + t.entries().get(i), 1);
                s = s.with("quest.counter." + t.entries().get(0), amount - (t.entries().size() - 1));
            }
            case "distinct_set" -> {
                for (int i = 0; i < amount; i++)
                    s = QuestRules.observeDistinct(s, t.key(), t.entries().isEmpty() ? "test:entry_" + i : t.entries().get(i));
            }
            case "stat_custom" -> {
                for (String entry : t.entries()) {
                    Identifier statId = BuiltInRegistries.CUSTOM_STAT.getValue(Identifier.parse(entry));
                    if (statId != null) player.getStats().setValue(player, Stats.CUSTOM.get(statId), 0);
                }
                if (!t.entries().isEmpty()) {
                    Identifier first = BuiltInRegistries.CUSTOM_STAT.getValue(Identifier.parse(t.entries().get(0)));
                    if (first != null) {
                        player.getStats().setValue(player, Stats.CUSTOM.get(first), amount * Math.max(1, t.divisor()));
                    }
                }
            }
            case "stat_mined" -> {
                for (String entry : t.entries()) {
                    var block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(entry));
                    player.getStats().setValue(player, Stats.BLOCK_MINED.get(block), 0);
                }
                if (!t.entries().isEmpty()) {
                    var first = BuiltInRegistries.BLOCK.getValue(Identifier.parse(t.entries().get(0)));
                    player.getStats().setValue(player, Stats.BLOCK_MINED.get(first), amount);
                }
            }
            case "knowledge_seen" -> {
                for (int i = 0; i < amount; i++) {
                    String entry = t.entries().isEmpty() ? "test:seen_" + i : t.entries().get(i);
                    s = s.with(KnowledgeConditionSemantics.distinctObservationKey(t.key(), entry), 1);
                }
            }
            case "knowledge_count" -> {
                List<KnowledgeCatalog.Entry> pool = KnowledgeCatalog.entries().stream()
                        .filter(e -> !"resolved".equals(t.key()) || !e.unresolved())
                        .toList();
                for (int i = 0; i < amount; i++) s = s.with(pool.get(i).stateKey(), 1);
            }
            case "research_nodes" -> {
                List<ResearchDefinition> pool = ResearchCatalog.allCatalogEntries().stream()
                        .filter(d -> !"non_root".equals(t.key()) || (!"ROOT".equals(d.tier()) && !d.id().endsWith("_root")))
                        .toList();
                for (int i = 0; i < amount; i++) s = s.with("research." + pool.get(i).id(), 1);
            }
            case "research_roots" -> {
                List<ResearchDefinition> roots = ResearchCatalog.allCatalogEntries().stream()
                        .filter(d -> "ROOT".equals(d.tier()) || d.id().endsWith("_root"))
                        .toList();
                for (int i = 0; i < amount; i++) s = s.with("research." + roots.get(i).id(), 1);
            }
            case "research_tier" -> {
                List<ResearchDefinition> pool = ResearchCatalog.allCatalogEntries().stream()
                        .filter(d -> t.key().equals(d.tier()))
                        .toList();
                for (int i = 0; i < amount; i++) s = s.with("research." + pool.get(i).id(), 1);
            }
            case "research_breadth" -> {
                List<ResearchDefinition> byDiscipline = new ArrayList<>();
                Set<String> seenDisciplines = new HashSet<>();
                for (ResearchDefinition d : ResearchCatalog.allCatalogEntries()) {
                    if (!"ROOT".equals(d.tier()) && !d.id().endsWith("_root") && seenDisciplines.add(d.discipline())) {
                        byDiscipline.add(d);
                    }
                }
                for (int i = 0; i < amount; i++) s = s.with("research." + byDiscipline.get(i).id(), 1);
            }
            case "research_discipline_complete" -> {
                if (amount >= 1) {
                    for (ResearchDefinition d : ResearchCatalog.allCatalogEntries()) {
                        if ("combat".equals(d.discipline())) s = s.with("research." + d.id(), d.maximumLevel());
                    }
                }
            }
            case "stat_killed" -> {
                clearKills(player);
                var type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(t.entries().get(0)));
                player.getStats().setValue(player, Stats.ENTITY_KILLED.get(type), amount);
            }
            case "stat_killed_each" -> {
                clearKills(player);
                for (int i = 0; i < amount; i++)
                    player.getStats().setValue(player, Stats.ENTITY_KILLED.get(
                            BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(t.entries().get(i)))), 3);
            }
            case "stat_killed_groups_each" -> {
                clearKills(player);
                for (int i = 0; i < amount; i++) {
                    String typeId = t.entries().get(i).split("\\|")[1];
                    player.getStats().setValue(player, Stats.ENTITY_KILLED.get(
                            BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(typeId))), 2);
                }
            }
            case "stat_killed_monsters" -> {
                clearKills(player);
                player.getStats().setValue(player, Stats.ENTITY_KILLED.get(EntityTypes.ZOMBIE), amount);
            }
            case "stat_killed_monsters_distinct" -> {
                clearKills(player);
                List<EntityType<?>> pool = monsters(t.key());
                for (int i = 0; i < amount; i++) player.getStats().setValue(player, Stats.ENTITY_KILLED.get(pool.get(i)), 2);
            }
            default -> throw new IllegalStateException("No injector for " + t.kind());
        }
        KnowledgeRuntime.updateState(player, s);
    }

    @GameTest(maxTicks = 400)
    public void everyQ1cRegisteredQuestCompletesExactlyAtItsTarget(GameTestHelper ctx) {
        Set<String> covered = alreadyCovered();
        List<String> failures = new ArrayList<>();
        int checked = 0;
        for (XpQuestCatalogData.Spec spec : XpQuestCatalogData.catalog().quests()) {
            if (!spec.registered() || covered.contains(spec.id())) continue;
            if (spec.tracker().kind().equals("distinct_groups") || spec.tracker().kind().equals("research_value")
                    || (spec.tracker().kind().equals("knowledge_seen") && "biomes_visited".equals(spec.tracker().key())
                        && spec.tracker().entries().isEmpty())
                    || spec.id().startsWith("xpq.exp.first_structure_loot")
                    || spec.tracker().key().startsWith("set.structure_loot")) continue; // C1 loot, research_value, and join-observed biomes have dedicated tests
            checked++;
            ServerPlayer player = ctx.makeMockServerPlayerInLevel();
            List<XpQuestCatalogData.Spec> one = List.of(spec);
            inject(player, spec, spec.target() - 1);
            ResearchProgressState partial = XpQuestTracking.reconcile(player, KnowledgeRuntime.state(player), one);
            if (partial.questComplete(spec.id()) || partial.questProgress(spec.id()) != spec.target() - 1)
                failures.add(spec.id() + " at target-1: progress " + partial.questProgress(spec.id()));
            inject(player, spec, spec.target());
            ResearchProgressState full = XpQuestTracking.reconcile(player, KnowledgeRuntime.state(player), one);
            if (!full.questComplete(spec.id())) failures.add(spec.id() + " not complete at target");
            ServerPlayer other = ctx.makeMockServerPlayerInLevel();
            ResearchProgressState nothing = XpQuestTracking.reconcile(other, KnowledgeRuntime.state(other), one);
            if (nothing.questComplete(spec.id()) || nothing.questProgress(spec.id()) != 0)
                failures.add(spec.id() + " gives another player progress " + nothing.questProgress(spec.id()));
        }
        System.out.println("QUEST_C3_THRESHOLDS checked=" + checked);
        ctx.assertTrue(checked == 111, "the table covered every registered quest without a dedicated test: " + checked);
        ctx.assertTrue(failures.isEmpty(), "threshold failures: " + failures);
        ctx.succeed();
    }

    @GameTest
    public void biomeSeenThresholdsWork(GameTestHelper ctx) {
        for (String id : List.of("xpq.exp.biomes_25", "xpq.exp.biomes_40")) {
            XpQuestCatalogData.Spec spec = XpQuestCatalogData.catalog().quests().stream()
                    .filter(q -> q.id().equals(id)).findFirst().orElseThrow();
            ServerPlayer player = ctx.makeMockServerPlayerInLevel();
            ServerPlayer other = ctx.makeMockServerPlayerInLevel();
            int base = XpQuestTracking.reconcile(player, KnowledgeRuntime.state(player), List.of(spec))
                    .questProgress(spec.id());
            int otherBefore = XpQuestTracking.reconcile(other, KnowledgeRuntime.state(other), List.of(spec))
                    .questProgress(spec.id());
            ResearchProgressState s = KnowledgeRuntime.state(player);
            for (int i = 0; i < spec.target() - 1 - base; i++) {
                s = s.with(KnowledgeConditionSemantics.distinctObservationKey("biomes_visited", "test:biome_" + i), 1);
            }
            KnowledgeRuntime.updateState(player, s);
            ResearchProgressState partial = XpQuestTracking.reconcile(player, KnowledgeRuntime.state(player), List.of(spec));
            ctx.assertTrue(!partial.questComplete(spec.id()) && partial.questProgress(spec.id()) == spec.target() - 1,
                    spec.id() + " at target-1: progress " + partial.questProgress(spec.id()));
            s = s.with(KnowledgeConditionSemantics.distinctObservationKey("biomes_visited", "test:biome_final"), 1);
            KnowledgeRuntime.updateState(player, s);
            ResearchProgressState full = XpQuestTracking.reconcile(player, KnowledgeRuntime.state(player), List.of(spec));
            ctx.assertTrue(full.questComplete(spec.id()), spec.id() + " complete at target");
            ResearchProgressState otherAfter = XpQuestTracking.reconcile(other, KnowledgeRuntime.state(other), List.of(spec));
            ctx.assertTrue(otherAfter.questProgress(spec.id()) == otherBefore && !otherAfter.questComplete(spec.id()),
                    spec.id() + " does not leak to another player");
        }
        ctx.succeed();
    }

    @GameTest
    public void researchValueThresholdsWork(GameTestHelper ctx) {
        XpQuestCatalogData.Spec spec10k = XpQuestCatalogData.catalog().quests().stream()
                .filter(q -> q.id().equals("xpq.grw.spent_10k")).findFirst().orElseThrow();
        XpQuestCatalogData.Spec spec50k = XpQuestCatalogData.catalog().quests().stream()
                .filter(q -> q.id().equals("xpq.grw.spent_50k")).findFirst().orElseThrow();

        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        ResearchProgressState s = KnowledgeRuntime.state(player);
        boolean checkedPartial10k = false;
        boolean checkedPartial50k = false;
        for (ResearchDefinition def : ResearchCatalog.allCatalogEntries()) {
            int max = def.maximumLevel();
            if (max <= 0) continue;
            ResearchProgressState candidate = s.with("research." + def.id(), max);
            if (!checkedPartial10k) {
                ResearchProgressState eval = XpQuestTracking.reconcile(player, candidate, List.of(spec10k));
                if (eval.questComplete(spec10k.id())) {
                    ResearchProgressState partial = XpQuestTracking.reconcile(player, s, List.of(spec10k));
                    ctx.assertTrue(!partial.questComplete(spec10k.id())
                                    && partial.questProgress(spec10k.id()) > 0
                                    && partial.questProgress(spec10k.id()) < spec10k.target(),
                            "spent_10k incomplete below 10000: " + partial.questProgress(spec10k.id()));
                    checkedPartial10k = true;
                }
            }
            if (!checkedPartial50k) {
                ResearchProgressState eval = XpQuestTracking.reconcile(player, candidate, List.of(spec50k));
                if (eval.questComplete(spec50k.id())) {
                    ResearchProgressState partial = XpQuestTracking.reconcile(player, s, List.of(spec50k));
                    ctx.assertTrue(!partial.questComplete(spec50k.id())
                                    && partial.questProgress(spec50k.id()) > 0
                                    && partial.questProgress(spec50k.id()) < spec50k.target(),
                            "spent_50k incomplete below 50000: " + partial.questProgress(spec50k.id()));
                    checkedPartial50k = true;
                }
            }
            s = candidate;
        }
        ResearchProgressState full = XpQuestTracking.reconcile(player, s, List.of(spec10k, spec50k));
        ctx.assertTrue(checkedPartial10k && checkedPartial50k, "verified partial thresholds below 10k and 50k");
        ctx.assertTrue(full.questComplete(spec10k.id()), "spent_10k completes at >= 10000");
        ctx.assertTrue(full.questComplete(spec50k.id()), "spent_50k completes at >= 50000");
        ctx.succeed();
    }

    @Override public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
