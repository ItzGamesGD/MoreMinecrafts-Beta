package dev.itzgamesgd.moreminecrafts.worldgenprobe.st01p8a;

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
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * ST-01 P8-A rendered-client capture (validation only; gated by {@code -Dmm.st01.p8a.client=true}).
 *
 * <p>A real rendered client connects to a dedicated 26.3 server whose overworld receives the committed royal_hall.nbt at
 * the approved origin. Static cameras are placed on the server and every frame is taken by the client with
 * {@code takeScreenshot}. A second phase drives a real player with held movement/jump keys from the north doorway to the
 * PRIMARY chest, right-clicks the chest, and checks the opened container. Each capture is listed with its camera pose,
 * FOV, GUI scale and window size in the manifest written to {@code mm.st01.p8a.client.out}.
 */
public final class St01P8AClientCaptureTest implements FabricClientGameTest {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Identifier TEMPLATE_ID = Identifier.fromNamespaceAndPath("moreminecrafts", "pyramid/royal_hall");
    private static final BlockPos ORIGIN = new BlockPos(-40, -24, 2);
    private static final BlockPos CHEST_LEFT = new BlockPos(-21, -20, 33);

    /** Static camera: name, feet position, yaw, pitch (Minecraft convention: yaw 0 = +Z, positive pitch looks down). */
    private record View(String name, double x, double y, double z, float yaw, float pitch) {}

    private static final List<View> VIEWS = List.of(
            new View("01-full-hall-exterior-oblique", -16.0, -6.0, -22.0, 6.6f, 10.3f),
            new View("02-roof-overview-from-north", -21.0, 4.0, -20.0, 0f, 23.7f),
            new View("03-entrance-exterior-approach", -21.0, -20.0, -10.0, 0f, 0f),
            new View("04-entrance-doorway-threshold", -21.0, -22.0, 3.0, 0f, 2f),
            new View("05-hall-interior-toward-dais", -21.0, -22.0, 12.0, 0f, 6f),
            new View("06-primary-chest-and-dais-north", -21.0, -18.38, 27.5, -4.8f, 19.5f),
            new View("07-primary-chest-and-dais-northwest", -27.0, -16.38, 27.0, -45f, 18.8f),
            new View("08-guardian-arena-from-north", -21.0, -18.38, 14.0, 0f, 16.8f),
            new View("09-guardian-arena-from-west", -34.0, -18.38, 22.0, -72.9f, 14.9f),
            new View("10-sarcophagus-dais-from-west", -34.0, -18.38, 33.0, -79.6f, 4.7f),
            new View("11-frontal-stairs-approach", -21.0, -18.38, 22.0, 0f, 18.1f),
            new View("12-side-depth-east-interior", -8.0, -16.38, 20.0, 85.6f, -10.4f),
            new View("13-side-depth-west-exterior", -44.0, -16.38, 20.0, -87.5f, 0f),
            new View("14-rear-view-from-south", -21.0, -8.38, 48.0, 180f, 15.7f));

