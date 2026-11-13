package dev.itzgamesgd.moreminecrafts.pyramid.worldgen;

import com.mojang.serialization.MapCodec;
import dev.itzgamesgd.moreminecrafts.pyramid.St01PyramidAssembly;
import dev.itzgamesgd.moreminecrafts.pyramid.St01PyramidAssembly.AccessRoute;
import dev.itzgamesgd.moreminecrafts.pyramid.St01PyramidAssembly.Orientation;
import dev.itzgamesgd.moreminecrafts.pyramid.St01RoyalChestPlacement;
import dev.itzgamesgd.moreminecrafts.pyramid.St01RouteSelection;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * ST-01 Pyramid natural structure (P8-C2-B): the complete P8-C2-A assembly.
 *
 * <p>Candidate selection (spacing 32 / separation 8 / triangular / salt 91401) is the structure set's job. For a candidate
 * chunk this class derives, in order and without loading any chunk:
 * <ol>
 *   <li>the ground anchor column (chunk centre) and an orientation (four rotations, from the start's seeded
 *       {@code WorldgenRandom}, vanilla convention, carried over from Phase 4);</li>
 *   <li>the {@link St01Eligibility} verdict for the full 81x81 Desert footprint and MODERATE terrain, against the real chunk
 *       generator (Policy D: no entrance or underground-fluid criterion);</li>
 *   <li>the ground anchor {@code (cx, yf, cz)} and, from the resulting world-space tomb anchor, the EAST/WEST route
 *       ({@link St01RouteSelection}).</li>
 * </ol>
 * Only an eligible candidate yields a start; its pieces are the footprint correction followed by the three assembly pieces
 * in the approved order.
 */
public final class St01PyramidStructure extends Structure {
    public static final MapCodec<St01PyramidStructure> CODEC = simpleCodec(St01PyramidStructure::new);

    /** findGenerationPoint evaluations per verdict, current process (a chunk may be evaluated more than once). */
    public static final Map<St01Eligibility.Verdict, AtomicLong> VERDICT_COUNTS = new EnumMap<>(St01Eligibility.Verdict.class);
    static {
        for (St01Eligibility.Verdict v : St01Eligibility.Verdict.values()) VERDICT_COUNTS.put(v, new AtomicLong());
    }

    public St01PyramidStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        Rotation rotation = Rotation.getRandom(context.random());
        St01Candidate.Decision decision = St01Candidate.decide(new GeneratorTerrain(context), context.chunkPos(), rotation, context.seed());
        VERDICT_COUNTS.get(decision.result().verdict()).incrementAndGet();
        if (decision.ground().isEmpty()) return Optional.empty();
        BlockPos ground = decision.ground().get();
        AccessRoute route = decision.route().orElseThrow();
        return Optional.of(new GenerationStub(ground,
                builder -> addPieces(builder, context.structureTemplateManager(), ground, decision.orientation(), route)));
    }

    /** Correction first, then ASSEMBLY, D1_ROYAL_HALL, D1_SEAM_EXCEPTION: the approved P8-C2-A order. */
    public static void addPieces(StructurePiecesBuilder builder, StructureTemplateManager templates, BlockPos ground,
            Orientation orientation, AccessRoute route) {
        builder.addPiece(new St01FootprintCorrectionPiece(ground.getX(), ground.getY(), ground.getZ()));
        for (int i = 0; i < St01PyramidAssembly.pieces(route).size(); i++) {
            builder.addPiece(new St01AssemblyPiece(templates, ground, orientation, route, i));
        }
    }

    @Override
    public StructureType<?> type() {
        return St01Worldgen.STRUCTURE_TYPE;
    }

    /**
     * {@link St01Eligibility.Terrain} over the real chunk generator and biome source; memoises per evaluation.
     *
     * <p>P8-D3: heights come from {@link St01FastTerrain#fillChunk} (whole chunks, identical values) and the Desert stage
     * is preceded by {@link St01FastTerrain#desertPairPossible}, each only where {@link St01FastTerrain} says it applies;
     * otherwise this is exactly the previous per-column view. The first batched height of every evaluation is compared
     * with {@code getBaseHeight}; on a mismatch the evaluation continues on the per-column path.
     */
    public static final class GeneratorTerrain implements St01Eligibility.Terrain {
        private final GenerationContext context;
        private final Map<Long, Integer> heights = new HashMap<>();
        private final Map<Long, int[]> chunkHeights = new HashMap<>();
        private final Map<Long, Boolean> columns = new HashMap<>();
        private final boolean prefilter;
        private boolean batched;
        private boolean spotChecked;

        public GeneratorTerrain(GenerationContext context) {
            this.context = context;
            this.batched = St01FastTerrain.batchedApplies(context.chunkGenerator());
            this.prefilter = St01FastTerrain.prefilterApplies(context.biomeSource());
        }

        private static long key(int x, int z) { return ((long) x << 32) ^ (z & 0xFFFFFFFFL); }

        @Override
        public int surfaceHeight(int x, int z) {
            if (batched) {
                int cx = x >> 4, cz = z >> 4;
                int[] chunk = chunkHeights.computeIfAbsent(key(cx, cz), k -> St01FastTerrain.fillChunk(
                        (NoiseBasedChunkGenerator) context.chunkGenerator(), context.heightAccessor(), context.randomState(), cx << 4, cz << 4));
                int h = chunk[(x - (cx << 4)) * 16 + (z - (cz << 4))];
                if (spotChecked) return h;
                spotChecked = true;
                if (St01FastTerrain.spotCheck(x, z, h, legacyHeight(x, z))) return h;
                batched = false;
                chunkHeights.clear();
            }
            return heights.computeIfAbsent(key(x, z), k -> legacyHeight(x, z));
        }

        private int legacyHeight(int x, int z) {
            St01FastTerrain.LEGACY_COLUMN_HEIGHTS.incrementAndGet();
            return context.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
        }

        @Override
        public boolean isDesertCell(int quartX, int quartY, int quartZ) {
            return context.biomeResolver().getNoiseBiome(quartX, quartY, quartZ).is(Biomes.DESERT);
        }

        @Override
        public boolean desertPossibleInColumn(int quartX, int quartZ) {
            if (!prefilter) return true;
            return columns.computeIfAbsent(key(quartX, quartZ), k -> St01FastTerrain.desertPairPossible(context.biomeSource(),
                    context.climateSampler(), context.heightAccessor(), quartX, quartZ));
        }
    }
}
