package dev.itzgamesgd.moreminecrafts.progression;

import java.lang.reflect.Method;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

/** Q1-A: persistent progress, uncapped counters, distinct sets, stat reads and retroactive reconciliation. */
public final class XpQuestTrackingGameTests implements CustomTestMethodInvoker {
    private static XpQuestCatalogData.Spec activate(String id) {
        XpQuestCatalogData.Spec spec = XpQuestCatalogData.catalog().quests().stream()
                .filter(q -> q.id().equals(id)).findFirst().orElseThrow();
        XpQuestTracking.register(spec);
        return spec;
    }

    private static ResearchProgressState state(ServerPlayer player) { return KnowledgeRuntime.state(player); }

    @GameTest
    public void vanillaStatisticsReconcileRetroactivelyAndIdempotently(GameTestHelper ctx) {
        activate("xpq.frm.breed_200");
        activate("xpq.cmb.hostile_100");
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        // History recorded before the quest system evaluated this player.
        player.awardStat(Stats.ANIMALS_BRED, 150);
        player.awardStat(Stats.ENTITY_KILLED.get(EntityTypes.ZOMBIE), 60);
        player.awardStat(Stats.ENTITY_KILLED.get(EntityTypes.COW), 500); // not a monster
        XpQuestTracking.reconcile(player);
        ctx.assertTrue(state(player).questProgress("xpq.frm.breed_200") == 150
                && !state(player).questComplete("xpq.frm.breed_200"), "retroactive partial from vanilla stats");
        ctx.assertTrue(state(player).questProgress("xpq.cmb.hostile_100") == 60, "only monster kills count");
        player.awardStat(Stats.ANIMALS_BRED, 50);
        player.awardStat(Stats.ENTITY_KILLED.get(EntityTypes.SKELETON), 40);
        XpQuestTracking.reconcile(player);
        ctx.assertTrue(state(player).questComplete("xpq.frm.breed_200")
                && state(player).questComplete("xpq.cmb.hostile_100"), "completes at target");
        ResearchProgressState settled = state(player);
        XpQuestTracking.reconcile(player);
        ctx.assertTrue(state(player) == settled, "re-reconciliation writes nothing (no revision bump)");
        ctx.succeed();
    }

    @GameTest
    public void knowledgeObservationKeysGiveFullRetroactiveDistinctProgress(GameTestHelper ctx) {
        activate("xpq.fsh.full_catalogue");
        activate("xpq.fsh.ocean_trio");
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        ResearchProgressState s = state(player);
        for (String fish : new String[]{"crucian_carp", "common_carp", "amur_catfish", "rainbow_trout", "largemouth_bass",
                "northern_pike", "atlantic_salmon", "yellowfin_tuna", "arapaima", "swordfish", "blue_marlin"})
            s = s.with(KnowledgeConditionSemantics.distinctObservationKey("fish_species_caught", fish), 1);
        KnowledgeRuntime.updateState(player, s);
        XpQuestTracking.reconcile(player);
        ctx.assertTrue(state(player).questProgress("xpq.fsh.full_catalogue") == 11
                && !state(player).questComplete("xpq.fsh.full_catalogue"), "11 of 12 species");
        ctx.assertTrue(state(player).questComplete("xpq.fsh.ocean_trio"), "named subset completes");
        KnowledgeRuntime.recordDistinctCondition(player, "fish_species_caught", "beluga_sturgeon");
        KnowledgeRuntime.recordDistinctCondition(player, "fish_species_caught", "beluga_sturgeon");
        XpQuestTracking.reconcile(player);
        ctx.assertTrue(state(player).questComplete("xpq.fsh.full_catalogue")
                && state(player).questProgress("xpq.fsh.full_catalogue") == 12, "duplicate observation counted once");
        ctx.succeed();
    }

    @GameTest
    public void cappedKnowledgeCounterSeedsAndUncappedCounterContinues(GameTestHelper ctx) {
        activate("xpq.frm.harvest_1000");
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        for (int i = 1; i <= 150; i++) KnowledgeRuntime.recordCondition(player, "crops_harvested", i);
        ctx.assertTrue(state(player).conditionProgress("crops_harvested") == 100, "Knowledge counter stays capped at 100");
        XpQuestTracking.reconcile(player);
        ctx.assertTrue(QuestRules.counter(state(player), "crops_harvested") == 100
                && state(player).questProgress("xpq.frm.harvest_1000") == 100, "seeded only from recorded evidence");
        XpQuestTracking.addCounter(player, "crops_harvested", 899);
        XpQuestTracking.reconcile(player);
        ctx.assertTrue(state(player).questProgress("xpq.frm.harvest_1000") == 999
                && !state(player).questComplete("xpq.frm.harvest_1000"), "counter passes the Knowledge cap");
        XpQuestTracking.addCounter(player, "crops_harvested", 1);
        XpQuestTracking.reconcile(player);
        ctx.assertTrue(state(player).questComplete("xpq.frm.harvest_1000"), "completes at 1,000");
        ctx.assertTrue(state(player).conditionProgress("crops_harvested") == 100, "Knowledge semantics unchanged");
        ctx.succeed();
    }

