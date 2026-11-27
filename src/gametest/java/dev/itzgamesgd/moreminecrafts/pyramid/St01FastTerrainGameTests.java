package dev.itzgamesgd.moreminecrafts.pyramid;

import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01Candidate;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01FastTerrain;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01PyramidStructure;
import dev.itzgamesgd.moreminecrafts.worldgenprobe.st01d3.St01D3Support;
import dev.itzgamesgd.moreminecrafts.worldgenprobe.st01d3.St01D3Support.KnownStart;
import dev.itzgamesgd.moreminecrafts.worldgenprobe.st01d3.St01D3Support.World;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * ST-01 P8-D3: C1 (column biome prefilter) and C2 (batched surface heights) are exact. Every assertion compares the new
 * path with the unchanged per-column reference on real Minecraft 26.3 overworld generators built for fixed seeds; any
 * difference fails the test. The extended 20-seed / 341,172-column regression is {@code St01D3Probe} mode
 * {@code regression}.
 */
public final class St01FastTerrainGameTests {

    private static int[] chunkHeightsLegacy(World w, LevelHeightAccessor h, int cx, int cz) {
        int[] out = new int[256];
        for (int ix = 0; ix < 16; ix++) for (int iz = 0; iz < 16; iz++)
            out[ix * 16 + iz] = w.generator.getBaseHeight(cx * 16 + ix, cz * 16 + iz, net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, h, w.randomState);
        return out;
    }

