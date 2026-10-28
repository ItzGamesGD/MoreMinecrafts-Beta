package dev.itzgamesgd.moreminecrafts.progression;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.level.GameType;

/** C5: real Raid.tick victory processing; hero credit is the vanilla raid-win award. */
public final class QuestRaidGameTests implements CustomTestMethodInvoker {
    private static final String OMEN = "xpq.boss.omen_raid";
    private static final String RAIDS = "xpq.cmb.raids_3";
    private static final UUID RESTART_ID = UUID.fromString("9d0c3a55-6c4f-4be1-8e0f-3a1b2c4d5e01");

    private static void set(Object target, String field, Object value) throws ReflectiveOperationException {
        Field f = Raid.class.getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }

    /** A started raid at its last wave with no raiders left, ready to turn to victory on the next tick. */
    private static Raid finishedRaid(GameTestHelper ctx, int omen, boolean inVillage) throws ReflectiveOperationException {
        ServerLevel level = ctx.getLevel();
        ctx.assertTrue(level.getDifficulty() != Difficulty.PEACEFUL, "raids cannot run on peaceful");
        BlockPos center = inVillage ? ctx.absolutePos(new BlockPos(1, 1, 1)) : new BlockPos(5000, 70, 5000);
        if (!inVillage) level.getChunkSource().addTicketWithRadius(net.minecraft.server.level.TicketType.FORCED,
                net.minecraft.world.level.ChunkPos.containing(center), 1);
        if (!inVillage) level.getChunk(center.getX() >> 4, center.getZ() >> 4);
        if (inVillage && level.getPoiManager().sectionsToVillage(net.minecraft.core.SectionPos.of(center)) > 1)
            level.getPoiManager().add(center, BuiltInRegistries.POINT_OF_INTEREST_TYPE.getOrThrow(PoiTypes.HOME));
        Raid raid = new Raid(center, level.getDifficulty());
        raid.setRaidOmenLevel(omen);
        set(raid, "started", true);
        set(raid, "postRaidTicks", 40);
        set(raid, "groupsSpawned", raid.getNumGroups(level.getDifficulty()) + (omen > 1 ? 1 : 0));
        return raid;
    }

    private static int wins(ServerPlayer player) { return QuestRules.counter(KnowledgeRuntime.state(player), "raid_victories"); }

    private static int omenWins(ServerPlayer player) { return QuestRules.counter(KnowledgeRuntime.state(player), "omen_raid_wins"); }

    private static boolean done(ServerPlayer player, String quest) { return KnowledgeRuntime.state(player).questComplete(quest); }

    @GameTest
    public void omenLevelThreeOrHigherVictoryCreditsEveryHeroButNobodyElse(GameTestHelper ctx) throws ReflectiveOperationException {
        ServerLevel level = ctx.getLevel();
        ServerPlayer hero = ctx.makeMockServerPlayerInLevel();
        ServerPlayer secondHero = ctx.makeMockServerPlayerInLevel();
        ServerPlayer bystander = ctx.makeMockServerPlayerInLevel();
        Raid raid = finishedRaid(ctx, 3, true);
        raid.addHeroOfTheVillage(hero);
        raid.addHeroOfTheVillage(secondHero);
        raid.tick(level);
        ctx.assertTrue(raid.isVictory(), "the raid turned to victory");
        ctx.assertTrue(done(hero, OMEN) && done(secondHero, OMEN) && omenWins(hero) == 1 && omenWins(secondHero) == 1,
                "every hero of the village is credited once");
        ctx.assertTrue(!done(bystander, OMEN) && wins(bystander) == 0, "a player who was not a hero gets nothing");
        for (int omen = 4; omen <= 5; omen++) {
            ServerPlayer veteran = ctx.makeMockServerPlayerInLevel();
            Raid higher = finishedRaid(ctx, omen, true);
            higher.addHeroOfTheVillage(veteran);
            higher.tick(level);
            ctx.assertTrue(done(veteran, OMEN), "omen level " + omen + " qualifies");
        }
        ctx.succeed();
    }

