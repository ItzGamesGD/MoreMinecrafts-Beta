package dev.itzgamesgd.moreminecrafts.progression;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Soil Study (`v3.research.16`) attribution, L1/L2/L3 effects, persistence and multiplayer isolation. */
public final class SoilStudyGameTests implements CustomTestMethodInvoker {
    private static final BlockPos RESTART_FARMLAND = new BlockPos((200 << 4) + 1, 80, (200 << 4) + 1);
    private static final UUID RESTART_CULTIVATOR = UUID.fromString("5c1f6a3e-0000-4000-8000-0000000000bb");
    private final List<ServerPlayer> players = new ArrayList<>();

    @GameTest
    public void tillingRecordsLastCultivatorAndReplacementClearsIt(GameTestHelper ctx) {
        ServerPlayer first = player(ctx, 0);
        ServerPlayer second = player(ctx, 0);
        BlockPos rel = new BlockPos(1, 1, 1);
        BlockPos pos = ctx.absolutePos(rel);
        ctx.setBlock(rel, Blocks.DIRT);
        till(ctx, first, rel);
        ctx.assertTrue(ctx.getBlockState(rel).is(Blocks.FARMLAND), "hoe tills dirt");
        ctx.assertTrue(FarmlandCultivators.cultivator(ctx.getLevel(), pos).equals(java.util.Optional.of(first.getUUID())),
                "successful tilling records the player");

        // A failed hoe use on existing farmland does not steal attribution.
        second.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WOODEN_HOE));
        useOnTop(ctx, second, rel);
        ctx.assertTrue(FarmlandCultivators.cultivator(ctx.getLevel(), pos).orElseThrow().equals(first.getUUID()),
                "a hoe click that does not till keeps the previous cultivator");

        // Moisture updates keep farmland and its record.
        ctx.setBlock(rel, Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, 7));
        ctx.assertTrue(FarmlandCultivators.cultivator(ctx.getLevel(), pos).isPresent(),
                "farmland-to-farmland updates keep the record");

        ctx.setBlock(rel, Blocks.DIRT);
        ctx.assertTrue(!FarmlandCultivators.data(chunk(ctx, pos)).cultivators().containsKey(pos.asLong()),
                "replacing farmland removes the stored record immediately");

        till(ctx, second, rel);
        ctx.assertTrue(FarmlandCultivators.cultivator(ctx.getLevel(), pos).orElseThrow().equals(second.getUUID()),
                "tilling again attributes the new cultivator");
        till(ctx, first, new BlockPos(1, 1, 3), true);
        ctx.assertTrue(FarmlandCultivators.cultivator(ctx.getLevel(), ctx.absolutePos(new BlockPos(1, 1, 3)))
                        .orElseThrow().equals(first.getUUID()),
                "neighbouring farmland keeps its own cultivator");
        ctx.assertTrue(FarmlandCultivators.cultivator(ctx.getLevel(), pos).orElseThrow().equals(second.getUUID()),
                "the first record is not overwritten by another player's farmland");
        ctx.succeed();
    }

    @GameTest
    public void staleAndNonFarmlandRecordsAreRejectedAndRemoved(GameTestHelper ctx) {
        BlockPos rel = new BlockPos(2, 1, 2);
        BlockPos pos = ctx.absolutePos(rel);
        UUID stranger = UUID.randomUUID();
        ctx.setBlock(rel, Blocks.STONE);
        FarmlandCultivators.record(ctx.getLevel(), pos, stranger);
        ctx.assertTrue(FarmlandCultivators.cultivator(ctx.getLevel(), pos).isEmpty()
                        && chunk(ctx, pos).getAttached(FarmlandCultivators.ATTACHMENT) == null,
                "attribution is never created for a non-farmland position");

        ctx.setBlock(rel, Blocks.FARMLAND);
        FarmlandCultivators.record(ctx.getLevel(), pos, stranger);
        ctx.assertTrue(FarmlandCultivators.cultivator(ctx.getLevel(), pos).isPresent(), "record exists");
        // Bypass the replacement hook: only lazy validation can notice that farmland is gone.
        chunk(ctx, pos).setBlockState(pos, Blocks.STONE.defaultBlockState(), 0);
        ctx.assertTrue(FarmlandCultivators.cultivator(ctx.getLevel(), pos).isEmpty(),
                "a record whose farmland vanished silently is rejected");
        ctx.assertTrue(chunk(ctx, pos).getAttached(FarmlandCultivators.ATTACHMENT) == null,
                "and that stale record is removed from the chunk");
        ctx.succeed();
    }

    @GameTest
    public void attributionSurvivesChunkSerializationAndReload(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos rel = new BlockPos(3, 1, 3);
        BlockPos pos = ctx.absolutePos(rel);
        UUID cultivator = UUID.fromString("5c1f6a3e-0000-4000-8000-0000000000aa");
        ctx.setBlock(rel, Blocks.FARMLAND);
        FarmlandCultivators.record(level, pos, cultivator);
        LevelChunk chunk = chunk(ctx, pos);
        var tag = SerializableChunkData.copyOf(level, chunk).write();
        var parsed = SerializableChunkData.parse(level, level.palettedContainerFactory(), tag);
        var reloaded = parsed.read(level, level.getPoiManager(),
                new RegionStorageInfo("soil-study-test", level.dimension(), "chunk"), chunk.getPos());
        FarmlandCultivatorData restored = ((AttachmentTarget) reloaded).getAttached(FarmlandCultivators.ATTACHMENT);
        ctx.assertTrue(restored != null && cultivator.equals(restored.cultivators().get(pos.asLong())),
                "Minecraft's chunk write/parse path restores the persistent cultivator record: " + restored);
        ctx.assertTrue(restored.cultivators().size() == FarmlandCultivators.data(chunk).cultivators().size(),
                "no records are lost or invented by reload");
        ctx.succeed();
    }

    /** Run with soil.restart.phase=write, then read in a second JVM sharing build/run/gameTest. */
    @GameTest
    public void attributionSurvivesRealServerRestart(GameTestHelper ctx) {
        String phase = System.getProperty("moreminecrafts.soil.restart.phase");
        ServerLevel level = ctx.getLevel();
        if (phase == null || phase.equals("write")) {
            level.setBlock(RESTART_FARMLAND, Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_ALL);
            BlockPos otherChunkFarmland = RESTART_FARMLAND.offset(16, 0, 0);
            level.setBlock(otherChunkFarmland, Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_ALL);
            LevelChunk chunk = level.getChunkAt(RESTART_FARMLAND);
            LevelChunk otherChunk = level.getChunkAt(otherChunkFarmland);
            ServerLevel nether = level.getServer().getLevel(net.minecraft.world.level.Level.NETHER);
            ctx.assertTrue(nether != null, "Nether dimension is available for isolation check");
            nether.setBlock(RESTART_FARMLAND, Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_ALL);
            LevelChunk netherChunk = nether.getChunkAt(RESTART_FARMLAND);
            level.getChunkSource().save(true);
            nether.getChunkSource().save(true);
            FarmlandCultivators.record(level, RESTART_FARMLAND, RESTART_CULTIVATOR);
            ctx.assertTrue(chunk.isUnsaved(), "attachment mutation marks the chunk dirty after a save");
            level.getChunkSource().save(true);
            ctx.assertTrue(FarmlandCultivators.cultivator(nether, RESTART_FARMLAND).isEmpty(),
                    "the same packed position in another dimension has no inherited cultivator");
            ctx.assertTrue(FarmlandCultivators.cultivator(level, otherChunkFarmland).isEmpty(),
                    "a neighboring chunk at the same Y/Z position has no inherited cultivator");
            System.out.println("SOIL_RESTART_WRITE overworld=" + chunk.getPos() + " nether=" + netherChunk.getPos()
                    + " neighboringChunk=" + otherChunk.getPos() + " cultivator=" + RESTART_CULTIVATOR);
            if (phase == null) ctx.succeed();
        } else if (phase.equals("read")) {
            ctx.assertTrue(level.getBlockState(RESTART_FARMLAND).is(Blocks.FARMLAND),
                    "saved farmland survives a fresh server process");
            ctx.assertTrue(FarmlandCultivators.cultivator(level, RESTART_FARMLAND)
                            .equals(java.util.Optional.of(RESTART_CULTIVATOR)),
                    "the cultivator attachment survives a fresh server process");
            ServerLevel nether = level.getServer().getLevel(net.minecraft.world.level.Level.NETHER);
            ctx.assertTrue(nether != null && nether.getBlockState(RESTART_FARMLAND).is(Blocks.FARMLAND),
                    "the corresponding Nether farmland survives independently");
            ctx.assertTrue(nether != null && FarmlandCultivators.cultivator(nether, RESTART_FARMLAND).isEmpty(),
                    "a restart does not leak attribution across dimensions");
            BlockPos otherChunkFarmland = RESTART_FARMLAND.offset(16, 0, 0);
            ctx.assertTrue(level.getBlockState(otherChunkFarmland).is(Blocks.FARMLAND)
                            && FarmlandCultivators.cultivator(level, otherChunkFarmland).isEmpty(),
                    "neighboring chunk data survives independently without inheriting attribution");
            System.out.println("SOIL_RESTART_READ chunk=" + level.getChunkAt(RESTART_FARMLAND).getPos()
                    + " cultivator=" + RESTART_CULTIVATOR);
        } else {
            ctx.assertTrue(false, "unknown Soil Study restart phase: " + phase);
        }
        ctx.succeed();
    }

    @GameTest
    public void trampleResistanceFollowsRecordedCultivatorOnly(GameTestHelper ctx) {
        ServerPlayer researched = player(ctx, 1);
        ServerPlayer plain = player(ctx, 0);
        BlockPos protectedRel = new BlockPos(1, 1, 1);
        BlockPos plainRel = new BlockPos(3, 1, 1);
        BlockPos unknownRel = new BlockPos(5, 1, 1);
        BlockPos supportRel = new BlockPos(7, 1, 1);
        for (BlockPos rel : List.of(protectedRel, plainRel, unknownRel, supportRel)) ctx.setBlock(rel, Blocks.FARMLAND);
        FarmlandCultivators.record(ctx.getLevel(), ctx.absolutePos(protectedRel), researched.getUUID());
        FarmlandCultivators.record(ctx.getLevel(), ctx.absolutePos(plainRel), plain.getUUID());
        FarmlandCultivators.record(ctx.getLevel(), ctx.absolutePos(supportRel), researched.getUUID());
        researched.setPos(ctx.absolutePos(unknownRel).getX() + 0.5D, ctx.absolutePos(unknownRel).getY() + 1.0D,
                ctx.absolutePos(unknownRel).getZ() + 0.5D); // nearby researcher gives no L1 fallback

        var cow = ctx.spawn(EntityTypes.COW, new BlockPos(1, 3, 3));
        for (BlockPos rel : List.of(protectedRel, plainRel, unknownRel)) {
            BlockPos pos = ctx.absolutePos(rel);
            BlockState state = ctx.getLevel().getBlockState(pos);
            state.getBlock().fallOn(ctx.getLevel(), state, pos, cow, 100.0D);
        }
        ctx.assertTrue(ctx.getBlockState(protectedRel).is(Blocks.FARMLAND),
                "farmland tilled by an L1 researcher resists trampling");
        ctx.assertTrue(ctx.getBlockState(plainRel).is(Blocks.DIRT),
                "farmland tilled by an unresearched player is trampled normally");
        ctx.assertTrue(ctx.getBlockState(unknownRel).is(Blocks.DIRT),
                "unknown-owner farmland has no L1 fallback and is trampled normally");

        // Other farmland transformations are untouched: a solid block above removes even protected farmland.
        ctx.setBlock(supportRel.above(), Blocks.STONE);
        BlockPos support = ctx.absolutePos(supportRel);
        ctx.getLevel().getBlockState(support).tick(ctx.getLevel(), support, ctx.getLevel().getRandom());
        ctx.assertTrue(ctx.getBlockState(supportRel).is(Blocks.DIRT),
                "support loss still converts protected farmland to dirt");
        ctx.succeed();
    }

    @GameTest public void hydrationAtDistanceFourIsVanillaForEveryone(GameTestHelper ctx) { hydrationAt(ctx, 4); }
    @GameTest public void hydrationAtDistanceFiveNeedsLevelThreeCultivator(GameTestHelper ctx) { hydrationAt(ctx, 5); }
    @GameTest public void hydrationAtDistanceSixNeedsLevelThreeCultivator(GameTestHelper ctx) { hydrationAt(ctx, 6); }
    @GameTest public void hydrationAtDistanceSevenIsNeverExtended(GameTestHelper ctx) { hydrationAt(ctx, 7); }

    private void hydrationAt(GameTestHelper ctx, int distance) {
        ServerPlayer l3 = player(ctx, 3);
        ServerPlayer l2 = player(ctx, 2);
        BlockPos farmlandL3 = new BlockPos(1, 1, 1);
        BlockPos farmlandL2 = new BlockPos(1, 5, 1);
        prepareFarmland(ctx, farmlandL3, l3.getUUID());
        prepareFarmland(ctx, farmlandL2, l2.getUUID());
        placeWater(ctx, farmlandL3.offset(distance, 0, 0));
        placeWater(ctx, farmlandL2.offset(distance, 0, 0));
        ctx.assertTrue(randomTickKeepsMoisture(ctx, farmlandL3) == (distance <= 6),
                "L3 cultivator hydration at distance " + distance);
        ctx.assertTrue(randomTickKeepsMoisture(ctx, farmlandL2) == (distance <= 4),
                "L2 cultivator hydration at distance " + distance + " stays vanilla");
        // Water diagonally at the same Chebyshev distance counts; one block further never does.
        BlockPos corner = new BlockPos(1, 9, 1);
        prepareFarmland(ctx, corner, l3.getUUID());
        placeWater(ctx, corner.offset(distance, 0, distance));
        ctx.assertTrue(randomTickKeepsMoisture(ctx, corner) == (distance <= 6),
                "diagonal reach is the same horizontal distance " + distance);
        ctx.succeed();
    }

    @GameTest
    public void unknownOwnerFallbackUsesOneNearbyLevelThreeResearcher(GameTestHelper ctx) {
        BlockPos farmlandRel = new BlockPos(1, 1, 1);
        prepareFarmland(ctx, farmlandRel, null);
        placeWater(ctx, farmlandRel.offset(6, 0, 0));
        BlockPos farmland = ctx.absolutePos(farmlandRel);
        ctx.assertTrue(!randomTickKeepsMoisture(ctx, farmlandRel), "no researcher: vanilla reach only");

        ServerPlayer far = player(ctx, 3);
        far.setPos(farmland.getX() + 17.0D, farmland.getY(), farmland.getZ());
        ctx.assertTrue(!randomTickKeepsMoisture(ctx, farmlandRel), "a researcher beyond 16 blocks does not qualify");
        ServerPlayer low = player(ctx, 2);
        low.setPos(farmland.getX() + 2.0D, farmland.getY(), farmland.getZ());
        ctx.assertTrue(!randomTickKeepsMoisture(ctx, farmlandRel), "a nearby L2 researcher does not qualify");
        far.setPos(farmland.getX() + 15.5D, farmland.getY(), farmland.getZ());
        ctx.assertTrue(randomTickKeepsMoisture(ctx, farmlandRel), "a researcher within 16 blocks qualifies");

        ServerPlayer second = player(ctx, 3);
        second.setPos(farmland.getX() - 3.0D, farmland.getY(), farmland.getZ());
        ServerPlayer third = player(ctx, 3);
        third.setPos(farmland.getX(), farmland.getY(), farmland.getZ() + 4.0D);
        BlockPos crop = farmlandRel.above();
        ctx.assertTrue(SoilStudyRuntime.cropGrowthBonus(ctx.getLevel(), ctx.absolutePos(crop)) == 0.20D,
                "three nearby researchers still give exactly one +20% fallback bonus");

        // A recorded unresearched cultivator blocks the fallback: the record is authoritative.
        FarmlandCultivators.record(ctx.getLevel(), farmland, UUID.randomUUID());
        ctx.assertTrue(SoilStudyRuntime.cropGrowthBonus(ctx.getLevel(), ctx.absolutePos(crop)) == 0.0D,
                "farmland with a recorded non-researcher gets no fallback bonus");
        ctx.assertTrue(!randomTickKeepsMoisture(ctx, farmlandRel), "and no extended hydration either");
        ctx.succeed();
    }

    @GameTest
    public void extendedHydrationNeverLoadsChunks(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos farmland = ctx.absolutePos(new BlockPos(1, 1, 1));
        int far = 16 * 4000;
        BlockPos remote = new BlockPos(far + 1, 64, far + 1);
        ctx.assertTrue(level.getChunkSource().getChunkNow(remote.getX() >> 4, remote.getZ() >> 4) == null,
                "fixture chunk is not loaded");
        // The scan helper must treat unloaded columns as dry rather than loading them.
        ctx.assertTrue(!SoilStudyRuntime.waterInExtendedRing(level, remote), "unloaded surroundings read as dry");
        ctx.assertTrue(level.getChunkSource().getChunkNow(remote.getX() >> 4, remote.getZ() >> 4) == null,
                "the ring scan did not load the chunk");
        ctx.assertTrue(farmland.getY() >= level.getMinY(), "sanity");
        ctx.succeed();
    }

    @GameTest
    public void growthChanceUsesRecordedCultivatorAndIsProbabilisticNotAStageJump(GameTestHelper ctx) {
        ServerPlayer l3 = player(ctx, 3);
        ServerPlayer l2 = player(ctx, 2);
        BlockPos rel = new BlockPos(1, 1, 1);
        prepareFarmland(ctx, rel, l3.getUUID());
        BlockPos crop = rel.above();
        ctx.setBlock(crop.above(), Blocks.LIGHT);

        ctx.assertTrue(rolledGrowth(ctx, crop, 0.0D), "an L3 cultivator's wheat takes the extra growth roll");
        ctx.assertTrue(!rolledGrowth(ctx, crop, 0.999999D),
                "the bonus is a probability: a high extra roll does not grow the crop");

        FarmlandCultivators.record(ctx.getLevel(), ctx.absolutePos(rel), l2.getUUID());
        ctx.assertTrue(!rolledGrowth(ctx, crop, 0.0D), "an L2 cultivator gives no growth bonus");
        FarmlandCultivators.record(ctx.getLevel(), ctx.absolutePos(rel), l3.getUUID());
        ctx.assertTrue(rolledGrowth(ctx, crop, 0.0D), "re-attributing to L3 restores the bonus");

        // Crops not planted on farmland are unaffected.
        BlockPos dirtRel = new BlockPos(3, 1, 1);
        ctx.setBlock(dirtRel, Blocks.DIRT);
        ctx.assertTrue(SoilStudyRuntime.cropGrowthBonus(ctx.getLevel(), ctx.absolutePos(dirtRel.above())) == 0.0D,
                "no bonus above non-farmland");

        // Measured over many ordinary rolls the relative growth rate is about +20%.
        int boosted = growthTrials(ctx, crop, 6000, 20261017L);
        FarmlandCultivators.record(ctx.getLevel(), ctx.absolutePos(rel), l2.getUUID());
        int plain = growthTrials(ctx, crop, 6000, 20261017L);
        double ratio = boosted / (double) plain;
        ctx.assertTrue(ratio > 1.10D && ratio < 1.30D,
                "L3 growth rate is about 1.2x vanilla: boosted=" + boosted + " plain=" + plain);
        ctx.succeed();
    }

    @GameTest
    public void seedYieldUsesTheHarvestersEntitlementAndVanillaCountOnly(GameTestHelper ctx) {
        ServerPlayer l2 = player(ctx, 2);
        ServerPlayer l1 = player(ctx, 1);
        ServerPlayer cultivatorOnly = player(ctx, 3);
        BlockState mature = Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7);
        List<ItemStack> vanilla = List.of(new ItemStack(Items.WHEAT), new ItemStack(Items.WHEAT_SEEDS, 4));
        List<ItemStack> withSeedStudy = List.of(new ItemStack(Items.WHEAT), new ItemStack(Items.WHEAT_SEEDS, 4),
                new ItemStack(Items.WHEAT_SEEDS, 1));

        List<ItemStack> boosted = SoilStudyRuntime.applySeedYield(l2, mature, vanilla, vanilla, () -> 0.0D);
        ctx.assertTrue(boosted.size() == 3 && boosted.get(2).is(Items.WHEAT_SEEDS) && boosted.get(2).getCount() == 2,
                "30% of 4 seeds is 1.2: one guaranteed plus a 20% second seed (roll 0.0)");
        List<ItemStack> noFraction = SoilStudyRuntime.applySeedYield(l2, mature, vanilla, vanilla, () -> 0.99D);
        ctx.assertTrue(noFraction.get(2).getCount() == 1, "the fractional seed is not granted on a high roll");
        ctx.assertTrue(vanilla.size() == 2 && vanilla.get(1).getCount() == 4, "vanilla loot is never mutated");

        List<ItemStack> afterSeedStudy = SoilStudyRuntime.applySeedYield(l2, mature, vanilla, withSeedStudy, () -> 0.99D);
        ctx.assertTrue(afterSeedStudy.size() == 4 && afterSeedStudy.get(3).getCount() == 1,
                "the bonus derives from the 4 vanilla seeds, not from Seed Study's extra seed");
        ctx.assertTrue(SoilStudyRuntime.applySeedYield(l1, mature, vanilla, vanilla, () -> 0.0D).size() == 2,
                "L1 has no seed bonus");
        ctx.assertTrue(SoilStudyRuntime.applySeedYield(l2, Blocks.WHEAT.defaultBlockState(), vanilla, vanilla,
                () -> 0.0D).size() == 2, "immature crops are ineligible");
        ctx.assertTrue(SoilStudyRuntime.applySeedYield(l2, mature, List.of(new ItemStack(Items.WHEAT)),
                List.of(new ItemStack(Items.WHEAT)), () -> 0.0D).size() == 1, "harvests without seeds are ineligible");

        // The cultivator's level is irrelevant: only the harvester decides L2.
        BlockPos rel = new BlockPos(1, 1, 1);
        prepareFarmland(ctx, rel, cultivatorOnly.getUUID());
        List<ItemStack> viaCultivator = SoilStudyRuntime.applySeedYield(l1, mature, vanilla, vanilla, () -> 0.0D);
        ctx.assertTrue(viaCultivator.size() == 2, "a researched cultivator does not entitle another harvester");

        // End to end through Block.getDrops, the real hook, with many harvests per entitlement.
        int paid = 0;
        int unpaid = 0;
        for (int i = 0; i < 400; i++) {
            paid += seeds(Block.getDrops(mature, ctx.getLevel(), ctx.absolutePos(rel.above()), null, l2, ItemStack.EMPTY));
            unpaid += seeds(Block.getDrops(mature, ctx.getLevel(), ctx.absolutePos(rel.above()), null, l1, ItemStack.EMPTY));
        }
        ctx.assertTrue(paid - unpaid > 100, "harvests by an L2 player yield clearly more seeds: " + paid + " vs " + unpaid);
        ctx.succeed();
    }

    // --- helpers ---

    private static int seeds(List<ItemStack> drops) {
        return drops.stream().filter(s -> s.is(Items.WHEAT_SEEDS)).mapToInt(ItemStack::getCount).sum();
    }

    /** Runs one ordinary random tick whose growth roll fails, then scripts the single extra roll. */
    private static boolean rolledGrowth(GameTestHelper ctx, BlockPos crop, double extraRoll) {
        ctx.setBlock(crop, Blocks.WHEAT);
        BlockPos abs = ctx.absolutePos(crop);
        BlockState state = ctx.getLevel().getBlockState(abs);
        state.randomTick(ctx.getLevel(), abs, new ScriptedRandom(extraRoll));
        return ctx.getLevel().getBlockState(abs).getValue(CropBlock.AGE) == 1;
    }

    private static int growthTrials(GameTestHelper ctx, BlockPos crop, int trials, long seed) {
        LegacyRandomSource random = new LegacyRandomSource(seed);
        BlockPos abs = ctx.absolutePos(crop);
        int grown = 0;
        for (int i = 0; i < trials; i++) {
            ctx.getLevel().setBlock(abs, Blocks.WHEAT.defaultBlockState(), 2);
            ctx.getLevel().getBlockState(abs).randomTick(ctx.getLevel(), abs, random);
            if (ctx.getLevel().getBlockState(abs).getValue(CropBlock.AGE) > 0) grown++;
        }
        ctx.getLevel().setBlock(abs, Blocks.AIR.defaultBlockState(), 2);
        return grown;
    }

    /** A random source whose ordinary growth roll never succeeds and whose next double is scripted. */
    private static final class ScriptedRandom extends LegacyRandomSource {
        private final double nextDouble;
        ScriptedRandom(double nextDouble) { super(1L); this.nextDouble = nextDouble; }
        @Override public int nextInt(int bound) { return 1; }
        @Override public double nextDouble() { return nextDouble; }
    }

    private static void prepareFarmland(GameTestHelper ctx, BlockPos rel, UUID cultivator) {
        ctx.setBlock(rel, Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, 3));
        ctx.setBlock(rel.below(), Blocks.DIRT);
        BlockPos pos = ctx.absolutePos(rel);
        FarmlandCultivators.clear(ctx.getLevel(), pos);
        if (cultivator != null) FarmlandCultivators.record(ctx.getLevel(), pos, cultivator);
    }

    private static void placeWater(GameTestHelper ctx, BlockPos rel) {
        ctx.setBlock(rel, Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.WATERLOGGED, true));
    }

    /** Moisture 3 jumps to 7 when water is in reach, otherwise vanilla dries it by one. */
    private static boolean randomTickKeepsMoisture(GameTestHelper ctx, BlockPos rel) {
        BlockPos abs = ctx.absolutePos(rel);
        BlockState state = Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, 3);
        ctx.getLevel().setBlock(abs, state, 2);
        state.randomTick(ctx.getLevel(), abs, ctx.getLevel().getRandom());
        return ctx.getLevel().getBlockState(abs).getValue(FarmlandBlock.MOISTURE) == 7;
    }

    private static void till(GameTestHelper ctx, ServerPlayer player, BlockPos rel) { till(ctx, player, rel, false); }

    private static void till(GameTestHelper ctx, ServerPlayer player, BlockPos rel, boolean placeDirt) {
        if (placeDirt) ctx.setBlock(rel, Blocks.DIRT);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WOODEN_HOE));
        useOnTop(ctx, player, rel);
    }

    private static void useOnTop(GameTestHelper ctx, ServerPlayer player, BlockPos rel) {
        BlockPos world = ctx.absolutePos(rel);
        var hit = new BlockHitResult(Vec3.atCenterOf(world), Direction.UP, world, false);
        player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    }

    private static LevelChunk chunk(GameTestHelper ctx, BlockPos pos) {
        return ctx.getLevel().getChunkAt(pos);
    }

    private ServerPlayer player(GameTestHelper ctx, int soilLevel) {
        var profile = new com.mojang.authlib.GameProfile(UUID.randomUUID(), "soil-" + players.size());
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(profile, false);
        var player = new ServerPlayer(ctx.getLevel().getServer(), ctx.getLevel(), cookie.gameProfile(),
                cookie.clientInformation());
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        ctx.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
        BlockPos home = ctx.absolutePos(new BlockPos(1, 2, 1));
        player.setPos(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D);
        if (soilLevel > 0) KnowledgeRuntime.updateState(player,
                ResearchProgressState.empty().with("research." + SoilStudyRuntime.ID, soilLevel));
        players.add(player);
        return player;
    }

    @Override
    public void invokeTestMethod(GameTestHelper ctx, Method method) throws ReflectiveOperationException {
        try {
            method.invoke(this, ctx);
        } finally {
            // Mock players must not linger and qualify unrelated crops as nearby researchers.
            for (ServerPlayer player : players) ctx.getLevel().getServer().getPlayerList().remove(player);
            players.clear();
        }
    }
}
