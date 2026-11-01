package dev.itzgamesgd.moreminecrafts.worldgenprobe.stage0d;

import dev.itzgamesgd.moreminecrafts.worldgenprobe.stage0d.ChamberCore.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Stage 0-E: qualification -> structure/footprint exclusion -> deterministic greedy spacing with unresolved propagation -> one 75% roll ->
 * post-roll preflight -> placement reservation. Pure Java. NO arena blocks are built: "placement reservation" is a durable record only.
 *
 * <p>Footprint radii derive from approved numbers only: arena exterior radius 34 (WG-Q02), +4 terrain-repair margin (WG-Q07) = BODY_R 38;
 * corridor path cap 32 (WG-Q08) beyond the wall = ENVELOPE_R 66. The exact corridor route is NOT planned here (not implemented), so the
 * corridor envelope is only reported, never used to reject.
 */
public final class PlacementPipeline {
    private PlacementPipeline() { }

    public static final int BODY_R = 38, ENVELOPE_R = 66;
    public enum Measure { BOX32, DISK32 }

    /** Axis-aligned occupied area of another structure: {minX, minZ, maxX, maxZ}, inclusive. */
    public interface Env {
        /** Occupied boxes of other structures that intersect the square around (cx,cz) of half-size r. */
        List<int[]> structuresNear(int cx, int cz, int r);
        /** Independent post-roll re-verification. Returns null when the placement passes, otherwise the failure reason. */
        String preflight(Chamber c, Measure m, int cx, int cz, int floorY);
    }

    public static final class Cand {
        public Chamber ch;
        public int cx, cz, floorY;
        public String reason = "";
    }

