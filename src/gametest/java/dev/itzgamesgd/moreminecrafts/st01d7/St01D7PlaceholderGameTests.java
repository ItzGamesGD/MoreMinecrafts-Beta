package dev.itzgamesgd.moreminecrafts.st01d7;

import dev.itzgamesgd.moreminecrafts.mixin.St01MerchantOffersAccessor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

/**
 * ST-01 P8-D7 experiment: the placeholder map offer through the real villager interaction, the real select-trade packet
 * handler and the real merchant menu (mock players), plus save/load, level-up, profession change and replacement.
 */
public final class St01D7PlaceholderGameTests {

    private static Villager cartographer(GameTestHelper ctx, int level, BlockPos rel) {
        ServerLevel l = ctx.getLevel();
        Villager v = EntityTypes.VILLAGER.create(l, EntitySpawnReason.COMMAND);
        BlockPos at = ctx.absolutePos(rel);
        v.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        v.setNoAi(true);
        l.addFreshEntity(v);
        var prof = l.registryAccess().lookupOrThrow(Registries.VILLAGER_PROFESSION).getOrThrow(VillagerProfession.CARTOGRAPHER);
        v.setVillagerData(v.getVillagerData().withProfession(prof).withLevel(level));
        v.setVillagerXp(level >= 2 ? 10 : 1);
        return v;
    }

    private static ServerPlayer playerNear(GameTestHelper ctx, Villager v) {
        ServerPlayer p = ctx.makeMockServerPlayerInLevel();
        p.snapTo(v.getX() + 1, v.getY(), v.getZ());
        return p;
    }

    private static MerchantOffer realMap() {
        ItemStack map = new ItemStack(Items.FILLED_MAP);
        CompoundTag t = new CompoundTag();
        t.putBoolean("d7_real", true);
        map.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        return new MerchantOffer(new ItemCost(Items.EMERALD, 8), Optional.of(new ItemCost(Items.COMPASS)), map, 12, 5, 0.2f);
    }

    private static boolean isReal(ItemStack s) {
        CustomData d = s.get(DataComponents.CUSTOM_DATA);
        return d != null && d.copyTag().getBooleanOr("d7_real", false);
    }

    private static int count(ServerPlayer p, java.util.function.Predicate<ItemStack> f) {
        int n = f.test(p.containerMenu.getCarried()) ? p.containerMenu.getCarried().getCount() : 0;
        for (ItemStack s : p.getInventory()) if (f.test(s)) n += s.getCount();
        return n;
    }

    private static MerchantOffers offers(Villager v) {
        return ((St01MerchantOffersAccessor) v).moreminecrafts$offers();
    }

    private static int addPlaceholder(Villager v) {
        MerchantOffers o = v.getOffers();
        o.add(St01D7Placeholders.placeholder("st01bench:map_r20", "minecraft:cartographer/level_2", 20));
        return o.size() - 1;
    }

    private static boolean open(Villager v, ServerPlayer p) {
        v.mobInteract(p, InteractionHand.MAIN_HAND);
        return p.containerMenu instanceof MerchantMenu && v.getTradingPlayer() == p;
    }

