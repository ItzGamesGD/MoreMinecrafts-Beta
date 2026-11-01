package dev.itzgamesgd.moreminecrafts.worldgenprobe.stage0f;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * ST-05 Stage 0-G real-client capture. Opens copies of the worlds built by the Stage 0-F live run in a real Minecraft 26.3 client
 * (dedicated server + client, software GL) and takes native screenshots. Scene list: -Dmm.st05g.scenes=<json>. Never runs unless that property is set.
 * Cut-away scenes remove blocks ONLY in the disposable photo copy of the world (frozen ticks) and are labelled as such in the scene metadata.
 */
@SuppressWarnings("UnstableApiUsage")
public final class St05ArenaVisualClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        String sp = System.getProperty("mm.st05g.scenes");
        if (sp == null) return;
        try {
            JsonObject root = new Gson().fromJson(Files.readString(Path.of(sp)), JsonObject.class);
            for (var we : root.getAsJsonArray("worlds")) runWorld(context, we.getAsJsonObject());
        } catch (java.io.IOException e) { throw new RuntimeException(e); }
    }

    private void runWorld(ClientGameTestContext context, JsonObject w) {
        installWorld(w.get("orig_world").getAsString());   // the harness always serves "world": replace it with the saved Stage 0-F world copy
        Properties props = new Properties();
        props.setProperty("view-distance", "10");
        props.setProperty("spawn-monsters", "false"); props.setProperty("spawn-animals", "false");
        try (TestDedicatedServerContext server = context.worldBuilder().createServer(props);
             TestDedicatedServerConnection connection = server.connect()) {
            context.waitFor(c -> c.player != null && c.level != null && c.getConnection() != null && c.getConnection().getConnection().isConnected());
            context.runOnClient(c -> { if (!c.gui.hud.isHidden()) c.gui.hud.toggle(); c.options.fov().set(85); });
            server.runOnServer(s -> {
                s.tickRateManager().setFrozen(true);
                ServerPlayer p = s.getPlayerList().getPlayers().getFirst();
                p.setGameMode(GameType.SPECTATOR);
                p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1_000_000, 0, false, false));
            });
            boolean cutDone = false;
            for (var se : w.getAsJsonArray("scenes")) {
                JsonObject sc = se.getAsJsonObject();
                String cut = sc.has("cutaway") ? sc.get("cutaway").getAsString() : "";
                if (!cut.isEmpty()) applyCutaway(server, sc);
                double x = sc.get("x").getAsDouble(), y = sc.get("y").getAsDouble(), z = sc.get("z").getAsDouble();
                float yaw = sc.get("yaw").getAsFloat(), pitch = sc.get("pitch").getAsFloat();
                server.runOnServer(s -> {
                    ServerPlayer p = s.getPlayerList().getPlayers().getFirst();
                    ServerLevel lv = p.level();
                    p.teleportTo(lv, x, y, z, Set.of(), yaw, pitch, true);
                });
                connection.waitForClientboundPackets();
                context.waitTicks(sc.has("wait") ? sc.get("wait").getAsInt() : 120);
                context.takeScreenshot(sc.get("name").getAsString());
            }
        }
    }

    private static void installWorld(String level) {
        try {
            Path dst = Path.of("world"), src = Path.of(System.getProperty("mm.st05g.worlds")).resolve(level);
            if (Files.exists(dst)) try (var st = Files.walk(dst)) { st.sorted(java.util.Comparator.reverseOrder()).forEach(q -> q.toFile().delete()); }
            try (var st = Files.walk(src)) {
                for (Path q : (Iterable<Path>) st::iterator) {
                    Path t = dst.resolve(src.relativize(q).toString());
                    if (Files.isDirectory(q)) Files.createDirectories(t); else Files.copy(q, t);
                }
            }
            Files.deleteIfExists(dst.resolve("session.lock"));
        } catch (java.io.IOException e) { throw new RuntimeException(e); }
    }

    /** Photo-copy only: remove blocks to expose the dome from above ("top") or the vertical section ("half": everything east of the centre). */
    private void applyCutaway(TestDedicatedServerContext server, JsonObject sc) {
        String kind = sc.get("cutaway").getAsString();
        int cx = sc.get("cx").getAsInt(), cz = sc.get("cz").getAsInt(), fy = sc.get("fy").getAsInt();
        server.runOnServer(s -> {
            ServerLevel lv = s.overworld();
            BlockState air = Blocks.AIR.defaultBlockState();
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            int R = 62, n = 0;
            for (int dx = -R; dx <= R; dx++) for (int dz = -R; dz <= R; dz++) {
                if (dx * dx + dz * dz > R * R) continue;
                if (kind.equals("half") && dx <= 0) continue;
                int y0 = kind.equals("top") ? fy + 24 : fy - 3, y1 = fy + 70;
                for (int y = y0; y <= y1; y++) {
                    p.set(cx + dx, y, cz + dz);
                    if (!lv.getBlockState(p).isAir()) { lv.setBlock(p, air, 2 | 16 | 32 | 128); n++; }
                }
            }
            System.out.println("ST05G cutaway " + kind + " removed " + n + " blocks (photo copy only)");
        });
    }
}
