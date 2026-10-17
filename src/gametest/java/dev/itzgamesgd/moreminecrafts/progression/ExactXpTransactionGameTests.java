package dev.itzgamesgd.moreminecrafts.progression;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

/** Exact raw-XP writes and the claim-once transaction (Q1-A2). */
public final class ExactXpTransactionGameTests implements CustomTestMethodInvoker {
    private static final AtomicInteger IDS = new AtomicInteger();

    private static String completedQuest(ServerPlayer player, int reward) {
        String id = "gametest.exact_xp." + IDS.incrementAndGet();
        QuestCatalog.register(new QuestDefinition(id, "gametest", "t", "d", "o", 1, reward));
        QuestProgressService.recordProgress(player, id, 1);
        return id;
    }

    private static boolean consistent(ServerPlayer player, int expected) {
        return RawXp.available(player) == expected && player.totalExperience == expected;
    }

    /** Mock players default to a mode with infinite materials, which keeps XP costs free by design. */
    static ServerPlayer survival(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        // Orbs from neighbouring tests can reach a fresh mock player; start every test from zero.
        RawXp.setTotal(player, 0);
        player.setScore(0);
        return player;
    }

    @GameTest
    public void exactWritesKeepBalanceAndTotalExperienceAtBoundaries(GameTestHelper ctx) {
        ServerPlayer player = survival(ctx);
        for (int total : new int[]{0, 1, 314, 315, 316, 351, 352, 353, 1_394, 1_395, 1_396, 1_506, 1_507, 1_508, 100_000}) {
            RawXp.setTotal(player, total);
            ctx.assertTrue(consistent(player, total), "exact set at " + total + " got " + RawXp.available(player)
                    + "/" + player.totalExperience);
        }
        RawXp.setTotal(player, 100_000);
        ctx.assertTrue(RawXp.pay(player, 99_999) && consistent(player, 1), "large deduction is exact");
        ctx.assertTrue(!RawXp.pay(player, 2) && consistent(player, 1), "1 XP short is refused without mutation");
        ctx.assertTrue(RawXp.pay(player, 1) && consistent(player, 0), "exact balance can be spent to zero");
        ctx.assertTrue(!RawXp.pay(player, 1) && consistent(player, 0), "zero balance refuses any cost");
        RawXp.grant(player, 100_000);
        ctx.assertTrue(consistent(player, 100_000), "large grant is exact");
        ctx.succeed();
    }

    @GameTest
    public void claimIsOnceExactAndRefusesInvalidRequests(GameTestHelper ctx) {
        ServerPlayer player = survival(ctx);
        RawXp.setTotal(player, 350);
        String id = completedQuest(player, 3_150);
        ctx.assertTrue(QuestProgressService.claim(player, "gametest.exact_xp.unknown")
                == QuestProgressService.ClaimResult.UNKNOWN_QUEST && consistent(player, 350), "unknown id refused");
        String incomplete = "gametest.exact_xp.incomplete." + IDS.incrementAndGet();
        QuestCatalog.register(new QuestDefinition(incomplete, "gametest", "t", "d", "o", 5, 100));
        ctx.assertTrue(QuestProgressService.claim(player, incomplete)
                == QuestProgressService.ClaimResult.INCOMPLETE && consistent(player, 350), "incomplete refused");
        // Two requests handled in the same server tick.
        var first = QuestProgressService.claim(player, id);
        var second = QuestProgressService.claim(player, id);
        ctx.assertTrue(first == QuestProgressService.ClaimResult.CLAIMED
                && second == QuestProgressService.ClaimResult.ALREADY_CLAIMED, "second claim in the same tick refused");
        ctx.assertTrue(consistent(player, 3_500), "reward granted exactly once: " + RawXp.available(player));
        ctx.succeed();
    }

    @GameTest
    public void grantFailureRollsBackClaimAndXp(GameTestHelper ctx) {
        ServerPlayer player = survival(ctx);
        RawXp.setTotal(player, 1_507);
        String id = completedQuest(player, 500);
        QuestProgressService.claimFailureInjectionForTests = () -> { throw new IllegalStateException("injected"); };
        QuestProgressService.ClaimResult failed;
        try {
            failed = QuestProgressService.claim(player, id);
        } finally {
            QuestProgressService.claimFailureInjectionForTests = null;
        }
        ctx.assertTrue(failed == QuestProgressService.ClaimResult.INVALID_STATE, "failure reported");
        ctx.assertTrue(!KnowledgeRuntime.state(player).questClaimed(id)
                && KnowledgeRuntime.state(player).questComplete(id), "claim rolled back, completion kept");
        ctx.assertTrue(consistent(player, 1_507) && player.getScore() == 0, "XP and score unchanged after rollback");
        ctx.assertTrue(QuestProgressService.claim(player, id) == QuestProgressService.ClaimResult.CLAIMED
                && consistent(player, 2_007), "retry after rollback succeeds once");
        ctx.succeed();
    }

    @GameTest
    public void deadPlayerCannotClaim(GameTestHelper ctx) {
        ServerPlayer player = survival(ctx);
        String id = completedQuest(player, 100);
        player.setHealth(0.0F);
        ctx.assertTrue(QuestProgressService.claim(player, id) == QuestProgressService.ClaimResult.UNAVAILABLE
                && !KnowledgeRuntime.state(player).questClaimed(id) && consistent(player, 0), "dead player refused");
        ctx.succeed();
    }

