package dev.itzgamesgd.moreminecrafts.k5probe;

import dev.itzgamesgd.moreminecrafts.k5probe.K5NaturalTreeProbe.ChunkData;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.TreeSet;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.chunk.ChunkAccess;

/** Runtime-only checks for the diagnostic hooks. These tests never touch Knowledge state. */
public final class K5NaturalTreeProbeGameTests implements CustomTestMethodInvoker {
    @GameTest
    public void k5FeatureIdentityUsesRegistryAllowlistAndFailsClosed(GameTestHelper ctx) {
        Identifier vanillaForestPlacement = Identifier.fromNamespaceAndPath("minecraft", "trees_birch_and_oak_leaf_litter");
        Identifier vanillaOakVariant = Identifier.fromNamespaceAndPath("minecraft", "oak_bees_0002_leaf_litter");
        Identifier vanillaLargeSpruce = Identifier.fromNamespaceAndPath("minecraft", "mega_spruce");
        Identifier moddedFeature = Identifier.fromNamespaceAndPath("examplemod", "custom_tree");
        Identifier unknownVanillaFeature = Identifier.fromNamespaceAndPath("minecraft", "custom_tree");

        ctx.assertTrue(K5NaturalTreeProbe.isEligibleTreeIdentity(vanillaForestPlacement, vanillaOakVariant, true),
                "the runtime-observed vanilla forest tree identity is allowlisted");
        ctx.assertTrue(K5NaturalTreeProbe.isEligibleTreeIdentity(vanillaForestPlacement, vanillaLargeSpruce, true),
                "a registered vanilla large-tree identity is allowlisted");
        ctx.assertTrue(!K5NaturalTreeProbe.isEligibleTreeIdentity(vanillaForestPlacement, moddedFeature, true),
                "a mod namespace cannot authorize a tree feature");
        ctx.assertTrue(!K5NaturalTreeProbe.isEligibleTreeIdentity(
                        Identifier.fromNamespaceAndPath("examplemod", "trees"), vanillaOakVariant, true),
                "a modded placed feature cannot authorize a vanilla tree source");
        ctx.assertTrue(!K5NaturalTreeProbe.isEligibleTreeIdentity(vanillaForestPlacement, unknownVanillaFeature, true),
                "an unknown minecraft feature key is rejected even in the vanilla namespace");
        ctx.assertTrue(!K5NaturalTreeProbe.isEligibleTreeIdentity(vanillaForestPlacement, null, true),
                "an unregistered feature with no registry key fails closed");
        ctx.assertTrue(!K5NaturalTreeProbe.isEligibleTreeIdentity(vanillaForestPlacement, vanillaOakVariant, false),
                "a non-TreeFeature implementation is rejected");
        ctx.assertTrue(!K5NaturalTreeProbe.isEligibleTreeIdentity(null, vanillaOakVariant, true),
                "an unknown placed-feature identity fails closed");
        ctx.succeed();
    }

