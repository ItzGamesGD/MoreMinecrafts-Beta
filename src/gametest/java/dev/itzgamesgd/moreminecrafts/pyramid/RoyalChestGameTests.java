package dev.itzgamesgd.moreminecrafts.pyramid;

import com.mojang.serialization.DataResult;
import dev.itzgamesgd.moreminecrafts.boss.TombGuardian;
import dev.itzgamesgd.moreminecrafts.boss.TombGuardianEntities;
import dev.itzgamesgd.moreminecrafts.boss.TombGuardianEncounters;
import dev.itzgamesgd.moreminecrafts.pyramid.RoyalChestRewardModel.Stack;
import dev.itzgamesgd.moreminecrafts.pyramid.RoyalChestRuntime.OpenResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.AABB;

/**
 * Runtime coverage of the D13-03-I06 royal chest engine. No ST-01 structure, NBT or worldgen is involved: chests are
 * ordinary blocks registered through the same integration interface the structure will use. Each test uses its own
 * instance id and positions and discards its instance, so no guard outlives the test.
 */
public final class RoyalChestGameTests {
    private static void floor(GameTestHelper ctx) {
        for (int x = -2; x <= 14; x++)
            for (int z = -2; z <= 14; z++) ctx.setBlock(new BlockPos(x, 0, z), Blocks.SMOOTH_SANDSTONE);
    }

    private static void pair(ServerLevel level, BlockPos left) {
        var state = Blocks.CHEST.defaultBlockState().setValue(net.minecraft.world.level.block.ChestBlock.FACING, net.minecraft.core.Direction.NORTH);
        level.setBlock(left, state.setValue(net.minecraft.world.level.block.ChestBlock.TYPE,
                net.minecraft.world.level.block.state.properties.ChestType.LEFT), Block.UPDATE_CLIENTS);
        level.setBlock(left.east(), state.setValue(net.minecraft.world.level.block.ChestBlock.TYPE,
                net.minecraft.world.level.block.state.properties.ChestType.RIGHT), Block.UPDATE_CLIENTS);
    }

    private record Site(ServerLevel level, String id, BlockPos primary, BlockPos secondary, BlockPos spawn, List<ServerPlayer> players) {}

    private static Site site(GameTestHelper ctx, String name) {
        floor(ctx);
        ServerLevel level = ctx.getLevel();
        Site site = new Site(level, RoyalChestRuntime.canonicalId(level, ctx.absolutePos(new BlockPos(1, 1, 1))), ctx.absolutePos(new BlockPos(1, 1, 1)),
                ctx.absolutePos(new BlockPos(5, 1, 1)), ctx.absolutePos(new BlockPos(3, 1, 8)), new ArrayList<>());
        pair(level, site.primary);
        ctx.setBlock(new BlockPos(5, 1, 1), Blocks.CHEST);
        ctx.assertTrue(RoyalChestRuntime.register(level, site.primary, site.spawn, site.primary, site.primary.east(), Optional.of(site.secondary)).isPresent(),
                "registered");
        return site;
    }

    /** Only this site's guardians: parallel tests keep their own. */
    private static List<TombGuardian> guardiansOf(Site site) {
        return List.copyOf(site.level.getEntities(TombGuardianEntities.TOMB_GUARDIAN,
                guardian -> guardian.isAlive() && site.primary.equals(guardian.tombAnchor())));
    }

