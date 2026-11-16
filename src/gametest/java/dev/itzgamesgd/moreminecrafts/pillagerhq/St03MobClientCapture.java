package dev.itzgamesgd.moreminecrafts.pillagerhq;

import dev.itzgamesgd.moreminecrafts.mob.CustomMobCatalog;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/**
 * Real-client screenshots of the four ST-03 Pillager Headquarters variants
 * (Pillager Commander, Pillager Beastmaster, Armory Guard, Pillager Drillmaster).
 * Captures front, three-quarter, and back views for each variant, plus one lineup screenshot.
 */
public final class St03MobClientCapture implements FabricClientGameTest {
    private static final int Y = 100;
    private static final int SPACING = 10;
    private static final List<EntityType<? extends Mob>> TYPES = List.of(
            CustomMobCatalog.PILLAGER_COMMANDER,
            CustomMobCatalog.PILLAGER_BEASTMASTER,
            CustomMobCatalog.ARMORY_GUARD,
            CustomMobCatalog.PILLAGER_DRILLMASTER
    );
    private static final List<String> NAMES = List.of(
            "pillager-commander",
            "pillager-beastmaster",
            "armory-guard",
            "pillager-drillmaster"
    );

    private record Shot(String name, double x, double z, float yaw, float pitch, int mobIndex, float mobYaw) {}

    @Override
    public void runTest(ClientGameTestContext context) {
        if (!Boolean.getBoolean("moreminecrafts.st03.mob.capture")) {
            return;
        }
        Properties properties = new Properties();
        properties.setProperty("online-mode", "false");
        properties.setProperty("difficulty", "easy");
        try (TestDedicatedServerContext server = context.worldBuilder().createServer(properties);
             TestDedicatedServerConnection connection = server.connect()) {
            context.waitFor(c -> c.player != null && c.level != null);
            server.runOnServer(s -> buildRoomAndMobs(s.overworld()));
            context.runOnClient(c -> {
                if (!c.gui.hud.isHidden()) {
                    c.gui.hud.toggle();
                }
            });

            List<Shot> shots = new ArrayList<>();
            for (int i = 0; i < TYPES.size(); i++) {
                double mx = i * SPACING + 0.5;
                shots.add(new Shot(NAMES.get(i) + "-front", mx, 3.2, 180, 10, i, 0));
                shots.add(new Shot(NAMES.get(i) + "-three-quarter", mx, 3.2, 180, 10, i, 40));
                shots.add(new Shot(NAMES.get(i) + "-back", mx, 3.2, 180, 10, i, 180));
            }

            int index = 0;
            for (Shot shot : shots) {
                server.runOnServer(s -> {
                    ServerLevel level = s.overworld();
                    for (Mob mob : level.getEntitiesOfClass(Mob.class, new net.minecraft.world.phys.AABB(
                            -10, Y - 2, -10, TYPES.size() * SPACING + 10, Y + 6, 10))) {
                        float yaw = Math.abs(mob.getX() - shot.x()) < 0.1 ? shot.mobYaw() : 25;
                        mob.setYRot(yaw);
                        mob.setYBodyRot(yaw);
                        mob.setYHeadRot(yaw);
                    }
                    s.getPlayerList().getPlayers().getFirst().teleportTo(
                            level, shot.x(), Y, shot.z(), Set.of(), shot.yaw(), shot.pitch(), true);
                });
                connection.waitForClientboundPackets();
                context.waitTicks(30);
                EntityType<? extends Mob> expected = TYPES.get(shot.mobIndex());
                context.runOnClient(c -> {
                    c.gui.toastManager().clear();
                    boolean found = false;
                    for (var entity : c.level.entitiesForRendering()) {
                        if (entity.getType() == expected) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        throw new AssertionError("Client did not receive " + shot.name());
                    }
                });
                context.waitTicks(2);
                System.out.println("ST03_MOB_SCREENSHOT " + context.takeScreenshot(
                        "st03-variant-%02d-%s".formatted(index++, shot.name())));
            }

            // Lineup of all 4 variants
            server.runOnServer(s -> {
                ServerLevel level = s.overworld();
                for (Mob mob : level.getEntitiesOfClass(Mob.class, new net.minecraft.world.phys.AABB(
                        -10, Y - 2, -10, TYPES.size() * SPACING + 10, Y + 6, 10))) {
                    mob.setYRot(15);
                    mob.setYBodyRot(15);
                    mob.setYHeadRot(15);
                }
                s.getPlayerList().getPlayers().getFirst().teleportTo(
                        level, 1.5 * SPACING + 0.5, Y + 1, 16, Set.of(), 180, 8, true);
            });
            connection.waitForClientboundPackets();
            context.waitTicks(40);
            context.runOnClient(c -> c.gui.toastManager().clear());
            System.out.println("ST03_MOB_SCREENSHOT " + context.takeScreenshot(
                    "st03-variant-%02d-lineup".formatted(index)));
            System.out.println("ST03_MOB_CAPTURE_COMPLETE " + (index + 1));
        }
    }

    private static void buildRoomAndMobs(ServerLevel level) {
        int maxX = TYPES.size() * SPACING + 2;
        for (int x = -6; x <= maxX; x++) {
            for (int z = -4; z <= 26; z++) {
                level.setBlock(new BlockPos(x, Y - 1, z), Blocks.POLISHED_DEEPSLATE.defaultBlockState(), 3);
                level.setBlock(new BlockPos(x, Y + 7, z), Blocks.GLOWSTONE.defaultBlockState(), 3);
                for (int y = Y; y < Y + 7; y++) {
                    level.setBlock(new BlockPos(x, y, z), z == -4
                            ? Blocks.DEEPSLATE_BRICKS.defaultBlockState()
                            : Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        var player = level.getServer().getPlayerList().getPlayers().getFirst();
        player.setGameMode(GameType.SPECTATOR);

        for (int i = 0; i < TYPES.size(); i++) {
            spawn(level, TYPES.get(i), i * SPACING + 0.5, 0.5);
        }
    }

    private static void spawn(ServerLevel level, EntityType<? extends Mob> type, double x, double z) {
        Mob mob = type.create(level, EntitySpawnReason.COMMAND);
        if (mob == null) {
            throw new AssertionError("Could not create " + type);
        }
        mob.setNoAi(true);
        mob.setPersistenceRequired();

        if (type == CustomMobCatalog.PILLAGER_COMMANDER) {
            ItemStack axe = new ItemStack(Items.IRON_AXE);
            var unbreaking = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(Enchantments.UNBREAKING);
            axe.enchant(unbreaking, 1);
            mob.setItemSlot(EquipmentSlot.MAINHAND, axe);
        } else if (type == CustomMobCatalog.PILLAGER_BEASTMASTER) {
            mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
        } else if (type == CustomMobCatalog.ARMORY_GUARD) {
            mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            mob.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        } else if (type == CustomMobCatalog.PILLAGER_DRILLMASTER) {
            ItemStack crossbow = new ItemStack(Items.CROSSBOW);
            var quickCharge = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(Enchantments.QUICK_CHARGE);
            crossbow.enchant(quickCharge, 1);
            mob.setItemSlot(EquipmentSlot.MAINHAND, crossbow);
        }

        mob.snapTo(x, Y, z, 0, 0);
        if (!level.addFreshEntity(mob)) {
            throw new AssertionError("Could not add " + type);
        }
    }
}
