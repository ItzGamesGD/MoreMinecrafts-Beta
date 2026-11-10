package dev.itzgamesgd.moreminecrafts.worldgenprobe.st01p4;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Rotation;

/**
 * ST-01 Phase 4 rendered-client capture of NATURALLY generated starts (validation only; gated by {@code -Dmm.st01.p4.client=true}).
 * Targets come from {@code -Dmm.st01.p4.client.targets="cx,yf,cz,ROTATION;..."} (taken from a scan result). For each start the camera views
 * show the corrected plateau and its edges from outside, and the buried royal-hall fixture from inside (teleported; creative flight).
 * Screenshots are numbered per target; a manifest lists every pose. The royal hall is a temporary integration fixture, not the pyramid.
 */
public final class St01Phase4ClientCaptureTest implements FabricClientGameTest {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private record View(String name, double x, double y, double z, float yaw, float pitch) {}

    private static float yawToward(double dx, double dz) { return (float) Math.toDegrees(Math.atan2(-dx, dz)); }

    private static float pitchToward(double dy, double horizontal) { return (float) -Math.toDegrees(Math.atan2(dy, horizontal)); }

    private static View look(String name, double x, double y, double z, double tx, double ty, double tz) {
        double dx = tx - x, dz = tz - z;
        return new View(name, x, y, z, yawToward(dx, dz), pitchToward(ty - y, Math.hypot(dx, dz)));
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        if (!Boolean.getBoolean("mm.st01.p4.client")) return;
        String out = System.getProperty("mm.st01.p4.client.out", "build/st01-phase4/client/manifest.json");
        long seed = Long.getLong("mm.st01.p4.client.seed", 1001L);
        List<String> targets = List.of(System.getProperty("mm.st01.p4.client.targets", "").split(";"));
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("minecraft", "26.3");
        manifest.put("seed", seed);
        List<Map<String, Object>> captures = new ArrayList<>();
        manifest.put("captures", captures);

        Properties properties = new Properties();
        properties.setProperty("online-mode", "false");
        properties.setProperty("level-seed", String.valueOf(seed));
        properties.setProperty("generate-structures", "true");
        properties.setProperty("spawn-protection", "0");
        properties.setProperty("view-distance", "10");
        properties.setProperty("simulation-distance", "4");
        properties.setProperty("spawn-monsters", "false");
        try (TestDedicatedServerContext server = context.worldBuilder().createServer(properties);
             TestDedicatedServerConnection connection = server.connect()) {
            context.waitFor(c -> c.player != null && c.level != null);
            server.runOnServer(s -> {
                ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
                player.setGameMode(GameType.CREATIVE);
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
            });
            int index = 0;
            for (String t : targets) {
                if (t.isBlank()) continue;
                String[] p = t.split(",");
                int cx = Integer.parseInt(p[0].trim()), yf = Integer.parseInt(p[1].trim()), cz = Integer.parseInt(p[2].trim());
                Rotation rot = Rotation.valueOf(p[3].trim());
                index++;
                String prefix = String.format("t%d-", index);
                List<View> views = new ArrayList<>();
                views.add(look("aerial-oblique-from-southwest", cx - 75, yf + 48, cz + 75, cx, yf, cz));
                views.add(look("aerial-oblique-from-northeast", cx + 75, yf + 48, cz - 75, cx, yf, cz));
                views.add(new View("top-down", cx + 0.5, yf + 120, cz + 0.5, 0f, 90f));
                views.add(look("edge-west-ground", cx - 62, yf + 4, cz, cx - 40, yf, cz));
                views.add(look("edge-east-ground", cx + 62, yf + 4, cz, cx + 40, yf, cz));
                views.add(look("edge-north-ground", cx, yf + 4, cz - 62, cx, yf, cz - 40));
                views.add(look("edge-south-ground", cx, yf + 4, cz + 62, cx, yf, cz + 40));
                views.add(look("plateau-centre-level", cx - 30, yf + 2, cz - 30, cx + 20, yf, cz + 20));
                // buried fixture interior: reference frame (P1/P8-A) rotated about the anchor column
                BlockPos in1 = St01Phase4Probe.pyramidWorld(cx, yf, cz, new BlockPos(-21, -22, 12), rot);
                BlockPos dais = St01Phase4Probe.pyramidWorld(cx, yf, cz, new BlockPos(-21, -18, 33), rot);
                BlockPos in2 = St01Phase4Probe.pyramidWorld(cx, yf, cz, new BlockPos(-21, -18, 22), rot);
                views.add(look("hall-interior-toward-dais", in1.getX() + 0.5, in1.getY() + 0.1, in1.getZ() + 0.5, dais.getX() + 0.5, dais.getY() + 1, dais.getZ() + 0.5));
                views.add(look("hall-primary-chest", in2.getX() + 0.5, in2.getY() + 0.1, in2.getZ() + 0.5, dais.getX() + 0.5, dais.getY() + 1, dais.getZ() + 0.5));
                for (View v : views) {
                    server.runOnServer(s -> {
                        ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
                        player.teleportTo(s.overworld(), v.x, v.y, v.z, Set.of(), v.yaw, v.pitch, true);
                    });
                    context.waitTicks(100);
                    connection.waitForClientboundPackets();
                    context.waitTicks(20);
                    Map<String, Object> cap = new LinkedHashMap<>();
                    cap.put("target", t);
                    cap.put("name", prefix + v.name);
                    cap.put("feet", List.of(v.x, v.y, v.z));
                    cap.put("yaw", v.yaw);
                    cap.put("pitch", v.pitch);
                    cap.put("gui_scale", context.computeOnClient(c -> c.getWindow().getGuiScale()));
                    cap.put("render_distance", context.computeOnClient(c -> c.options.renderDistance().get()));
                    cap.put("screenshot", String.valueOf(context.takeScreenshot(prefix + v.name)));
                    captures.add(cap);
                }
            }
        }
        try {
            Path p = Path.of(out);
            if (p.getParent() != null) Files.createDirectories(p.getParent());
            Files.writeString(p, GSON.toJson(manifest), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AssertionError("cannot write manifest", e);
        }
    }
}
