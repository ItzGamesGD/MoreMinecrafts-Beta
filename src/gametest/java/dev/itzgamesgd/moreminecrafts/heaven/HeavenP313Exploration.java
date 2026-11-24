package dev.itzgamesgd.moreminecrafts.heaven;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.itzgamesgd.moreminecrafts.worldgenprobe.HeavenWorldgenProbe;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * P3.13 opt-in ({@code -Dmoreminecrafts.heaven.p313explore=true}): ordinary-player exploration of a natural island across a world save / reopen, singleplayer, production pools.
 * Session A: the player hovers at the WEST edge of the island (only the chunks inside the view distance exist); the world is saved and closed. Session B: reopened, the player
 * moves to the EAST edge, then over the centre. After every phase the server-side state is sampled WITHOUT forcing chunks to load ({@code getChunkNow}): loaded chunks of the
 * island, blocks that need a block entity, and how many of them are already in the chunk maps. At the end every chunk of the island is loaded and every block entity is accessed
 * and hashed; the hashes are compared with the reference dump of a single-stage generation ({@code -Dmm.heaven.p313.reference}, a HeavenWorldgenProbe bedump=full result).
 */
public final class HeavenP313Exploration implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        if (!Boolean.getBoolean("moreminecrafts.heaven.p313explore")) return;
        Map<String, Object> doc = new LinkedHashMap<>(); Map<String, Object> rows = new LinkedHashMap<>(); doc.put("sites", rows);
        try {
            JsonObject sites = JsonParser.parseString(Files.readString(Path.of(System.getProperty("mm.heaven.sitefile")))).getAsJsonObject();
            JsonObject ref = JsonParser.parseString(Files.readString(Path.of(System.getProperty("mm.heaven.p313.reference")))).getAsJsonObject().getAsJsonObject("islands");
            String[] want = System.getProperty("mm.heaven.p313.sites", "forest#6,mountain_meadow#9").split(",");
            TestWorldSave save;
            int[][] boxes = new int[want.length][];
            try (TestSingleplayerContext world = ctx.worldBuilder().setUseConsistentSettings(false).adjustSettings(st -> st.setSeed("42")).create()) {
                save = world.getWorldSave(); TestServerContext server = world.getServer();
                ctx.runOnClient(c -> c.options.renderDistance().set(6));
                server.runOnServer(s -> { for (String c : new String[]{"gamerule doDaylightCycle false", "weather clear", "time set midnight", "gamerule doMobSpawning false"}) cmd(s, c);
                    s.getPlayerList().getPlayers().getFirst().setGameMode(GameType.SPECTATOR); });
                for (int i = 0; i < want.length; i++) {
                    String[] xz = sites.get(want[i]).getAsString().split(","); int sx = Integer.parseInt(xz[0]), sz = Integer.parseInt(xz[1]); String base = want[i].split("#")[0];
                    move(ctx, server, sx + 0.5, 230, sz + 0.5); ctx.waitTicks(60);
                    boxes[i] = server.computeOnServer(s -> startBox(s, base, sx, sz)); final int[] bx = boxes[i];
                    Map<String, Object> row = new LinkedHashMap<>(); rows.put(want[i], row);
                    if (boxes[i] == null) { row.put("error", "no start"); continue; }
                    row.put("box", (boxes[i][3] - boxes[i][0] + 1) + "x" + (boxes[i][4] - boxes[i][1] + 1) + "x" + (boxes[i][5] - boxes[i][2] + 1));
                    double cz = (boxes[i][2] + boxes[i][5]) / 2.0; int ey = boxes[i][4] + 12;
                    move(ctx, server, boxes[i][0] - 30.5, ey, cz); ctx.waitTicks(40);
                    row.put("A_west_t40", server.computeOnServer(s -> sample(s, bx)));
                    ctx.waitTicks(300);
                    row.put("A_west_t340", server.computeOnServer(s -> sample(s, bx)));
                }
                server.runOnServer(s -> s.saveEverything(false, true, true));
            }
            try (TestSingleplayerContext world = save.open()) {
                TestServerContext server = world.getServer();
                server.runOnServer(s -> { for (String c : new String[]{"gamerule doDaylightCycle false", "weather clear", "gamerule doMobSpawning false"}) cmd(s, c); s.getPlayerList().getPlayers().getFirst().setGameMode(GameType.SPECTATOR); });
                for (int i = 0; i < want.length; i++) {
                    @SuppressWarnings("unchecked") Map<String, Object> row = (Map<String, Object>) rows.get(want[i]); if (boxes[i] == null) continue; final int[] bx = boxes[i];
                    double cz = (boxes[i][2] + boxes[i][5]) / 2.0, cx = (boxes[i][0] + boxes[i][3]) / 2.0; int ey = boxes[i][4] + 12;
                    row.put("B_reopened_before_moving", server.computeOnServer(s -> sample(s, bx)));
                    move(ctx, server, boxes[i][3] + 30.5, ey, cz); ctx.waitTicks(40);
                    row.put("B_east_t40", server.computeOnServer(s -> sample(s, bx)));
                    ctx.waitTicks(300);
                    row.put("B_east_t340", server.computeOnServer(s -> sample(s, bx)));
                    move(ctx, server, cx, ey + 30, cz); ctx.waitTicks(200);
                    row.put("B_centre_t200", server.computeOnServer(s -> sample(s, bx)));
                    // final: force every chunk, access and hash every block entity, compare with the single-stage reference
                    List<String> dump = server.computeOnServer(s -> dump(s, bx));
                    List<String> exp = new ArrayList<>(); var refRow = ref.get(want[i]).getAsJsonObject().getAsJsonArray("bedump"); for (var e : refRow) exp.add(e.getAsString());
                    row.put("final_blocks_with_BE", dump.size()); row.put("reference_blocks_with_BE", exp.size());
                    row.put("final_all_block_entities_exist", dump.stream().noneMatch(x -> x.contains("access=NO")));
                    row.put("final_equals_reference_position_block_nbt", strip(dump).equals(strip(exp)));
                    // same positions + block ids? and which block kinds differ in NBT (trial spawners / vaults carry game-time fields; chests, barrels, pots do not)
                    java.util.Set<String> pa = new java.util.TreeSet<>(), pb = new java.util.TreeSet<>(); for (String r : dump) { String[] f = r.split(" "); pa.add(f[0] + " " + f[1]); } for (String r : exp) { String[] f = r.split(" "); pb.add(f[0] + " " + f[1]); }
                    row.put("final_same_positions_and_block_ids", pa.equals(pb));
                    Map<String, String> nb = new java.util.HashMap<>(); for (String r : exp) { String[] f = r.split(" "); nb.put(f[0], f[4]); }
                    Map<String, Integer> diffKinds = new java.util.TreeMap<>(), sameKinds = new java.util.TreeMap<>();
                    for (String r : dump) { String[] f = r.split(" "); (f[4].equals(nb.get(f[0])) ? sameKinds : diffKinds).merge(f[1], 1, Integer::sum); }
                    row.put("nbt_equal_by_kind", sameKinds); row.put("nbt_differs_by_kind", diffKinds);
                    System.out.println("HEAVEN_P313_EXPLORE " + want[i] + " " + row);
                }
            }
        } catch (Throwable t) { doc.put("error", t.toString()); t.printStackTrace(); }
        try {
            Path out = Path.of(System.getProperty("mm.heaven.p313.out")); Files.createDirectories(out.toAbsolutePath().getParent());
            Files.writeString(out, new GsonBuilder().setPrettyPrinting().create().toJson(doc));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static List<String> strip(List<String> rows) { List<String> o = new ArrayList<>(); for (String r : rows) { String[] f = r.split(" "); o.add(f[0] + " " + f[1] + " " + f[4]); } java.util.Collections.sort(o); return o; }

    private static void move(ClientGameTestContext ctx, TestServerContext server, double x, double y, double z) {
        server.runOnServer(s -> s.getPlayerList().getPlayers().getFirst().teleportTo(s.overworld(), x, y, z, Set.of(), 90f, 30f, true));
    }

    private static void cmd(MinecraftServer s, String c) { s.getCommands().performPrefixedCommand(s.createCommandSourceStack(), c); }

    private static int[] startBox(MinecraftServer s, String base, int sx, int sz) {
        var level = s.overworld(); var registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE); Identifier sid = Identifier.parse("moreminecrafts:sky_island_" + base);
        int cx = sx >> 4, cz = sz >> 4; level.getChunk(cx, cz, ChunkStatus.FULL, true);
        for (var st : level.structureManager().startsForStructure(cx, cz, x -> registry.getKey(x) != null && registry.getKey(x).equals(sid))) { BoundingBox b = st.getBoundingBox(); return new int[]{b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ()}; }
        return null;
    }

    /** Non-forcing sample: only chunks that are currently loaded. */
    private static Map<String, Object> sample(MinecraftServer s, int[] b) {
        var level = s.overworld(); int loadedChunks = 0, totalChunks = 0, needBE = 0, inMap = 0;
        for (int cx = b[0] >> 4; cx <= b[3] >> 4; cx++) for (int cz = b[2] >> 4; cz <= b[5] >> 4; cz++) {
            totalChunks++; var ch = level.getChunkSource().getChunkNow(cx, cz); if (ch == null) continue; loadedChunks++;
            for (int x = Math.max(b[0], cx << 4); x <= Math.min(b[3], (cx << 4) + 15); x++) for (int z = Math.max(b[2], cz << 4); z <= Math.min(b[5], (cz << 4) + 15); z++)
                for (int y = b[1]; y <= b[4]; y++) { BlockPos p = new BlockPos(x, y, z); if (ch.getBlockState(p).hasBlockEntity()) { needBE++; if (ch.getBlockEntities().get(p) != null) inMap++; } }
        }
        Map<String, Object> m = new LinkedHashMap<>(); m.put("island_chunks", totalChunks); m.put("loaded_island_chunks", loadedChunks); m.put("blocks_needing_BE_in_loaded_chunks", needBE); m.put("of_which_BE_object_in_chunk_map", inMap);
        return m;
    }

    private static List<String> dump(MinecraftServer s, int[] b) {
        var level = s.overworld(); List<ChunkPos> chunks = new ArrayList<>();
        for (int cx = b[0] >> 4; cx <= b[3] >> 4; cx++) for (int cz = b[2] >> 4; cz <= b[5] >> 4; cz++) { level.getChunk(cx, cz, ChunkStatus.FULL, true); chunks.add(new ChunkPos(cx, cz)); }
        return HeavenWorldgenProbe.dumpBlockEntities(level, new BoundingBox(b[0], b[1], b[2], b[3], b[4], b[5]), chunks, true);
    }
}