    @GameTest(maxTicks = 100)
    public void placeholderIsNeverSoldThroughServerTradePaths(GameTestHelper ctx) {
        Villager v = cartographer(ctx, 2, new BlockPos(1, 2, 1));
        int idx = addPlaceholder(v);
        MerchantOffer p0 = v.getOffers().get(idx);
        ServerPlayer p = playerNear(ctx, v);
        p.getInventory().add(new ItemStack(Items.EMERALD, 64));
        p.getInventory().add(new ItemStack(Items.COMPASS, 1));
        ctx.assertTrue(open(v, p), "trading screen did not open through mobInteract");
        int xp0 = v.getVillagerXp(), emeralds0 = count(p, s -> s.is(Items.EMERALD)), compasses0 = count(p, s -> s.is(Items.COMPASS));
        MerchantMenu menu = (MerchantMenu) p.containerMenu;
        // the client selects the placeholder: the server's select-trade handler moves nothing and offers no result
        p.connection.handleSelectTrade(new ServerboundSelectTradePacket(idx));
        ctx.assertTrue(menu.getSlot(0).getItem().isEmpty() && menu.getSlot(1).getItem().isEmpty(), "payment moved for a placeholder");
        ctx.assertTrue(menu.getSlot(2).getItem().isEmpty(), "result offered for a placeholder");
        // a modified client puts the displayed cost in the payment slots anyway and tries every way to take the result
        menu.getSlot(0).set(new ItemStack(Items.EMERALD, 64));
        menu.getSlot(1).set(new ItemStack(Items.COMPASS, 1));
        p.connection.handleSelectTrade(new ServerboundSelectTradePacket(idx));
        for (ContainerInput input : List.of(ContainerInput.PICKUP, ContainerInput.QUICK_MOVE, ContainerInput.THROW, ContainerInput.PICKUP_ALL))
            menu.clicked(2, 0, input, p);
        ctx.assertTrue(!St01D7Placeholders.isPlaceholder(new MerchantOffer(new ItemCost(Items.EMERALD), menu.getSlot(2).getItem(), 1, 0, 0)), "placeholder result in slot");
        ctx.assertTrue(count(p, s -> s.has(DataComponents.CUSTOM_DATA) && s.get(DataComponents.CUSTOM_DATA).copyTag().contains(St01D7Placeholders.MARKER)) == 0,
                "placeholder item reached the player");
        ctx.assertTrue(p0.getUses() == 0 && v.getVillagerXp() == xp0, "a placeholder trade was executed");
        // payment conservation: whatever the slots held is still in the slots or the inventory (no other offer matched it)
        int emeralds = count(p, s -> s.is(Items.EMERALD)) + menu.getSlot(0).getItem().getCount();
        ctx.assertTrue(emeralds >= emeralds0, "emeralds consumed: " + emeralds0 + " -> " + emeralds);
        // other code paths: direct take (other mods), restock, out-of-range packet
        ItemStack a = new ItemStack(Items.EMERALD, 64), b = new ItemStack(Items.COMPASS);
        ctx.assertTrue(!p0.satisfiedBy(a, b) && !p0.take(a, b) && a.getCount() == 64 && b.getCount() == 1, "direct take of a placeholder succeeded");
        v.restock();
        ctx.assertTrue(p0.isOutOfStock() && p0.getUses() == 0, "restock made the placeholder available");
        p.connection.handleSelectTrade(new ServerboundSelectTradePacket(9999));
        p.connection.handleSelectTrade(new ServerboundSelectTradePacket(-5));
        ctx.assertTrue(menu.getSlot(2).getItem().isEmpty() || !St01D7Placeholders.isPlaceholder(new MerchantOffer(new ItemCost(Items.EMERALD), menu.getSlot(2).getItem(), 1, 0, 0)),
                "out-of-range selection produced a placeholder result");
        ctx.assertTrue(compasses0 >= 1, "setup");
        p.closeContainer();
        v.discard();
        ctx.succeed();
    }

    @GameTest(maxTicks = 100)
    public void placeholderDoesNotShadowAnOfferWithTheSameCostItem(GameTestHelper ctx) {
        Villager v = cartographer(ctx, 2, new BlockPos(1, 2, 1));
        MerchantOffers o = v.getOffers();
        o.clear();
        o.add(St01D7Placeholders.placeholder("st01bench:map_r20", "minecraft:cartographer/level_2", 20));
        o.add(new MerchantOffer(new ItemCost(Items.EMERALD, 1), new ItemStack(Items.PAPER, 3), 12, 1, 0.0f));
        ServerPlayer p = playerNear(ctx, v);
        ctx.assertTrue(open(v, p), "open");
        MerchantMenu menu = (MerchantMenu) p.containerMenu;
        menu.getSlot(0).set(new ItemStack(Items.EMERALD, 1)); // no selection: vanilla picks the first satisfied offer
        ctx.assertTrue(menu.getSlot(2).getItem().is(Items.PAPER), "the placeholder (listed first) must not hide the paper offer: " + menu.getSlot(2).getItem());
        p.closeContainer();
        v.discard();
        ctx.succeed();
    }

