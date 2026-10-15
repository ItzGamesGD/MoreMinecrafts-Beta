package dev.itzgamesgd.moreminecrafts.progression;

import java.lang.reflect.Method;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

public final class Research23AMovementExhaustionGameTests implements CustomTestMethodInvoker {
    @GameTest
    public void unrelatedFoodExhaustionIsUnchangedAtPurchasedLevelThree(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        KnowledgeRuntime.updateState(player, ResearchProgressState.empty()
                .with("research.v3.research.55", 3));

        float before = player.getFoodData().getExhaustionLevel();
        player.causeFoodExhaustion(0.25F);
        float after = player.getFoodData().getExhaustionLevel();

        ctx.assertTrue(Math.abs((after - before) - 0.25F) < 0.0001F,
                "non-movement exhaustion does not pass through the sprint/swim reduction");
        ctx.succeed();
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method testMethod) throws ReflectiveOperationException {
        testMethod.invoke(this, helper);
    }
}
