package dev.itzgamesgd.moreminecrafts.k5probe;

import dev.itzgamesgd.moreminecrafts.k5probe.K5NaturalTreeProbe.ChunkData;
import java.lang.reflect.Method;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
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

        ctx.assertTrue(K5NaturalTreeProbe.isEligibleTreeIdentity(ordinaryVanilla, vanillaTreeFeature, true),
                "registered vanilla placed-feature and TreeFeature registry identities are eligible");
        ctx.assertTrue(K5NaturalTreeProbe.isEligibleTreeIdentity(vanillaLargeTreeBiome, vanillaTreeFeature, true),
                "vanilla large-tree placement identity remains eligible");
        ctx.assertTrue(!K5NaturalTreeProbe.isEligibleTreeIdentity(moddedSource, vanillaTreeFeature, true),
                "a mod namespace cannot authorize a tree feature");
        ctx.assertTrue(!K5NaturalTreeProbe.isEligibleTreeIdentity(ordinaryVanilla, moddedSource, true),
                "a mod-registered TreeFeature cannot be authorized by a vanilla placed feature");
        ctx.assertTrue(!K5NaturalTreeProbe.isEligibleTreeIdentity(ordinaryVanilla, vanillaTreeFeature, false),
                "non-TreeFeature implementations are excluded");
        ctx.assertTrue(!K5NaturalTreeProbe.isEligibleTreeIdentity(null, vanillaTreeFeature, true),
                "unknown or unregistered placed-feature identity fails closed");
        ctx.succeed();
    }

    @GameTest
    public void k5NaturalBiomeDecorationHookRunsAndRestoresScope(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos origin = ctx.absolutePos(new BlockPos(1, 1, 1));
        ChunkAccess chunk = level.getChunkAt(origin);
        long callsBefore = K5NaturalTreeProbe.biomeDecorationCalls();
        long trunkWritesBefore = K5NaturalTreeProbe.naturalTrunkWrites();
        level.getChunkSource().getGenerator().applyBiomeDecoration((WorldGenLevel) level, chunk,
                level.structureManager());
        ctx.assertTrue(K5NaturalTreeProbe.biomeDecorationCalls() == callsBefore + 1,
                "the mapped ChunkGenerator.applyBiomeDecoration hook observed this real runtime call");
        ctx.assertTrue(K5NaturalTreeProbe.naturalTrunkWrites() > trunkWritesBefore,
                "natural biome decoration executed TreeFeature trunk callbacks that wrote real log blocks");
        ctx.assertTrue(!K5NaturalTreeProbe.isNaturalBiomeDecoration(),
                "natural-generation scope is cleared after the generation call");

        ChunkData data = K5NaturalTreeProbe.data(chunk);
        ctx.assertTrue(data != null && !data.positions().isEmpty(),
                "at least one actual natural TreeFeature trunk-write position was marked");
        for (long packed : data.positions()) {
            BlockPos markedPos = BlockPos.of(packed);
            ctx.assertTrue(level.getBlockState(markedPos).is(net.minecraft.tags.BlockTags.LOGS),
                    "every positive marker resolves to an existing log block");
        }

        BlockPos consumedMarker = BlockPos.of(data.positions().iterator().next());
        level.setBlock(consumedMarker, Blocks.AIR.defaultBlockState(), 3);
        ChunkData afterRemoval = K5NaturalTreeProbe.data(level.getChunkAt(consumedMarker));
        ctx.assertTrue(afterRemoval == null || !afterRemoval.positions().contains(consumedMarker.asLong()),
                "replacing the original log removes its marker");

        level.setBlock(consumedMarker, Blocks.OAK_LOG.defaultBlockState(), 3);
        ChunkData afterPlayerPlacement = K5NaturalTreeProbe.data(level.getChunkAt(consumedMarker));
        ctx.assertTrue(afterPlayerPlacement == null || !afterPlayerPlacement.positions().contains(consumedMarker.asLong()),
                "placing a log at a consumed natural position does not restore eligibility");
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