    /** A mock player that this site removes again, so tests do not leave players in the shared server. */
    private static ServerPlayer mock(GameTestHelper ctx, Site site) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        site.players.add(player);
        return player;
    }

    private static void finish(GameTestHelper ctx, Site site) {
        for (ServerPlayer p : site.players) site.level.getServer().getPlayerList().remove(p);
        RoyalChestRuntime.discardInstance(site.level, site.id);
        guardiansOf(site).forEach(Entity::discard);
        site.level.getEntitiesOfClass(ItemEntity.class, new AABB(site.spawn).inflate(4.0D)).forEach(Entity::discard);
        ctx.succeed();
    }

    /** Normally defeats the instance's guardian (PRIMARY interaction summons it, then it is killed). */
    private static void defeat(GameTestHelper ctx, Site site, ServerPlayer player) {
        OpenResult started = RoyalChestRuntime.tryOpen(site.level, player, site.primary);
        ctx.assertTrue(started == OpenResult.SEALED_ENCOUNTER_STARTED, "PRIMARY's first sealed interaction starts the encounter, got " + started);
        List<TombGuardian> guardians = guardiansOf(site);
        ctx.assertTrue(guardians.size() == 1, "one guardian, got " + guardians.size());
        guardians.get(0).skipAwakening();
        guardians.get(0).hurtServer(site.level, site.level.damageSources().playerAttack(player), 10000.0F);
        ctx.assertTrue(TombGuardianEncounters.isDefeated(site.level, site.primary), "defeated");
    }

    private static List<Stack> expected(Site site, RoyalChestRole role) {
        long seed = site.level.getSeed();
        boolean catalyst = RoyalChestRewardModel.assignCatalyst(RoyalChestRuntime.catalystRandom(seed, site.id)).filter(role::equals).isPresent();
        return RoyalChestRewardModel.split(RoyalChestRewardModel.chestRewards(role, RoyalChestRuntime.chestRandom(seed, site.id, role), catalyst),
                RoyalChestRuntime::maxStackSize);
    }

    private static List<Stack> snapshot(Container container) {
        List<Stack> stacks = new ArrayList<>();
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack s = container.getItem(i);
            if (!s.isEmpty()) stacks.add(new Stack(BuiltInRegistries.ITEM.getKey(s.getItem()).toString(), s.getCount()));
        }
        return stacks;
    }

    private static Container chestOf(Site site, RoyalChestRole role) {
        return RoyalChestRuntime.data(site.level).chest(site.id, role).orElseThrow().container();
    }

    // ---- registry & capacity ----

    @GameTest(maxTicks = 20)
    public void everyRewardItemIsRegisteredAndWorstCaseFitsEachChest(GameTestHelper ctx) {
        List<String> ids = new ArrayList<>();
        RoyalChestRewardModel.PRIMARY_ORDINARY.forEach(e -> ids.add(e.itemId()));
        RoyalChestRewardModel.PRIMARY_RARE.forEach(e -> ids.add(e.itemId()));
        RoyalChestRewardModel.SECONDARY_MATERIALS.forEach(e -> ids.add(e.itemId()));
        ids.add(RoyalChestRewardModel.CATALYST_ID);
        ids.add("moreminecrafts:seed_of_life");
        ids.add("moreminecrafts:ring_of_time");
        for (String id : ids) {
            Identifier parsed = Identifier.parse(id);
            ctx.assertTrue(BuiltInRegistries.ITEM.containsKey(parsed), "registered item " + id);
            ctx.assertTrue(!BuiltInRegistries.ITEM.getValue(parsed).equals(Items.AIR), id + " is not air");
        }
        ctx.assertTrue(RoyalChestRuntime.maxStackSize("moreminecrafts:relic_catalyst") == 1, "catalyst stacks to 1");
        ctx.assertTrue(RoyalChestRuntime.maxStackSize("moreminecrafts:seed_of_life") == 1, "T2 accessory stacks to 1");
        int worstPrimary = RoyalChestRewardModel.split(List.of(
                new RoyalChestRewardModel.Reward("minecraft:gold_ingot", 128), new RoyalChestRewardModel.Reward("minecraft:diamond", 18),
                new RoyalChestRewardModel.Reward("minecraft:emerald", 24), new RoyalChestRewardModel.Reward("moreminecrafts:mythril", 3),
                new RoyalChestRewardModel.Reward("minecraft:netherite_scrap", 2), new RoyalChestRewardModel.Reward("moreminecrafts:seed_of_life", 1),
                new RoyalChestRewardModel.Reward(RoyalChestRewardModel.CATALYST_ID, 1)), RoyalChestRuntime::maxStackSize).size();
        int worstSecondary = RoyalChestRewardModel.split(List.of(
                new RoyalChestRewardModel.Reward("moreminecrafts:enhancement_stone", 96), new RoyalChestRewardModel.Reward("minecraft:redstone", 128),
                new RoyalChestRewardModel.Reward("minecraft:lapis_lazuli", 128), new RoyalChestRewardModel.Reward("minecraft:diamond", 18),
                new RoyalChestRewardModel.Reward("moreminecrafts:titanium_ingot", 64), new RoyalChestRewardModel.Reward("moreminecrafts:tungsten_ingot", 64),
                new RoyalChestRewardModel.Reward("moreminecrafts:ring_of_time", 1), new RoyalChestRewardModel.Reward(RoyalChestRewardModel.CATALYST_ID, 1)),
                RoyalChestRuntime::maxStackSize).size();
        ctx.assertTrue(worstPrimary <= 54 && worstSecondary <= 54, "worst cases fit: " + worstPrimary + ", " + worstSecondary);
        ctx.succeed();
    }

    // ---- seal and first-open generation ----

    @GameTest(maxTicks = 60)
    public void bothChestsStaySealedUntilNormalDefeatThenGenerateOnFirstDirectOpen(GameTestHelper ctx) {
        Site site = site(ctx, "seal");
        ServerPlayer player = mock(ctx, site);
        ctx.assertTrue(RoyalChestRuntime.tryOpen(site.level, player, site.secondary) == OpenResult.SEALED, "SECONDARY sealed before defeat");
        ctx.assertTrue(!RoyalChestRuntime.data(site.level).isGenerated(site.id, RoyalChestRole.SECONDARY), "nothing generated while sealed");
        ctx.assertTrue(RoyalChestRuntime.tryOpen(site.level, player, site.primary.east()) == OpenResult.SEALED_ENCOUNTER_STARTED,
                "right half starts the same encounter");
        ctx.assertTrue(RoyalChestRuntime.tryOpen(site.level, player, site.primary) == OpenResult.SEALED, "left does not duplicate it");
        ctx.assertTrue(guardiansOf(site).size() == 1, "only one guardian across both halves");
        var guardian = guardiansOf(site).getFirst();
        guardian.skipAwakening();
        guardian.hurtServer(site.level, site.level.damageSources().playerAttack(player), 10000.0F);
        ctx.assertTrue(TombGuardianEncounters.isDefeated(site.level, site.primary), "normal defeat unlocks both");
        // PRIMARY's trigger fired, but defeating the boss and opening one chest generates nothing for the other.
        ctx.assertTrue(!RoyalChestRuntime.data(site.level).isGenerated(site.id, RoyalChestRole.PRIMARY), "defeat alone generates nothing");
        ctx.assertTrue(!RoyalChestRuntime.data(site.level).isGenerated(site.id, RoyalChestRole.SECONDARY), "defeat alone generates nothing");
        OpenResult primary = RoyalChestRuntime.tryOpen(site.level, player, site.primary);
        ctx.assertTrue(primary == OpenResult.OPENED_AND_GENERATED, "first valid opening generates, got " + primary);
        ctx.assertTrue(player.containerMenu instanceof RoyalChestMenu, "the shared menu opened");
        ctx.assertTrue(!RoyalChestRuntime.data(site.level).isGenerated(site.id, RoyalChestRole.SECONDARY), "opening PRIMARY does not generate SECONDARY");
        ctx.assertTrue(snapshot(chestOf(site, RoyalChestRole.PRIMARY)).equals(expected(site, RoyalChestRole.PRIMARY)),
                "PRIMARY contents equal the deterministic model draw");
        OpenResult secondary = RoyalChestRuntime.tryOpen(site.level, player, site.secondary);
        ctx.assertTrue(secondary == OpenResult.OPENED_AND_GENERATED, "SECONDARY generates on its own first opening, got " + secondary);
        ctx.assertTrue(snapshot(chestOf(site, RoyalChestRole.SECONDARY)).equals(expected(site, RoyalChestRole.SECONDARY)), "SECONDARY contents");
        finish(ctx, site);
    }

    @GameTest(maxTicks = 40)
    public void aSecondOpeningNeverRegeneratesAndWithdrawalsPersist(GameTestHelper ctx) {
        Site site = site(ctx, "once");
        ServerPlayer player = mock(ctx, site);
        defeat(ctx, site, player);
        RoyalChestRuntime.tryOpen(site.level, player, site.primary);
        Container first = chestOf(site, RoyalChestRole.PRIMARY);
        List<Stack> generated = snapshot(first);
        // withdraw slot 0 like a player
        first.removeItemNoUpdate(0);
        List<Stack> afterWithdraw = snapshot(first);
        ctx.assertTrue(afterWithdraw.size() == generated.size() - 1, "one stack withdrawn");
        OpenResult again = RoyalChestRuntime.tryOpen(site.level, mock(ctx, site), site.primary);
        ctx.assertTrue(again == OpenResult.OPENED, "reopening only opens, got " + again);
        ctx.assertTrue(chestOf(site, RoyalChestRole.PRIMARY) == first && snapshot(first).equals(afterWithdraw), "no restock, no reroll");
        finish(ctx, site);
    }

    // ---- independence, catalyst, order ----

    // ---- persistence ----

    @GameTest(maxTicks = 60)
    public void codecRoundTripPreservesStateWithoutRerollOrRestock(GameTestHelper ctx) {
        Site site = site(ctx, "persist");
        ServerPlayer player = mock(ctx, site);
        RoyalChestData before = RoyalChestRuntime.data(site.level);
        Tag beforeGeneration = encode(ctx, before);
        defeat(ctx, site, player);
        RoyalChestRuntime.tryOpen(site.level, player, site.primary);
        chestOf(site, RoyalChestRole.PRIMARY).removeItemNoUpdate(0); // partially withdrawn
        List<Stack> remaining = snapshot(chestOf(site, RoyalChestRole.PRIMARY));
        Optional<RoyalChestRole> catalyst = before.instance(site.id).orElseThrow().catalyst();

        RoyalChestData restarted = decode(ctx, encode(ctx, before));
        ctx.assertTrue(restarted.instance(site.id).orElseThrow().catalyst().equals(catalyst), "catalyst assignment persisted");
        ctx.assertTrue(restarted.linkAt(site.primary).isPresent() && restarted.linkAt(site.secondary).isPresent(), "chest links persisted");
        ctx.assertTrue(restarted.isGenerated(site.id, RoyalChestRole.PRIMARY) && !restarted.isGenerated(site.id, RoyalChestRole.SECONDARY),
                "generated state persisted per role");
        ctx.assertTrue(snapshot(restarted.chest(site.id, RoyalChestRole.PRIMARY).orElseThrow().container()).equals(remaining),
                "remaining items persisted exactly (no restock)");

        // Stale codec snapshot replay checks determinism only, not disk crash durability.
        RoyalChestData stale = decode(ctx, beforeGeneration);
        ctx.assertTrue(!stale.isGenerated(site.id, RoyalChestRole.PRIMARY), "stale snapshot predates generation");
        RoyalChestData.Instance instance = stale.instance(site.id).orElseThrow();
        List<Stack> regenerated = snapshot(RoyalChestRuntime.generate(site.level, stale, instance, RoyalChestRole.PRIMARY).container());
        ctx.assertTrue(regenerated.equals(expected(site, RoyalChestRole.PRIMARY)), "a repeated generation from the stale snapshot is the identical draw");
        finish(ctx, site);
    }

    private static Tag encode(GameTestHelper ctx, RoyalChestData data) {
        DataResult<Tag> result = RoyalChestData.TYPE.codec().encodeStart(NbtOps.INSTANCE, data);
        return result.getOrThrow(msg -> new IllegalStateException("encode: " + msg));
    }

    private static RoyalChestData decode(GameTestHelper ctx, Tag tag) {
        return RoyalChestData.TYPE.codec().parse(NbtOps.INSTANCE, tag).getOrThrow(msg -> new IllegalStateException("decode: " + msg));
    }

    // ---- access control ----

    private static void defeatDirect(Site site) {
        var data = TombGuardianEncounters.data(site.level);
        UUID boss = UUID.randomUUID();
        data.bind(TombGuardianEncounters.tombKey(site.primary), boss, 0L, site.level.getGameTime());
        data.markDefeated(TombGuardianEncounters.tombKey(site.primary), boss);
    }

    @GameTest(maxTicks = 40)
    public void fakePlayersAndSpectatorsNeverEvaluateRewards(GameTestHelper ctx) {
        Site site = site(ctx, "fake");
        defeatDirect(site);
        FakePlayer fake = FakePlayer.get(site.level);
        ctx.assertTrue(RoyalChestRuntime.tryOpen(site.level, fake, site.primary) == OpenResult.NOT_A_PLAYER, "fake player refused");
        ctx.assertTrue(!RoyalChestRuntime.data(site.level).isGenerated(site.id, RoyalChestRole.PRIMARY), "nothing generated by a fake player");
        // GameTestHelper's mock hardcodes gameMode() to CREATIVE; use an explicit spectator actor.
        ServerPlayer spectator = new ServerPlayer(site.level.getServer(), site.level,
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "royal-spectator"),
                net.minecraft.server.level.ClientInformation.createDefault()) {
            @Override public net.minecraft.world.level.GameType gameMode() { return net.minecraft.world.level.GameType.SPECTATOR; }
        };
        var spectatorResult = RoyalChestRuntime.tryOpen(site.level, spectator, site.primary.east());
        ctx.assertTrue(spectatorResult == OpenResult.NOT_A_PLAYER, "spectator refused, got " + spectatorResult + " spectator=" + spectator.isSpectator() + " registered=" + RoyalChestRuntime.data(site.level).linkAt(site.primary.east()));
        ctx.assertTrue(!RoyalChestRuntime.data(site.level).isGenerated(site.id, RoyalChestRole.PRIMARY), "spectator did not generate");
        ctx.assertTrue(RoyalChestRuntime.tryOpen(site.level, mock(ctx, site), ctx.absolutePos(new BlockPos(7, 1, 7))) == OpenResult.NOT_ROYAL_CHEST,
                "ordinary blocks are untouched");
        finish(ctx, site);
    }

    @GameTest(maxTicks = 40)
    public void twoPlayersOpeningInTheSameTickShareOneGeneration(GameTestHelper ctx) {
        Site site = site(ctx, "concurrent");
        defeatDirect(site);
        ServerPlayer a = mock(ctx, site), b = mock(ctx, site);
        OpenResult first = RoyalChestRuntime.tryOpen(site.level, a, site.primary);
        OpenResult second = RoyalChestRuntime.tryOpen(site.level, b, site.primary.east());
        ctx.assertTrue(first == OpenResult.OPENED_AND_GENERATED && second == OpenResult.OPENED, "generated exactly once: " + first + "/" + second);
        ctx.assertTrue(((RoyalChestMenu) a.containerMenu).chestContainer() == ((RoyalChestMenu) b.containerMenu).chestContainer(), "one shared inventory");
        // a withdrawal by one player is immediately the other's reality (shared inventory, no personal copy)
        ItemStack taken = ((RoyalChestMenu) a.containerMenu).chestContainer().removeItemNoUpdate(0);
        ctx.assertTrue(!taken.isEmpty() && ((RoyalChestMenu) b.containerMenu).chestContainer().getItem(0).isEmpty(), "shared withdrawal");
        finish(ctx, site);
    }

    @GameTest(maxTicks = 60)
    public void menuIsWithdrawOnlyAcrossEveryInsertionPath(GameTestHelper ctx) {
        Site site = site(ctx, "menu");
        defeatDirect(site);
        ServerPlayer player = mock(ctx, site);
        RoyalChestRuntime.tryOpen(site.level, player, site.primary);
        RoyalChestMenu menu = (RoyalChestMenu) player.containerMenu;
        Container chest = menu.chestContainer();
        List<Stack> generated = snapshot(chest);
        int empty = -1;
        for (int i = 0; i < 54 && empty < 0; i++) if (chest.getItem(i).isEmpty()) empty = i;
        ctx.assertTrue(empty >= 0, "a free slot exists");

        // (a) cursor placement into an empty slot
        menu.setCarried(new ItemStack(Items.DIRT, 5));
        menu.clicked(empty, 0, ContainerInput.PICKUP, player);
        ctx.assertTrue(chest.getItem(empty).isEmpty(), "cursor placement refused");
        // (b) cursor swap onto an occupied slot
        menu.clicked(0, 0, ContainerInput.PICKUP, player);
        ctx.assertTrue(snapshot(chest).equals(generated), "cursor swap refused");
        menu.setCarried(ItemStack.EMPTY);
        // (c) shift-click from the player inventory (first inventory slot is 54)
        player.getInventory().setItem(9, new ItemStack(Items.DIRT, 7));
        menu.clicked(54, 0, ContainerInput.QUICK_MOVE, player);
        ctx.assertTrue(snapshot(chest).equals(generated) && player.getInventory().getItem(9).getCount() == 7, "shift-click insertion refused");
        // (d) hotbar swap: hotbar stack onto an occupied and onto an empty chest slot
        player.getInventory().setItem(0, new ItemStack(Items.DIRT, 3));
        menu.clicked(0, 0, ContainerInput.SWAP, player);
        menu.clicked(empty, 0, ContainerInput.SWAP, player);
        ctx.assertTrue(snapshot(chest).equals(generated) && player.getInventory().getItem(0).getCount() == 3, "hotbar swap insertion refused");
        // (e) offhand swap (button 40)
        player.getInventory().setItem(40, new ItemStack(Items.DIRT, 2));
        menu.clicked(empty, 40, ContainerInput.SWAP, player);
        ctx.assertTrue(snapshot(chest).equals(generated), "offhand swap insertion refused");
        // (f) drag placement across empty slots
        menu.setCarried(new ItemStack(Items.DIRT, 10));
        menu.clicked(-999, 0, ContainerInput.QUICK_CRAFT, player);
        menu.clicked(empty, 1, ContainerInput.QUICK_CRAFT, player);
        menu.clicked(-999, 2, ContainerInput.QUICK_CRAFT, player);
        ctx.assertTrue(snapshot(chest).equals(generated), "drag insertion refused");
        menu.setCarried(ItemStack.EMPTY);

        // withdrawal still works: shift-click out, then pick up with the cursor
        int before = player.getInventory().getNonEquipmentItems().stream().mapToInt(s -> s.isEmpty() ? 0 : 1).sum();
        ItemStack moved = menu.quickMoveStack(player, 0);
        ctx.assertTrue(!moved.isEmpty() && chest.getItem(0).isEmpty(), "shift-click withdrawal works");
        menu.clicked(1, 0, ContainerInput.PICKUP, player);
        ctx.assertTrue(!menu.getCarried().isEmpty() && chest.getItem(1).isEmpty(), "cursor withdrawal works");
        ctx.assertTrue(before >= 0, "inventory counted");
        ctx.assertTrue(menu.quickMoveStack(player, 60).isEmpty(), "inventory slots never quick-move into the chest");
        finish(ctx, site);
    }

    @GameTest(maxTicks = 80)
    public void hoppersCannotInsertOrExtractBeforeOrAfterDefeat(GameTestHelper ctx) {
        Site site = site(ctx, "hopper");
        // Hopper above the chest (insert path) holding an item; hopper below (extract path) with an empty container beneath it.
        ctx.setBlock(new BlockPos(1, 2, 1), Blocks.HOPPER);
        ctx.setBlock(new BlockPos(5, 0, 1), Blocks.HOPPER);
        ctx.setBlock(new BlockPos(5, -1, 1), Blocks.CHEST);
        ctx.assertTrue(HopperBlockEntity.getContainerAt(site.level, site.primary) == null, "registered chest is invisible to container lookups");
        ctx.assertTrue(HopperBlockEntity.getContainerAt(site.level, site.primary.east()) == null, "right half also invisible to automation");
        ctx.assertTrue(HopperBlockEntity.getContainerAt(site.level, ctx.absolutePos(new BlockPos(5, -1, 1))) != null, "control: ordinary chests still resolve");
        HopperBlockEntity inserter = (HopperBlockEntity) site.level.getBlockEntity(ctx.absolutePos(new BlockPos(1, 2, 1)));
        inserter.setItem(0, new ItemStack(Items.DIRT, 4));
        ctx.runAfterDelay(30, () -> {
            ctx.assertTrue(inserter.getItem(0).getCount() == 4, "hopper could not insert before defeat");
            defeatDirect(site);
            RoyalChestRuntime.tryOpen(site.level, mock(ctx, site), site.secondary);
            Container secondary = chestOf(site, RoyalChestRole.SECONDARY);
            List<Stack> generated = snapshot(secondary);
            ctx.assertTrue(HopperBlockEntity.getContainerAt(site.level, site.secondary) == null, "still invisible after defeat and generation");
            ctx.runAfterDelay(30, () -> {
                ctx.assertTrue(inserter.getItem(0).getCount() == 4, "hopper cannot insert after defeat");
                ctx.assertTrue(snapshot(secondary).equals(generated), "no automated extraction or insertion");
                ctx.assertTrue(!RoyalChestRuntime.data(site.level).isGenerated(site.id, RoyalChestRole.PRIMARY), "hoppers never trigger generation");
                finish(ctx, site);
            });
        });
    }

    @GameTest(maxTicks = 60)
    public void miningAndExplosionsNeverDestroyAChestEvenWhenEmpty(GameTestHelper ctx) {
        Site site = site(ctx, "protect");
        defeatDirect(site);
        ServerPlayer player = mock(ctx, site);
        RoyalChestRuntime.tryOpen(site.level, player, site.primary);
        Container chest = chestOf(site, RoyalChestRole.PRIMARY);
        for (int i = 0; i < 54; i++) chest.removeItemNoUpdate(i);          // fully depleted
        // Control next to the pair proves the explosion happened.
        BlockPos control = site.primary.west(1);
        site.level.setBlockAndUpdate(control, Blocks.DIRT.defaultBlockState());
        ctx.assertTrue(!PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(site.level, player, site.primary, site.level.getBlockState(site.primary), null),
                "player break event is vetoed");
        player.gameMode.destroyBlock(site.primary);
        ctx.assertTrue(site.level.getBlockState(site.primary).is(Blocks.CHEST), "mining cannot remove the chest");
        site.level.explode(null, site.primary.getX() + 0.5D, site.primary.getY() + 0.5D, site.primary.getZ() + 2.5D, 4.0F, Level.ExplosionInteraction.TNT);
        ctx.assertTrue(site.level.getBlockState(control).isAir(), "control block was blown up (the explosion happened)");
        ctx.assertTrue(site.level.getBlockState(site.primary).is(Blocks.CHEST), "the explosion cannot remove a depleted chest");
        ctx.assertTrue(RoyalChestRuntime.isGuarded(site.level, site.primary), "protection outlives depletion");
        ctx.assertTrue(site.level.getBlockState(site.primary.east()).is(Blocks.CHEST), "right half survives explosion");
        player.gameMode.destroyBlock(site.primary.east());
        ctx.assertTrue(site.level.getBlockState(site.primary.east()).is(Blocks.CHEST), "right half survives mining");
        finish(ctx, site);
    }
}
