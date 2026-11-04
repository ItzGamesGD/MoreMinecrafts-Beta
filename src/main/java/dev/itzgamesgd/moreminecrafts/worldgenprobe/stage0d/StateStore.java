package dev.itzgamesgd.moreminecrafts.worldgenprobe.stage0d;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Stage 0-D probe persistence: line-oriented, per-record SHA-256, trailer chain hash, atomic replace, quarantine-by-rename (never delete).
 * This is a sidecar-file experiment, NOT native SavedData atomicity (that remains out of scope / NOT RUN).
 */
public final class StateStore {
    public static final String FORMAT = "ST05D-STATE-1";

    public record Header(String algo, String params, long seed, String snapshotKind) {
        String line() { return FORMAT + " algo=" + algo + " params=" + params + " seed=" + seed + " snapshot=" + snapshotKind; }
    }

    /** Chamber decision record. {@code finalized} decisions are immutable. */
    public record Rec(long id, long anchor, int ownerRx, int ownerRz, int size, long setHash, boolean suitable, int cx, int cz, String status, boolean rollWin, boolean finalized, String proof) {
        String body() {
            return "C id=" + Long.toHexString(id) + " anchor=" + Long.toHexString(anchor) + " owner=" + ownerRx + "," + ownerRz + " size=" + size
                + " set=" + Long.toHexString(setHash) + " suit=" + (suitable ? 1 : 0) + " c=" + cx + "," + cz + " st=" + status + " roll=" + (rollWin ? 1 : 0)
                + " final=" + (finalized ? 1 : 0) + " proof=" + proof;
        }
        String decision() { return status + "/" + (rollWin ? 1 : 0) + "/" + (suitable ? 1 : 0) + "/" + cx + "," + cz + "/" + setHash + "/" + anchor; }
    }

    /** Cumulative analysis budget for one unresolved group; once exhausted it stays exhausted. */
    public record Budget(String group, long used, long cap, boolean exhausted) {
        String body() { return "B group=" + group + " used=" + used + " cap=" + cap + " exhausted=" + (exhausted ? 1 : 0); }
    }

    public static final class Loaded {
        public Header header;
        public boolean headerOk, trailerOk, existed;
        public final Map<Long, Rec> recs = new TreeMap<>(Long::compareUnsigned);
        public final Map<String, Budget> budgets = new TreeMap<>();
        public final List<String> quarantinedLines = new ArrayList<>();
        public Path quarantinedFile;
        public boolean damaged() { return !quarantinedLines.isEmpty() || (existed && !trailerOk) || quarantinedFile != null; }
    }

    public static boolean isFinalStatus(String s) {
        return switch (s) {
            case "ACCEPTED", "REJECTED_SPACING", "NOT_SUITABLE", "NOT_QUALIFYING", "REJECTED_STRUCTURE", "REJECTED_FOOTPRINT", "ROLL_FAILED", "ROLL_WON_PREFLIGHT_FAILED", "PLACEMENT_RESERVED" -> true;
            default -> false;
        };
    }

    static String stamp(String body) { return body + " #" + ChamberCore.sha256(body).substring(0, 16); }

    public static Rec fromChamber(ChamberCore.Chamber c, boolean allowFinal) {
        boolean fin = allowFinal && isFinalStatus(c.status);
        String ev = c.id + ":" + c.anchor + ":" + c.size + ":" + c.setHash + ":" + c.suitable + ":" + c.cx + "," + c.cz + ":" + ChamberCore.ALGO;
        return new Rec(c.id, c.anchor, c.ownerRx, c.ownerRz, c.size, c.setHash, c.suitable, c.cx, c.cz, c.status, c.rollWin, fin, ChamberCore.sha256(ev).substring(0, 24));
    }

    public static void write(Path file, Header h, Map<Long, Rec> recs, Map<String, Budget> budgets) throws IOException {
        StringBuilder sb = new StringBuilder();
        StringBuilder chain = new StringBuilder();
        sb.append(stamp(h.line())).append('\n');
        int n = 0;
        for (Rec r : recs.values()) { String l = stamp(r.body()); sb.append(l).append('\n'); chain.append(l); n++; }
        for (Budget b : budgets.values()) { String l = stamp(b.body()); sb.append(l).append('\n'); chain.append(l); n++; }
        sb.append(stamp("END n=" + n + " chain=" + ChamberCore.sha256(chain.toString()).substring(0, 24))).append('\n');
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.createDirectories(file.toAbsolutePath().getParent());
        Files.writeString(tmp, sb.toString(), StandardCharsets.UTF_8);
        Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }

    private static String strip(String line) {
        int i = line.lastIndexOf(" #");
        if (i < 0) return null;
        String body = line.substring(0, i);
        return ChamberCore.sha256(body).substring(0, 16).equals(line.substring(i + 2)) ? body : null;
    }

    private static Map<String, String> kv(String body) {
        Map<String, String> m = new TreeMap<>();
        for (String tok : body.split(" ")) { int e = tok.indexOf('='); if (e > 0) m.put(tok.substring(0, e), tok.substring(e + 1)); }
        return m;
    }

