package dev.itzgamesgd.moreminecrafts.pyramid;

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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import dev.itzgamesgd.moreminecrafts.pyramid.St01PyramidAssembly.AccessRoute;
import dev.itzgamesgd.moreminecrafts.pyramid.St01PyramidAssembly.Orientation;

/**
 * P8-C2-A rendered-client capture (validation only; gated by {@code -Dmm.st01.assembly.client=true}). A real 26.3
 * client connects to a dedicated flat-world server. The server places two complete assemblies from the exported
 * templates: SOUTH/WEST and EAST/EAST. The ground around each is filled with sand, as a test-only stand-in for desert
 * terrain. Cameras are authored-frame poses mapped through {@link St01PyramidAssembly#worldPos}. Spectator mode keeps the
 * commanded pose. Night vision keeps the interiors legible, so the images show geometry, not the torch light level.
 */
public final class St01PyramidAssemblyClientCaptureTest implements FabricClientGameTest {
    private static final int GROUND_Y = -28; // underground bottom (Y-32) rests on the flat world's grass layer at -61
    private static final BlockPos SOUTH_WEST = new BlockPos(0, GROUND_Y, 0);
    private static final BlockPos EAST_EAST = new BlockPos(240, GROUND_Y, 0);

    /** Camera in authored coordinates (feet) plus look-at point, for the pyramid placed at {@code ground}. */
    private record View(String name, BlockPos ground, Orientation o, double x, double y, double z, double tx, double ty, double tz) {}

    private static View sw(String n, double x, double y, double z, double tx, double ty, double tz) {
        return new View(n, SOUTH_WEST, Orientation.SOUTH, x, y, z, tx, ty, tz);
    }

    private static final List<View> VIEWS = List.of(
            sw("01-exterior-south-east-oblique", 62, 26, 88, 0, 12, 0),
            sw("02-exterior-entrance-front", 0, 2, 66, 0, 8, 30),
            sw("03-exterior-aerial-north-west", -62, 70, -62, 0, 10, 0),
            new View("04-exterior-rotated-east-facing-pyramid", EAST_EAST, Orientation.EAST, 50, 30, 85, 0, 12, 0),
            sw("05-entrance-hall", 0, 0, 39, 0, 4, 22),
            sw("06-gallery-and-support-piers", 0, 0, 17, 0, 4, -4),
            sw("07-altar-pedestal", 0, 13, -13, 0, 12, -18),
            sw("08-royal-hall-d1-toward-primary", -21, -22, 8, -21, -20, 33),
            sw("09-antechamber-statues", -13, -22, -7, -27, -20, -4),
            sw("10-offering-annex", -33, -22, -7, -37, -21, -7),
            sw("11-l1-tomb-t1", 12, -6, -19, 12, -6, -24),
            sw("12-l2-store-b2", 15, -14, 16, 14, -13, 21),
            sw("13-secret-descent-and-landing", 0, -30, 35, -8, -25, 34),
            sw("14-west-access-route", -14, -30, 30, -14, -29, 21),
            sw("15-treasury-vault-sculpture", 7, -30, 26, 25, -26, 26),
            sw("16-treasury-piles", 18, -30, 18, 18, -29, 32),
            sw("17-secret-crypt", 9, -30, -1, 16, -29, -1),
            new View("18-east-access-route-rotated", EAST_EAST, Orientation.EAST, 0, -30, 31, 0, -29, 24));

