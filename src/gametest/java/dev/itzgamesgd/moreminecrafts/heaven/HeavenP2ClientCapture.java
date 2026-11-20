package dev.itzgamesgd.moreminecrafts.heaven;

import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;

/**
 * Opt-in real-client P1 vs P2 comparison and underside lighting study ({@code -Dmoreminecrafts.heaven.p2capture=true}).
 * P1 and P2 islands are placed 140 blocks apart and photographed from identical relative cameras (noon, clear);
 * then P2 at midnight, P2 underside with ambient occlusion off, and two control plates (heaven cream stone, white
 * concrete) floating in open sky. Server-side sky-light levels under the island and under the plates are logged.
 * Nothing about appearance is asserted.
 */
public final class HeavenP2ClientCapture implements FabricClientGameTest {
    private static final String GATE = "moreminecrafts.heaven.p2capture";
    private static final BlockPos P1_AT = new BlockPos(0, 230, 0);
    private static final BlockPos P2_AT = new BlockPos(0, 230, 140);
    private static final BlockPos PLATES_AT = new BlockPos(100, 250, 70);
    private static final double CX = 22.5, CY = 11.5, CZ = 18.5;

    private record View(String name, double tx, double ty, double tz, double yaw, double pitch, double dist, int ticks) {}

    private static final View[] VIEWS = {
            new View("full-design-angle", CX, CY, CZ, 35, 14, 44, 240), new View("full-opposite", CX, CY, CZ, 215, 18, 44, 120),
            new View("edge-grass-loam-rock", 29, 9.5, 31, 35, 10, 15, 100), new View("underside", 24, 5, 22, 35, -30, 34, 120),
            new View("giant-starlotus", 27, 19, 10, 160, 6, 17, 100), new View("giant-lily", 16, 19, 11, 200, 6, 18, 100),
            new View("giant-daffodil", 22, 19, 29, 30, 6, 18, 100), new View("trees-blossom-white", 23, 19, 21, 120, 12, 22, 100),
            new View("mid-68", CX, CY, CZ, 35, 10, 68, 160)};

    @Override
    public void runTest(ClientGameTestContext context) {
        if (!Boolean.getBoolean(GATE)) return;
        Properties properties = new Properties();
        properties.setProperty("online-mode", "false"); properties.setProperty("difficulty", "peaceful");
        properties.setProperty("level-seed", "2632030"); properties.setProperty("view-distance", "12");
        properties.setProperty("simulation-distance", "6"); properties.setProperty("spawn-monsters", "false");
        properties.setProperty("max-tick-time", "-1");
        int[] shot = {0};
        try (TestDedicatedServerContext server = context.worldBuilder().setUseConsistentSettings(false).createServer(properties);
             TestDedicatedServerConnection connection = server.connect()) {
            context.waitFor(c -> c.player != null && c.level != null);
            context.runOnClient(c -> { if (!c.gui.hud.isHidden()) c.gui.hud.toggle(); });
            server.runOnServer(s -> {
                ServerLevel level = s.overworld();
                place(level, "moreminecrafts:heaven_prototype/plain_medium_d_v5", P1_AT);
                place(level, "moreminecrafts:heaven_prototype/plain_medium_d_v5_p2", P2_AT);
                Block concrete = BuiltInRegistries.BLOCK.getValue(Identifier.parse("minecraft:white_concrete"));
                if (concrete == Blocks.AIR) throw new AssertionError("minecraft:white_concrete not found");
                // control plates in open sky: 9x9 heaven cream stone and 9x9 white concrete, 4 blocks apart, nothing above
                for (int x = 0; x < 9; x++) for (int z = 0; z < 9; z++) {
                    level.setBlock(PLATES_AT.offset(x, 0, z), BuiltInRegistries.BLOCK.getValue(Identifier.parse("moreminecrafts:heaven_cream_stone")).defaultBlockState(), Block.UPDATE_ALL);
                    level.setBlock(PLATES_AT.offset(x + 13, 0, z), concrete.defaultBlockState(), Block.UPDATE_ALL);
                }
                cmd(s, "weather clear"); cmd(s, "time set noon");
                s.getPlayerList().getPlayers().getFirst().setGameMode(GameType.SPECTATOR);
            });
            for (String island : new String[]{"p1", "p2"}) {
                BlockPos at = island.equals("p1") ? P1_AT : P2_AT;
                for (View v : VIEWS) {
                    orbit(context, server, connection, at, v.tx(), v.ty(), v.tz(), v.yaw(), v.pitch(), v.dist(), v.ticks(), "noon");
                    fps(context, island + "-" + v.name());
                    snap(context, shot, island + "-" + v.name());
                }
            }
            // P2 at midnight
            for (View v : new View[]{VIEWS[0], VIEWS[3], VIEWS[2]}) {
                orbit(context, server, connection, P2_AT, v.tx(), v.ty(), v.tz(), v.yaw(), v.pitch(), v.dist(), v.ticks() + 60, "midnight");
                snap(context, shot, "p2-night-" + v.name());
            }
            // underside study: light levels, AO off, open-sky control plates
            server.runOnServer(s -> logSkyLight(s.overworld()));
            context.runOnClient(c -> c.options.ambientOcclusion().set(false));
            context.waitTicks(40);
            orbit(context, server, connection, P2_AT, 24, 5, 22, 35, -30, 34, 200, "noon");
            snap(context, shot, "p2-underside-ao-off");
            context.runOnClient(c -> c.options.ambientOcclusion().set(true));
            context.waitTicks(40);
            look(context, server, connection, PLATES_AT, 10.5, -14, -10, 10.5, 0, 4.5, 200, "noon");
            snap(context, shot, "control-plates-from-below");
            look(context, server, connection, PLATES_AT, 10.5, 9, -14, 10.5, 0.5, 4.5, 120, "noon");
            snap(context, shot, "control-plates-from-above-side");
            System.out.println("HEAVEN_P2_CAPTURE_COMPLETE " + shot[0]);
        }
    }