    @GameTest
    public void lowerOmenLevelsCountAsRaidWinsButNotAsOmenWins(GameTestHelper ctx) throws ReflectiveOperationException {
        ServerLevel level = ctx.getLevel();
        ServerPlayer hero = ctx.makeMockServerPlayerInLevel();
        for (int omen = 0; omen <= 2; omen++) {
            Raid raid = finishedRaid(ctx, omen, true);
            raid.addHeroOfTheVillage(hero);
            raid.tick(level);
            ctx.assertTrue(raid.isVictory(), "omen level " + omen + " raid was won");
        }
        ctx.assertTrue(omenWins(hero) == 0 && !done(hero, OMEN), "omen levels 0 to 2 never qualify");
        ctx.assertTrue(wins(hero) == 3 && done(hero, RAIDS), "three victories complete the village-guardian quest");
        ServerPlayer twice = ctx.makeMockServerPlayerInLevel();
        for (int i = 0; i < 2; i++) {
            Raid raid = finishedRaid(ctx, 1, true);
            raid.addHeroOfTheVillage(twice);
            raid.tick(level);
        }
        ctx.assertTrue(wins(twice) == 2 && !done(twice, RAIDS), "two victories are insufficient");
        ctx.succeed();
    }

    @GameTest
    public void failedStoppedAndHerolessRaidsCreditNobody(GameTestHelper ctx) throws ReflectiveOperationException {
        ServerLevel level = ctx.getLevel();
        ServerPlayer hero = ctx.makeMockServerPlayerInLevel();
        // The village is gone while waves had spawned: vanilla turns this into LOSS.
        Raid lost = finishedRaid(ctx, 4, false);
        lost.addHeroOfTheVillage(hero);
        Pillager survivor = EntityTypes.PILLAGER.create(level, EntitySpawnReason.COMMAND);
        survivor.setPos(lost.getCenter().getX() + 0.5D, lost.getCenter().getY(), lost.getCenter().getZ() + 0.5D);
        level.addFreshEntity(survivor);
        lost.joinRaid(level, 1, survivor, lost.getCenter(), false);
        lost.tick(level);
        ctx.assertTrue(lost.isLoss() && !lost.isVictory(), "the raid was lost");
        survivor.hurtServer(level, hero.damageSources().playerAttack(hero), 100_000F);
        for (int i = 0; i < 10; i++) lost.tick(level);
        ctx.assertTrue(lost.isLoss(), "a lost raid stays lost even after its last raider dies");
        // Cancelled / abandoned.
        Raid stopped = finishedRaid(ctx, 4, true);
        stopped.addHeroOfTheVillage(hero);
        stopped.stop();
        for (int i = 0; i < 3; i++) stopped.tick(level);
        ctx.assertTrue(stopped.isStopped() && !stopped.isVictory(), "the raid was stopped");
        // Incomplete: another wave is still to come.
        Raid incomplete = finishedRaid(ctx, 4, true);
        set(incomplete, "groupsSpawned", 1);
        incomplete.addHeroOfTheVillage(hero);
        incomplete.tick(level);
        ctx.assertTrue(!incomplete.isVictory(), "an unfinished raid is not a victory");
        // Victory with nobody who qualifies.
        Raid herolessWin = finishedRaid(ctx, 5, true);
        herolessWin.tick(level);
        ctx.assertTrue(herolessWin.isVictory(), "a heroless raid can still end in victory");
        ctx.assertTrue(wins(hero) == 0 && omenWins(hero) == 0 && !done(hero, OMEN) && !done(hero, RAIDS),
                "failed, stopped, incomplete and heroless raids credit nobody");
        ctx.succeed();
    }

    @GameTest
    public void spectatorOfflineAndRepeatedProcessingCannotCredit(GameTestHelper ctx) throws ReflectiveOperationException {
        ServerLevel level = ctx.getLevel();
        ServerPlayer spectator = QuestPersistentPlayers.mock(level, GameType.SPECTATOR);
        ServerPlayer present = ctx.makeMockServerPlayerInLevel();
        Raid raid = finishedRaid(ctx, 4, true);
        raid.addHeroOfTheVillage(spectator);
        Field heroes = Raid.class.getDeclaredField("heroesOfTheVillage");
        heroes.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Set<UUID> heroIds = (java.util.Set<UUID>) heroes.get(raid);
        heroIds.add(UUID.fromString("00000000-0000-0000-0000-00000000dead")); // a hero who is not online
        raid.addHeroOfTheVillage(present);
        raid.tick(level);
        ctx.assertTrue(wins(spectator) == 0, "a spectating hero is excluded by vanilla and so by the quest");
        ctx.assertTrue(wins(present) == 1 && omenWins(present) == 1, "the present hero is credited");
        // Celebration and removal ticks after the victory must not credit again.
        for (int i = 0; i < 700; i++) raid.tick(level);
        ctx.assertTrue(wins(present) == 1 && omenWins(present) == 1 && wins(spectator) == 0,
                "repeated raid processing adds nothing");
        ServerPlayer reloaded = ctx.makeMockServerPlayerInLevel();
        var out = net.minecraft.world.level.storage.TagValueOutput.createWithContext(
                net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess());
        present.saveWithoutId(out);
        reloaded.load(net.minecraft.world.level.storage.TagValueInput.create(
                net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess(), out.buildResult()));
        ctx.assertTrue(done(reloaded, OMEN) && wins(reloaded) == 1, "player save and load keeps raid credit");
        ctx.succeed();
    }

