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

        var food = player.getFoodData();
        int beforeFood = food.getFoodLevel();
        food.setSaturation(0.0F);
        player.causeFoodExhaustion(4.25F);
        food.tick(player);

        ctx.assertTrue(food.getFoodLevel() == beforeFood - 1,
                "an unrelated exhaustion call reaches vanilla food processing without reduction");
        ctx.succeed();
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method testMethod) throws ReflectiveOperationException {
        testMethod.invoke(this, helper);
    }
}
