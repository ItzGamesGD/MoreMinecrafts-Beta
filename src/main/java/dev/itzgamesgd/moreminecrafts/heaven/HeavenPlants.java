package dev.itzgamesgd.moreminecrafts.heaven;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Heaven P3 plant-instance rules for the multi-cell vegetation (giant flowers, medium flowers).
 *
 * <p>A plant instance is identified from ANY of its cells against the P2 species layouts
 * ({@code data/moreminecrafts/heaven/plant_layouts.json}, written by {@code tools/heaven-p3/build_layouts.py}):
 * a layout cell belongs to the instance only when the world holds exactly the expected block, part and facing at the
 * expected offset. A touching neighbour flower therefore never matches (its cells carry other parts at those offsets),
 * so removal never spreads to another plant or to terrain. At most {@value #MAX_CELLS} positions are inspected.
 *
 * <ul>
 *   <li>F6 (approved): removing a giant-flower STEM removes every matched cell of that one flower in the same tick.
 *       Removing a leaf or bloom part removes only that block.</li>
 *   <li>F8 (approved, multi-cell interpretation): a plant whose ground row (layout y &lt;= 0) no longer stands on
 *       {@code #moreminecrafts:supports_heaven_vegetation} is removed as a whole. A medium flower is one plant: removing
 *       any of its cells removes the rest (like a vanilla double plant).</li>
 *   <li>F7 (drop amounts NOT approved): cascaded cells are removed with {@code setBlock}, which never drops; only the
 *       block the player broke goes through the normal drop path. {@link #onPlantRemoved} is the single per-instance
 *       hook where an approved giant-flower drop would go; it is intentionally empty.</li>
 * </ul>
 * Unloaded positions are skipped (never force-loaded).
 */
public final class HeavenPlants {
    public static final TagKey<Block> SUPPORTS = TagKey.create(Registries.BLOCK, ModIds.id("supports_heaven_vegetation"));
    static final int MAX_CELLS = 128;
    private static final Direction[] ORDER = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    public record Cell(int dx, int dy, int dz, Identifier block, int part) {}
    public record Layout(String id, String group, List<Cell> cells) {}
    private record Candidate(Layout layout, Cell cell) {}
    /** One identified plant: its root, rotation and the matched world positions (includes the origin). */
    public record Instance(Layout layout, BlockPos root, int rotation, List<BlockPos> cells, List<Cell> matched) {
        public String id() { return layout.id(); }
        public boolean giant() { return layout.group().equals("giant"); }
    }
    /** Observable result of the last whole-plant removal (tests, profiling). Server thread only. */
    public record Removal(String layout, int removed, int skippedUnloaded, int dropHookCalls, long nanos) {}

    private static volatile Map<String, List<Candidate>> index;
    private static volatile Map<String, Layout> layouts;
    private static boolean removing;
    private static Removal last;
    private static int dropHookCalls;

    private HeavenPlants() {}

    public static Removal lastRemoval() { return last; }
    public static int dropHookCalls() { return dropHookCalls; }

    private static Map<String, List<Candidate>> index() {
        Map<String, List<Candidate>> i = index;
        if (i != null) return i;
        synchronized (HeavenPlants.class) {
            if (index != null) return index;
            Map<String, List<Candidate>> built = new HashMap<>();
            Map<String, Layout> byId = new HashMap<>();
            for (Layout l : read()) {
                byId.put(l.id(), l);
                for (Cell c : l.cells()) built.computeIfAbsent(key(c.block(), c.part()), k -> new ArrayList<>()).add(new Candidate(l, c));
            }
            layouts = Map.copyOf(byId);
            index = built;
            return built;
        }
    }

    /** The P2 layout of one plant species / variant (e.g. {@code giant_starlotus}, {@code medium_lavender_0}). */
    public static Layout layout(String id) {
        index();
        Layout l = layouts.get(id);
        if (l == null) throw new IllegalArgumentException("unknown plant layout " + id);
        return l;
    }

    public static java.util.Collection<Layout> layouts() { index(); return layouts.values(); }

    /** World offset of a layout cell for a plant rotated {@code r} quarter turns clockwise. */
    public static BlockPos rotated(Cell c, int r) { return offset(c, r); }

    private static List<Layout> read() {
        String path = "/data/moreminecrafts/heaven/plant_layouts.json";
        try (InputStream in = HeavenPlants.class.getResourceAsStream(path)) {
            if (in == null) throw new IllegalStateException("missing " + path);
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            List<Layout> out = new ArrayList<>();
            for (JsonElement e : root.getAsJsonArray("layouts")) {
                JsonObject o = e.getAsJsonObject();
                List<Cell> cells = new ArrayList<>();
                for (JsonElement ce : o.getAsJsonArray("cells")) {
                    JsonArray a = ce.getAsJsonArray();
                    cells.add(new Cell(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt(),
                            Identifier.parse(a.get(3).getAsString()), a.get(4).getAsInt()));
                }
                if (cells.size() > MAX_CELLS) throw new IllegalStateException("layout too large: " + o.get("id"));
                out.add(new Layout(o.get("id").getAsString(), o.get("group").getAsString(), List.copyOf(cells)));
            }
            return out;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String key(Identifier block, int part) { return block + "#" + part; }

    private static int partOf(BlockState s) {
        return s.getBlock() instanceof HeavenPartBlock b && b.partProperty() != null ? s.getValue(b.partProperty()) : 0;
    }

    private static int rotationOf(BlockState s) {
        if (!s.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) return 0;
        Direction d = s.getValue(BlockStateProperties.HORIZONTAL_FACING);
        for (int r = 0; r < 4; r++) if (ORDER[r] == d) return r;
        return 0;
    }

    /** Same convention as tools/heaven-p2/build_p2.py rot_cell_offset and StructureTemplate CLOCKWISE_90: (x,z) -> (-z,x). */
    private static BlockPos offset(Cell c, int r) {
        int u = c.dx(), w = c.dz();
        for (int k = 0; k < r; k++) { int t = u; u = -w; w = t; }
        return new BlockPos(u, c.dy(), w);
    }

    private static boolean matches(BlockGetter level, BlockPos p, Cell c, int rotation, boolean facing) {
        BlockState s = level.getBlockState(p);
        if (!(s.getBlock() instanceof HeavenPartBlock b) || !b.id().equals(c.block()) || partOf(s) != c.part()) return false;
        return !facing || rotationOf(s) == rotation;
    }

    /**
     * Identify the plant that {@code state} at {@code pos} is (or was) a cell of. When the block has already been removed
     * ({@code present=false}) the origin counts as matched. Returns null when no layout matches at least two cells.
     */
    public static Instance find(BlockGetter level, BlockPos pos, BlockState state, boolean present, java.util.function.Predicate<BlockPos> loaded) {
        if (!(state.getBlock() instanceof HeavenPartBlock block)) return null;
        List<Candidate> cands = index().get(key(block.id(), partOf(state)));
        if (cands == null) return null;
        int rotation = rotationOf(state);
        boolean facing = block.hasFacing();
        Instance best = null;
        for (Candidate cand : cands) {
            BlockPos root = pos.subtract(offset(cand.cell(), rotation));
            List<BlockPos> cells = new ArrayList<>();
            List<Cell> matched = new ArrayList<>();
            for (Cell c : cand.layout().cells()) {
                BlockPos p = root.offset(offset(c, rotation));
                boolean hit = p.equals(pos) ? (c == cand.cell() || present && matches(level, p, c, rotation, facing))
                        : loaded.test(p) && matches(level, p, c, rotation, facing);
                if (hit) { cells.add(p); matched.add(c); }
            }
            if (cells.size() >= 2 && (best == null || cells.size() > best.cells().size()))
                best = new Instance(cand.layout(), root, rotation, List.copyOf(cells), List.copyOf(matched));
        }
        return best;
    }

    /** F8: the plant's ground row (layout y &lt;= 0) must stand on a supporting block. */
    public static boolean supported(BlockGetter level, Instance inst) {
        for (int i = 0; i < inst.cells().size(); i++) {
            if (inst.matched().get(i).dy() > 0) continue;
            BlockPos below = inst.cells().get(i).below();
            if (level.getBlockState(below).is(SUPPORTS)) return true;
        }
        return false;
    }

    /** Called after a giant/medium cell was removed from the world (any cause). */
    static void onCellRemoved(ServerLevel level, BlockPos pos, BlockState removed) {
        if (removing) return;                                    // our own cascade: never re-enter
        if (!(removed.getBlock() instanceof HeavenPartBlock block)) return;
        boolean giant = block.rule() == HeavenPartBlock.Rule.GIANT;
        if (giant && !block.isGiantStem()) return;               // F6: leaves / blooms are removed individually
        Instance inst = find(level, pos, removed, false, level::isLoaded);
        if (inst != null) removeInstance(level, inst, pos);
    }

    /** Scheduled after the block under a multi-cell plant changed. */
    static void checkSupport(ServerLevel level, BlockPos pos, BlockState state) {
        if (removing) return;
        Instance inst = find(level, pos, state, true, level::isLoaded);
        if (inst != null && !supported(level, inst)) removeInstance(level, inst, null);
    }

    /** Removes every matched cell of one plant (except {@code alreadyGone}) without drops. */
    public static Removal removeInstance(ServerLevel level, Instance inst, BlockPos alreadyGone) {
        long t0 = System.nanoTime();
        int removed = 0, skipped = 0;
        removing = true;
        try {
            for (BlockPos p : inst.cells()) {
                if (p.equals(alreadyGone)) { removed++; continue; }
                if (!level.isLoaded(p)) { skipped++; continue; }
                level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                removed++;
            }
            onPlantRemoved(level, inst);
        } finally {
            removing = false;
        }
        last = new Removal(inst.id(), removed, skipped, dropHookCalls, System.nanoTime() - t0);
        return last;
    }

    /** Single per-instance drop hook. F7 (giant-flower drop item, amount, chance) is NOT approved: no drops. */
    private static void onPlantRemoved(ServerLevel level, Instance inst) {
        dropHookCalls++;
    }
}
