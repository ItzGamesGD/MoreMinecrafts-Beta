package dev.itzgamesgd.moreminecrafts.progression;

import java.lang.reflect.Method;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Kill evidence across disconnect/rejoin (default) and a clean two-JVM restart
 * ({@code moreminecrafts.questcombat.restart.phase=write|read}). Kill statistics live in the vanilla stats
 * file, counters and completion in the player record; both must come back together.
 */
public final class QuestCombatRestartGameTests implements CustomTestMethodInvoker {
    private static final UUID PLAYER_ID = UUID.fromString("a41f6d02-97b3-4e5b-b6f0-2a7d9c1e5501");
    private static final String NAME = "xpq-combat-restart";

    private static void killWith(GameTestHelper ctx, ServerPlayer player, String item, EntityType<? extends LivingEntity> type) {
        player.setItemInHand(InteractionHand.MAIN_HAND,
                new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(item))));
        ServerLevel level = ctx.getLevel();
        LivingEntity victim = type.create(level, EntitySpawnReason.COMMAND);
        BlockPos pos = ctx.absolutePos(new BlockPos(1, 1, 1));
        victim.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        level.addFreshEntity(victim);
        victim.hurtServer(level, player.damageSources().playerAttack(player), 100_000F);
    }

    @SuppressWarnings("unchecked")
    private static EntityType<? extends LivingEntity> mm(String path) {
        return (EntityType<? extends LivingEntity>) BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse("moreminecrafts:" + path));
    }

    private static int killed(ServerPlayer player, EntityType<?> type) {
        return player.getStats().getValue(Stats.ENTITY_KILLED.get(type));
    }

    private static void resetKillStat(ServerPlayer player, EntityType<?> type) {
        player.getStats().setValue(player, Stats.ENTITY_KILLED.get(type), 0);
    }

    private static int counter(ServerPlayer player, String key) {
        return QuestRules.counter(KnowledgeRuntime.state(player), key);
    }

    private static void write(GameTestHelper ctx, ServerPlayer player) {
        // This fixed-UUID fixture is reused across local runs. Clear its prior player record and
        // vanilla Monster stats so earlier read-phase kills cannot seed or inflate this run.
        KnowledgeRuntime.updateState(player, ResearchProgressState.empty());
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE)
            if (type.getCategory() == net.minecraft.world.entity.MobCategory.MONSTER) resetKillStat(player, type);
        resetKillStat(player, mm("cave_bat"));
        killWith(ctx, player, "moreminecrafts:iron_dagger", EntityTypes.ZOMBIE);
        killWith(ctx, player, "moreminecrafts:iron_greatsword", mm("cave_zombie"));
        killWith(ctx, player, "minecraft:iron_sword", mm("bandit"));
        killWith(ctx, player, "minecraft:iron_sword", EntityTypes.ELDER_GUARDIAN);
        for (String path : new String[] {"cave_zombie", "cave_skeleton", "cave_creeper", "small_spider", "swamp_spider", "cave_bat"})
            killWith(ctx, player, "minecraft:iron_sword", mm(path));
        for (String path : new String[] {"desert_spider", "swamp_zombie", "frozen_zombie", "fungal_zombie", "wild_slime"})
            killWith(ctx, player, "minecraft:iron_sword", mm(path));
        BuiltInRegistries.ENTITY_TYPE.stream()
                .filter(type -> type.getCategory() == net.minecraft.world.entity.MobCategory.MONSTER)
                .filter(type -> BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace().equals("moreminecrafts"))
                .sorted(java.util.Comparator.comparing(type -> BuiltInRegistries.ENTITY_TYPE.getKey(type).toString()))
                .limit(25)
                .forEach(type -> player.getStats().setValue(player, Stats.ENTITY_KILLED.get(type),
                        Math.max(1, killed(player, type))));
        QuestCombatGameTests.reconcileCaveDwellersForTest(player);
        XpQuestTracking.reconcile(player);
        ResearchProgressState state = KnowledgeRuntime.state(player);
        ctx.assertTrue(killed(player, EntityTypes.ZOMBIE) == 1 && killed(player, mm("cave_zombie")) == 2
                && counter(player, "kills_dagger") == 1 && counter(player, "kills_greatsword") == 1
                && counter(player, "kills_bandit") == 1,
                "live kills recorded before saving: zombie=" + killed(player, EntityTypes.ZOMBIE)
                        + " cave_zombie=" + killed(player, mm("cave_zombie"))
                        + " dagger=" + counter(player, "kills_dagger")
                        + " greatsword=" + counter(player, "kills_greatsword")
                        + " bandit=" + counter(player, "kills_bandit"));
        ctx.assertTrue(state.questComplete("xpq.cmb.mm_arms") && state.questComplete("xpq.boss.elder_guardian")
                && state.questProgress("xpq.cmb.bestiary_5") == 5
                && state.questComplete("xpq.cmb.cave_dwellers")
                && state.questComplete("xpq.cmb.bestiary_25")
                && state.questComplete("xpq.cmb.habitats_5"),
                "completed and partial quests before saving: arms=" + state.questComplete("xpq.cmb.mm_arms")
                        + " elder=" + state.questComplete("xpq.boss.elder_guardian")
                        + " bestiary=" + state.questProgress("xpq.cmb.bestiary_5")
                        + " bestiary25=" + state.questProgress("xpq.cmb.bestiary_25")
                        + " habitats=" + state.questProgress("xpq.cmb.habitats_5")
                        + " cave=" + state.questProgress("xpq.cmb.cave_dwellers")
                        + "/" + state.questComplete("xpq.cmb.cave_dwellers"));
    }

    private static void verify(GameTestHelper ctx, ServerPlayer player) {
        ResearchProgressState state = KnowledgeRuntime.state(player);
        ctx.assertTrue(killed(player, EntityTypes.ZOMBIE) == 1 && killed(player, mm("cave_zombie")) == 2
                && killed(player, mm("bandit")) == 1 && killed(player, EntityTypes.ELDER_GUARDIAN) == 1,
                "kill statistics restored");
        for (String path : new String[] {"cave_zombie", "cave_skeleton", "cave_creeper", "small_spider", "swamp_spider", "cave_bat"})
            ctx.assertTrue(killed(player, mm(path)) >= 1, "cave-dwellers statistic restored: " + path);
        ctx.assertTrue(counter(player, "kills_dagger") == 1 && counter(player, "kills_greatsword") == 1
                && counter(player, "kills_bandit") == 1 && counter(player, "kills_bandit_archer") == 0,
                "event counters restored");
        for (String path : new String[] {"desert_spider", "swamp_zombie", "frozen_zombie", "fungal_zombie", "wild_slime"})
            ctx.assertTrue(killed(player, mm(path)) >= 1, "habitat candidate statistic restored: " + path);
        XpQuestTracking.reconcile(player);
        state = KnowledgeRuntime.state(player);
        ctx.assertTrue(state.questComplete("xpq.cmb.mm_arms") && state.questComplete("xpq.boss.elder_guardian")
                && state.questProgress("xpq.cmb.bestiary_5") == 5 && state.questProgress("xpq.cmb.hostile_100") >= 9
                && state.questComplete("xpq.cmb.cave_dwellers"),
                "quest state and statistic-derived progress restored: arms=" + state.questComplete("xpq.cmb.mm_arms")
                        + " elder=" + state.questComplete("xpq.boss.elder_guardian")
                        + " bestiary=" + state.questProgress("xpq.cmb.bestiary_5")
                        + " hostile=" + state.questProgress("xpq.cmb.hostile_100")
                        + " cave=" + state.questProgress("xpq.cmb.cave_dwellers")
                        + "/" + state.questComplete("xpq.cmb.cave_dwellers"));
        ctx.assertTrue(state.questProgress("xpq.cmb.bestiary_25") == 25
                        && state.questComplete("xpq.cmb.bestiary_25")
                        && state.questProgress("xpq.cmb.habitats_5") == 5
                        && state.questComplete("xpq.cmb.habitats_5"),
                "the new bestiary and habitat completion evidence restore and reconcile after the clean restart");
        var bestiaryUi = ProgressionScreenData.quests(state, "combat").stream()
                .filter(row -> row.id().equals("xpq.cmb.bestiary_25")).findFirst().orElseThrow();
        long completedTargets = bestiaryUi.groups().getFirst().targets().stream()
                .filter(ProgressionScreenData.QuestTarget::complete).count();
        long persistedMonsterStats = BuiltInRegistries.ENTITY_TYPE.stream()
                .filter(type -> type.getCategory() == net.minecraft.world.entity.MobCategory.MONSTER)
                .filter(type -> BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace().equals("moreminecrafts"))
                .filter(type -> killed(player, type) > 0).count();
        ctx.assertTrue(bestiaryUi.groups().getFirst().targets().size() == 37
                        && completedTargets == persistedMonsterStats && completedTargets >= 25,
                "the restored player's checklist has all 37 entries and matches persisted kills: UI="
                        + completedTargets + " stats=" + persistedMonsterStats);
        ResearchProgressState caveState = QuestCombatGameTests.reconcileCaveDwellersForTest(player);
        ctx.assertTrue(caveState.questProgress("xpq.cmb.cave_dwellers") == 6
                        && caveState.questComplete("xpq.cmb.cave_dwellers"),
                "the persisted six-species evidence still reconciles after process restart");
        killWith(ctx, player, "moreminecrafts:iron_dagger", EntityTypes.SKELETON);
        ctx.assertTrue(counter(player, "kills_dagger") == 2 && killed(player, EntityTypes.SKELETON) == 1,
                "new kills continue from the restored totals");
        int xp = RawXp.available(player);
        ctx.assertTrue(QuestProgressService.claim(player, "xpq.cmb.mm_arms") == QuestProgressService.ClaimResult.CLAIMED
                && QuestProgressService.claim(player, "xpq.cmb.mm_arms") == QuestProgressService.ClaimResult.ALREADY_CLAIMED
                && RawXp.available(player) == xp + 250, "the restored completion claims exactly once (250 raw XP)");
    }

    @GameTest(maxTicks = 200)
    public void killEvidenceSurvivesRejoinAndCleanRestart(GameTestHelper ctx) {
        String phase = System.getProperty("moreminecrafts.questcombat.restart.phase");
        ServerLevel level = ctx.getLevel();
        if ("read".equals(phase)) {
            verify(ctx, QuestPersistentPlayers.join(ctx, PLAYER_ID, NAME, true));
            System.out.println("QUEST_COMBAT_RESTART_READ player=" + PLAYER_ID);
            ctx.succeed();
            return;
        }
        ServerPlayer player = QuestPersistentPlayers.join(ctx, PLAYER_ID, NAME, false);
        write(ctx, player);
        level.getServer().getPlayerList().saveAll();
        if ("write".equals(phase)) {
            System.out.println("QUEST_COMBAT_RESTART_WRITE player=" + PLAYER_ID);
            ctx.succeed();
            return;
        }
        // Disconnect and rejoin in the same process: the new player object has nothing but the saved records.
        level.getServer().getPlayerList().remove(player);
        ServerPlayer rejoined = QuestPersistentPlayers.join(ctx, PLAYER_ID, NAME, true);
        ctx.assertTrue(rejoined != player, "a new player object represents the rejoin");
        verify(ctx, rejoined);
        ctx.succeed();
    }

    @Override public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
