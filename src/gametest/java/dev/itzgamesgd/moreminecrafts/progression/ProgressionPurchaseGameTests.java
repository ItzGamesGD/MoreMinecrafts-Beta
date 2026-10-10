package dev.itzgamesgd.moreminecrafts.progression;

import java.lang.reflect.Method;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ProgressionPurchaseGameTests implements CustomTestMethodInvoker {
    @GameTest
    public void researchPurchaseConsumesResourcesOnceAndIsPlayerPrivate(GameTestHelper ctx) {
        ServerPlayer buyer = ctx.makeMockServerPlayerInLevel();
        buyer.giveExperiencePoints(1200);
        buyer.getInventory().add(new ItemStack(Items.IRON_BLOCK, 4));
        buyer.getInventory().add(new ItemStack(Items.REDSTONE, 64));
        buyer.getInventory().add(new ItemStack(Items.DIAMOND, 4));
        KnowledgeRuntime.grantKnowledge(buyer, 9);
        int xpBefore = RawXp.available(buyer);

        ctx.assertTrue(ResearchPurchaseService.purchase(buyer, "throwable_pickaxe", 1)
                == ResearchPurchaseService.Result.PURCHASED, "valid purchase succeeds");
        ctx.assertTrue(KnowledgeRuntime.state(buyer).researchLevel("throwable_pickaxe") == 1,
                "purchase records its level");
        ctx.assertTrue(RawXp.available(buyer) == xpBefore - 900, "raw XP is deducted exactly");
        ctx.assertTrue(buyer.getInventory().countItem(Items.IRON_BLOCK) == 0
                && buyer.getInventory().countItem(Items.REDSTONE) == 0
                && buyer.getInventory().countItem(Items.DIAMOND) == 0,
                "only the required materials are consumed");

        int xpAfterFirstPurchase = RawXp.available(buyer);
        ctx.assertTrue(ResearchPurchaseService.purchase(buyer, "throwable_pickaxe", 1)
                == ResearchPurchaseService.Result.ALREADY_PURCHASED, "duplicate purchase is rejected");
        ctx.assertTrue(RawXp.available(buyer) == xpAfterFirstPurchase,
                "duplicate request does not spend more XP");

        ServerPlayer otherPlayer = ctx.makeMockServerPlayerInLevel();
        ctx.assertTrue(!KnowledgeRuntime.state(otherPlayer).hasKnowledge(9)
                && KnowledgeRuntime.state(otherPlayer).researchLevel("throwable_pickaxe") == 0,
                "progression does not leak to another player");

        ServerPlayer poorBuyer = ctx.makeMockServerPlayerInLevel();
        poorBuyer.giveExperiencePoints(1200);
        KnowledgeRuntime.grantKnowledge(poorBuyer, 9);
        int poorXpBefore = RawXp.available(poorBuyer);
        ctx.assertTrue(ResearchPurchaseService.purchase(poorBuyer, "throwable_pickaxe", 1)
                == ResearchPurchaseService.Result.MISSING_MATERIALS, "missing materials reject the purchase");
        ctx.assertTrue(RawXp.available(poorBuyer) == poorXpBefore
                && KnowledgeRuntime.state(poorBuyer).researchLevel("throwable_pickaxe") == 0,
                "failed purchase preserves XP and research state");

        ServerPlayer unqualified = ctx.makeMockServerPlayerInLevel();
        fillThrowablePickaxeMaterials(unqualified);
        unqualified.giveExperiencePoints(1200);
        int unqualifiedXp = RawXp.available(unqualified);
        ctx.assertTrue(ResearchPurchaseService.purchase(unqualified, "throwable_pickaxe", 1)
                == ResearchPurchaseService.Result.MISSING_KNOWLEDGE, "missing knowledge rejects purchase");
        ctx.assertTrue(unqualified.getInventory().countItem(Items.IRON_BLOCK) == 4
                && RawXp.available(unqualified) == unqualifiedXp
                && KnowledgeRuntime.state(unqualified).researchLevel("throwable_pickaxe") == 0,
                "missing knowledge consumes nothing");

        ServerPlayer lowXp = ctx.makeMockServerPlayerInLevel();
        fillThrowablePickaxeMaterials(lowXp);
        KnowledgeRuntime.grantKnowledge(lowXp, 9);
        int lowXpBefore = RawXp.available(lowXp);
        ctx.assertTrue(ResearchPurchaseService.purchase(lowXp, "throwable_pickaxe", 1)
                == ResearchPurchaseService.Result.INSUFFICIENT_XP, "insufficient raw XP rejects purchase");
        ctx.assertTrue(lowXp.getInventory().countItem(Items.IRON_BLOCK) == 4
                && RawXp.available(lowXp) == lowXpBefore
                && KnowledgeRuntime.state(lowXp).researchLevel("throwable_pickaxe") == 0,
                "insufficient XP consumes nothing");

        ctx.assertTrue(ResearchPurchaseService.purchase(buyer, "unknown_research", 1)
                == ResearchPurchaseService.Result.INVALID_ID, "unknown research IDs are rejected");
        ctx.assertTrue(ResearchPurchaseService.purchase(buyer, "throwable_pickaxe", 2)
                == ResearchPurchaseService.Result.INVALID_LEVEL, "levels above the cap are rejected");
        ctx.succeed();
    }

    private static void fillThrowablePickaxeMaterials(ServerPlayer player) {
        player.getInventory().add(new ItemStack(Items.IRON_BLOCK, 4));
        player.getInventory().add(new ItemStack(Items.REDSTONE, 64));
        player.getInventory().add(new ItemStack(Items.DIAMOND, 4));
    }

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
