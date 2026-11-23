package dev.itzgamesgd.moreminecrafts.trade;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.progression.KnowledgeRuntime;
import dev.itzgamesgd.moreminecrafts.progression.ResearchProgressState;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.GameType;

/**
 * Server-side checks of the structured Piglin trade. The offer used here is a TEST-ONLY PLACEHOLDER
 * (it is not shipped and carries no design meaning); the shipped catalog is empty.
 */
public final class PiglinTradeGameTests {
    private static final String UNLOCK = "test_piglin_unlock";

    private static void clearSharedStock(GameTestHelper ctx) {
        PiglinTradeStock.get(ctx.getLevel().getServer().overworld()).clearForTest();
    }

    private static void installCatalog(GameTestHelper ctx, boolean withUnlockId) {
        clearSharedStock(ctx);
        MerchantOffer offer = new MerchantOffer(new ItemCost(Items.DIRT, 2), new ItemStack(Items.STICK), 2, 0, 0.0f);
        PiglinTradeCatalog.setForTest(new PiglinTradeCatalog.Loaded(
                new PiglinTradeConfig(withUnlockId ? Optional.of(UNLOCK) : Optional.empty()),
                Map.of(ModIds.id("test_placeholder"), new PiglinTradeEntry(Optional.empty(), offer))));
    }

    private static ServerPlayer player(GameTestHelper ctx, boolean unlocked) {
        ServerPlayer p = ctx.makeMockServerPlayerInLevel();
        p.setGameMode(GameType.SURVIVAL);
        if (unlocked) p.setAttached(KnowledgeRuntime.STATE, ResearchProgressState.empty().with("research." + UNLOCK, 1));
        return p;
    }

    private static int count(ServerPlayer p, net.minecraft.world.item.Item item) {
        int n = 0;
        for (ItemStack s : p.getInventory().getNonEquipmentItems()) if (s.is(item)) n += s.getCount();
        return n;
    }

    @GameTest
    public void closedWithoutConfiguredUnlockOrWithoutResearch(GameTestHelper ctx) {
        Piglin piglin = ctx.spawnMob(EntityTypes.PIGLIN, 2, 2, 2).spawn();
        installCatalog(ctx, false);
        ctx.assertTrue(!PiglinTrading.tryOpen(player(ctx, true), piglin), "no unlock id configured must stay closed");
        installCatalog(ctx, true);
        ctx.assertTrue(!PiglinTrading.tryOpen(player(ctx, false), piglin), "player without research must be refused");
        PiglinTradeCatalog.setForTest(PiglinTradeCatalog.Loaded.EMPTY);
        ctx.assertTrue(!PiglinTrading.tryOpen(player(ctx, true), piglin), "empty catalog must not open");
        ctx.succeed();
    }

    @GameTest
    public void selectionPreviewConsumesNothingAndResultTakeConsumesOnce(GameTestHelper ctx) {
        Piglin piglin = ctx.spawnMob(EntityTypes.PIGLIN, 2, 2, 2).spawn();
        installCatalog(ctx, true);
        ServerPlayer p = player(ctx, true);
        p.getInventory().add(new ItemStack(Items.DIRT, 5));
        ctx.assertTrue(PiglinTrading.tryOpen(p, piglin), "unlocked player must open the trade");
        ctx.assertTrue(p.containerMenu instanceof MerchantMenu, "must be the vanilla MerchantMenu");
        MerchantMenu menu = (MerchantMenu) p.containerMenu;

        menu.setSelectionHint(0);
        menu.tryMoveItems(0); // auto-populate payment from inventory: moves items, does not pay
        menu.updateSellItem();
        ctx.assertTrue(menu.getSlot(2).getItem().is(Items.STICK), "preview must show the result");
        ctx.assertTrue(PiglinTradeStock.get(ctx.getLevel().getServer().overworld()).usesOf(
                "moreminecrafts:test_placeholder") == 0, "preview must not use stock");
        ctx.assertTrue(count(p, Items.DIRT) + menu.getSlot(0).getItem().getCount() == 5, "no payment lost on preview");

        menu.clicked(2, 0, ContainerInput.PICKUP, p); // take result = the transaction
        ctx.assertTrue(menu.getCarried().is(Items.STICK) && menu.getCarried().getCount() == 1, "exactly one result awarded to cursor");
        ctx.assertTrue(count(p, Items.DIRT) + menu.getSlot(0).getItem().getCount() == 3, "exactly 2 dirt consumed once");
        ctx.assertTrue(PiglinTradeStock.get(ctx.getLevel().getServer().overworld()).usesOf(
                "moreminecrafts:test_placeholder") == 1, "stock use recorded once");
        ctx.succeed();
    }

