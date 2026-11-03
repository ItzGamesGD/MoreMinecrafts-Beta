package dev.itzgamesgd.moreminecrafts.worldgenprobe.stage0f;

import dev.itzgamesgd.moreminecrafts.worldgenprobe.stage0f.Terrain.Kind;
import java.util.ArrayList;
import java.util.List;

/**
 * Arena body geometry from the approved numbers only (WG-Q02/Q06/Q12-Q16/Q18-Q22): exterior radius 34 (68-block diameter), combat floor
 * radius 29, outer wall 12 high + 11-high dome (23 total), four 7x8 arch entrances (N/E/S/W), 8-12 perimeter columns, six Vaults on radius 27,
 * one Ominous Vault at the exact center, deepslate-family masonry only, intact (50%) / ruined-partial-dome (25%) / ruined-natural-ceiling (25%).
 * The activation crystal (Stage 1 controller content) is NOT placed; the space above the center Vault is kept free for it.
 *
 * <p>All coordinates are absolute. {@code floorY} is the first air layer above the combat floor; the floor surface block is at floorY-1.
 */
public final class ArenaDesign {
    private ArenaDesign() { }

    public static final int EXT_R = 34, IN_R = 29, REPAIR = 4, WALL_H = 12, DOME_H = 11, TOTAL_H = 23, VAULT_R = 27, ENT_W = 7, ENT_H = 8;
    public static final double DOME_SPHERE_R = (EXT_R * EXT_R + DOME_H * DOME_H) / (2.0 * DOME_H);
    public static final String DB = "minecraft:deepslate_bricks", PD = "minecraft:polished_deepslate", DT = "minecraft:deepslate_tiles",
            CDB = "minecraft:cracked_deepslate_bricks", CDT = "minecraft:cracked_deepslate_tiles", COB = "minecraft:cobbled_deepslate",
            DS = "minecraft:deepslate";
    public enum Variant { INTACT, RUINED_PARTIAL_DOME, RUINED_NATURAL_CEILING }
    /** Cardinal axis directions N(-z) E(+x) S(+z) W(-x). */
    public static final double[] LAMP_RING_R = {4.5, 10, 15.5, 21, 26.5};
    public static final int[] LAMP_RING_N = {5, 11, 16, 22, 28};
    public static final int LIGHT_MIN = 8;
    public static final int[][] DIR = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
    public static final String[] DIRNAME = {"north", "east", "south", "west"};

    public static long mix(long a, long b) {
        long h = a * 0x9E3779B97F4A7C15L + b;
        h ^= h >>> 30; h *= 0xBF58476D1CE4E5B9L; h ^= h >>> 27; h *= 0x94D049BB133111EBL; h ^= h >>> 31;
        return h;
    }
    public static long cellHash(long id, int x, int y, int z) { return mix(mix(mix(id, x), y), z); }

    public static Variant variantOf(long id) {
        long v = Long.remainderUnsigned(mix(id, 0x51ED), 4);
        return v <= 1 ? Variant.INTACT : v == 2 ? Variant.RUINED_PARTIAL_DOME : Variant.RUINED_NATURAL_CEILING;
    }
    public static int columnCount(long id) { return 8 + (int) Long.remainderUnsigned(mix(id, 0xC01), 5); }

    /** Dome cap height (0..11) at radius r (cells), spherical cap with base radius 34 and height 11. */
    public static int capAt(double r) {
        double s = Math.sqrt(DOME_SPHERE_R * DOME_SPHERE_R - r * r) - Math.sqrt(DOME_SPHERE_R * DOME_SPHERE_R - (double) EXT_R * EXT_R);
        return (int) Math.max(0, Math.min(DOME_H, Math.round(s)));
    }

    static String domeMat(long id, int x, int y, int z, int r, boolean ruined) {
        String base = switch ((r / 3) % 3) { case 0 -> PD; case 1 -> DB; default -> DT; };
        if (ruined && Long.remainderUnsigned(cellHash(id, x, y, z), 6) == 0) return base.equals(DB) ? CDB : base.equals(DT) ? CDT : base;
        return base;
    }

    static String stair(String id, String facing, String half) { return id + "[facing=" + facing + ",half=" + half + ",shape=straight]"; }
    static String opposite(String d) { return switch (d) { case "north" -> "south"; case "south" -> "north"; case "east" -> "west"; default -> "east"; }; }

