package dev.itzgamesgd.moreminecrafts.mob.sky;

import dev.itzgamesgd.moreminecrafts.mixin.TrialSpawnerConfigAccessor;
import dev.itzgamesgd.moreminecrafts.skytower.SkyTowerLayout;
import dev.itzgamesgd.moreminecrafts.skytower.SkyTowerWorldgenRegistration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;

/**
 * Opt-in real-client captures for the Sky Islands verification
 * ({@code -Dmoreminecrafts.sky.capture=true}): the ten sky mobs (group C and group B) full body in a lit test room,
 * natural sky-island far views, the Sky Tower exterior and the combo-floor interiors of a naturally generated tower.
 * Nothing here asserts gameplay; it only records what is rendered.
 */
public final class SkyIslandsClientCapture implements FabricClientGameTest {
    private static final String GATE = "moreminecrafts.sky.capture";
    private static final String SEED = "2632028";
    private static final int ROOM_Y = 250;
    private static final int SPACING = 6;
    private static final int[] FLOORS = {10, 21, 33, 47};
    private static final int SCAN_RADIUS = 160;

    private record Found(BoundingBox box, BoundingBox towerBox, BlockPos towerPiecePos,
                         net.minecraft.world.level.block.Rotation towerRotation, int cx, int cz) {}

    @Override
    public void runTest(ClientGameTestContext context) {
        if (!Boolean.getBoolean(GATE)) return;
        Properties properties = new Properties();
        properties.setProperty("online-mode", "false");
        properties.setProperty("difficulty", "easy");
        properties.setProperty("level-seed", SEED);
        properties.setProperty("view-distance", "12");
        properties.setProperty("simulation-distance", "6");
        properties.setProperty("spawn-monsters", "false");
        properties.setProperty("max-tick-time", "-1");
        int[] shot = {0};
        try (TestDedicatedServerContext server = context.worldBuilder().createServer(properties);
             TestDedicatedServerConnection connection = server.connect()) {
            context.waitFor(c -> c.player != null && c.level != null);
            context.runOnClient(c -> {
                if (!c.gui.hud.isHidden()) c.gui.hud.toggle();
            });
            server.runOnServer(s -> {
                System.out.println("SKY_CAPTURE_SEED " + s.overworld().getSeed());
                var p = s.getPlayerList().getPlayers().getFirst();
                p.setGameMode(GameType.SPECTATOR);
                p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 100000, 0, false, false));
            });

