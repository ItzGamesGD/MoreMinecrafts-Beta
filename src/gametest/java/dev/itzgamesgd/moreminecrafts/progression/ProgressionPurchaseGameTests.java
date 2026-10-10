package dev.itzgamesgd.moreminecrafts.progression;

import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.GameType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ProgressionPurchaseGameTests implements CustomTestMethodInvoker {
    @GameTest
    public void researchPurchaseConsumesResourcesOnceAndIsPlayerPrivate(GameTestHelper ctx) {
        var buyer = ctx.makeMockServerPlayer(GameType.SURVIVAL);
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

        var otherPlayer = ctx.makeMockServerPlayer(GameType.SURVIVAL);
        ctx.assertTrue(!KnowledgeRuntime.state(otherPlayer).hasKnowledge(9)
                && KnowledgeRuntime.state(otherPlayer).researchLevel("throwable_pickaxe") == 0,
                "progression does not leak to another player");

        var poorBuyer = ctx.makeMockServerPlayer(GameType.SURVIVAL);
        poorBuyer.giveExperiencePoints(1200);
        KnowledgeRuntime.grantKnowledge(poorBuyer, 9);
        int poorXpBefore = RawXp.available(poorBuyer);
        ctx.assertTrue(ResearchPurchaseService.purchase(poorBuyer, "throwable_pickaxe", 1)
                == ResearchPurchaseService.Result.MISSING_MATERIALS, "missing materials reject the purchase");
        ctx.assertTrue(RawXp.available(poorBuyer) == poorXpBefore
                && KnowledgeRuntime.state(poorBuyer).researchLevel("throwable_pickaxe") == 0,
                "failed purchase preserves XP and research state");
        ctx.succeed();
    }
}
