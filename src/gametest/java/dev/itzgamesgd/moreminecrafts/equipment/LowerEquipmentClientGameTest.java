package dev.itzgamesgd.moreminecrafts.equipment;

import com.mojang.blaze3d.platform.InputConstants;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import java.lang.reflect.Field;
import java.util.Map;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.RecipeBookType;

/** Real client, server attachment sync, screen input, and the installed LambDynamicLights engine. */
public final class LowerEquipmentClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        require(FabricLoader.getInstance().isModLoaded("lambdynlights"), "LambDynamicLights runtime must be installed");
        try (var world = context.worldBuilder().create()) {
            var connection = world.getConnection();
            connection.waitForChunksRender();
            world.getServer().runCommand("gamemode survival @p");
            world.getServer().runOnServer(server -> {
                var player = connection.getServerPlayer();
                player.inventoryMenu.setCarried(new ItemStack(ModItems.MINER_LANTERN));
                player.inventoryMenu.broadcastChanges();
            });
            connection.waitForClientboundPackets();
            context.waitFor(client -> client.player.inventoryMenu.getCarried().is(ModItems.MINER_LANTERN));
            context.runOnClient(client -> client.player.getRecipeBook().setOpen(RecipeBookType.CRAFTING, false));
            context.setScreen(() -> new InventoryScreen(connection.getClientPlayer()));
            context.waitForScreen(InventoryScreen.class);
            var click = context.computeOnClient(client -> {
                var screen = (InventoryScreen) client.gui.screen();
                var socket = client.player.inventoryMenu.getSlot(46);
                var helmet = client.player.inventoryMenu.getSlot(5);
                require(socket.x == helmet.x - 18 && socket.y == helmet.y, "Client socket must be LEFT of helmet");
                var outside = AbstractRecipeBookScreen.class.getDeclaredMethod("hasClickedOutside",
                        double.class, double.class, int.class, int.class);
                outside.setAccessible(true);
                int left = intField(screen, "leftPos"), top = intField(screen, "topPos");
                require(!(boolean) outside.invoke(screen, left + socket.x + 8.0D, top + socket.y + 8.0D, left, top),
                        "Closed recipe book must not treat the socket as an outside/drop click");
                var window = client.getWindow();
                return new double[]{(intField(screen, "leftPos") + socket.x + 8.0D)
                        * window.getScreenWidth() / window.getGuiScaledWidth(),
                        (intField(screen, "topPos") + socket.y + 8.0D)
                        * window.getScreenHeight() / window.getGuiScaledHeight()};
            });
            context.getInput().setCursorPos(click[0], click[1]);
            context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
            context.waitFor(client -> client.player.getAttachedOrElse(EquipmentAttachments.LANTERN, ItemStack.EMPTY)
                    .is(ModItems.MINER_LANTERN));
            world.getServer().waitFor(server ->
                    connection.getServerPlayer().getAttachedOrElse(EquipmentAttachments.LANTERN, ItemStack.EMPTY)
                            .is(ModItems.MINER_LANTERN));
            connection.waitForClientboundPackets();
            context.runOnClient(client -> require(client.player.inventoryMenu.getCarried().isEmpty(),
                    "Actual screen click must equip without retaining or duplicating the cursor lantern"));
            context.takeScreenshot("lower-equipment-lantern-socket");
            context.setScreen(() -> null);
            context.waitTicks(5);
            var previousHead = context.computeOnClient(client -> {
                Object bridge = FabricLoader.getInstance().getEntrypoints("lambdynlights:initializer",
                        Class.forName("dev.lambdaurora.lambdynlights.api.DynamicLightsInitializer"))
                        .stream().filter(value -> value.getClass().getName().endsWith("LanternDynamicLightBridge"))
                        .findFirst().orElseThrow();
                require(field(bridge, "manager") != null, "LambDynamicLights must invoke our initializer");
                var active = (Map<?, ?>) field(bridge, "active");
                Object behavior = active.get(client.player.getUUID());
                require(behavior != null, "Equipped player must register a real dynamic-light behavior");
                BlockPos head = BlockPos.containing(client.player.getX(),
                        client.player.getY() + client.player.getBbHeight() * 0.85D, client.player.getZ());
                var lightAtPos = behavior.getClass().getDeclaredMethod("lightAtPos", BlockPos.class, double.class);
                lightAtPos.setAccessible(true);
                require((double) lightAtPos.invoke(behavior, head, 0.0D) == 15.0D, "Lantern emission must be level 15");
                require(dynamicLight(head) > 13.0D, "Installed engine must illuminate the equipped player's head");
                return head;
            });
            world.getServer().runCommand("execute at @p run tp @p ~32 ~ ~");
            connection.waitForClientboundPackets();
            context.waitFor(client -> Math.abs(client.player.getX() - previousHead.getX()) > 25.0D);
            context.waitTicks(5);
            context.runOnClient(client -> {
                BlockPos head = BlockPos.containing(client.player.getX(),
                        client.player.getY() + client.player.getBbHeight() * 0.85D, client.player.getZ());
                require(dynamicLight(head) > 13.0D, "Dynamic light must follow the player");
                require(dynamicLight(previousHead) == 0.0D, "Old position must not retain lantern light");
            });
            world.getServer().runOnServer(server ->
                    connection.getServerPlayer().setAttached(EquipmentAttachments.LANTERN, ItemStack.EMPTY));
            connection.waitForClientboundPackets();
            context.waitFor(client -> client.player.getAttachedOrElse(EquipmentAttachments.LANTERN, ItemStack.EMPTY).isEmpty());
            context.waitTicks(5);
            context.runOnClient(client -> require(dynamicLight(client.player.blockPosition()) == 0.0D,
                    "Removing the Lantern must remove dynamic illumination"));
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Failed to inspect real screen/light integration", exception);
        }
    }

    private static double dynamicLight(BlockPos pos) throws ReflectiveOperationException {
        var type = Class.forName("dev.lambdaurora.lambdynlights.LambDynLights");
        return (double) type.getMethod("getDynamicLightLevel", BlockPos.class).invoke(type.getField("INSTANCE").get(null), pos);
    }

    private static Object field(Object owner, String name) throws ReflectiveOperationException {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(owner);
    }

    private static int intField(Object screen, String name) throws ReflectiveOperationException {
        Field field = AbstractContainerScreen.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.getInt(screen);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
