package dev.itzgamesgd.moreminecrafts.pyramid.worldgen;

import java.util.Arrays;

/**
 * Deterministic, bounded candidate qualification for one ST-01 anchor. Pure logic over a {@link Terrain} view, so the identical
 * code runs in production (real chunk generator), in unit tests (synthetic terrain) and in the validation probe.
 *
 * <p>Stages are evaluated in this order and reported distinctly: (1) full-footprint Desert, (2) MODERATE terrain
 * (height difference, cut, skirt), (3) underground fluid safety. A rejection has no side effects.
 *
 * <p>Definitions are the repository's established O-14 definitions: heights are {@code WORLD_SURFACE_WG} base heights on the
 * {@value St01PlacementConfig#HEIGHT_SAMPLE_STEP}-block grid across the 81x81 footprint; height difference = max - min; foundation
 * level {@code yf} = median height; cut = max(0, max - yf); skirt = max(0, yf - min).
 *
 * <p>Biome coverage: Minecraft resolves the biome of block (x,y,z) through {@code BiomeManager}, which returns one of the eight
 * quart cells {@code floor((x-2)/4)+{0,1}} x {@code floor((y-2)/4)+{0,1}} x {@code floor((z-2)/4)+{0,1}} (the jittered Voronoi
 * choice always lies in that 2x2x2 neighbourhood). Requiring every such cell around every footprint column to be Desert therefore
 * guarantees every footprint block resolves to Desert, whatever the per-world jitter is.
 */
public final class St01Eligibility {
    private St01Eligibility() {}

    /** Read-only terrain view. Implementations must be deterministic and must not trigger chunk loading. */
    public interface Terrain {
        /** {@code WORLD_SURFACE_WG} base height at the block column (first free Y above the surface). */
        int surfaceHeight(int x, int z);

        /** Whether the noise biome of the quart cell is Desert. */
        boolean isDesertCell(int quartX, int quartY, int quartZ);

        /** Whether any generated base-terrain block in [minY, maxY] of the column holds fluid (aquifer water/lava, noise-cave fluid). */
        boolean hasFluid(int x, int z, int minY, int maxY);
    }

    /** Underground volume the structure occupies, relative to the anchor column (dx, dz) and foundation level (dy). Inclusive. */
    public record Volume(int minDx, int maxDx, int minDy, int maxDy, int minDz, int maxDz) {}

    public enum Verdict {
        ELIGIBLE,
        REJECT_BIOME,
        REJECT_HEIGHT_DIFFERENCE,
        REJECT_CUT,
        REJECT_SKIRT,
        REJECT_FLUID
    }

    public record Result(Verdict verdict, int minHeight, int maxHeight, int foundationY, int heightDifference, int cut, int skirt) {
        public boolean eligible() { return verdict == Verdict.ELIGIBLE; }
        public boolean biomeQualified() { return verdict != Verdict.REJECT_BIOME; }
        public boolean terrainQualified() {
            return verdict == Verdict.ELIGIBLE || verdict == Verdict.REJECT_FLUID;
        }
    }

    private static final Result BIOME_REJECT = new Result(Verdict.REJECT_BIOME, 0, 0, 0, 0, 0, 0);

    public static Result evaluate(Terrain terrain, int centerX, int centerZ, Volume underground) {
        if (!fullFootprintDesert(terrain, centerX, centerZ)) return BIOME_REJECT;

        int half = St01PlacementConfig.FOOTPRINT_HALF_WIDTH;
        int step = St01PlacementConfig.HEIGHT_SAMPLE_STEP;
        int perAxis = 2 * half / step + 1;
        int[] heights = new int[perAxis * perAxis];
        int n = 0;
        for (int dx = -half; dx <= half; dx += step) {
            for (int dz = -half; dz <= half; dz += step) heights[n++] = terrain.surfaceHeight(centerX + dx, centerZ + dz);
        }
        Arrays.sort(heights);
        int min = heights[0], max = heights[heights.length - 1];
        int yf = heights[heights.length / 2];
        int diff = max - min, cut = Math.max(0, max - yf), skirt = Math.max(0, yf - min);

        if (diff > St01PlacementConfig.MAX_HEIGHT_DIFFERENCE) return new Result(Verdict.REJECT_HEIGHT_DIFFERENCE, min, max, yf, diff, cut, skirt);
        if (cut > St01PlacementConfig.MAX_CUT) return new Result(Verdict.REJECT_CUT, min, max, yf, diff, cut, skirt);
        if (skirt > St01PlacementConfig.MAX_SKIRT) return new Result(Verdict.REJECT_SKIRT, min, max, yf, diff, cut, skirt);

        // The vanilla stub check samples the biome at the stub position (anchor column, foundation level); require the same cell here so a
        // stub accepted by this class cannot be silently dropped by Structure.GenerationContext.isValidBiome.
        if (!terrain.isDesertCell(Math.floorDiv(centerX, 4), Math.floorDiv(yf, 4), Math.floorDiv(centerZ, 4))) {
            return new Result(Verdict.REJECT_BIOME, min, max, yf, diff, cut, skirt);
        }

        if (underground != null && fluidIntersects(terrain, centerX, centerZ, yf, underground)) {
            return new Result(Verdict.REJECT_FLUID, min, max, yf, diff, cut, skirt);
        }
        return new Result(Verdict.ELIGIBLE, min, max, yf, diff, cut, skirt);
    }

    /** Every quart cell that any footprint block can resolve to must be Desert (see class comment). */
    public static boolean fullFootprintDesert(Terrain terrain, int centerX, int centerZ) {
        int half = St01PlacementConfig.FOOTPRINT_HALF_WIDTH;
        int qx0 = Math.floorDiv(centerX - half - 2, 4), qx1 = Math.floorDiv(centerX + half - 2, 4) + 1;
        int qz0 = Math.floorDiv(centerZ - half - 2, 4), qz1 = Math.floorDiv(centerZ + half - 2, 4) + 1;
        // Cheap early exit on the centre cell before the full sweep.
        if (!cellColumnDesert(terrain, Math.floorDiv(centerX, 4), Math.floorDiv(centerZ, 4))) return false;
        for (int qx = qx0; qx <= qx1; qx++) {
            for (int qz = qz0; qz <= qz1; qz++) {
                if (!cellColumnDesert(terrain, qx, qz)) return false;
            }
        }
        return true;
    }

    /** Desert for both vertical quart candidates of the surface block of the cell's centre column. */
    private static boolean cellColumnDesert(Terrain terrain, int qx, int qz) {
        int surfaceBlockY = terrain.surfaceHeight(qx * 4 + 2, qz * 4 + 2) - 1;
        int qy = Math.floorDiv(surfaceBlockY - 2, 4);
        return terrain.isDesertCell(qx, qy, qz) && terrain.isDesertCell(qx, qy + 1, qz);
    }

    private static boolean fluidIntersects(Terrain terrain, int cx, int cz, int yf, Volume v) {
        int m = St01PlacementConfig.FLUID_MARGIN;
        for (int x = cx + v.minDx() - m; x <= cx + v.maxDx() + m; x++) {
            for (int z = cz + v.minDz() - m; z <= cz + v.maxDz() + m; z++) {
                if (terrain.hasFluid(x, z, yf + v.minDy() - m, yf + v.maxDy() + m)) return true;
            }
        }
        return false;
    }
}
