package dev.itzgamesgd.moreminecrafts.bounty;

import dev.itzgamesgd.moreminecrafts.registry.ModDataComponents;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;

/**
 * Owner decisions D1 and D3 (2026-10-25). D1: contract offers follow vanilla restocking. D3: the approved price bands are
 * base prices; vanilla Hero of the Village and, where present, the Villager Trading research discount apply on top, with
 * no contract-only floor beyond vanilla's own minimum of one.
 */
public final class ContractPricingGameTests implements CustomTestMethodInvoker {
    private static final List<ContractData.Tier> TIERS = List.of(ContractData.Tier.BEGINNER,
            ContractData.Tier.INTERMEDIATE, ContractData.Tier.ADVANCED);

    private static Villager expertCleric(GameTestHelper ctx) {
        Villager villager = EntityTypes.VILLAGER.create(ctx.getLevel(), EntitySpawnReason.COMMAND);
        var cleric = ctx.getLevel().registryAccess().lookupOrThrow(Registries.VILLAGER_PROFESSION).getOrThrow(
                ResourceKey.create(Registries.VILLAGER_PROFESSION, Identifier.withDefaultNamespace("cleric")));
        villager.setVillagerData(villager.getVillagerData().withProfession(cleric).withLevel(4));
        return villager;
    }

    private static MerchantOffer contractOffer(Villager villager, ContractData.Tier tier) {
        for (MerchantOffer offer : villager.getOffers())
            if (offer.getResult().get(ModDataComponents.PENDING_CONTRACT) == tier) return offer;
        throw new IllegalStateException("no " + tier + " contract offer");
    }

    /** Villager's real private price rebuild, run when a player opens trading. */
    private static void updatePrices(Villager villager, ServerPlayer player) throws ReflectiveOperationException {
        Method update = Villager.class.getDeclaredMethod("updateSpecialPrices", net.minecraft.world.entity.player.Player.class);
        update.setAccessible(true);
        update.invoke(villager, player);
    }

