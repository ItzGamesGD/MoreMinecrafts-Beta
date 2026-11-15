package dev.itzgamesgd.moreminecrafts.spidercolony;

import dev.itzgamesgd.moreminecrafts.registry.ModBlocks;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;

/**
 * Opt-in real-client validation and screenshot capture in a genuine naturally-generated
 * Overworld cave (no artificial carving) via {@code -Dmoreminecrafts.spider.capture=true}.
 */
public final class SpiderColonyClientCapture implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        if (!Boolean.getBoolean("moreminecrafts.spider.capture")) return;

        Properties properties = new Properties();
        properties.setProperty("online-mode", "false");
        properties.setProperty("difficulty", "easy");
        properties.setProperty("level-seed", "42");

        try (TestDedicatedServerContext server = context.worldBuilder().createServer(properties);
             TestDedicatedServerConnection connection = server.connect()) {
            context.waitFor(c -> c.player != null && c.level != null);
            context.runOnClient(c -> {
                if (!c.gui.hud.isHidden()) c.gui.hud.toggle();
            });

            List<BlockPos> cocoonPositions = new ArrayList<>();
            List<BlockPos> eggSacPositions = new ArrayList<>();
            List<BlockPos> cobwebPositions = new ArrayList<>();
            int[] colonyCenter = new int[3];

            server.runOnServer(s -> {
                ServerLevel level = s.overworld();
                ColonyLedger.RecordedColony placed = findAndPlaceInNaturalCave(level);
                if (placed == null) {
                    throw new AssertionError("No natural Overworld cave qualified in search windows");
                }
                colonyCenter[0] = placed.x();
                colonyCenter[1] = placed.y();
                colonyCenter[2] = placed.z();
                System.out.println("SPIDER_COLONY_NATURAL_CAVE_OK seed=" + level.getSeed()
                        + " nestId=" + placed.nestId()
                        + " center=(" + placed.x() + "," + placed.y() + "," + placed.z() + ")"
                        + " radius=" + placed.nestRadius());

                for (int dx = -56; dx <= 56; dx++) {
                    for (int dz = -56; dz <= 56; dz++) {
                        for (int y = SpiderColonyDetector.Y_MIN; y <= SpiderColonyDetector.Y_MAX; y++) {
                            BlockPos p = new BlockPos(placed.x() + dx, y, placed.z() + dz);
                            BlockState st = level.getBlockState(p);
                            if (st.is(ModBlocks.HANGING_COCOON)) cocoonPositions.add(p);
                            else if (st.is(ModBlocks.SPIDER_EGG_SAC)) eggSacPositions.add(p);
                            else if (st.is(Blocks.COBWEB)) cobwebPositions.add(p);
                        }
                    }
                }
                System.out.println("SPIDER_COLONY_DECOR_COUNTS cocoons=" + cocoonPositions.size()
                        + " eggs=" + eggSacPositions.size() + " cobwebs=" + cobwebPositions.size());
                if (cocoonPositions.isEmpty() || eggSacPositions.isEmpty() || cobwebPositions.isEmpty()) {
                    throw new AssertionError("Expected cocoons, egg sacs, and cobwebs in natural colony");
                }
            });

            context.waitTicks(15);
            double[] motherPos = new double[3];
            server.runOnServer(s -> {
                ServerLevel level = s.overworld();
                BlockPos center = new BlockPos(colonyCenter[0], colonyCenter[1], colonyCenter[2]);
                List<MotherSpider> mothers = level.getEntities(
                        SpiderColonyEntities.MOTHER,
                        new AABB(center).inflate(30),
                        MotherSpider::isAlive);
                if (mothers.isEmpty()) {
                    throw new AssertionError("No MotherSpider found in natural colony");
                }
                MotherSpider mother = mothers.getFirst();
                mother.setNoAi(true);
                mother.setYRot(0);
                mother.setYBodyRot(0);
                mother.setYHeadRot(0);
                motherPos[0] = mother.getX();
                motherPos[1] = mother.getY();
                motherPos[2] = mother.getZ();

                var player = s.getPlayerList().getPlayers().getFirst();
                player.setGameMode(GameType.SPECTATOR);
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 4000, 0, false, false));
            });
            connection.waitForClientboundPackets();
            context.waitTicks(10);

            // 0: Mother front
            lookAtFromOpenAir(server, connection, context,
                    new BlockPos((int) motherPos[0], (int) motherPos[1] + 1, (int) motherPos[2]),
                    5.5, 1.5, 0.0, 1.0);
            takeShot(context, "spider-colony-00-mother-front");

            // 1: Mother side
            lookAtFromOpenAir(server, connection, context,
                    new BlockPos((int) motherPos[0], (int) motherPos[1] + 1, (int) motherPos[2]),
                    5.5, 1.5, 1.0, 0.0);
            takeShot(context, "spider-colony-01-mother-side");

            // 2: Hanging cocoon close-up
            BlockPos cocoon = cocoonPositions.getFirst();
            lookAtFromOpenAir(server, connection, context, cocoon, 3.0, -0.5, 0.0, 1.0);
            takeShot(context, "spider-colony-02-cocoon-closeup");

            // 3: Colony overview (inside natural cavern)
            BlockPos overviewTarget = new BlockPos((int) motherPos[0], (int) motherPos[1] + 1, (int) motherPos[2]);
            lookAtFromOpenAir(server, connection, context, overviewTarget, 11.0, 4.5, -0.7, 0.7);
            takeShot(context, "spider-colony-03-colony-overview");

            // 4: Egg sac close-up
            BlockPos eggSac = eggSacPositions.getFirst();
            lookAtFromOpenAir(server, connection, context, eggSac, 2.8, 1.0, 0.0, 1.0);
            takeShot(context, "spider-colony-04-eggsac-closeup");

            // 5: Cobweb close-up
            BlockPos cobweb = cobwebPositions.getFirst();
            lookAtFromOpenAir(server, connection, context, cobweb, 3.0, 0.2, 0.0, 1.0);
            takeShot(context, "spider-colony-05-cobweb-closeup");

            System.out.println("SPIDER_COLONY_CAPTURE_COMPLETE 6");
        }
    }

    private static ColonyLedger.RecordedColony findAndPlaceInNaturalCave(ServerLevel level) {
        ColonyLedger ledger = SpiderColonyEntities.data(level);
        if (!ledger.recordedColonies().isEmpty()) {
            return ledger.recordedColonies().getFirst();
        }
        int halo = SpiderColonyDetector.CHUNK_HALO;
        int side = SpiderColonyDetector.WINDOW_SIDE_CHUNKS;
        for (int ring = 0; ring <= 4; ring++) {
            for (int rx = -ring; rx <= ring; rx++) {
                for (int rz = -ring; rz <= ring; rz++) {
                    if (Math.abs(rx) != ring && Math.abs(rz) != ring) continue;
                    int wx = rx * side;
                    int wz = rz * side;
                    if (ledger.isWindowEvaluated(wx, wz)) continue;
                    for (int cx = wx - halo; cx <= wx + side; cx++) {
                        for (int cz = wz - halo; cz <= wz + side; cz++) {
                            level.getChunk(cx, cz, ChunkStatus.FULL, true);
                        }
                    }
                    ColonyLedger.RecordedColony placed = SpiderColonyWorldgen.evaluateWindow(level, wx, wz);
                    System.out.println("SPIDER_COLONY_PROBE wx=" + wx + " wz=" + wz + " placed=" + (placed != null));
                    if (placed != null) {
                        return placed;
                    }
                }
            }
        }
        return null;
    }

    private static void lookAtFromOpenAir(
            TestDedicatedServerContext server,
            TestDedicatedServerConnection connection,
            ClientGameTestContext context,
            BlockPos target,
            double dist,
            double dyOffset,
            double prefDx,
            double prefDz) {
        double[] cam = new double[5]; // x, y, z, yaw, pitch
        server.runOnServer(s -> {
            ServerLevel level = s.overworld();
            double[][] dirs = {
                    {prefDx, prefDz},
                    {1, 0}, {-1, 0}, {0, 1}, {0, -1},
                    {0.7, 0.7}, {-0.7, 0.7}, {0.7, -0.7}, {-0.7, -0.7}
            };
            double bestX = target.getX() + 0.5 + prefDx * 2.5;
            double bestY = target.getY() + 0.5 + dyOffset;
            double bestZ = target.getZ() + 0.5 + prefDz * 2.5;

            outer:
            for (double d = dist; d >= 2.0; d -= 1.0) {
                for (double[] dir : dirs) {
                    double len = Math.hypot(dir[0], dir[1]);
                    if (len < 1e-6) continue;
                    double cx = target.getX() + 0.5 + (dir[0] / len) * d;
                    double cy = target.getY() + 0.5 + dyOffset;
                    double cz = target.getZ() + 0.5 + (dir[1] / len) * d;
                    BlockPos cp = BlockPos.containing(cx, cy, cz);
                    if (level.getBlockState(cp).isAir() && level.getBlockState(cp.above()).isAir()) {
                        bestX = cx;
                        bestY = cy;
                        bestZ = cz;
                        break outer;
                    }
                }
            }

            double tx = target.getX() + 0.5 - bestX;
            double ty = target.getY() + 0.5 - (bestY + 1.62);
            double tz = target.getZ() + 0.5 - bestZ;
            double horiz = Math.hypot(tx, tz);
            float yaw = (float) Math.toDegrees(Math.atan2(-tx, tz));
            float pitch = (float) Math.toDegrees(-Math.atan2(ty, horiz));
            cam[0] = bestX;
            cam[1] = bestY;
            cam[2] = bestZ;
            cam[3] = yaw;
            cam[4] = pitch;

            s.getPlayerList().getPlayers().getFirst()
                    .teleportTo(level, bestX, bestY, bestZ, Set.of(), yaw, pitch, true);
        });
        connection.waitForClientboundPackets();
        context.waitTicks(12);
    }

    private static void takeShot(ClientGameTestContext context, String name) {
        context.runOnClient(c -> c.gui.toastManager().clear());
        System.out.println("SPIDER_COLONY_SCREENSHOT " + context.takeScreenshot(name));
    }
}