    /** Loads and verifies. Header mismatch (wrong seed/algorithm/params/snapshot) renames the whole file aside (quarantine) and returns empty. */
    public static Loaded load(Path file, Header expected) throws IOException {
        Loaded L = new Loaded();
        if (!Files.exists(file)) return L;
        L.existed = true;
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        String hb = lines.isEmpty() ? null : strip(lines.get(0));
        if (hb == null || !hb.equals(expected.line())) {
            Path q = file.resolveSibling(file.getFileName() + ".quarantine-" + System.nanoTime());
            Files.move(file, q);
            L.quarantinedFile = q;
            return L;
        }
        L.headerOk = true;
        L.header = expected;
        StringBuilder chain = new StringBuilder();
        int counted = 0, declared = -1;
        String declChain = null;
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            String body = strip(line);
            if (body == null) { L.quarantinedLines.add(line); continue; }
            if (body.startsWith("END ")) { Map<String, String> m = kv(body); declared = Integer.parseInt(m.get("n")); declChain = m.get("chain"); continue; }
            try {
                Map<String, String> m = kv(body);
                if (body.startsWith("C ")) {
                    String[] ow = m.get("owner").split(","), c = m.get("c").split(",");
                    Rec r = new Rec(Long.parseUnsignedLong(m.get("id"), 16), Long.parseUnsignedLong(m.get("anchor"), 16), Integer.parseInt(ow[0]), Integer.parseInt(ow[1]),
                        Integer.parseInt(m.get("size")), Long.parseUnsignedLong(m.get("set"), 16), m.get("suit").equals("1"), Integer.parseInt(c[0]), Integer.parseInt(c[1]),
                        m.get("st"), m.get("roll").equals("1"), m.get("final").equals("1"), m.get("proof"));
                    L.recs.put(r.id, r);
                } else if (body.startsWith("B ")) {
                    L.budgets.put(m.get("group"), new Budget(m.get("group"), Long.parseLong(m.get("used")), Long.parseLong(m.get("cap")), m.get("exhausted").equals("1")));
                } else { L.quarantinedLines.add(line); continue; }
                chain.append(line);
                counted++;
            } catch (RuntimeException e) {
                L.quarantinedLines.add(line);
            }
        }
        L.trailerOk = declared == counted && declChain != null && declChain.equals(ChamberCore.sha256(chain.toString()).substring(0, 24));
        if (L.damaged()) {
            Path q = file.resolveSibling(file.getFileName() + ".damaged-" + System.nanoTime());
            Files.copy(file, q);   // preserve evidence; never delete
            L.quarantinedFile = q;
        }
        return L;
    }

    public static final class Reconciled {
        public final Map<Long, Rec> recs = new TreeMap<>(Long::compareUnsigned);
        public final List<String> integrityViolations = new ArrayList<>();
        public final List<String> held = new ArrayList<>();
        public int restoredVerified, newlyFinalized, newlyPending;
    }

    /**
     * Merges freshly derived decisions with stored ones. Stored FINAL decisions are never changed: a differing fresh decision is reported as
     * an integrity violation and the stored record wins. If the stored file was damaged, a fresh decision with no valid stored counterpart is
     * HELD (not finalized, no roll consumed) because the lost record cannot be compared.
     */
    public static Reconciled reconcile(Loaded stored, Map<Long, Rec> fresh) {
        Reconciled R = new Reconciled();
        boolean hold = stored.damaged() || (stored.existed && !stored.headerOk);
        for (Map.Entry<Long, Rec> e : fresh.entrySet()) {
            Rec f = e.getValue(), s = stored.recs.get(e.getKey());
            if (s != null && s.finalized) {
                if (s.decision().equals(f.decision())) R.restoredVerified++;
                else R.integrityViolations.add("FINALIZED_DECISION_MISMATCH id=" + Long.toHexString(f.id) + " stored=" + s.decision() + " fresh=" + f.decision());
                R.recs.put(s.id, s);
            } else if (s == null && hold && f.finalized) {
                R.held.add(Long.toHexString(f.id));
                R.recs.put(f.id, new Rec(f.id, f.anchor, f.ownerRx, f.ownerRz, f.size, f.setHash, f.suitable, f.cx, f.cz, "QUARANTINE_HOLD", f.rollWin, false, f.proof));
            } else {
                R.recs.put(f.id, f);
                if (f.finalized) R.newlyFinalized++; else R.newlyPending++;
            }
        }
        for (Rec s : stored.recs.values()) if (s.finalized && !fresh.containsKey(s.id)) R.recs.put(s.id, s);   // finalized records are never dropped
        return R;
    }

    /** Charges {@code ops} to a group's cumulative budget; exhausted groups stay exhausted regardless of later cap changes. */
    public static Budget charge(Map<String, Budget> budgets, String group, long ops, long cap) {
        Budget b = budgets.get(group);
        long used = (b == null ? 0 : b.used) + ops;
        boolean ex = (b != null && b.exhausted) || used >= cap;
        Budget nb = new Budget(group, used, b == null ? cap : b.cap, ex);
        budgets.put(group, nb);
        return nb;
    }
}
