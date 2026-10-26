package dev.itzgamesgd.moreminecrafts.progression;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.end.DragonRespawnStage;
import net.minecraft.world.level.dimension.end.EnderDragonFight;

/** C4: the real EnderDragonFight lifecycle (create, defeat, respawn stage END), driven in the End level. */
public final class QuestDragonRematchGameTests implements CustomTestMethodInvoker {
    private static final String QUEST = "xpq.boss.dragon_rematch";
    private static final UUID OFFLINE_ID = UUID.fromString("c2f1b6a0-5b17-4f65-a3a7-9d0e4c8b7701");

    private static Object get(Object target, String field) throws ReflectiveOperationException {
        Field f = EnderDragonFight.class.getDeclaredField(field);
        f.setAccessible(true);
        return f.get(target);
    }

    private static void set(Object target, String field, Object value) throws ReflectiveOperationException {
        Field f = EnderDragonFight.class.getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static EnderDragonFight freshFight(GameTestHelper ctx, ServerLevel end) throws ReflectiveOperationException {
        EnderDragonFight fight = end.getDragonFight();
        ctx.assertTrue(fight != null, "the End has a dragon fight");
        for (EnderDragon leftover : end.getDragons()) leftover.discard();
        set(fight, "dragonKilled", false);
        set(fight, "hasPreviouslyKilledDragon", false);
        set(fight, "dragonUUID", null);
        set(fight, "needsStateScanning", false);
        data(end).takePending(OFFLINE_ID);
        return fight;
    }

    private static QuestDragonRematchData data(ServerLevel end) {
        return end.getServer().overworld().getDataStorage().computeIfAbsent(QuestDragonRematchData.TYPE);
    }

    /** The fight's own private dragon factory, the same one every real spawn and respawn stage uses. */
    private static EnderDragon spawnFightDragon(EnderDragonFight fight) throws ReflectiveOperationException {
        Method create = EnderDragonFight.class.getDeclaredMethod("createNewDragon");
        create.setAccessible(true);
        EnderDragon dragon = (EnderDragon) create.invoke(fight);
        dragon.getPhaseManager().setPhase(EnderDragonPhase.SITTING_SCANNING);
        return dragon;
    }

    /** The legitimate respawn path: respawn stage END clears the killed flag and creates a fresh fight dragon. */
    private static EnderDragon respawn(EnderDragonFight fight, ServerLevel end) throws ReflectiveOperationException {
        set(fight, "respawnStage", DragonRespawnStage.START);
        Method stage = EnderDragonFight.class.getDeclaredMethod("setRespawnStage", DragonRespawnStage.class);
        stage.setAccessible(true);
        stage.invoke(fight, DragonRespawnStage.END);
        EnderDragon dragon = (EnderDragon) end.getEntity(fight.dragonUUID());
        dragon.getPhaseManager().setPhase(EnderDragonPhase.SITTING_SCANNING);
        return dragon;
    }

    private static void defeat(ServerLevel end, EnderDragon dragon, DamageSource source) throws ReflectiveOperationException {
        dragon.hurtServer(end, source, 1_000_000F);
        Method tickDeath = EnderDragon.class.getDeclaredMethod("tickDeath");
        tickDeath.setAccessible(true);
        for (int i = 0; i < 205 && !dragon.isRemoved(); i++) tickDeath.invoke(dragon);
    }

    private static boolean complete(ServerPlayer player) { return KnowledgeRuntime.state(player).questComplete(QUEST); }

    private static int credits(ServerPlayer player) {
        return QuestRules.counter(KnowledgeRuntime.state(player), QuestDragonRematch.COUNTER);
    }

    @GameTest(maxTicks = 400)
    public void firstKillIsNotARematchAndTheRespawnedDragonCreditsOnlyItsKiller(GameTestHelper ctx) throws ReflectiveOperationException {
        ServerLevel end = ctx.getLevel().getServer().getLevel(Level.END);
        EnderDragonFight fight = freshFight(ctx, end);
        ServerPlayer first = ctx.makeMockServerPlayerInLevel();
        ServerPlayer second = ctx.makeMockServerPlayerInLevel();
        ServerPlayer present = ctx.makeMockServerPlayerInLevel();

        EnderDragon original = spawnFightDragon(fight);
        ctx.assertTrue(!fight.hasPreviouslyKilledDragon(), "fresh fight has no previous kill");
        defeat(end, original, first.damageSources().playerAttack(first));
        ctx.assertTrue(original.isRemoved() && fight.hasPreviouslyKilledDragon(), "the first dragon completed the fight");
        ctx.assertTrue(!complete(first) && credits(first) == 0, "the first dragon defeat is not a rematch");
        ctx.assertTrue(first.getStats().getValue(net.minecraft.stats.Stats.ENTITY_KILLED.get(
                net.minecraft.world.entity.EntityTypes.ENDER_DRAGON)) == 1,
                "the first kill still records the vanilla dragon statistic (twin bosses)");

        EnderDragon rematch = respawn(fight, end);
        ctx.assertTrue(!rematch.getUUID().equals(original.getUUID()), "a genuinely new dragon was spawned");
        defeat(end, rematch, second.damageSources().playerAttack(second));
        ctx.assertTrue(rematch.isRemoved() && complete(second) && credits(second) == 1,
                "the respawned dragon's credited killer completes the rematch");
        ctx.assertTrue(!complete(first) && !complete(present), "the first-kill player and a present bystander get nothing");

        // Repeated completion callbacks for the same dragon never credit again.
        fight.setDragonKilled(rematch);
        fight.setDragonKilled(rematch);
        ctx.assertTrue(credits(second) == 1, "repeated callbacks add nothing");

        EnderDragon third = respawn(fight, end);
        defeat(end, third, ctx.getLevel().damageSources().explosion(null, null));
        ctx.assertTrue(third.isRemoved() && credits(second) == 1 && !complete(present) && credits(first) == 0,
                "a dragon that dies with no attributable player credits nobody and raises nothing");
        ctx.succeed();
    }

    @GameTest(maxTicks = 400)
    public void anotherPlayersLateDamageDecidesTheCreditNotThePresenceInTheEnd(GameTestHelper ctx) throws ReflectiveOperationException {
        ServerLevel end = ctx.getLevel().getServer().getLevel(Level.END);
        EnderDragonFight fight = freshFight(ctx, end);
        ServerPlayer summoner = ctx.makeMockServerPlayerInLevel();
        ServerPlayer finisher = ctx.makeMockServerPlayerInLevel();
        defeat(end, spawnFightDragon(fight), summoner.damageSources().playerAttack(summoner));
        EnderDragon rematch = respawn(fight, end);
        rematch.hurtServer(end, summoner.damageSources().playerAttack(summoner), 10F);
        rematch.hurtServer(end, finisher.damageSources().playerAttack(finisher), 1_000_000F);
        ctx.assertTrue(rematch.getKillCredit() == finisher, "vanilla credit is the last player attacker");
        Method tickDeath;
        try {
            tickDeath = EnderDragon.class.getDeclaredMethod("tickDeath");
            tickDeath.setAccessible(true);
            for (int i = 0; i < 205 && !rematch.isRemoved(); i++) tickDeath.invoke(rematch);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException(failure);
        }
        ctx.assertTrue(complete(finisher) && !complete(summoner),
                "only the credited finisher completes, not the player who summoned or first damaged the dragon");
        ctx.succeed();
    }

    @GameTest(maxTicks = 400)
    public void aDragonOutsideTheFightOrACommandKillNeverCredits(GameTestHelper ctx) throws ReflectiveOperationException {
        ServerLevel end = ctx.getLevel().getServer().getLevel(Level.END);
        EnderDragonFight fight = freshFight(ctx, end);
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        defeat(end, spawnFightDragon(fight), player.damageSources().playerAttack(player));
        // a stray dragon that is not the fight's own dragon
        EnderDragon stray = net.minecraft.world.entity.EntityTypes.ENDER_DRAGON.create(end,
                net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        stray.setDragonFight(fight);
        end.addFreshEntity(stray);
        stray.getPhaseManager().setPhase(EnderDragonPhase.SITTING_SCANNING);
        defeat(end, stray, player.damageSources().playerAttack(player));
        ctx.assertTrue(credits(player) == 0, "a dragon that is not the fight's dragon never credits");
        // /kill style removal of the real rematch dragon carries no player
        EnderDragon rematch = respawn(fight, end);
        rematch.kill(end);
        ctx.assertTrue(credits(player) == 0 && fight.hasPreviouslyKilledDragon(), "kill() credits nobody");
        ctx.succeed();
    }

    @GameTest(maxTicks = 400)
    public void anOfflineKillerReceivesTheCreditOnceAtNextLogin(GameTestHelper ctx) throws ReflectiveOperationException {
        ServerLevel end = ctx.getLevel().getServer().getLevel(Level.END);
        EnderDragonFight fight = freshFight(ctx, end);
        ServerPlayer first = ctx.makeMockServerPlayerInLevel();
        defeat(end, spawnFightDragon(fight), first.damageSources().playerAttack(first));
        ServerPlayer killer = QuestPersistentPlayers.join(ctx, OFFLINE_ID, "xpq-dragon-offline", false);
        EnderDragon rematch = respawn(fight, end);
        rematch.hurtServer(end, killer.damageSources().playerAttack(killer), 1_000_000F);
        ctx.assertTrue(data(end).candidate(rematch.getUUID()) != null, "credit is recorded in world data when the dragon dies");
        ctx.getLevel().getServer().getPlayerList().saveAll();
        ctx.getLevel().getServer().getPlayerList().remove(killer);
        Method tickDeath = EnderDragon.class.getDeclaredMethod("tickDeath");
        tickDeath.setAccessible(true);
        for (int i = 0; i < 205 && !rematch.isRemoved(); i++) tickDeath.invoke(rematch);
        ctx.assertTrue(data(end).hasPending(OFFLINE_ID) && data(end).candidate(rematch.getUUID()) == null,
                "the offline killer's credit is pending and the candidate is consumed");
        ServerPlayer back = QuestPersistentPlayers.join(ctx, OFFLINE_ID, "xpq-dragon-offline", true);
        ctx.assertTrue(complete(back) && credits(back) == 1 && !data(end).hasPending(OFFLINE_ID),
                "login applies the pending credit and clears it");
        ctx.getLevel().getServer().getPlayerList().saveAll();
        ctx.getLevel().getServer().getPlayerList().remove(back);
        ServerPlayer again = QuestPersistentPlayers.join(ctx, OFFLINE_ID, "xpq-dragon-offline", true);
        ctx.assertTrue(credits(again) == 1, "a later login adds nothing");
        ctx.succeed();
    }

    private static final UUID RESTART_ID = UUID.fromString("c2f1b6a0-5b17-4f65-a3a7-9d0e4c8b7702");

    /** Run with moreminecrafts.questdragon.restart.phase=write|read; without it this is a no-op. */
    @GameTest(maxTicks = 400)
    public void rematchCreditSurvivesACleanRestart(GameTestHelper ctx) throws ReflectiveOperationException {
        String phase = System.getProperty("moreminecrafts.questdragon.restart.phase");
        if (phase == null) { ctx.succeed(); return; }
        ServerLevel end = ctx.getLevel().getServer().getLevel(Level.END);
        if (phase.equals("write")) {
            EnderDragonFight fight = freshFight(ctx, end);
            data(end).takePending(RESTART_ID);
            ServerPlayer first = ctx.makeMockServerPlayerInLevel();
            defeat(end, spawnFightDragon(fight), first.damageSources().playerAttack(first));
            ServerPlayer killer = QuestPersistentPlayers.join(ctx, RESTART_ID, "xpq-dragon-restart", false);
            EnderDragon rematch = respawn(fight, end);
            rematch.hurtServer(end, killer.damageSources().playerAttack(killer), 1_000_000F);
            ctx.getLevel().getServer().getPlayerList().saveAll();
            ctx.getLevel().getServer().getPlayerList().remove(killer);
            Method tickDeath = EnderDragon.class.getDeclaredMethod("tickDeath");
            tickDeath.setAccessible(true);
            for (int i = 0; i < 205 && !rematch.isRemoved(); i++) tickDeath.invoke(rematch);
            ctx.assertTrue(data(end).hasPending(RESTART_ID) && fight.hasPreviouslyKilledDragon(),
                    "the rematch completed while its credited killer was offline");
            ctx.getLevel().getServer().overworld().getDataStorage().saveAndJoin();
            System.out.println("QUEST_DRAGON_RESTART_WRITE player=" + RESTART_ID);
            ctx.succeed();
            return;
        }
        ctx.assertTrue(end.getDragonFight().hasPreviouslyKilledDragon(), "the fight's previous-kill flag was restored");
        ctx.assertTrue(data(end).hasPending(RESTART_ID), "the pending rematch credit was restored");
        ServerPlayer killer = QuestPersistentPlayers.join(ctx, RESTART_ID, "xpq-dragon-restart", true);
        ctx.assertTrue(complete(killer) && credits(killer) == 1 && !data(end).hasPending(RESTART_ID),
                "the restored credit applies once at login");
        ctx.getLevel().getServer().getPlayerList().saveAll();
        ctx.getLevel().getServer().getPlayerList().remove(killer);
        ServerPlayer again = QuestPersistentPlayers.join(ctx, RESTART_ID, "xpq-dragon-restart", true);
        ctx.assertTrue(credits(again) == 1, "a second login grants nothing");
        System.out.println("QUEST_DRAGON_RESTART_READ player=" + RESTART_ID);
        ctx.succeed();
    }

    @Override public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