    @Override
    public void runTest(ClientGameTestContext context) {
        if (!Boolean.getBoolean("mm.st01.assembly.client")) return;
        String out = System.getProperty("mm.st01.assembly.client.out", "build/st01-p8c2a/client/manifest.json");
        Map<String, Object> manifest = new LinkedHashMap<>();
        List<Map<String, Object>> captures = new ArrayList<>();
        manifest.put("minecraft", "26.3");
        manifest.put("placements", List.of("SOUTH/WEST at " + SOUTH_WEST.toShortString(), "EAST/EAST at " + EAST_EAST.toShortString()));
        manifest.put("night_vision", true);
        manifest.put("captures", captures);
        Properties properties = new Properties();
        properties.setProperty("online-mode", "false");
        properties.setProperty("level-type", "minecraft:flat");
        properties.setProperty("generate-structures", "false");
        properties.setProperty("spawn-protection", "0");
        properties.setProperty("view-distance", "10");
        properties.setProperty("simulation-distance", "6");
        try (TestDedicatedServerContext server = context.worldBuilder().createServer(properties);
             TestDedicatedServerConnection connection = server.connect()) {
            context.waitFor(c -> c.player != null && c.level != null);
            server.runOnServer(s -> {
                ServerLevel level = s.overworld();
                place(level, SOUTH_WEST, Orientation.SOUTH, AccessRoute.WEST);
                place(level, EAST_EAST, Orientation.EAST, AccessRoute.EAST);
                ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
                player.setGameMode(GameType.SPECTATOR);
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 3600, 0, false, false, false));
            });
            context.runOnClient(c -> { c.gui.hud.toggle(); c.options.renderDistance().set(12); });
            context.waitTicks(40);
            connection.waitForClientboundPackets();
            for (View v : VIEWS) {
                BlockPos feet = St01PyramidAssembly.worldPos(BlockPos.containing(v.x, v.y, v.z), v.ground, v.o);
                BlockPos look = St01PyramidAssembly.worldPos(BlockPos.containing(v.tx, v.ty, v.tz), v.ground, v.o);
                double x = feet.getX() + 0.5, y = feet.getY(), z = feet.getZ() + 0.5;
                double tx = look.getX() + 0.5, ty = look.getY() + 0.5, tz = look.getZ() + 0.5;
                float yaw = (float) Math.toDegrees(Math.atan2(-(tx - x), tz - z));
                float pitch = (float) -Math.toDegrees(Math.atan2(ty - (y + 1.62), Math.hypot(tx - x, tz - z)));
                Map<String, Object> cap = new LinkedHashMap<>();
                cap.put("name", v.name);
                cap.put("orientation", v.o.name());
                cap.put("authored_feet", List.of(v.x, v.y, v.z));
                cap.put("world_feet", List.of(x, y, z));
                cap.put("yaw", yaw);
                cap.put("pitch", pitch);
                server.runOnServer(s -> s.getPlayerList().getPlayers().getFirst().teleportTo(s.overworld(), x, y, z, Set.of(), yaw, pitch, true));
                boolean reached = true;
                try {
                    context.waitFor(c -> c.player.position().distanceTo(new Vec3(x, y, z)) < 0.6, 400);
                } catch (RuntimeException e) {
                    reached = false;
                }
                context.waitTicks(100);
                connection.waitForClientboundPackets();
                context.waitTicks(10);
                cap.put("position_reached", reached);
                cap.put("client_eye_block", context.computeOnClient(c -> String.valueOf(
                        BuiltInRegistries.BLOCK.getKey(c.level.getBlockState(BlockPos.containing(c.player.getEyePosition())).getBlock()))));
                cap.put("client_look_target_block", context.computeOnClient(c -> String.valueOf(
                        BuiltInRegistries.BLOCK.getKey(c.level.getBlockState(look).getBlock()))));
                cap.put("screenshot", String.valueOf(context.takeScreenshot("st01-p8c2a-" + v.name)));
                captures.add(cap);
            }
            manifest.put("client", context.computeOnClient(c -> Map.of("window", c.getWindow().getWidth() + "x" + c.getWindow().getHeight(),
                    "fov", c.options.fov().get(), "render_distance", c.options.renderDistance().get())));
        }
        try {
            Path p = Path.of(out);
            Files.createDirectories(p.getParent());
            Files.writeString(p, new GsonBuilder().setPrettyPrinting().create().toJson(manifest), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        System.out.println("ST01_P8C2A_CLIENT_CAPTURE_DONE captures=" + captures.size());
    }

    private static void place(ServerLevel level, BlockPos ground, Orientation o, AccessRoute route) {
        int r = 72;
        for (int cx = (ground.getX() - r) >> 4; cx <= (ground.getX() + r) >> 4; cx++)
            for (int cz = (ground.getZ() - r) >> 4; cz <= (ground.getZ() + r) >> 4; cz++) level.getChunk(cx, cz);
        // Test-only desert ground: sand up to the ground plane around the footprint (the asset itself has no terrain).
        var sand = Blocks.SAND.defaultBlockState();
        for (BlockPos p : BlockPos.betweenClosed(ground.getX() - r, ground.getY() - 32, ground.getZ() - r, ground.getX() + r, ground.getY() - 1, ground.getZ() + r))
            level.setBlock(p, sand, Block.UPDATE_CLIENTS);
        if (!St01PyramidAssembly.place(level, level.getStructureTemplateManager(), ground, o, route, level.getRandom(), Block.UPDATE_CLIENTS))
            throw new AssertionError("assembly placement failed " + o + "/" + route);
    }
}
