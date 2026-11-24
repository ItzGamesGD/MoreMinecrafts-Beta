package dev.itzgamesgd.moreminecrafts.pillagerhq.worldgen;

import java.util.Optional;
import java.util.function.Predicate;

/** Exhaustive N=2 search, with the same pass-site ordering as the V2 probe's integer score. */
public final class St03SiteSearch {
    private St03SiteSearch() {}
    public record Site(int originX, int originZ, int dxChunks, int dzChunks, St03Eligibility.Result result) {}

    public static Optional<Site> select(St03Eligibility.Terrain terrain, int candidateOriginX, int candidateOriginZ) {
        return selectWithDenseY(terrain, candidateOriginX, candidateOriginZ, site -> site.result().foundationY());
    }

    public static Optional<Site> select(St03Eligibility.Terrain terrain, int candidateOriginX, int candidateOriginZ, Predicate<Site> actualLimits) {
        return selectWithDenseY(terrain, candidateOriginX, candidateOriginZ,
                site -> actualLimits.test(site) ? site.result().foundationY() : Integer.MIN_VALUE);
    }

    public static Optional<Site> selectWithDenseY(St03Eligibility.Terrain terrain, int candidateOriginX, int candidateOriginZ,
                                                  java.util.function.ToIntFunction<Site> actualFoundationY) {
        Site best = null;
        int bestScore = Integer.MAX_VALUE;
        int n = St03PlacementConfig.SEARCH_RADIUS_CHUNKS;
        for (int dx = -n; dx <= n; dx++) {
            for (int dz = -n; dz <= n; dz++) {
                int x = candidateOriginX + dx * 16, z = candidateOriginZ + dz * 16;
                var result = St03Eligibility.evaluate(terrain, x, z);
                if (!result.eligible()) continue;
                int score = Math.max(result.cut(), Math.max(result.skirt(), result.footingFill())) * 100000
                        + result.rawHeightDifference() * 1000 + Math.max(Math.abs(dx), Math.abs(dz)) * 10;
                // Strict < preserves the probe's dx-then-dz iteration tie-break (including negative coordinates).
                if (score < bestScore) {
                    Site candidate = new Site(x, z, dx, dz, result);
                    int actualY = actualFoundationY.applyAsInt(candidate);
                    if (actualY == Integer.MIN_VALUE) continue;
                    if (actualY != result.foundationY()) {
                        result = new St03Eligibility.Result(result.verdict(), result.minHeight(), result.maxHeight(),
                                actualY, result.heightDifference(), result.cut(), result.skirt(), result.footingFill());
                        candidate = new Site(x, z, dx, dz, result);
                    }
                    bestScore = score;
                    best = candidate;
                }
            }
        }
        return Optional.ofNullable(best);
    }
}
