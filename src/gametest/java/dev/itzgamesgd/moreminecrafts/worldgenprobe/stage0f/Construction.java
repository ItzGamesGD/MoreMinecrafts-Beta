package dev.itzgamesgd.moreminecrafts.worldgenprobe.stage0f;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.CRC32;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Persistence, idempotent application and crash recovery of an approved construction plan (RESOLVE Q06/Q07/Q18/Q20, WG-Q28).
 * Reservation (decided earlier: spacing + the single 75% roll) and construction progress are SEPARATE records; a construction failure of any
 * kind never alters the reservation, never rerolls, never reactivates a spacing loser. The persisted plan - not the current terrain - is
 * the authority for recovery. Multi-chunk atomicity is NOT assumed: every cell is compared against {pre-state, any planned state}.
 */
public final class Construction {
    private Construction() { }

    public enum State { RESERVED, PLANNED, APPLYING, APPLIED, VERIFIED, CONSTRUCTION_FAILED, QUARANTINED }

    public interface WorldAccess {
        /** Canonical block state string, or null when the position is not loaded / not readable. */
        String get(int x, int y, int z);
        void set(int x, int y, int z, String spec);
        String canon(String spec);
        /** Durability point (flush chunks to disk). */
        void flush();
    }

    public static final class SimulatedCrash extends RuntimeException {
        public SimulatedCrash(String m) { super(m); }
    }

    // ------------------------------------------------------------------------------------------------ journal
    public static final class Journal {
        final Path dir;
        public Journal(Path dir) throws IOException { this.dir = dir; Files.createDirectories(dir); }
        Path file(String id, String ext) { return dir.resolve("f-" + id + "." + ext); }

        static String crc(String s) { CRC32 c = new CRC32(); c.update(s.getBytes(StandardCharsets.UTF_8)); return Long.toHexString(c.getValue()); }

        public synchronized void append(String id, String line) throws IOException {
            String l = line + " #" + crc(line) + "\n";
            try (FileChannel ch = FileChannel.open(file(id, "journal"), StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE)) {
                ch.write(java.nio.ByteBuffer.wrap(l.getBytes(StandardCharsets.UTF_8)));
                ch.force(true);
            }
        }

        /** Valid lines; a torn (CRC-failing) FINAL line is ignored, a corrupt line elsewhere makes the journal untrustworthy (returns null). */
        public List<String> read(String id) throws IOException {
            Path p = file(id, "journal");
            if (!Files.exists(p)) return new ArrayList<>();
            List<String> raw = Files.readAllLines(p, StandardCharsets.UTF_8), out = new ArrayList<>();
            for (int i = 0; i < raw.size(); i++) {
                String l = raw.get(i);
                int h = l.lastIndexOf(" #");
                boolean ok = h > 0 && crc(l.substring(0, h)).equals(l.substring(h + 2));
                if (!ok) { if (i == raw.size() - 1) continue; return null; }
                out.add(l.substring(0, h));
            }
            return out;
        }

        public void writePlan(String id, Plan plan) throws IOException {
            try (DataOutputStream o = new DataOutputStream(new GZIPOutputStream(Files.newOutputStream(file(id, "plan.gz"))))) {
                o.writeInt(plan.palette.size());
                for (String s : plan.palette) o.writeUTF(s);
                o.writeInt(plan.ops.size());
                for (long[] op : plan.ops) { o.writeLong(op[0]); o.writeInt((int) op[1]); o.writeByte((int) op[2]); }
            }
        }

        public Plan readPlan(String id) throws IOException {
            Plan p = new Plan();
            try (DataInputStream in = new DataInputStream(new GZIPInputStream(Files.newInputStream(file(id, "plan.gz"))))) {
                int n = in.readInt();
                List<String> pal = new ArrayList<>();
                for (int i = 0; i < n; i++) pal.add(in.readUTF());
                int m = in.readInt();
                for (int i = 0; i < m; i++) {
                    long pos = in.readLong(); int si = in.readInt(); int ph = in.readByte();
                    p.put(ph, Plan.ux(pos), Plan.uy(pos), Plan.uz(pos), pal.get(si));
                }
            }
            p.finish();
            for (long c : p.cells()) { String s = p.finalSpecAt(Plan.ux(c), Plan.uy(c), Plan.uz(c)); if (s.equals(Plan.AIR)) p.open.add(c); else p.solid.add(c); }
            return p;
        }

        public void writePre(String id, Map<Long, String> pre) throws IOException {
            try (DataOutputStream o = new DataOutputStream(new GZIPOutputStream(Files.newOutputStream(file(id, "pre.gz"))))) {
                o.writeInt(pre.size());
                for (Map.Entry<Long, String> e : new TreeMap<>(pre).entrySet()) { o.writeLong(e.getKey()); o.writeUTF(e.getValue() == null ? "?" : e.getValue()); }
            }
        }