    @GameTest
    public void insufficientPaymentAndSoldOutAreRejected(GameTestHelper ctx) {
        Piglin piglin = ctx.spawnMob(EntityTypes.PIGLIN, 2, 2, 2).spawn();
        installCatalog(ctx, true);
        ServerPlayer poor = player(ctx, true);
        poor.getInventory().add(new ItemStack(Items.DIRT, 1));
        ctx.assertTrue(PiglinTrading.tryOpen(poor, piglin), "open");
        MerchantMenu m1 = (MerchantMenu) poor.containerMenu;
        m1.setSelectionHint(0);
        m1.tryMoveItems(0);
        m1.clicked(2, 0, ContainerInput.PICKUP, poor);
        ctx.assertTrue(count(poor, Items.STICK) == 0, "insufficient input must not yield a result");
        poor.closeContainer();

        ServerPlayer rich = player(ctx, true);
        rich.getInventory().add(new ItemStack(Items.DIRT, 16));
        ctx.assertTrue(PiglinTrading.tryOpen(rich, piglin), "reopen");
        MerchantMenu m2 = (MerchantMenu) rich.containerMenu;
        m2.setSelectionHint(0);
        for (int i = 0; i < 4; i++) {
            m2.tryMoveItems(0);
            m2.clicked(2, 0, ContainerInput.QUICK_MOVE, rich); // shift-click repeat trade
        }
        ctx.assertTrue(count(rich, Items.STICK) == 2, "max_uses=2 caps repeated/shift trades, got " + count(rich, Items.STICK));
        ctx.succeed();
    }

    @GameTest
    public void secondPlayerCannotOpenWhileFirstIsTrading(GameTestHelper ctx) {
        Piglin piglin = ctx.spawnMob(EntityTypes.PIGLIN, 2, 2, 2).spawn();
        installCatalog(ctx, true);
        ServerPlayer a = player(ctx, true);
        ServerPlayer b = player(ctx, true);
        ctx.assertTrue(PiglinTrading.tryOpen(a, piglin), "first opens");
        ctx.assertTrue(!PiglinTrading.tryOpen(b, piglin), "second refused while first is trading");
        ctx.assertTrue(PiglinTradeStock.get(ctx.getLevel().getServer().overworld()) != null, "shared stock exists");
        ctx.succeed();
    }