    @GameTest
    public void ownedResearchValueAndCountsAreRetroactive(GameTestHelper ctx) {
        activate("xpq.grw.spent_10k");
        activate("xpq.grw.seven_roots");
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        ResearchProgressState s = state(player);
        long value = 0;
        for (ResearchDefinition d : ResearchCatalog.allCatalogEntries()) {
            if (value >= 10_000) break;
            s = s.with("research." + d.id(), d.maximumLevel());
            for (int l = 1; l <= d.maximumLevel(); l++) value += d.cost(l).rawXp();
        }
        for (ResearchDefinition d : ResearchCatalog.allCatalogEntries())
            if (d.id().endsWith("_root")) s = s.with("research." + d.id(), 1);
        KnowledgeRuntime.updateState(player, s);
        XpQuestTracking.reconcile(player);
        ctx.assertTrue(state(player).questComplete("xpq.grw.spent_10k"), "owned value " + value + " >= 10,000");
        ctx.assertTrue(state(player).questComplete("xpq.grw.seven_roots"), "all seven roots owned");
        ctx.succeed();
    }

    @GameTest
    public void successfulBlockPlacementCountsAndCancelledDoesNot(GameTestHelper ctx) {
        activate("xpq.bld.place_1000");
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        BlockPos floor = new BlockPos(1, 1, 1);
        ctx.setBlock(floor, Blocks.STONE);
        ctx.placeAt(player, new ItemStack(Items.OAK_PLANKS), floor, Direction.UP);
        ctx.assertBlockPresent(Blocks.OAK_PLANKS, floor.above());
        ctx.placeAt(player, new ItemStack(Items.OAK_PLANKS), floor.above(), Direction.UP);
        XpQuestTracking.reconcile(player);
        ctx.assertTrue(state(player).questProgress("xpq.bld.place_1000") == 2, "two successful placements: "
                + state(player).questProgress("xpq.bld.place_1000"));
        player.setGameMode(GameType.ADVENTURE); // placement is refused in Adventure mode
        ctx.placeAt(player, new ItemStack(Items.OAK_PLANKS), floor.above(2), Direction.UP);
        XpQuestTracking.reconcile(player);
        ctx.assertTrue(state(player).questProgress("xpq.bld.place_1000") == 2, "refused placement adds nothing");
        ctx.succeed();
    }

    @GameTest
    public void progressSurvivesSaveLoadDeathCopyAndStaysPerPlayer(GameTestHelper ctx) {
        activate("xpq.frm.harvest_1000");
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        XpQuestTracking.addCounter(player, "crops_harvested", 1_000);
        KnowledgeRuntime.updateState(player, QuestRules.observeDistinct(state(player), "xpq.test.distinct", "x"));
        XpQuestTracking.reconcile(player);
        ctx.assertTrue(state(player).questComplete("xpq.frm.harvest_1000"), "complete before save");

        TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, ctx.getLevel().registryAccess());
        player.saveWithoutId(out);
        CompoundTag saved = out.buildResult();
        ServerPlayer restarted = ctx.makeMockServerPlayerInLevel();
        restarted.load(TagValueInput.create(ProblemReporter.DISCARDING, ctx.getLevel().registryAccess(), saved));
        ctx.assertTrue(state(restarted).questComplete("xpq.frm.harvest_1000")
                && QuestRules.counter(state(restarted), "crops_harvested") == 1_000
                && QuestRules.distinctCount(state(restarted), "xpq.test.distinct") == 1
                && state(restarted).get(XpQuestTracking.SCHEMA_KEY) == XpQuestTracking.SCHEMA_VERSION,
                "save/load keeps completion, counters, distinct sets and schema");

        ResearchProgressState beforeDeath = state(player);
        ServerPlayer respawned = ctx.getLevel().getServer().getPlayerList().respawn(
                player, false, net.minecraft.world.entity.Entity.RemovalReason.KILLED);
        var diff = new java.util.TreeSet<String>();
        beforeDeath.counters().forEach((k, v) -> { if (!k.equals("progression.revision") && !v.equals(state(respawned).counters().get(k))) diff.add(k + "=" + v + "/" + state(respawned).counters().get(k)); });
        ctx.assertTrue(diff.isEmpty(), "death copy keeps the whole progression state: " + diff);

        ctx.assertTrue(state(respawned).questComplete("xpq.frm.harvest_1000"), "death keeps completion");
        ServerPlayer other = ctx.makeMockServerPlayerInLevel();
        XpQuestTracking.reconcile(other);
        ctx.assertTrue(!state(other).questComplete("xpq.frm.harvest_1000")
                && QuestRules.counter(state(other), "crops_harvested") == 0, "another player is unaffected");
        ctx.succeed();
    }

    @GameTest
    public void claimedQuestIsFrozenAndUnregisteredQuestsAreIgnored(GameTestHelper ctx) {
        activate("xpq.frm.trades_200");
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        player.awardStat(Stats.TRADED_WITH_VILLAGER, 200);
        XpQuestTracking.reconcile(player);
        KnowledgeRuntime.updateState(player, state(player).claimQuest("xpq.frm.trades_200"));
        ResearchProgressState claimed = state(player);
        player.awardStat(Stats.TRADED_WITH_VILLAGER, 500);
        XpQuestTracking.reconcile(player);
        ctx.assertTrue(state(player).questClaimed("xpq.frm.trades_200")
                && state(player).questProgress("xpq.frm.trades_200") == claimed.questProgress("xpq.frm.trades_200"),
                "claimed quest is never rewritten");
        player.awardStat(Stats.CUSTOM.get(Stats.BOAT_ONE_CM), 3_000_000);
        XpQuestTracking.reconcile(player);
        ctx.assertTrue(state(player).questProgress("xpq.exp.sail_20k") == 0, "unregistered quest is not evaluated");
        ctx.assertTrue(QuestCatalog.byId("xpq.boss.monument_clear") == null, "VERIFY quest is not registered");
        ctx.succeed();
    }

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