        public Map<Long, String> readPre(String id) throws IOException {
            Map<Long, String> m = new HashMap<>();
            try (DataInputStream in = new DataInputStream(new GZIPInputStream(Files.newInputStream(file(id, "pre.gz"))))) {
                int n = in.readInt();
                for (int i = 0; i < n; i++) { long k = in.readLong(); m.put(k, in.readUTF()); }
            }
            return m;
        }
    }

    /** Latest valid state of a placement from its journal (null journal => QUARANTINED). */
    public static final class Rec {
        public String id = "", reason = "", planHash = "";
        public State state = State.RESERVED;
        public int cx, cz, baseFloorY, floorY, cand = -1, batches, batchesWritten;
        public boolean rollWin, journalCorrupt;
    }

    public static Rec load(Journal j, String id) throws IOException {
        Rec r = new Rec();
        r.id = id;
        List<String> ls = j.read(id);
        if (ls == null) { r.state = State.QUARANTINED; r.journalCorrupt = true; r.reason = "JOURNAL_CORRUPT_MIDDLE_LINE"; return r; }
        for (String l : ls) {
            String[] f = l.split(" ");
            switch (f[0]) {
                case "RESERVATION" -> { r.cx = Integer.parseInt(f[2]); r.cz = Integer.parseInt(f[3]); r.baseFloorY = Integer.parseInt(f[4]); r.rollWin = f[5].equals("WIN"); r.state = State.RESERVED; }
                case "PLANNED" -> { r.state = State.PLANNED; r.cand = Integer.parseInt(f[2]); r.floorY = Integer.parseInt(f[3]); r.planHash = f[4]; r.batches = Integer.parseInt(f[5]); }
                case "APPLYING" -> r.state = State.APPLYING;
                case "BATCH" -> r.batchesWritten = Math.max(r.batchesWritten, Integer.parseInt(f[1]) + 1);
                case "APPLIED" -> r.state = State.APPLIED;
                case "VERIFIED" -> r.state = State.VERIFIED;
                case "CONSTRUCTION_FAILED" -> { r.state = State.CONSTRUCTION_FAILED; r.reason = l.substring(l.indexOf(' ') + 1); }
                case "QUARANTINED" -> { r.state = State.QUARANTINED; r.reason = l.substring(l.indexOf(' ') + 1); }
                default -> { }
            }
        }
        return r;
    }

    // ------------------------------------------------------------------------------------------------ apply
    /** Batches: per phase, per chunk column in sorted order. Returns {phase, chunkX, chunkZ, firstOpIndex..} grouped op index lists. */
    public static List<List<long[]>> batches(Plan plan) {
        TreeMap<String, List<long[]>> m = new TreeMap<>();
        for (long[] op : plan.ops) {
            String key = String.format("%d|%08d|%08d", op[2], (Plan.ux(op[0]) >> 4) + 1_000_000, (Plan.uz(op[0]) >> 4) + 1_000_000);
            m.computeIfAbsent(key, k -> new ArrayList<>()).add(op);
        }
        return new ArrayList<>(m.values());
    }

    public static Map<Long, String> capturePre(Plan plan, WorldAccess w) {
        Map<Long, String> pre = new HashMap<>();
        for (long c : plan.cells()) pre.put(c, w.get(Plan.ux(c), Plan.uy(c), Plan.uz(c)));
        return pre;
    }

    /** Record a ready plan: reservation untouched, plan + pre-state persisted BEFORE the first world edit. */
    public static void commitPlan(Journal j, String id, Plan plan, int cand, int floorY, WorldAccess w) throws IOException {
        Map<Long, String> pre = capturePre(plan, w);
        for (String s : pre.values()) if (s == null) throw new IOException("plan cell not readable at commit time");
        j.writePre(id, pre);
        j.writePlan(id, plan);
        j.append(id, "PLANNED " + id + " " + cand + " " + floorY + " " + plan.hash() + " " + batches(plan).size());
    }

    /**
     * Idempotent application of the persisted plan. {@code crashAfterBatches >= 0} injects a crash after that many batches (tests only).
     * Every batch is journalled after it is written; a flush (durability point) is journalled every {@code flushEvery} batches.
     */
    public static void apply(Journal j, String id, Plan plan, WorldAccess w, int crashAfterBatches, int flushEvery) throws IOException {
        j.append(id, "APPLYING " + id);
        List<List<long[]>> bs = batches(plan);
        for (int i = 0; i < bs.size(); i++) {
            for (long[] op : bs.get(i)) w.set(Plan.ux(op[0]), Plan.uy(op[0]), Plan.uz(op[0]), plan.palette.get((int) op[1]));
            j.append(id, "BATCH " + i + " WRITTEN");
            if (flushEvery > 0 && (i + 1) % flushEvery == 0) { w.flush(); j.append(id, "FLUSHED " + i); }
            if (crashAfterBatches >= 0 && i + 1 == crashAfterBatches) throw new SimulatedCrash("crash after batch " + i);
        }
        w.flush();
        j.append(id, "APPLIED " + id);
    }