    @GameTest(maxTicks = 100)
    public void replacementWhileTheScreenIsOpenSellsTheRealMapOnce(GameTestHelper ctx) {
        Villager v = cartographer(ctx, 2, new BlockPos(1, 2, 1));
        List<MerchantOffer> before = new ArrayList<>(v.getOffers());
        int idx = addPlaceholder(v);
        ServerPlayer p = playerNear(ctx, v);
        p.getInventory().add(new ItemStack(Items.EMERALD, 64));
        p.getInventory().add(new ItemStack(Items.COMPASS, 2));
        ctx.assertTrue(open(v, p), "open");
        MerchantMenu menu = (MerchantMenu) p.containerMenu;
        p.connection.handleSelectTrade(new ServerboundSelectTradePacket(idx));
        ctx.assertTrue(menu.getSlot(2).getItem().isEmpty(), "placeholder selected: no result");
        int size = v.getOffers().size(), xp0 = v.getVillagerXp();
        St01D7Placeholders.resolverOverride = (vv, m) -> Optional.of(realMap());
        try {
            var outcome = St01D7Placeholders.replace(v, ctx.getLevel());
            ctx.assertTrue(outcome.equals(List.of(St01D7Placeholders.Outcome.REPLACED)), "outcome " + outcome);
            ctx.assertTrue(St01D7Placeholders.replace(v, ctx.getLevel()).isEmpty(), "replacement must be idempotent");
        } finally {
            St01D7Placeholders.resolverOverride = null;
        }
        MerchantOffers after = v.getOffers();
        ctx.assertTrue(after.size() == size, "slot count changed: " + size + " -> " + after.size());
        for (int i = 0; i < before.size(); i++) ctx.assertTrue(after.get(i) == before.get(i), "existing offer " + i + " changed");
        ctx.assertTrue(isReal(after.get(idx).getResult()), "the placeholder slot must hold the real offer");
        // the same selection now pays and sells exactly once
        p.connection.handleSelectTrade(new ServerboundSelectTradePacket(idx));
        ctx.assertTrue(isReal(menu.getSlot(2).getItem()), "real result not offered after replacement: " + menu.getSlot(2).getItem());
        menu.clicked(2, 0, ContainerInput.QUICK_MOVE, p);
        ctx.assertTrue(count(p, St01D7PlaceholderGameTests::isReal) >= 1 && after.get(idx).getUses() >= 1, "purchase failed");
        ctx.assertTrue(v.getVillagerXp() > xp0, "trade xp not awarded");
        int uses = after.get(idx).getUses();
        ctx.assertTrue(uses == count(p, St01D7PlaceholderGameTests::isReal), "uses " + uses + " != maps bought");
        p.closeContainer();
        v.discard();
        ctx.succeed();
    }

    @GameTest(maxTicks = 100)
    public void aSecondPlayerCannotTradeWithTheSameVillager(GameTestHelper ctx) {
        Villager v = cartographer(ctx, 2, new BlockPos(1, 2, 1));
        addPlaceholder(v);
        ServerPlayer a = playerNear(ctx, v), b = playerNear(ctx, v);
        ctx.assertTrue(open(v, a), "first player");
        v.mobInteract(b, InteractionHand.MAIN_HAND);
        ctx.assertTrue(!(b.containerMenu instanceof MerchantMenu) && v.getTradingPlayer() == a, "second player opened the same villager");
        a.closeContainer();
        ctx.assertTrue(v.getTradingPlayer() == null && open(v, b), "second player after the first closed");
        b.closeContainer();
        v.discard();
        ctx.succeed();
    }

