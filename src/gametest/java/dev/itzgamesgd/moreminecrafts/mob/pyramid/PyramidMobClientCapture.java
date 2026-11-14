package dev.itzgamesgd.moreminecrafts.mob.pyramid;

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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/**
 * Real-client screenshots of the four pyramid variants for appearance review (opt-in via
 * {@code -Dmoreminecrafts.pyramid.mob.capture=true}). Each adult is shot from the front, three-quarter and back;
 * each zombie-family variant also as a baby; plus one lineup. Test-only lit room; no gameplay is exercised.
 */
public final class PyramidMobClientCapture implements FabricClientGameTest {
    private static final int Y = 100;
    private static final int SPACING = 12;
    private static final List<EntityType<? extends Mob>> TYPES = List.of(PyramidMobs.BURIED_MUMMY,
            PyramidMobs.CRYPT_ARCHER, PyramidMobs.FUNERAL_SERVANT, PyramidMobs.GILDED_MUMMY);
    private static final List<String> NAMES = List.of("buried-mummy", "crypt-archer", "funeral-servant", "gilded-mummy");

    private record Shot(String name, double x, double z, float yaw, float pitch, int mobIndex, boolean baby, float mobYaw) {}

    @Override
    public void runTest(ClientGameTestContext context) {
        if (!Boolean.getBoolean("moreminecrafts.pyramid.mob.capture")) return;
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
                shots.add(new Shot(NAMES.get(i) + "-front", mx, 3.1, 180, 12, i, false, 0));
                shots.add(new Shot(NAMES.get(i) + "-three-quarter", mx, 3.1, 180, 12, i, false, 40));
                shots.add(new Shot(NAMES.get(i) + "-back", mx, 3.1, 180, 12, i, false, 180));
                if (TYPES.get(i) != PyramidMobs.CRYPT_ARCHER) {
                    shots.add(new Shot(NAMES.get(i) + "-baby", mx + 6, 2.0, 180, 22, i, true, 25));
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
                        if (entity.getType() == expected && entity instanceof Mob mob
                                && (!(mob instanceof Zombie) || mob.isBaby() == shot.baby())) {
                            found = true;
                        }
                    }
                    if (!found) throw new AssertionError("Client did not receive " + shot.name());
                });
                context.waitTicks(2);
                System.out.println("PYRAMID_MOB_SCREENSHOT " + context.takeScreenshot(
                        "pyramid-variant-%02d-%s".formatted(index++, shot.name())));
            }
            // lineup of all adults and babies, seen from further back
            server.runOnServer(s -> {
                ServerLevel level = s.overworld();
                for (Mob mob : level.getEntitiesOfClass(Mob.class, new net.minecraft.world.phys.AABB(-10, Y - 2, -10,
                        TYPES.size() * SPACING + 10, Y + 6, 10))) {
                    mob.setYRot(20);
                    mob.setYBodyRot(20);
                    mob.setYHeadRot(20);
                }
                s.getPlayerList().getPlayers().getFirst().teleportTo(level, 1.5 * SPACING + 3.5, Y + 3, 25,
                        Set.of(), 180, 6, true);
            });
            connection.waitForClientboundPackets();
            context.waitTicks(40);
            context.runOnClient(c -> c.gui.toastManager().clear());
            System.out.println("PYRAMID_MOB_SCREENSHOT " + context.takeScreenshot(
                    "pyramid-variant-%02d-lineup".formatted(index)));
            System.out.println("PYRAMID_MOB_CAPTURE_COMPLETE " + (index + 1));
        }
    }

    private static void buildRoomAndMobs(ServerLevel level) {
        int maxX = TYPES.size() * SPACING + 2;
        for (int x = -6; x <= maxX; x++) {
            for (int z = -4; z <= 28; z++) {
                level.setBlock(new BlockPos(x, Y - 1, z), Blocks.SMOOTH_SANDSTONE.defaultBlockState(), 3);
                level.setBlock(new BlockPos(x, Y + 7, z), Blocks.GLOWSTONE.defaultBlockState(), 3);
                for (int y = Y; y < Y + 7; y++) {
                    level.setBlock(new BlockPos(x, y, z), z == -4
                            ? Blocks.CUT_SANDSTONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        var player = level.getServer().getPlayerList().getPlayers().getFirst();
        player.setGameMode(GameType.SPECTATOR);
        for (int i = 0; i < TYPES.size(); i++) {
            spawn(level, TYPES.get(i), i * SPACING + 0.5, 0.5, false);
            if (TYPES.get(i) != PyramidMobs.CRYPT_ARCHER) {
                spawn(level, TYPES.get(i), i * SPACING + 6.5, 0.5, true);
            }
        }
    }

    private static void spawn(ServerLevel level, EntityType<? extends Mob> type, double x, double z, boolean baby) {
        Mob mob = type.create(level, EntitySpawnReason.COMMAND);
        if (mob == null) throw new AssertionError("Could not create " + type);
        mob.setNoAi(true);
        mob.setPersistenceRequired();
        if (mob instanceof Zombie zombie) zombie.setBaby(baby);
        if (type == PyramidMobs.CRYPT_ARCHER) mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        mob.snapTo(x, Y, z, 0, 0);
        if (!level.addFreshEntity(mob)) throw new AssertionError("Could not add " + type);
    }
}
