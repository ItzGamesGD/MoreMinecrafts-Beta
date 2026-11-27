package dev.itzgamesgd.moreminecrafts.progression;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;

/**
 * Real ore-feature output in the flat GameTest world: fill a fixed chunk with stone, then run the
 * genuine overworld-noise biome decoration on it (same harness as the K5 natural tree tests). The
 * ores it writes are produced by vanilla ore features inside natural decoration.
 */
final class QuestOreFixture {
    static final int MIN_Y = 0;
    static final int MAX_Y = 63;

    private QuestOreFixture() {}

    static List<BlockPos> decorateWithNaturalOres(ServerLevel level, ChunkPos pos) {
        level.getChunkSource().addTicketWithRadius(TicketType.FORCED, pos, 0);
        LevelChunk chunk = level.getChunk(pos.x(), pos.z());
        var biome = level.registryAccess().lookupOrThrow(Registries.BIOME)
                .get(Identifier.fromNamespaceAndPath("minecraft", "plains")).orElseThrow();
        var noise = level.registryAccess().lookupOrThrow(Registries.NOISE_SETTINGS)
                .get(Identifier.fromNamespaceAndPath("minecraft", "overworld")).orElseThrow();
        chunk.fillBiomesFromNoise((qx, qy, qz) -> biome);
        int x0 = pos.getMinBlockX(), z0 = pos.getMinBlockZ();
        for (int x = 0; x < 16; x++)
            for (int z = 0; z < 16; z++)
                for (int y = MIN_Y; y <= MAX_Y; y++)
                    level.setBlock(new BlockPos(x0 + x, y, z0 + z), Blocks.STONE.defaultBlockState(), 2);
        new NoiseBasedChunkGenerator(new FixedBiomeSource(biome), noise)
                .applyBiomeDecoration(level, chunk, level.structureManager());
        return ores(level, pos);
    }

    static List<BlockPos> ores(ServerLevel level, ChunkPos pos) {
        List<BlockPos> found = new ArrayList<>();
        for (int x = pos.getMinBlockX(); x <= pos.getMaxBlockX(); x++)
            for (int z = pos.getMinBlockZ(); z <= pos.getMaxBlockZ(); z++)
                for (int y = MIN_Y; y <= MAX_Y; y++) {
                    BlockPos p = new BlockPos(x, y, z);
                    if (QuestOreProvenance.isTrackedOre(level.getBlockState(p))) found.add(p);
                }
        return found;
    }
}
