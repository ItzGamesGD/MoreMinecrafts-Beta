package dev.itzgamesgd.moreminecrafts.progression;

import java.lang.reflect.Method;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameType;

public final class Research23AXpCollectionGameTests implements CustomTestMethodInvoker {
    @GameTest
    public void xpControlAndXpAcquisitionAwardOneVanillaPickupToTheNearestPlayer(GameTestHelper ctx) {
        ServerPlayer nearer = player(ctx, 5.0, 2.0, 5.0);
        ServerPlayer farther = player(ctx, 8.0, 2.0, 5.0);
        KnowledgeRuntime.updateState(nearer, ResearchProgressState.empty()
                .with("research.v3.research.67", 2)
                .with("research.v3.research.64", 3));
        KnowledgeRuntime.updateState(farther, ResearchProgressState.empty()
                .with("research.v3.research.67", 1)
                .with("research.v3.research.64", 3)
                .with("research.xp_fraction.v3.research.64", 77));

        int nearerBefore = RawXp.available(nearer);
        int fartherBefore = RawXp.available(farther);
        ExperienceOrb orb = orbAt(ctx, 5.0, 2.0, 7.0, 100);
        ctx.getLevel().addFreshEntity(orb);
        orb.tick();

        ctx.assertTrue(orb.isRemoved(), "one tick sends the eligible orb through vanilla pickup");
        ctx.assertTrue(RawXp.available(nearer) - nearerBefore == 109,
                "nearest player gets 100 base XP plus the level-three nine percent bonus once");
        ctx.assertTrue(RawXp.available(farther) == fartherBefore,
                "the competing eligible player does not also receive the orb or its bonus");
        ctx.assertTrue(KnowledgeRuntime.state(farther).get("research.xp_fraction.v3.research.64") == 77,
                "the nonrecipient's saved XP fraction remains isolated");
        ctx.succeed();
    }

    @GameTest
    public void xpControlPreservesPerPlayerFractionalXpAcrossRepeatedVanillaPickups(GameTestHelper ctx) {
        ServerPlayer researcher = player(ctx, 5.0, 2.0, 5.0);
        KnowledgeRuntime.updateState(researcher, ResearchProgressState.empty()
                .with("research.v3.research.67", 1)
                .with("research.v3.research.64", 3));
        int before = RawXp.available(researcher);
        for (int i = 0; i < 100; i++) {
            // Vanilla playerTouch accepts one orb per two ticks (takeXpDelay); model that cadence elapsing.
            researcher.takeXpDelay = 0;
            ExperienceOrb orb = orbAt(ctx, 5.0, 2.0, 7.0, 1);
            ctx.getLevel().addFreshEntity(orb);
            orb.tick();
            ctx.assertTrue(orb.isRemoved(), "each one-XP orb follows one vanilla pickup path");
        }
        ctx.assertTrue(RawXp.available(researcher) - before == 109,
                "fractional level-three bonus accumulates independently on the receiver");
        ctx.assertTrue(KnowledgeRuntime.state(researcher).get("research.xp_fraction.v3.research.64") == 0,
                "one hundred one-XP orb pickups leave no remainder");
        ctx.succeed();
    }

    @GameTest
    public void unpurchasedPlayerLeavesRemoteOrbForVanillaPickup(GameTestHelper ctx) {
        ServerPlayer player = player(ctx, 5.0, 2.0, 5.0);
        int before = RawXp.available(player);
        ExperienceOrb orb = orbAt(ctx, 5.0, 2.0, 7.0, 9);
        ctx.getLevel().addFreshEntity(orb);
        orb.tick();
        ctx.assertTrue(!orb.isRemoved(), "unowned research does not remotely collect an orb");
        ctx.assertTrue(RawXp.available(player) == before, "unowned research awards no XP");
        ctx.succeed();
    }

    @GameTest
    public void purchaseRadiusDoesNotInterceptVanillaPickupOutsideSixBlocks(GameTestHelper ctx) {
        ServerPlayer player = player(ctx, 5.0, 2.0, 5.0);
        KnowledgeRuntime.updateState(player, ResearchProgressState.empty()
                .with("research.v3.research.67", 2));
        int before = RawXp.available(player);
        ExperienceOrb orb = orbAt(ctx, 5.0, 2.0, 11.01, 5);
        ctx.getLevel().addFreshEntity(orb);
        orb.tick();
        ctx.assertTrue(!orb.isRemoved() && RawXp.available(player) == before,
                "a paid radius leaves an orb just outside six blocks untouched");
        orb.playerTouch(player);
        ctx.assertTrue(orb.isRemoved() && RawXp.available(player) - before == 5,
                "ordinary vanilla pickup still collects its exact XP value");
        ctx.succeed();
    }

    @GameTest
    public void xpControlKeepsMendingInTheVanillaOrbPickupPath(GameTestHelper ctx) {
        ServerPlayer player = player(ctx, 5.0, 2.0, 5.0);
        KnowledgeRuntime.updateState(player, ResearchProgressState.empty()
                .with("research.v3.research.67", 2));
        ItemStack mendingTool = new ItemStack(Items.DIAMOND_PICKAXE);
        mendingTool.setDamageValue(20);
        EnchantmentHelper.updateEnchantments(mendingTool, enchantments -> enchantments.set(
                T1Operations.enchantment(player, "minecraft:mending").orElseThrow(), 1));
        player.getInventory().setItem(0, mendingTool);

        ExperienceOrb orb = orbAt(ctx, 5.0, 2.0, 7.0, 100);
        ctx.getLevel().addFreshEntity(orb);
        orb.tick();

        ctx.assertTrue(orb.isRemoved(), "remote collection invokes vanilla orb pickup");
        ctx.assertTrue(mendingTool.getDamageValue() < 20,
                "vanilla Mending repairs the player's item during the same pickup");
        ctx.assertTrue(RawXp.available(player) > 0,
                "remaining XP is still awarded through the ordinary receiver path");
        ctx.succeed();
    }

    /** Coordinates are structure-relative, so the scene sits in the test's own loaded, ticking area. */
    private static ServerPlayer player(GameTestHelper ctx, double x, double y, double z) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(ctx.absoluteVec(new net.minecraft.world.phys.Vec3(x, y, z)));
        return player;
    }

    private static ExperienceOrb orbAt(GameTestHelper ctx, double x, double y, double z, int value) {
        var position = ctx.absoluteVec(new net.minecraft.world.phys.Vec3(x, y, z));
        return new ExperienceOrb(ctx.getLevel(), position.x, position.y, position.z, value);
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method testMethod) throws ReflectiveOperationException {
        testMethod.invoke(this, helper);
    }
}
