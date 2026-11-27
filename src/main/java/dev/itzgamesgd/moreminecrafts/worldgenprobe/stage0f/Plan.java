package dev.itzgamesgd.moreminecrafts.worldgenprobe.stage0f;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * A deterministic construction plan: an ordered list of block operations in four phases. The plan, not the terrain, is the
 * authority after it is persisted: recovery re-applies the persisted plan and never re-plans from half-edited terrain.
 * Phases: SEAL (fluid seals / support replacement) -> SOLID (shell, floor, lining) -> CARVE (air volume) -> DECOR (vaults, lamps...).
 */
public final class Plan {
    public static final int SEAL = 0, SOLID = 1, CARVE = 2, DECOR = 3;
    public static final String AIR = "minecraft:air";

    public static long pack(int x, int y, int z) { return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | ((y + 2048) & 0xFFF); }
    public static int ux(long p) { return (int) (p >> 38) << 6 >> 6; }
    public static int uz(long p) { return (int) ((p >> 12) & 0x3FFFFFF) << 6 >> 6; }
    public static int uy(long p) { return (int) (p & 0xFFF) - 2048; }

    public final List<String> palette = new ArrayList<>();
    final Map<String, Integer> palIdx = new HashMap<>();
    /** ops in application order. */
    public final List<long[]> ops = new ArrayList<>();       // {pos, specIdx, phase}
    final Map<Long, Integer> finalSpec = new LinkedHashMap<>();
    /** Cells that must be air once construction is complete (interior volume + doorways + corridor boxes). */
    public final Set<Long> open = new HashSet<>();
    /** Cells that must hold a solid, non-fluid block once complete (shell, floor, lining, seals). */
    public final Set<Long> solid = new HashSet<>();
    public final Map<String, Integer> stats = new TreeMap<>();

    int spec(String s) { return palIdx.computeIfAbsent(s, k -> { palette.add(k); return palette.size() - 1; }); }

    /** Add (or override) an operation. Within one phase the last operation on a cell wins. */
    final Map<Long, Integer> finalPhase = new HashMap<>();
    public void put(int phase, int x, int y, int z, String spec) {
        long p = pack(x, y, z);
        int si = spec(spec);
        ops.add(new long[]{p, si, phase});
        Integer fp = finalPhase.get(p);
        if (fp == null || phase >= fp) {
            finalSpec.put(p, si); finalPhase.put(p, phase);
            if (!spec.equals(AIR)) open.remove(p);   // a cell that finally holds a block is not an open cell
        }
    }

    /** Stable-sort ops into application order (SEAL, SOLID, CARVE, DECOR). Call once after planning. */
    public void finish() { ops.sort((a, b) -> Long.compare(a[2], b[2])); }

    public boolean has(int x, int y, int z) { return finalSpec.containsKey(pack(x, y, z)); }
    public String finalSpecAt(int x, int y, int z) { Integer i = finalSpec.get(pack(x, y, z)); return i == null ? null : palette.get(i); }
    public Set<Long> cells() { return finalSpec.keySet(); }
    public int size() { return finalSpec.size(); }
    public void stat(String k, int v) { stats.merge(k, v, Integer::sum); }

    /** Deterministic digest over the ordered ops (position, spec string, phase). */
    public String hash() {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            for (long[] o : ops) {
                md.update((byte) o[2]);
                md.update(Long.toString(o[0]).getBytes());
                md.update(palette.get((int) o[1]).getBytes());
                md.update((byte) 10);
            }
            byte[] d = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { throw new IllegalStateException(e); }
    }

    /** Acceptable current states per cell during recovery: every spec any op assigned to it (plus the persisted pre-state). */
    public Map<Long, Set<Integer>> acceptable() {
        Map<Long, Set<Integer>> m = new HashMap<>();
        for (long[] o : ops) m.computeIfAbsent(o[0], k -> new HashSet<>()).add((int) o[1]);
        return m;
    }
}