    @GameTest
    public void k5DeterministicNaturalForestDecorationMarksRealVanillaTrunkLogs(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        ctx.assertTrue(level.getSeed() == 0L, "GameTest fixture runs with its pinned seed 0");

        var biomeRegistry = level.registryAccess().lookupOrThrow(Registries.BIOME);
        var forest = biomeRegistry.get(Identifier.fromNamespaceAndPath("minecraft", "forest")).orElseThrow();
        var noiseRegistry = level.registryAccess().lookupOrThrow(Registries.NOISE_SETTINGS);
        var overworldNoise = noiseRegistry.get(Identifier.fromNamespaceAndPath("minecraft", "overworld")).orElseThrow();
        var placedFeatureRegistry = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
        Set<Identifier> registeredTreeFeatureIds = new TreeSet<>();
        var featureRegistry = level.registryAccess().lookupOrThrow(Registries.FEATURE);
        for (Feature registeredFeature : featureRegistry) {
            if (registeredFeature instanceof TreeFeature) {
                Identifier key = featureRegistry.getKey(registeredFeature);
                registeredTreeFeatureIds.add(key);
                if (key != null && key.getNamespace().equals("minecraft")) {
                    ctx.assertTrue(K5NaturalTreeProbe.isKnownVanillaTreeFeatureId(key),
                            "every registered vanilla TreeFeature key is in the pinned allowlist: " + key);
                }
            }
        }
        Set<Identifier> vanillaForestTreePlacements = new TreeSet<>();
        for (HolderSet<PlacedFeature> step : forest.value().getGenerationSettings().features()) {
            for (Holder<PlacedFeature> placedHolder : step) {
                PlacedFeature placed = placedHolder.value();
                if (placed.getFeatures().anyMatch(feature -> feature.value() instanceof TreeFeature)) {
                    Identifier key = placedFeatureRegistry.getKey(placed);
                    if (key != null && key.getNamespace().equals("minecraft")) {
                        vanillaForestTreePlacements.add(key);
                    }
                }
            }
        }
        ctx.assertTrue(!vanillaForestTreePlacements.isEmpty(),
                "the registered minecraft:forest generation settings contain registered vanilla TreeFeature placements");

        NoiseBasedChunkGenerator fixtureGenerator =
                new NoiseBasedChunkGenerator(new FixedBiomeSource(forest), overworldNoise);
        BlockPos fixedChunkProbe = new BlockPos(8, 4, 8);
        ChunkAccess chunk = level.getChunkAt(fixedChunkProbe);
        ctx.assertTrue(chunk.getPos().equals(new ChunkPos(0, 0)),
                "fixture chunk coordinates are fixed at (0,0)");
        chunk.fillBiomesFromNoise((quartX, quartY, quartZ) -> forest);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                level.setBlock(new BlockPos(x, 1, z), Blocks.DIRT.defaultBlockState(), 3);
                level.setBlock(new BlockPos(x, 2, z), Blocks.DIRT.defaultBlockState(), 3);
                level.setBlock(new BlockPos(x, 3, z), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
            }
        }

        ChunkData before = K5NaturalTreeProbe.data(chunk);
        ctx.assertTrue(before == null || before.positions().isEmpty(),
                "the controlled target chunk starts without provenance markers");
        long callsBefore = K5NaturalTreeProbe.biomeDecorationCalls();
        long callbacksBefore = K5NaturalTreeProbe.naturalTreeTrunkCallbacks();
        fixtureGenerator.applyBiomeDecoration(level, chunk, level.structureManager());

