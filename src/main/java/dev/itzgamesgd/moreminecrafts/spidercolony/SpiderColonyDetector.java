package dev.itzgamesgd.moreminecrafts.spidercolony;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Exact Java implementation of the Stage 1 spider-colony detection criteria
 * ({@code tools/spider-colony-probe/analysis.py}, {@code dedup.py}, {@code structures.py},
 * and {@code SpiderColonyProbe.java}):
 * <ul>
 *   <li>6-connected traversable air components (fluids excluded) in {@code y in [-48, 48]}</li>
 *   <li>3D Euclidean distance transform with sample-boundary padding ({@code clearance >= 10} blocks)</li>
 *   <li>Inscribed sphere center (maximum clearance voxel per core component)</li>
 *   <li>Overlap deduplication within the same 6-connected air component</li>
 *   <li>16-block structure bounding-box buffer exclusion (including 1-chunk halo references)</li>
 * </ul>
 */
public final class SpiderColonyDetector {
    public static final int WINDOW_SIDE_CHUNKS = 8;
    public static final int WINDOW_SIDE_BLOCKS = WINDOW_SIDE_CHUNKS * 16; // 128
    public static final int CHUNK_HALO = 1;
    public static final int Y_MIN = -48;
    public static final int Y_MAX = 48;
    public static final int Y_SIZE = Y_MAX - Y_MIN + 1; // 97
    public static final double CORE_MIN_RADIUS_BLOCKS = 10.0;
    public static final int STRUCTURE_BUFFER_BLOCKS = 16;

    private static final double INF = 1e12;
    private static final int[] DX = {1, -1, 0, 0, 0, 0};
    private static final int[] DZ = {0, 0, 1, -1, 0, 0};
    private static final int[] DY = {0, 0, 0, 0, 1, -1};

    private SpiderColonyDetector() {}

    public record Box3i(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        public Box3i expand(int buffer) {
            return new Box3i(
                    minX - buffer, minY - buffer, minZ - buffer,
                    maxX + buffer, maxY + buffer, maxZ + buffer);
        }

        /** Matches {@code tools/spider-colony-probe/structures.py#intersects}. */
        public boolean intersectsSphere(int px, int py, int pz, double radius) {
            double dx = Math.max(0, Math.max(minX - px, px - maxX));
            double dy = Math.max(0, Math.max(minY - py, py - maxY));
            double dz = Math.max(0, Math.max(minZ - pz, pz - maxZ));
            return (dx * dx + dy * dy + dz * dz) <= radius * radius;
        }

        public boolean containsPoint(int px, int py, int pz) {
            return px >= minX && px <= maxX && py >= minY && py <= maxY && pz >= minZ && pz <= maxZ;
        }
    }

    public record CoreCandidate(
            int worldX,
            int worldY,
            int worldZ,
            int airComponentId,
            double diameterBlocks,
            boolean componentTouchesBoundary) {
        public double radiusBlocks() {
            return diameterBlocks * 0.5;
        }
    }

    public record DetectionResult(List<CoreCandidate> candidates, int[][][] airLabels) {}

    /**
     * Detects qualifying colony chamber cores in an {@code side x side x Y_SIZE} air voxel grid.
     *
     * @param air {@code air[lx][lz][ly]} where {@code ly = worldY - Y_MIN}
     * @param side horizontal side length in blocks (typically {@link #WINDOW_SIDE_BLOCKS} = 128)
     * @param originWorldX world X of {@code lx = 0}
     * @param originWorldZ world Z of {@code lz = 0}
     * @param structureBoxes all structure bounding boxes in the window plus {@link #CHUNK_HALO}
     * @return surviving, deduplicated, structure-buffered core candidates sorted by diameter descending
     */
    public static List<CoreCandidate> detect(
            boolean[][][] air,
            int side,
            int originWorldX,
            int originWorldZ,
            List<Box3i> structureBoxes) {
        return detectWithLabels(air, side, originWorldX, originWorldZ, structureBoxes).candidates();
    }

