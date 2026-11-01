package dev.itzgamesgd.moreminecrafts.worldgenprobe.stage0f;

import dev.itzgamesgd.moreminecrafts.worldgenprobe.stage0f.Construction.*;
import dev.itzgamesgd.moreminecrafts.worldgenprobe.stage0f.Terrain.Grid;
import dev.itzgamesgd.moreminecrafts.worldgenprobe.stage0f.Terrain.Kind;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure (no Minecraft) targeted tests for routing, alternative-placement order, clearance, fluids (sealable vs unsealable), structure collision,
 * persistence, partial edits / crash recovery, quarantine and reservation immutability. Run: {@code java -cp <classes> ...ConstructionSelfTest}.
 */
public final class ConstructionSelfTest {
    static int pass, fail;
    static final List<String> log = new ArrayList<>();
    static void ck(boolean c, String m) { if (c) pass++; else { fail++; log.add("FAIL " + m); System.out.println("FAIL " + m); } }

    // ------------------------------------------------------------------------------------------------ simulated world
    static final class GridWorld implements WorldAccess {
        final Grid g; final Map<Long, String> over = new HashMap<>(); int writes;
        GridWorld(Grid g) { this.g = g; }
        public String get(int x, int y, int z) {
            String o = over.get(Plan.pack(x, y, z));
            if (o != null) return o;
            return switch (g.kind(x, y, z)) {
                case AIR -> "minecraft:air"; case ROCK -> "minecraft:stone"; case GRAVITY -> "minecraft:gravel"; case WATER -> "minecraft:water";
                case LAVA -> "minecraft:lava"; case PROTECTED -> "minecraft:bedrock"; case UNKNOWN -> null; };
        }
        public void set(int x, int y, int z, String spec) { over.put(Plan.pack(x, y, z), spec); writes++; }
        public String canon(String s) { return s; }
        public void flush() { }
        Terrain after() {
            return (x, y, z) -> {
                String s = get(x, y, z);
                if (s == null) return Kind.UNKNOWN;
                if (s.equals("minecraft:air") || s.contains("torch") || s.contains("redstone_lamp[lit=true]") && false) return Kind.AIR;
                if (s.equals("minecraft:water")) return Kind.WATER;
                if (s.equals("minecraft:lava")) return Kind.LAVA;
                if (s.equals("minecraft:gravel") || s.equals("minecraft:sand")) return Kind.GRAVITY;
                if (s.equals("minecraft:bedrock")) return Kind.PROTECTED;
                return Kind.ROCK;
            };
        }
    }

    static Grid base(int cx, int cz) { return new Grid(cx - 135, -64, cz - 135, 271, 100, 271, Kind.ROCK); }
    static void hall(Grid g, int x0, int x1, int z0, int z1, int yFloorBlock, int h) { g.fillBox(x0, yFloorBlock + 1, z0, x1, yFloorBlock + h, z1, Kind.AIR); }

    static final int CX = 0, CZ = 0, FY = -25;
    static long idOf(ArenaDesign.Variant v) { for (long i = 1; ; i++) if (ArenaDesign.variantOf(i * 7919) == v) return i * 7919; }

    static ConstructionPlanner.Result planOn(Grid g, long id, int floorY, List<int[]> boxes) { return ConstructionPlanner.plan(g, id, CX, CZ, floorY, boxes); }

    static GridWorld applied(Grid g, ConstructionPlanner.Cand c) {
        GridWorld w = new GridWorld(g);
        for (long[] op : c.plan.ops) w.set(Plan.ux(op[0]), Plan.uy(op[0]), Plan.uz(op[0]), c.plan.palette.get((int) op[1]));
        return w;
    }

