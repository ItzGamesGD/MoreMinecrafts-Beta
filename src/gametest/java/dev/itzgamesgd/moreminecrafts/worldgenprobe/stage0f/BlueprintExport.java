package dev.itzgamesgd.moreminecrafts.worldgenprobe.stage0f;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.itzgamesgd.moreminecrafts.worldgenprobe.stage0f.Terrain.Kind;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

/**
 * Stage 0-G blueprint source-data exporter. Uses the SAME {@link ArenaDesign#build} and the SAME persisted construction plans as the live
 * Stage 0-F run, so blueprints are projections of the real definition rather than separate drawings.
 * Usage: BlueprintExport out-dir [persisted-state-dir id ...]   (reference variants are always exported)
 */
public final class BlueprintExport {
    private BlueprintExport() { }

    /** Finds an id (deterministic scan) of a given variant, for reference geometry. */
    static long idFor(ArenaDesign.Variant v) {
        for (long i = 1; ; i++) { long id = ArenaDesign.mix(0xB10E0000L, i); if (ArenaDesign.variantOf(id) == v) return id; }
    }

    static void write(Path dir, String name, String source, long id, int cx, int cz, int floorY, Plan plan, ArenaDesign.Layout layout) throws Exception {
        Gson g = new GsonBuilder().create();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name); m.put("source", source); m.put("id", String.format("%016x", id));
        m.put("variant", ArenaDesign.variantOf(id).name());
        m.put("center", List.of(cx, floorY, cz)); m.put("floorY", floorY);
        Map<String, Object> k = new LinkedHashMap<>();
        k.put("EXT_R", ArenaDesign.EXT_R); k.put("IN_R", ArenaDesign.IN_R); k.put("WALL_H", ArenaDesign.WALL_H); k.put("DOME_H", ArenaDesign.DOME_H);
        k.put("TOTAL_H", ArenaDesign.TOTAL_H); k.put("VAULT_R", ArenaDesign.VAULT_R); k.put("ENT_W", ArenaDesign.ENT_W); k.put("ENT_H", ArenaDesign.ENT_H);
        k.put("DIR_N_E_S_W", ArenaDesign.DIR);
        m.put("constants", k);
        m.put("vaults_xz", layout.vaults); m.put("columns_xz", layout.columns);
        m.put("palette", plan.palette);
        m.put("plan_hash", plan.hash());
        List<int[]> cells = new ArrayList<>();
        for (long c : plan.cells()) {
            String s = plan.finalSpecAt(Plan.ux(c), Plan.uy(c), Plan.uz(c));
            int pi = plan.palette.indexOf(s);
            cells.add(new int[]{Plan.ux(c), Plan.uy(c), Plan.uz(c), pi});
        }
        m.put("cells_xyz_palette", cells);
        m.put("open_cells", plan.open.size());
        try (Writer w = new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(dir.resolve(name + ".json.gz"))), StandardCharsets.UTF_8)) { g.toJson(m, w); }
        System.out.println("exported " + name + " cells=" + cells.size() + " variant=" + m.get("variant"));
    }

    public static void main(String[] a) throws Exception {
        if (a[0].equals("findid")) {      // findid <hex-prefix-14-digits> <VARIANT>: first id with that prefix whose variant matches (fixture-id helper)
            long base = Long.parseUnsignedLong(a[1], 16) << 8;
            for (long i = 0; i < 256; i++) if (ArenaDesign.variantOf(base | i).name().equals(a[2])) { System.out.println(String.format("%016x", base | i)); return; }
            System.out.println("none"); return;
        }
        Path out = Path.of(a[0]); Files.createDirectories(out);
        // reference geometry: uniform solid rock, centre 0,0, floor Y -24 (hazard checks irrelevant) - same ArenaDesign.build as production
        Terrain.Grid ref = new Terrain.Grid(-80, -60, -80, 161, 120, 161, Kind.ROCK);
        for (ArenaDesign.Variant v : ArenaDesign.Variant.values()) {
            long id = idFor(v);
            Plan p = new Plan();
            ArenaDesign.Layout L = ArenaDesign.build(p, ref, id, 0, 0, -24);
            p.finish();
            write(out, "reference-" + v.name().toLowerCase(), "ArenaDesign.build on uniform rock (reference geometry)", id, 0, 0, -24, p, L);
        }
        for (int i = 1; i + 1 < a.length; i += 2) {
            Construction.Journal j = new Construction.Journal(Path.of(a[i]));
            String sid = a[i + 1];
            Plan p = j.readPlan(sid);
            long id = Long.parseUnsignedLong(sid, 16);
            String[] meta = Files.readString(Path.of(a[i], "f-" + sid + ".meta")).split("\n");
            // centre/floor from the live reservation list are passed through meta-less: recover from the plan's vault ring is overkill; take from args file
            int cx = Integer.parseInt(System.getProperty("cx." + sid)), cz = Integer.parseInt(System.getProperty("cz." + sid)), fy = Integer.parseInt(System.getProperty("fy." + sid));
            Plan scratch = new Plan();
            ArenaDesign.Layout L = ArenaDesign.build(scratch, ref2(cx, cz, fy), id, cx, cz, fy);
            write(out, "built-" + sid.substring(0, 8), "persisted live Stage 0-F plan (state dir " + a[i] + ")", id, cx, cz, fy, p, L);
        }
    }

    static Terrain.Grid ref2(int cx, int cz, int fy) { return new Terrain.Grid(cx - 80, fy - 60, cz - 80, 161, 120, 161, Kind.ROCK); }
}
