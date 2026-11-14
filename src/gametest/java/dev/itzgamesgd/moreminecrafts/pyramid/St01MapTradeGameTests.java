package dev.itzgamesgd.moreminecrafts.pyramid;

import dev.itzgamesgd.moreminecrafts.mixin.St01MerchantOffersAccessor;
import dev.itzgamesgd.moreminecrafts.pyramid.maptrade.St01MapPlaceholders;
import dev.itzgamesgd.moreminecrafts.pyramid.maptrade.St01MapTargetReadiness;
import dev.itzgamesgd.moreminecrafts.pyramid.maptrade.St01MapTargetReadiness.State;
import dev.itzgamesgd.moreminecrafts.pyramid.maptrade.St01MapTrade;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraft.resources.ResourceKey;
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
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

/**
 * ST-01 P8-D8: the cartographer level-4 ST-01 map trade and its non-blocking placeholder, through the real villager
 * interaction, the real select-trade packet handler and the merchant menu (mock players). The GameTest level has no ST-01
 * placement, so real maps are exercised by the dedicated-server benchmark; here readiness is stubbed per villager.
 */
public final class St01MapTradeGameTests {

    private static final Map<Villager, St01MapPlaceholders.Readiness> STUBS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final St01MapPlaceholders.Readiness PRODUCTION = St01MapTargetReadiness::evaluate;

    static {
        St01MapPlaceholders.readiness = (v, l, r, p, a) -> STUBS.getOrDefault(v, PRODUCTION).evaluate(v, l, r, p, a);
    }

    /** A cartographer whose level-4 roll is already used up (so it never adds a map by itself), unless {@code roll}. */
    private static Villager cartographer(GameTestHelper ctx, int level) {
        return cartographer(ctx, level, false);
    }

    private static Villager cartographer(GameTestHelper ctx, int level, boolean roll) {
        ServerLevel l = ctx.getLevel();
        Villager v = EntityTypes.VILLAGER.create(l, EntitySpawnReason.COMMAND);
        if (!roll) v.setAttached(St01MapTrade.ROLLED, true);
        BlockPos at = ctx.absolutePos(new BlockPos(1, 2, 1));
        v.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        v.setNoAi(true);
        l.addFreshEntity(v);
        var prof = l.registryAccess().lookupOrThrow(Registries.VILLAGER_PROFESSION).getOrThrow(VillagerProfession.CARTOGRAPHER);
        v.setVillagerData(v.getVillagerData().withProfession(prof).withLevel(level));
        v.setVillagerXp(new int[] {0, 1, 10, 70, 150, 250}[level]);
        return v;
    }

    private static Holder<VillagerTrade> mapTrade(GameTestHelper ctx) {
        return ctx.getLevel().registryAccess().lookupOrThrow(Registries.VILLAGER_TRADE).getOrThrow(St01MapTrade.TRADE);
    }

    private static ResourceKey<TradeSet> levelFourSet(Villager v) {
        return v.getVillagerData().profession().value().getTrades(4);
    }

    private static int addPlaceholder(GameTestHelper ctx, Villager v, boolean appended, ResourceKey<TradeSet> set) {
        MerchantOffers o = v.getOffers();
        o.add(St01MapPlaceholders.placeholderFor(mapTrade(ctx), v, ctx.getLevel(), set, appended));
        return o.size() - 1;
    }

    private static ServerPlayer playerNear(GameTestHelper ctx, Villager v) {
        ServerPlayer p = ctx.makeMockServerPlayerInLevel();
        p.snapTo(v.getX() + 1, v.getY(), v.getZ());
        return p;
    }

    private static boolean open(Villager v, ServerPlayer p) {
        v.mobInteract(p, InteractionHand.MAIN_HAND);
        return p.containerMenu instanceof MerchantMenu && v.getTradingPlayer() == p;
    }

