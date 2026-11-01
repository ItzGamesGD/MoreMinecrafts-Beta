package dev.itzgamesgd.moreminecrafts.worldgenprobe.stage0f;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.StringReader;
import dev.itzgamesgd.moreminecrafts.worldgenprobe.stage0f.Construction.*;
import dev.itzgamesgd.moreminecrafts.worldgenprobe.stage0f.Terrain.Kind;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/**
 * ST-05 Stage 0-F live driver (Minecraft 26.3 dedicated server): constructs the arena body and the cave-connection corridors for placements
 * that Stage 0-D/0-E already reserved (spacing + the single 75% roll are NEVER recomputed here), against the REAL final terrain.
 * Modes: f-run (plan + apply + verify new placements), f-crash (inject a hard crash mid-construction), f-recover (restart recovery),
 * f-verify (read-only re-verification, then a second run() that must perform zero writes).
 */
public final class St05Stage0FLive {
    private St05Stage0FLive() { }

    static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static volatile boolean pending = false;
    static final int R = ConstructionPlanner.BODY_R + 2 * Corridors.PATH_CAP + 16;      // inspected radius (118)

    record Res(String id, int cx, int cz, int floorY) { }

    static List<Res> reservations(Path f) throws Exception {
        List<Res> out = new ArrayList<>();
        for (String l : Files.readAllLines(f)) {
            l = l.trim();
            if (l.isEmpty() || l.startsWith("#")) continue;
            String[] p = l.split("\\s+");
            out.add(new Res(p[0], Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3])));
        }
        return out;
    }

    // ------------------------------------------------------------------------------------------------ world access
    static final class LevelWorld implements WorldAccess {
        final ServerLevel level; final MinecraftServer server;
        final BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        final Map<String, BlockState> parsed = new HashMap<>();
        long writes, saves;
        LevelWorld(MinecraftServer s, ServerLevel l) { server = s; level = l; }
        static String canonState(BlockState s) { return s.toString(); }
        BlockState parse(String spec) {
            return parsed.computeIfAbsent(spec, k -> {
                try { return BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK, new StringReader(k), false).blockState(); }
                catch (Exception e) { throw new IllegalStateException("bad block spec " + k, e); }
            });
        }
        public String get(int x, int y, int z) { return canonState(level.getBlockState(p.set(x, y, z))); }
        public void set(int x, int y, int z, String spec) {
            // 2 = send to clients, 16 = no neighbour shape update, 32 = no drops: a plan op must never cascade into unplanned edits
            level.setBlock(p.set(x, y, z), parse(spec), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS);
            writes++;
        }
        public String canon(String spec) { return canonState(parse(spec)); }
        public void flush() { server.saveAllChunks(true, true, true); saves++; }
    }

    // ------------------------------------------------------------------------------------------------ terrain snapshot
    static final class Snap {
        final Terrain.Grid grid; final int cx, cz, y0, y1;
        Snap(Terrain.Grid g, int cx, int cz, int y0, int y1) { grid = g; this.cx = cx; this.cz = cz; this.y0 = y0; this.y1 = y1; }
    }

    static final IdentityHashMap<BlockState, Kind> KIND = new IdentityHashMap<>();
    static Kind classify(BlockState s) {
        Kind k = KIND.get(s);
        if (k != null) return k;
        if (s.isAir()) k = Kind.AIR;
        else if (!s.getFluidState().isEmpty()) k = s.getFluidState().is(net.minecraft.tags.FluidTags.LAVA) ? Kind.LAVA : Kind.WATER;
        else if (s.getBlock() instanceof FallingBlock) k = Kind.GRAVITY;
        else if (s.getDestroySpeed(EmptyBlockGetter.INSTANCE, BlockPos.ZERO) < 0 || s.hasBlockEntity()) k = Kind.PROTECTED;     // bedrock, barrier, structure blocks; chests, spawners, sculk sensors, vaults, signs...
        else if (s.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).isEmpty()) k = Kind.AIR;                                                                                // vines, lichen, carpets, torches: clearable, non-solid
        else k = Kind.ROCK;
        KIND.put(s, k);
        return k;
    }

    static Snap snapshot(ServerLevel level, int cx, int cz, int baseFloorY, boolean generate) {
        int y0 = baseFloorY - 20, y1 = baseFloorY + 34;
        Terrain.Grid g = new Terrain.Grid(cx - R, y0, cz - R, 2 * R + 1, y1 - y0 + 1, 2 * R + 1, Kind.UNKNOWN);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int chx = Math.floorDiv(cx - R, 16); chx <= Math.floorDiv(cx + R, 16); chx++)
            for (int chz = Math.floorDiv(cz - R, 16); chz <= Math.floorDiv(cz + R, 16); chz++) {
                ChunkAccess ch = level.getChunk(chx, chz, ChunkStatus.FULL, generate);
                if (ch == null) continue;
                for (int x = Math.max(chx * 16, cx - R); x <= Math.min(chx * 16 + 15, cx + R); x++)
                    for (int z = Math.max(chz * 16, cz - R); z <= Math.min(chz * 16 + 15, cz + R); z++)
                        for (int y = y0; y <= y1; y++) g.set(x, y, z, classify(ch.getBlockState(p.set(x, y, z))));
            }
        return new Snap(g, cx, cz, y0, y1);
    }

    /** Other structures' actual piece boxes {minX,minY,minZ,maxX,maxY,maxZ} that could touch the inspected area (WG-Q27, conservative). */
    static List<int[]> structureBoxes(ServerLevel level, int cx, int cz, Map<String, Integer> hits) {
        List<int[]> out = new ArrayList<>();
        int reach = R + 160;
        for (int chx = Math.floorDiv(cx - reach, 16); chx <= Math.floorDiv(cx + reach, 16); chx++)
            for (int chz = Math.floorDiv(cz - reach, 16); chz <= Math.floorDiv(cz + reach, 16); chz++) {
                ChunkAccess ch = level.getChunk(chx, chz, ChunkStatus.STRUCTURE_STARTS, true);
                for (var e : ch.getAllStarts().entrySet()) {
                    StructureStart st = e.getValue();
                    if (!st.isValid()) continue;
                    String name = String.valueOf(level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getKey(e.getKey()));
                    boolean near = false;
                    for (var piece : st.getPieces()) {
                        var b = piece.getBoundingBox();
                        if (b.maxX() < cx - R || b.minX() > cx + R || b.maxZ() < cz - R || b.minZ() > cz + R) continue;
                        out.add(new int[]{b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ()});
                        near = true;
                    }
                    if (near) hits.merge(name, 1, Integer::sum);
                }
            }
        return out;
    }

    // ------------------------------------------------------------------------------------------------ meta (routes + cave evidence)
    static String metaOf(ConstructionPlanner.Cand c, Corridors.Cave cave) {
        StringBuilder sb = new StringBuilder();
        sb.append("variant ").append(c.layout.variant).append('\n');
        for (Corridors.Route r : c.routes) {
            sb.append("route ").append(r.dir).append(' ').append(r.cells.size()).append('\n');
            for (Corridors.Cell k : r.cells) sb.append(k.x).append(' ').append(k.z).append(' ').append(k.yf).append(' ').append(k.h).append(' ').append(k.s).append(' ').append(k.dy).append('\n');
            Corridors.Cell e = r.cells.get(r.cells.size() - 1);
            for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) for (int dy = -2; dy <= 2; dy++)
                if (cave.standing.contains(Plan.pack(e.x + dx, e.yf + dy, e.z + dz))) sb.append("cave ").append(e.x + dx).append(' ').append(e.yf + dy).append(' ').append(e.z + dz).append('\n');
        }
        return sb.toString();
    }

    static final class Meta { ArenaDesign.Variant variant; final List<Corridors.Route> routes = new ArrayList<>(); final Corridors.Cave cave = new Corridors.Cave(); }

    static Meta readMeta(String s) {
        Meta m = new Meta();
        Corridors.Route cur = null;
        for (String l : s.split("\n")) {
            String[] f = l.split(" ");
            if (f[0].equals("variant")) m.variant = ArenaDesign.Variant.valueOf(f[1]);
            else if (f[0].equals("route")) { cur = new Corridors.Route(); cur.dir = Integer.parseInt(f[1]); cur.ok = true; m.routes.add(cur); }
            else if (f[0].equals("cave")) m.cave.standing.add(Plan.pack(Integer.parseInt(f[1]), Integer.parseInt(f[2]), Integer.parseInt(f[3])));
            else if (f.length == 6 && cur != null) {
                Corridors.Cell k = new Corridors.Cell();
                k.x = Integer.parseInt(f[0]); k.z = Integer.parseInt(f[1]); k.yf = Integer.parseInt(f[2]); k.h = Integer.parseInt(f[3]); k.s = Integer.parseInt(f[4]); k.dy = Integer.parseInt(f[5]);
                cur.cells.add(k); cur.length = k.s;
            }
        }
        return m;
    }

    // ------------------------------------------------------------------------------------------------ per-placement handling
    static Map<String, Object> handle(MinecraftServer server, ServerLevel level, LevelWorld w, Journal j, Res r, int crashAfter, boolean allowPlan, List<Runnable> afterTicks) throws Exception {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("id", r.id()); d.put("center", r.cx() + "," + r.cz()); d.put("reserved_floorY", r.floorY());
        if (j.read(r.id()) == null || j.read(r.id()).isEmpty()) j.append(r.id(), "RESERVATION " + r.id() + " " + r.cx() + " " + r.cz() + " " + r.floorY() + " WIN");
        Rec rec = Construction.load(j, r.id());
        d.put("state_at_entry", rec.state.name());
        long t0 = System.nanoTime();
        long wr0 = w.writes;
        switch (rec.state) {
            case RESERVED -> {
                if (!allowPlan) { d.put("action", "SKIPPED_NOT_ALLOWED"); break; }
                plan(server, level, w, j, r, crashAfter, d);
            }
            case PLANNED, APPLYING -> {
                forceChunks(level, r, true);
                int rc = Integer.getInteger("mm.st05f.recovercrash", -1);
                Rec after;
                try { after = Construction.recover(j, r.id(), w, rc); }
                catch (SimulatedCrash e) { System.out.println("ST05F_CRASH_DURING_RECOVERY id=" + r.id() + " after_batches=" + rc); Runtime.getRuntime().halt(137); return d; }
                d.put("action", "RECOVERED"); d.put("state_after_recovery", after.state.name()); d.put("journal_tail", tail(j, r.id()));
                if (after.state == State.APPLIED) verify(server, level, w, j, r, d, afterTicks);
                forceChunks(level, r, false);
            }
            case APPLIED -> { forceChunks(level, r, true); verify(server, level, w, j, r, d, afterTicks); forceChunks(level, r, false); }
            case VERIFIED -> {
                // read-only re-verification; the placement must never be rebuilt
                forceChunks(level, r, true);
                Plan plan = j.readPlan(r.id());
                int mm = Construction.mismatches(plan, w, null);
                d.put("action", "REVERIFIED_READ_ONLY"); d.put("plan_mismatches", mm);
                Meta m = readMeta(Files.readString(j.file(r.id(), "meta")));
                Snap after = snapshot(level, r.cx(), r.cz(), r.floorY(), false);
                Map<String, Object> meas = new LinkedHashMap<>();
                List<String> f = Verify.check(after.grid, plan, m.routes, m.cave, r.cx(), r.cz(), rec.floorY, m.variant, 7, meas);
                d.put("verify_failures", f); d.put("measured", meas);
                forceChunks(level, r, false);
            }
            default -> { d.put("action", "TERMINAL_NO_ACTION"); d.put("reason", rec.reason); }
        }
        d.put("writes", w.writes - wr0);
        d.put("ms", (System.nanoTime() - t0) / 1_000_000);
        Rec fin = Construction.load(j, r.id());
        d.put("final_state", fin.state.name()); d.put("final_reason", fin.reason);
        d.put("reservation_preserved", fin.cx == r.cx() && fin.cz == r.cz() && fin.baseFloorY == r.floorY() && fin.rollWin);
        return d;
    }

    static List<String> tail(Journal j, String id) throws Exception {
        List<String> ls = j.read(id);
        return ls == null ? List.of("CORRUPT") : ls.subList(Math.max(0, ls.size() - 6), ls.size());
    }

    static void forceChunks(ServerLevel level, Res r, boolean on) {
        for (int x = Math.floorDiv(r.cx() - R, 16); x <= Math.floorDiv(r.cx() + R, 16); x++)
            for (int z = Math.floorDiv(r.cz() - R, 16); z <= Math.floorDiv(r.cz() + R, 16); z++) level.setChunkForced(x, z, on);
    }

    static void plan(MinecraftServer server, ServerLevel level, LevelWorld w, Journal j, Res r, int crashAfter, Map<String, Object> d) throws Exception {
        long t0 = System.nanoTime();
        forceChunks(level, r, true);
        Snap snap = snapshot(level, r.cx(), r.cz(), r.floorY(), true);
        Map<String, Integer> hits = new TreeMap<>();
        List<int[]> boxes = structureBoxes(level, r.cx(), r.cz(), hits);
        d.put("snapshot_ms", (System.nanoTime() - t0) / 1_000_000);
        d.put("structure_pieces_near", boxes.size()); d.put("structure_types_near", hits);
        long id = Long.parseUnsignedLong(r.id(), 16);
        long t1 = System.nanoTime();
        ConstructionPlanner.Result pr = ConstructionPlanner.plan(snap.grid, id, r.cx(), r.cz(), r.floorY(), boxes);
        d.put("planning_ms", (System.nanoTime() - t1) / 1_000_000);
        List<Object> cs = new ArrayList<>();
        for (ConstructionPlanner.Cand c : pr.cands) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("idx", c.idx); m.put("floorY", c.floorY); m.put("ok", c.ok); m.put("reason", c.reason); m.put("score_milli", c.scoreMilli); m.put("parts", c.parts); m.put("route_work", c.work);
            if (c.ok) { m.put("hash", c.hash); m.put("cells", c.plan.size()); m.put("stats", c.plan.stats); m.put("routes", c.routes.stream().map(x -> "dir=" + x.dir + " len=" + x.length + " stairs=" + x.stairCells + " turns=" + x.turns).toList()); }
            cs.add(m);
        }
        d.put("candidates", cs);
        d.put("cave_components", pr.cave.components); d.put("cave_qualified_components", pr.cave.qualified);
        if (pr.chosen == null) {
            j.append(r.id(), "CONSTRUCTION_FAILED " + pr.status.replace('\n', ' '));
            d.put("action", "CONSTRUCTION_FAILED_NO_FIT"); d.put("status", pr.status);
            forceChunks(level, r, false);
            return;
        }
        ConstructionPlanner.Cand c = pr.chosen;
        d.put("chosen_candidate", c.idx); d.put("chosen_floorY", c.floorY); d.put("variant", c.layout.variant.name()); d.put("plan_hash", c.hash);
        d.put("plan_cells", c.plan.size()); d.put("plan_ops", c.plan.ops.size()); d.put("plan_stats", c.plan.stats);
        Files.writeString(j.file(r.id(), "meta"), metaOf(c, pr.cave), StandardCharsets.UTF_8);
        Construction.commitPlan(j, r.id(), c.plan, c.idx, c.floorY, w);
        d.put("batches", Construction.batches(c.plan).size());
        long t2 = System.nanoTime();
        if (crashAfter >= 0) {
            try { Construction.apply(j, r.id(), c.plan, w, crashAfter, 4); }
            catch (SimulatedCrash e) {
                // hard crash: no further save, no shutdown hooks; whatever reached disk at the last flush is all that survives
                System.out.println("ST05F_CRASH_INJECTED id=" + r.id() + " after_batches=" + crashAfter);
                Runtime.getRuntime().halt(137);
            }
        } else Construction.apply(j, r.id(), c.plan, w, -1, 4);
        d.put("apply_ms", (System.nanoTime() - t2) / 1_000_000);
        d.put("action", "CONSTRUCTED");
        Meta m = new Meta(); m.variant = c.layout.variant; m.routes.addAll(c.routes); m.cave.standing.addAll(pr.cave.standing);
        forceChunks(level, r, true);
        verifyWith(server, level, w, j, r, d, c.plan, m, c.floorY, null);
        forceChunks(level, r, false);
    }

    static void verify(MinecraftServer server, ServerLevel level, LevelWorld w, Journal j, Res r, Map<String, Object> d, List<Runnable> afterTicks) throws Exception {
        Rec rec = Construction.load(j, r.id());
        Plan plan = j.readPlan(r.id());
        Meta m = readMeta(Files.readString(j.file(r.id(), "meta")));
        verifyWith(server, level, w, j, r, d, plan, m, rec.floorY, afterTicks);
    }

    static void verifyWith(MinecraftServer server, ServerLevel level, LevelWorld w, Journal j, Res r, Map<String, Object> d, Plan plan, Meta m, int floorY, List<Runnable> afterTicks) throws Exception {
        List<String> samples = new ArrayList<>();
        int mm = Construction.mismatches(plan, w, samples);
        Snap after = snapshot(level, r.cx(), r.cz(), r.floorY(), false);
        Map<String, Object> meas = new LinkedHashMap<>();
        List<String> f = new ArrayList<>(Verify.check(after.grid, plan, m.routes, m.cave, r.cx(), r.cz(), floorY, m.variant, 7, meas));
        // real block identities of the seven Vaults
        int vaults = 0; boolean ominousCenter = false;
        for (long c : plan.cells()) {
            String spec = plan.finalSpecAt(Plan.ux(c), Plan.uy(c), Plan.uz(c));
            if (!spec.startsWith("minecraft:vault")) continue;
            BlockState s = level.getBlockState(new BlockPos(Plan.ux(c), Plan.uy(c), Plan.uz(c)));
            if (s.is(net.minecraft.world.level.block.Blocks.VAULT)) vaults++;
            if (Plan.ux(c) == r.cx() && Plan.uz(c) == r.cz() && s.getValue(net.minecraft.world.level.block.VaultBlock.OMINOUS)) ominousCenter = true;
        }
        if (vaults != 7) f.add("VAULT_BLOCKS " + vaults);
        if (!ominousCenter) f.add("CENTER_VAULT_NOT_OMINOUS");
        meas.put("vault_blocks", vaults); meas.put("ominous_vault_at_exact_center", ominousCenter);
        if (mm > 0) f.add("PLAN_MISMATCH x" + mm + " " + samples);
        d.put("plan_mismatches", mm); d.put("verify_failures", f); d.put("measured", meas);
        if (f.isEmpty()) j.append(r.id(), "VERIFIED " + r.id() + " " + plan.hash());
        else j.append(r.id(), "QUARANTINED VERIFY_FAILED " + String.join(";", f).replace('\n', ' '));
        // fluid re-entry stress: schedule neighbour updates on every plan cell that touches natural fluid, to be re-checked after ticks
        if (afterTicks != null) {
            int touched = stress(level, plan, after.grid);
            d.put("fluid_boundary_cells_stressed", touched);
        }
    }

    static int stress(ServerLevel level, Plan plan, Terrain.Grid before) {
        int n = 0;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (long c : plan.cells()) {
            int x = Plan.ux(c), y = Plan.uy(c), z = Plan.uz(c);
            boolean near = false;
            for (int[] dd : ConstructionPlanner.N6) if (before.fluid(x + dd[0], y + dd[1], z + dd[2])) { near = true; break; }
            if (!near) continue;
            level.neighborChanged(p.set(x, y, z), level.getBlockState(p).getBlock(), null);
            n++;
        }
        return n;
    }

    // ------------------------------------------------------------------------------------------------ entry point
    public static void run(MinecraftServer server, ServerLevel level, String mode, Path out, Map<String, Object> doc) throws Exception {
        Path res = Path.of(System.getProperty("mm.st05f.res"));
        Path stateDir = Path.of(System.getProperty("mm.st05f.statedir"));
        Journal j = new Journal(stateDir);
        LevelWorld w = new LevelWorld(server, level);
        List<Res> all = reservations(res);
        if (Boolean.getBoolean("mm.st05f.reverse")) java.util.Collections.reverse(all);   // generation-order independence check
        doc.put("mode", mode); doc.put("reservations", all.size()); doc.put("algo", "st05-stage0f-construction-v1");
        List<Object> rows = new ArrayList<>();
        doc.put("placements", rows);
        List<Runnable> afterTicks = new ArrayList<>();
        String only = System.getProperty("mm.st05f.only");
        int crashAfter = Integer.getInteger("mm.st05f.crashafter", -1);
        boolean doPlan = mode.equals("f-run") || mode.equals("f-crash");
        for (Res r : all) {
            if (only != null && !only.contains(r.id())) continue;
            int ca = mode.equals("f-crash") && r.id().equals(System.getProperty("mm.st05f.crashid")) ? crashAfter : -1;
            if (mode.equals("f-crash") && ca < 0 && !r.id().equals(System.getProperty("mm.st05f.crashid"))) continue;
            rows.add(handle(server, level, w, j, r, ca, doPlan, afterTicks));
        }
        doc.put("world_writes", w.writes); doc.put("chunk_saves", w.saves);
        if (mode.equals("f-verify")) {
            // idempotency: a second full pass over every placement must perform zero writes
            long before = w.writes;
            List<Object> second = new ArrayList<>();
            for (Res r : all) second.add(handle(server, level, w, j, r, -1, true, null));
            doc.put("second_pass_writes", w.writes - before);
            doc.put("second_pass_states", second.stream().map(x -> ((Map<?, ?>) x).get("final_state")).toList());
        }
        int settle = Integer.getInteger("mm.st05f.settle", mode.equals("f-run") ? 600 : 0);
        if (settle > 0) {
            pending = true;
            int[] left = {settle};
            ServerTickEvents.END_SERVER_TICK.register(s -> {
                if (--left[0] > 0) return;
                try {
                    List<Object> post = new ArrayList<>();
                    for (Res r : all) {
                        if (only != null && !only.contains(r.id())) continue;
                        Rec rec = Construction.load(j, r.id());
                        if (rec.state != State.VERIFIED) continue;
                        forceChunks(level, r, true);
                        Plan plan = j.readPlan(r.id());
                        Meta m = readMeta(Files.readString(j.file(r.id(), "meta")));
                        Snap after = snapshot(level, r.cx(), r.cz(), r.floorY(), false);
                        Map<String, Object> meas = new LinkedHashMap<>();
                        List<String> f = Verify.check(after.grid, plan, m.routes, m.cave, r.cx(), r.cz(), rec.floorY, m.variant, 7, meas);
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("id", r.id()); row.put("ticks_waited", settle); row.put("plan_mismatches", Construction.mismatches(plan, w, null)); row.put("verify_failures", f);
                        post.add(row);
                        if (!f.isEmpty() || (int) row.get("plan_mismatches") > 0) j.append(r.id(), "QUARANTINED POST_TICK_DRIFT " + f);
                        forceChunks(level, r, false);
                    }
                    doc.put("post_tick_reverification", post);
                    s.saveAllChunks(true, true, true);
                    Files.createDirectories(out.getParent());
                    Files.writeString(out, GSON.toJson(doc), StandardCharsets.UTF_8);
                } catch (Throwable t) {
                    try { Files.writeString(out, GSON.toJson(Map.of("error", String.valueOf(t))), StandardCharsets.UTF_8); } catch (Exception ignored) { }
                    Runtime.getRuntime().halt(1);
                }
                Runtime.getRuntime().halt(0);
            });
        }
    }
}
