package dev.itzgamesgd.moreminecrafts.worldgenprobe.st01d3;

import dev.itzgamesgd.moreminecrafts.pyramid.St01PyramidAssembly.AccessRoute;
import dev.itzgamesgd.moreminecrafts.pyramid.St01PyramidAssembly.Orientation;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01Candidate;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01DecisionMemo;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01PlacementConfig;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01PyramidStructure;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01Eligibility;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01Worldgen;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;

/**
 * ST-01 P8-D3 verification support (test source set only): vanilla overworld generators for arbitrary seeds built from
 * the server's registries, the pre-P8-D3 reference terrain, and the 46 real-server starts of the P8-C2-C Policy D run
 * ({@code docs/design/st01-p8c2c/evidence/policy_d.json}).
 */
public final class St01D3Support {
    private St01D3Support() {}

    /** The exact 20 seeds of the P8-C2-B / P8-C2-C worldgen matrix. */
    public static final long[] SEEDS = {1001, 1002, 1003, 1004, 1005, 527213223, 1034165336, 1541117449, 2048069562, 407538027, 914490140,
            1421442253, 1928394366, 287862831, 794814944, 119599970, 218938829, 318277688, 417616547, 516955406};
    /** P8-C2-C window: chunk centres within +-10,240 blocks of the origin. */
    public static final int WINDOW_BLOCKS = 10240;

    public record KnownStart(long seed, int chunkX, int chunkZ, int groundX, int groundY, int groundZ, Orientation orientation, AccessRoute route) {}

