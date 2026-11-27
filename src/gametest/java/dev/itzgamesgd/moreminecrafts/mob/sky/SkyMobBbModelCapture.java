package dev.itzgamesgd.moreminecrafts.mob.sky;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/**
 * Real-client screenshots of the the three remodelled Sky Mobs (Blockbench models rendered by BbModelCapture) for appearance review
 * (opt-in via {@code -Dmoreminecrafts.sky.bbmodel.capture=true}).
 */
public final class SkyMobBbModelCapture implements FabricClientGameTest {
    private static final int Y = 100;
    private static final int SPACING = 12;
    private static final List<EntityType<? extends Mob>> TYPES = List.of(
            SkyNormalMobs.CRYSTAL_BUG,
            SkyNormalMobs.SKYGUARD_ARCHER,
            SkyNormalMobs.FALLEN_SENTINEL
    );
    private static final List<String> NAMES = List.of("crystal-bug", "skyguard-archer", "fallen-sentinel");
    private static final float[] ANGLES = {0, 40, 90, 180};
    private static final String[] ANGLE_NAMES = {"front", "three-quarter", "side", "back"};

    private record Shot(String name, double x, double z, float yaw, float pitch, int mobIndex, float mobYaw) {}

    @Override
    public void runTest(ClientGameTestContext context) {
        if (!Boolean.getBoolean("moreminecrafts.sky.bbmodel.capture")) return;
        Properties properties = new Properties();
        properties.setProperty("online-mode", "false");
        properties.setProperty("difficulty", "easy");
        try (TestDedicatedServerContext server = context.worldBuilder().createServer(properties);
             TestDedicatedServerConnection connection = server.connect()) {
            context.waitFor(c -> c.player != null && c.level != null);
            server.runOnServer(s -> buildRoomAndMobs(s.overworld()));
            context.runOnClient(c -> { if (!c.gui.hud.isHidden()) c.gui.hud.toggle(); });

            List<Shot> shots = new ArrayList<>();
            for (int i = 0; i < TYPES.size(); i++) {
                double mx = i * SPACING + 0.5;
                for (int a = 0; a < ANGLES.length; a++) {
                    shots.add(new Shot(NAMES.get(i) + "-" + ANGLE_NAMES[a], mx, 4.2, 180, 10, i, ANGLES[a]));
                }
            }
            int index = 0;
            for (Shot shot : shots) {
                server.runOnServer(s -> {
                    ServerLevel level = s.overworld();
                    for (Mob mob : level.getEntitiesOfClass(Mob.class, new net.minecraft.world.phys.AABB(-10, Y - 2, -10,
                            TYPES.size() * SPACING + 10, Y + 6, 10))) {
                        float yaw = Math.abs(mob.getX() - shot.x()) < 0.1 ? shot.mobYaw() : 25;
                        mob.setYRot(yaw);
                        mob.setYBodyRot(yaw);
                        mob.setYHeadRot(yaw);
                    }
                    s.getPlayerList().getPlayers().getFirst().teleportTo(level, shot.x(), Y, shot.z(), Set.of(),
                            shot.yaw(), shot.pitch(), true);
                });
                connection.waitForClientboundPackets();
                context.waitTicks(30);
                EntityType<? extends Mob> expected = TYPES.get(shot.mobIndex());
                context.runOnClient(c -> {
                    c.gui.toastManager().clear();
                    boolean found = false;
                    for (var entity : c.level.entitiesForRendering()) {
                        if (entity.getType() == expected && entity instanceof Mob) {
                            found = true;
                        }
                    }
                    if (!found) throw new AssertionError("Client did not receive " + shot.name());
                });
                context.waitTicks(2);
                System.out.println("SKY_BBMODEL_SCREENSHOT " + context.takeScreenshot(
                        "sky-bbmodel-%02d-%s".formatted(index++, shot.name())));
            }
            System.out.println("SKY_BBMODEL_CAPTURE_COMPLETE");
        }
    }

    private static void buildRoomAndMobs(ServerLevel level) {
        int maxX = TYPES.size() * SPACING + 8;
        for (int x = -8; x <= maxX; x++) {
            for (int z = -6; z <= 8; z++) {
                level.setBlockAndUpdate(new BlockPos(x, Y - 1, z), Blocks.SMOOTH_QUARTZ.defaultBlockState());
                for (int y = Y; y <= Y + 5; y++) {
                    boolean wall = x == -8 || x == maxX || z == -6 || z == 8;
                    level.setBlockAndUpdate(new BlockPos(x, y, z),
                            wall ? Blocks.SMOOTH_QUARTZ.defaultBlockState() : Blocks.AIR.defaultBlockState());
                }
                level.setBlockAndUpdate(new BlockPos(x, Y + 6, z), Blocks.SEA_LANTERN.defaultBlockState());
            }
        }
        for (int i = 0; i < TYPES.size(); i++) {
            double x = i * SPACING + 0.5;
            Mob mob = TYPES.get(i).create(level, EntitySpawnReason.COMMAND);
            if (mob == null) continue;
            mob.setPos(x, Y, 0.5);
            if (mob instanceof SkyguardArcher) {
                mob.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BOW));
            }
            mob.setNoAi(true);
            mob.setPersistenceRequired();
            level.addFreshEntity(mob);
        }
        var player = level.getServer().getPlayerList().getPlayers().getFirst();
        player.setGameMode(GameType.SPECTATOR);
        player.teleportTo(level, 0.5, Y, 3.5, Set.of(), 180, 12, true);
    }
}
