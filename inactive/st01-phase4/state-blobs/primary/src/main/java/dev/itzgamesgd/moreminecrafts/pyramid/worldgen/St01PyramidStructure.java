package dev.itzgamesgd.moreminecrafts.pyramid.worldgen;

import com.mojang.serialization.MapCodec;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * ST-01 Pyramid structure: qualifies an anchor with {@link St01Eligibility} against the real chunk generator (no chunk loading, no
 * neighbour generation) and, only when eligible, emits the correction piece plus the pieces of the {@link St01RoyalHallFixture}.
 * Candidate selection (spacing 32 / separation 8 / triangular / salt 91401) is the structure set's job, not this class's.
 */
public final class St01PyramidStructure extends Structure {
    public static final MapCodec<St01PyramidStructure> CODEC = simpleCodec(St01PyramidStructure::new);

    /** Production counters, one per verdict, for cross-checking probes. Counts findGenerationPoint evaluations (a chunk may be evaluated more than once). */
    public static final Map<St01Eligibility.Verdict, AtomicLong> VERDICT_COUNTS = new java.util.EnumMap<>(St01Eligibility.Verdict.class);
    static {
        for (St01Eligibility.Verdict v : St01Eligibility.Verdict.values()) VERDICT_COUNTS.put(v, new AtomicLong());
    }

    public St01PyramidStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        int cx = chunk.getMinBlockX() + 8, cz = chunk.getMinBlockZ() + 8;
        Rotation rotation = Rotation.getRandom(context.random());
        St01Eligibility.Result result = St01Eligibility.evaluate(new GeneratorTerrain(context), cx, cz, St01RoyalHallFixture.volume(rotation));
        VERDICT_COUNTS.get(result.verdict()).incrementAndGet();
        if (!result.eligible()) return Optional.empty();
        int yf = result.foundationY();
        return Optional.of(new GenerationStub(new BlockPos(cx, yf, cz),
                builder -> St01RoyalHallFixture.addPieces(builder, context.structureTemplateManager(), cx, yf, cz, rotation)));
    }

    @Override
    public StructureType<?> type() {
        return St01Worldgen.STRUCTURE_TYPE;
    }

    /** {@link St01Eligibility.Terrain} over the real chunk generator and biome source; memoises per evaluation. */
    public static final class GeneratorTerrain implements St01Eligibility.Terrain {
        private final GenerationContext context;
        private final Map<Long, Integer> heights = new HashMap<>();
        private final Map<Long, NoiseColumn> columns = new HashMap<>();

        public GeneratorTerrain(GenerationContext context) {
            this.context = context;
        }

        private static long key(int x, int z) { return ((long) x << 32) ^ (z & 0xFFFFFFFFL); }

        @Override
        public int surfaceHeight(int x, int z) {
            return heights.computeIfAbsent(key(x, z), k -> context.chunkGenerator().getBaseHeight(x, z,
                    Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState()));
        }

        @Override
        public boolean isDesertCell(int quartX, int quartY, int quartZ) {
            return context.biomeResolver().getNoiseBiome(quartX, quartY, quartZ).is(Biomes.DESERT);
        }

        @Override
        public boolean hasFluid(int x, int z, int minY, int maxY) {
            LevelHeightAccessor accessor = context.heightAccessor();
            int lo = Math.max(minY, accessor.getMinY()), hi = Math.min(maxY, accessor.getMaxY());
            NoiseColumn column = columns.computeIfAbsent(key(x, z), k -> context.chunkGenerator().getBaseColumn(x, z, accessor, context.randomState()));
            for (int y = lo; y <= hi; y++) {
                if (!column.getBlock(y).getFluidState().isEmpty()) return true;
            }
            return false;
        }
    }
}