    @GameTest
    public void heroesComeFromRealRaiderKillsAndAreCreditedWhenTheRaidIsWon(GameTestHelper ctx) throws ReflectiveOperationException {
        ServerLevel level = ctx.getLevel();
        ServerPlayer killer = ctx.makeMockServerPlayerInLevel();
        ServerPlayer helper = ctx.makeMockServerPlayerInLevel();
        Raid raid = finishedRaid(ctx, 3, true);
        set(raid, "groupsSpawned", 1);
        Pillager pillager = EntityTypes.PILLAGER.create(level, EntitySpawnReason.COMMAND);
        BlockPos at = ctx.absolutePos(new BlockPos(2, 1, 2));
        pillager.setPos(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D);
        level.addFreshEntity(pillager);
        raid.joinRaid(level, 1, pillager, at, false);
        ctx.assertTrue(pillager.getCurrentRaid() == raid, "the real raider belongs to the raid");
        pillager.hurtServer(level, killer.damageSources().playerAttack(killer), 100_000F);
        // The helper only watched; the killer's final blow made them a hero of the village.
        set(raid, "groupsSpawned", raid.getNumGroups(level.getDifficulty()) + 1);
        raid.tick(level);
        ctx.assertTrue(raid.isVictory(), "no raiders remain, so the raid was won");
        ctx.assertTrue(done(killer, OMEN) && wins(killer) == 1 && !done(helper, OMEN) && wins(helper) == 0,
                "only the player whose blow killed a raider is credited");
        ctx.succeed();
    }

    /** Run with moreminecrafts.questraid.restart.phase=write|read; without it the test is a no-op. */
    @GameTest(maxTicks = 200)
    public void raidCreditSurvivesACleanRestart(GameTestHelper ctx) throws ReflectiveOperationException {
        String phase = System.getProperty("moreminecrafts.questraid.restart.phase");
        if (phase == null) { ctx.succeed(); return; }
        ServerLevel level = ctx.getLevel();
        if (phase.equals("write")) {
            ServerPlayer hero = QuestPersistentPlayers.join(ctx, RESTART_ID, "xpq-raid-restart", false);
            for (int omen : new int[]{4, 1}) {
                Raid raid = finishedRaid(ctx, omen, true);
                raid.addHeroOfTheVillage(hero);
                raid.tick(level);
            }
            ctx.assertTrue(done(hero, OMEN) && wins(hero) == 2 && !done(hero, RAIDS), "write JVM: one omen win, two wins");
            level.getServer().getPlayerList().saveAll();
            System.out.println("QUEST_RAID_RESTART_WRITE player=" + RESTART_ID);
            ctx.succeed();
            return;
        }
        ServerPlayer hero = QuestPersistentPlayers.join(ctx, RESTART_ID, "xpq-raid-restart", true);
        ctx.assertTrue(done(hero, OMEN) && wins(hero) == 2 && omenWins(hero) == 1, "read JVM restored raid credit");
        Raid third = finishedRaid(ctx, 2, true);
        third.addHeroOfTheVillage(hero);
        third.tick(level);
        ctx.assertTrue(done(hero, RAIDS) && omenWins(hero) == 1, "a third victory completes the guardian quest only");
        int xp = RawXp.available(hero);
        ctx.assertTrue(QuestProgressService.claim(hero, OMEN) == QuestProgressService.ClaimResult.CLAIMED
                && QuestProgressService.claim(hero, OMEN) == QuestProgressService.ClaimResult.ALREADY_CLAIMED
                && RawXp.available(hero) == xp + 3150, "the restored omen quest claims exactly once");
        System.out.println("QUEST_RAID_RESTART_READ player=" + RESTART_ID);
        ctx.succeed();
    }

    @Override public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
