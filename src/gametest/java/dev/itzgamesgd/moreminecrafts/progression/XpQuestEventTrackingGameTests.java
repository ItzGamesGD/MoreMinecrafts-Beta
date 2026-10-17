package dev.itzgamesgd.moreminecrafts.progression;

import dev.itzgamesgd.moreminecrafts.fishing.FishSpecies;
import dev.itzgamesgd.moreminecrafts.registry.ModDataComponents;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/** Q1-B: live evidence hooks, ore provenance and per-quest registration checks. */
public final class XpQuestEventTrackingGameTests implements CustomTestMethodInvoker {
    private static ResearchProgressState state(ServerPlayer player) { return KnowledgeRuntime.state(player); }

    private static ServerPlayer survival(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    private static XpQuestCatalogData.Spec spec(String id) {
        return XpQuestCatalogData.catalog().quests().stream().filter(q -> q.id().equals(id)).findFirst().orElseThrow();
    }

    private static int counter(ServerPlayer player, String key) { return QuestRules.counter(state(player), key); }

    // ---------------------------------------------------------------- B1 placement

    @GameTest
    public void confirmedPlacementsCountOnEveryPathAndRefusedOnesDoNot(GameTestHelper ctx) {
        ServerPlayer player = survival(ctx);
        int before = counter(player, XpQuestHooks.BLOCKS_PLACED);
        BlockPos floor = new BlockPos(1, 1, 1);
        ctx.setBlock(floor, Blocks.STONE);
        ctx.placeAt(player, new ItemStack(Items.OAK_PLANKS), floor, Direction.UP);
        ctx.placeAt(player, new ItemStack(Items.OAK_PLANKS), floor.above(), Direction.UP);
        ctx.assertBlockPresent(Blocks.OAK_PLANKS, floor.above(2));
        ctx.assertTrue(counter(player, XpQuestHooks.BLOCKS_PLACED) == before + 2, "repeated block placements both count");

        // Door: one placement action writes two blocks.
        ctx.setBlock(new BlockPos(3, 1, 1), Blocks.STONE);
        ctx.placeAt(player, new ItemStack(Items.OAK_DOOR), new BlockPos(3, 1, 1), Direction.UP);
        ctx.assertBlockPresent(Blocks.OAK_DOOR, new BlockPos(3, 2, 1));
        ctx.assertTrue(counter(player, XpQuestHooks.BLOCKS_PLACED) == before + 3, "a door is one placement");

        // Lily pad: PlaceOnWaterBlockItem.use bypasses ItemStack.useOn and therefore ITEM_USED.
        BlockPos water = ctx.absolutePos(new BlockPos(5, 1, 1));
        ctx.getLevel().setBlock(water, Blocks.WATER.defaultBlockState(), 3);
        player.setPos(water.getX() + 0.5D, water.getY() + 1.6D, water.getZ() + 0.5D);
        player.setXRot(90.0F);
        player.setYRot(0.0F);
        ItemStack lily = new ItemStack(Items.LILY_PAD);
        player.setItemInHand(InteractionHand.MAIN_HAND, lily);
        int lilyStatBefore = player.getStats().getValue(Stats.ITEM_USED.get(Items.LILY_PAD));
        player.getItemInHand(InteractionHand.MAIN_HAND).use(ctx.getLevel(), player, InteractionHand.MAIN_HAND);
        ctx.assertTrue(ctx.getLevel().getBlockState(water.above()).is(Blocks.LILY_PAD), "lily pad placed on water");
        ctx.assertTrue(player.getStats().getValue(Stats.ITEM_USED.get(Items.LILY_PAD)) == lilyStatBefore,
                "vanilla ITEM_USED misses the lily pad (the reason for the live hook)");
        ctx.assertTrue(counter(player, XpQuestHooks.BLOCKS_PLACED) == before + 4, "live hook counts the lily pad");

        // Refused: Adventure mode cannot place, so BlockItem.place never succeeds.
        player.setGameMode(GameType.ADVENTURE);
        ctx.placeAt(player, new ItemStack(Items.OAK_PLANKS), floor.above(2), Direction.UP);
        ctx.assertBlockNotPresent(Blocks.OAK_PLANKS, floor.above(3));
        // Failed: target position occupied by a block that cannot be replaced.
        player.setGameMode(GameType.SURVIVAL);
        ctx.setBlock(new BlockPos(7, 2, 1), Blocks.STONE);
        ctx.setBlock(new BlockPos(7, 1, 1), Blocks.STONE);
        ctx.useBlock(new BlockPos(7, 1, 1), player);
        ctx.assertTrue(counter(player, XpQuestHooks.BLOCKS_PLACED) == before + 4, "refused or failed placement adds nothing");
        ctx.assertTrue(QuestRules.distinctCount(state(player), XpQuestHooks.PLACED_BLOCK_SET) >= 3,
                "distinct placed blocks recorded (planks, door, lily pad)");
        ctx.succeed();
    }

    @GameTest
    public void placementHistoryIsSeededOnceFromVanillaStatsWithoutDoubleCounting(GameTestHelper ctx) {
        XpQuestTracking.register(spec("xpq.bld.place_1000"));
        ServerPlayer player = survival(ctx);
        // Simulate a pre-update player: history in vanilla stats, no baseline yet.
        player.awardStat(Stats.ITEM_USED.get(Items.STONE), 600);
        player.awardStat(Stats.ITEM_USED.get(Items.DIRT), 50);
        ResearchProgressState s = state(player);
        var stripped = new java.util.HashMap<>(s.counters());
        stripped.keySet().removeIf(k -> k.startsWith("quest.baseline.") || k.startsWith("quest.seen.set.blocks_placed."));
        KnowledgeRuntime.updateState(player, new ResearchProgressState(stripped));
        XpQuestHooks.captureBaselines(player);
        ctx.assertTrue(state(player).get(XpQuestHooks.baselineKey(XpQuestHooks.BLOCK_ITEMS_USED_BASELINE)) == 650,
                "baseline = provable vanilla history");
        BlockPos floor = new BlockPos(1, 1, 1);
        ctx.setBlock(floor, Blocks.STONE);
        ctx.placeAt(player, new ItemStack(Items.STONE), floor, Direction.UP); // also raises ITEM_USED to 601
        XpQuestHooks.captureBaselines(player);
        XpQuestTracking.reconcile(player);
        ctx.assertTrue(state(player).questProgress("xpq.bld.place_1000") == 651,
                "651 = 650 history + 1 live, not 652: " + state(player).questProgress("xpq.bld.place_1000"));
        ctx.succeed();
    }

    // ---------------------------------------------------------------- B2 ore provenance

    private static final ChunkPos NATURAL_ORE_CHUNK = new ChunkPos(96, 96);

    @GameTest(maxTicks = 200)
    public void onlyVerifiedNaturalOreCounts(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        List<BlockPos> ores = QuestOreFixture.decorateWithNaturalOres(level, NATURAL_ORE_CHUNK);
        ctx.assertTrue(ores.size() >= 6, "real overworld ore features produced ores: " + ores.size());
        for (BlockPos ore : ores)
            ctx.assertTrue(QuestOreProvenance.verdict(level, ore) == QuestOreProvenance.Verdict.NATURAL,
                    "ore written by natural decoration is natural at " + ore);
        ServerPlayer miner = survival(ctx);
        ServerPlayer other = survival(ctx);
        BlockPos natural = ores.get(0);
        String id = BuiltInRegistries.BLOCK.getKey(level.getBlockState(natural).getBlock()).toString();
        ctx.assertTrue(miner.gameMode.destroyBlock(natural), "mined natural ore");
        ctx.assertTrue(counter(miner, "natural_ore_mined") == 1 && counter(miner, "natural_mined." + id) == 1
                && state(miner).get(QuestRules.seenKey(XpQuestHooks.NATURAL_ORE_SET, id)) == 1, "natural ore counted");
        ctx.assertTrue(counter(other, "natural_ore_mined") == 0, "credited to the miner only");

        // Replaced: a player puts an ore back where a natural one was.
        level.setBlock(natural, Blocks.DIAMOND_ORE.defaultBlockState(), 3);
        ctx.assertTrue(QuestOreProvenance.verdict(level, natural) == QuestOreProvenance.Verdict.NON_NATURAL, "replaced ore is not natural");
        ctx.assertTrue(miner.gameMode.destroyBlock(natural) && counter(miner, "natural_ore_mined") == 1, "replaced ore does not count");

        // A state-only update on the same generated ore preserves positive evidence.
        BlockPos second = ores.get(1);
        level.setBlock(second, level.getBlockState(second), 3);
        ctx.assertTrue(QuestOreProvenance.verdict(level, second) == QuestOreProvenance.Verdict.NATURAL,
                "same-block state update keeps the natural marker");

        ctx.succeed();
    }

    @GameTest(maxTicks = 40)
    public void pistonMovedNaturalOreDoesNotCount(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos ore = ctx.absolutePos(new BlockPos(2, 3, 2));
        level.setBlock(ore, Blocks.IRON_ORE.defaultBlockState(), 3);
        // The natural-feature-write hook is exercised by the separate real decoration test.
        // Here a positive source marker isolates the real piston movement behavior.
        K5NaturalTreeProvenance.enterNaturalBiomeDecoration(level.registryAccess());
        try {
            QuestOreProvenance.recordGenerated(level, ore, level.getBlockState(ore));
        } finally {
            K5NaturalTreeProvenance.exitNaturalBiomeDecoration();
        }
        ctx.assertTrue(QuestOreProvenance.verdict(level, ore) == QuestOreProvenance.Verdict.NATURAL, "natural before the move");
        BlockState movedState = level.getBlockState(ore);
        for (int i = 1; i <= 12; i++) level.setBlock(ore.above(i), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(ore.below(), Blocks.PISTON.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.UP), 3);
        level.setBlock(ore.below(2), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        ServerPlayer miner = survival(ctx);
        BlockPos moved = ore.above();
        ctx.succeedWhen(() -> {
            ctx.assertTrue(level.getBlockState(moved).is(movedState.getBlock()), "piston pushed the ore up");
            ctx.assertTrue(QuestOreProvenance.verdict(level, moved) != QuestOreProvenance.Verdict.NATURAL,
                    "piston-moved ore is not natural");
            ctx.assertTrue(miner.gameMode.destroyBlock(moved) && counter(miner, "natural_ore_mined") == 0,
                    "piston-moved ore does not count");
        });
    }

    @GameTest(maxTicks = 60)
    public void crossChunkPistonArrivalDoesNotInheritNaturalOrigin(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos local = ctx.absolutePos(new BlockPos(2, 3, 2));
        BlockPos ore = new BlockPos((local.getX() & ~15) + 15, local.getY(), local.getZ());
        BlockPos destination = ore.east();
        level.setBlock(ore, Blocks.IRON_ORE.defaultBlockState(), 3);
        K5NaturalTreeProvenance.enterNaturalBiomeDecoration(level.registryAccess());
        try {
            QuestOreProvenance.recordGenerated(level, ore, level.getBlockState(ore));
        } finally {
            K5NaturalTreeProvenance.exitNaturalBiomeDecoration();
        }
        for (int i = 1; i <= 12; i++) level.setBlock(ore.east(i), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(ore.west(), Blocks.PISTON.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.EAST), 3);
        level.setBlock(ore.west(2), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        ServerPlayer miner = survival(ctx);
        ctx.succeedWhen(() -> {
            ctx.assertTrue(level.getBlockState(destination).is(Blocks.IRON_ORE), "ore crossed the chunk boundary");
            ctx.assertTrue(QuestOreProvenance.verdict(level, destination) != QuestOreProvenance.Verdict.NATURAL,
                    "destination has no natural marker");
            ctx.assertTrue(QuestOreProvenance.verdict(level, ore) != QuestOreProvenance.Verdict.NATURAL,
                    "source marker was consumed");
            ctx.assertTrue(miner.gameMode.destroyBlock(destination) && counter(miner, "natural_ore_mined") == 0,
                    "cross-chunk piston ore never counts");
        });
    }

    @GameTest
    public void nestedAndCancelledBreakFramesDoNotLoseOrInventOreCredit(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        ServerPlayer miner = survival(ctx);
        BlockPos outer = ctx.absolutePos(new BlockPos(1, 2, 1));
        BlockPos inner = ctx.absolutePos(new BlockPos(2, 2, 1));
        BlockPos cancelled = ctx.absolutePos(new BlockPos(3, 2, 1));
        for (BlockPos pos : List.of(outer, inner, cancelled))
            level.setBlock(pos, Blocks.IRON_ORE.defaultBlockState(), 3);
        K5NaturalTreeProvenance.enterNaturalBiomeDecoration(level.registryAccess());
        try {
            for (BlockPos pos : List.of(outer, inner, cancelled))
                QuestOreProvenance.recordGenerated(level, pos, level.getBlockState(pos));
        } finally {
            K5NaturalTreeProvenance.exitNaturalBiomeDecoration();
        }
        BlockState state = Blocks.IRON_ORE.defaultBlockState();
        ctx.assertTrue(PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, miner, outer, state, null),
                "outer break allowed");
        ctx.assertTrue(PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, miner, inner, state, null),
                "nested break allowed");
        level.setBlock(inner, Blocks.AIR.defaultBlockState(), 3);
        PlayerBlockBreakEvents.AFTER.invoker().afterBlockBreak(level, miner, inner, state, null);
        level.setBlock(outer, Blocks.AIR.defaultBlockState(), 3);
        PlayerBlockBreakEvents.AFTER.invoker().afterBlockBreak(level, miner, outer, state, null);
        ctx.assertTrue(counter(miner, "natural_ore_mined") == 2, "both nested breaks credited once");
        PlayerBlockBreakEvents.AFTER.invoker().afterBlockBreak(level, miner, outer, state, null);
        PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, miner, cancelled, state, null);
        PlayerBlockBreakEvents.CANCELED.invoker().onBlockBreakCanceled(level, miner, cancelled, state, null);
        PlayerBlockBreakEvents.AFTER.invoker().afterBlockBreak(level, miner, cancelled, state, null);
        ctx.assertTrue(counter(miner, "natural_ore_mined") == 2, "repeated and cancelled callbacks add nothing");
        ctx.succeed();
    }

