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

    private static int counter(ServerPlayer player, String key) {
        return QuestRules.counter(KnowledgeRuntime.state(player), key);
    }

    private static void write(GameTestHelper ctx, ServerPlayer player) {
        killWith(ctx, player, "moreminecrafts:iron_dagger", EntityTypes.ZOMBIE);
        killWith(ctx, player, "moreminecrafts:iron_greatsword", mm("cave_zombie"));
        killWith(ctx, player, "minecraft:iron_sword", mm("bandit"));
        killWith(ctx, player, "minecraft:iron_sword", EntityTypes.ELDER_GUARDIAN);
        XpQuestTracking.reconcile(player);
        ResearchProgressState state = KnowledgeRuntime.state(player);
        ctx.assertTrue(killed(player, EntityTypes.ZOMBIE) == 1 && killed(player, mm("cave_zombie")) == 1
                && counter(player, "kills_dagger") == 1 && counter(player, "kills_greatsword") == 1
                && counter(player, "kills_bandit") == 1, "live kills recorded before saving");
        ctx.assertTrue(state.questComplete("xpq.cmb.mm_arms") && state.questComplete("xpq.boss.elder_guardian")
                && state.questProgress("xpq.cmb.bestiary_5") == 2, "completed and partial quests before saving");
    }

    private static void verify(GameTestHelper ctx, ServerPlayer player) {
        ResearchProgressState state = KnowledgeRuntime.state(player);
        ctx.assertTrue(killed(player, EntityTypes.ZOMBIE) == 1 && killed(player, mm("cave_zombie")) == 1
                && killed(player, mm("bandit")) == 1 && killed(player, EntityTypes.ELDER_GUARDIAN) == 1,
                "kill statistics restored");
        ctx.assertTrue(counter(player, "kills_dagger") == 1 && counter(player, "kills_greatsword") == 1
                && counter(player, "kills_bandit") == 1 && counter(player, "kills_bandit_archer") == 0,
                "event counters restored");
        XpQuestTracking.reconcile(player);
        state = KnowledgeRuntime.state(player);
        ctx.assertTrue(state.questComplete("xpq.cmb.mm_arms") && state.questComplete("xpq.boss.elder_guardian")
                && state.questProgress("xpq.cmb.bestiary_5") == 2 && state.questProgress("xpq.cmb.hostile_100") == 4
                && !state.questComplete("xpq.cmb.bestiary_5"), "quest state and statistic-derived progress restored");
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
