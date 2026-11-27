package dev.itzgamesgd.moreminecrafts.item;

import java.util.List;
import java.util.Properties;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/**
 * Real-client screenshots of the utility tools and 2D icons (opt-in via
 * {@code -Dmoreminecrafts.utility.capture=true}). Each model id is shown in the hand of its own mannequin (default idle pose, the item as the player would see it held).
 */
public final class UtilityItemCapture implements FabricClientGameTest {
    private static final int Y = 100;
    private static final int SPACING = 10;
    private static final List<String> MODELS = List.of("mining_drill", "mob_capture_tool", "potion_injector");
    private static final List<String> FIRST_PERSON_MODELS = List.of("mining_drill", "mob_capture_tool", "potion_injector", "vanilla_pickaxe_reference", "flame_core", "parachute");
    private static final List<String> ICON_IDS = List.of("mining_drill", "mob_capture_tool", "potion_injector", "parachute", "gauntlet", "bounty_contract", "special_contract", "captured_mob_egg", "flame_core", "current_core", "wind_core", "earth_core", "thunder_core", "royal_tomb_relic_core", "forge_lord_relic_core", "abyssal_relic_core", "verdant_relic_core", "void_relic_core", "relic_catalyst");
    private static final float[] ANGLES = {40, 0, 90};
    private static final String[] ANGLE_NAMES = {"three-quarter", "front", "side"};

    @Override
    public void runTest(ClientGameTestContext context) {
        if (!Boolean.getBoolean("moreminecrafts.utility.capture")) return;
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
                        for (Mannequin mob : level.getEntitiesOfClass(Mannequin.class, new net.minecraft.world.phys.AABB(-10, Y - 2, -10,
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
                    System.out.println("UTILITY_SCREENSHOT " + context.takeScreenshot("utility-capture-%02d-%s".formatted(index++, shotName)));
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
                System.out.println("UTILITY_SCREENSHOT " + context.takeScreenshot("utility-capture-%02d-%s-firstperson".formatted(index++, model)));
            }
            // Inventory icons: every item in real inventory slots at GUI scale 2 and 4.
            server.runOnServer(s -> {
                var player = s.getPlayerList().getPlayers().getFirst();
                player.setGameMode(GameType.SURVIVAL);
                int slot = 9;
                for (String id : ICON_IDS) {
                    player.getInventory().setItem(slot++, heldStack(id));
                }
            });
            connection.waitForClientboundPackets();
            for (int scale : new int[] {2, 4}) {
                context.runOnClient(c -> { c.gui.setScreen(null); c.options.guiScale().set(scale); });
                context.waitTick();
                context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
                context.waitForScreen(InventoryScreen.class);
                context.waitTicks(10);
                context.getInput().setCursorPos(2, 2);
                context.waitTick();
                System.out.println("UTILITY_SCREENSHOT " + context.takeScreenshot("utility-capture-%02d-inventory-scale-%d".formatted(index++, scale)));
            }
            context.runOnClient(c -> { c.gui.setScreen(null); c.options.guiScale().set(0); });
            context.waitForScreen(null);
            System.out.println("UTILITY_CAPTURE_COMPLETE");
        }
    }

    private static ItemStack heldStack(String model) {
        if (model.startsWith("vanilla")) return new ItemStack(Items.IRON_PICKAXE);
        ItemStack stack = new ItemStack(Items.PAPER);
        stack.set(DataComponents.ITEM_MODEL, Identifier.parse("moreminecrafts:" + model));
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
            var mannequin = EntityTypes.MANNEQUIN.create(level, EntitySpawnReason.COMMAND);
            if (mannequin == null) continue;
            mannequin.setPos(i * SPACING + 0.5, Y, 0.5);
            mannequin.setItemSlot(EquipmentSlot.MAINHAND, heldStack(MODELS.get(i)));
            level.addFreshEntity(mannequin);
        }
        var player = level.getServer().getPlayerList().getPlayers().getFirst();
        player.setGameMode(GameType.SPECTATOR);
        player.teleportTo(level, 0.5, Y, 3.2, Set.of(), 180, 10, true);
    }
}
