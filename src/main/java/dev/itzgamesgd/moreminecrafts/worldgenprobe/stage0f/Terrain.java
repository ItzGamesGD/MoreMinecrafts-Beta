package dev.itzgamesgd.moreminecrafts.worldgenprobe.stage0f;

/** Read-only terrain view used by the pure planner. Kinds are a safety classification, not block ids. */
public interface Terrain {
    enum Kind { AIR, ROCK, GRAVITY, WATER, LAVA, PROTECTED, UNKNOWN }

    Kind kind(int x, int y, int z);

    default boolean fluid(int x, int y, int z) { Kind k = kind(x, y, z); return k == Kind.WATER || k == Kind.LAVA; }

    /** Dense box terrain, used for tests and as the immutable snapshot of live terrain. Outside the box: UNKNOWN. */
    final class Grid implements Terrain {
        public final int ox, oy, oz, sx, sy, sz;
        final byte[] k;
        public Grid(int ox, int oy, int oz, int sx, int sy, int sz, Kind fill) {
            this.ox = ox; this.oy = oy; this.oz = oz; this.sx = sx; this.sy = sy; this.sz = sz;
            k = new byte[sx * sy * sz];
            java.util.Arrays.fill(k, (byte) fill.ordinal());
        }
        int idx(int x, int y, int z) { return ((x - ox) * sz + (z - oz)) * sy + (y - oy); }
        public boolean inside(int x, int y, int z) { return x >= ox && x < ox + sx && y >= oy && y < oy + sy && z >= oz && z < oz + sz; }
        public Kind kind(int x, int y, int z) { return inside(x, y, z) ? Kind.values()[k[idx(x, y, z)]] : Kind.UNKNOWN; }
        public void set(int x, int y, int z, Kind kd) { if (inside(x, y, z)) k[idx(x, y, z)] = (byte) kd.ordinal(); }
        public void fillBox(int x0, int y0, int z0, int x1, int y1, int z1, Kind kd) {
            for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) for (int z = z0; z <= z1; z++) set(x, y, z, kd);
        }
        public long digest() {
            long h = 1125899906842597L;
            for (byte b : k) h = 31 * h + b;
            return h;
        }
    }
}
