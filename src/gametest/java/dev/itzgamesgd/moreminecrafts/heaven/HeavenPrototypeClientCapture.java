package dev.itzgamesgd.moreminecrafts.heaven;

import java.util.Properties;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;

/**
 * Opt-in real-client captures of the heaven sky island P1 prototype ({@code -Dmoreminecrafts.heaven.capture=true}).
 * Places {@code moreminecrafts:heaven_prototype/plain_medium_d_v5} high above a normal world (the same template a
 * developer places with {@code /place template}) and records screenshots from the design-review angles plus the
 * client FPS. Nothing is asserted about appearance: the images are reviewed by a person.
 */
public final class HeavenPrototypeClientCapture implements FabricClientGameTest {
    private static final String GATE = "moreminecrafts.heaven.capture";
    private static final Identifier TEMPLATE = Identifier.parse("moreminecrafts:heaven_prototype/plain_medium_d_v5");
    private static final BlockPos ORIGIN = new BlockPos(0, 230, 0);
    /** Island centre in template coordinates (terrain x 0..44, y 0..13 surface, z 0..36). */
    private static final double CX = 22.5, CY = 11.5, CZ = 18.5;

    @Override
    public void runTest(ClientGameTestContext context) {
        if (!Boolean.getBoolean(GATE)) return;
        Properties properties = new Properties();
        properties.setProperty("online-mode", "false");
        properties.setProperty("difficulty", "peaceful");
        properties.setProperty("level-seed", "2632030");
        properties.setProperty("view-distance", "12");
        properties.setProperty("simulation-distance", "6");
        properties.setProperty("spawn-monsters", "false");
        properties.setProperty("max-tick-time", "-1");
        int[] shot = {0};
        try (TestDedicatedServerContext server = context.worldBuilder().setUseConsistentSettings(false).createServer(properties);
             TestDedicatedServerConnection connection = server.connect()) {
            context.waitFor(c -> c.player != null && c.level != null);
            context.runOnClient(c -> { if (!c.gui.hud.isHidden()) c.gui.hud.toggle(); });
            server.runOnServer(s -> {
                ServerLevel level = s.overworld();
                for (int cx = -1; cx <= 3; cx++) for (int cz = -1; cz <= 3; cz++) level.getChunk(cx, cz);
                var template = level.getStructureTemplateManager().get(TEMPLATE).orElseThrow(() -> new AssertionError("missing " + TEMPLATE));
                if (!template.placeInWorld(level, ORIGIN, ORIGIN, new StructurePlaceSettings(), level.getRandom(), Block.UPDATE_CLIENTS))
                    throw new AssertionError("placeInWorld failed");
                s.getCommands().performPrefixedCommand(s.createCommandSourceStack(), "weather clear");
                s.getCommands().performPrefixedCommand(s.createCommandSourceStack(), "time set noon");
                var p = s.getPlayerList().getPlayers().getFirst();
                p.setGameMode(GameType.SPECTATOR);
                System.out.println("HEAVEN_CAPTURE_PLACED " + TEMPLATE + " at " + ORIGIN);
            });
            // Design-review angles (yaw 35 deg from +x+z, as in the offline renders), then gameplay views.
            orbit(context, server, connection, CX, CY, CZ, 35, 14, 62, 300);
            fps(context, "full");
            snap(context, shot, "full-design-angle");
            orbit(context, server, connection, CX, CY, CZ, 215, 18, 62, 120);
            snap(context, shot, "full-opposite");
            orbit(context, server, connection, 29, 9.5, 31, 35, 10, 15, 120);
            snap(context, shot, "edge-grass-loam-rock");
            orbit(context, server, connection, 24, 5, 22, 35, -30, 34, 160);
            snap(context, shot, "underside");
            orbit(context, server, connection, 27, 19, 10, 40, 6, 17, 120);
            snap(context, shot, "giant-starlotus");
            orbit(context, server, connection, 16, 19, 11, 200, 6, 18, 120);
            snap(context, shot, "giant-lily");
            orbit(context, server, connection, 22, 19, 29, 30, 6, 18, 120);
            snap(context, shot, "giant-daffodil");
            look(context, server, connection, 31.5, 14 + 1.62, 30.5, 18, 17, 12, 120);
            fps(context, "walk");
            snap(context, shot, "walk-eye-level");
            orbit(context, server, connection, CX, CY, CZ, 35, 10, 120, 200);
            fps(context, "far");
            snap(context, shot, "far-readability");
            System.out.println("HEAVEN_CAPTURE_COMPLETE " + shot[0]);
        }
    }

    private static void orbit(ClientGameTestContext context, TestDedicatedServerContext server, TestDedicatedServerConnection connection,
                              double tx, double ty, double tz, double yawDeg, double pitchDeg, double dist, int wait) {
        double yaw = Math.toRadians(yawDeg), pitch = Math.toRadians(pitchDeg);
        double px = tx + dist * Math.cos(pitch) * Math.sin(yaw), py = ty + dist * Math.sin(pitch), pz = tz + dist * Math.cos(pitch) * Math.cos(yaw);
        look(context, server, connection, px, py, pz, tx, ty, tz, wait);
    }

    /** Positions are template coordinates; ORIGIN is added here. */
    private static void look(ClientGameTestContext context, TestDedicatedServerContext server, TestDedicatedServerConnection connection,
                             double px, double py, double pz, double tx, double ty, double tz, int wait) {
        double dx = tx - px, dz = tz - pz;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(ty - py, Math.hypot(dx, dz)));
        double ax = ORIGIN.getX() + px, ay = ORIGIN.getY() + py, az = ORIGIN.getZ() + pz;
        server.runOnServer(s -> {
            s.getCommands().performPrefixedCommand(s.createCommandSourceStack(), "time set noon");
            s.getPlayerList().getPlayers().getFirst().teleportTo(s.overworld(), ax, ay, az, Set.of(), yaw, pitch, true);
        });
        connection.waitForClientboundPackets();
        BlockPos target = BlockPos.containing(ORIGIN.getX() + tx, ORIGIN.getY() + ty, ORIGIN.getZ() + tz);
        context.waitFor(c -> c.level != null && c.level.hasChunkAt(target), 600);
        context.waitTicks(wait);
    }

    private static void fps(ClientGameTestContext context, String view) {
        context.runOnClient(c -> System.out.println("HEAVEN_CAPTURE_FPS view=" + view + " fps=" + c.getFps()));
    }

    private static void snap(ClientGameTestContext context, int[] shot, String name) {
        context.runOnClient(c -> c.gui.toastManager().clear());
        System.out.println("HEAVEN_CAPTURE_SCREENSHOT " + context.takeScreenshot("heaven-prototype-%02d-%s".formatted(shot[0]++, name)));
    }
}
