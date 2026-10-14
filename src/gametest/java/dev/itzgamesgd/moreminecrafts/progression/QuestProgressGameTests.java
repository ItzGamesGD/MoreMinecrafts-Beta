package dev.itzgamesgd.moreminecrafts.progression;

import java.lang.reflect.Method;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;

public final class QuestProgressGameTests implements CustomTestMethodInvoker {
    private static final String TEST_QUEST_ID = "gametest.one_time_progression_reward";

    @GameTest
    public void questProgressAndRewardArePrivateAndClaimableOnce(GameTestHelper ctx) {
        QuestCatalog.register(new QuestDefinition(TEST_QUEST_ID, "gametest",
                "test.quest.title", "test.quest.description", "test.quest.objective", 3, 25));
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        int before = RawXp.available(player);

        QuestProgressService.recordProgress(player, TEST_QUEST_ID, 2);
        ctx.assertTrue(KnowledgeRuntime.state(player).questProgress(TEST_QUEST_ID) == 2
                && !KnowledgeRuntime.state(player).questComplete(TEST_QUEST_ID),
                "partial progress is stored but not complete");
        QuestProgressService.recordProgress(player, TEST_QUEST_ID, 2);
        ctx.assertTrue(KnowledgeRuntime.state(player).questProgress(TEST_QUEST_ID) == 3
                && KnowledgeRuntime.state(player).questComplete(TEST_QUEST_ID),
                "progress clamps at target and completes");

        ctx.assertTrue(QuestProgressService.claim(player, TEST_QUEST_ID)
                == QuestProgressService.ClaimResult.CLAIMED, "completed reward is claimable");
        int afterClaim = RawXp.available(player);
        ctx.assertTrue(afterClaim == before + 25, "claim grants raw XP once");
        ctx.assertTrue(QuestProgressService.claim(player, TEST_QUEST_ID)
                == QuestProgressService.ClaimResult.ALREADY_CLAIMED,
                "duplicate claim is rejected");
        ctx.assertTrue(RawXp.available(player) == afterClaim, "duplicate claim grants no XP");

        ServerPlayer other = ctx.makeMockServerPlayerInLevel();
        ctx.assertTrue(!KnowledgeRuntime.state(other).questComplete(TEST_QUEST_ID)
                && !KnowledgeRuntime.state(other).questClaimed(TEST_QUEST_ID),
                "quest progress is private per player");
        ctx.succeed();
    }

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
