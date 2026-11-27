package dev.itzgamesgd.moreminecrafts.progression;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

/** Villager Trading (`v3.research.53`) discount: per-player, non-stacking, minimum cost one. */
public final class VillagerTradingGameTests implements CustomTestMethodInvoker {
    private final List<ServerPlayer> players = new ArrayList<>();

    @GameTest
    public void discountIsPlayerSpecificNonStackingAndNeverBelowOne(GameTestHelper ctx) throws Exception {
        ServerPlayer plain = player(ctx, 0);
        ServerPlayer level1 = player(ctx, 1);
        ServerPlayer level2 = player(ctx, 2);
        ServerPlayer level3 = player(ctx, 3);
        Villager villager = EntityTypes.VILLAGER.create(ctx.getLevel(), EntitySpawnReason.COMMAND);
        ctx.assertTrue(villager != null, "villager is constructible");
        villager.getOffers().clear();
        villager.getOffers().add(offer(20, Items.IRON_SWORD));
        villager.getOffers().add(offer(1, Items.BREAD));
        villager.getOffers().add(offer(64, Items.DIAMOND));

        ctx.assertTrue(prices(villager, plain).equals(List.of(20, 1, 64)), "unpurchased players pay vanilla prices");
        ctx.assertTrue(prices(villager, level1).equals(List.of(19, 1, 60)), "L1 is 5% off, minimum one");
        ctx.assertTrue(prices(villager, level2).equals(List.of(18, 1, 57)), "L2 is 10% off");
        ctx.assertTrue(prices(villager, level3).equals(List.of(17, 1, 54)), "L3 is 15% off");
        ctx.assertTrue(prices(villager, level3).equals(List.of(17, 1, 54)), "repeating the update never stacks");
        ctx.assertTrue(prices(villager, plain).equals(List.of(20, 1, 64)),
                "a different player on the same villager sees only their own entitlement");

        // Vanilla Hero of the Village applies first; the research discount is taken from that price.
        level2.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 1200, 0));
        List<Integer> hero = prices(villager, level2);
        plain.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 1200, 0));
        List<Integer> heroOnly = prices(villager, plain);
        ctx.assertTrue(heroOnly.get(0) < 20, "vanilla Hero of the Village still reduces prices");
        ctx.assertTrue(hero.get(0) == ResearchVillagerDiscount.discountedCost(heroOnly.get(0), 2)
                        && hero.get(1) == 1,
                "research discount is applied once after vanilla modifiers: " + hero + " vs " + heroOnly);
        ctx.succeed();
    }

    @GameTest
    public void discountReachesCappedDemandPrice(GameTestHelper ctx) throws Exception {
        ServerPlayer level2 = player(ctx, 2);
        Villager villager = EntityTypes.VILLAGER.create(ctx.getLevel(), EntitySpawnReason.COMMAND);
        ctx.assertTrue(villager != null, "villager is constructible");
        MerchantOffer inflated = offer(64, Items.DIAMOND);
        inflated.setToOutOfStock();
        inflated.updateDemand();
        ctx.assertTrue(inflated.getDemand() > 0, "vanilla demand exceeds the 64-item price cap");
        villager.getOffers().add(inflated);
        ctx.assertTrue(prices(villager, level2).equals(List.of(57)),
                "L2 takes ceil(10% of 64) from the capped vanilla price");
        ctx.assertTrue(prices(villager, level2).equals(List.of(57)), "the next rebuild does not stack");
        ctx.succeed();
    }

    @GameTest
    public void retainedLibrarianBookIsFixedBeforeOffersAreSent(GameTestHelper ctx) throws Exception {
        RecordingPlayer level1 = (RecordingPlayer) player(ctx, 1);
        level1.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 1200, 0));
        Villager villager = EntityTypes.VILLAGER.create(ctx.getLevel(), EntitySpawnReason.COMMAND);
        ctx.assertTrue(villager != null, "villager is constructible");
        ItemStack retainedBook = new ItemStack(Items.ENCHANTED_BOOK);
        EnchantmentHelper.updateEnchantments(retainedBook, enchants -> enchants.set(
                T1Operations.enchantment(level1, "minecraft:sharpness").orElseThrow(), 1));
        MerchantOffer fixed = new MerchantOffer(new ItemCost(Items.EMERALD, 64),
                Optional.of(new ItemCost(Items.BOOK)), retainedBook, 1, 1, 0.2F);
        ctx.assertTrue(T1Supply.isFixedBookOffer(fixed), "fixture is an approved fixed-price offer");
        villager.getOffers().add(fixed);
        villager.getOffers().add(offer(10, Items.BREAD));
        villager.setTradingPlayer(level1);
        List<Integer> rebuilt = prices(villager, level1);
        ctx.assertTrue(rebuilt.get(0) == 64 && level1.sentCosts.get(0) == 64,
                "retained book stays at 64 both on the server and in the sent offers");
        ctx.assertTrue(level1.sentCosts.get(1) < 10, "ordinary offers still receive discounts");
        ctx.succeed();
    }

    private static MerchantOffer offer(int emeralds, net.minecraft.world.item.Item result) {
        return new MerchantOffer(new ItemCost(Items.EMERALD, emeralds), new ItemStack(result), 12, 1, 0.0F);
    }

    /** Invokes Villager's real private updateSpecialPrices, the hook the discount rides on. */
    private static List<Integer> prices(Villager villager, ServerPlayer player) throws Exception {
        Method update = Villager.class.getDeclaredMethod("updateSpecialPrices",
                net.minecraft.world.entity.player.Player.class);
        update.setAccessible(true);
        update.invoke(villager, player);
        List<Integer> costs = new ArrayList<>();
        for (MerchantOffer offer : villager.getOffers()) costs.add(offer.getCostA().getCount());
        return costs;
    }

    private ServerPlayer player(GameTestHelper ctx, int level) {
        var profile = new com.mojang.authlib.GameProfile(UUID.randomUUID(), "trader-" + players.size());
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(profile, false);
        var player = new RecordingPlayer(ctx.getLevel().getServer(), ctx.getLevel(), cookie.gameProfile(),
                cookie.clientInformation());
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        ctx.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
        if (level > 0) KnowledgeRuntime.updateState(player, ResearchProgressState.empty()
                .with("research." + ResearchVillagerDiscount.ID, level));
        players.add(player);
        return player;
    }

    private static final class RecordingPlayer extends ServerPlayer {
        private List<Integer> sentCosts = List.of();

        private RecordingPlayer(net.minecraft.server.MinecraftServer server, ServerLevel level,
                com.mojang.authlib.GameProfile profile, net.minecraft.server.level.ClientInformation information) {
            super(server, level, profile, information);
        }

        @Override
        public void sendMerchantOffers(int containerId, net.minecraft.world.item.trading.MerchantOffers offers,
                int villagerLevel, int villagerXp, boolean showProgress, boolean canRestock) {
            sentCosts = offers.stream().map(offer -> offer.getCostA().getCount()).toList();
            super.sendMerchantOffers(containerId, offers, villagerLevel, villagerXp, showProgress, canRestock);
        }
    }

    @Override
    public void invokeTestMethod(GameTestHelper ctx, Method method) throws ReflectiveOperationException {
        try {
            method.invoke(this, ctx);
        } finally {
            for (ServerPlayer player : players) ctx.getLevel().getServer().getPlayerList().remove(player);
            players.clear();
        }
    }
}