    private static ServerPlayer player(GameTestHelper ctx) {
        var profile = new com.mojang.authlib.GameProfile(UUID.randomUUID(), "pricing-" + UUID.randomUUID().toString().substring(0, 6));
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(profile, false);
        var player = new ServerPlayer(ctx.getLevel().getServer(), ctx.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        ctx.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        return player;
    }

    /** Vanilla Hero of the Village reduction: at least one emerald, independent of the offer's price multiplier. */
    private static int heroPrice(int base, int amplifier) {
        int discount = Math.max((int) Math.floor((0.3 + 0.0625 * amplifier) * base), 1);
        return Math.clamp(base - discount, 1, 64);
    }

    @GameTest
    public void approvedBandsAreBasePricesAndHeroOfTheVillageAppliesToContracts(GameTestHelper ctx) throws Exception {
        Villager villager = expertCleric(ctx);
        ServerPlayer plain = player(ctx);
        updatePrices(villager, plain);
        for (ContractData.Tier tier : TIERS) {
            int base = ContractTrades.price(tier);
            int[] band = ContractEconomy.band(tier);
            ctx.assertTrue(base >= band[0] && base <= band[1], tier + " base price inside the approved band");
            ctx.assertTrue(contractOffer(villager, tier).getCostA().getCount() == base, tier + " undiscounted price is the base");
        }
        for (int amplifier : new int[]{0, 1, 2, 3, 4, 20, 255}) {
            ServerPlayer hero = player(ctx);
            hero.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 1200, amplifier));
            updatePrices(villager, hero);
            for (ContractData.Tier tier : TIERS) {
                int base = ContractTrades.price(tier);
                int paid = contractOffer(villager, tier).getCostA().getCount();
                ctx.assertTrue(paid == heroPrice(base, amplifier), tier + " Hero " + (amplifier + 1) + " price " + paid
                        + " expected " + heroPrice(base, amplifier));
                ctx.assertTrue(paid >= 1 && paid < base, tier + " Hero discount applies and stays a valid price");
                ItemStack emeralds = new ItemStack(Items.EMERALD, paid);
                ctx.assertTrue(contractOffer(villager, tier).satisfiedBy(emeralds, ItemStack.EMPTY),
                        tier + " the discounted price buys the contract");
            }
        }
        updatePrices(villager, plain);
        for (ContractData.Tier tier : TIERS)
            ctx.assertTrue(contractOffer(villager, tier).getCostA().getCount() == ContractTrades.price(tier),
                    tier + " discounts never persist into another player's price");
        ctx.succeed();
    }

    /**
     * The Villager Trading research discount is not on this baseline; it arrives with the #176 integration. Run there,
     * this test proves it stacks with Hero of the Village on contract offers and never yields a price below one.
     */
    @GameTest
    public void researchDiscountStacksWithHeroOnContractsWhenPresent(GameTestHelper ctx) throws Exception {
        Class<?> discount;
        try {
            discount = Class.forName("dev.itzgamesgd.moreminecrafts.progression.ResearchVillagerDiscount");
        } catch (ClassNotFoundException absent) {
            System.out.println("CONTRACT_RESEARCH_DISCOUNT_STACK skipped=research-discount-not-on-this-baseline");
            ctx.succeed();
            return;
        }
        String researchId = (String) discount.getField("ID").get(null);
        Method discountedCost = discount.getMethod("discountedCost", int.class, int.class);
        Class<?> stateType = Class.forName("dev.itzgamesgd.moreminecrafts.progression.ResearchProgressState");
        Class<?> runtime = Class.forName("dev.itzgamesgd.moreminecrafts.progression.KnowledgeRuntime");
        Villager villager = expertCleric(ctx);
        for (int level = 1; level <= 3; level++) {
            for (int amplifier : new int[]{-1, 0, 4, 255}) {
                ServerPlayer player = player(ctx);
                Object state = stateType.getMethod("with", String.class, int.class)
                        .invoke(stateType.getMethod("empty").invoke(null), "research." + researchId, level);
                runtime.getMethod("updateState", ServerPlayer.class, stateType).invoke(null, player, state);
                if (amplifier >= 0) player.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 1200, amplifier));
                updatePrices(villager, player);
                for (ContractData.Tier tier : TIERS) {
                    int base = ContractTrades.price(tier);
                    int vanilla = amplifier >= 0 ? heroPrice(base, amplifier) : base;
                    int expected = (int) discountedCost.invoke(null, vanilla, level);
                    int paid = contractOffer(villager, tier).getCostA().getCount();
                    ctx.assertTrue(paid == expected, tier + " research L" + level + " Hero " + amplifier + " price " + paid
                            + " expected " + expected);
                    ctx.assertTrue(paid >= 1 && paid <= vanilla, tier + " stacked discount stays a valid price: " + paid);
                }
            }
        }
        System.out.println("CONTRACT_RESEARCH_DISCOUNT_STACK executed=true");
        ctx.succeed();
    }

    @GameTest
    public void villagerContractStockRestocksLikeVanillaAndSellsFreshContracts(GameTestHelper ctx) {
        Villager villager = expertCleric(ctx);
        ServerPlayer buyer = ctx.makeMockServerPlayerInLevel();
        Set<String> ids = new HashSet<>();
        for (int round = 0; round < 3; round++) {
            List<MerchantOffer> contracts = new ArrayList<>();
            for (MerchantOffer offer : villager.getOffers())
                if (offer.getResult().get(ModDataComponents.PENDING_CONTRACT) != null) contracts.add(offer);
            ctx.assertTrue(contracts.size() == 4, "Expert stock is Beginner 2, Intermediate 1, Advanced 1: " + contracts.size());
            for (MerchantOffer offer : contracts) {
                ctx.assertTrue(!offer.isOutOfStock() && offer.getMaxUses() == 1, "one unit per offer, in stock at round " + round);
                buyer.getInventory().clearContent();
                buyer.getInventory().setItem(0, offer.assemble());
                offer.increaseUses();
                ContractMaterializer.materialize(buyer);
                ContractData bought = ContractStacks.read(buyer.getInventory().getItem(0));
                ctx.assertTrue(bought != null && ids.add(bought.id()), "every purchase is a newly rolled contract");
                ctx.assertTrue(offer.isOutOfStock(), "sold out after one purchase");
            }
            ctx.assertTrue(villager.getOffers().stream().filter(o -> o.getResult().get(ModDataComponents.PENDING_CONTRACT) != null)
                    .allMatch(MerchantOffer::isOutOfStock), "getOffers never refills sold contracts by itself");
            villager.restock();
        }
        ctx.assertTrue(ids.size() == 12, "three vanilla restocks sold twelve distinct contracts");
        ctx.succeed();
    }

    @GameTest
    public void wanderingTraderContractStockNeverRefills(GameTestHelper ctx) {
        WanderingTrader trader = EntityTypes.WANDERING_TRADER.create(ctx.getLevel(), EntitySpawnReason.COMMAND);
        int contracts = 0;
        for (MerchantOffer offer : trader.getOffers())
            if (offer.getResult().get(ModDataComponents.PENDING_CONTRACT) != null) {
                contracts++;
                offer.increaseUses();
            }
        ctx.assertTrue(contracts >= 1 && contracts <= 4, "approved wandering stock: " + contracts);
        int size = trader.getOffers().size();
        for (int call = 0; call < 5; call++) trader.getOffers();
        ctx.assertTrue(trader.getOffers().size() == size, "no contract offers are added after the first roll");
        ctx.assertTrue(trader.getOffers().stream().filter(o -> o.getResult().get(ModDataComponents.PENDING_CONTRACT) != null)
                .allMatch(MerchantOffer::isOutOfStock), "sold wandering contracts stay sold, as vanilla never restocks traders");
        ctx.succeed();
    }

    @Override public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
