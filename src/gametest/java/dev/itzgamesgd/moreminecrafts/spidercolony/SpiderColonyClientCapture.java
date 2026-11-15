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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;

/**
 * Opt-in real-client validation and screenshot capture for Spider Colony worldgen,
 * MotherSpider (2.3x scale, hitbox, pathfinding, provisional texture), and HangingCocoon
 * ({@code -Dmoreminecrafts.spider.capture=true}).
 */
public final class SpiderColonyClientCapture implements FabricClientGameTest {
    private static final int CX = 64;
    private static final int CY = -10;
    private static final int CZ = 64;
    private static final int CAVERN_RADIUS = 13;
    private static final int FLOOR_Y = -20;

    @Override
    public void runTest(ClientGameTestContext context) {
        if (!Boolean.getBoolean("moreminecrafts.spider.capture")) return;

        Properties properties = new Properties();
        properties.setProperty("online-mode", "false");
        properties.setProperty("difficulty", "easy");

        try (TestDedicatedServerContext server = context.worldBuilder().createServer(properties);
             TestDedicatedServerConnection connection = server.connect()) {
            context.waitFor(c -> c.player != null && c.level != null);
            context.runOnClient(c -> {
                if (!c.gui.hud.isHidden()) c.gui.hud.toggle();
            });

            List<BlockPos> cocoonPositions = new ArrayList<>();
            server.runOnServer(s -> {
                ServerLevel level = s.overworld();
                prepareWindowAndCavern(level);

                // (a) Run real worldgen evaluation on window (0, 0)
                ColonyLedger.RecordedColony placed = SpiderColonyWorldgen.evaluateWindow(level, 0, 0);
                if (placed == null) {
                    throw new AssertionError("Check (a) failed: evaluateWindow did not place a Spider Colony");
                }
                System.out.println("SPIDER_COLONY_CHECK_A PASS nestId=" + placed.nestId()
                        + " center=(" + placed.x() + "," + placed.y() + "," + placed.z() + ")"
                        + " radius=" + placed.nestRadius());

                // (b) Collect hanging cocoons and verify ceiling attachment
                for (int dx = -22; dx <= 22; dx++) {
                    for (int dz = -22; dz <= 22; dz++) {
                        for (int y = FLOOR_Y; y <= CY + CAVERN_RADIUS + 4; y++) {
                            BlockPos p = new BlockPos(CX + dx, y, CZ + dz);
                            BlockState st = level.getBlockState(p);
                            if (st.is(ModBlocks.HANGING_COCOON)) {
                                if (!level.getBlockState(p.above()).isFaceSturdy(level, p.above(), Direction.DOWN)) {
                                    throw new AssertionError("Check (b) failed: cocoon at " + p + " has no sturdy ceiling");
                                }
                                cocoonPositions.add(p);
                            }
                        }
                    }
                }
                if (cocoonPositions.isEmpty()) {
                    throw new AssertionError("Check (b) failed: no HangingCocoon blocks placed in colony");
                }
            });

            // Wait 20 ticks and re-verify cocoons did not fall or float, and verify (c) MotherSpider & (d) size/hitbox/pathfinding
            context.waitTicks(20);
            server.runOnServer(s -> {
                ServerLevel level = s.overworld();
                for (BlockPos p : cocoonPositions) {
                    if (!level.getBlockState(p).is(ModBlocks.HANGING_COCOON)
                            || !level.getBlockState(p.above()).isFaceSturdy(level, p.above(), Direction.DOWN)) {
                        throw new AssertionError("Check (b) failed after 20 ticks at " + p);
                    }
                }
                System.out.println("SPIDER_COLONY_CHECK_B PASS cocoons=" + cocoonPositions.size() + " all ceiling-attached");

                List<MotherSpider> mothers = level.getEntities(
                        SpiderColonyEntities.MOTHER,
                        new AABB(CX - 25, FLOOR_Y - 5, CZ - 25, CX + 25, CY + 25, CZ + 25),
                        MotherSpider::isAlive);
                if (mothers.isEmpty()) {
                    throw new AssertionError("Check (c) failed: no MotherSpider spawned in colony");
                }
                MotherSpider mother = mothers.getFirst();
                System.out.println("SPIDER_COLONY_CHECK_C PASS motherId=" + mother.getUUID()
                        + " pos=(" + String.format("%.2f,%.2f,%.2f", mother.getX(), mother.getY(), mother.getZ()) + ")");

                float w = mother.getBbWidth();
                float h = mother.getBbHeight();
                if (Math.abs(w - SpiderColonyEntities.WIDTH_PROVISIONAL) > 0.01F
                        || Math.abs(h - SpiderColonyEntities.HEIGHT_PROVISIONAL) > 0.01F) {
                    throw new AssertionError("Check (d) failed: unexpected hitbox " + w + "x" + h);
                }
                // Trigger pathfinding to a point 7 blocks away on the chamber floor
                mother.snapTo(CX + 0.5, FLOOR_Y + 1, CZ + 0.5, 0, 0);
                boolean started = mother.getNavigation().moveTo(CX + 7.5, FLOOR_Y + 1, CZ + 0.5, 1.0D);
                if (!started) {
                    throw new AssertionError("Check (d) failed: navigation.moveTo returned false");
                }
            });

            context.waitTicks(45);
            server.runOnServer(s -> {
                ServerLevel level = s.overworld();
                MotherSpider mother = level.getEntities(
                        SpiderColonyEntities.MOTHER,
                        new AABB(CX - 25, FLOOR_Y - 5, CZ - 25, CX + 25, CY + 25, CZ + 25),
                        MotherSpider::isAlive).getFirst();
                double moved = Math.hypot(mother.getX() - (CX + 0.5), mother.getZ() - (CZ + 0.5));
                if (moved < 1.0) {
                    throw new AssertionError("Check (d) failed: MotherSpider did not pathfind/move (moved=" + moved + ")");
                }
                System.out.println("SPIDER_COLONY_CHECK_D PASS scale=2.3 hitbox="
                        + mother.getBbWidth() + "x" + mother.getBbHeight() + " pathfindMoved=" + String.format("%.2f", moved));

                // Prepare MotherSpider at center for clean screenshots, freeze AI so it holds pose
                mother.setNoAi(true);
                mother.snapTo(CX + 0.5, FLOOR_Y + 1, CZ + 0.5, 0, 0);
                mother.setYRot(0);
                mother.setYBodyRot(0);
                mother.setYHeadRot(0);

                // Ensure chamber has clear lighting & spectator player has night vision
                addShowcaseLighting(level);
                var player = s.getPlayerList().getPlayers().getFirst();
                player.setGameMode(GameType.SPECTATOR);
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 2000, 0, false, false));
            });
            connection.waitForClientboundPackets();
            context.waitTicks(10);