    /** The 46 actual structure starts of the 20 seeds, verified on real 26.3 servers in P8-C2-C (chunk, ground, orientation, route). */
    public static final List<KnownStart> KNOWN_STARTS = List.of(
            new KnownStart(1001L, -477, -217, -7624, 69, -3464, Orientation.NORTH, AccessRoute.EAST),
            new KnownStart(1001L, -463, 458, -7400, 65, 7336, Orientation.EAST, AccessRoute.WEST),
            new KnownStart(1001L, -435, -279, -6952, 70, -4456, Orientation.WEST, AccessRoute.WEST),
            new KnownStart(1001L, 48, -434, 776, 69, -6936, Orientation.WEST, AccessRoute.WEST),
            new KnownStart(1002L, 44, -147, 712, 72, -2344, Orientation.SOUTH, AccessRoute.EAST),
            new KnownStart(1002L, 329, 527, 5272, 71, 8440, Orientation.NORTH, AccessRoute.WEST),
            new KnownStart(1002L, 366, -49, 5864, 69, -776, Orientation.SOUTH, AccessRoute.EAST),
            new KnownStart(1002L, 521, -210, 8344, 66, -3352, Orientation.SOUTH, AccessRoute.WEST),
            new KnownStart(1002L, 613, -172, 9816, 67, -2744, Orientation.WEST, AccessRoute.EAST),
            new KnownStart(1004L, -571, -501, -9128, 68, -8008, Orientation.WEST, AccessRoute.WEST),
            new KnownStart(1004L, -146, 522, -2328, 66, 8360, Orientation.NORTH, AccessRoute.EAST),
            new KnownStart(1004L, -111, 168, -1768, 70, 2696, Orientation.EAST, AccessRoute.WEST),
            new KnownStart(1005L, 7, 296, 120, 69, 4744, Orientation.EAST, AccessRoute.WEST),
            new KnownStart(119599970L, -467, 106, -7464, 63, 1704, Orientation.EAST, AccessRoute.WEST),
            new KnownStart(119599970L, -311, 559, -4968, 72, 8952, Orientation.SOUTH, AccessRoute.WEST),
            new KnownStart(119599970L, 10, 629, 168, 66, 10072, Orientation.NORTH, AccessRoute.EAST),
            new KnownStart(119599970L, 355, -15, 5688, 67, -232, Orientation.NORTH, AccessRoute.WEST),
            new KnownStart(218938829L, -406, -565, -6488, 69, -9032, Orientation.SOUTH, AccessRoute.EAST),
            new KnownStart(218938829L, 42, -597, 680, 70, -9544, Orientation.NORTH, AccessRoute.EAST),
            new KnownStart(287862831L, -503, -308, -8040, 67, -4920, Orientation.SOUTH, AccessRoute.WEST),
            new KnownStart(287862831L, 72, 207, 1160, 66, 3320, Orientation.NORTH, AccessRoute.EAST),
            new KnownStart(287862831L, 170, 233, 2728, 67, 3736, Orientation.WEST, AccessRoute.EAST),
            new KnownStart(287862831L, 336, -215, 5384, 68, -3432, Orientation.NORTH, AccessRoute.WEST),
            new KnownStart(318277688L, 532, 396, 8520, 68, 6344, Orientation.NORTH, AccessRoute.WEST),
            new KnownStart(318277688L, 560, 389, 8968, 72, 6232, Orientation.WEST, AccessRoute.EAST),
            new KnownStart(407538027L, -376, -362, -6008, 66, -5784, Orientation.NORTH, AccessRoute.EAST),
            new KnownStart(407538027L, -276, -256, -4408, 68, -4088, Orientation.SOUTH, AccessRoute.WEST),
            new KnownStart(407538027L, -210, -241, -3352, 68, -3848, Orientation.WEST, AccessRoute.EAST),
            new KnownStart(407538027L, -186, 588, -2968, 71, 9416, Orientation.EAST, AccessRoute.EAST),
            new KnownStart(417616547L, -591, 561, -9448, 67, 8984, Orientation.EAST, AccessRoute.EAST),
            new KnownStart(417616547L, 549, -45, 8792, 66, -712, Orientation.NORTH, AccessRoute.EAST),
            new KnownStart(417616547L, 586, -53, 9384, 75, -840, Orientation.NORTH, AccessRoute.EAST),
            new KnownStart(516955406L, 197, 260, 3160, 71, 4168, Orientation.NORTH, AccessRoute.EAST),
            new KnownStart(527213223L, -120, -301, -1912, 68, -4808, Orientation.EAST, AccessRoute.WEST),
            new KnownStart(527213223L, 622, -460, 9960, 65, -7352, Orientation.NORTH, AccessRoute.WEST),
            new KnownStart(794814944L, 303, -241, 4856, 67, -3848, Orientation.NORTH, AccessRoute.WEST),
            new KnownStart(914490140L, -468, 489, -7480, 64, 7832, Orientation.WEST, AccessRoute.WEST),
            new KnownStart(914490140L, 431, 230, 6904, 65, 3688, Orientation.EAST, AccessRoute.WEST),
            new KnownStart(1034165336L, 179, -213, 2872, 68, -3400, Orientation.EAST, AccessRoute.EAST),
            new KnownStart(1541117449L, -408, 486, -6520, 63, 7784, Orientation.SOUTH, AccessRoute.WEST),
            new KnownStart(1541117449L, 109, -319, 1752, 68, -5096, Orientation.NORTH, AccessRoute.WEST),
            new KnownStart(1541117449L, 301, 198, 4824, 71, 3176, Orientation.WEST, AccessRoute.EAST),
            new KnownStart(1541117449L, 328, 176, 5256, 65, 2824, Orientation.EAST, AccessRoute.EAST),
            new KnownStart(1928394366L, -497, 430, -7944, 68, 6888, Orientation.SOUTH, AccessRoute.WEST),
            new KnownStart(1928394366L, -84, -337, -1336, 70, -5384, Orientation.NORTH, AccessRoute.EAST),
            new KnownStart(2048069562L, 102, 483, 1640, 73, 7736, Orientation.NORTH, AccessRoute.WEST)
    );

    /** A vanilla overworld (default preset) for one seed, independent of the level it is built from. */
    public static final class World {
        public final RegistryAccess access;
        public final long seed;
        public final NoiseBasedChunkGenerator generator;
        public final BiomeSource biomeSource;
        public final RandomState randomState;
        public final LevelHeightAccessor height;
        public final Structure st01;
        public final RandomSpreadStructurePlacement placement = new RandomSpreadStructurePlacement(St01PlacementConfig.SPACING_CHUNKS,
                St01PlacementConfig.SEPARATION_CHUNKS, RandomSpreadType.TRIANGULAR, St01PlacementConfig.SALT);

