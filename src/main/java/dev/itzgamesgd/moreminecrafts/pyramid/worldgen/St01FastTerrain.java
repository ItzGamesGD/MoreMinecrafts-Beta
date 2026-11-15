package dev.itzgamesgd.moreminecrafts.pyramid.worldgen;

import dev.itzgamesgd.moreminecrafts.mixin.NoiseBasedChunkGeneratorAccessor;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.densityfunction.DensityVolume;
import net.minecraft.world.level.levelgen.densityfunction.ScopedDensityBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * P8-D3: exact-equivalent fast terrain for the ST-01 eligibility test. Neither part changes a verdict; both only change
 * how the same values are obtained.
 *
 * <p><b>C2 batched heights.</b> Minecraft 26.3 {@code NoiseBasedChunkGenerator.getBaseHeight} builds a 1 x H x 1
 * {@link DensityVolume}, a {@link NoiseChunk} and an aquifer for every single column and samples the whole column of
 * {@code finalDensity} before walking it top-down. Interpolated density is aligned to the global cell grid, so the
 * same values come out of one 16 x H x 16 volume per chunk. {@link #fillChunk} is the vanilla {@code iterateNoiseColumn}
 * algorithm over that volume: same settings, null beardifier, {@link Blender#empty()}, the generator's own fluid picker,
 * the same aquifer substance test and the same {@code WORLD_SURFACE_WG} opaque predicate, returning the same
 * "first free Y" (or the minimum Y when nothing is opaque).
 *
 * <p><b>C1 column biome prefilter.</b> The same height-free test vanilla 26.3 structures run before sampling heights
 * ({@code Structure.GenerationContext.couldStructureExistInColumn}): one column resolver from
 * {@link BiomeSource#createResolverForChunk} (the resolver the BIOMES step fills chunk biomes with). A column passes when
 * any two vertically adjacent quart cells in the range the ST-01 surface test can sample are both Desert, which is
 * necessary for {@code St01Eligibility.cellColumnDesert}.
 *
 * <p><b>Scope and fallback.</b> Each part is used only for the exact vanilla classes it was verified against
 * ({@link NoiseBasedChunkGenerator} and {@link MultiNoiseBiomeSource}); any other generator or biome source keeps the
 * unchanged per-column path. The batched path is also checked against {@code getBaseHeight} on the first column of
 * every evaluation; a mismatch disables it for the rest of the process and that evaluation is recomputed on the legacy
 * path. {@code -Dmm.st01.terrain.legacy=true} forces the legacy path for both parts (A/B diagnostics).
 */
public final class St01FastTerrain {
    private St01FastTerrain() {}

    private static final Logger LOGGER = LoggerFactory.getLogger("moreminecrafts/st01-terrain");
    private static final Predicate<BlockState> OPAQUE = Heightmap.Types.WORLD_SURFACE_WG.isOpaque();
    private static final AtomicBoolean BATCHED_DISABLED = new AtomicBoolean();

    /** Diagnostics for the current process. */
    public static final AtomicLong BATCHED_CHUNK_FILLS = new AtomicLong();
    public static final AtomicLong LEGACY_COLUMN_HEIGHTS = new AtomicLong();
    public static final AtomicLong SPOT_CHECKS = new AtomicLong();
    public static final AtomicLong SPOT_CHECK_MISMATCHES = new AtomicLong();
    public static final AtomicLong COLUMN_PREFILTER_TESTS = new AtomicLong();
    public static final AtomicLong COLUMN_PREFILTER_REJECTS = new AtomicLong();

    /** {@code -Dmm.st01.terrain.legacy=true}: per-column getBaseHeight and no prefilter (the pre-P8-D3 behaviour). */
    public static boolean legacyForced() {
        return Boolean.getBoolean("mm.st01.terrain.legacy");
    }

    /** Whether batched heights are used for {@code generator} (exact vanilla noise generator, not forced off, not disabled). */
    public static boolean batchedApplies(ChunkGenerator generator) {
        return !legacyForced() && !BATCHED_DISABLED.get() && generator.getClass() == NoiseBasedChunkGenerator.class;
    }

    /** Whether the column prefilter is used for {@code biomeSource} (exact vanilla multi-noise source, not forced off). */
    public static boolean prefilterApplies(BiomeSource biomeSource) {
        return !legacyForced() && biomeSource.getClass() == MultiNoiseBiomeSource.class;
    }

    /** Records a spot-check result; a mismatch permanently disables the batched path in this process. */
    static boolean spotCheck(int x, int z, int batched, int reference) {
        SPOT_CHECKS.incrementAndGet();
        if (batched == reference) return true;
        SPOT_CHECK_MISMATCHES.incrementAndGet();
        if (BATCHED_DISABLED.compareAndSet(false, true)) {
            LOGGER.warn("ST-01 batched surface height {} != getBaseHeight {} at {},{}; using per-column heights from now on "
                    + "(another mod may change NoiseBasedChunkGenerator heights)", batched, reference, x, z);
        }
        return false;
    }

    // ---- C2 -------------------------------------------------------------------------------------------------

    /**
     * {@code WORLD_SURFACE_WG} base heights of the 16 x 16 columns of the chunk with minimum block corner
     * ({@code minX}, {@code minZ}), indexed {@code (x - minX) * 16 + (z - minZ)}; identical to 256 calls of
     * {@code generator.getBaseHeight(x, z, WORLD_SURFACE_WG, heightAccessor, randomState)}.
     */
    public static int[] fillChunk(NoiseBasedChunkGenerator generator, LevelHeightAccessor heightAccessor, RandomState randomState,
            int minX, int minZ) {
        BATCHED_CHUNK_FILLS.incrementAndGet();
        NoiseGeneratorSettings settings = generator.generatorSettings().value();
        NoiseSettings noise = settings.noiseSettings().clampToHeightAccessor(heightAccessor);
        int[] out = new int[256];
        if (noise.height() <= 0) {
            Arrays.fill(out, heightAccessor.getMinY());
            return out;
        }
        Supplier<Aquifer.FluidPicker> picker = ((NoiseBasedChunkGeneratorAccessor) (Object) generator).moreminecrafts$globalFluidPicker();
        DensityVolume volume = new DensityVolume(16, noise.height(), 16, minX, noise.minY(), minZ);
        try (NoiseChunk noiseChunk = new NoiseChunk(randomState, null, settings, picker.get(), Blender.empty(), volume)) {
            Aquifer aquifer = noiseChunk.aquifer();
            BlockState defaultBlock = settings.defaultBlock();
            try (ScopedDensityBuffer density = noiseChunk.cachingSamplers().get(settings.noiseRouter().finalDensity())
                    .sampleVolume(noiseChunk.volume())) {
                for (int ix = 0; ix < 16; ix++) {
                    for (int iz = 0; iz < 16; iz++) {
                        int x = minX + ix, z = minZ + iz, result = heightAccessor.getMinY();
                        for (int iy = volume.sizeY() - 1; iy >= 0; iy--) {
                            int y = volume.blockY(iy);
                            BlockState state = aquifer.computeSubstance(x, y, z, density.get(volume.indexUnchecked(ix, iy, iz)));
                            if (OPAQUE.test(state == null ? defaultBlock : state)) {
                                result = y + 1;
                                break;
                            }
                        }
                        out[ix * 16 + iz] = result;
                    }
                }
            }
        }
        return out;
    }

    // ---- C1 -------------------------------------------------------------------------------------------------

    /** Lowest quart Y {@code cellColumnDesert} can sample: surface block {@code minY - 1} (no opaque block). */
    static int lowestSampledQuartY(LevelHeightAccessor heightAccessor) {
        return Math.floorDiv(heightAccessor.getMinY() - 1 - 2, 4);
    }

    /** Highest quart Y {@code cellColumnDesert} can sample: {@code qy + 1} for surface block {@code maxY}. */
    static int highestSampledQuartY(LevelHeightAccessor heightAccessor) {
        return Math.floorDiv(heightAccessor.getMaxY() - 2, 4) + 1;
    }

    /**
     * Height-free necessary condition for {@code St01Eligibility.cellColumnDesert(qx, qz)}: whatever the surface height,
     * that test needs Desert at two vertically adjacent quart cells {@code qy, qy + 1} inside
     * [{@link #lowestSampledQuartY}, {@link #highestSampledQuartY}]. Returns {@code false} only if no such pair exists.
     */
    public static boolean desertPairPossible(BiomeSource biomeSource, Climate.Sampler sampler, LevelHeightAccessor heightAccessor,
            int qx, int qz) {
        COLUMN_PREFILTER_TESTS.incrementAndGet();
        int qy0 = lowestSampledQuartY(heightAccessor), qy1 = highestSampledQuartY(heightAccessor);
        BiomeResolver column = biomeSource.createResolverForChunk(sampler, qx, qy0, qz, 1, qy1 - qy0 + 1, 1);
        boolean below = false;
        for (int qy = qy0; qy <= qy1; qy++) {
            boolean desert = column.getNoiseBiome(qx, qy, qz).is(Biomes.DESERT);
            if (desert && below) return true;
            below = desert;
        }
        COLUMN_PREFILTER_REJECTS.incrementAndGet();
        return false;
    }
}