    private static boolean isContract(MerchantOffer o) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(o.getResult().getItem()).getPath().equals("bounty_contract");
    }

    private static MerchantOffers offers(Villager v) {
        return ((St01MerchantOffersAccessor) v).moreminecrafts$offers();
    }

    private static int count(ServerPlayer p, java.util.function.Predicate<ItemStack> f) {
        int n = f.test(p.containerMenu.getCarried()) ? p.containerMenu.getCarried().getCount() : 0;
        for (ItemStack s : p.getInventory()) if (f.test(s)) n += s.getCount();
        return n;
    }

    private static void levelUp(GameTestHelper ctx, Villager v) throws Exception {
        v.setVillagerXp(new int[] {0, 10, 70, 150, 250, 250}[v.getVillagerData().level()]);
        Method m = Villager.class.getDeclaredMethod("increaseMerchantCareer", ServerLevel.class);
        m.setAccessible(true);
        m.invoke(v, ctx.getLevel());
    }

    private static void withChance(String chance, Runnable r) {
        String previous = System.getProperty("mm.st01.maptrade.chance");
        System.setProperty("mm.st01.maptrade.chance", chance);
        try {
            r.run();
        } finally {
            if (previous == null) System.clearProperty("mm.st01.maptrade.chance"); else System.setProperty("mm.st01.maptrade.chance", previous);
        }
    }

    @GameTest(maxTicks = 100)
    public void theMapTradeIsCartographerLevelFourSixteenEmeraldsAndACompass(GameTestHelper ctx) {
        Villager v = cartographer(ctx, 4);
        MerchantOffer p = St01MapPlaceholders.placeholderFor(mapTrade(ctx), v, ctx.getLevel(), levelFourSet(v), true);
        ctx.assertTrue(p.getItemCostA().item().value() == Items.EMERALD && p.getItemCostA().count() == 16, "price A " + p.getItemCostA());
        ctx.assertTrue(p.getItemCostB().isPresent() && p.getItemCostB().get().item().value() == Items.COMPASS && p.getItemCostB().get().count() == 1,
                "price B " + p.getItemCostB());
        ctx.assertTrue(St01MapTrade.LEVEL == 4 && St01MapTrade.CHANCE == 0.5 && St01MapTrade.PROFESSION.equals(VillagerProfession.CARTOGRAPHER), "constants");
        v.discard();
        ctx.succeed();
    }

    @GameTest(maxTicks = 100)
    public void reachingLevelFourRollsOnceAndAppendsAfterTheVanillaOffers(GameTestHelper ctx) {
        Villager v = cartographer(ctx, 3, true);
        STUBS.put(v, (vv, l, r, p, a) -> State.NOT_READY);
        List<MerchantOffer> before = new ArrayList<>(v.getOffers());
        withChance("1", () -> {
            try { levelUp(ctx, v); } catch (Exception e) { throw new RuntimeException(e); }
        });
        MerchantOffers after = offers(v);
        ctx.assertTrue(v.getVillagerData().level() == 4 && Boolean.TRUE.equals(v.getAttached(St01MapTrade.ROLLED)), "rolled at level 4");
        for (int i = 0; i < before.size(); i++) ctx.assertTrue(after.get(i) == before.get(i), "earlier offer " + i + " changed");
        long placeholders = after.stream().filter(St01MapPlaceholders::isPlaceholder).count();
        ctx.assertTrue(placeholders == 1 && St01MapPlaceholders.isPlaceholder(after.getLast()), "one placeholder, last: " + after.size());
        int vanillaLevelFour = after.size() - before.size() - 1;
        ctx.assertTrue(vanillaLevelFour >= 2, "the vanilla level-4 offers must still be generated: " + vanillaLevelFour);
        // generating the level-4 trades again (lazy regeneration) never rolls twice
        withChance("1", () -> St01MapTrade.onTradesGenerated(v));
        ctx.assertTrue(offers(v).stream().filter(St01MapPlaceholders::isPlaceholder).count() == 1, "rolled twice");
        // chance 0: rolled, nothing added
        Villager w = cartographer(ctx, 3, true);
        int size = w.getOffers().size();
        withChance("0", () -> {
            try { levelUp(ctx, w); } catch (Exception e) { throw new RuntimeException(e); }
        });
        ctx.assertTrue(Boolean.TRUE.equals(w.getAttached(St01MapTrade.ROLLED)) && offers(w).stream().noneMatch(St01MapPlaceholders::isPlaceholder)
                && offers(w).stream().noneMatch(o -> o.getResult().is(Items.FILLED_MAP) && o.getItemCostA().count() == 16), "chance 0 added a map " + size);
        STUBS.remove(v);
        St01MapPlaceholders.resolve(v, ctx.getLevel(), false);
        v.discard();
        w.discard();
        ctx.succeed();
    }

    @GameTest(maxTicks = 100)
    public void whenTheSearchCanFindNothingTheSlotGetsAnotherLevelFourTrade(GameTestHelper ctx) {
        Villager v = cartographer(ctx, 3, true);
        STUBS.put(v, (vv, l, r, p, a) -> State.NOT_FOUND); // e.g. no ST-01 within the radius, or no ST-01 placement in this dimension
        int before = (int) v.getOffers().stream().filter(o -> !isContract(o)).count();
        withChance("1", () -> {
            try { levelUp(ctx, v); } catch (Exception e) { throw new RuntimeException(e); }
        });
        MerchantOffers all = offers(v);
        ctx.assertTrue(all.stream().noneMatch(St01MapPlaceholders::isPlaceholder), "no placeholder where nothing can be found");
        // the mod's contract offers (topped up separately) are not part of the vanilla level-4 slots
        List<MerchantOffer> after = all.stream().filter(o -> !isContract(o)).toList();
        ctx.assertTrue(after.size() == before + 3, "two vanilla level-4 offers plus the substitute: " + before + " -> " + after.size());
        MerchantOffer substitute = after.getLast();
        ctx.assertTrue(!substitute.getResult().is(Items.FILLED_MAP), "the substitute must not be a map");
        for (int i = before; i < after.size() - 1; i++)
            ctx.assertTrue(!(ItemStack.isSameItemSameComponents(after.get(i).getResult(), substitute.getResult())
                    && after.get(i).getItemCostA().item().equals(substitute.getItemCostA().item())), "substitute duplicates offer " + i);
        v.discard();
        ctx.succeed();
    }

    @GameTest(maxTicks = 100)
    public void thePlaceholderIsNeverSoldThroughServerTradePaths(GameTestHelper ctx) {
        Villager v = cartographer(ctx, 4);
        int idx = addPlaceholder(ctx, v, true, levelFourSet(v));
        MerchantOffer p0 = v.getOffers().get(idx);
        ServerPlayer p = playerNear(ctx, v);
        p.getInventory().add(new ItemStack(Items.EMERALD, 64));
        p.getInventory().add(new ItemStack(Items.COMPASS, 2));
        ctx.assertTrue(open(v, p), "trading screen did not open");
        int xp0 = v.getVillagerXp();
        MerchantMenu menu = (MerchantMenu) p.containerMenu;
        p.connection.handleSelectTrade(new ServerboundSelectTradePacket(idx));
        ctx.assertTrue(menu.getSlot(0).getItem().isEmpty() && menu.getSlot(1).getItem().isEmpty(), "payment moved for a placeholder");
        ctx.assertTrue(menu.getSlot(2).getItem().isEmpty(), "result offered for a placeholder");
        menu.getSlot(0).set(new ItemStack(Items.EMERALD, 64));
        menu.getSlot(1).set(new ItemStack(Items.COMPASS, 1));
        p.connection.handleSelectTrade(new ServerboundSelectTradePacket(idx));
        for (ContainerInput input : List.of(ContainerInput.PICKUP, ContainerInput.QUICK_MOVE, ContainerInput.THROW, ContainerInput.PICKUP_ALL))
            menu.clicked(2, 0, input, p);
        ctx.assertTrue(count(p, s -> s.is(Items.MAP) || s.is(Items.FILLED_MAP)) == 0, "a map reached the player");
        ctx.assertTrue(p0.getUses() == 0 && v.getVillagerXp() == xp0, "a placeholder trade was executed");
        ItemStack a = new ItemStack(Items.EMERALD, 64), b = new ItemStack(Items.COMPASS);
        ctx.assertTrue(!p0.satisfiedBy(a, b) && !p0.take(a, b) && a.getCount() == 64 && b.getCount() == 1, "direct take succeeded");
        v.restock();
        ctx.assertTrue(p0.isOutOfStock() && p0.getUses() == 0, "restock made the placeholder available");
        p.connection.handleSelectTrade(new ServerboundSelectTradePacket(9999));
        p.connection.handleSelectTrade(new ServerboundSelectTradePacket(-5));
        p.closeContainer();
        St01MapPlaceholders.resolve(v, ctx.getLevel(), false);
        v.discard();
        ctx.succeed();
    }

    @GameTest(maxTicks = 100)
    public void thePlaceholderDoesNotHideAnOfferWithTheSameCostItem(GameTestHelper ctx) {
        Villager v = cartographer(ctx, 4);
        MerchantOffers o = v.getOffers();
        o.clear();
        o.add(St01MapPlaceholders.placeholderFor(mapTrade(ctx), v, ctx.getLevel(), levelFourSet(v), true));
        o.add(new MerchantOffer(new ItemCost(Items.EMERALD, 1), new ItemStack(Items.PAPER, 3), 12, 1, 0.0f));
        ServerPlayer p = playerNear(ctx, v);
        ctx.assertTrue(open(v, p), "open");
        MerchantMenu menu = (MerchantMenu) p.containerMenu;
        menu.getSlot(0).set(new ItemStack(Items.EMERALD, 16));
        ctx.assertTrue(menu.getSlot(2).getItem().is(Items.PAPER), "the placeholder must not hide the paper offer: " + menu.getSlot(2).getItem());
        p.closeContainer();
        o.clear();
        v.discard();
        ctx.succeed();
    }

    @GameTest(maxTicks = 100)
    public void resolvingWhileTheScreenIsOpenKeepsTheSlotAndSellsOnce(GameTestHelper ctx) {
        Villager v = cartographer(ctx, 4);
        List<MerchantOffer> before = new ArrayList<>(v.getOffers());
        int idx = addPlaceholder(ctx, v, true, levelFourSet(v));
        ServerPlayer p = playerNear(ctx, v);
        p.getInventory().add(new ItemStack(Items.EMERALD, 64));
        ctx.assertTrue(open(v, p), "open");
        MerchantMenu menu = (MerchantMenu) p.containerMenu;
        p.connection.handleSelectTrade(new ServerboundSelectTradePacket(idx));
        int size = v.getOffers().size(), xp0 = v.getVillagerXp();
        // no ST-01 placement here: the real trade finds nothing, so the slot receives another level-4 trade
        var outcome = St01MapPlaceholders.resolve(v, ctx.getLevel(), true);
        ctx.assertTrue(outcome.equals(List.of(St01MapPlaceholders.Outcome.FALLBACK_TRADE)), "outcome " + outcome);
        ctx.assertTrue(St01MapPlaceholders.resolve(v, ctx.getLevel(), true).isEmpty(), "resolution must be idempotent");
        MerchantOffers after = v.getOffers();
        ctx.assertTrue(after.size() == size, "slot count changed: " + size + " -> " + after.size());
        for (int i = 0; i < before.size(); i++) ctx.assertTrue(after.get(i) == before.get(i), "existing offer " + i + " changed");
        MerchantOffer substitute = after.get(idx);
        ctx.assertTrue(!St01MapPlaceholders.isPlaceholder(substitute), "slot still holds the placeholder");
        menu.getSlot(0).set(new ItemStack(Items.EMERALD, 64));
        p.connection.handleSelectTrade(new ServerboundSelectTradePacket(idx));
        ItemStack shown = menu.getSlot(2).getItem().copy();
        ctx.assertTrue(!shown.isEmpty() && ItemStack.isSameItem(shown, substitute.getResult()), "substitute not offered after resolution: " + shown);
        menu.clicked(2, 0, ContainerInput.QUICK_MOVE, p);
        ctx.assertTrue(substitute.getUses() >= 1 && v.getVillagerXp() > xp0, "purchase after resolution failed: shown " + shown + " uses "
                + substitute.getUses() + " xp " + xp0 + "->" + v.getVillagerXp() + " slot0 " + menu.getSlot(0).getItem() + " result " + menu.getSlot(2).getItem());
        p.closeContainer();
        v.discard();
        ctx.succeed();
    }

    @GameTest(maxTicks = 100)
    public void anAppendedSlotWithNoSubstituteIsRemovedAndAPoolSlotStaysUnpurchasable(GameTestHelper ctx) {
        Villager v = cartographer(ctx, 4);
        int size = v.getOffers().size();
        addPlaceholder(ctx, v, true, null);
        ctx.assertTrue(St01MapPlaceholders.resolve(v, ctx.getLevel(), false).equals(List.of(St01MapPlaceholders.Outcome.REMOVED))
                && v.getOffers().size() == size, "appended slot");
        int idx = addPlaceholder(ctx, v, false, null);
        ctx.assertTrue(St01MapPlaceholders.resolve(v, ctx.getLevel(), false).equals(List.of(St01MapPlaceholders.Outcome.KEPT_UNPURCHASABLE)), "pool slot");
        MerchantOffer kept = v.getOffers().get(idx);
        ctx.assertTrue(St01MapPlaceholders.isPlaceholder(kept) && kept.isOutOfStock() && !kept.satisfiedBy(new ItemStack(Items.EMERALD, 64), new ItemStack(Items.COMPASS)),
                "kept slot must stay unpurchasable");
        ctx.assertTrue(St01MapPlaceholders.resolve(v, ctx.getLevel(), true).isEmpty(), "an unresolved placeholder is final");
        v.discard();
        ctx.succeed();
    }

    @GameTest(maxTicks = 300)
    public void repeatedFailuresGiveUpAndSubstituteInsteadOfWaitingForever(GameTestHelper ctx) {
        Villager v = cartographer(ctx, 4);
        int idx = addPlaceholder(ctx, v, true, levelFourSet(v));
        int[] calls = {0};
        STUBS.put(v, (vv, l, r, p, a) -> {
            calls[0]++;
            p.countFailure(); // a preparation that keeps failing (without the back-off delay)
            return State.NOT_READY;
        });
        St01MapPlaceholders.markPending(v);
        ctx.succeedWhen(() -> {
            ctx.assertTrue(!St01MapPlaceholders.isPlaceholder(v.getOffers().get(idx)), "still waiting after " + calls[0] + " attempts");
            ctx.assertTrue(calls[0] == St01MapPlaceholders.MAX_FAILURES + 1, "gave up after " + calls[0] + " attempts");
            STUBS.remove(v);
            v.discard();
        });
    }

    @GameTest(maxTicks = 100)
    public void aPlaceholderOlderThanTheDeadlineIsSubstituted(GameTestHelper ctx) {
        Villager v = cartographer(ctx, 4);
        MerchantOffer p = St01MapPlaceholders.placeholderFor(mapTrade(ctx), v, ctx.getLevel(), levelFourSet(v), true);
        CompoundTag marker = St01MapPlaceholders.markerOf(p).get();
        marker.putLong("since", ctx.getLevel().getGameTime() - St01MapPlaceholders.DEADLINE_TICKS - 1);
        CompoundTag root = new CompoundTag();
        root.put(St01MapPlaceholders.MARKER, marker);
        p.getResult().set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(root));
        v.getOffers().add(p);
        int idx = v.getOffers().size() - 1;
        STUBS.put(v, (vv, l, r, pp, a) -> State.NOT_READY);
        St01MapPlaceholders.markPending(v);
        ctx.succeedWhen(() -> {
            ctx.assertTrue(!St01MapPlaceholders.isPlaceholder(v.getOffers().get(idx)), "an expired placeholder must be substituted");
            STUBS.remove(v);
            v.discard();
        });
    }

    @GameTest(maxTicks = 200)
    public void aSavedPlaceholderIsRestoredUnpurchasableAndPendingAgainOnLoad(GameTestHelper ctx) {
        Villager v = cartographer(ctx, 4);
        int idx = addPlaceholder(ctx, v, true, levelFourSet(v));
        int size = v.getOffers().size();
        TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, ctx.getLevel().registryAccess());
        v.saveWithoutId(out);
        CompoundTag tag = out.buildResult();
        St01MapPlaceholders.resolve(v, ctx.getLevel(), false);
        v.discard();
        Villager copy = EntityTypes.VILLAGER.create(ctx.getLevel(), EntitySpawnReason.LOAD);
        copy.load(TagValueInput.create(ProblemReporter.DISCARDING, ctx.getLevel().registryAccess(), tag));
        copy.setUUID(java.util.UUID.randomUUID());
        MerchantOffers loaded = offers(copy);
        ctx.assertTrue(loaded != null && loaded.size() == size, "offers not restored");
        MerchantOffer p = loaded.get(idx);
        ctx.assertTrue(St01MapPlaceholders.isPlaceholder(p) && p.isOutOfStock() && p.getMaxUses() == 0
                && !p.satisfiedBy(new ItemStack(Items.EMERALD, 64), new ItemStack(Items.COMPASS)), "restored placeholder purchasable or lost");
        STUBS.put(copy, (vv, l, r, pp, a) -> State.NOT_READY);
        int pending = St01MapPlaceholders.pendingCount();
        ctx.getLevel().addFreshEntity(copy); // ENTITY_LOAD re-registers it
        ctx.succeedWhen(() -> {
            ctx.assertTrue(St01MapPlaceholders.pendingCount() > pending || ctx.getLevel().getEntity(copy.getUUID()) != copy,
                    "a loaded villager with a placeholder must be pending again");
            STUBS.remove(copy);
            St01MapPlaceholders.resolve(copy, ctx.getLevel(), false);
            copy.discard();
        });
    }

    @GameTest(maxTicks = 100)
    public void levelUpsKeepThePlaceholderSlotAndAProfessionChangeClearsIt(GameTestHelper ctx) throws Exception {
        Villager v = cartographer(ctx, 4);
        int idx = addPlaceholder(ctx, v, true, levelFourSet(v));
        List<MerchantOffer> before = new ArrayList<>(v.getOffers());
        levelUp(ctx, v);
        MerchantOffers after = v.getOffers();
        ctx.assertTrue(v.getVillagerData().level() == 5 && after.size() > before.size(), "level 5 trades not added");
        for (int i = 0; i < before.size(); i++) ctx.assertTrue(after.get(i) == before.get(i), "offer " + i + " moved or changed by the level-up");
        ctx.assertTrue(St01MapPlaceholders.isPlaceholder(after.get(idx)), "placeholder lost by the level-up");
        var farmer = ctx.getLevel().registryAccess().lookupOrThrow(Registries.VILLAGER_PROFESSION).getOrThrow(VillagerProfession.FARMER);
        v.setVillagerData(v.getVillagerData().withProfession(farmer));
        ctx.assertTrue(offers(v) == null, "vanilla must reset offers on a profession change");
        ctx.assertTrue(v.getOffers().stream().noneMatch(St01MapPlaceholders::isPlaceholder), "placeholder survived a profession change");
        ctx.assertTrue(St01MapPlaceholders.resolve(v, ctx.getLevel(), true).isEmpty(), "nothing to resolve after a profession change");
        v.discard();
        ctx.succeed();
    }

    @GameTest(maxTicks = 100)
    public void aSecondPlayerCannotTradeWithTheSameVillager(GameTestHelper ctx) {
        Villager v = cartographer(ctx, 4);
        addPlaceholder(ctx, v, true, levelFourSet(v));
        ServerPlayer a = playerNear(ctx, v), b = playerNear(ctx, v);
        ctx.assertTrue(open(v, a), "first player");
        v.mobInteract(b, InteractionHand.MAIN_HAND);
        ctx.assertTrue(!(b.containerMenu instanceof MerchantMenu) && v.getTradingPlayer() == a, "second player opened the same villager");
        a.closeContainer();
        ctx.assertTrue(v.getTradingPlayer() == null && open(v, b), "second player after the first closed");
        b.closeContainer();
        St01MapPlaceholders.resolve(v, ctx.getLevel(), false);
        v.discard();
        ctx.succeed();
    }

    @GameTest(maxTicks = 100)
    public void otherTradeHooksLeaveThePlaceholderAlone(GameTestHelper ctx) {
        Villager v = cartographer(ctx, 4);
        int idx = addPlaceholder(ctx, v, true, levelFourSet(v));
        int size = v.getOffers().size();
        for (int i = 0; i < 3; i++) v.getOffers(); // T1Supply.normalizeTrades and ContractTrades.ensureVillager run on every call
        ctx.assertTrue(v.getOffers().size() == size && St01MapPlaceholders.isPlaceholder(v.getOffers().get(idx)), "a trade hook changed the placeholder");
        St01MapPlaceholders.resolve(v, ctx.getLevel(), false);
        for (int i = 0; i < 3; i++) v.getOffers();
        ctx.assertTrue(v.getOffers().size() == size, "a trade hook added or removed offers after resolution");
        v.discard();
        ctx.succeed();
    }

    @GameTest(maxTicks = 200)
    public void unloadingTheVillagerWithTheScreenOpenClosesItAndSkipsResolution(GameTestHelper ctx) {
        Villager v = cartographer(ctx, 4);
        addPlaceholder(ctx, v, true, levelFourSet(v));
        ServerPlayer p = playerNear(ctx, v);
        ctx.assertTrue(open(v, p), "open");
        v.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
        ctx.assertTrue(St01MapPlaceholders.resolve(v, ctx.getLevel(), true).isEmpty(), "a removed villager must not be modified");
        ctx.succeedWhen(() -> ctx.assertTrue(!(p.containerMenu instanceof MerchantMenu), "the trading screen of an unloaded villager stayed open"));
    }
}