    // ------------------------------------------------------------------------------------------------ recovery
    public static final class Decision {
        public boolean resume;
        public String quarantine = "";
        public int atPre, atIntermediate, atFinal, foreign, unreadable;
        public long firstForeign = Long.MIN_VALUE;
        public String canonical() { return "resume=" + resume + " pre=" + atPre + " mid=" + atIntermediate + " final=" + atFinal + " foreign=" + foreign + " unreadable=" + unreadable + " q=" + quarantine; }
    }

    /**
     * Resume is allowed only when EVERY plan cell is in {pre-state} U {a planned state}. Anything else (a player edit, another mod, a corrupt
     * pre-state) is quarantined: nothing is deleted, overwritten, rerolled or re-planned.
     */
    public static Decision decide(Plan plan, Map<Long, String> pre, WorldAccess w) {
        Decision d = new Decision();
        Map<Long, java.util.Set<Integer>> acc = plan.acceptable();
        for (long c : new TreeSet<>(plan.cells())) {
            String now = w.get(Plan.ux(c), Plan.uy(c), Plan.uz(c));
            if (now == null) { d.unreadable++; continue; }
            String fin = w.canon(plan.finalSpecAt(Plan.ux(c), Plan.uy(c), Plan.uz(c)));
            if (now.equals(fin)) { d.atFinal++; continue; }
            if (now.equals(pre.get(c))) { d.atPre++; continue; }
            boolean mid = false;
            for (int si : acc.get(c)) if (now.equals(w.canon(plan.palette.get(si)))) { mid = true; break; }
            if (mid) d.atIntermediate++;
            else { d.foreign++; if (d.firstForeign == Long.MIN_VALUE) d.firstForeign = c; }
        }
        if (d.unreadable > 0) d.quarantine = "UNREADABLE_CELLS(" + d.unreadable + ")";
        else if (d.foreign > 0) d.quarantine = "FOREIGN_BLOCKS(" + d.foreign + ")";
        d.resume = d.quarantine.isEmpty();
        return d;
    }

    /** Full-plan conformance (used after APPLIED and on restart of a VERIFIED placement): number of cells not at their final state. */
    public static int mismatches(Plan plan, WorldAccess w, List<String> samples) {
        int bad = 0;
        for (long c : new TreeSet<>(plan.cells())) {
            String now = w.get(Plan.ux(c), Plan.uy(c), Plan.uz(c));
            String fin = w.canon(plan.finalSpecAt(Plan.ux(c), Plan.uy(c), Plan.uz(c)));
            if (!fin.equals(now)) { bad++; if (samples != null && samples.size() < 8) samples.add(Plan.ux(c) + "," + Plan.uy(c) + "," + Plan.uz(c) + " want " + fin + " have " + now); }
        }
        return bad;
    }

    /**
     * Restart driver: returns the resulting state. VERIFIED/FAILED/QUARANTINED are terminal and untouched. A PLANNED/APPLYING placement is
     * validated against the persisted plan and either resumed (idempotent re-application) or quarantined.
     */
    public static Rec recover(Journal j, String id, WorldAccess w) throws IOException { return recover(j, id, w, -1); }

    /** {@code crashAfter >= 0} injects a crash inside the resumed application (tests: a crash during recovery). */
    public static Rec recover(Journal j, String id, WorldAccess w, int crashAfter) throws IOException {
        Rec r = load(j, id);
        if (r.state == State.RESERVED || r.state == State.VERIFIED || r.state == State.CONSTRUCTION_FAILED || r.state == State.QUARANTINED) return r;
        Plan plan;
        Map<Long, String> pre;
        try { plan = j.readPlan(id); pre = j.readPre(id); }
        catch (Exception e) { j.append(id, "QUARANTINED PERSISTED_PLAN_UNREADABLE " + e.getClass().getSimpleName()); return load(j, id); }
        if (!plan.hash().equals(r.planHash)) { j.append(id, "QUARANTINED PLAN_HASH_MISMATCH"); return load(j, id); }
        Decision d = decide(plan, pre, w);
        j.append(id, "RECOVERY " + d.canonical());
        if (!d.resume) { j.append(id, "QUARANTINED " + d.quarantine); return load(j, id); }
        if (r.state == State.APPLIED) { return r; }
        apply(j, id, plan, w, crashAfter, 8);
        return load(j, id);
    }
}