    public static DetectionResult detectWithLabels(
            boolean[][][] air,
            int side,
            int originWorldX,
            int originWorldZ,
            List<Box3i> structureBoxes) {
        int[][][] airLabels = labelSixConnected(air, side, Y_SIZE);
        Set<Integer> boundaryIds = boundaryComponentIds(airLabels, side, Y_SIZE);
        double[][][] distance = euclideanClearance(air, side, Y_SIZE);

        boolean[][][] coreMask = new boolean[side][side][Y_SIZE];
        boolean anyCore = false;
        for (int x = 0; x < side; x++) {
            for (int z = 0; z < side; z++) {
                for (int y = 0; y < Y_SIZE; y++) {
                    if (distance[x][z][y] >= CORE_MIN_RADIUS_BLOCKS) {
                        coreMask[x][z][y] = true;
                        anyCore = true;
                    }
                }
            }
        }
        if (!anyCore) return new DetectionResult(List.of(), airLabels);

        int[][][] coreLabels = labelSixConnected(coreMask, side, Y_SIZE);
        Map<Integer, CoreCandidate> bestPerCore = new HashMap<>();
        for (int x = 0; x < side; x++) {
            for (int z = 0; z < side; z++) {
                for (int y = 0; y < Y_SIZE; y++) {
                    int cid = coreLabels[x][z][y];
                    if (cid == 0) continue;
                    double d = distance[x][z][y];
                    CoreCandidate prev = bestPerCore.get(cid);
                    if (prev == null || (2.0 * d) > prev.diameterBlocks()) {
                        int airId = airLabels[x][z][y];
                        bestPerCore.put(cid, new CoreCandidate(
                                originWorldX + x,
                                Y_MIN + y,
                                originWorldZ + z,
                                airId,
                                2.0 * d,
                                boundaryIds.contains(airId)));
                    }
                }
            }
        }

        List<CoreCandidate> merged = mergeOverlappingCores(new ArrayList<>(bestPerCore.values()));
        List<Box3i> expandedBoxes = new ArrayList<>(structureBoxes.size());
        for (Box3i box : structureBoxes) {
            expandedBoxes.add(box.expand(STRUCTURE_BUFFER_BLOCKS));
        }

        List<CoreCandidate> surviving = new ArrayList<>();
        for (CoreCandidate c : merged) {
            boolean blocked = false;
            for (Box3i box : expandedBoxes) {
                if (box.intersectsSphere(c.worldX(), c.worldY(), c.worldZ(), c.radiusBlocks())) {
                    blocked = true;
                    break;
                }
            }
            if (!blocked) {
                surviving.add(c);
            }
        }
        surviving.sort(Comparator.comparingDouble(CoreCandidate::diameterBlocks).reversed()
                .thenComparingInt(CoreCandidate::worldX)
                .thenComparingInt(CoreCandidate::worldZ)
                .thenComparingInt(CoreCandidate::worldY));
        return new DetectionResult(surviving, airLabels);
    }

    /** Matches {@code tools/spider-colony-probe/dedup.py#merge_overlapping_cores}. */
    public static List<CoreCandidate> mergeOverlappingCores(List<CoreCandidate> cores) {
        int n = cores.size();
        if (n <= 1) return cores;
        int[] parent = new int[n];
        for (int i = 0; i < n; i++) parent[i] = i;

        for (int i = 0; i < n; i++) {
            CoreCandidate a = cores.get(i);
            for (int j = 0; j < i; j++) {
                CoreCandidate b = cores.get(j);
                if (a.airComponentId() != b.airComponentId()) continue;
                double dx = a.worldX() - b.worldX();
                double dy = a.worldY() - b.worldY();
                double dz = a.worldZ() - b.worldZ();
                double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (dist <= (a.diameterBlocks() + b.diameterBlocks()) * 0.5) {
                    parent[findRoot(parent, i)] = findRoot(parent, j);
                }
            }
        }

        Map<Integer, CoreCandidate> groups = new HashMap<>();
        for (int i = 0; i < n; i++) {
            int r = findRoot(parent, i);
            CoreCandidate c = cores.get(i);
            CoreCandidate cur = groups.get(r);
            if (cur == null || c.diameterBlocks() > cur.diameterBlocks()) {
                groups.put(r, c);
            }
        }
        return new ArrayList<>(groups.values());
    }

    private static int findRoot(int[] parent, int i) {
        while (parent[i] != i) {
            parent[i] = parent[parent[i]];
            i = parent[i];
        }
        return i;
    }

    /** 6-connected component labeling matching {@code analysis.py#components}. */
    static int[][][] labelSixConnected(boolean[][][] mask, int side, int ySize) {
        int[][][] labels = new int[side][side][ySize];
        int nextId = 0;
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        int strideZ = ySize;
        int strideX = side * ySize;

        for (int x = 0; x < side; x++) {
            for (int z = 0; z < side; z++) {
                for (int y = 0; y < ySize; y++) {
                    if (!mask[x][z][y] || labels[x][z][y] != 0) continue;
                    int id = ++nextId;
                    labels[x][z][y] = id;
                    queue.addLast(x * strideX + z * strideZ + y);
                    while (!queue.isEmpty()) {
                        int idx = queue.removeFirst();
                        int cx = idx / strideX;
                        int rem = idx % strideX;
                        int cz = rem / strideZ;
                        int cy = rem % strideZ;
                        for (int d = 0; d < 6; d++) {
                            int nx = cx + DX[d];
                            int nz = cz + DZ[d];
                            int ny = cy + DY[d];
                            if (nx < 0 || nx >= side || nz < 0 || nz >= side || ny < 0 || ny >= ySize) continue;
                            if (!mask[nx][nz][ny] || labels[nx][nz][ny] != 0) continue;
                            labels[nx][nz][ny] = id;
                            queue.addLast(nx * strideX + nz * strideZ + ny);
                        }
                    }
                }
            }
        }
        return labels;
    }

