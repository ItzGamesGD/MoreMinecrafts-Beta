package dev.itzgamesgd.moreminecrafts.bounty;

import java.lang.reflect.Method;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

/** Exact seller stock: pure logic first, then real villager and wandering trader entities through the mixin. */
public final class ContractTradeGameTests implements CustomTestMethodInvoker {
    private static final List<String> ELIGIBLE = List.of("farmer", "fisherman", "fletcher", "weaponsmith",
            "toolsmith", "cartographer", "cleric");
    private static final List<String> INELIGIBLE = List.of("armorer", "butcher", "librarian", "mason", "shepherd",
            "leatherworker", "nitwit");
    private static final ContractData.Tier B = ContractData.Tier.BEGINNER;
    private static final ContractData.Tier I = ContractData.Tier.INTERMEDIATE;
    private static final ContractData.Tier A = ContractData.Tier.ADVANCED;

    /** Beginner at Apprentice (2), Intermediate at Journeyman (3), Advanced at Expert (4) and above. */
    private static int[] expected(int level) {
        return new int[]{level >= 2 ? 2 : 0, level >= 3 ? 1 : 0, level >= 4 ? 1 : 0};
    }

    private static void assertStock(GameTestHelper ctx, MerchantOffers offers, int[] want, String label) {
        ctx.assertTrue(ContractTrades.count(offers, B) == want[0], label + " beginner " + ContractTrades.count(offers, B));
        ctx.assertTrue(ContractTrades.count(offers, I) == want[1], label + " intermediate " + ContractTrades.count(offers, I));
        ctx.assertTrue(ContractTrades.count(offers, A) == want[2], label + " advanced " + ContractTrades.count(offers, A));
    }

    private static MerchantOffers vanillaLikeOffers() {
        MerchantOffers offers = new MerchantOffers();
        offers.add(new MerchantOffer(new net.minecraft.world.item.trading.ItemCost(Items.WHEAT, 20),
                new net.minecraft.world.item.ItemStack(Items.EMERALD), 16, 2, 0.05F));
        return offers;
    }

    @GameTest
    public void pureStockMatchesApprovedUnlockTableForEveryProfessionAndLevel(GameTestHelper ctx) {
        for (String profession : ELIGIBLE) {
            for (int level = 1; level <= 5; level++) {
                MerchantOffers offers = vanillaLikeOffers();
                ContractTrades.ensureVillager("minecraft:" + profession, level, offers);
                assertStock(ctx, offers, expected(level), profession + " L" + level);
                ctx.assertTrue(offers.size() == 1 + (expected(level)[0] + expected(level)[1] + expected(level)[2]),
                        profession + " L" + level + " keeps the vanilla offer and adds nothing else");
            }
        }
        for (String profession : INELIGIBLE) {
            for (int level = 1; level <= 5; level++) {
                MerchantOffers offers = vanillaLikeOffers();
                ctx.assertTrue(ContractTrades.ensureVillager("minecraft:" + profession, level, offers) == 0,
                        profession + " L" + level + " receives no contract stock");
            }
        }
        ctx.succeed();
    }

    @GameTest
    public void stockIsIdempotentAndExhaustionDoesNotRegenerate(GameTestHelper ctx) {
        MerchantOffers offers = vanillaLikeOffers();
        ContractTrades.ensureVillager("minecraft:farmer", 4, offers);
        ctx.assertTrue(ContractTrades.ensureVillager("minecraft:farmer", 4, offers) == 0, "second call adds nothing");
        for (MerchantOffer offer : offers)
            if (offer.getResult().get(dev.itzgamesgd.moreminecrafts.registry.ModDataComponents.PENDING_CONTRACT) != null)
                offer.increaseUses();
        assertStock(ctx, offers, new int[]{2, 1, 1}, "after exhaustion");
        ctx.assertTrue(ContractTrades.ensureVillager("minecraft:farmer", 4, offers) == 0, "sold-out stock is not regenerated");
        ctx.succeed();
    }

    @GameTest
    public void priceStaysInsideApprovedBands(GameTestHelper ctx) {
        ctx.assertTrue(ContractTrades.price(B) == 3 && ContractTrades.price(I) == 8 && ContractTrades.price(A) == 16, "midpoint prices");
        ctx.succeed();
    }