    @GameTest
    public void explodedOreCreditsNobodyAndSetblockOreIsNotNatural(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        ServerPlayer bystander = survival(ctx);
        BlockPos natural = QuestOreFixture.decorateWithNaturalOres(level, new ChunkPos(96, 98)).get(0);
        ctx.assertTrue(QuestOreProvenance.verdict(level, natural) == QuestOreProvenance.Verdict.NATURAL, "natural before the blast");
        level.explode(null, natural.getX() + 0.5D, natural.getY() + 0.5D, natural.getZ() + 0.5D, 3.0F, Level.ExplosionInteraction.TNT);
        ctx.assertTrue(!QuestOreProvenance.isTrackedOre(level.getBlockState(natural)), "explosion removed the ore");
        ctx.assertTrue(counter(bystander, "natural_ore_mined") == 0, "an explosion is not player mining");
        // /setblock and every other Level.setBlock writer: non-natural.
        BlockPos commanded = ctx.absolutePos(new BlockPos(5, 2, 5));
        level.setBlock(commanded, Blocks.EMERALD_ORE.defaultBlockState(), 3);
        ctx.assertTrue(QuestOreProvenance.verdict(level, commanded) != QuestOreProvenance.Verdict.NATURAL, "setblock ore is not natural");
        ctx.assertTrue(bystander.gameMode.destroyBlock(commanded) && counter(bystander, "natural_ore_mined") == 0,
                "setblock ore does not count");
        ctx.succeed();
    }