    @GameTest
    public void unlockedGoldIngotRightClickOpensTradeInsteadOfBarter(GameTestHelper ctx) {
        Piglin piglin = ctx.spawnMob(EntityTypes.PIGLIN, 2, 2, 2).spawn();
        installCatalog(ctx, true);
        ServerPlayer p = player(ctx, true);
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.GOLD_INGOT, 3));
        ctx.assertTrue(PiglinTrading.handleUse(p, piglin) == net.minecraft.world.InteractionResult.SUCCESS_SERVER, "click consumed");
        ctx.assertTrue(p.containerMenu instanceof MerchantMenu, "trade opens even with gold ingot in hand");
        ctx.assertTrue(p.getMainHandItem().getCount() == 3, "gold ingot not consumed by the click");
        ctx.assertTrue(piglin.getOffhandItem().isEmpty(), "no vanilla barter started (offhand empty)");
        ctx.succeed();
    }

    @GameTest
    public void lockedPlayerKeepsVanillaBarterAndUnlockedMidBarterIsSwallowed(GameTestHelper ctx) {
        Piglin piglin = ctx.spawnMob(EntityTypes.PIGLIN, 2, 2, 2).spawn();
        installCatalog(ctx, true);
        ServerPlayer locked = player(ctx, false);
        locked.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.GOLD_INGOT));
        ctx.assertTrue(PiglinTrading.handleUse(locked, piglin) == net.minecraft.world.InteractionResult.PASS,
                "locked player falls through to vanilla");
        ServerPlayer p = player(ctx, true);
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.GOLD_INGOT));
        piglin.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, new ItemStack(Items.GOLD_INGOT));
        ctx.assertTrue(PiglinTrading.handleUse(p, piglin) == net.minecraft.world.InteractionResult.SUCCESS_SERVER, "swallowed");
        ctx.assertTrue(!(p.containerMenu instanceof MerchantMenu), "no trade during an ongoing admire sequence");
        ctx.succeed();
    }

    @GameTest
    public void shippedOffersLoadFromDatapackAndTierTwoIsResearchGated(GameTestHelper ctx) {
        PiglinTradeCatalog.setForTest(null);
        ctx.assertTrue(PiglinTradeCatalog.current(ctx.getLevel().registryAccess()).offers().size() == 23,
                "the real reload-listener path must yield all 23 offers");
        PiglinTradeCatalog.Loaded loaded = PiglinTradeCatalog.parse(
                PiglinTradeCatalog.Listener.load(ctx.getLevel().getServer().getResourceManager()), ctx.getLevel().registryAccess());
        ctx.assertTrue(loaded.config().requiredResearch().equals(Optional.of("piglin_trading")), "config unlock id");
        ctx.assertTrue(loaded.offers().size() == 23, "all 23 approved offers must parse, got " + loaded.offers().size());
        PiglinTradeCatalog.setForTest(loaded);
        Piglin piglin = ctx.spawnMob(EntityTypes.PIGLIN, 2, 2, 2).spawn();

        ServerPlayer tier1 = ctx.makeMockServerPlayerInLevel();
        tier1.setAttached(KnowledgeRuntime.STATE, ResearchProgressState.empty().with("research.piglin_trading", 1));
        ctx.assertTrue(PiglinTrading.tryOpen(tier1, piglin), "tier-1 player opens");
        ctx.assertTrue(((MerchantMenu) tier1.containerMenu).getOffers().size() == 10, "tier-1 sees 10 offers");
        tier1.closeContainer();

        ServerPlayer tier2 = ctx.makeMockServerPlayerInLevel();
        tier2.setAttached(KnowledgeRuntime.STATE, ResearchProgressState.empty()
                .with("research.piglin_trading", 1).with("research.piglin_trading_2", 1));
        ctx.assertTrue(PiglinTrading.tryOpen(tier2, piglin), "tier-2 player opens");
        var offers = ((MerchantMenu) tier2.containerMenu).getOffers();
        ctx.assertTrue(offers.size() == 23, "tier-2 sees 23 offers, got " + offers.size());
        boolean adamantium = offers.stream().anyMatch(o -> o.getResult().is(
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(ModIds.id("adamantium_ingot")))
                && o.getMaxUses() == 1 && o.getCostA().is(Items.GOLD_BLOCK) && o.getCostA().getCount() == 64
                && o.getCostB().is(Items.NETHERITE_INGOT) && o.getCostB().getCount() == 2);
        ctx.assertTrue(adamantium, "adamantium offer: 64 gold blocks + 2 netherite ingots, stock 1");
        PiglinTradeCatalog.setForTest(null);
        ctx.succeed();
    }

    @GameTest
    public void shippedTwoSlotOffersTradeOnceAndPremiumStockIsOne(GameTestHelper ctx) {
        PiglinTradeCatalog.setForTest(null);
        Piglin piglin = ctx.spawnMob(EntityTypes.PIGLIN, 2, 2, 2).spawn();
        ServerPlayer p = ctx.makeMockServerPlayerInLevel();
        p.setGameMode(GameType.SURVIVAL);
        p.setAttached(KnowledgeRuntime.STATE, ResearchProgressState.empty()
                .with("research.piglin_trading", 1).with("research.piglin_trading_2", 1));
        p.getInventory().add(new ItemStack(Items.GOLD_BLOCK, 64));
        p.getInventory().add(new ItemStack(Items.GOLD_BLOCK, 64));
        p.getInventory().add(new ItemStack(Items.NETHERITE_INGOT, 2));
        ctx.assertTrue(PiglinTrading.tryOpen(p, piglin), "open");
        MerchantMenu menu = (MerchantMenu) p.containerMenu;
        var adam = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(ModIds.id("adamantium_ingot"));
        int index = -1;
        for (int i = 0; i < menu.getOffers().size(); i++) if (menu.getOffers().get(i).getResult().is(adam)) index = i;
        ctx.assertTrue(index >= 0, "adamantium offer listed");
        menu.setSelectionHint(index);
        menu.tryMoveItems(index);
        menu.clicked(2, 0, ContainerInput.PICKUP, p);
        ctx.assertTrue(menu.getCarried().is(adam) && menu.getCarried().getCount() == 1, "one adamantium ingot received");
        int gold = count(p, Items.GOLD_BLOCK) + menu.getSlot(0).getItem().getCount() + menu.getSlot(1).getItem().getCount();
        ctx.assertTrue(gold == 128 - 64, "exactly 64 gold blocks consumed, remaining " + gold);
        menu.setCarried(ItemStack.EMPTY);
        menu.tryMoveItems(index);
        menu.updateSellItem();
        ctx.assertTrue(menu.getSlot(2).getItem().isEmpty(), "stock 1 is exhausted: no second adamantium ingot");
        PiglinTradeCatalog.setForTest(null);
        ctx.succeed();
    }
}
