package dev.itzgamesgd.moreminecrafts.progression;

import java.lang.reflect.Method;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;

public final class Research23AXpCollectionGameTests implements CustomTestMethodInvoker {
    @GameTest
    public void xpControlCollectsFullOrbValueOnceForNearestEligiblePlayer(GameTestHelper ctx) {
        ServerPlayer nearer = ctx.makeMockServerPlayerInLevel();
        ServerPlayer farther = ctx.makeMockServerPlayerInLevel();
        nearer.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        farther.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        nearer.setPos(5.0, 2.0, 5.0);
        farther.setPos(8.0, 2.0, 5.0);
        KnowledgeRuntime.updateState(nearer, ResearchProgressState.empty()
                .with("research.v3.research.67", 2));
        KnowledgeRuntime.updateState(farther, ResearchProgressState.empty()
                .with("research.v3.research.67", 1));

        int nearerBefore = RawXp.available(nearer);
        int fartherBefore = RawXp.available(farther);
        ExperienceOrb orb = new ExperienceOrb(ctx.getLevel(), 5.0, 2.0, 7.0, 7);
        ctx.getLevel().addFreshEntity(orb);
        orb.tick();

        ctx.assertTrue(orb.isRemoved(), "orb in the 6-block radius was collected immediately");
        ctx.assertTrue(RawXp.available(nearer) - nearerBefore == 7,
                "vanilla XP pickup awarded the orb's exact value once to the nearest eligible player");
        ctx.assertTrue(RawXp.available(farther) == fartherBefore,
                "the competing player did not also receive the same orb");
        ctx.succeed();
    }

    @GameTest
    public void unpurchasedPlayerLeavesRemoteOrbForVanillaPickup(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.setPos(5.0, 2.0, 5.0);
        int before = RawXp.available(player);
        ExperienceOrb orb = new ExperienceOrb(ctx.getLevel(), 5.0, 2.0, 7.0, 9);
        ctx.getLevel().addFreshEntity(orb);
        orb.tick();
        ctx.assertTrue(!orb.isRemoved(), "unowned research does not remotely collect an orb");
        ctx.assertTrue(RawXp.available(player) == before, "unowned research awards no XP");
        ctx.succeed();
    }

    @GameTest
    public void purchaseRadiusDoesNotInterceptVanillaPickupOutsideSixBlocks(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.setPos(5.0, 2.0, 5.0);
        KnowledgeRuntime.updateState(player, ResearchProgressState.empty()
                .with("research.v3.research.67", 2));
        int before = RawXp.available(player);
        ExperienceOrb orb = new ExperienceOrb(ctx.getLevel(), 5.0, 2.0, 11.01, 5);
        ctx.getLevel().addFreshEntity(orb);
        orb.tick();
        ctx.assertTrue(!orb.isRemoved() && RawXp.available(player) == before,
                "paid remote collection leaves an orb just outside 6 blocks untouched");
        orb.playerTouch(player);
        ctx.assertTrue(orb.isRemoved() && RawXp.available(player) - before == 5,
                "the ordinary vanilla pickup pathway still collects the exact orb value");
        ctx.succeed();
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method testMethod) throws ReflectiveOperationException {
        testMethod.invoke(this, helper);
    }
}