    @GameTest
    public void playerPlacedOreNeverCounts(GameTestHelper ctx) {
        ServerPlayer player = survival(ctx);
        BlockPos floor = new BlockPos(1, 1, 1);
        ctx.setBlock(floor, Blocks.STONE);
        ctx.placeAt(player, new ItemStack(Items.DIAMOND_ORE), floor, Direction.UP);
        BlockPos placed = ctx.absolutePos(floor.above());
        ctx.assertTrue(ctx.getLevel().getBlockState(placed).is(Blocks.DIAMOND_ORE), "ore placed");
        ctx.assertTrue(QuestOreProvenance.verdict(ctx.getLevel(), placed) != QuestOreProvenance.Verdict.NATURAL,
                "player-placed ore marked non-natural");
        ctx.assertTrue(player.gameMode.destroyBlock(placed) && counter(player, "natural_ore_mined") == 0,
                "silk-touch style re-mining gives nothing");
        ctx.succeed();
    }

    @GameTest
    public void unknownOriginChunkNeverCountsAndGenerationMarksTracked(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        var generated = QuestOreFixture.decorateWithNaturalOres(level, new ChunkPos(120, 120));
        ctx.assertTrue(!generated.isEmpty()
                && QuestOreProvenance.verdict(level, generated.get(0)) == QuestOreProvenance.Verdict.NATURAL,
                "a generated ore has positive evidence");
        // Pre-update chunk: no attachment. An ore present there (raw write, as if generated earlier) is unknown.
        var old = level.getChunk(124, 124);
        old.removeAttached(QuestOreProvenance.Storage.ATTACHMENT);
        BlockPos ore = new BlockPos(124 * 16 + 3, 10, 124 * 16 + 3);
        old.setBlockState(ore, Blocks.DIAMOND_ORE.defaultBlockState(), 0);
        ctx.assertTrue(QuestOreProvenance.verdict(level, ore) == QuestOreProvenance.Verdict.UNKNOWN, "old-world ore is unknown");
        ServerPlayer player = survival(ctx);
        ctx.assertTrue(player.gameMode.destroyBlock(ore) && counter(player, "natural_ore_mined") == 0,
                "unknown origin is never treated as natural");
        ctx.succeed();
    }