    /**
     * In-process crash/restart model: the claimed flag (player attachment) and the XP fields are saved
     * in the same player record. Reloading the record saved before the claim (crash before autosave)
     * yields an unclaimed quest and the old XP; reloading the record saved after yields both changes.
     * Either way the reward is granted exactly once overall. A real JVM kill is not exercised here.
     */
    @GameTest
    public void savedPlayerRecordKeepsClaimAndXpTogether(GameTestHelper ctx) {
        ServerPlayer player = survival(ctx);
        RawXp.setTotal(player, 400);
        String id = completedQuest(player, 1_000);
        CompoundTag beforeClaim = save(ctx, player);
        ctx.assertTrue(QuestProgressService.claim(player, id) == QuestProgressService.ClaimResult.CLAIMED, "claimed");
        CompoundTag afterClaim = save(ctx, player);

        ServerPlayer crashed = survival(ctx);
        load(ctx, crashed, beforeClaim);
        ctx.assertTrue(!KnowledgeRuntime.state(crashed).questClaimed(id) && consistent(crashed, 400),
                "pre-claim record: unclaimed with old XP");
        ctx.assertTrue(QuestProgressService.claim(crashed, id) == QuestProgressService.ClaimResult.CLAIMED
                && consistent(crashed, 1_400), "pre-claim record: claim once");

        ServerPlayer restarted = survival(ctx);
        load(ctx, restarted, afterClaim);
        ctx.assertTrue(KnowledgeRuntime.state(restarted).questClaimed(id) && consistent(restarted, 1_400),
                "post-claim record: claimed with new XP");
        ctx.assertTrue(QuestProgressService.claim(restarted, id) == QuestProgressService.ClaimResult.ALREADY_CLAIMED
                && consistent(restarted, 1_400), "post-claim record: no second grant");
        ctx.succeed();
    }

    /** Score parity with vanilla: grants raise the score like giveExperiencePoints; spending does not. */
    @GameTest
    public void exactGrantMatchesVanillaScoreAndSpendingKeepsScore(GameTestHelper ctx) {
        ServerPlayer vanilla = survival(ctx);
        ServerPlayer exact = survival(ctx);
        vanilla.giveExperiencePoints(1_000);
        RawXp.grant(exact, 1_000);
        ctx.assertTrue(exact.getScore() == vanilla.getScore() && exact.getScore() == 1_000,
                "grant score " + exact.getScore() + " vs vanilla " + vanilla.getScore());
        ctx.assertTrue(exact.experienceLevel == vanilla.experienceLevel && exact.totalExperience == vanilla.totalExperience
                && RawXp.available(exact) == 1_000, "grant level/total match vanilla");
        int levelsBefore = vanilla.getScore();
        vanilla.giveExperienceLevels(-5); // vanilla spending path
        ctx.assertTrue(vanilla.getScore() == levelsBefore, "vanilla level spending keeps score");
        ctx.assertTrue(RawXp.pay(exact, 300) && exact.getScore() == 1_000, "exact spending keeps score too");
        ctx.succeed();
    }

    @GameTest
    public void scoreboardExperienceAndLevelCriteriaFollowExactWrites(GameTestHelper ctx) {
        ServerPlayer player = survival(ctx);
        var scoreboard = ctx.getLevel().getScoreboard();
        String suffix = Integer.toString(IDS.incrementAndGet());
        var xp = scoreboard.addObjective("q1xp" + suffix, net.minecraft.world.scores.criteria.ObjectiveCriteria.EXPERIENCE,
                net.minecraft.network.chat.Component.literal("xp"), net.minecraft.world.scores.criteria.ObjectiveCriteria.RenderType.INTEGER, false, null);
        var level = scoreboard.addObjective("q1lv" + suffix, net.minecraft.world.scores.criteria.ObjectiveCriteria.LEVEL,
                net.minecraft.network.chat.Component.literal("lv"), net.minecraft.world.scores.criteria.ObjectiveCriteria.RenderType.INTEGER, false, null);
        // doTick also runs Player.aiStep, which may pick up a stray orb from a neighbouring test, so
        // compare the criteria with the player's own fields after the tick rather than fixed numbers.
        RawXp.grant(player, 1_507);
        ctx.assertTrue(consistent(player, 1_507), "exact grant");
        player.doTick();
        ctx.assertTrue(scoreboard.getPlayerScoreInfo(player, xp).value() == player.totalExperience
                && player.totalExperience == RawXp.available(player) && player.totalExperience >= 1_507
                && scoreboard.getPlayerScoreInfo(player, level).value() == player.experienceLevel,
                "scoreboard after grant: xp=" + info(scoreboard, player, xp) + " level=" + info(scoreboard, player, level)
                        + " total=" + player.totalExperience);
        int beforePay = player.totalExperience;
        RawXp.pay(player, 112);
        ctx.assertTrue(consistent(player, beforePay - 112), "exact deduction");
        player.doTick();
        ctx.assertTrue(scoreboard.getPlayerScoreInfo(player, xp).value() == player.totalExperience
                && player.totalExperience == RawXp.available(player)
                && scoreboard.getPlayerScoreInfo(player, level).value() == player.experienceLevel,
                "scoreboard after exact deduction");
        scoreboard.removeObjective(xp);
        scoreboard.removeObjective(level);
        ctx.succeed();
    }

    private static String info(net.minecraft.world.scores.Scoreboard board, ServerPlayer player, net.minecraft.world.scores.Objective objective) {
        var info = board.getPlayerScoreInfo(player, objective);
        return info == null ? "none" : Integer.toString(info.value());
    }

    static CompoundTag save(GameTestHelper ctx, ServerPlayer player) {
        TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, ctx.getLevel().registryAccess());
        player.saveWithoutId(out);
        return out.buildResult();
    }

    static void load(GameTestHelper ctx, ServerPlayer player, CompoundTag tag) {
        player.load(TagValueInput.create(ProblemReporter.DISCARDING, ctx.getLevel().registryAccess(), tag));
    }

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