            captureMobs(context, server, connection, shot);
            Found found = locate(context, server);
            captureIslandAndTower(context, server, connection, found, shot);
            System.out.println("SKY_ISLANDS_CAPTURE_COMPLETE " + shot[0]);
        }
    }

    // ---- mobs -----------------------------------------------------------------------------------------------

    private static void captureMobs(ClientGameTestContext context, TestDedicatedServerContext server,
                                    TestDedicatedServerConnection connection, int[] shot) {
        List<EntityType<? extends Mob>> types = new ArrayList<>(SkyNormalMobs.allTypes());
        types.addAll(SkyVariantMobs.allTypes());
        server.runOnServer(s -> {
            ServerLevel level = s.overworld();
            int maxX = types.size() * SPACING + 2;
            for (int x = -6; x <= maxX; x++) {
                for (int z = -6; z <= 14; z++) {
                    level.setBlock(new BlockPos(x, ROOM_Y - 1, z), Blocks.SMOOTH_STONE.defaultBlockState(), 3);
                    level.setBlock(new BlockPos(x, ROOM_Y + 8, z), Blocks.GLOWSTONE.defaultBlockState(), 3);
                    for (int y = ROOM_Y; y < ROOM_Y + 8; y++) {
                        level.setBlock(new BlockPos(x, y, z), z == -6
                                ? Blocks.SMOOTH_STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
            for (int i = 0; i < types.size(); i++) {
                EntityType<? extends Mob> type = types.get(i);
                Mob mob = type.create(level, EntitySpawnReason.COMMAND);
                if (mob == null) throw new AssertionError("Could not create " + type);
                mob.setNoAi(true);
                mob.setPersistenceRequired();
                mob.snapTo(i * SPACING + 0.5, ROOM_Y, 0.5, 0, 0);
                mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()),
                        EntitySpawnReason.COMMAND, null);
                if (mob instanceof net.minecraft.world.entity.monster.zombie.Zombie z) z.setBaby(false);
                if (!level.addFreshEntity(mob)) throw new AssertionError("Could not add " + type);
                System.out.println("SKY_MOB_SPAWNED " + BuiltInRegistries.ENTITY_TYPE.getKey(type)
                        + " box=" + String.format("%.2fx%.2f", mob.getBbWidth(), mob.getBbHeight()));
            }
        });
        for (int i = 0; i < types.size(); i++) {
            EntityType<? extends Mob> type = types.get(i);
            double mx = i * SPACING + 0.5;
            float yaw = 30;
            server.runOnServer(s -> {
                ServerLevel level = s.overworld();
                for (Mob mob : level.getEntitiesOfClass(Mob.class,
                        new net.minecraft.world.phys.AABB(-10, ROOM_Y - 1, -10, types.size() * SPACING + 10, ROOM_Y + 8, 10))) {
                    mob.setYRot(yaw);
                    mob.setYBodyRot(yaw);
                    mob.setYHeadRot(yaw);
                }
                s.getPlayerList().getPlayers().getFirst().teleportTo(level, mx, ROOM_Y + 1.2, 6.5, Set.of(), 180, 6, true);
            });
            connection.waitForClientboundPackets();
            context.waitTicks(40);
            context.runOnClient(c -> {
                c.gui.toastManager().clear();
                boolean seen = false;
                for (var entity : c.level.entitiesForRendering()) if (entity.getType() == type) seen = true;
                if (!seen) throw new AssertionError("Client did not receive " + type);
            });
            String name = BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath().replace('_', '-');
            snap(context, shot, "mob-" + name);
        }
        // Lineups: group C (first five) and group B (last five).
        for (int group = 0; group < 2; group++) {
            double mid = group * 5 * SPACING + 2 * SPACING + 0.5;
            server.runOnServer(s -> s.getPlayerList().getPlayers().getFirst()
                    .teleportTo(s.overworld(), mid, ROOM_Y + 2.0, 13.5, Set.of(), 180, 6, true));
            connection.waitForClientboundPackets();
            context.waitTicks(40);
            snap(context, shot, group == 0 ? "lineup-group-c" : "lineup-group-b");
        }
    }

    // ---- natural island discovery ----------------------------------------------------------------------------

    private static Found locate(ClientGameTestContext context, TestDedicatedServerContext server) {
        Found[] result = {null};
        for (int radius = 0; radius <= SCAN_RADIUS && result[0] == null; radius++) {
            final int r = radius;
            server.runOnServer(s -> {
                ServerLevel level = s.overworld();
                var registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
                for (int cx = -r; cx <= r && result[0] == null; cx++) {
                    for (int cz = -r; cz <= r && result[0] == null; cz++) {
                        if (Math.abs(cx) != r && Math.abs(cz) != r) continue;
                        var chunk = level.getChunk(cx, cz, ChunkStatus.STRUCTURE_STARTS);
                        for (var entry : chunk.getAllStarts().entrySet()) {
                            var id = registry.getKey(entry.getKey());
                            if (id == null || !id.getNamespace().equals("moreminecrafts")
                                    || !id.getPath().startsWith("sky_island_") || !entry.getValue().isValid()) continue;
                            for (var raw : entry.getValue().getPieces()) {
                                if (raw instanceof PoolElementStructurePiece piece
                                        && piece.getElement() instanceof SinglePoolElement single
                                        && single.getTemplateLocation() != null
                                        && single.getTemplateLocation().getPath().matches(
                                                "sky_islands/(snowy|meadow|forest|badlands|desert|rocky)/tower")) {
                                    result[0] = new Found(entry.getValue().getBoundingBox(), piece.getBoundingBox(),
                                            piece.getPosition(), piece.getRotation(), cx, cz);
                                    System.out.println("SKY_CAPTURE_FOUND_TOWER_START structure=" + id
                                            + " chunk=" + cx + "," + cz + " box=" + entry.getValue().getBoundingBox());
                                }
                            }
                        }
                    }
                }
            });
            context.waitTick();
        }
        if (result[0] == null) throw new AssertionError("No sky island with a tower within " + SCAN_RADIUS + " chunks");
        return result[0];
    }

    // ---- island and tower ------------------------------------------------------------------------------------

    private static void captureIslandAndTower(ClientGameTestContext context, TestDedicatedServerContext server,
                                              TestDedicatedServerConnection connection, Found found, int[] shot) {
        BoundingBox box = found.box();
        BlockPos origin = SkyTowerWorldgenRegistration.towerOrigin(found.towerPiecePos(), found.towerRotation());
        int rotation = SkyTowerWorldgenRegistration.rotationIndex(found.towerRotation());
        var site = new SkyTowerLayout.Site("minecraft:overworld", origin.getX(), origin.getY(), origin.getZ(), rotation);
        System.out.println("SKY_CAPTURE_TOWER_SITE " + site.id());
        server.runOnServer(s -> {
            ServerLevel level = s.overworld();
            for (int cx = (box.minX() - 16) >> 4; cx <= (box.maxX() + 16) >> 4; cx++)
                for (int cz = (box.minZ() - 16) >> 4; cz <= (box.maxZ() + 16) >> 4; cz++)
                    level.getChunk(cx, cz, ChunkStatus.FULL, true);
        });
        // Let the mod's chunk-load hook configure the tower spawners naturally; nothing is applied by this test.
        context.waitTicks(100);
        List<String> configs = new ArrayList<>();
        server.runOnServer(s -> {
            ServerLevel level = s.overworld();
            for (int i = 0; i < 16; i++) {
                var pt = site.spawner(i);
                var pos = new BlockPos(pt.x(), pt.y(), pt.z());
                String cfg = "missing";
                if (level.getBlockEntity(pos) instanceof TrialSpawnerBlockEntity be) {
                    cfg = ((TrialSpawnerConfigAccessor) (Object) be.getTrialSpawner()).moreminecrafts$config()
                            .normal().unwrapKey().map(k -> k.identifier().toString()).orElse("direct");
                }
                configs.add(cfg);
                System.out.println("SKY_TOWER_SPAWNER index=" + i + " expectedCombo="
                        + dev.itzgamesgd.moreminecrafts.skytower.SkyTowerTrialSpawners.combo(i)
                        + " normalConfig=" + cfg);
            }
        });

        double cx = (box.minX() + box.maxX()) / 2.0, cz = (box.minZ() + box.maxZ()) / 2.0;
        double cy = (box.minY() + box.maxY()) / 2.0;
        double span = Math.max(box.getXSpan(), box.getZSpan());
        double dist = Math.min(110, span / 2.0 + 45);
        look(context, server, connection, cx + dist, cy + 14, cz + dist, cx, cy, cz, 40);
        snap(context, shot, "island-far-1");
        look(context, server, connection, cx - dist, cy + 14, cz - dist, cx, cy, cz, 40);
        snap(context, shot, "island-far-2");

        int[] tb = site.bounds();
        double tx = (tb[0] + tb[3]) / 2.0, tz = (tb[2] + tb[5]) / 2.0, ty = tb[1] + 40;
        look(context, server, connection, tx + 70, ty + 12, tz + 70, tx, ty, tz, 60);
        snap(context, shot, "tower-exterior-1");
        look(context, server, connection, tx - 70, ty + 12, tz - 70, tx, ty, tz, 60);
        snap(context, shot, "tower-exterior-2");
        look(context, server, connection, tx + 40, tb[1] + 20, tz - 40, tx, tb[1] + 25, tz, 60);
        snap(context, shot, "tower-exterior-close");

        String[] floorCombos = {"a-b", "b-c", "c-d-e", "d-e"};
        for (int f = 0; f < FLOORS.length; f++) {
            var from1 = site.at(21, FLOORS[f] + 3, 21);
            var to1 = site.at(31, FLOORS[f] + 2, 31);
            look(context, server, connection, from1.x() + .5, from1.y(), from1.z() + .5, to1.x() + .5, to1.y(), to1.z() + .5, 50);
            snap(context, shot, "tower-floor%d-combos-%s-a".formatted(f, floorCombos[f]));
            var from2 = site.at(27, FLOORS[f] + 3, 27);
            var to2 = site.at(17, FLOORS[f] + 2, 17);
            look(context, server, connection, from2.x() + .5, from2.y(), from2.z() + .5, to2.x() + .5, to2.y(), to2.z() + .5, 50);
            snap(context, shot, "tower-floor%d-combos-%s-b".formatted(f, floorCombos[f]));
        }
    }

    private static void look(ClientGameTestContext context, TestDedicatedServerContext server,
                             TestDedicatedServerConnection connection,
                             double px, double py, double pz, double tx, double ty, double tz, int wait) {
        double dx = tx - px, dz = tz - pz;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(ty - py, Math.hypot(dx, dz)));
        server.runOnServer(s -> s.getPlayerList().getPlayers().getFirst()
                .teleportTo(s.overworld(), px, py, pz, Set.of(), yaw, pitch, true));
        connection.waitForClientboundPackets();
        context.waitTicks(wait);
    }

    private static void snap(ClientGameTestContext context, int[] shot, String name) {
        context.runOnClient(c -> c.gui.toastManager().clear());
        System.out.println("SKY_CAPTURE_SCREENSHOT " + context.takeScreenshot("sky-islands-%02d-%s".formatted(shot[0]++, name)));
    }
}