    @GameTest
    public void oreProvenanceCodecRoundTrips(GameTestHelper ctx) {
        var data = new QuestOreProvenance.ChunkData(QuestOreProvenance.SCHEMA_VERSION, Set.of(1L, -5L, 123456789L));
        var encoded = QuestOreProvenance.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, data).getOrThrow();
        var decoded = QuestOreProvenance.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, encoded).getOrThrow();
        ctx.assertTrue(decoded.equals(data), "in-process codec round trip (not a process restart)");
        ctx.succeed();
    }

    // ---------------------------------------------------------------- B3 Knowledge-backed counters

    @GameTest
    public void knowledgeCountersContinuePastTheCapWithOnceOnlyBaseline(GameTestHelper ctx) {
        XpQuestTracking.register(spec("xpq.frm.harvest_1000"));
        ServerPlayer player = survival(ctx);
        // Pre-update history: 80 harvests recorded by Knowledge, no baseline yet.
        var stripped = new java.util.HashMap<>(state(player).with("condition.crops_harvested", 80).counters());
        stripped.keySet().removeIf(k -> k.startsWith("quest.baseline."));
        KnowledgeRuntime.updateState(player, new ResearchProgressState(stripped));
        XpQuestHooks.captureBaselines(player);
        ctx.assertTrue(state(player).get(XpQuestHooks.baselineKey("crops_harvested")) == 80, "baseline from Knowledge history");
        // Collectors report "stored + 1" per event, even after the Knowledge cap.
        for (int i = 0; i < 919; i++)
            KnowledgeRuntime.recordCondition(player, "crops_harvested", state(player).conditionProgress("crops_harvested") + 1);
        ctx.assertTrue(state(player).conditionProgress("crops_harvested") == 100, "Knowledge counter still capped at 100");
        XpQuestHooks.captureBaselines(player);
        XpQuestTracking.reconcile(player);
        ctx.assertTrue(state(player).questProgress("xpq.frm.harvest_1000") == 999, "80 + 919 live = 999");
        KnowledgeRuntime.recordCondition(player, "crops_harvested", state(player).conditionProgress("crops_harvested") + 1);
        XpQuestTracking.reconcile(player);
        ctx.assertTrue(state(player).questComplete("xpq.frm.harvest_1000"), "completes at 1,000");
        ctx.succeed();
    }

    // ---------------------------------------------------------------- B3 taming, travel, fish sale

    @GameTest
    public void tamingFiveSpeciesThroughVanillaTriggers(GameTestHelper ctx) {
        XpQuestTracking.register(spec("xpq.frm.tame_5"));
        ServerPlayer player = survival(ctx);
        var level = ctx.getLevel();
        var wolf = EntityTypes.WOLF.create(level, EntitySpawnReason.COMMAND);
        var cat = EntityTypes.CAT.create(level, EntitySpawnReason.COMMAND);
        var parrot = EntityTypes.PARROT.create(level, EntitySpawnReason.COMMAND);
        var horse = EntityTypes.HORSE.create(level, EntitySpawnReason.COMMAND);
        var llama = EntityTypes.LLAMA.create(level, EntitySpawnReason.COMMAND);
        wolf.tame(player);
        cat.tame(player);
        parrot.tame(player);
        XpQuestTracking.reconcile(player);
        ctx.assertTrue(state(player).questProgress("xpq.frm.tame_5") == 3, "three TamableAnimal species");
        wolf.tame(player); // repeat: distinct set
        ctx.assertTrue(horse.tameWithName(player) && llama.tameWithName(player), "horse and llama tamed");
        XpQuestTracking.reconcile(player);
        ctx.assertTrue(state(player).questComplete("xpq.frm.tame_5"), "all five species");
        ServerPlayer other = survival(ctx);
        ctx.assertTrue(QuestRules.distinctCount(state(other), XpQuestHooks.TAMED_SET) == 0, "attributed to the tamer only");
        ctx.succeed();
    }

    @GameTest
    public void netherMovementStatisticsFeedOnlyTheNetherCounter(GameTestHelper ctx) {
        ServerPlayer player = survival(ctx);
        player.awardStat(Stats.WALK_ONE_CM, 5_000);
        ctx.assertTrue(counter(player, "nether_travel_cm") == 0, "overworld movement does not count");
        ServerLevel nether = ctx.getLevel().getServer().getLevel(Level.NETHER);
        ctx.assertTrue(nether != null, "nether level available");
        player.teleportTo(nether, 0.5D, 100.0D, 0.5D, Set.of(), 0.0F, 0.0F, false);
        ctx.assertTrue(player.level().dimension() == Level.NETHER, "player in the Nether");
        ctx.assertTrue(counter(player, "nether_travel_cm") == 0, "teleporting awards no movement statistic and adds nothing");
        player.awardStat(Stats.SPRINT_ONE_CM, 7_000);
        player.awardStat(Stats.JUMP, 3); // not a movement distance
        ctx.assertTrue(counter(player, "nether_travel_cm") == 7_000, "Nether movement counted in centimetres");
        ctx.succeed();
    }

    @GameTest
    public void fishermanSaleCountsFishAndEmeralds(GameTestHelper ctx) {
        var level = ctx.getLevel();
        BlockPos base = ctx.absolutePos(new BlockPos(1, 1, 1));
        var profession = level.registryAccess().lookupOrThrow(Registries.VILLAGER_PROFESSION).getOrThrow(VillagerProfession.FISHERMAN);
        Villager fisherman = EntityTypes.VILLAGER.create(level, EntitySpawnReason.COMMAND);
        fisherman.setVillagerData(fisherman.getVillagerData().withProfession(profession));
        fisherman.setNoAi(true);
        fisherman.setPos(base.getX() + 0.5D, base.getY(), base.getZ() + 0.5D);
        level.addFreshEntity(fisherman);
        ServerPlayer seller = survival(ctx);
        seller.setShiftKeyDown(true);
        ItemStack fish = new ItemStack(ModItems.fishItem(FishSpecies.BLUE_MARLIN));
        fish.set(ModDataComponents.FISH_GRADE, 5);
        fish.set(ModDataComponents.FISH_LENGTH_TENTHS_CM, FishSpecies.BLUE_MARLIN.lengthDistribution().maximum());
        fish.set(ModDataComponents.FISH_WEIGHT_TENTHS_KG, FishSpecies.BLUE_MARLIN.weightDistribution().maximum());
        seller.setItemInHand(InteractionHand.MAIN_HAND, fish);
        var hit = new EntityHitResult(fisherman, new Vec3(fisherman.getX(), fisherman.getEyeY(), fisherman.getZ()));
        var result = UseEntityCallback.EVENT.invoker().interact(seller, level, InteractionHand.MAIN_HAND, fisherman, hit);
        int emeralds = seller.getInventory().countItem(Items.EMERALD);
        ctx.assertTrue(result.consumesAction() && emeralds > 0, "sale accepted");
        ctx.assertTrue(counter(seller, "fish_sold") == 1 && counter(seller, "fish_sale_emeralds") == emeralds,
                "one fish and the exact emerald payment recorded");
        var refused = UseEntityCallback.EVENT.invoker().interact(seller, level, InteractionHand.MAIN_HAND, fisherman, hit);
        ctx.assertTrue(counter(seller, "fish_sold") == 1, "no fish in hand: no sale counted (" + refused + ")");
        ctx.succeed();
    }

    // ---------------------------------------------------------------- B4 per-quest registration

    /** Registered quests have valid production evidence keys and eligible NOW status. */
    @GameTest
    public void registeredQuestEvidenceKeysAreValid(GameTestHelper ctx) {
        List<XpQuestCatalogData.Spec> registered = XpQuestCatalogData.catalog().quests().stream()
                .filter(XpQuestCatalogData.Spec::registered).toList();
        ctx.assertTrue(registered.size() == 30, "30 source-verified quests registered for Q1-B: " + registered.size());
        for (XpQuestCatalogData.Spec spec : registered) {
            XpQuestCatalogData.Tracker t = spec.tracker();
            // A missing id would resolve to air and make an injected threshold test pass vacuously.
            if (t.kind().equals("stat_crafted_distinct"))
                for (String id : t.entries())
                    ctx.assertTrue(BuiltInRegistries.ITEM.containsKey(Identifier.parse(id)), spec.id() + " crafts a real item " + id);
            if (t.kind().equals("distinct_set") || t.kind().equals("distinct_groups"))
                for (String group : t.entries())
                    for (String id : group.split("\\|"))
                        ctx.assertTrue(BuiltInRegistries.BLOCK.containsKey(Identifier.parse(id))
                                || BuiltInRegistries.ENTITY_TYPE.containsKey(Identifier.parse(id)), spec.id() + " names a real id " + id);
            if (t.kind().equals("counter_sum") || (t.kind().equals("counter") && t.key().startsWith("natural_mined.")))
                for (String key : t.kind().equals("counter") ? List.of(t.key()) : t.entries())
                    ctx.assertTrue(BuiltInRegistries.BLOCK.containsKey(Identifier.parse(key.substring("natural_mined.".length())))
                            && QuestOreProvenance.isTrackedOre(BuiltInRegistries.BLOCK.getValue(
                                    Identifier.parse(key.substring("natural_mined.".length()))).defaultBlockState()),
                            spec.id() + " counts a tracked ore " + key);
        }
        ctx.succeed();
    }

    /** One threshold test per registered quest; source-event acceptance is tracked separately. */
    private void assertQuestThreshold(GameTestHelper ctx, String id) {
        XpQuestCatalogData.Spec spec = spec(id);
        if (!spec.registered()) {
            ctx.assertTrue(spec.status().equals("NOW"), spec.id() + " stays in the approved NOW catalog");
            ctx.succeed();
            return;
        }
        ctx.assertTrue(spec.status().equals("NOW") && QuestCatalog.byId(spec.id()) != null,
                spec.id() + " is NOW and registered at startup");
        ServerPlayer player = survival(ctx);
        List<XpQuestCatalogData.Spec> one = List.of(spec);
        KnowledgeRuntime.updateState(player, inject(player, spec, spec.target() - 1));
        ResearchProgressState partial = XpQuestTracking.reconcile(player, state(player), one);
        ctx.assertTrue(!partial.questComplete(spec.id()), spec.id() + " incomplete at target - 1");
        KnowledgeRuntime.updateState(player, inject(player, spec, spec.target()));
        ResearchProgressState full = XpQuestTracking.reconcile(player, state(player), one);
        ctx.assertTrue(full.questComplete(spec.id()), spec.id() + " complete at target");
        ServerPlayer other = survival(ctx);
        ResearchProgressState unrelated = XpQuestTracking.reconcile(other, state(other), one);
        ctx.assertTrue(!unrelated.questComplete(spec.id()) && unrelated.questProgress(spec.id()) == 0,
                spec.id() + " gives no progress to another player");
        ctx.succeed();
    }

    @GameTest
    public void threshold_exp_archaeology_30(GameTestHelper ctx) {
        assertQuestThreshold(ctx, "xpq.exp.archaeology_30");
    }

    @GameTest
    public void threshold_dim_nether_travel_50k(GameTestHelper ctx) {
        assertQuestThreshold(ctx, "xpq.dim.nether_travel_50k");
    }

    @GameTest
    public void threshold_cmb_shield_500(GameTestHelper ctx) {
        assertQuestThreshold(ctx, "xpq.cmb.shield_500");
    }

    @GameTest
    public void threshold_cmb_trident_50(GameTestHelper ctx) {
        assertQuestThreshold(ctx, "xpq.cmb.trident_50");
    }

    @GameTest
    public void threshold_min_first_custom_ore(GameTestHelper ctx) {
        assertQuestThreshold(ctx, "xpq.min.first_custom_ore");
    }

    @GameTest
    public void threshold_min_custom_ore_survey(GameTestHelper ctx) {
        assertQuestThreshold(ctx, "xpq.min.custom_ore_survey");
    }

    @GameTest
    public void threshold_min_ore_1000(GameTestHelper ctx) {
        assertQuestThreshold(ctx, "xpq.min.ore_1000");
    }

    @GameTest
    public void threshold_min_deepslate_custom(GameTestHelper ctx) {
        assertQuestThreshold(ctx, "xpq.min.deepslate_custom");
    }

    @GameTest
    public void threshold_min_logs_1000(GameTestHelper ctx) {
        assertQuestThreshold(ctx, "xpq.min.logs_1000");
    }

    @GameTest
    public void threshold_min_pickaxe_5000(GameTestHelper ctx) {
        assertQuestThreshold(ctx, "xpq.min.pickaxe_5000");
    }

    @GameTest
    public void threshold_min_ore_5000(GameTestHelper ctx) {
        assertQuestThreshold(ctx, "xpq.min.ore_5000");
    }

    @GameTest
    public void threshold_min_diamond_ore_64(GameTestHelper ctx) {
        assertQuestThreshold(ctx, "xpq.min.diamond_ore_64");
    }

    @GameTest
    public void threshold_min_adamantium_10(GameTestHelper ctx) {
        assertQuestThreshold(ctx, "xpq.min.adamantium_10");
    }

    @GameTest
    public void threshold_min_debris_16(GameTestHelper ctx) {
        assertQuestThreshold(ctx, "xpq.min.debris_16");
    }

    @GameTest
    public void threshold_min_ore_20000(GameTestHelper ctx) {
        assertQuestThreshold(ctx, "xpq.min.ore_20000");
    }

    @GameTest
    public void threshold_frm_composite_foods(GameTestHelper ctx) {
        assertQuestThreshold(ctx, "xpq.frm.composite_foods");
    }

    @GameTest
    public void threshold_frm_tame_5(GameTestHelper ctx) {
        assertQuestThreshold(ctx, "xpq.frm.tame_5");
    }

    @GameTest
    public void threshold_frm_harvest_1000(GameTestHelper ctx) {
        assertQuestThreshold(ctx, "xpq.frm.harvest_1000");
    }

    @GameTest
    public void threshold_frm_till_1000(GameTestHelper ctx) {
        assertQuestThreshold(ctx, "xpq.frm.till_1000");
    }

    @GameTest
    public void threshold_frm_harvest_5000(GameTestHelper ctx) {
        assertQuestThreshold(ctx, "xpq.frm.harvest_5000");
    }

    @GameTest
    public void threshold_frm_barter_200(GameTestHelper ctx) {
        assertQuestThreshold(ctx, "xpq.frm.barter_200");
    }

    @GameTest
    public void threshold_fsh_first_sale(GameTestHelper ctx) {
        assertQuestThreshold(ctx, "xpq.fsh.first_sale");
    }

    @GameTest
    public void threshold_fsh_sale_64(GameTestHelper ctx) {
        assertQuestThreshold(ctx, "xpq.fsh.sale_64");
    }

    @GameTest
    public void threshold_fsh_catch_500(GameTestHelper ctx) {
        assertQuestThreshold(ctx, "xpq.fsh.catch_500");
    }

    @GameTest
    public void threshold_fsh_emerald_500(GameTestHelper ctx) {
        assertQuestThreshold(ctx, "xpq.fsh.emerald_500");
    }

    @GameTest
    public void threshold_cft_sleeping_bag(GameTestHelper ctx) {
        assertQuestThreshold(ctx, "xpq.cft.sleeping_bag");
    }

    @GameTest
    public void threshold_cft_all_daggers(GameTestHelper ctx) {
        assertQuestThreshold(ctx, "xpq.cft.all_daggers");
    }

    @GameTest
    public void threshold_cft_anvil_50(GameTestHelper ctx) {
        assertQuestThreshold(ctx, "xpq.cft.anvil_50");
    }

    @GameTest
    public void threshold_cft_redstone_100(GameTestHelper ctx) {
        assertQuestThreshold(ctx, "xpq.cft.redstone_100");
    }

    @GameTest
    public void threshold_cft_parachute(GameTestHelper ctx) {
        assertQuestThreshold(ctx, "xpq.cft.parachute");
    }

    @GameTest
    public void threshold_cft_all_greatswords(GameTestHelper ctx) {
        assertQuestThreshold(ctx, "xpq.cft.all_greatswords");
    }

    @GameTest
    public void threshold_cft_mm_blocks(GameTestHelper ctx) {
        assertQuestThreshold(ctx, "xpq.cft.mm_blocks");
    }

    @GameTest
    public void threshold_bld_place_1000(GameTestHelper ctx) {
        assertQuestThreshold(ctx, "xpq.bld.place_1000");
    }

    @GameTest
    public void threshold_bld_variety_50(GameTestHelper ctx) {
        assertQuestThreshold(ctx, "xpq.bld.variety_50");
    }

    @GameTest
    public void threshold_bld_place_10000(GameTestHelper ctx) {
        assertQuestThreshold(ctx, "xpq.bld.place_10000");
    }

    @GameTest
    public void threshold_bld_variety_150(GameTestHelper ctx) {
        assertQuestThreshold(ctx, "xpq.bld.variety_150");
    }

    @GameTest
    public void threshold_bld_place_50000(GameTestHelper ctx) {
        assertQuestThreshold(ctx, "xpq.bld.place_50000");
    }

    @GameTest
    public void threshold_bld_place_200000(GameTestHelper ctx) {
        assertQuestThreshold(ctx, "xpq.bld.place_200000");
    }

    @GameTest
    public void threshold_alc_potions_250(GameTestHelper ctx) {
        assertQuestThreshold(ctx, "xpq.alc.potions_250");
    }

    /** Writes the evidence a tracker reads, at {@code amount} units of the quest target. */
    private static ResearchProgressState inject(ServerPlayer player, XpQuestCatalogData.Spec spec, int amount) {
        XpQuestCatalogData.Tracker t = spec.tracker();
        ResearchProgressState s = state(player);
        return switch (t.kind()) {
            case "counter" -> s.with("quest.counter." + t.key(), amount * t.divisor())
                    .with(XpQuestHooks.baselineKey(t.seedCondition() == null ? "unused" : t.seedCondition()), 0);
            case "counter_sum" -> s.with("quest.counter." + t.entries().get(0), amount);
            case "placement" -> s.with("quest.counter." + XpQuestHooks.BLOCKS_PLACED, amount)
                    .with(XpQuestHooks.baselineKey(XpQuestHooks.BLOCK_ITEMS_USED_BASELINE), 0);
            case "distinct_set" -> {
                for (int i = 0; i < amount; i++) {
                    String entry = t.entries().isEmpty() ? "minecraft:test_block_" + i : t.entries().get(i);
                    s = QuestRules.observeDistinct(s, t.key(), entry);
                }
                yield s;
            }
            case "distinct_groups" -> {
                for (int i = 0; i < amount; i++) s = QuestRules.observeDistinct(s, t.key(), t.entries().get(i).split("\\|")[0]);
                yield s;
            }
            case "stat_crafted_distinct" -> {
                for (int i = 0; i < amount; i++)
                    player.awardStat(Stats.ITEM_CRAFTED.get(BuiltInRegistries.ITEM.getValue(Identifier.parse(t.entries().get(i)))));
                yield s;
            }
            default -> throw new IllegalStateException("No injector for " + t.kind());
        };
    }

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