    @Override
    public void runTest(ClientGameTestContext context) {
        if (!Boolean.getBoolean("mm.st01.p8a.client")) return;
        String out = System.getProperty("mm.st01.p8a.client.out", "build/st01-p8a-client/manifest.json");
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("minecraft", "26.3");
        manifest.put("origin", String.valueOf(ORIGIN));
        List<Map<String, Object>> captures = new ArrayList<>();
        manifest.put("captures", captures);
        Map<String, Object> walk = new LinkedHashMap<>();
        manifest.put("walk", walk);

        Properties properties = new Properties();
        properties.setProperty("online-mode", "false");
        properties.setProperty("level-type", "minecraft:flat");
        properties.setProperty("generate-structures", "false");
        properties.setProperty("spawn-protection", "0");
        properties.setProperty("view-distance", "8");
        properties.setProperty("simulation-distance", "6");
        try (TestDedicatedServerContext server = context.worldBuilder().createServer(properties);
             TestDedicatedServerConnection connection = server.connect()) {
            context.waitFor(c -> c.player != null && c.level != null);
            server.runOnServer(s -> {
                ServerLevel level = s.overworld();
                ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
                StructureTemplate template = level.getStructureTemplateManager().get(TEMPLATE_ID).orElseThrow();
                if (!template.placeInWorld(level, ORIGIN, ORIGIN, new StructurePlaceSettings(), level.getRandom(), Block.UPDATE_CLIENTS)) {
                    throw new AssertionError("template placement returned false");
                }
                player.setGameMode(GameType.CREATIVE);
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
            });
            context.waitTicks(20);
            connection.waitForClientboundPackets();

            for (View v : VIEWS) {
                server.runOnServer(s -> {
                    ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
                    player.teleportTo(s.overworld(), v.x, v.y, v.z, Set.of(), v.yaw, v.pitch, true);
                });
                context.waitTicks(80);
                connection.waitForClientboundPackets();
                context.waitTicks(10);
                Map<String, Object> cap = pose(context, v.name, v.x, v.y, v.z, v.yaw, v.pitch);
                captures.add(cap);
                cap.put("screenshot", String.valueOf(context.takeScreenshot(v.name)));
            }

            // ---- Phase 2: real player movement from the doorway to the PRIMARY chest (survival physics, real key input)
            server.runOnServer(s -> {
                ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
                player.getAbilities().flying = false;
                player.setGameMode(GameType.SURVIVAL);
                player.onUpdateAbilities();
                player.teleportTo(s.overworld(), -20.5, -22.0, 3.5, Set.of(), 0f, 0f, true);
            });
            context.waitTicks(40);
            connection.waitForClientboundPackets();

            List<Map<String, Object>> trace = new ArrayList<>();
            walk.put("trace", trace);
            context.getInput().holdKey(options -> options.keyUp);
            context.getInput().holdKey(options -> options.keyJump);
            boolean arrived = false;
            int ticks;
            for (ticks = 0; ticks < 1800 && !arrived; ticks++) {
                context.waitTick();
                if (ticks % 30 == 0) {
                    double[] p = context.computeOnClient(c -> new double[]{c.player.getX(), c.player.getY(), c.player.getZ()});
                    trace.add(Map.of("tick", ticks, "x", round(p[0]), "y", round(p[1]), "z", round(p[2])));
                }
                arrived = context.computeOnClient(c -> c.player.getY() > -20.2 && c.player.getZ() > 31.8);
            }
            context.getInput().releaseKey(options -> options.keyUp);
            context.getInput().releaseKey(options -> options.keyJump);
            context.waitTicks(20);
            walk.put("ticks", ticks);
            walk.put("reached_dais_top", arrived);
            if (!arrived) {
                throw new AssertionError("real player did not climb to the dais top within " + ticks + " ticks");
            }
            double[] end = context.computeOnClient(c -> new double[]{c.player.getX(), c.player.getY(), c.player.getZ()});
            walk.put("end", Map.of("x", round(end[0]), "y", round(end[1]), "z", round(end[2])));
            double dist = Math.sqrt(Math.pow(end[0] - (CHEST_LEFT.getX() + 0.5), 2) + Math.pow(end[1] - (CHEST_LEFT.getY() + 0.5), 2)
                    + Math.pow(end[2] - (CHEST_LEFT.getZ() + 0.5), 2));
            walk.put("distance_to_chest_centre", round(dist));

            // ---- Phase 3: real right-click on the PRIMARY chest; verify the opened container
            context.getInput().lookAt(CHEST_LEFT);
            context.waitTicks(5);
            context.getInput().pressMouse(1);
            context.waitFor(c -> c.gui.screen() instanceof AbstractContainerScreen<?>, 200);
            context.waitTicks(10);
            connection.waitForClientboundPackets();
            Map<String, Object> chestResult = context.computeOnClient(c -> {
                Map<String, Object> m = new LinkedHashMap<>();
                var menu = c.player.containerMenu;
                m.put("screen", c.gui.screen() == null ? "null" : c.gui.screen().getClass().getSimpleName());
                m.put("menu", menu.getClass().getSimpleName());
                m.put("total_slots", menu.slots.size());
                if (menu instanceof ChestMenu chest) m.put("rows", chest.getRowCount());
                m.put("container_slots", menu.slots.size() - 36);
                return m;
            });
            manifest.put("chest_open", chestResult);
            context.takeScreenshot("08-primary-chest-open-54-slot");

            // summary of the client environment used for the captures
            Map<String, Object> env = context.computeOnClient(c -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("window_width", c.getWindow().getWidth());
                m.put("window_height", c.getWindow().getHeight());
                m.put("gui_scale", c.getWindow().getGuiScale());
                m.put("fov", c.options.fov().get());
                m.put("render_distance", c.options.renderDistance().get());
                return m;
            });
            manifest.put("client", env);
        }

        try {
            Path p = Path.of(out);
            if (p.getParent() != null) Files.createDirectories(p.getParent());
            Files.writeString(p, GSON.toJson(manifest), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AssertionError("could not write capture manifest", e);
        }
        System.out.println("ST01_P8A_CLIENT_CAPTURE_DONE captures=" + captures.size());
    }

    private static Map<String, Object> pose(ClientGameTestContext context, String name, double x, double y, double z, float yaw, float pitch) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("feet", List.of(x, y, z));
        m.put("yaw", yaw);
        m.put("pitch", pitch);
        m.put("client_pose", context.computeOnClient(c -> List.of(round(c.player.getX()), round(c.player.getY()), round(c.player.getZ()),
                round(c.player.getYRot()), round(c.player.getXRot()))));
        m.put("fov", context.computeOnClient(c -> c.options.fov().get()));
        m.put("gui_scale", context.computeOnClient(c -> c.getWindow().getGuiScale()));
        return m;
    }

    private static double round(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }
}