    static void verifyApplied(String name, Grid g, ConstructionPlanner.Result r, long id) {
        ConstructionPlanner.Cand c = r.chosen;
        GridWorld w = applied(g, c);
        Map<String, Object> m = new LinkedHashMap<>();
        List<String> f = Verify.check(w.after(), c.plan, c.routes, r.cave, CX, CZ, c.floorY, c.layout.variant, 7, m);
        ck(f.isEmpty(), name + " final-state verification: " + f);
        ck(Construction.mismatches(c.plan, w, null) == 0, name + " plan conformance");
        ck(c.layout.columns.size() >= 8 && c.layout.columns.size() <= 12, name + " columns 8-12: " + c.layout.columns.size());
        ck(c.layout.vaults.size() == 6, name + " six vaults");
        int vaults = 0; for (long cell : c.plan.cells()) if (c.plan.finalSpecAt(Plan.ux(cell), Plan.uy(cell), Plan.uz(cell)).startsWith("minecraft:vault")) vaults++;
        ck(vaults == 7, name + " seven vault blocks: " + vaults);
        ck(c.plan.finalSpecAt(CX, c.floorY, CZ).contains("ominous=true"), name + " ominous vault at exact centre");
        ck(c.routes.size() >= 1 && c.routes.size() <= 2, name + " one or two connections: " + c.routes.size());
        for (Corridors.Route R : c.routes) ck(R.length <= Corridors.PATH_CAP, name + " route within 32: " + R.length);
    }