    /** Chunks covering negative coordinates, chunk borders, far coordinates, relief extremes and biome borders. */
    private static List<int[]> boundaryChunks(World w, int[] knownStartChunk) {
        Set<String> seen = new LinkedHashSet<>();
        List<int[]> out = new ArrayList<>();
        java.util.function.BiConsumer<Integer, Integer> add = (x, z) -> { if (seen.add(x + "," + z)) out.add(new int[]{x, z}); };
        add.accept(-1, -1); add.accept(-1, 0); add.accept(0, -1); add.accept(0, 0);
        add.accept(-1875, -1875); add.accept(1874, -1875); add.accept(-1875, 1874); add.accept(1874, 1874);
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) add.accept(knownStartChunk[0] + dx, knownStartChunk[1] + dz);
        // relief extremes and a biome border from a coarse scan of chunk centres
        int best = Integer.MIN_VALUE, worst = Integer.MAX_VALUE, steep = -1;
        int[] hi = null, lo = null, st = null, border = null;
        BiomeResolver point = w.biomeSource.createResolver(w.newSampler());
        for (int i = -12; i < 12; i++) for (int j = -12; j < 12; j++) {
            int cx = i * 40, cz = j * 40;
            int h = w.legacyHeight(cx * 16 + 8, cz * 16 + 8), h2 = w.legacyHeight(cx * 16 + 15, cz * 16 + 15);
            if (h > best) { best = h; hi = new int[]{cx, cz}; }
            if (h < worst) { worst = h; lo = new int[]{cx, cz}; }
            if (Math.abs(h - h2) > steep) { steep = Math.abs(h - h2); st = new int[]{cx, cz}; }
            if (border == null) {
                Holder<Biome> a = point.getNoiseBiome(cx * 4, (h - 1) >> 2, cz * 4), b = point.getNoiseBiome(cx * 4 + 3, (h2 - 1) >> 2, cz * 4 + 3);
                if (!a.equals(b)) border = new int[]{cx, cz};
            }
        }
        add.accept(hi[0], hi[1]); add.accept(lo[0], lo[1]); add.accept(st[0], st[1]);
        if (border != null) add.accept(border[0], border[1]);
        return out;
    }

    @GameTest(maxTicks = 400)
    public void batchedHeightsEqualGetBaseHeight(GameTestHelper ctx) {
        int columns = 0, mismatches = 0;
        String first = null;
        for (KnownStart s : new KnownStart[]{St01D3Support.KNOWN_STARTS.stream().filter(k -> k.seed() == 1004L).findFirst().orElseThrow(),
                St01D3Support.KNOWN_STARTS.stream().filter(k -> k.seed() == 1001L).findFirst().orElseThrow()}) {
            World w = new World(ctx.getLevel().registryAccess(), s.seed());
            List<int[]> chunks = boundaryChunks(w, new int[]{s.chunkX(), s.chunkZ()});
            List<LevelHeightAccessor> accessors = List.of(w.height, LevelHeightAccessor.create(0, 128), LevelHeightAccessor.create(320, 64),
                    LevelHeightAccessor.create(-64, 8));
            for (int a = 0; a < accessors.size(); a++) {
                LevelHeightAccessor h = accessors.get(a);
                List<int[]> set = a == 0 ? chunks : chunks.subList(0, 6);
                for (int[] c : set) {
                    int[] fast = St01FastTerrain.fillChunk(w.generator, h, w.randomState, c[0] * 16, c[1] * 16);
                    int[] legacy = chunkHeightsLegacy(w, h, c[0], c[1]);
                    for (int i = 0; i < 256; i++) {
                        columns++;
                        if (fast[i] != legacy[i]) {
                            mismatches++;
                            if (first == null) first = "seed " + s.seed() + " accessor " + h.getMinY() + "+" + h.getHeight() + " chunk " + c[0] + "," + c[1]
                                    + " column " + (i / 16) + "," + (i % 16) + ": batched " + fast[i] + " vs getBaseHeight " + legacy[i];
                        }
                    }
                }
            }
        }
        ctx.assertTrue(mismatches == 0, mismatches + " of " + columns + " columns differ; first: " + first);
        ctx.assertTrue(columns >= 19000, "too few columns compared: " + columns);
        ctx.succeed();
    }

    @GameTest(maxTicks = 400)
    public void columnResolverEqualsPointResolver(GameTestHelper ctx) {
        long cells = 0, mismatches = 0;
        String first = null;
        for (long seed : new long[]{1004L, 1001L}) {
            World w = new World(ctx.getLevel().registryAccess(), seed);
            Climate.Sampler sampler = w.newSampler();
            BiomeResolver point = w.biomeSource.createResolver(sampler);
            int qy0 = Math.floorDiv(w.height.getMinY() - 3, 4), qy1 = Math.floorDiv(w.height.getMaxY() - 2, 4) + 1;
            for (int i = -30; i < 30; i++) for (int j = -30; j < 30; j++) {
                int qx = i * 37 + 3, qz = j * 37 - 5;
                BiomeResolver column = w.biomeSource.createResolverForChunk(sampler, qx, qy0, qz, 1, qy1 - qy0 + 1, 1);
                for (int qy = qy0; qy <= qy1; qy++) {
                    cells++;
                    Holder<Biome> a = column.getNoiseBiome(qx, qy, qz), b = point.getNoiseBiome(qx, qy, qz);
                    if (!a.equals(b)) { mismatches++; if (first == null) first = seed + " " + qx + "," + qy + "," + qz + ": " + a + " vs " + b; }
                }
            }
        }
        ctx.assertTrue(mismatches == 0, mismatches + " of " + cells + " quart cells differ; first: " + first);
        ctx.succeed();
    }

    @GameTest(maxTicks = 400)
    public void prefilterIsNecessaryForEveryCentreCell(GameTestHelper ctx) {
        World w = new World(ctx.getLevel().registryAccess(), 1004L);
        Climate.Sampler sampler = w.newSampler();
        BiomeResolver point = w.biomeSource.createResolver(sampler);
        int candidates = 0, centreDesert = 0, rejected = 0, violations = 0;
        for (int rx = -20; rx < 20; rx++) for (int rz = -20; rz < 20; rz++) {
            ChunkPos c = w.candidate(rx, rz);
            int qx = Math.floorDiv(c.getMinBlockX() + 8, 4), qz = Math.floorDiv(c.getMinBlockZ() + 8, 4);
            int surface = w.legacyHeight(qx * 4 + 2, qz * 4 + 2) - 1, qy = Math.floorDiv(surface - 2, 4);
            boolean legacy = point.getNoiseBiome(qx, qy, qz).is(Biomes.DESERT) && point.getNoiseBiome(qx, qy + 1, qz).is(Biomes.DESERT);
            boolean possible = St01FastTerrain.desertPairPossible(w.biomeSource, sampler, w.height, qx, qz);
            candidates++;
            if (legacy) centreDesert++;
            if (!possible) rejected++;
            if (legacy && !possible) violations++;
        }
        ctx.assertTrue(violations == 0, violations + " centre cells pass the height-based Desert test but were rejected by the prefilter");
        ctx.assertTrue(centreDesert > 0 && rejected > candidates / 2, "coverage: " + centreDesert + " desert, " + rejected + " rejected of " + candidates);
        ctx.succeed();
    }

    @GameTest(maxTicks = 800)
    public void decisionsEqualLegacyAndRealServerStarts(GameTestHelper ctx) {
        long mismatchesBefore = St01FastTerrain.SPOT_CHECK_MISMATCHES.get(), fillsBefore = St01FastTerrain.BATCHED_CHUNK_FILLS.get();
        long rejectsBefore = St01FastTerrain.COLUMN_PREFILTER_REJECTS.get();
        int compared = 0, eligible = 0;
        for (long seed : new long[]{1004L, 1005L}) {
            World w = new World(ctx.getLevel().registryAccess(), seed);
            List<ChunkPos> chunks = new ArrayList<>();
            for (int rx = -6; rx < 6; rx++) for (int rz = -6; rz < 6; rz++) chunks.add(w.candidate(rx, rz));
            for (KnownStart s : St01D3Support.KNOWN_STARTS) if (s.seed() == seed) chunks.add(new ChunkPos(s.chunkX(), s.chunkZ()));
            for (ChunkPos c : chunks) {
                St01Candidate.Decision fast = St01D3Support.fastDecision(w, c), legacy = St01D3Support.legacyDecision(w, c);
                compared++;
                ctx.assertTrue(fast.equals(legacy), "seed " + seed + " chunk " + c.x() + "," + c.z() + ": fast " + St01D3Support.describe(fast)
                        + " vs legacy " + St01D3Support.describe(legacy));
                if (fast.result().eligible()) eligible++;
            }
            for (KnownStart s : St01D3Support.KNOWN_STARTS) {
                if (s.seed() != seed) continue;
                St01Candidate.Decision d = St01D3Support.fastDecision(w, new ChunkPos(s.chunkX(), s.chunkZ()));
                ctx.assertTrue(d.result().eligible() && d.ground().orElseThrow().equals(new BlockPos(s.groundX(), s.groundY(), s.groundZ()))
                        && d.orientation() == s.orientation() && d.route().orElseThrow() == s.route(),
                        "real-server start " + s + " is not reproduced: " + St01D3Support.describe(d));
            }
        }
        ctx.assertTrue(St01FastTerrain.SPOT_CHECK_MISMATCHES.get() == mismatchesBefore, "batched spot check reported a mismatch");
        ctx.assertTrue(St01FastTerrain.BATCHED_CHUNK_FILLS.get() > fillsBefore, "the batched path was not used");
        ctx.assertTrue(St01FastTerrain.COLUMN_PREFILTER_REJECTS.get() > rejectsBefore, "the prefilter was not used");
        ctx.assertTrue(eligible >= 4, "the four real starts of seeds 1004/1005 must be eligible: " + eligible + " of " + compared);
        ctx.succeed();
    }

    @GameTest(maxTicks = 100)
    public void otherGeneratorsAndBiomeSourcesKeepTheLegacyPath(GameTestHelper ctx) {
        var levelGenerator = ctx.getLevel().getChunkSource().getGenerator();
        if (levelGenerator.getClass() != NoiseBasedChunkGenerator.class)
            ctx.assertTrue(!St01FastTerrain.batchedApplies(levelGenerator), "a non-vanilla-noise generator must use per-column heights");
        Holder<Biome> desert = ctx.getLevel().registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.DESERT);
        ctx.assertTrue(!St01FastTerrain.prefilterApplies(new FixedBiomeSource(desert)), "a non-multi-noise biome source must not be prefiltered");
        World w = new World(ctx.getLevel().registryAccess(), 1004L);
        ctx.assertTrue(St01FastTerrain.batchedApplies(w.generator) && St01FastTerrain.prefilterApplies(w.biomeSource), "vanilla overworld must use C1 and C2");
        String previous = System.getProperty("mm.st01.terrain.legacy");
        System.setProperty("mm.st01.terrain.legacy", "true");
        try {
            ctx.assertTrue(!St01FastTerrain.batchedApplies(w.generator) && !St01FastTerrain.prefilterApplies(w.biomeSource), "the legacy switch must disable C1 and C2");
            ChunkPos start = new ChunkPos(-111, 168);
            Structure.GenerationContext c = w.context(start);
            long fills = St01FastTerrain.BATCHED_CHUNK_FILLS.get();
            St01Candidate.Decision forced = St01Candidate.decide(new St01PyramidStructure.GeneratorTerrain(c), start,
                    net.minecraft.world.level.block.Rotation.getRandom(c.random()), w.seed);
            ctx.assertTrue(St01FastTerrain.BATCHED_CHUNK_FILLS.get() == fills, "the legacy switch must not fill chunks");
            ctx.assertTrue(forced.equals(St01D3Support.legacyDecision(w, start)), "the forced legacy path must equal the reference");
        } finally {
            if (previous == null) System.clearProperty("mm.st01.terrain.legacy"); else System.setProperty("mm.st01.terrain.legacy", previous);
        }
        ctx.succeed();
    }
}