        ctx.assertTrue(K5NaturalTreeProbe.biomeDecorationCalls() == callsBefore + 1,
                "the verified natural ChunkGenerator.applyBiomeDecoration hook was entered");
        ctx.assertTrue(!K5NaturalTreeProbe.isNaturalBiomeDecoration(),
                "natural-generation scope is cleared after biome decoration returns");
        Set<Identifier> observedTreeFeatureIds = K5NaturalTreeProbe.lastNaturalTreeFeatureIds();
        Set<Long> observedTrunkPositions = K5NaturalTreeProbe.lastNaturalTreeCallbackPositions();
        ChunkData data = K5NaturalTreeProbe.data(chunk);
        var positions = new TreeSet<>(data == null ? Set.<Long>of() : data.positions());
        System.out.println("K5_DETERMINISTIC_FIXTURE seed=0 dimension=" + level.dimension().identifier()
                + " generator=NoiseBasedChunkGenerator"
                + " noiseSettings=minecraft:overworld biome=minecraft:forest chunk=[0,0]"
                + " configuredTreeFeatureIds=" + registeredTreeFeatureIds
                + " treePlacements=" + vanillaForestTreePlacements
                + " observedTreeFeatureIds=" + observedTreeFeatureIds
                + " callbackCount=" + (K5NaturalTreeProbe.naturalTreeTrunkCallbacks() - callbacksBefore)
                + " callbackPositions=" + new TreeSet<>(observedTrunkPositions)
                + " markerCount=" + positions.size()
                + " markers=" + positions);
        ctx.assertTrue(K5NaturalTreeProbe.naturalTreeTrunkCallbacks() > callbacksBefore,
                "natural biome decoration invoked an actual registered TreeFeature trunk setter");
        ctx.assertTrue(observedTreeFeatureIds.contains(
                        Identifier.fromNamespaceAndPath("minecraft", "oak_bees_0002_leaf_litter")),
                "the deterministic forest invokes the runtime-observed registered vanilla Feature identity");
        ctx.assertTrue(!observedTrunkPositions.isEmpty(),
                "the runtime trace identifies actual trunk callback positions");
        Set<Long> actualLogPositions = new TreeSet<>();
        var callbackStates = new java.util.TreeMap<String, String>();
        for (long packed : observedTrunkPositions) {
            BlockPos callbackPos = BlockPos.of(packed);
            var state = level.getBlockState(callbackPos);
            callbackStates.put(callbackPos.toString(), state.toString());
            if (state.is(net.minecraft.tags.BlockTags.LOGS)) actualLogPositions.add(packed);
        }
        System.out.println("K5_CALLBACK_FINAL_BLOCK_STATES " + callbackStates);
        ctx.assertTrue(!actualLogPositions.isEmpty(),
                "at least one natural TreeFeature callback position remains a real log block after decoration");
        ctx.assertTrue(data != null && !data.positions().isEmpty(),
                "the natural TreeFeature marker predicate records at least one actual trunk position");
        ctx.assertTrue(actualLogPositions.containsAll(positions),
                "every marker was independently observed as a final log at a trunk callback position");
        for (long packed : positions) {
            BlockPos markedPos = BlockPos.of(packed);
            var state = level.getBlockState(markedPos);
            ctx.assertTrue(level.getChunkAt(markedPos) == chunk,
                    "each marker is attached to the exact chunk and world that contain its position");
            ctx.assertTrue(state.is(Blocks.OAK_LOG) || state.is(Blocks.BIRCH_LOG),
                    "every marked position contains an expected vanilla forest trunk log: " + markedPos + " = " + state);
            ctx.assertTrue(data.positions().contains(markedPos.asLong()),
                    "the independently read block position is present in the persistent chunk marker set");
        }
        ctx.succeed();
    }

    @GameTest
    public void k5ChunkAttachmentStoresPackedPositionData(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos logPos = ctx.absolutePos(new BlockPos(1, 1, 1));
        level.setBlock(logPos, Blocks.OAK_LOG.defaultBlockState(), 3);
        ChunkAccess chunk = level.getChunkAt(logPos);
        var target = (net.fabricmc.fabric.api.attachment.v1.AttachmentTarget) chunk;
        target.setAttached(K5NaturalTreeProbe.NATURAL_TREE_LOGS,
                new ChunkData(K5NaturalTreeProbe.SCHEMA_VERSION, Set.of(logPos.asLong())));

        ChunkData data = K5NaturalTreeProbe.data(chunk);
        ctx.assertTrue(data != null && data.schemaVersion() == K5NaturalTreeProbe.SCHEMA_VERSION,
                "persistent Fabric chunk attachment is available and uses the declared schema");
        ctx.assertTrue(data.positions().equals(Set.of(logPos.asLong())),
                "the attachment codec stores a packed position");
        ctx.succeed();
    }

    @GameTest
    public void k5PlayerGrownTwoByTwoSpruceTreeIsNotTagged(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos origin = ctx.absolutePos(new BlockPos(2, 1, 2));
        // mega_pine is the registered feature selected by the spruce TreeGrower. Remove
        // ambient terrain/structures in a fixed clearance volume so placement does not
        // depend on the GameTest runner's randomly assigned world coordinates.
        for (int x = -8; x <= 8; x++) {
            for (int z = -8; z <= 8; z++) {
                for (int y = 0; y <= 48; y++) {
                    level.setBlock(origin.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        for (int x = 0; x < 4; x++) {
            for (int z = 0; z < 4; z++) {
                level.setBlock(origin.offset(x, -1, z), Blocks.DIRT.defaultBlockState(), 3);
            }
        }

        SaplingBlock sapling = (SaplingBlock) Blocks.SPRUCE_SAPLING;
        var matureSapling = sapling.defaultBlockState().setValue(SaplingBlock.STAGE, 1);
        BlockPos[] saplingPositions = {origin, origin.east(), origin.south(), origin.east().south()};
        for (BlockPos saplingPos : saplingPositions) {
            level.setBlock(saplingPos, matureSapling, 3);
        }
        for (BlockPos saplingPos : saplingPositions) {
            ctx.assertTrue(level.getBlockState(saplingPos).is(Blocks.SPRUCE_SAPLING)
                            && level.getBlockState(saplingPos.below()).is(Blocks.DIRT),
                    "each of four actual mature spruce saplings is planted on dirt before growth: " + saplingPos);
        }

        K5NaturalTreeProbe.beginTreeGrowerTrace();
        sapling.advanceTree(level, origin, matureSapling, RandomSource.create(0L));

        int generatedLogCount = 0;
        for (int x = -1; x <= 5; x++) {
            for (int z = -1; z <= 5; z++) {
                for (int y = 1; y <= 32; y++) {
                    if (level.getBlockState(origin.offset(x, y - 1, z)).is(net.minecraft.tags.BlockTags.LOGS)) {
                        generatedLogCount++;
                    }
                }
            }
        }
        int baseTrunkLogs = 0;
        for (BlockPos trunkPos : saplingPositions) {
            if (level.getBlockState(trunkPos).is(net.minecraft.tags.BlockTags.LOGS)) baseTrunkLogs++;
        }
        System.out.println("K5_SAPLING_2X2 baseTrunkLogs=" + baseTrunkLogs
                + " generatedLogCount=" + generatedLogCount
                + " featureAttempts=" + K5NaturalTreeProbe.treeGrowerFeatureAttempts());
        ctx.assertTrue(baseTrunkLogs == 4 && generatedLogCount >= 4,
                "the real SaplingBlock.advanceTree -> TreeGrower.growTree path grew a trunk from all four sapling positions");
        ChunkData data = K5NaturalTreeProbe.data(level.getChunkAt(origin));
        ctx.assertTrue(data == null || data.positions().isEmpty(),
                "sapling TreeGrower direct Feature.place calls did not enter natural-biome scope");
        ctx.succeed();
    }

    @GameTest
    public void k5BonemealGrownTreeIsNotTagged(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos soil = ctx.absolutePos(new BlockPos(2, 1, 2));
        BlockPos saplingPos = soil.above();
        level.setBlock(soil, Blocks.DIRT.defaultBlockState(), 3);
        level.setBlock(saplingPos, Blocks.OAK_SAPLING.defaultBlockState()
                .setValue(SaplingBlock.STAGE, 1), 3);
        var player = ctx.makeMockServerPlayerInLevel();
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BONE_MEAL, 64));
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(saplingPos),
                net.minecraft.core.Direction.UP, saplingPos, false);

        for (int attempt = 0; attempt < 64 && level.getBlockState(saplingPos).is(Blocks.OAK_SAPLING); attempt++) {
            player.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(
                    player, net.minecraft.world.InteractionHand.MAIN_HAND, hit));
        }
        int logs = 0;
        for (int x = -1; x <= 4; x++) {
            for (int z = -1; z <= 4; z++) {
                for (int y = 1; y <= 40; y++) {
                    if (level.getBlockState(saplingPos.offset(x, y, z)).is(net.minecraft.tags.BlockTags.LOGS)) logs++;
                }
            }
        }
        ctx.assertTrue(logs > 0, "vanilla bone-meal use grew a real oak sapling tree");
        ChunkData data = K5NaturalTreeProbe.data(level.getChunkAt(saplingPos));
        ctx.assertTrue(data == null || data.positions().isEmpty(), "bonemeal TreeGrower placement is outside natural decoration");
        ctx.succeed();
    }

    @GameTest
    public void k5PlayerPlacedLogIsNotTagged(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos support = ctx.absolutePos(new BlockPos(2, 1, 2));
        level.setBlock(support, Blocks.STONE.defaultBlockState(), 3);
        var player = ctx.makeMockServerPlayerInLevel();
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.OAK_LOG, 1));
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(support),
                net.minecraft.core.Direction.UP, support, false);
        var result = player.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(
                player, net.minecraft.world.InteractionHand.MAIN_HAND, hit));
        BlockPos placedPos = support.above();
        ctx.assertTrue(result.consumesAction() && level.getBlockState(placedPos).is(Blocks.OAK_LOG),
                "ordinary Survival item use placed a real oak log");
        ChunkData data = K5NaturalTreeProbe.data(level.getChunkAt(placedPos));
        ctx.assertTrue(data == null || data.positions().isEmpty(), "a player item placement has no natural worldgen scope");
        ctx.succeed();
    }

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