    private static void place(ServerLevel level, String id, BlockPos at) {
        for (int cx = (at.getX() >> 4) - 1; cx <= (at.getX() >> 4) + 4; cx++)
            for (int cz = (at.getZ() >> 4) - 1; cz <= (at.getZ() >> 4) + 4; cz++) level.getChunk(cx, cz);
        var t = level.getStructureTemplateManager().get(Identifier.parse(id)).orElseThrow(() -> new AssertionError("missing " + id));
        if (!t.placeInWorld(level, at, at, new StructurePlaceSettings(), level.getRandom(), Block.UPDATE_CLIENTS))
            throw new AssertionError("placeInWorld failed for " + id);
        System.out.println("HEAVEN_P2_CAPTURE_PLACED " + id + " at " + at);
    }

    private static void cmd(net.minecraft.server.MinecraftServer s, String c) {
        s.getCommands().performPrefixedCommand(s.createCommandSourceStack(), c);
    }

    /** Sky light in the air cell directly below every downward-facing island / plate face. */
    private static void logSkyLight(ServerLevel level) {
        TreeMap<Integer, Integer> island = new TreeMap<>(), cream = new TreeMap<>(), concrete = new TreeMap<>();
        for (int x = 0; x < 45; x++) for (int z = 0; z < 37; z++) for (int y = 0; y < 14; y++) {
            BlockPos p = P2_AT.offset(x, y, z);
            if (!level.getBlockState(p).isAir() && level.getBlockState(p.below()).isAir() && level.getBlockState(p).isSolidRender())
                island.merge(level.getBrightness(LightLayer.SKY, p.below()), 1, Integer::sum);
        }
        for (int x = 0; x < 9; x++) for (int z = 0; z < 9; z++) {
            cream.merge(level.getBrightness(LightLayer.SKY, PLATES_AT.offset(x, -1, z)), 1, Integer::sum);
            concrete.merge(level.getBrightness(LightLayer.SKY, PLATES_AT.offset(x + 13, -1, z)), 1, Integer::sum);
        }
        System.out.println("HEAVEN_P2_SKYLIGHT island_bottom_faces=" + island + " cream_plate=" + cream + " concrete_plate=" + concrete
                + " block_light_under_island=" + level.getBrightness(LightLayer.BLOCK, P2_AT.offset(22, 3, 18)));
    }

    private static void orbit(ClientGameTestContext context, TestDedicatedServerContext server, TestDedicatedServerConnection connection, BlockPos at,
                              double tx, double ty, double tz, double yawDeg, double pitchDeg, double dist, int wait, String time) {
        double yaw = Math.toRadians(yawDeg), pitch = Math.toRadians(pitchDeg);
        look(context, server, connection, at, tx + dist * Math.cos(pitch) * Math.sin(yaw), ty + dist * Math.sin(pitch),
                tz + dist * Math.cos(pitch) * Math.cos(yaw), tx, ty, tz, wait, time);
    }

    private static void look(ClientGameTestContext context, TestDedicatedServerContext server, TestDedicatedServerConnection connection, BlockPos at,
                             double px, double py, double pz, double tx, double ty, double tz, int wait, String time) {
        double dx = tx - px, dz = tz - pz;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(ty - py, Math.hypot(dx, dz)));
        double ax = at.getX() + px, ay = at.getY() + py, az = at.getZ() + pz;
        server.runOnServer(s -> {
            cmd(s, "time set " + time);
            s.getPlayerList().getPlayers().getFirst().teleportTo(s.overworld(), ax, ay, az, Set.of(), yaw, pitch, true);
        });
        connection.waitForClientboundPackets();
        BlockPos target = BlockPos.containing(at.getX() + tx, at.getY() + ty, at.getZ() + tz);
        context.waitFor(c -> c.level != null && c.level.hasChunkAt(target), 600);
        context.waitTicks(wait);
    }

    private static void fps(ClientGameTestContext context, String view) {
        context.runOnClient(c -> System.out.println("HEAVEN_P2_CAPTURE_FPS view=" + view + " fps=" + c.getFps()));
    }

    private static void snap(ClientGameTestContext context, int[] shot, String name) {
        context.runOnClient(c -> c.gui.toastManager().clear());
        System.out.println("HEAVEN_P2_CAPTURE_SCREENSHOT " + context.takeScreenshot("heaven-p2-%02d-%s".formatted(shot[0]++, name)));
    }
}