    /** Cells reserved by design decor, used to keep columns clear of Vaults and entrances. */
    public static final class Layout {
        public final List<int[]> vaults = new ArrayList<>();   // {x,z}
        public final List<int[]> columns = new ArrayList<>();
        public Variant variant;
    }

    /**
     * Adds the arena body to the plan. Terrain is only consulted for hazard checks at ruin openings (never to decide the geometry).
     * Returns the layout for tests/verification.
     */
    public static Layout build(Plan plan, Terrain t, long id, int cx, int cz, int floorY) {
        Layout L = new Layout();
        Variant var = variantOf(id);
        L.variant = var;
        boolean ruined = var != Variant.INTACT;
        // ---- radial structure
        for (int dx = -EXT_R; dx <= EXT_R; dx++) for (int dz = -EXT_R; dz <= EXT_R; dz++) {
            double r = Math.sqrt((double) dx * dx + (double) dz * dz);
            if (r > EXT_R) continue;
            int x = cx + dx, z = cz + dz, ri = (int) r;
            boolean interior = r <= IN_R;
            int cap = capAt(r);
            // foundation (2 layers below combat floor level) under the whole body
            plan.put(Plan.SOLID, x, floorY - 2, z, DB);
            plan.solid.add(Plan.pack(x, floorY - 2, z));
            if (interior) {
                // flat combat floor: concentric rings + radial spokes, deepslate family only
                double ang = Math.atan2(dz, dx);
                int spoke = (int) Math.floor((ang + Math.PI) / (Math.PI / 4));
                String f = (((int) (r / 2) + spoke) & 1) == 0 ? PD : DT;
                if (ruined && Long.remainderUnsigned(cellHash(id, x, floorY - 1, z), 11) == 0) f = f.equals(DT) ? CDT : PD;
                plan.put(Plan.SOLID, x, floorY - 1, z, f);
                plan.solid.add(Plan.pack(x, floorY - 1, z));
                int top = WALL_H - 1 + cap;                       // highest solid offset at this radius (11 at the rim, 22 at the centre)
                int topAir = var == Variant.RUINED_NATURAL_CEILING ? WALL_H - 1 : top - 2;
                for (int o = 0; o <= topAir; o++) { plan.put(Plan.CARVE, x, floorY + o, z, Plan.AIR); plan.open.add(Plan.pack(x, floorY + o, z)); }
                if (var != Variant.RUINED_NATURAL_CEILING) {
                    boolean hole = false;
                    if (var == Variant.RUINED_PARTIAL_DOME) {
                        double p = 0.45 * (r / EXT_R) * (r / EXT_R);   // ruin concentrated near the perimeter
                        hole = (cellHash(id, x, 7, z) >>> 11) / (double) (1L << 53) < p;
                        if (hole) {
                            // never leave a gravity, fluid or unknown cell in or directly above an opened ceiling position
                            for (int o = top - 1; o <= top + 2 && hole; o++) { Kind kk = t.kind(x, floorY + o, z); if (bad(kk) || kk == Kind.PROTECTED || kk == Kind.UNKNOWN) hole = false; }
                        }
                    }
                    if (!hole) for (int o = top - 1; o <= top; o++) {
                        plan.put(Plan.SOLID, x, floorY + o, z, domeMat(id, x, floorY + o, z, ri, ruined));
                        plan.solid.add(Plan.pack(x, floorY + o, z));
                    }
                    else plan.stat("dome_holes", 1);
                }
            } else {
                // wall ring (r in (29,34]) up to the dome top; natural-ceiling variant stops at the wall top
                int top = var == Variant.RUINED_NATURAL_CEILING ? WALL_H - 1 : WALL_H - 1 + cap;
                for (int o = -1; o <= top; o++) {
                    String m = o >= WALL_H ? domeMat(id, x, floorY + o, z, ri, ruined) : (o == -1 ? DB : ((o & 3) == 3 ? PD : DB));
                    if (ruined && o >= 0 && o < WALL_H && Long.remainderUnsigned(cellHash(id, x, floorY + o, z), 7) == 0) m = CDB;
                    plan.put(Plan.SOLID, x, floorY + o, z, m);
                    plan.solid.add(Plan.pack(x, floorY + o, z));
                }
            }
        }
        // ---- four 7x8 arch entrances through the wall (carved after the wall is solid)
        for (int d = 0; d < 4; d++) {
            int ax = DIR[d][0], az = DIR[d][1], lx = -az, lz = ax;
            for (int a = IN_R - 3; a <= EXT_R + 1; a++) for (int l = -3; l <= 3; l++) {
                int x = cx + ax * a + lx * l, z = cz + az * a + lz * l;
                plan.put(Plan.SOLID, x, floorY - 1, z, PD); plan.solid.add(Plan.pack(x, floorY - 1, z));
                plan.put(Plan.SOLID, x, floorY - 2, z, DB); plan.solid.add(Plan.pack(x, floorY - 2, z));
                for (int o = 0; o < ENT_H; o++) { plan.put(Plan.CARVE, x, floorY + o, z, Plan.AIR); plan.open.add(Plan.pack(x, floorY + o, z)); plan.solid.remove(Plan.pack(x, floorY + o, z)); }
            }
            // arch frame: pillars at |l|=4 and a lintel row above the opening, with stair "voussoirs" at the inner corners
            for (int a = IN_R - 3; a <= EXT_R + 1; a++) {
                for (int s = -1; s <= 1; s += 2) {
                    int x = cx + ax * a + lx * 4 * s, z = cz + az * a + lz * 4 * s;
                    for (int o = 0; o <= ENT_H; o++) if (o <= WALL_H && inRing(x - cx, z - cz)) { plan.put(Plan.SOLID, x, floorY + o, z, (o & 1) == 0 ? PD : DB); plan.solid.add(Plan.pack(x, floorY + o, z)); }
                }
                for (int l = -3; l <= 3; l++) {
                    int x = cx + ax * a + lx * l, z = cz + az * a + lz * l;
                    if (inRing(x - cx, z - cz)) { plan.put(Plan.SOLID, x, floorY + ENT_H, z, DB); plan.solid.add(Plan.pack(x, floorY + ENT_H, z)); }
                }
            }
        }
        // ---- vaults: six ordinary on radius 27, ominous at the exact center; keep the cell above the center clear
        for (int k = 0; k < 6; k++) {
            double a = Math.toRadians(15 + 60 * k);
            int vx = cx + (int) Math.round(VAULT_R * Math.cos(a)), vz = cz + (int) Math.round(VAULT_R * Math.sin(a));
            L.vaults.add(new int[]{vx, vz});
            String face = Math.abs(vx - cx) > Math.abs(vz - cz) ? (vx > cx ? "west" : "east") : (vz > cz ? "north" : "south");
            plan.put(Plan.DECOR, vx, floorY, vz, "minecraft:vault[facing=" + face + ",ominous=false,vault_state=inactive]");
            plan.open.remove(Plan.pack(vx, floorY, vz));
            plan.solid.add(Plan.pack(vx, floorY, vz));
        }
        plan.put(Plan.DECOR, cx, floorY, cz, "minecraft:vault[facing=north,ominous=true,vault_state=inactive]");
        plan.open.remove(Plan.pack(cx, floorY, cz)); plan.solid.add(Plan.pack(cx, floorY, cz));
        // ---- perimeter columns (8-12, never interior), clear of vaults and entrance axes
        int n = columnCount(id);
        // valid perimeter slots every 3 degrees on radius 28: clear of the entrance axes (+-5 blocks) and of every Vault; then the n slots nearest
        // to the n evenly spaced target angles are used, so 8-12 columns are ALWAYS produced
        List<int[]> slots = new ArrayList<>();
        List<Double> slotDeg = new ArrayList<>();
        for (int a3 = 0; a3 < 120; a3++) {
            double a = Math.toRadians(a3 * 3.0);
            int px = cx + (int) Math.round(28 * Math.cos(a)), pz = cz + (int) Math.round(28 * Math.sin(a));
            boolean ok = true;
            for (int[] s : slots) if (s[0] == px && s[1] == pz) ok = false;
            for (int[] v : L.vaults) if (Math.hypot(px - v[0], pz - v[1]) < 3.5) ok = false;
            for (int d = 0; d < 4 && ok; d++) {
                int ax = DIR[d][0], az = DIR[d][1];
                int along = (px - cx) * ax + (pz - cz) * az, lat = Math.abs((px - cx) * -az + (pz - cz) * ax);
                if (along > 0 && lat <= 5) ok = false;
            }
            if (ok) { slots.add(new int[]{px, pz}); slotDeg.add(a3 * 3.0); }
        }
        boolean[] used = new boolean[slots.size()];
        double off = Long.remainderUnsigned(mix(id, 0xC0F), 360);
        for (int k = 0; k < n; k++) {
            double target = (off + k * 360.0 / n) % 360;
            int best = -1; double bd = 1e9;
            for (int i = 0; i < slots.size(); i++) {
                if (used[i]) continue;
                double dd = Math.abs(slotDeg.get(i) - target); dd = Math.min(dd, 360 - dd);
                if (dd < bd) { bd = dd; best = i; }
            }
            if (best >= 0) { used[best] = true; L.columns.add(slots.get(best)); }
        }
        for (int[] c : L.columns) {
            int top = ruined ? WALL_H - 1 - (int) Long.remainderUnsigned(cellHash(id, c[0], 3, c[1]), 4) : WALL_H - 1;
            for (int o = 0; o <= top; o++) {
                plan.put(Plan.DECOR, c[0], floorY + o, c[1], o == 0 || o == top ? PD : DB);
                plan.solid.add(Plan.pack(c[0], floorY + o, c[1])); plan.open.remove(Plan.pack(c[0], floorY + o, c[1]));
            }
        }
        // ---- lighting: flush redstone lamps (powered by a redstone block below) and wall torches
        // WG-Q12 "bright combat floor": concentric flush-lamp rings (Stage 0-G: was 16 lamps, which left the outer floor nearly dark).
        // Engineering target checked by Verify: every combat-floor cell reaches block light >= LIGHT_MIN (see Verify.floorLight).
        for (int ring = 0; ring < LAMP_RING_R.length; ring++) for (int k = 0; k < LAMP_RING_N[ring]; k++) {
            double a = Math.toRadians(360.0 * (k + 0.5 * (ring % 2)) / LAMP_RING_N[ring]), r = LAMP_RING_R[ring];
            int lx = cx + (int) Math.round(r * Math.cos(a)), lz = cz + (int) Math.round(r * Math.sin(a));
            plan.put(Plan.DECOR, lx, floorY - 2, lz, "minecraft:redstone_block");
            plan.put(Plan.DECOR, lx, floorY - 1, lz, "minecraft:redstone_lamp[lit=true]");
        }
        for (int k = 0; k < 12; k++) {
            double a = Math.toRadians(15 + 30 * k + 4);
            int tx = cx + (int) Math.round(28.4 * Math.cos(a)), tz = cz + (int) Math.round(28.4 * Math.sin(a));
            int ddx = tx - cx, ddz = tz - cz;
            int ox = Math.abs(ddx) >= Math.abs(ddz) ? Integer.signum(ddx) : 0, oz = ox == 0 ? Integer.signum(ddz) : 0;
            int wx = tx + ox, wz = tz + oz;                      // attached wall cell
            double wr = Math.sqrt((double) (wx - cx) * (wx - cx) + (double) (wz - cz) * (wz - cz));
            boolean free = plan.open.contains(Plan.pack(tx, floorY + 4, tz)) && wr > IN_R && wr <= EXT_R && plan.solid.contains(Plan.pack(wx, floorY + 4, wz));
            for (int[] c : L.columns) if (c[0] == tx && c[1] == tz) free = false;
            if (!free) continue;
            String face = ox > 0 ? "west" : ox < 0 ? "east" : oz > 0 ? "north" : "south";
            plan.put(Plan.DECOR, tx, floorY + 4, tz, "minecraft:wall_torch[facing=" + face + "]");
            plan.open.remove(Plan.pack(tx, floorY + 4, tz));
        }
        plan.stat("arena_variant_" + var.ordinal(), 1);
        return L;
    }

    static boolean bad(Kind k) { return k == Kind.GRAVITY || k == Kind.WATER || k == Kind.LAVA; }
    static boolean inRing(int dx, int dz) { double r = Math.sqrt((double) dx * dx + (double) dz * dz); return r > IN_R && r <= EXT_R; }
}
