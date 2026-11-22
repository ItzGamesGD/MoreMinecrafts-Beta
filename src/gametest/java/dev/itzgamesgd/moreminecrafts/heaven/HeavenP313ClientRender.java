package dev.itzgamesgd.moreminecrafts.heaven;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import java.util.List;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * P3.13 opt-in real-client rendering (loading-aware: waits for the client renderer to finish all sections, records it, and takes an EARLY frame for the loading comparison) of NATURALLY generated sky islands ({@code -Dmoreminecrafts.heaven.p313render=true}): a real client joins a dedicated server
 * (seed 42, the production jar, no extra data pack) and looks at each listed natural site ({@code -Dmm.heaven.sitefile}, {@code -Dmm.heaven.p313.sites}) at noon and midnight.
 * Screenshots: heaven-p313-NN-<site>-<day|night>. Software renderer: appearance only, no performance measurement.
 */
public final class HeavenP313ClientRender implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        if (!Boolean.getBoolean("moreminecrafts.heaven.p313render")) return;
        int[] shot = {0};
        try {
            JsonObject sites = JsonParser.parseString(Files.readString(Path.of(System.getProperty("mm.heaven.sitefile")))).getAsJsonObject();
            String[] want = System.getProperty("mm.heaven.p313.sites", "meadow,meadow#2,meadow#3,mountain_meadow,mountain_meadow#4,forest,forest#1,forest#3").split(",");
            Properties props = new Properties();
            props.setProperty("online-mode", "false"); props.setProperty("view-distance", "10"); props.setProperty("simulation-distance", "6"); props.setProperty("max-tick-time", "-1");
            props.setProperty("spawn-animals", "false"); props.setProperty("spawn-monsters", "false");
            try (TestDedicatedServerContext server = ctx.worldBuilder().setUseConsistentSettings(false).adjustSettings(st -> st.setSeed("42")).createServer(props);
                 var connection = server.connect()) {
                ctx.runOnClient(c -> { if (!c.gui.hud.isHidden()) c.gui.hud.toggle(); c.options.renderDistance().set(12); });
                server.runOnServer(s -> { for (String c : new String[]{"gamerule doDaylightCycle false", "gamerule doWeatherCycle false", "weather clear", "gamerule randomTickSpeed 0"}) cmd(s, c);
                    s.getPlayerList().getPlayers().getFirst().setGameMode(GameType.SPECTATOR); });
                for (String target : want) {
                    if (!sites.has(target)) { System.out.println("HEAVEN_P313_RENDER " + target + " site missing"); continue; }
                    String[] xz = sites.get(target).getAsString().split(","); int sx = Integer.parseInt(xz[0]), sz = Integer.parseInt(xz[1]); String base = target.split("#")[0];
                    server.runOnServer(s -> cmd(s, "time set noon"));
                    server.runOnServer(s -> s.getPlayerList().getPlayers().getFirst().teleportTo(s.overworld(), sx + 40.5, 220, sz + 0.5, Set.of(), 0f, 0f, true));
                    ctx.waitTicks(100);
                    int[] b = server.computeOnServer(s -> box(s, base, sx, sz));
                    if (b == null) { System.out.println("HEAVEN_P313_RENDER " + target + " no start"); continue; }
                    BoundingBox bb = new BoundingBox(b[0], b[1], b[2], b[3], b[4], b[5]);
                    double cx = (bb.minX() + bb.maxX()) / 2.0, cy = (bb.minY() + bb.maxY()) / 2.0, cz = (bb.minZ() + bb.maxZ()) / 2.0;
                    boolean longX = bb.getXSpan() >= bb.getZSpan(); double span = Math.max(bb.getXSpan(), bb.getZSpan()), dist = span * 0.6 + 28, up = bb.getYSpan() * 0.5 + 12;
                    BlockPos eye = longX ? new BlockPos((int) cx, (int) (cy + up), (int) (cz - dist)) : new BlockPos((int) (cx - dist), (int) (cy + up), (int) cz);
                    BlockPos eye2 = longX ? new BlockPos((int) cx, (int) (cy + up), (int) (cz + dist)) : new BlockPos((int) (cx + dist), (int) (cy + up), (int) cz);
                    BlockPos look = new BlockPos((int) cx, (int) cy, (int) cz);
                    System.out.println("HEAVEN_P313_RENDER " + target + " boxMin=" + bb.minX() + "," + bb.minY() + "," + bb.minZ() + " boxMax=" + bb.maxX() + "," + bb.maxY() + "," + bb.maxZ() + " eye=" + eye + " look=" + look);
                    if (target.equals("mountain_meadow#4")) {            // loading comparison: frame right after arrival (renderer not finished) vs after the renderer reports all sections built
                        server.runOnServer(s -> cmd(s, "time set noon"));
                        lookNoWait(ctx, server, eye, look); ctx.waitTicks(25);
                        boolean doneEarly = ctx.computeOnClient(c -> c.levelRenderer.hasRenderedAllSections());
                        ctx.runOnClient(c -> c.gui.toastManager().clear());
                        System.out.println("HEAVEN_P313_RENDER " + target + " early renderedAll=" + doneEarly + " " + ctx.takeScreenshot("heaven-p313-%02d-%s-day-early".formatted(shot[0]++, target.replace('#', '_'))));
                    }
                    for (String when : new String[]{"day", "night"}) {
                        server.runOnServer(s -> cmd(s, when.equals("day") ? "time set noon" : "time set midnight"));
                        int waited = look(ctx, server, eye, look);
                        boolean done = ctx.computeOnClient(c -> c.levelRenderer.hasRenderedAllSections());
                        { final BoundingBox fbb = bb;   // client vs server block census inside the island box (every 3rd cell): is the data on the client at all?
                          int[] cl = ctx.computeOnClient(c -> census(c.level, fbb)); int[] sv = server.computeOnServer(s -> census(s.overworld(), fbb));
                          System.out.println("HEAVEN_P313_RENDER " + target + " census client[nonAir=" + cl[0] + " heavenBlocks=" + cl[1] + " chunksPresent=" + cl[2] + "] server[nonAir=" + sv[0] + " heavenBlocks=" + sv[1] + " chunksPresent=" + sv[2] + "]"); }
                        if (Boolean.getBoolean("mm.heaven.p313.census") && when.equals("day")) {      // is the island data ever delivered to the client? per-chunk detail + repeated census
                            for (int r = 0; r < 6; r++) {
                                ctx.waitTicks(100); int[] cl2 = ctx.computeOnClient(c -> census(c.level, fbb2(bb))); int[] sv2 = server.computeOnServer(s -> census(s.overworld(), fbb2(bb)));
                                System.out.println("HEAVEN_P313_RENDER " + target + " census+" + (r + 1) * 100 + "ticks client[nonAir=" + cl2[0] + "] server[nonAir=" + sv2[0] + "]");
                            }
                            List<String> perChunk = server.computeOnServer(s -> perChunk(s, fbb2(bb)));
                            List<String> perClient = ctx.computeOnClient(c -> perChunkClient(c.level, fbb2(bb)));
                            System.out.println("HEAVEN_P313_RENDER " + target + " perChunk(server: chunk nonAir trackedByPlayer status) " + perChunk);
                            System.out.println("HEAVEN_P313_RENDER " + target + " perChunk(client: chunk nonAir) " + perClient);
                        }
                        System.out.println("HEAVEN_P313_RENDER " + target + " clientPlayer=" + ctx.computeOnClient(c -> c.player == null ? "null" : c.player.position() + " yaw=" + c.player.getYRot() + " pitch=" + c.player.getXRot()));
                        ctx.runOnClient(c -> c.gui.toastManager().clear());
                        System.out.println("HEAVEN_P313_RENDER " + target + " " + when + " box=" + bb.getXSpan() + "x" + bb.getYSpan() + "x" + bb.getZSpan() + " renderedAll=" + done + " waitedTicks=" + waited + " "
                                + ctx.takeScreenshot("heaven-p313-%02d-%s-%s".formatted(shot[0]++, target.replace('#', '_'), when)));
                        if (when.equals("day")) {
                            look(ctx, server, eye2, look);
                            ctx.runOnClient(c -> c.gui.toastManager().clear());
                            System.out.println("HEAVEN_P313_RENDER " + target + " day-opposite " + ctx.takeScreenshot("heaven-p313-%02d-%s-day-opposite".formatted(shot[0]++, target.replace('#', '_'))));
                        }
                    }
                }
            }
        } catch (Throwable t) { System.out.println("HEAVEN_P313_RENDER error " + t); t.printStackTrace(); }
    }

    private static int[] box(MinecraftServer s, String base, int sx, int sz) {
        var level = s.overworld(); var registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE); Identifier sid = Identifier.parse(base.contains(":") ? base : "moreminecrafts:sky_island_" + base);
        int cx = sx >> 4, cz = sz >> 4; level.getChunk(cx, cz, ChunkStatus.FULL, true);
        for (var st : level.structureManager().startsForStructure(cx, cz, x -> registry.getKey(x) != null && registry.getKey(x).equals(sid))) {
            BoundingBox b = st.getBoundingBox();
            for (int x = b.minX() >> 4; x <= b.maxX() >> 4; x++) for (int z = b.minZ() >> 4; z <= b.maxZ() >> 4; z++) level.getChunk(x, z, ChunkStatus.FULL, true);
            return new int[]{b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ()};
        }
        return null;
    }

    private static BoundingBox fbb2(BoundingBox b) { return b; }

    private static List<String> perChunk(MinecraftServer s, BoundingBox b) {
        var level = s.overworld(); var player = s.getPlayerList().getPlayers().getFirst(); List<String> out = new java.util.ArrayList<>();
        for (int cx = b.minX() >> 4; cx <= b.maxX() >> 4; cx++) for (int cz = b.minZ() >> 4; cz <= b.maxZ() >> 4; cz++) {
            var ch = level.getChunkSource().getChunkNow(cx, cz); int n = 0;
            if (ch != null) for (int x = cx << 4; x < (cx << 4) + 16; x += 3) for (int z = cz << 4; z < (cz << 4) + 16; z += 3) for (int y = b.minY(); y <= b.maxY(); y += 3) if (!ch.getBlockState(new BlockPos(x, y, z)).isAir()) n++;
            out.add(cx + "," + cz + " " + (ch == null ? "unloaded" : n) + " tracked=" + player.getChunkTrackingView().contains(new ChunkPos(cx, cz)) + " " + (ch == null ? "-" : ch.getPersistedStatus()));
        }
        return out;
    }

    private static List<String> perChunkClient(net.minecraft.world.level.Level level, BoundingBox b) {
        List<String> out = new java.util.ArrayList<>();
        for (int cx = b.minX() >> 4; cx <= b.maxX() >> 4; cx++) for (int cz = b.minZ() >> 4; cz <= b.maxZ() >> 4; cz++) {
            int n = 0; boolean has = level.hasChunk(cx, cz);
            if (has) for (int x = cx << 4; x < (cx << 4) + 16; x += 3) for (int z = cz << 4; z < (cz << 4) + 16; z += 3) for (int y = b.minY(); y <= b.maxY(); y += 3) if (!level.getBlockState(new BlockPos(x, y, z)).isAir()) n++;
            out.add(cx + "," + cz + " " + (has ? n : "absent"));
        }
        return out;
    }

    private static int[] census(net.minecraft.world.level.Level level, BoundingBox b) {
        int nonAir = 0, heaven = 0, chunks = 0; java.util.Set<Long> seen = new java.util.HashSet<>();
        for (int x = b.minX(); x <= b.maxX(); x += 3) for (int z = b.minZ(); z <= b.maxZ(); z += 3) {
            if (level.hasChunkAt(new BlockPos(x, b.minY(), z)) && seen.add(((long) (x >> 4) << 32) ^ ((z >> 4) & 0xffffffffL))) chunks++;
            for (int y = b.minY(); y <= b.maxY(); y += 3) { var st = level.getBlockState(new BlockPos(x, y, z)); if (!st.isAir()) { nonAir++; if (net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(st.getBlock()).getPath().startsWith("heaven_")) heaven++; } }
        }
        return new int[]{nonAir, heaven, chunks};
    }

    private static void cmd(MinecraftServer s, String c) { s.getCommands().performPrefixedCommand(s.createCommandSourceStack(), c); }

    private static void lookNoWait(ClientGameTestContext ctx, TestDedicatedServerContext server, BlockPos eye, BlockPos target) {
        double px = eye.getX() + 0.5, py = eye.getY() + 0.5, pz = eye.getZ() + 0.5, dx = target.getX() + 0.5 - px, dy = target.getY() + 0.5 - py, dz = target.getZ() + 0.5 - pz;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz)), pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
        server.runOnServer(s -> s.getPlayerList().getPlayers().getFirst().teleportTo(s.overworld(), px, py, pz, Set.of(), yaw, pitch, true));
    }

    /** teleport + look, wait until the client has the chunk AND its renderer reports every section built (max 900 ticks); returns the ticks waited after arrival. */
    private static int look(ClientGameTestContext ctx, TestDedicatedServerContext server, BlockPos eye, BlockPos target) {
        lookNoWait(ctx, server, eye, target);
        ctx.waitFor(c -> c.level != null && c.level.hasChunkAt(target), 600);
        int waited = 0;
        ctx.waitTicks(40); waited += 40;
        while (waited < 900 && !ctx.computeOnClient(c -> c.levelRenderer.hasRenderedAllSections())) { ctx.waitTicks(20); waited += 20; }
        ctx.waitTicks(20);
        return waited + 20;
    }
}
