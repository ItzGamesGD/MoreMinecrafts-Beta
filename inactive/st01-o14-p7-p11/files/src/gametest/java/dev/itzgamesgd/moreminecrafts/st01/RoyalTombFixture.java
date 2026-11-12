package dev.itzgamesgd.moreminecrafts.st01;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * ST-01 P10 test fixture: the P6 integrated voxel model (P1-P4 cavities + P3 royal hall elements, candidate C overlay, CLOSED state) exported by
 * tools/st01-p10/export_fixture.py from the UNMODIFIED P1-P6 engines and placed block-for-block into a real server level.
 * Voxel semantics: "solid" in the model is a full solid block here (chests, statues, braziers and the 12-block cover are full blocks, exactly as the
 * model's collision proxy treats them). No cell is added, removed or moved to make a test pass.
 * Model -> world transform is a pure translation by {@code origin}; the WEST fixture is the exporter's own X-mirrored model (not mirrored here).
 */
public final class RoyalTombFixture {
    public final String side;
    public final EntranceLayout.Orientation orientation;
    public final int x0, x1, y0, y1, z0, z1, nx, ny, nz;
    public final boolean[] solid;      // index x + nx*(y + ny*z), model-relative to (x0,y0,z0)
    public final byte[] kind;
    public final List<String> kinds;
    public final List<int[]> coverModel = new ArrayList<>();
    public final int hallCx, hallCz, hallFloor, hallHalf;
    public final java.util.Map<String, int[]> goals = new java.util.LinkedHashMap<>();
    public final String closedRegionDigest;
    public final int nSolid;

    private RoyalTombFixture(String side, JsonObject o) {
        this.side = side;
        this.orientation = side.equals("E") ? EntranceLayout.Orientation.EAST : EntranceLayout.Orientation.WEST;
        JsonArray r = o.getAsJsonArray("region");
        x0 = r.get(0).getAsInt(); x1 = r.get(1).getAsInt(); y0 = r.get(2).getAsInt(); y1 = r.get(3).getAsInt(); z0 = r.get(4).getAsInt(); z1 = r.get(5).getAsInt();
        nx = x1 - x0 + 1; ny = y1 - y0 + 1; nz = z1 - z0 + 1;
        solid = new boolean[nx * ny * nz];
        kind = new byte[nx * ny * nz];
        decode(o.getAsJsonArray("solid_rle_xyz"), solid, null);
        decode(o.getAsJsonArray("kind_rle_xyz"), null, kind);
        kinds = new ArrayList<>();
        for (JsonElement e : o.getAsJsonArray("kinds")) kinds.add(e.getAsString());
        for (JsonElement e : o.getAsJsonArray("cover_cells")) { JsonArray c = e.getAsJsonArray(); coverModel.add(new int[]{c.get(0).getAsInt(), c.get(1).getAsInt(), c.get(2).getAsInt()}); }
        for (var en : o.getAsJsonObject("goals").entrySet()) {
            JsonArray a = en.getValue().getAsJsonArray();
            if (a.get(0).isJsonArray()) a = a.get(a.size() / 2).getAsJsonArray();      // CHEST_FRONT is a list of five cells: use the middle one
            goals.put(en.getKey(), new int[]{a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt()});
        }
        JsonObject h = o.getAsJsonObject("hall");
        hallCx = h.get("cx").getAsInt(); hallCz = h.get("cz").getAsInt(); hallFloor = h.get("floor").getAsInt(); hallHalf = h.get("half").getAsInt();
        closedRegionDigest = o.get("closed_region_digest").getAsString();
        nSolid = o.get("n_solid").getAsInt();
    }

    private static void decode(JsonArray rle, boolean[] bools, byte[] bytes) {
        int pos = 0;
        for (JsonElement e : rle) {
            JsonArray pair = e.getAsJsonArray();
            int v = pair.get(0).getAsInt(), n = pair.get(1).getAsInt();
            for (int i = 0; i < n; i++, pos++) { if (bools != null) bools[pos] = v != 0; if (bytes != null) bytes[pos] = (byte) v; }
        }
    }

    public static RoyalTombFixture load(String side) {
        try (InputStream in = RoyalTombFixture.class.getResourceAsStream("/st01/p10_fixture_" + side + ".json")) {
            if (in == null) throw new IllegalStateException("fixture resource missing for " + side);
            return new RoyalTombFixture(side, JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject());
        } catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
    }

    public int idx(int mx, int my, int mz) { return (mx - x0) + nx * ((my - y0) + ny * (mz - z0)); }
    public boolean inRegion(int mx, int my, int mz) { return mx >= x0 && mx <= x1 && my >= y0 && my <= y1 && mz >= z0 && mz <= z1; }
    public boolean modelSolid(int mx, int my, int mz) { return inRegion(mx, my, mz) && solid[idx(mx, my, mz)]; }
    public String modelKind(int mx, int my, int mz) { int k = inRegion(mx, my, mz) ? kind[idx(mx, my, mz)] : 0; return k == 0 ? "rock" : kinds.get(k - 1); }

    /** The royal chest block = structure anchor (P3 CHEST_PRIMARY). */
    public int[] anchorModel() { return new int[]{hallCx, hallFloor + 2, hallCz + 12}; }

    private static BlockState palette(String kind, boolean cover) {
        if (cover) return Blocks.STONE_BRICKS.defaultBlockState();
        return switch (kind) {
            case "pillar_base", "pillar_shaft", "pillar_cap" -> Blocks.SANDSTONE.defaultBlockState();
            case "lintel" -> Blocks.CUT_SANDSTONE.defaultBlockState();
            case "obelisk" -> Blocks.CHISELED_SANDSTONE.defaultBlockState();
            case "dais" -> Blocks.SMOOTH_SANDSTONE.defaultBlockState();
            case "sarcophagus" -> Blocks.POLISHED_ANDESITE.defaultBlockState();
            case "chest_primary", "chest_secondary" -> Blocks.GOLD_BLOCK.defaultBlockState();
            case "statue", "alcove_col" -> Blocks.STONE_BRICKS.defaultBlockState();
            case "brazier" -> Blocks.NETHER_BRICKS.defaultBlockState();
            default -> Blocks.STONE.defaultBlockState();
        };
    }

    /** A fixture placed in a level at a world translation. */
    public final class Placed {
        public final ServerLevel level;
        public final BlockPos origin;           // world position of model (0,0,0)
        public final BlockPos anchor;
        Placed(ServerLevel level, BlockPos origin) {
            this.level = level; this.origin = origin;
            int[] a = anchorModel();
            this.anchor = origin.offset(a[0], a[1], a[2]);   // hallCx is already the side's own (mirrored) centre
        }
        public RoyalTombFixture fixture() { return RoyalTombFixture.this; }
        public BlockPos world(int mx, int my, int mz) { return origin.offset(mx, my, mz); }
        public BlockPos model(BlockPos w) { return w.subtract(origin); }
        public List<ChunkPos> chunks() {
            List<ChunkPos> out = new ArrayList<>();
            for (int cx = (origin.getX() + x0) >> 4; cx <= (origin.getX() + x1) >> 4; cx++)
                for (int cz = (origin.getZ() + z0) >> 4; cz <= (origin.getZ() + z1) >> 4; cz++) out.add(new ChunkPos(cx, cz));
            return out;
        }

        /** Loads/forces the chunks and writes every cell of the model region (air cells explicitly cleared). Returns elapsed ms. */
        public long build() {
            long t0 = System.nanoTime();
            for (ChunkPos c : chunks()) { level.getChunk(c.x(), c.z()); level.setChunkForced(c.x(), c.z(), true); }
            java.util.Set<Long> cover = coverKeys();
            BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
            BlockState air = Blocks.AIR.defaultBlockState();
            for (int z = z0; z <= z1; z++) for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) {
                int i = idx(x, y, z);
                m.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                BlockState want = solid[i] ? palette(kind[i] == 0 ? "rock" : kinds.get(kind[i] - 1), cover.contains(packModel(x, y, z))) : air;
                if (level.getBlockState(m) != want) level.setBlock(m, want, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
            return (System.nanoTime() - t0) / 1_000_000L;
        }

        private java.util.Set<Long> coverKeys() { java.util.Set<Long> s = new java.util.HashSet<>(); for (int[] c : coverModel) s.add(packModel(c[0], c[1], c[2])); return s; }
        private long packModel(int x, int y, int z) { return BlockPos.asLong(x, y, z); }

        public void release() { for (ChunkPos c : chunks()) level.setChunkForced(c.x(), c.z(), false); }

        /** Restores the CLOSED cover blocks (test-only helper for reusing a placed fixture between trials). */
        public void restoreCover() {
            for (int[] c : coverModel) level.setBlock(world(c[0], c[1], c[2]), palette("rock", true), Block.UPDATE_CLIENTS);
        }

        /** Cell-by-cell comparison between the placed world and the model: returns {mismatched cells, solid cells in world, solid cells in model}. */
        public int[] voxelDiff() {
            int bad = 0, wSolid = 0, mSolid = 0;
            BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
            for (int z = z0; z <= z1; z++) for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) {
                m.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                boolean ws = !level.getBlockState(m).isAir();
                boolean ms = solid[idx(x, y, z)];
                if (ws) wSolid++;
                if (ms) mSolid++;
                if (ws != ms) bad++;
            }
            return new int[]{bad, wSolid, mSolid};
        }
    }

    public Placed place(ServerLevel level, BlockPos origin) { return new Placed(level, origin); }
}