    public static void main(String[] a) throws Exception {
        long t0 = System.currentTimeMillis();
        // ---- A: hall east of the arena, flat. All three architectural variants build and satisfy every approved dimension.
        for (ArenaDesign.Variant v : ArenaDesign.Variant.values()) {
            Grid g = base(CX, CZ);
            hall(g, 47, 120, -22, 22, FY - 1, 16);
            long id = idOf(v);
            ConstructionPlanner.Result r = planOn(g, id, FY, List.of());
            ck(r.chosen != null, "A/" + v + " plan found: " + r.status);
            if (r.chosen == null) continue;
            ck(r.chosen.layout.variant == v, "A/" + v + " variant");
            verifyApplied("A/" + v, g, r, id);
            ck(r.cands.size() == 8, "eight candidates evaluated");
        }
        // ---- A2: natural-ceiling variant with gravel and water directly above the 12-high volume: supports/seals must not be mistaken for design height
        {
            Grid g = base(CX, CZ); hall(g, 47, 120, -22, 22, FY - 1, 16);
            g.fillBox(CX - 28, FY + ArenaDesign.WALL_H, CZ - 28, CX + 28, FY + ArenaDesign.WALL_H, CZ + 28, Kind.GRAVITY);
            g.fillBox(CX - 5, FY + ArenaDesign.WALL_H + 1, CZ - 5, CX + 5, FY + ArenaDesign.WALL_H + 3, CZ + 5, Kind.WATER);
            long id = idOf(ArenaDesign.Variant.RUINED_NATURAL_CEILING);
            ConstructionPlanner.Result r = planOn(g, id, FY, List.of());
            ck(r.chosen != null, "A2 natural ceiling under gravel/water: " + r.status);
            if (r.chosen != null) {
                ck(r.chosen.plan.stats.getOrDefault("gravity_support_cells", 0) > 0, "A2 gravity supports planned");
                verifyApplied("A2", g, r, id);
            }
        }
        // ---- determinism: identical plan hash for repeated planning
        {
            Grid g = base(CX, CZ); hall(g, 47, 120, -22, 22, FY - 1, 16);
            String h1 = planOn(g, 12345, FY, List.of()).chosen.hash, h2 = planOn(g, 12345, FY, List.of()).chosen.hash;
            ck(h1.equals(h2), "plan hash deterministic");
            ck(!h1.equals(planOn(g, 54321, FY, List.of()).chosen.hash), "plan hash depends on placement id");
        }
        // ---- B: nearest cave beyond the 32-block path cap -> every candidate fails, recorded as construction failure
        {
            Grid g = base(CX, CZ); hall(g, 100, 130, -25, 25, FY - 1, 16);
            ConstructionPlanner.Result r = planOn(g, 777, FY, List.of());
            ck(r.chosen == null && r.status.startsWith("CONSTRUCTION_FAILED_NO_FIT"), "B cave beyond 32: " + r.status);
            for (ConstructionPlanner.Cand c : r.cands) ck(c.reason.startsWith("NO_CAVE_CONNECTION"), "B reason " + c.reason);
        }
        // ---- B2: a tiny disconnected void is not a cave (path < 64)
        {
            Grid g = base(CX, CZ); hall(g, 47, 70, -3, 3, FY - 1, 12);
            ConstructionPlanner.Result r = planOn(g, 778, FY, List.of());
            ck(r.chosen == null, "B2 tiny pocket (path<64) is not a traversable natural cave: " + r.status);
        }
        // ---- C: stairs: hall floor 4 higher and 4 lower than the entrance floor
        for (int dyHall : new int[]{7, -7}) {      // beyond the +-4 range of the eight alternative floor levels, so real stairs are required
            Grid g = base(CX, CZ); hall(g, 47, 120, -22, 22, FY - 1 + dyHall, 16);
            // make the whole area between arena and hall rock above/below as needed; hall floor stays rock below
            ConstructionPlanner.Result r = planOn(g, 4242, FY, List.of());
            ck(r.chosen != null, "C stairs " + dyHall + " plan: " + r.status);
            if (r.chosen == null) continue;
            int stairs = r.chosen.routes.get(0).stairCells;
            ck(stairs >= 3, "C stairs present for elevation difference " + dyHall + ": " + stairs);
            boolean facing = false;
            for (long[] op : r.chosen.plan.ops) { String s = r.chosen.plan.palette.get((int) op[1]); if (s.contains("_stairs[facing=") && s.contains("half=bottom,shape=straight")) facing = true; }
            ck(facing, "C stair facing/half/shape specified");
            verifyApplied("C/" + dyHall, g, r, 4242);
        }
        // ---- D: protected blocks (bedrock plates) block all four directions at r=40 -> no clearance route
        {
            Grid g = base(CX, CZ); hall(g, 47, 120, -22, 22, FY - 1, 16);
            for (int d = 0; d < 4; d++) { int ax = ArenaDesign.DIR[d][0], az = ArenaDesign.DIR[d][1], lx = -az, lz = ax;
                for (int l = -30; l <= 30; l++) for (int y = FY - 10; y <= FY + 20; y++) g.set(CX + ax * 40 + lx * l, y, CZ + az * 40 + lz * l, Kind.PROTECTED); }
            ConstructionPlanner.Result r = planOn(g, 99, FY, List.of());
            ck(r.chosen == null, "D protected plates leave no 5x5 route: " + r.status);
        }
        // ---- D2: 5x5 clearance: a 1-block-high lintel of protected blocks inside the only approach lane
        {
            Grid g = base(CX, CZ); hall(g, 47, 120, -22, 22, FY - 1, 16);
            ConstructionPlanner.Result ok = planOn(g, 99, FY, List.of());
            ck(ok.chosen != null, "D2 baseline plan");
            if (ok.chosen != null) {
                Route0 r0 = new Route0(ok.chosen.routes.get(0));
                for (int[] p : r0.cells) g.set(p[0], p[1] + 5, p[2], Kind.PROTECTED);      // ceiling cell of the 5-high box
                ConstructionPlanner.Result r = planOn(g, 99, FY, List.of());
                boolean sameRoute = r.chosen != null && r.chosen.routes.stream().anyMatch(R -> R.dir == ok.chosen.routes.get(0).dir && R.length == ok.chosen.routes.get(0).length && same(R, ok.chosen.routes.get(0)));
                ck(!sameRoute, "D2 blocked clearance cell forces a different route or failure");
            }
        }
        // ---- E: sealable fluid: bounded lava pocket and water pocket touching the wall / corridor
        {
            Grid g = base(CX, CZ); hall(g, 47, 120, -22, 22, FY - 1, 16);
            g.fillBox(CX + 29, FY + 2, CZ + 8, CX + 36, FY + 7, CZ + 14, Kind.LAVA);            // straddles the east wall
            g.fillBox(CX + 3, FY, CZ + 3, CX + 6, FY + 3, CZ + 6, Kind.LAVA);                    // inside the arena volume
            g.fillBox(CX + 3, FY + 1, CZ + 3, CX + 3, FY + 1, CZ + 3, Kind.WATER);
            g.fillBox(CX + 36, FY - 2, CZ + 3, CX + 46, FY + 3, CZ + 5, Kind.WATER);            // beside the east lane, beyond its lining
            g.fillBox(CX + 36, FY + 5, CZ - 6, CX + 46, FY + 8, CZ + 6, Kind.WATER);            // above the east lane
            g.fillBox(CX - 5, FY + 24, CZ - 5, CX + 5, FY + 27, CZ + 5, Kind.WATER);          // just above the dome
            ConstructionPlanner.Result r = planOn(g, 31337, FY, List.of());
            ck(r.chosen != null, "E bounded fluids are sealable: " + r.status);
            if (r.chosen != null) {
                ck(r.chosen.plan.stats.getOrDefault("sealed_fluid_cells", 0) > 0 || r.chosen.plan.stats.getOrDefault("fluid_cells_removed_in_volume", 0) > 0, "E fluid seals/removals planned");
                verifyApplied("E", g, r, 31337);
            }
        }
        // ---- F: unsealable fluid: lava sheet reaching the inspected boundary
        {
            Grid g = base(CX, CZ); hall(g, 47, 120, -22, 22, FY - 1, 16);
            g.fillBox(CX - 134, FY + 3, CZ - 134, CX + 134, FY + 9, CZ + 134, Kind.LAVA);
            ConstructionPlanner.Result r = planOn(g, 31, FY, List.of());
            ck(r.chosen == null, "F unbounded lava lake rejected: " + r.status);
            boolean unb = r.cands.stream().anyMatch(c -> c.reason.contains("UNSEALABLE_FLUID") || c.reason.contains("NO_CAVE"));
            ck(unb, "F reason recorded");
        }
        // ---- G: structure collision (WG-Q27): body, and a box that only the corridor would touch
        {
            Grid g = base(CX, CZ); hall(g, 47, 120, -22, 22, FY - 1, 16);
            ConstructionPlanner.Result r = planOn(g, 5, FY, List.of(new int[]{-10, FY - 5, 30, 10, FY + 10, 50}));
            ck(r.chosen == null && r.cands.get(0).reason.equals("STRUCTURE_COLLISION_BODY"), "G body collision: " + r.status);
            ConstructionPlanner.Result r2 = planOn(g, 5, FY, List.of(new int[]{49, FY - 3, -1, 51, FY + 8, 1}));
            ck(r2.chosen == null || r2.chosen.routes.stream().noneMatch(R -> Planner0.hits(R, 49, 51, -1, 1)), "G corridor avoids structure box");
        }
        // ---- H: alternative order / failover: a protected block in the foundation layer kills candidate 0 only
        {
            Grid g = base(CX, CZ); hall(g, 47, 120, -22, 22, FY - 1, 16);
            g.set(CX + 3, FY - 2, CZ + 3, Kind.PROTECTED);
            ConstructionPlanner.Result r = planOn(g, 8, FY, List.of());
            ck(r.cands.get(0).reason.equals("PROTECTED_BLOCK_IN_BODY"), "H candidate 0 rejected: " + r.cands.get(0).reason);
            ck(r.chosen != null && r.chosen.idx >= 1, "H later candidate chosen: " + (r.chosen == null ? r.status : r.chosen.idx));
            ck(r.cands.get(1).ok, "H candidate 1 (dy=+1) eligible");
        }
        // ---- J: reference-Y band: floorY -40 allows only dy >= 0
        {
            Grid g = base(CX, CZ); hall(g, 47, 120, -22, 22, -41, 16);
            ConstructionPlanner.Result r = ConstructionPlanner.plan(g, 6, CX, CZ, -40, List.of());
            int yb = 0; for (ConstructionPlanner.Cand c : r.cands) if (c.reason.equals("Y_BAND")) yb++;
            ck(yb == 4 || yb == 3, "J Y_BAND rejections for dy<0: " + yb);
            ck(r.chosen != null, "J plan at -40: " + r.status);
        }
        // ---- K: turn plus elevation (regression for the live finding: a stair directly next to a turn made overlapping 5-wide cross-sections conflict)
        for (int dyHall : new int[]{6, -6}) {
            Grid g = base(CX, CZ); hall(g, 40, 120, 12, 70, FY - 1 + dyHall, 16);
            ConstructionPlanner.Result r = planOn(g, 606, FY, List.of());
            ck(r.chosen != null, "K turn+elevation plan " + dyHall + ": " + r.status);
            if (r.chosen == null) continue;
            int turns = 0; for (Corridors.Route R : r.chosen.routes) turns += R.turns;
            ck(turns >= 1, "K route needs a turn: " + turns);
            for (Corridors.Route R : r.chosen.routes) for (int i = 0; i < R.cells.size(); i++) if (i > 0 && R.cells.get(i).h != R.cells.get(i - 1).h)
                for (int k = Math.max(0, i - 2); k <= Math.min(R.cells.size() - 1, i + 1); k++) ck(R.cells.get(k).dy == 0 || k == Math.max(0, i - 2) && false, "K level cells around a turn (cell " + k + ")");
            verifyApplied("K/" + dyHall, g, r, 606);
        }
        // ---- columns: 8-12 for every placement id, all distinct, none on an entrance lane or next to a Vault
        {
            int bad = 0;
            Grid g = base(CX, CZ);
            for (long i = 1; i <= 3000; i++) {
                Plan p = new Plan();
                ArenaDesign.Layout lay = ArenaDesign.build(p, g, i * 2654435761L, CX, CZ, FY);
                if (lay.columns.size() < 8 || lay.columns.size() > 12 || lay.columns.size() != ArenaDesign.columnCount(i * 2654435761L)) bad++;
                java.util.Set<Long> seen = new java.util.HashSet<>();
                for (int[] c : lay.columns) if (!seen.add(Plan.pack(c[0], 0, c[1]))) bad++;
            }
            ck(bad == 0, "3000 placement ids: 8-12 distinct columns each, count == columnCount(id): bad=" + bad);
        }
        persistence();
        System.out.println("ConstructionSelfTest pass=" + pass + " fail=" + fail + " ms=" + (System.currentTimeMillis() - t0));
        if (fail > 0) System.exit(1);
    }