    @GameTest(maxTicks = 100)
    public void placeholderSurvivesSaveAndLoadUnchangedAndUnpurchasable(GameTestHelper ctx) {
        Villager v = cartographer(ctx, 2, new BlockPos(1, 2, 1));
        int idx = addPlaceholder(v);
        int size = v.getOffers().size();
        TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, ctx.getLevel().registryAccess());
        v.saveWithoutId(out);
        CompoundTag tag = out.buildResult();
        Villager copy = EntityTypes.VILLAGER.create(ctx.getLevel(), EntitySpawnReason.LOAD);
        copy.load(TagValueInput.create(ProblemReporter.DISCARDING, ctx.getLevel().registryAccess(), tag));
        MerchantOffers loaded = offers(copy);
        ctx.assertTrue(loaded != null && loaded.size() == size, "offers not restored");
        MerchantOffer p = loaded.get(idx);
        ctx.assertTrue(St01D7Placeholders.isPlaceholder(p) && St01D7Placeholders.markerOf(p).get().getIntOr("radius", -1) == 20, "marker lost");
        ctx.assertTrue(p.isOutOfStock() && p.getMaxUses() == 0 && !p.satisfiedBy(new ItemStack(Items.EMERALD, 64), ItemStack.EMPTY), "loaded placeholder purchasable");
        v.discard();
        ctx.succeed();
    }

    @GameTest(maxTicks = 100)
    public void levelUpsKeepThePlaceholderSlotAndAProfessionChangeClearsIt(GameTestHelper ctx) throws Exception {
        Villager v = cartographer(ctx, 2, new BlockPos(1, 2, 1));
        int idx = addPlaceholder(v);
        List<MerchantOffer> before = new ArrayList<>(v.getOffers());
        v.setVillagerXp(70);
        Method m = Villager.class.getDeclaredMethod("increaseMerchantCareer", ServerLevel.class);
        m.setAccessible(true);
        m.invoke(v, ctx.getLevel());
        MerchantOffers after = v.getOffers();
        ctx.assertTrue(v.getVillagerData().level() == 3 && after.size() > before.size(), "level 3 trades not added");
        for (int i = 0; i < before.size(); i++) ctx.assertTrue(after.get(i) == before.get(i), "offer " + i + " moved or changed by the level-up");
        ctx.assertTrue(St01D7Placeholders.isPlaceholder(after.get(idx)), "placeholder lost by the level-up");
        var farmer = ctx.getLevel().registryAccess().lookupOrThrow(Registries.VILLAGER_PROFESSION).getOrThrow(VillagerProfession.FARMER);
        v.setVillagerData(v.getVillagerData().withProfession(farmer));
        ctx.assertTrue(offers(v) == null, "vanilla must reset offers on a profession change");
        ctx.assertTrue(v.getOffers().stream().noneMatch(St01D7Placeholders::isPlaceholder), "placeholder survived a profession change");
        ctx.assertTrue(St01D7Placeholders.replace(v, ctx.getLevel()).isEmpty(), "nothing to replace after a profession change");
        v.discard();
        ctx.succeed();
    }

    @GameTest(maxTicks = 100)
    public void notFoundEitherKeepsAnUnpurchasablePlaceholderOrRerollsLikeVanilla(GameTestHelper ctx) {
        Villager v = cartographer(ctx, 2, new BlockPos(1, 2, 1));
        int idx = addPlaceholder(v);
        int size = v.getOffers().size();
        String previous = St01D7Placeholders.notFound;
        St01D7Placeholders.resolverOverride = (vv, m) -> Optional.empty();
        try {
            St01D7Placeholders.notFound = "keep";
            ctx.assertTrue(St01D7Placeholders.replace(v, ctx.getLevel()).equals(List.of(St01D7Placeholders.Outcome.NOT_FOUND_KEPT)), "keep");
            MerchantOffer kept = v.getOffers().get(idx);
            ctx.assertTrue(St01D7Placeholders.isPlaceholder(kept) && kept.isOutOfStock() && v.getOffers().size() == size, "kept placeholder");
            ctx.assertTrue(St01D7Placeholders.replace(v, ctx.getLevel()).isEmpty(), "an unresolved placeholder is not retried");
            // reroll: a fresh placeholder, then a vanilla-like draw of another trade of the cartographer level-2 set
            v.getOffers().set(idx, St01D7Placeholders.placeholder("st01bench:map_r20", "minecraft:cartographer/level_2", 20));
            St01D7Placeholders.notFound = "reroll";
            var outcome = St01D7Placeholders.replace(v, ctx.getLevel());
            ctx.assertTrue(outcome.equals(List.of(St01D7Placeholders.Outcome.NOT_FOUND_REROLLED)), "reroll outcome " + outcome);
            ctx.assertTrue(!St01D7Placeholders.isPlaceholder(v.getOffers().get(idx)) && v.getOffers().size() == size, "rerolled slot");
        } finally {
            St01D7Placeholders.notFound = previous;
            St01D7Placeholders.resolverOverride = null;
        }
        v.discard();
        ctx.succeed();
    }

    @GameTest(maxTicks = 100)
    public void otherTradeHooksLeaveThePlaceholderAndReplacementAlone(GameTestHelper ctx) {
        Villager v = cartographer(ctx, 2, new BlockPos(1, 2, 1));
        int idx = addPlaceholder(v);
        int size = v.getOffers().size();
        for (int i = 0; i < 3; i++) v.getOffers(); // T1Supply.normalizeTrades and ContractTrades.ensureVillager run on every call
        ctx.assertTrue(v.getOffers().size() == size && St01D7Placeholders.isPlaceholder(v.getOffers().get(idx)), "a trade hook changed the list or the placeholder");
        St01D7Placeholders.resolverOverride = (vv, m) -> Optional.of(realMap());
        try {
            St01D7Placeholders.replace(v, ctx.getLevel());
        } finally {
            St01D7Placeholders.resolverOverride = null;
        }
        for (int i = 0; i < 3; i++) v.getOffers();
        ctx.assertTrue(v.getOffers().size() == size && isReal(v.getOffers().get(idx).getResult()), "a trade hook changed the replaced offer or added offers");
        v.discard();
        ctx.succeed();
    }

    @GameTest(maxTicks = 200)
    public void unloadingTheVillagerWithTheScreenOpenClosesItAndSkipsReplacement(GameTestHelper ctx) {
        Villager v = cartographer(ctx, 2, new BlockPos(1, 2, 1));
        addPlaceholder(v);
        ServerPlayer p = playerNear(ctx, v);
        ctx.assertTrue(open(v, p), "open");
        v.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
        ctx.assertTrue(St01D7Placeholders.replace(v, ctx.getLevel()).isEmpty(), "a removed villager must not be modified");
        ctx.succeedWhen(() -> ctx.assertTrue(!(p.containerMenu instanceof MerchantMenu), "the trading screen of an unloaded villager stayed open"));
    }
}