    @GameTest
    public void realVillagersGenerateExactStockAndRestockKeepsCounts(GameTestHelper ctx) {
        var professions = ctx.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.VILLAGER_PROFESSION);
        for (String name : ELIGIBLE) {
            Holder<VillagerProfession> profession = professions.getOrThrow(
                    net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.VILLAGER_PROFESSION,
                            Identifier.withDefaultNamespace(name)));
            for (int level = 1; level <= 5; level++) {
                Villager villager = EntityTypes.VILLAGER.create(ctx.getLevel(), EntitySpawnReason.COMMAND);
                villager.setVillagerData(villager.getVillagerData().withProfession(profession).withLevel(level));
                MerchantOffers offers = villager.getOffers();
                assertStock(ctx, offers, expected(level), "real " + name + " L" + level);
                int size = offers.size();
                ctx.assertTrue(villager.getOffers().size() == size, name + " L" + level + " repeated getOffers is stable");
                if (level >= 2) {
                    for (MerchantOffer offer : offers)
                        if (offer.getResult().get(dev.itzgamesgd.moreminecrafts.registry.ModDataComponents.PENDING_CONTRACT) != null)
                            offer.increaseUses();
                    villager.restock();
                    assertStock(ctx, villager.getOffers(), expected(level), "real " + name + " L" + level + " after restock");
                    for (MerchantOffer offer : villager.getOffers())
                        if (offer.getResult().get(dev.itzgamesgd.moreminecrafts.registry.ModDataComponents.PENDING_CONTRACT) != null)
                            ctx.assertTrue(offer.getUses() == 0, name + " L" + level + " vanilla restock replenishes uses");
                }
                boolean vanillaKept = false;
                for (MerchantOffer offer : offers)
                    if (offer.getResult().get(dev.itzgamesgd.moreminecrafts.registry.ModDataComponents.PENDING_CONTRACT) == null)
                        vanillaKept = true;
                ctx.assertTrue(vanillaKept, name + " L" + level + " keeps its vanilla offers");
            }
        }
        ctx.succeed();
    }

    @GameTest
    public void realIneligibleProfessionHasNoContracts(GameTestHelper ctx) {
        var professions = ctx.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.VILLAGER_PROFESSION);
        for (String name : List.of("armorer", "librarian", "mason")) {
            Holder<VillagerProfession> profession = professions.getOrThrow(
                    net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.VILLAGER_PROFESSION,
                            Identifier.withDefaultNamespace(name)));
            Villager villager = EntityTypes.VILLAGER.create(ctx.getLevel(), EntitySpawnReason.COMMAND);
            villager.setVillagerData(villager.getVillagerData().withProfession(profession).withLevel(5));
            assertStock(ctx, villager.getOffers(), new int[]{0, 0, 0}, "real " + name);
        }
        ctx.succeed();
    }

    @GameTest
    public void realWanderingTradersStayInsideApprovedRangesAndCoverThem(GameTestHelper ctx) {
        boolean[] seenBeginner = new boolean[3];
        boolean[] seenIntermediate = new boolean[2];
        boolean[] seenAdvanced = new boolean[2];
        for (int sample = 0; sample < 400; sample++) {
            WanderingTrader trader = EntityTypes.WANDERING_TRADER.create(ctx.getLevel(), EntitySpawnReason.COMMAND);
            MerchantOffers offers = trader.getOffers();
            int b = ContractTrades.count(offers, B);
            int i = ContractTrades.count(offers, I);
            int a = ContractTrades.count(offers, A);
            ctx.assertTrue(b >= 1 && b <= 2, "beginner stock " + b);
            ctx.assertTrue(i >= 0 && i <= 1, "intermediate stock " + i);
            ctx.assertTrue(a >= 0 && a <= 1, "advanced stock " + a);
            seenBeginner[b] = true;
            seenIntermediate[i] = true;
            seenAdvanced[a] = true;
            int size = offers.size();
            ctx.assertTrue(trader.getOffers().size() == size, "wandering trader stock is rolled once");
            long vanilla = offers.stream().filter(o -> o.getResult().get(dev.itzgamesgd.moreminecrafts.registry.ModDataComponents.PENDING_CONTRACT) == null).count();
            ctx.assertTrue(vanilla > 0, "wandering trader keeps vanilla offers");
        }
        ctx.assertTrue(seenBeginner[1] && seenBeginner[2], "both beginner quantities occur");
        ctx.assertTrue(seenIntermediate[0] && seenIntermediate[1], "both intermediate quantities occur");
        ctx.assertTrue(seenAdvanced[0] && seenAdvanced[1], "both advanced quantities occur");
        ctx.succeed();
    }

    @GameTest
    public void purchasedPlaceholderBecomesFixedContractAfterPickup(GameTestHelper ctx) {
        var player = ctx.makeMockServerPlayerInLevel();
        MerchantOffer offer = ContractTrades.offer(I);
        ctx.assertTrue(dev.itzgamesgd.moreminecrafts.bounty.ContractStacks.read(offer.getResult()) == null,
                "seller listing exposes no objectives");
        player.getInventory().setItem(0, offer.getResult().copy());
        ContractMaterializer.materialize(player);
        ContractData data = ContractStacks.read(player.getInventory().getItem(0));
        ctx.assertTrue(data != null && data.tier() == I && data.status() == ContractData.Status.OFFERED,
                "purchase yields an unaccepted intermediate contract");
        String id = data.id();
        ContractMaterializer.materialize(player);
        ctx.assertTrue(ContractStacks.read(player.getInventory().getItem(0)).id().equals(id), "contract is fixed, not re-rolled");
        ctx.assertTrue(ContractService.act(player, 0, id, ContractService.Action.ACCEPT), "acceptance after purchase");
        ctx.succeed();
    }

    @Override public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
