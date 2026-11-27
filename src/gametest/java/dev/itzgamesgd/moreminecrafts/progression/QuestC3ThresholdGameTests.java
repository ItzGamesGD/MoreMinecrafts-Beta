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
            case "counter" -> s = s.with("quest.counter." + t.key(), amount);
            case "counter_each" -> { for (int i = 0; i < amount; i++) s = s.with("quest.counter." + t.entries().get(i), 1); }
            case "counter_gated" -> {
                for (int i = 1; i < t.entries().size(); i++) s = s.with("quest.counter." + t.entries().get(i), 1);
                s = s.with("quest.counter." + t.entries().get(0), amount - (t.entries().size() - 1));
            }
            case "distinct_set" -> {
                for (int i = 0; i < amount; i++)
                    s = QuestRules.observeDistinct(s, t.key(), t.entries().isEmpty() ? "test:entry_" + i : t.entries().get(i));
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
            if (spec.tracker().kind().equals("distinct_groups") || spec.id().startsWith("xpq.exp.first_structure_loot")
                    || spec.tracker().key().startsWith("set.structure_loot")) continue; // C1 loot has its own live tests
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
        ctx.assertTrue(checked == 37, "the table covered every Q1-C registered quest without a Q1-B threshold test: " + checked);
        ctx.assertTrue(failures.isEmpty(), "threshold failures: " + failures);
        ctx.succeed();
    }

    @Override public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
