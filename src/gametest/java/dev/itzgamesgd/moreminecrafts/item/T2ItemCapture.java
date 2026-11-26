package dev.itzgamesgd.moreminecrafts.item;

import java.util.List;
import java.util.Properties;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/**
 * Real-client screenshots of Tier 2 weapon item models held by skeletons (opt-in via
 * {@code -Dmoreminecrafts.t2.capture=true}). Each model id is shown on its own skeleton in an aggressive (aim) pose.
 */
public final class T2ItemCapture implements FabricClientGameTest {
    private static final int Y = 100;
    private static final int SPACING = 10;
    private static final List<String> MODELS = List.of("wind_sword", "wind_sword", "wind_archers_bow_pose2", "flowing_water_sword");
    private static final List<String> FIRST_PERSON_MODELS = List.of("wind_sword", "wind_archers_bow_pose2", "vanilla_bow_reference", "flowing_water_sword", "vanilla_sword_reference");
    private static final float[] ANGLES = {40, 0};
    private static final String[] ANGLE_NAMES = {"three-quarter", "front"};

    @Override
    public void runTest(ClientGameTestContext context) {
        if (!Boolean.getBoolean("moreminecrafts.t2.capture")) return;
        Properties properties = new Properties();
        properties.setProperty("online-mode", "false");
        properties.setProperty("difficulty", "easy");
        try (TestDedicatedServerContext server = context.worldBuilder().createServer(properties);
             TestDedicatedServerConnection connection = server.connect()) {
            context.waitFor(c -> c.player != null && c.level != null);
            server.runOnServer(s -> buildRoom(s.overworld()));
            context.runOnClient(c -> { if (!c.gui.hud.isHidden()) c.gui.hud.toggle(); });
            int index = 0;
            for (int i = 0; i < MODELS.size(); i++) {
                double mx = i * SPACING + 0.5;
                for (int a = 0; a < ANGLES.length; a++) {
                    float mobYaw = ANGLES[a];
                    String shotName = MODELS.get(i) + "-" + ANGLE_NAMES[a];
                    server.runOnServer(s -> {
                        ServerLevel level = s.overworld();
                        for (Mob mob : level.getEntitiesOfClass(Mob.class, new net.minecraft.world.phys.AABB(-10, Y - 2, -10,
                                MODELS.size() * SPACING + 10, Y + 6, 10))) {
                            float yaw = Math.abs(mob.getX() - mx) < 0.1 ? mobYaw : 25;
                            mob.setYRot(yaw);
                            mob.setYBodyRot(yaw);
                            mob.setYHeadRot(yaw);
                        }
                        s.getPlayerList().getPlayers().getFirst().teleportTo(level, mx, Y, 3.2, Set.of(), 180, 10, true);
                    });
                    connection.waitForClientboundPackets();
                    context.waitTicks(30);
                    context.runOnClient(c -> c.gui.toastManager().clear());
                    context.waitTicks(2);
                    System.out.println("T2_SCREENSHOT " + context.takeScreenshot("t2-capture-%02d-%s".formatted(index++, shotName)));
                }
            }
            // First-person: the local player holds the item in survival with the HUD (and hand) visible.
            context.runOnClient(c -> { if (c.gui.hud.isHidden()) c.gui.hud.toggle(); });
            for (String model : FIRST_PERSON_MODELS) {
                ItemStack stack = heldStack(model);
                server.runOnServer(s -> {
                    var player = s.getPlayerList().getPlayers().getFirst();
                    player.setGameMode(GameType.SURVIVAL);
                    player.setItemSlot(EquipmentSlot.MAINHAND, stack.copy());
                    player.teleportTo(s.overworld(), 0.5, Y, 3.2, Set.of(), 180, 0, true);
                });
                connection.waitForClientboundPackets();
                context.waitTicks(30);
                context.runOnClient(c -> c.gui.toastManager().clear());
                context.waitTicks(2);
                System.out.println("T2_SCREENSHOT " + context.takeScreenshot("t2-capture-%02d-%s-firstperson".formatted(index++, model)));
            }
            System.out.println("T2_CAPTURE_COMPLETE");
        }
    }

    private static ItemStack heldStack(String model) {
        boolean sword = model.contains("sword");
        ItemStack stack = new ItemStack(sword ? Items.IRON_SWORD : Items.BOW);
        if (!model.startsWith("vanilla")) {
            stack.set(DataComponents.ITEM_MODEL, Identifier.parse("moreminecrafts:" + model));
        }
        return stack;
    }

    private static void buildRoom(ServerLevel level) {
        int maxX = MODELS.size() * SPACING + 8;
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
        for (int i = 0; i < MODELS.size(); i++) {
            Mob mob = EntityTypes.SKELETON.create(level, EntitySpawnReason.COMMAND);
            if (mob == null) continue;
            mob.setPos(i * SPACING + 0.5, Y, 0.5);
            mob.setItemSlot(EquipmentSlot.MAINHAND, heldStack(MODELS.get(i)));
            mob.setAggressive(true);
            mob.setNoAi(true);
            mob.setPersistenceRequired();
            level.addFreshEntity(mob);
        }
        var player = level.getServer().getPlayerList().getPlayers().getFirst();
        player.setGameMode(GameType.SPECTATOR);
        player.teleportTo(level, 0.5, Y, 3.2, Set.of(), 180, 10, true);
    }
}
