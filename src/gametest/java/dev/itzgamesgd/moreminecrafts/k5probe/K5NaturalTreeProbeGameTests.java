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
    public void k5PlacedFeatureIdentityRejectsModdedAndUnknownSources(GameTestHelper ctx) {
        Identifier ordinaryVanilla = Identifier.fromNamespaceAndPath("minecraft", "trees_plains");
        Identifier vanillaLargeTreeBiome = Identifier.fromNamespaceAndPath("minecraft", "trees_dark_forest");
        Identifier vanillaTreeFeature = Identifier.fromNamespaceAndPath("minecraft", "tree");
        Identifier moddedSource = Identifier.fromNamespaceAndPath("examplemod", "custom_tree");
        Identifier nonVanillaTreeIdInVanillaNamespace =
                Identifier.fromNamespaceAndPath("minecraft", "custom_tree");

        ctx.assertTrue(K5NaturalTreeProbe.isEligibleTreeIdentity(ordinaryVanilla, vanillaTreeFeature, true),
                "registered vanilla placed-feature and TreeFeature registry identities are eligible");
        ctx.assertTrue(K5NaturalTreeProbe.isEligibleTreeIdentity(vanillaLargeTreeBiome, vanillaTreeFeature, true),
                "vanilla large-tree placement identity remains eligible");
        ctx.assertTrue(!K5NaturalTreeProbe.isEligibleTreeIdentity(moddedSource, vanillaTreeFeature, true),
                "a mod namespace cannot authorize a tree feature");
        ctx.assertTrue(!K5NaturalTreeProbe.isEligibleTreeIdentity(ordinaryVanilla, moddedSource, true),
                "a mod-registered TreeFeature cannot be authorized by a vanilla placed feature");
        ctx.assertTrue(!K5NaturalTreeProbe.isEligibleTreeIdentity(ordinaryVanilla, nonVanillaTreeIdInVanillaNamespace, true),
                "an unknown Feature registry key under minecraft cannot masquerade as the vanilla tree identity");
        ctx.assertTrue(!K5NaturalTreeProbe.isEligibleTreeIdentity(ordinaryVanilla, vanillaTreeFeature, false),
                "non-TreeFeature implementations are excluded");
        ctx.assertTrue(!K5NaturalTreeProbe.isEligibleTreeIdentity(null, vanillaTreeFeature, true),
                "unknown or unregistered placed-feature identity fails closed");
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
        var featureRegistry = level.registryAccess().lookupOrThrow(Registries.FEATURE);
        var treeFeature = featureRegistry.get(Identifier.fromNamespaceAndPath("minecraft", "tree")).orElseThrow().value();
        var placedFeatureRegistry = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
        Set<Identifier> vanillaForestTreePlacements = new TreeSet<>();
        for (HolderSet<PlacedFeature> step : forest.value().getGenerationSettings().features()) {
            for (Holder<PlacedFeature> placedHolder : step) {
                PlacedFeature placed = placedHolder.value();
                if (placed.getFeatures().anyMatch(feature -> feature.value() == treeFeature)) {
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
        long trunkWritesBefore = K5NaturalTreeProbe.naturalTrunkWrites();
        fixtureGenerator.applyBiomeDecoration(level, chunk, level.structureManager());

        ctx.assertTrue(K5NaturalTreeProbe.biomeDecorationCalls() == callsBefore + 1,
                "the verified natural ChunkGenerator.applyBiomeDecoration hook was entered");
        ctx.assertTrue(!K5NaturalTreeProbe.isNaturalBiomeDecoration(),
                "natural-generation scope is cleared after biome decoration returns");
        ctx.assertTrue(K5NaturalTreeProbe.naturalTrunkWrites() > trunkWritesBefore,
                "the registered vanilla forest configuration placed a real TreeFeature trunk");

        ChunkData data = K5NaturalTreeProbe.data(chunk);
        ctx.assertTrue(data != null && !data.positions().isEmpty(),
                "actual TreeFeature trunk writes were recorded in this chunk's provenance attachment");
        var positions = new TreeSet<>(data.positions());
        System.out.println("K5_DETERMINISTIC_FIXTURE seed=0 dimension=" + level.dimension().identifier()
                + " generator=NoiseBasedChunkGenerator"
                + " noiseSettings=minecraft:overworld biome=minecraft:forest chunk=[0,0]"
                + " treePlacements=" + vanillaForestTreePlacements
                + " markerCount=" + positions.size()
                + " markers=" + positions);
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
        for (int x = 0; x < 4; x++)
            for (int z = 0; z < 4; z++)
                level.setBlock(origin.offset(x, -1, z), Blocks.DIRT.defaultBlockState(), 3);
        SaplingBlock sapling = (SaplingBlock) Blocks.SPRUCE_SAPLING;
        var matureSapling = sapling.defaultBlockState().setValue(SaplingBlock.STAGE, 1);
        level.setBlock(origin, matureSapling, 3);
        level.setBlock(origin.east(), matureSapling, 3);
        level.setBlock(origin.south(), matureSapling, 3);
        level.setBlock(origin.east().south(), matureSapling, 3);
        sapling.advanceTree(level, origin, matureSapling, RandomSource.create(0L));

        int generatedLogCount = 0;
        for (int x = -1; x <= 5; x++)
            for (int z = -1; z <= 5; z++)
                for (int y = 1; y <= 16; y++)
                    if (level.getBlockState(origin.offset(x, y - 1, z)).is(net.minecraft.tags.BlockTags.LOGS))
                        generatedLogCount++;
        ctx.assertTrue(generatedLogCount >= 4,
                "the 2x2 sapling path generated a real tree trunk through TreeGrower.growTree");
        ChunkData data = K5NaturalTreeProbe.data(level.getChunkAt(origin));
        ctx.assertTrue(data == null || data.positions().isEmpty(),
                "sapling TreeGrower direct Feature.place calls did not enter natural-biome scope");
        ctx.succeed();
    }

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