        public World(RegistryAccess access, long seed, LevelHeightAccessor height) {
            this.access = access;
            this.seed = seed;
            this.height = height;
            this.biomeSource = MultiNoiseBiomeSource.createFromPreset(access.lookupOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
                    .getOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD));
            Holder<NoiseGeneratorSettings> settings = access.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(NoiseGeneratorSettings.OVERWORLD);
            this.generator = new NoiseBasedChunkGenerator(biomeSource, settings);
            this.randomState = RandomState.create(access.lookupOrThrow(Registries.NOISE), seed, settings.value());
            this.st01 = access.lookupOrThrow(Registries.STRUCTURE).getValue(St01Worldgen.STRUCTURE_ID);
        }

        public World(RegistryAccess access, long seed) { this(access, seed, LevelHeightAccessor.create(-64, 384)); }

        public Climate.Sampler newSampler() {
            return randomState.createClimateSampler(SamplerContext.builder().enableCaches().build());
        }

        /** The context vanilla's StructureCheck builds for a candidate chunk (fresh climate sampler per call). */
        public Structure.GenerationContext context(ChunkPos chunk) {
            return new Structure.GenerationContext(access, generator, biomeSource, newSampler(), randomState, null, seed, chunk, height,
                    h -> st01.biomes().contains(h));
        }

        public ChunkPos candidate(int regionX, int regionZ) {
            return placement.getPotentialStructureChunk(seed, regionX * placement.spacing(), regionZ * placement.spacing());
        }

        public int legacyHeight(int x, int z) {
            return generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, height, randomState);
        }
    }

    /** The pre-P8-D3 production terrain, verbatim: per-column getBaseHeight (memoised), point biome resolver, no prefilter. */
    public static final class LegacyTerrain implements St01Eligibility.Terrain {
        private final Structure.GenerationContext context;
        private final Map<Long, Integer> heights = new HashMap<>();
        public int heightCalls;

        public LegacyTerrain(Structure.GenerationContext context) { this.context = context; }

        @Override public int surfaceHeight(int x, int z) {
            return heights.computeIfAbsent(((long) x << 32) ^ (z & 0xFFFFFFFFL), k -> {
                heightCalls++;
                return context.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
            });
        }

        @Override public boolean isDesertCell(int qx, int qy, int qz) {
            return context.biomeResolver().getNoiseBiome(qx, qy, qz).is(Biomes.DESERT);
        }
    }

    /** Production decision (P8-D3 terrain), exactly as {@code St01PyramidStructure.findGenerationPoint} computes it. */
    public static St01Candidate.Decision fastDecision(World w, ChunkPos chunk) {
        Structure.GenerationContext ctx = w.context(chunk);
        return St01Candidate.decide(new St01PyramidStructure.GeneratorTerrain(ctx), chunk, Rotation.getRandom(ctx.random()), w.seed);
    }

    /** Reference decision on the pre-P8-D3 terrain. */
    public static St01Candidate.Decision legacyDecision(World w, ChunkPos chunk) {
        Structure.GenerationContext ctx = w.context(chunk);
        return St01Candidate.decide(new LegacyTerrain(ctx), chunk, Rotation.getRandom(ctx.random()), w.seed);
    }

    /** Production decision through the P8-D4 memo, exactly as {@code St01PyramidStructure.findGenerationPoint} obtains it. */
    public static St01Candidate.Decision memoDecision(World w, ChunkPos chunk) {
        Structure.GenerationContext ctx = w.context(chunk);
        Rotation rotation = Rotation.getRandom(ctx.random());
        return St01DecisionMemo.decide(ctx, rotation, () -> St01Candidate.decide(new St01PyramidStructure.GeneratorTerrain(ctx), chunk, rotation, w.seed));
    }

    public static boolean memoHit(World w, ChunkPos chunk) {
        return St01DecisionMemo.contains(w.context(chunk));
    }

    public static String describe(St01Candidate.Decision d) {
        return d.result() + " ground=" + d.ground().map(Object::toString).orElse("-") + " " + d.orientation() + " " + d.route().map(Enum::name).orElse("-");
    }
}