    public static final class Result {
        public final Map<Long, String> status = new TreeMap<>(Long::compareUnsigned);
        public final Map<Long, String> reason = new TreeMap<>(Long::compareUnsigned);
        public final Map<String, Integer> c = new LinkedHashMap<>();
        public int denominator;
        public final List<String> invariantViolations = new ArrayList<>();
        public int footprintOnlyRejections, corridorEnvelopeStructureTouch, corridorEnvelopeArenaOverlapPairs;
        public double deferredFraction;
        void inc(String k) { c.merge(k, 1, Integer::sum); }
        public String canonical() {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<Long, String> e : status.entrySet()) sb.append(Long.toHexString(e.getKey())).append(' ').append(e.getValue()).append(' ').append(reason.getOrDefault(e.getKey(), "")).append('\n');
            sb.append(c).append(" denom=").append(denominator).append('\n');
            return sb.toString();
        }
    }

    static boolean circleHitsBox(int cx, int cz, int r, int[] b) {
        double dx = Math.max(0, Math.max(b[0] - cx, cx - b[2])), dz = Math.max(0, Math.max(b[1] - cz, cz - b[3]));
        return dx * dx + dz * dz <= (double) r * r;
    }

    /** universe = [minX,minZ,maxX,maxZ) of observed own areas; a candidate within SPACING of its border has unobserved neighbours. */
    public static Result run(Resolution r, long seed, Measure m, int[] universe, Env env) {
        Result R = new Result();
        List<Cand> eligible = new ArrayList<>();
        for (Chamber ch : r.chambers.values()) {
            if (ch.status.equals("OWNER_OUTSIDE_OBSERVED_UNIVERSE")) { R.status.put(ch.id, "OUTSIDE_UNIVERSE"); R.inc("outside_universe"); continue; }
            if (ch.status.equals("DEFERRED_IDENTITY_CONFLICT")) { R.status.put(ch.id, "DEFERRED_IDENTITY_CONFLICT"); R.inc("identity_conflict"); continue; }
            R.inc("owned_chambers");
            boolean suit = m == Measure.BOX32 ? ch.boxSuit : ch.diskSuit, any = m == Measure.BOX32 ? ch.boxAnyY : ch.diskAnyY;
            int path = m == Measure.BOX32 ? ch.boxPath : ch.diskPath;
            if (!suit) { String why = any ? "REFERENCE_Y_OUT_OF_BAND" : "NO_WIDTH_HEIGHT_FIT"; R.status.put(ch.id, "NOT_QUALIFYING"); R.reason.put(ch.id, why); R.inc("not_qualifying_" + why.toLowerCase()); continue; }
            if (path == 0) { R.status.put(ch.id, "NOT_QUALIFYING"); R.reason.put(ch.id, "PATH_UNDER_64"); R.inc("not_qualifying_path_under_64"); continue; }
            Cand c = new Cand();
            c.ch = ch;
            c.cx = m == Measure.BOX32 ? ch.boxCx : ch.diskCx; c.cz = m == Measure.BOX32 ? ch.boxCz : ch.diskCz; c.floorY = m == Measure.BOX32 ? ch.boxFloorY : ch.diskFloorY;
            if (path == 2) { R.status.put(ch.id, "DEFERRED_PATH_UNKNOWN"); c.reason = "PATH_UNKNOWN"; R.inc("deferred_path_unknown"); }
            R.inc("qualifying_owned");
            eligible.add(c);
        }
        // structure collision (WG-Q27): body footprint vs occupied boxes of other structures -> hard pre-roll rejection
        List<Cand> live = new ArrayList<>();
        for (Cand c : eligible) {
            if (R.status.containsKey(c.ch.id)) { live.add(c); continue; }   // path unknown: stays in the dependency graph as unresolved
            boolean body = false, touch = false;
            for (int[] b : env0(env, c)) { if (circleHitsBox(c.cx, c.cz, BODY_R, b)) body = true; if (circleHitsBox(c.cx, c.cz, ENVELOPE_R, b)) touch = true; }
            if (body) { R.status.put(c.ch.id, "REJECTED_STRUCTURE"); R.inc("rejected_structure_body"); continue; }
            if (touch) R.corridorEnvelopeStructureTouch++;
            live.add(c);
        }
        // deterministic greedy by priority
        for (Cand c : live) c.ch.priority = ChamberCore.priority(seed, c.ch.id);
        live.sort((a, b) -> { int k = Long.compareUnsigned(b.ch.priority, a.ch.priority); return k != 0 ? k : Long.compareUnsigned(a.ch.id, b.ch.id); });
        Map<Long, String> st = new TreeMap<>(Long::compareUnsigned);
        List<Cand> done = new ArrayList<>();
        for (Cand c : live) {
            boolean rejSpacing = false, rejFootprint = false, blocked = c.reason.equals("PATH_UNKNOWN");
            // unobserved neighbourhood: a candidate whose 256 radius leaves the observed universe depends on candidates we never saw
            if (universe != null && (c.cx - ChamberCore.SPACING < universe[0] || c.cz - ChamberCore.SPACING < universe[1] || c.cx + ChamberCore.SPACING >= universe[2] || c.cz + ChamberCore.SPACING >= universe[3])) { blocked = true; if (!c.reason.equals("PATH_UNKNOWN")) c.reason = "UNOBSERVED_NEIGHBOURHOOD"; }
            for (Group g : r.groups) {
                double dx = Math.max(0, Math.max(g.minX - c.cx, c.cx - g.maxX)), dz = Math.max(0, Math.max(g.minZ - c.cz, c.cz - g.maxZ));
                if (dx * dx + dz * dz < (double) ChamberCore.SPACING * ChamberCore.SPACING) { blocked = true; if (c.reason.isEmpty()) c.reason = "UNRESOLVED_GROUP_NEARBY"; break; }
            }
            for (Cand d : done) {
                double dx = c.cx - d.cx, dz = c.cz - d.cz, dist2 = dx * dx + dz * dz;
                boolean spacing = dist2 < (double) ChamberCore.SPACING * ChamberCore.SPACING, foot = dist2 < (double) (2 * BODY_R) * (2 * BODY_R);
                if (dist2 < (double) (2 * ENVELOPE_R) * (2 * ENVELOPE_R)) R.corridorEnvelopeArenaOverlapPairs++;
                String ds = st.get(d.ch.id);
                if (!spacing && !foot) continue;
                if (ds.equals("ACCEPTED")) { if (spacing) rejSpacing = true; else rejFootprint = true; }
                else if (ds.equals("BLOCKED_BY_UNRESOLVED") && spacing) { blocked = true; if (c.reason.isEmpty()) c.reason = "HIGHER_PRIORITY_NEIGHBOUR_UNRESOLVED"; }
            }
            String s;
            if (rejSpacing) { s = "REJECTED_SPACING"; R.inc("rejected_spacing"); }
            else if (rejFootprint) { s = "REJECTED_FOOTPRINT"; R.inc("rejected_footprint"); R.footprintOnlyRejections++; }
            else if (blocked) { s = "BLOCKED_BY_UNRESOLVED"; R.inc("blocked_by_unresolved"); R.reason.put(c.ch.id, c.reason); }
            else { s = "ACCEPTED"; R.inc("accepted_denominator"); R.denominator++; }
            st.put(c.ch.id, s);
            done.add(c);
        }
        // one roll per accepted candidate, then post-roll preflight and reservation
        for (Cand c : live) {
            String s = st.get(c.ch.id);
            if (!s.equals("ACCEPTED")) { if (!R.status.containsKey(c.ch.id)) R.status.put(c.ch.id, s); continue; }
            if (!ChamberCore.rollWin(seed, c.ch.id)) { R.status.put(c.ch.id, "ROLL_FAILED"); R.inc("roll_failed"); continue; }
            R.inc("roll_won");
            String why = env.preflight(c.ch, m, c.cx, c.cz, c.floorY);
            if (why != null) { R.status.put(c.ch.id, "ROLL_WON_PREFLIGHT_FAILED"); R.reason.put(c.ch.id, why); R.inc("preflight_failed"); continue; }
            R.status.put(c.ch.id, "PLACEMENT_RESERVED"); R.inc("placement_reserved");
        }
        int owned = R.c.getOrDefault("owned_chambers", 0);
        R.deferredFraction = owned + r.groups.size() == 0 ? 0 : (double) (R.c.getOrDefault("blocked_by_unresolved", 0) + R.c.getOrDefault("identity_conflict", 0) + r.groups.size()) / (owned + r.groups.size());
        // invariants
        int den = R.denominator, won = R.c.getOrDefault("roll_won", 0), failed = R.c.getOrDefault("roll_failed", 0);
        if (won + failed != den) R.invariantViolations.add("roll_won+roll_failed != denominator");
        if (R.c.getOrDefault("preflight_failed", 0) + R.c.getOrDefault("placement_reserved", 0) != won) R.invariantViolations.add("preflight_failed+placement_reserved != roll_won");
        int q = R.c.getOrDefault("qualifying_owned", 0);
        int partition = R.c.getOrDefault("rejected_structure_body", 0) + R.c.getOrDefault("rejected_spacing", 0) + R.c.getOrDefault("rejected_footprint", 0) + R.c.getOrDefault("blocked_by_unresolved", 0) + den;
        if (q != partition) R.invariantViolations.add("qualifying_owned != structure+spacing+footprint+blocked+denominator (" + q + " vs " + partition + ")");
        return R;
    }

    private static List<int[]> env0(Env env, Cand c) { return env.structuresNear(c.cx, c.cz, ENVELOPE_R); }

    /** Denominator invariance: forcing every preflight to fail (or pass) must leave all pre-roll outcomes and the denominator untouched. */
    public static List<String> denominatorInvariance(Resolution r, long seed, Measure m, int[] universe, Env base) {
        List<String> v = new ArrayList<>();
        Result a = run(r, seed, m, universe, base);
        for (boolean failAll : new boolean[]{true, false}) {
            Env forced = new Env() {
                public List<int[]> structuresNear(int cx, int cz, int rr) { return base.structuresNear(cx, cz, rr); }
                public String preflight(Chamber c, Measure mm, int cx, int cz, int fy) { return failAll ? "FORCED_FAIL" : null; }
            };
            Result b = run(r, seed, m, universe, forced);
            if (a.denominator != b.denominator) v.add("denominator changed under forced preflight " + (failAll ? "failure" : "success"));
            for (Map.Entry<Long, String> e : a.status.entrySet()) {
                String x = e.getValue(), y = b.status.get(e.getKey());
                boolean post = x.equals("ROLL_WON_PREFLIGHT_FAILED") || x.equals("PLACEMENT_RESERVED");
                if (!post && !x.equals(y)) v.add("pre-roll/roll outcome changed for " + Long.toHexString(e.getKey()));
                if (post && !(y.equals("ROLL_WON_PREFLIGHT_FAILED") || y.equals("PLACEMENT_RESERVED"))) v.add("roll winner lost/gained by preflight change " + Long.toHexString(e.getKey()));
            }
            if (b.status.size() != a.status.size()) v.add("candidate set changed (reselection)");
        }
        return v;
    }
}
