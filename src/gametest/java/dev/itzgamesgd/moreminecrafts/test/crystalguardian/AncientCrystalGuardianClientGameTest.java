package dev.itzgamesgd.moreminecrafts.test.crystalguardian;

import dev.itzgamesgd.moreminecrafts.client.crystalguardian.AncientCrystalGuardianModel;
import dev.itzgamesgd.moreminecrafts.client.crystalguardian.AncientCrystalGuardianRenderer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Blocks;

/**
 * REAL Minecraft client verification of the converted Ancient Crystal Guardian (rendered frames, not a separate renderer).
 * Gated by {@code -Dmoreminecrafts.crystalguardian.client=true} so ordinary runs of the client GameTest suite are unaffected.
 *
 * <p>Captures (to {@code <run>/screenshots}): static front/side/back/three-quarter/close-up views, walk clips (side and front),
 * arm-swing clips (side and front), and a walk+swing overlap clip; writes {@code <run>/crystal-guardian-evidence/pose-series.csv}
 * with the actual client-side model rotations of every captured tick and asserts on them (return to rest pose, no rotation
 * accumulation over repeated swings, walk speed scaling, walk/swing independence).
 */
@SuppressWarnings("UnstableApiUsage")
public final class AncientCrystalGuardianClientGameTest implements FabricClientGameTest {
    public static final String PROPERTY = "moreminecrafts.crystalguardian.client";
    private static final int PLATFORM_Y = 199;     // floor blocks; guardian stands at y = 200
    private static final double CAM_Y = 202.0D;
    private static final float EPS = 1.0E-4F;

    private final StringBuilder csv = new StringBuilder("phase,tick,entityX,entityZ," + String.join(",", AncientCrystalGuardianModel.DEBUG_POSE_COLUMNS) + "\n");
    private UUID guardianId;

