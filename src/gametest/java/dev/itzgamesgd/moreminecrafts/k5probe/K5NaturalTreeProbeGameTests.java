package dev.itzgamesgd.moreminecrafts.k5probe;

import dev.itzgamesgd.moreminecrafts.k5probe.K5NaturalTreeProbe.ChunkData;
import java.lang.reflect.Method;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
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
    public void k5NaturalBiomeDecorationHookRunsAndRestoresScope(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos origin = ctx.absolutePos(new BlockPos(1, 1, 1));
        ChunkAccess chunk = level.getChunkAt(origin);
        long callsBefore = K5NaturalTreeProbe.biomeDecorationCalls();
        level.getChunkSource().getGenerator().applyBiomeDecoration((WorldGenLevel) level, chunk,
                level.structureManager());
        ctx.assertTrue(K5NaturalTreeProbe.biomeDecorationCalls() == callsBefore + 1,
                "the mapped ChunkGenerator.applyBiomeDecoration hook observed this real runtime call");
        ctx.assertTrue(!K5NaturalTreeProbe.isNaturalBiomeDecoration(),
                "natural-generation scope is cleared after the generation call");
        ctx.succeed();
    }

    @GameTest
    public void k5ChunkAttachmentStoresOnlyActualTreeTrunkLogWrites(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos logPos = ctx.absolutePos(new BlockPos(1, 1, 1));
        level.setBlock(logPos, Blocks.OAK_LOG.defaultBlockState(), 3);
        K5NaturalTreeProbe.enterNaturalBiomeDecoration();
        try {
            K5NaturalTreeProbe.recordTrunkWrite((WorldGenLevel) level, logPos,
                    Blocks.OAK_LOG.defaultBlockState());
            K5NaturalTreeProbe.recordTrunkWrite((WorldGenLevel) level, logPos,
                    Blocks.OAK_LOG.defaultBlockState());
            BlockPos nonLog = logPos.above();
            level.setBlock(nonLog, Blocks.OAK_LEAVES.defaultBlockState(), 3);
            K5NaturalTreeProbe.recordTrunkWrite((WorldGenLevel) level, nonLog,
                    Blocks.OAK_LEAVES.defaultBlockState());
        } finally {
            K5NaturalTreeProbe.exitNaturalBiomeDecoration();
        }

        ChunkData data = K5NaturalTreeProbe.data(level.getChunkAt(logPos));
        ctx.assertTrue(data != null && data.schemaVersion() == K5NaturalTreeProbe.SCHEMA_VERSION,
                "persistent Fabric chunk attachment is available and uses the declared schema");
        ctx.assertTrue(data.positions().equals(Set.of(logPos.asLong())),
                "the packed trunk position is recorded once and leaves are excluded");
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