            // Shot 0: MotherSpider front full-body
            teleportSpectator(server, connection, context, CX + 0.5, FLOOR_Y + 2.5, CZ + 7.5, 180, 6);
            takeShot(context, "spider-colony-00-mother-front");

            // Shot 1: MotherSpider side full-body
            teleportSpectator(server, connection, context, CX + 7.5, FLOOR_Y + 2.5, CZ + 0.5, 90, 6);
            takeShot(context, "spider-colony-01-mother-side");

            // Shot 2: Hanging cocoon close-up
            BlockPos focusCocoon = cocoonPositions.getFirst();
            teleportSpectator(server, connection, context,
                    focusCocoon.getX() + 0.5, focusCocoon.getY() - 0.5, focusCocoon.getZ() + 2.8, 180, -10);
            takeShot(context, "spider-colony-02-cocoon-closeup");

            // Shot 3: Colony overview (wide angle)
            teleportSpectator(server, connection, context, CX - 10.5, FLOOR_Y + 8.0, CZ + 11.5, -135, 22);
            takeShot(context, "spider-colony-03-colony-overview");

            // (e) Verify breaking REWARD=true drops string and REWARD=false spawns vanilla spiders
            server.runOnServer(s -> {
                ServerLevel level = s.overworld();
                var player = s.getPlayerList().getPlayers().getFirst();
                player.setGameMode(GameType.SURVIVAL);

                BlockPos rewardPos = new BlockPos(CX - 3, FLOOR_Y + 5, CZ - 3);
                level.setBlock(rewardPos.above(), Blocks.STONE.defaultBlockState(), 3);
                level.setBlock(rewardPos, ModBlocks.HANGING_COCOON.defaultBlockState().setValue(HangingCocoonBlock.REWARD, true), 3);
                BlockState rState = level.getBlockState(rewardPos);
                rState.getBlock().playerWillDestroy(level, rewardPos, rState, player);
                level.removeBlock(rewardPos, false);
                int stringCount = level.getEntitiesOfClass(ItemEntity.class, new AABB(rewardPos).inflate(4),
                        e -> e.getItem().is(Items.STRING)).stream().mapToInt(e -> e.getItem().getCount()).sum();
                if (stringCount < 1 || stringCount > 2) {
                    throw new AssertionError("Check (e) failed: REWARD=true cocoon dropped " + stringCount + " string");
                }

                BlockPos hazardPos = new BlockPos(CX + 3, FLOOR_Y + 5, CZ - 3);
                level.setBlock(hazardPos.above(), Blocks.STONE.defaultBlockState(), 3);
                level.setBlock(hazardPos, ModBlocks.HANGING_COCOON.defaultBlockState().setValue(HangingCocoonBlock.REWARD, false), 3);
                BlockState hState = level.getBlockState(hazardPos);
                hState.getBlock().playerWillDestroy(level, hazardPos, hState, player);
                level.removeBlock(hazardPos, false);
                var vanillaSpider = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("spider"));
                int spiderCount = level.getEntitiesOfClass(Spider.class, new AABB(hazardPos).inflate(6),
                        e -> e.getType() == vanillaSpider).size();
                if (spiderCount < 1 || spiderCount > 2) {
                    throw new AssertionError("Check (e) failed: REWARD=false cocoon spawned " + spiderCount + " spiders");
                }
                System.out.println("SPIDER_COLONY_CHECK_E PASS rewardString=" + stringCount + " hazardSpiders=" + spiderCount);
            });

            System.out.println("SPIDER_COLONY_CAPTURE_COMPLETE 4");
        }
    }

    private static void prepareWindowAndCavern(ServerLevel level) {
        int halo = SpiderColonyDetector.CHUNK_HALO;
        int side = SpiderColonyDetector.WINDOW_SIDE_CHUNKS;
        for (int cx = -halo; cx <= side; cx++) {
            for (int cz = -halo; cz <= side; cz++) {
                level.getChunk(cx, cz, ChunkStatus.FULL, true);
            }
        }
        // Build solid stone shell & enclosed spherical cavern centered at (CX, CY, CZ) with flat floor at FLOOR_Y
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int outer = CAVERN_RADIUS + 3;
        for (int dx = -outer; dx <= outer; dx++) {
            for (int dz = -outer; dz <= outer; dz++) {
                for (int y = FLOOR_Y - 2; y <= CY + outer; y++) {
                    int dy = y - CY;
                    double distSq = dx * dx + dy * dy + dz * dz;
                    p.set(CX + dx, y, CZ + dz);
                    if (y > FLOOR_Y && distSq <= CAVERN_RADIUS * CAVERN_RADIUS) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                    } else {
                        level.setBlock(p, Blocks.STONE.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    private static void addShowcaseLighting(ServerLevel level) {
        BlockPos[] lights = {
                new BlockPos(CX + 12, FLOOR_Y + 3, CZ),
                new BlockPos(CX - 12, FLOOR_Y + 3, CZ),
                new BlockPos(CX, FLOOR_Y + 3, CZ + 12),
                new BlockPos(CX, FLOOR_Y + 3, CZ - 12),
                new BlockPos(CX, FLOOR_Y, CZ)
        };
        for (BlockPos pos : lights) {
            if (level.getBlockState(pos).is(Blocks.STONE) || level.getBlockState(pos).isAir()) {
                level.setBlock(pos, Blocks.GLOWSTONE.defaultBlockState(), 3);
            }
        }
    }

    private static void teleportSpectator(
            TestDedicatedServerContext server,
            TestDedicatedServerConnection connection,
            ClientGameTestContext context,
            double x, double y, double z, float yaw, float pitch) {
        server.runOnServer(s -> s.getPlayerList().getPlayers().getFirst()
                .teleportTo(s.overworld(), x, y, z, Set.of(), yaw, pitch, true));
        connection.waitForClientboundPackets();
        context.waitTicks(12);
    }

    private static void takeShot(ClientGameTestContext context, String name) {
        context.runOnClient(c -> c.gui.toastManager().clear());
        System.out.println("SPIDER_COLONY_SCREENSHOT " + context.takeScreenshot(name));
    }
}