    @Override
    public void runTest(ClientGameTestContext context) {
        if (!Boolean.getBoolean(PROPERTY)) return;
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            TestServerContext server = world.getServer();
            context.waitFor(c -> c.player != null && c.level != null);
            prepareStage(server);
            world.getConnection().waitForClientboundPackets();
            context.runOnClient(c -> c.gui.hud.toggle());

            camera(context, world, server, 0.5D, CAM_Y, 9.0D, 180.0F, 0.0F);
            spawnGuardian(server, 0.5D, 200.0D, 0.5D);
            world.getConnection().waitForClientboundPackets();
            world.getConnection().waitForChunksRender();
            context.waitTicks(10);

            // ---- A. static views (rest pose) ----
            float[] rest = pose(context);
            requireRest(rest, "initial rest pose");
            shot(context, world, server, "front", 0.5D, CAM_Y, 9.0D, 180.0F, 0.0F);
            shot(context, world, server, "front_closeup", 0.5D, 203.3D, 4.6D, 180.0F, 4.0F);
            shot(context, world, server, "side_east", 9.5D, CAM_Y, 0.5D, 90.0F, 0.0F);
            shot(context, world, server, "side_west", -8.5D, CAM_Y, 0.5D, -90.0F, 0.0F);
            shot(context, world, server, "back", 0.5D, CAM_Y, -8.0D, 0.0F, 0.0F);
            shot(context, world, server, "three_quarter_front_right", 6.4D, 202.6D, 6.9D, 138.0F, 5.0F);
            shot(context, world, server, "three_quarter_back_left", -5.8D, 202.6D, -5.2D, -42.0F, 5.0F);
            shot(context, world, server, "top_down", 0.5D, 209.0D, 3.0D, 180.0F, 62.0F);

            // ---- B. walk, side view (speed x1.0) ----
            placeGuardian(server, 0.5D, -5.0D, 0.0F);
            camera(context, world, server, 11.5D, CAM_Y, 0.5D, 90.0F, 0.0F);
            context.waitTicks(5);
            server.runCommand("mmcrystalguardian walk 70 1.0");
            double walkStartZ = guardianZ(server);
            capture(context, "walk_side", 70, true);
            double walkedBlocks = guardianZ(server) - walkStartZ;
            if (walkedBlocks < 4.0D) throw new AssertionError("guardian walked only " + walkedBlocks + " blocks in 70 ticks");
            server.runCommand("mmcrystalguardian stop");
            context.waitTicks(40);
            requireRest(pose(context), "pose after walk stop");

            // ---- C. walk, front view ----
            placeGuardian(server, 0.5D, -5.0D, 0.0F);
            camera(context, world, server, 0.5D, CAM_Y, 12.0D, 180.0F, 0.0F);
            context.waitTicks(5);
            server.runCommand("mmcrystalguardian walk 60 1.0");
            capture(context, "walk_front", 60, true);
            server.runCommand("mmcrystalguardian stop");
            context.waitTicks(40);
            requireRest(pose(context), "pose after front walk stop");

            // ---- D. walk speed scaling (pose series only): faster walking must cycle the legs faster ----
            int slowCycles = legZeroCrossings(context, server, "walk_x0.5", 0.5D);
            int fastCycles = legZeroCrossings(context, server, "walk_x1.5", 1.5D);
            System.out.println("CRYSTAL_GUARDIAN_WALK_SPEED_SCALING slowCrossings=" + slowCycles + " fastCrossings=" + fastCycles);
            if (fastCycles <= slowCycles) throw new AssertionError("leg cycle rate does not increase with speed: " + slowCycles + " vs " + fastCycles);

            // ---- E. single arm swing, side view ----
            placeGuardian(server, 0.5D, 0.5D, 0.0F);
            camera(context, world, server, 10.5D, CAM_Y + 0.4D, 0.5D, 90.0F, 4.0F);
            context.waitTicks(8);
            server.runCommand("mmcrystalguardian swing");
            List<float[]> swing = capture(context, "swing_side", GuardianPoseMath.SWING_TICKS + 6, true);
            double armMax = swing.stream().mapToDouble(p -> p[4]).max().orElse(0), armMin = swing.stream().mapToDouble(p -> p[4]).min().orElse(0);
            if (armMax < GuardianPoseMath.ARM_WINDUP - 0.25F || armMin > GuardianPoseMath.ARM_STRIKE + 0.25F)
                throw new AssertionError("right arm did not reach windup/strike extremes: max=" + armMax + " min=" + armMin);
            requireRest(swing.getLast(), "pose at end of swing");
            for (float[] p : swing) // 0.01 rad tolerance: vanilla walk-animation speed decays asymptotically after the preceding walk test
            if (Math.abs(p[5]) > 0.01F) throw new AssertionError("left arm moved during a standing swing: " + p[5]);

            // ---- F. single arm swing, front three-quarter ----
            placeGuardian(server, 0.5D, 0.5D, 0.0F);
            camera(context, world, server, 6.0D, 202.8D, 8.0D, 143.0F, 6.0F);
            context.waitTicks(8);
            server.runCommand("mmcrystalguardian swing");
            capture(context, "swing_front", GuardianPoseMath.SWING_TICKS + 6, true);

            // ---- G. repeated swings: no accumulation ----
            server.runCommand("mmcrystalguardian loop 3 10");
            int loopTicks = 3 * (GuardianPoseMath.SWING_TICKS + 10) + 10;
            List<float[]> loop = capture(context, "swing_loop", loopTicks, false);
            requireRest(loop.getLast(), "pose after repeated swings");
            List<double[]> peaks = swingPeaks(loop);
            System.out.println("CRYSTAL_GUARDIAN_LOOP_PEAKS " + peaks.stream().map(a -> a[0] + "/" + a[1]).toList());
            if (peaks.size() != 3) throw new AssertionError("expected 3 swings in loop, found " + peaks.size());
            for (double[] a : peaks) {
                if (Math.abs(a[0] - peaks.get(0)[0]) > 0.05D || Math.abs(a[1] - peaks.get(0)[1]) > 0.05D)
                    throw new AssertionError("swing peaks differ across repeats (accumulation?): " + peaks.stream().map(x -> x[0] + "/" + x[1]).toList());
            }
            for (int i = 0; i < 4; i++) { // a swing requested during a swing is ignored: no stacked swing
                server.runCommand("mmcrystalguardian swing");
                context.waitTicks(2);
            }
            context.waitTicks(GuardianPoseMath.SWING_TICKS + 10);
            requireRest(pose(context), "pose after spammed swing requests");

            // ---- H. walk + swing overlap: legs keep walking, arm follows the swing ----
            placeGuardian(server, 0.5D, -4.0D, 0.0F);
            camera(context, world, server, 11.5D, CAM_Y, 0.5D, 90.0F, 0.0F);
            context.waitTicks(5);
            server.runCommand("mmcrystalguardian walk 80 1.0");
            context.waitTicks(15);
            server.runCommand("mmcrystalguardian swing");
            List<float[]> both = capture(context, "walk_and_swing", 60, true);
            double armPeak = both.stream().mapToDouble(p -> Math.abs(p[4])).max().orElse(0);
            double legPeak = both.stream().mapToDouble(p -> Math.abs(p[0])).max().orElse(0);
            if (armPeak < 1.2D) throw new AssertionError("swing did not dominate the right arm while walking: " + armPeak);
            if (legPeak < 0.15D) throw new AssertionError("legs stopped walking during the swing: " + legPeak);
            server.runCommand("mmcrystalguardian reset");
            context.waitTicks(GuardianPoseMath.SWING_TICKS + 40);
            requireRest(pose(context), "pose after walk+swing reset");

            writeSeries();
            System.out.println("CRYSTAL_GUARDIAN_CLIENT_PASS");
        }
    }

    // ---------------------------------------------------------------- stage / entity helpers

    private void prepareStage(TestServerContext server) {
        server.runOnServer(s -> {
            ServerLevel level = s.overworld();
            for (int x = -30; x <= 30; x++) for (int z = -30; z <= 30; z++) {
                level.setBlock(new BlockPos(x, PLATFORM_Y, z), Blocks.SMOOTH_QUARTZ.defaultBlockState(), 18);
                for (int y = PLATFORM_Y + 1; y <= PLATFORM_Y + 16; y++) level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 18);
            }
        });
        server.runCommand("time set noon");
        server.runCommand("weather clear");
        server.runCommand("difficulty peaceful");
        server.runCommand("gamemode spectator @a");
    }

    private void spawnGuardian(TestServerContext server, double x, double y, double z) {
        server.runOnServer(s -> {
            AncientCrystalGuardianTest g = AncientCrystalGuardianTestEntities.TYPE.create(s.overworld(), EntitySpawnReason.COMMAND);
            if (g == null) throw new AssertionError("could not create test entity");
            g.snapTo(x, y, z, 0.0F, 0.0F);
            s.overworld().addFreshEntity(g);
            guardianId = g.getUUID();
        });
    }

    private void placeGuardian(TestServerContext server, double x, double z, float yaw) {
        server.runCommand("mmcrystalguardian reset");
        server.runOnServer(s -> {
            var g = s.overworld().getEntity(guardianId);
            if (g == null) throw new AssertionError("guardian missing");
            g.snapTo(x, 200.0D, z, yaw, 0.0F);
            g.setYHeadRot(yaw);
            g.setDeltaMovement(0, 0, 0);
        });
    }

    private double guardianZ(TestServerContext server) {
        return server.computeOnServer(s -> s.overworld().getEntity(guardianId).getZ());
    }

    private void camera(ClientGameTestContext context, TestSingleplayerContext world, TestServerContext server,
                        double x, double y, double z, float yaw, float pitch) {
        server.runOnServer(s -> s.getPlayerList().getPlayers().getFirst().teleportTo(s.overworld(), x, y, z, Set.of(), yaw, pitch, true));
        world.getConnection().waitForClientboundPackets();
    }

    private void shot(ClientGameTestContext context, TestSingleplayerContext world, TestServerContext server, String name,
                      double x, double y, double z, float yaw, float pitch) {
        camera(context, world, server, x, y, z, yaw, pitch);
        context.waitTicks(6);
        context.takeScreenshot("crystal_guardian_" + name);
        requireRest(pose(context), "static view " + name);
    }

    // ---------------------------------------------------------------- pose capture / series

    /** Current client-side model pose of the (single) test guardian; waits for one more rendered tick first is the caller's job. */
    private float[] pose(ClientGameTestContext context) {
        return context.computeOnClient(c -> {
            for (var e : c.level.entitiesForRendering()) {
                if (e instanceof AncientCrystalGuardianTest g) {
                    var renderer = (AncientCrystalGuardianRenderer) c.getEntityRenderDispatcher().getRenderer(g);
                    return renderer.debugModel().debugPose();
                }
            }
            throw new AssertionError("no guardian entity on the client");
        });
    }

    private double[] entityXZ(ClientGameTestContext context) {
        return context.computeOnClient(c -> {
            for (var e : c.level.entitiesForRendering()) if (e instanceof AncientCrystalGuardianTest g) return new double[] {g.getX(), g.getZ()};
            return new double[] {0, 0};
        });
    }

    /** Advances {@code ticks} client ticks; records the model pose every tick and optionally screenshots every tick. */
    private List<float[]> capture(ClientGameTestContext context, String phase, int ticks, boolean screenshots) {
        List<float[]> series = new ArrayList<>();
        for (int i = 0; i < ticks; i++) {
            context.waitTick();
            if (screenshots) context.takeScreenshot(String.format("crystal_guardian_%s_%03d", phase, i));
            float[] p = pose(context);
            double[] xz = entityXZ(context);
            series.add(p);
            csv.append(phase).append(',').append(i).append(',').append(String.format("%.3f,%.3f", xz[0], xz[1]));
            for (float v : p) csv.append(',').append(String.format("%.5f", v));
            csv.append('\n');
        }
        return series;
    }

    private int legZeroCrossings(ClientGameTestContext context, TestServerContext server, String phase, double speed) {
        placeGuardian(server, 0.5D, -10.0D, 0.0F);
        context.waitTicks(6);
        server.runCommand("mmcrystalguardian walk 80 " + speed);
        List<float[]> s = capture(context, phase, 80, false);
        server.runCommand("mmcrystalguardian stop");
        context.waitTicks(40);
        int crossings = 0;
        for (int i = 1; i < s.size(); i++) if (s.get(i - 1)[0] * s.get(i)[0] < 0.0F) crossings++;
        return crossings;
    }

    /** Peaks (max, min of armRight) of every contiguous window in which the right arm is away from rest. */
    private static List<double[]> swingPeaks(List<float[]> series) {
        List<double[]> peaks = new ArrayList<>();
        double max = 0, min = 0;
        boolean in = false;
        for (float[] p : series) {
            boolean away = Math.abs(p[4]) > 1.0E-3F;
            if (away) { if (!in) { max = 0; min = 0; in = true; } max = Math.max(max, p[4]); min = Math.min(min, p[4]); }
            else if (in) { peaks.add(new double[] {max, min}); in = false; }
        }
        if (in) peaks.add(new double[] {max, min});
        return peaks;
    }

    private static void requireRest(float[] pose, String what) {
        for (int i = 0; i < pose.length; i++) {
            if (Math.abs(pose[i]) > EPS) {
                throw new AssertionError(what + ": " + AncientCrystalGuardianModel.DEBUG_POSE_COLUMNS[i] + " = " + pose[i] + " is not the rest value (0)");
            }
        }
    }

    private void writeSeries() {
        try {
            Path dir = Path.of("crystal-guardian-evidence");
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("pose-series.csv"), csv.toString());
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }
}
