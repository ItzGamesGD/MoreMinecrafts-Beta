package dev.itzgamesgd.moreminecrafts.accessory;

import dev.itzgamesgd.moreminecrafts.equipment.EquipmentAttachments;
import dev.itzgamesgd.moreminecrafts.registry.ModDataComponents;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** End-to-end inventory fixture. Input is Fabric Client GameTest input, never OS pointer input. */
@SuppressWarnings("UnstableApiUsage")
public final class AccessoryInventoryClientGameTest implements FabricClientGameTest {
    private static final int ACCESSORY_FIRST_MENU_SLOT = 47;
    private static final int SUPPORT_SLOT = AccessoryCategory.SUPPORT_RECOVERY.ordinal();
    private static final int DEFENSE_SLOT = AccessoryCategory.DEFENSE_SURVIVAL.ordinal();

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestDedicatedServerContext server = context.worldBuilder().createServer();
             TestServerConnection ignored = server.connect()) {
            context.waitFor(client -> client.player != null && client.level != null
                    && client.getConnection() != null && client.getConnection().getConnection().isConnected());

            prepareServerFixture(server);
            context.waitFor(client -> client.player.getAttachedOrElse(
                    EquipmentAttachments.ACCESSORIES, AccessoryLoadout.empty()).occupiedSlots() == 5);
            context.waitTicks(2);

            openInventory(context);
            assertAllFiveSlotsVisibleAndOccupied(context);
            for (int scale : List.of(2, 3, 4)) {
                setScaleAndOpenInventory(context, scale);
                assertFiveSlotBounds(context);
                context.takeScreenshot("accessory-inventory-scale-" + scale + "-recipe-book-closed");
            }

            InventoryScreen screen = currentInventory(context);
            Object recipeToggle = findRecipeToggle(screen);
            double[] recipeTogglePos = widgetCenter(recipeToggle);
            context.getInput().setCursorPos(recipeTogglePos[0], recipeTogglePos[1]);
            context.getInput().pressMouse(0);
            context.waitTicks(2);
            if (!recipeBookVisible(screen)) {
                throw new AssertionError("Recipe book did not open after Client GameTest cursor click");
            }
            context.takeScreenshot("accessory-inventory-recipe-book-open-all-five");

            screen = currentInventory(context);
            recipeToggle = findRecipeToggle(screen);
            recipeTogglePos = widgetCenter(recipeToggle);
            context.getInput().setCursorPos(recipeTogglePos[0], recipeTogglePos[1]);
            context.getInput().pressMouse(0);
            context.waitTicks(2);
            if (recipeBookVisible(screen)) throw new AssertionError("Recipe book did not close after Client GameTest cursor click");
            setScaleAndOpenInventory(context, 4);
            assertFiveSlotBounds(context);
            context.takeScreenshot("accessory-inventory-all-five-gui-scale-4");

            runCursorEquipUnequip(context, server);
            runShiftClickTransfer(context, server);
            runInvalidPlacement(context, server);

            context.runOnClient(client -> client.gui.setScreen(null));
            context.waitForScreen(null);
        }
    }

    private static void prepareServerFixture(TestDedicatedServerContext server) {
        server.runOnServer(minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                player.getInventory().setItem(slot, ItemStack.EMPTY);
            }
            player.setAttached(EquipmentAttachments.ACCESSORIES, new AccessoryLoadout(List.of(
                    named(AccessoryDefinition.STAR_COVENANT, "client-qa-combat", 7),
                    accessory(AccessoryDefinition.EMBER_RING),
                    accessory(AccessoryDefinition.SEED_OF_LIFE),
                    accessory(AccessoryDefinition.SNOW_CHARM),
                    accessory(AccessoryDefinition.WHISPER_OF_THE_SEA))));
            player.getInventory().setItem(9, accessory(AccessoryDefinition.SEED_OF_LIFE));
            player.getInventory().setItem(10, accessory(AccessoryDefinition.STAR_COVENANT));
            player.getInventory().setItem(11, accessory(AccessoryDefinition.RING_OF_LIFE));
            player.inventoryMenu.broadcastChanges();
        });
    }

    private static ItemStack accessory(AccessoryDefinition definition) {
        return new ItemStack(ModItems.ACCESSORIES.get(definition));
    }

    private static ItemStack named(AccessoryDefinition definition, String marker, int level) {
        ItemStack stack = accessory(definition);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal(marker));
        stack.set(ModDataComponents.ENHANCEMENT_LEVEL, level);
        return stack;
    }

    private static void openInventory(ClientGameTestContext context) {
        context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
        context.waitForScreen(InventoryScreen.class);
        context.waitTicks(2);
    }

    private static void setScaleAndOpenInventory(ClientGameTestContext context, int scale) {
        context.runOnClient(client -> {
            client.gui.setScreen(null);
            client.options.guiScale().set(scale);
        });
        context.waitTick();
        openInventory(context);
    }

    private static InventoryScreen currentInventory(ClientGameTestContext context) {
        return context.computeOnClient(client -> {
            if (!(client.gui.screen() instanceof InventoryScreen inventory)) {
                throw new AssertionError("Expected InventoryScreen, saw " + client.gui.screen());
            }
            return inventory;
        });
    }

    private static void assertAllFiveSlotsVisibleAndOccupied(ClientGameTestContext context) {
        context.runOnClient(client -> {
            InventoryScreen screen = (InventoryScreen) client.gui.screen();
            var menu = client.player.inventoryMenu;
            if (menu.slots.size() < ACCESSORY_FIRST_MENU_SLOT + AccessoryLoadout.SIZE) {
                throw new AssertionError("Vanilla inventory menu does not contain all five accessory slots");
            }
            for (int index = 0; index < AccessoryLoadout.SIZE; index++) {
                Slot slot = menu.getSlot(ACCESSORY_FIRST_MENU_SLOT + index);
                if (slot.getItem().isEmpty()) throw new AssertionError("Accessory UI slot " + index + " is empty");
                if (slot.getItem().getCount() != 1) throw new AssertionError("Accessory UI slot " + index + " has a non-unit stack");
                if (slot.x < 0 || slot.y < 0) throw new AssertionError("Accessory slot " + index + " has negative screen coordinates");
            }
            AccessoryLoadout clientLoadout = client.player.getAttachedOrElse(
                    EquipmentAttachments.ACCESSORIES, AccessoryLoadout.empty());
            if (clientLoadout.occupiedSlots() != AccessoryLoadout.SIZE) {
                throw new AssertionError("Client attachment did not synchronize all five equipped accessories");
            }
            ItemStack synchronizedStack = clientLoadout.get(AccessoryCategory.COMBAT.ordinal());
            if (!Component.literal("client-qa-combat").equals(synchronizedStack.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME))
                    || !Integer.valueOf(7).equals(synchronizedStack.get(ModDataComponents.ENHANCEMENT_LEVEL))) {
                throw new AssertionError("Synced stack components differ from the authoritative server stack");
            }
        });
    }

    private static void assertFiveSlotBounds(ClientGameTestContext context) {
        context.runOnClient(client -> {
            InventoryScreen screen = (InventoryScreen) client.gui.screen();
            int left = intField(screen, "leftPos");
            int top = intField(screen, "topPos");
            int width = intField(screen, "width");
            int height = intField(screen, "height");
            for (int index = 0; index < AccessoryLoadout.SIZE; index++) {
                Slot slot = client.player.inventoryMenu.getSlot(ACCESSORY_FIRST_MENU_SLOT + index);
                if (left + slot.x < 0 || left + slot.x + 16 > width
                        || top + slot.y < 0 || top + slot.y + 16 > height) {
                    throw new AssertionError("Accessory slot " + index + " clips at GUI scale "
                            + client.getWindow().getGuiScale() + ": left=" + left + " top=" + top
                            + " gui=" + width + "x" + height + " slot=" + slot.x + "," + slot.y);
                }
            }
        });
    }

    private static void runCursorEquipUnequip(ClientGameTestContext context, TestDedicatedServerContext server) {
        server.runOnServer(minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            player.setAttached(EquipmentAttachments.ACCESSORIES,
                    player.getAttachedOrElse(EquipmentAttachments.ACCESSORIES, AccessoryLoadout.empty())
                            .with(SUPPORT_SLOT, ItemStack.EMPTY));
            player.inventoryMenu.broadcastChanges();
        });
        context.waitTicks(2);
        openInventory(context);

        clickMenuSlot(context, 9);
        clickMenuSlot(context, ACCESSORY_FIRST_MENU_SLOT + SUPPORT_SLOT);
        context.waitFor(client -> !client.player.getAttachedOrElse(
                EquipmentAttachments.ACCESSORIES, AccessoryLoadout.empty()).get(SUPPORT_SLOT).isEmpty());
        context.runOnClient(client -> {
            ItemStack equipped = client.player.getAttachedOrElse(
                    EquipmentAttachments.ACCESSORIES, AccessoryLoadout.empty()).get(SUPPORT_SLOT);
            if (equipped.getItem() != ModItems.ACCESSORIES.get(AccessoryDefinition.SEED_OF_LIFE)) {
                throw new AssertionError("Cursor click failed to equip valid support accessory");
            }
            if (!Component.literal("client-qa-combat").equals(client.player.getAttachedOrElse(
                    EquipmentAttachments.ACCESSORIES, AccessoryLoadout.empty()).get(0)
                    .get(net.minecraft.core.component.DataComponents.CUSTOM_NAME))) {
                throw new AssertionError("Unrelated equipped stack metadata changed during equip");
            }
        });
        assertServerConservation(server, AccessoryDefinition.SEED_OF_LIFE, 1);

        clickMenuSlot(context, ACCESSORY_FIRST_MENU_SLOT + SUPPORT_SLOT);
        clickMenuSlot(context, 9);
        context.waitTicks(2);
        context.runOnClient(client -> {
            if (!client.player.getAttachedOrElse(
                    EquipmentAttachments.ACCESSORIES, AccessoryLoadout.empty()).get(SUPPORT_SLOT).isEmpty()) {
                throw new AssertionError("Cursor click failed to unequip support accessory");
            }
            if (count(client.player.getInventory(), ModItems.ACCESSORIES.get(AccessoryDefinition.SEED_OF_LIFE)) != 1) {
                throw new AssertionError("Unequipped accessory did not return to its original inventory slot");
            }
        });
        assertServerConservation(server, AccessoryDefinition.SEED_OF_LIFE, 1);
    }

    private static void runShiftClickTransfer(ClientGameTestContext context, TestDedicatedServerContext server) {
        server.runOnServer(minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            player.setAttached(EquipmentAttachments.ACCESSORIES,
                    player.getAttachedOrElse(EquipmentAttachments.ACCESSORIES, AccessoryLoadout.empty())
                            .with(DEFENSE_SLOT, ItemStack.EMPTY));
            player.inventoryMenu.broadcastChanges();
        });
        context.waitTicks(2);
        openInventory(context);

        context.getInput().holdShift();
        clickMenuSlot(context, 11);
        context.getInput().releaseShift();
        context.waitFor(client -> !client.player.getAttachedOrElse(
                EquipmentAttachments.ACCESSORIES, AccessoryLoadout.empty()).get(DEFENSE_SLOT).isEmpty());
        assertServerConservation(server, AccessoryDefinition.RING_OF_LIFE, 1);

        context.getInput().holdShift();
        clickMenuSlot(context, ACCESSORY_FIRST_MENU_SLOT + DEFENSE_SLOT);
        context.getInput().releaseShift();
        context.waitTicks(2);
        context.runOnClient(client -> {
            if (!client.player.getAttachedOrElse(
                    EquipmentAttachments.ACCESSORIES, AccessoryLoadout.empty()).get(DEFENSE_SLOT).isEmpty()) {
                throw new AssertionError("Shift-click failed to unequip a valid accessory");
            }
            if (count(client.player.getInventory(), ModItems.ACCESSORIES.get(AccessoryDefinition.RING_OF_LIFE)) != 1) {
                throw new AssertionError("Shift-click unequip did not conserve the accessory");
            }
        });
        assertServerConservation(server, AccessoryDefinition.RING_OF_LIFE, 1);
    }

    private static void runInvalidPlacement(ClientGameTestContext context, TestDedicatedServerContext server) {
        openInventory(context);
        clickMenuSlot(context, 10);
        clickMenuSlot(context, ACCESSORY_FIRST_MENU_SLOT + SUPPORT_SLOT);
        context.waitTicks(2);
        context.runOnClient(client -> {
            var loadout = client.player.getAttachedOrElse(EquipmentAttachments.ACCESSORIES, AccessoryLoadout.empty());
            if (!loadout.get(SUPPORT_SLOT).isEmpty()) {
                throw new AssertionError("Wrong-category combat accessory entered support slot");
            }
            if (client.player.containerMenu.getCarried().getItem()
                    != ModItems.ACCESSORIES.get(AccessoryDefinition.STAR_COVENANT)) {
                throw new AssertionError("Rejected placement did not retain the invalid item on the cursor");
            }
        });
        assertServerConservation(server, AccessoryDefinition.STAR_COVENANT, 1);
        context.takeScreenshot("accessory-inventory-invalid-category-placement");
    }

    private static void clickMenuSlot(ClientGameTestContext context, int menuSlot) {
        InventoryScreen screen = currentInventory(context);
        double[] position = context.computeOnClient(client -> {
            Slot slot = client.player.containerMenu.getSlot(menuSlot);
            int left = intField(screen, "leftPos");
            int top = intField(screen, "topPos");
            return new double[]{left + slot.x + 8.0, top + slot.y + 8.0};
        });
        context.getInput().setCursorPos(position[0], position[1]);
        context.getInput().pressMouse(0);
    }

    private static void assertServerConservation(TestDedicatedServerContext server, AccessoryDefinition definition, int expected) {
        server.computeOnServer(minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            int inventoryCount = count(player.getInventory(), ModItems.ACCESSORIES.get(definition));
            int equippedCount = player.getAttachedOrElse(EquipmentAttachments.ACCESSORIES, AccessoryLoadout.empty())
                    .stacks().stream().filter(stack -> stack.getItem() == ModItems.ACCESSORIES.get(definition))
                    .mapToInt(ItemStack::getCount).sum();
            int carriedCount = player.containerMenu.getCarried().getItem() == ModItems.ACCESSORIES.get(definition)
                    ? player.containerMenu.getCarried().getCount() : 0;
            if (inventoryCount + equippedCount + carriedCount != expected) {
                throw new AssertionError(definition + " conservation failed: inventory=" + inventoryCount
                        + " equipped=" + equippedCount + " cursor=" + carriedCount + " expected=" + expected);
            }
            return null;
        });
    }

    private static int count(net.minecraft.world.entity.player.Inventory inventory, net.minecraft.world.item.Item item) {
        int count = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.getItem() == item) count += stack.getCount();
        }
        return count;
    }

    private static Object findRecipeToggle(InventoryScreen screen) {
        Object found = findObject(screen, value -> value.getClass().getSimpleName().contains("ToggleButton"),
                Collections.newSetFromMap(new IdentityHashMap<>()), 0);
        if (found == null) throw new AssertionError("Inventory recipe-book toggle widget was not found");
        return found;
    }

    private static boolean recipeBookVisible(InventoryScreen screen) {
        Object component = findObject(screen, value -> value.getClass().getSimpleName().contains("RecipeBookComponent"),
                Collections.newSetFromMap(new IdentityHashMap<>()), 0);
        if (component == null) throw new AssertionError("Inventory recipe-book component was not found");
        for (Class<?> type = component.getClass(); type != null; type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                if (method.getParameterCount() == 0 && method.getReturnType() == boolean.class
                        && method.getName().toLowerCase().contains("visible")) {
                    try {
                        method.setAccessible(true);
                        return (boolean) method.invoke(component);
                    } catch (ReflectiveOperationException error) {
                        throw new AssertionError("Could not inspect recipe-book visibility", error);
                    }
                }
            }
            for (Field field : type.getDeclaredFields()) {
                if (field.getType() == boolean.class && field.getName().toLowerCase().contains("visible")) {
                    try {
                        field.setAccessible(true);
                        return field.getBoolean(component);
                    } catch (ReflectiveOperationException error) {
                        throw new AssertionError("Could not inspect recipe-book visibility", error);
                    }
                }
            }
        }
        throw new AssertionError("Recipe-book visibility member was not found");
    }

    private static Object findObject(Object root, java.util.function.Predicate<Object> match, Set<Object> visited, int depth) {
        if (root == null || depth > 6 || !visited.add(root)) return null;
        if (match.test(root)) return root;
        if (root instanceof Iterable<?> values) {
            for (Object value : values) {
                Object found = findObject(value, match, visited, depth + 1);
                if (found != null) return found;
            }
        }
        for (Class<?> type = root.getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()
                        || field.getType().isEnum() || field.getType() == String.class) continue;
                try {
                    field.setAccessible(true);
                    Object found = findObject(field.get(root), match, visited, depth + 1);
                    if (found != null) return found;
                } catch (RuntimeException | IllegalAccessException ignored) {
                    // Skip inaccessible implementation details; the screen's own widget tree is also inspected.
                }
            }
        }
        return null;
    }

    private static double[] widgetCenter(Object widget) {
        try {
            Method getX = widget.getClass().getMethod("getX");
            Method getY = widget.getClass().getMethod("getY");
            return new double[]{((Number) getX.invoke(widget)).doubleValue() + 4.0,
                    ((Number) getY.invoke(widget)).doubleValue() + 4.0};
        } catch (ReflectiveOperationException error) {
            throw new AssertionError("Could not read recipe-book toggle position", error);
        }
    }

    private static int intField(Object instance, String name) {
        for (Class<?> type = instance.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.getInt(instance);
            } catch (NoSuchFieldException ignored) {
                // Search the parent type.
            } catch (ReflectiveOperationException error) {
                throw new AssertionError("Could not read screen field " + name, error);
            }
        }
        throw new AssertionError("Screen field not found: " + name);
    }
}