    static boolean same(Corridors.Route a, Corridors.Route b) {
        if (a.cells.size() != b.cells.size()) return false;
        for (int i = 0; i < a.cells.size(); i++) if (a.cells.get(i).x != b.cells.get(i).x || a.cells.get(i).z != b.cells.get(i).z || a.cells.get(i).yf != b.cells.get(i).yf) return false;
        return true;
    }
    static final class Route0 { final List<int[]> cells = new ArrayList<>(); Route0(Corridors.Route R) { for (Corridors.Cell c : R.cells) if (c.s >= 10 && c.s <= 12) cells.add(new int[]{c.x, c.yf, c.z}); } }
    static final class Planner0 { static boolean hits(Corridors.Route R, int x0, int x1, int z0, int z1) { for (Corridors.Cell c : R.cells) if (c.x + 3 >= x0 && c.x - 3 <= x1 && c.z + 3 >= z0 && c.z - 3 <= z1) return true; return false; } }

    // ------------------------------------------------------------------------------------------------ persistence / recovery
    static void persistence() throws Exception {
        Path tmp = Files.createTempDirectory("st05f");
        Grid g = base(CX, CZ); hall(g, 47, 120, -22, 22, FY - 1, 16);
        g.fillBox(CX + 29, FY + 2, CZ + 8, CX + 36, FY + 7, CZ + 14, Kind.LAVA);
        ConstructionPlanner.Result r = planOn(g, 2024, FY, List.of());
        ConstructionPlanner.Cand c = r.chosen;
        int nb = Construction.batches(c.plan).size();
        ck(nb > 4, "enough batches to interrupt: " + nb);
        // a crash after k batches, then restart recovery must resume to the exact final state, for several k including 0, first, middle, last-1
        int[] ks = {1, 2, nb / 3, nb / 2, nb - 2, nb - 1};
        for (int k : ks) {
            Path dir = tmp.resolve("k" + k);
            Journal j = new Journal(dir);
            String id = "k" + k;
            GridWorld w = new GridWorld(g);
            j.append(id, "RESERVATION " + id + " " + CX + " " + CZ + " " + FY + " WIN");
            Construction.commitPlan(j, id, c.plan, c.idx, c.floorY, w);
            try { Construction.apply(j, id, c.plan, w, k, 3); ck(false, "crash expected at " + k); } catch (SimulatedCrash e) { }
            Rec mid = Construction.load(j, id);
            ck(mid.state == State.APPLYING && mid.batchesWritten == k, "journal shows APPLYING with " + k + " batches: " + mid.state + "/" + mid.batchesWritten);
            int before = Construction.mismatches(c.plan, w, null);
            ck(before > 0, "partial state differs from the plan after crash k=" + k);
            // "restart": fresh journal object, plan read back from disk, world untouched except the lost-in-memory simulation
            Rec after = Construction.recover(new Journal(dir), id, w);
            ck(after.state == State.APPLIED, "k=" + k + " resumed to APPLIED: " + after.state + " " + after.reason);
            ck(Construction.mismatches(c.plan, w, null) == 0, "k=" + k + " exact plan conformance after recovery");
            // second recovery is a no-op (no duplicate construction)
            int wr = w.writes;
            Rec again = Construction.recover(new Journal(dir), id, w);
            ck(w.writes == wr && again.state == State.APPLIED, "k=" + k + " second recovery performs no writes");
        }
        // lost-in-memory chunks: journal says batches written but the world still holds pre-state => resumable (cells at pre-state)
        {
            Path dir = tmp.resolve("lost"); Journal j = new Journal(dir); String id = "lost";
            GridWorld w = new GridWorld(g);
            j.append(id, "RESERVATION " + id + " " + CX + " " + CZ + " " + FY + " WIN");
            Construction.commitPlan(j, id, c.plan, c.idx, c.floorY, w);
            try { Construction.apply(j, id, c.plan, w, nb / 2, 0); } catch (SimulatedCrash e) { }
            w.over.clear();                                       // everything since the last durability point was lost
            Rec after = Construction.recover(new Journal(dir), id, w);
            ck(after.state == State.APPLIED && Construction.mismatches(c.plan, w, null) == 0, "lost unflushed edits are re-applied: " + after.state);
        }
        // foreign edit: a player/other mod changed a planned cell -> quarantine, nothing overwritten, nothing deleted
        {
            Path dir = tmp.resolve("foreign"); Journal j = new Journal(dir); String id = "foreign";
            GridWorld w = new GridWorld(g);
            j.append(id, "RESERVATION " + id + " " + CX + " " + CZ + " " + FY + " WIN");
            Construction.commitPlan(j, id, c.plan, c.idx, c.floorY, w);
            try { Construction.apply(j, id, c.plan, w, nb / 2, 0); } catch (SimulatedCrash e) { }
            long cell = c.plan.ops.get(c.plan.ops.size() - 1)[0];
            w.set(Plan.ux(cell), Plan.uy(cell), Plan.uz(cell), "minecraft:diamond_block");
            Map<Long, String> snap = new HashMap<>(w.over);
            Rec after = Construction.recover(new Journal(dir), id, w);
            ck(after.state == State.QUARANTINED && after.reason.startsWith("FOREIGN_BLOCKS"), "foreign block => QUARANTINED: " + after.state + " " + after.reason);
            ck(snap.equals(w.over), "quarantine writes nothing (no delete / overwrite)");
            Rec again = Construction.recover(new Journal(dir), id, w);
            ck(again.state == State.QUARANTINED && snap.equals(w.over), "quarantine is sticky and still writes nothing");
        }
        // torn final journal line is ignored; a corrupt middle line quarantines
        {
            Path dir = tmp.resolve("torn"); Journal j = new Journal(dir); String id = "torn";
            GridWorld w = new GridWorld(g);
            j.append(id, "RESERVATION " + id + " " + CX + " " + CZ + " " + FY + " WIN");
            Construction.commitPlan(j, id, c.plan, c.idx, c.floorY, w);
            try { Construction.apply(j, id, c.plan, w, 3, 0); } catch (SimulatedCrash e) { }
            Files.writeString(j.file(id, "journal"), "BATCH 99 WR", java.nio.file.StandardOpenOption.APPEND);   // torn write
            Rec r1 = Construction.load(new Journal(dir), id);
            ck(r1.state == State.APPLYING && r1.batchesWritten == 3, "torn last line ignored: " + r1.state + "/" + r1.batchesWritten);
            List<String> ls = Files.readAllLines(j.file(id, "journal"));
            ls.set(1, ls.get(1) + "X");
            Files.write(j.file(id, "journal"), ls);
            ck(Construction.load(new Journal(dir), id).state == State.QUARANTINED, "corrupt middle journal line => QUARANTINED");
        }
        // plan file tampered -> hash mismatch -> quarantine
        {
            Path dir = tmp.resolve("tamper"); Journal j = new Journal(dir); String id = "tamper";
            GridWorld w = new GridWorld(g);
            j.append(id, "RESERVATION " + id + " " + CX + " " + CZ + " " + FY + " WIN");
            Construction.commitPlan(j, id, c.plan, c.idx, c.floorY, w);
            Files.write(j.file(id, "plan.gz"), new byte[]{1, 2, 3});
            Rec r1 = Construction.recover(new Journal(dir), id, w);
            ck(r1.state == State.QUARANTINED, "unreadable plan => QUARANTINED: " + r1.state);
        }
        // completed placements are never rebuilt; failures never produce a new plan (no new generation opportunity)
        {
            Path dir = tmp.resolve("done"); Journal j = new Journal(dir); String id = "done";
            GridWorld w = new GridWorld(g);
            j.append(id, "RESERVATION " + id + " " + CX + " " + CZ + " " + FY + " WIN");
            Construction.commitPlan(j, id, c.plan, c.idx, c.floorY, w);
            Construction.apply(j, id, c.plan, w, -1, 4);
            j.append(id, "VERIFIED " + id);
            int wr = w.writes;
            Rec x = Construction.recover(new Journal(dir), id, w);
            ck(x.state == State.VERIFIED && w.writes == wr, "VERIFIED placement untouched on restart");
            Path d2 = tmp.resolve("fail"); Journal j2 = new Journal(d2); String id2 = "fail";
            j2.append(id2, "RESERVATION " + id2 + " " + CX + " " + CZ + " " + FY + " WIN");
            j2.append(id2, "CONSTRUCTION_FAILED NO_FIT");
            Rec f = Construction.recover(j2, id2, w);
            ck(f.state == State.CONSTRUCTION_FAILED && f.rollWin && f.cx == CX && f.cz == CZ && f.baseFloorY == FY, "failed construction keeps reservation (centre, floorY, roll WIN) and stays failed");
            ck(!Files.exists(j2.file(id2, "plan.gz")), "failure creates no plan / no new attempt");
        }
    }
}