    /** Matches {@code analysis.py#boundary_component_ids}. */
    static Set<Integer> boundaryComponentIds(int[][][] labels, int side, int ySize) {
        Set<Integer> ids = new HashSet<>();
        for (int x = 0; x < side; x++) {
            for (int z = 0; z < side; z++) {
                addNonZero(ids, labels[x][z][0]);
                addNonZero(ids, labels[x][z][ySize - 1]);
            }
        }
        for (int x = 0; x < side; x++) {
            for (int y = 0; y < ySize; y++) {
                addNonZero(ids, labels[x][0][y]);
                addNonZero(ids, labels[x][side - 1][y]);
            }
        }
        for (int z = 0; z < side; z++) {
            for (int y = 0; y < ySize; y++) {
                addNonZero(ids, labels[0][z][y]);
                addNonZero(ids, labels[side - 1][z][y]);
            }
        }
        return ids;
    }

    private static void addNonZero(Set<Integer> set, int v) {
        if (v != 0) set.add(v);
    }

    /**
     * Exact 3D Euclidean Distance Transform with false-padded sample boundaries,
     * matching {@code analysis.py#clearance}.
     */
    static double[][][] euclideanClearance(boolean[][][] air, int side, int ySize) {
        double[][][] sq = new double[side][side][ySize];
        int maxLen = Math.max(side, ySize) + 2;
        double[] f = new double[maxLen];
        double[] out = new double[maxLen];
        int[] v = new int[maxLen];
        double[] zBound = new double[maxLen + 1];

        // Pass 1: Y axis
        for (int x = 0; x < side; x++) {
            for (int z = 0; z < side; z++) {
                f[0] = 0.0;
                f[ySize + 1] = 0.0;
                for (int y = 0; y < ySize; y++) {
                    f[y + 1] = air[x][z][y] ? INF : 0.0;
                }
                edt1d(f, ySize + 2, out, v, zBound);
                for (int y = 0; y < ySize; y++) {
                    sq[x][z][y] = out[y + 1];
                }
            }
        }

        // Pass 2: Z axis
        for (int x = 0; x < side; x++) {
            for (int y = 0; y < ySize; y++) {
                f[0] = 0.0;
                f[side + 1] = 0.0;
                for (int z = 0; z < side; z++) {
                    f[z + 1] = sq[x][z][y];
                }
                edt1d(f, side + 2, out, v, zBound);
                for (int z = 0; z < side; z++) {
                    sq[x][z][y] = out[z + 1];
                }
            }
        }

        // Pass 3: X axis
        for (int z = 0; z < side; z++) {
            for (int y = 0; y < ySize; y++) {
                f[0] = 0.0;
                f[side + 1] = 0.0;
                for (int x = 0; x < side; x++) {
                    f[x + 1] = sq[x][z][y];
                }
                edt1d(f, side + 2, out, v, zBound);
                for (int x = 0; x < side; x++) {
                    sq[x][z][y] = Math.sqrt(out[x + 1]);
                }
            }
        }
        return sq;
    }

    private static void edt1d(double[] f, int m, double[] out, int[] v, double[] z) {
        int k = 0;
        v[0] = 0;
        z[0] = Double.NEGATIVE_INFINITY;
        z[1] = Double.POSITIVE_INFINITY;
        for (int q = 1; q < m; q++) {
            double fq = f[q];
            if (fq >= INF) continue;
            double s;
            while (true) {
                int vk = v[k];
                s = ((fq + (double) q * q) - (f[vk] + (double) vk * vk)) / (2.0 * (q - vk));
                if (s > z[k]) break;
                k--;
            }
            k++;
            v[k] = q;
            z[k] = s;
            z[k + 1] = Double.POSITIVE_INFINITY;
        }
        k = 0;
        for (int q = 0; q < m; q++) {
            while (z[k + 1] < q) k++;
            double dq = q - v[k];
            out[q] = dq * dq + f[v[k]];
        }
    }
}
