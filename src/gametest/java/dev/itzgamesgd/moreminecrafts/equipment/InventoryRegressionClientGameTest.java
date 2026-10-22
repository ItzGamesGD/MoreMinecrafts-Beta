package dev.itzgamesgd.moreminecrafts.equipment;

import dev.itzgamesgd.moreminecrafts.accessory.AccessoryCategory;
import dev.itzgamesgd.moreminecrafts.accessory.AccessoryDefinition;
import dev.itzgamesgd.moreminecrafts.accessory.AccessoryLoadout;
import dev.itzgamesgd.moreminecrafts.registry.ModDataComponents;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;

/** Rendered inventory regressions for vanilla slots, recipes, and the persistent Backpack store. */
@SuppressWarnings("UnstableApiUsage")
public final class InventoryRegressionClientGameTest implements FabricClientGameTest {
    private static final int LEFT_BUTTON = 1;
    private static final int BACKPACK_ITEM_MENU_SLOT = 0;
    private static final int BACKPACK_PLAYER_MAIN_FIRST = 27;

    @Override
    public void runTest(ClientGameTestContext context) {
        try (var server = context.worldBuilder().createServer();
             TestDedicatedServerConnection connection = server.connect()) {
            context.waitFor(client -> client.player != null && client.level != null
                    && client.getConnection() != null && client.getConnection().getConnection().isConnected());
            prepareVanillaFixture(server);
            connection.waitForClientboundPackets();
            exerciseVanillaInventory(context, server, connection);
            exerciseBackpack(context, server, connection);
            context.runOnClient(client -> client.gui.setScreen(null));
            context.waitForScreen(null);
        }
    }

    private static void prepareVanillaFixture(net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext server) {
        server.runOnServer(minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            player.setGameMode(GameType.SURVIVAL);
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                player.getInventory().setItem(slot, ItemStack.EMPTY);
            }
            player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            player.setAttached(EquipmentAttachments.ACCESSORIES, new AccessoryLoadout(java.util.List.of(
                    new ItemStack(ModItems.ACCESSORIES.get(AccessoryDefinition.STAR_COVENANT)),
                    new ItemStack(ModItems.ACCESSORIES.get(AccessoryDefinition.EMBER_RING)),
                    new ItemStack(ModItems.ACCESSORIES.get(AccessoryDefinition.SEED_OF_LIFE)),
                    new ItemStack(ModItems.ACCESSORIES.get(AccessoryDefinition.SNOW_CHARM)),
                    new ItemStack(ModItems.ACCESSORIES.get(AccessoryDefinition.WHISPER_OF_THE_SEA)))));
            player.getInventory().setItem(9, new ItemStack(Items.IRON_CHESTPLATE));
            player.getInventory().setItem(10, new ItemStack(Items.SHIELD));
            for (int slot = 11; slot <= 14; slot++) player.getInventory().setItem(slot, new ItemStack(Items.OAK_PLANKS));
            player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            player.getInventory().setItem(0, ItemStack.EMPTY);
            player.inventoryMenu.broadcastChanges();
        });
    }

    private static void exerciseVanillaInventory(ClientGameTestContext context,
                                                  net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext server,
                                                  TestDedicatedServerConnection connection) {
        context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
        context.waitForScreen(InventoryScreen.class);

        // Vanilla InventoryMenu indices: head 5, chest 6, legs 7, feet 8, offhand 45, accessory slots 47–51.
        clickMenuSlot(context, 9);
        clickMenuSlot(context, 6);
        connection.waitForServerboundPackets();
        connection.waitForClientboundPackets();
        context.runOnClient(client -> {
            if (client.player.inventoryMenu.getSlot(6).getItem().getItem() != Items.IRON_CHESTPLATE) {
                throw new AssertionError("Vanilla chest armor slot did not accept a chestplate");
            }
            assertAccessoryPanel(client.player.inventoryMenu, "after chest armor equip");
        });

        clickMenuSlot(context, 10);
        clickMenuSlot(context, 45);
        connection.waitForServerboundPackets();
        connection.waitForClientboundPackets();
        context.runOnClient(client -> {
            if (client.player.inventoryMenu.getSlot(45).getItem().getItem() != Items.SHIELD) {
                throw new AssertionError("Vanilla offhand slot did not accept a shield");
            }
            assertAccessoryPanel(client.player.inventoryMenu, "after offhand equip");
        });

        for (int plankSlot = 11; plankSlot <= 14; plankSlot++) {
            clickMenuSlot(context, plankSlot);
            clickMenuSlot(context, plankSlot - 10); // 2x2 crafting grid slots 1–4.
        }
        connection.waitForServerboundPackets();
        connection.waitForClientboundPackets();
        context.waitFor(client -> client.player.inventoryMenu.getSlot(0).getItem().is(Items.CRAFTING_TABLE));
        context.runOnClient(client -> assertAccessoryPanel(client.player.inventoryMenu, "while crafting"));
        clickMenuSlot(context, 0);
        clickMenuSlot(context, 15);
        connection.waitForServerboundPackets();
        connection.waitForClientboundPackets();
        context.runOnClient(client -> {
            if (client.player.inventoryMenu.getSlot(15).getItem().getItem() != Items.CRAFTING_TABLE) {
                throw new AssertionError("Vanilla 2x2 crafting output did not place a crafting table");
            }
            for (int gridSlot = 1; gridSlot <= 4; gridSlot++) {
                if (!client.player.inventoryMenu.getSlot(gridSlot).getItem().isEmpty()) {
                    throw new AssertionError("Crafting grid was not consumed after collecting its recipe output");
                }
            }
            assertAccessoryPanel(client.player.inventoryMenu, "after crafting");
        });
        toggleRecipeBook(context, true);
        context.takeScreenshot("inventory-regression-recipe-book-open");
        toggleRecipeBook(context, false);
        assertClientServerMenusConverged(context, server);
        System.out.println("VANILLA_ARMOR_OFFHAND_CRAFTING_RECIPE_BOOK_PASS");
    }

    private static void exerciseBackpack(ClientGameTestContext context,
                                         net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext server,
                                         TestDedicatedServerConnection connection) {
        server.runOnServer(minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            BackpackStorage storage = BackpackStorage.empty();
            storage.items().set(0, named(Items.EMERALD, "backpack-opened"));
            storage.items().set(8, named(Items.DIAMOND, "backpack-retained-through-death"));
            player.setAttached(EquipmentAttachments.BACKPACK, storage);
            player.getInventory().setItem(0, new ItemStack(ModItems.BACKPACK));
            player.getInventory().setSelectedSlot(0);
            player.inventoryMenu.broadcastChanges();
        });
        connection.waitForClientboundPackets();
        context.runOnClient(client -> client.gui.setScreen(null));
        context.waitForScreen(null);
        context.getInput().pressKey(options -> options.keyUse);
        context.waitFor(client -> client.player.containerMenu instanceof ChestMenu);
        context.waitForScreen(ContainerScreen.class);
        connection.waitForClientboundPackets();
        context.runOnClient(client -> {
            ItemStack stored = client.player.containerMenu.getSlot(BACKPACK_ITEM_MENU_SLOT).getItem();
            if (stored.getItem() != Items.EMERALD
                    || !Component.literal("backpack-opened").equals(stored.get(DataComponents.CUSTOM_NAME))) {
                throw new AssertionError("Using the Backpack item did not open its synchronized persistent store");
            }
        });
        context.takeScreenshot("inventory-regression-backpack-open");

        clickContainerSlot(context, BACKPACK_ITEM_MENU_SLOT);
        clickContainerSlot(context, BACKPACK_PLAYER_MAIN_FIRST);
        connection.waitForServerboundPackets();
        connection.waitForClientboundPackets();
        server.computeOnServer(minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            if (!player.getAttachedOrElse(EquipmentAttachments.BACKPACK, BackpackStorage.empty()).items().get(0).isEmpty()) {
                throw new AssertionError("Backpack withdrawal left a ghost item in persistent storage");
            }
            if (player.getInventory().countItem(Items.EMERALD) != 1) {
                throw new AssertionError("Backpack withdrawal did not converge to the player inventory");
            }
            return null;
        });
        context.runOnClient(client -> {
            if (client.player.inventoryMenu.getCarried().getItem() == Items.EMERALD) {
                throw new AssertionError("Backpack withdrawal left a ghost cursor stack");
            }
        });
        context.runOnClient(client -> client.gui.setScreen(null));
        context.waitForScreen(null);

        server.runOnServer(minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            player.setGameMode(GameType.SURVIVAL);
            player.die(player.level().damageSources().generic());
            var drops = player.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new AABB(player.blockPosition()).inflate(16));
            ServerPlayer respawned = minecraftServer.getPlayerList().respawn(
                    player, false, Entity.RemovalReason.KILLED);
            ItemStack retained = respawned.getAttachedOrElse(EquipmentAttachments.BACKPACK,
                    BackpackStorage.empty()).items().get(8);
            if (retained.getItem() != Items.DIAMOND
                    || !Component.literal("backpack-retained-through-death").equals(retained.get(DataComponents.CUSTOM_NAME))) {
                throw new AssertionError("Backpack contents did not survive normal death and respawn");
            }
            drops.forEach(net.minecraft.world.entity.item.ItemEntity::discard);
            respawned.getInventory().setItem(0, new ItemStack(ModItems.BACKPACK));
            respawned.getInventory().setSelectedSlot(0);
            respawned.inventoryMenu.broadcastChanges();
        });
        connection.waitForClientboundPackets();
        context.waitFor(client -> client.player != null && client.player.getAttachedOrElse(
                EquipmentAttachments.BACKPACK, BackpackStorage.empty()).items().get(8).getItem() == Items.DIAMOND);
        System.out.println("BACKPACK_INTERACTION_DEATH_RETENTION_PASS");
    }

    private static ItemStack named(net.minecraft.world.item.Item item, String name) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }

    private static void clickMenuSlot(ClientGameTestContext context, int slotIndex) {
        double[] point = menuSlotPosition(context, slotIndex);
        context.getInput().setCursorPos(point[0], point[1]);
        context.getInput().pressMouse(LEFT_BUTTON);
    }

    private static void clickContainerSlot(ClientGameTestContext context, int slotIndex) {
        context.runOnClient(client -> {
            if (!(client.gui.screen() instanceof AbstractContainerScreen<?> screen)) {
                throw new AssertionError("Expected an open container screen");
            }
            var slot = client.player.containerMenu.getSlot(slotIndex);
            double scale = client.getWindow().getGuiScale();
            MouseButtonEvent event = new MouseButtonEvent((screenIntField(screen, "leftPos") + slot.x + 8.0),
                    (screenIntField(screen, "topPos") + slot.y + 8.0), new MouseButtonInfo(LEFT_BUTTON, 0));
            screen.mouseClicked(event, false);
            screen.mouseReleased(event);
        });
    }

    private static double[] menuSlotPosition(ClientGameTestContext context, int slotIndex) {
        return context.computeOnClient(client -> {
            if (!(client.gui.screen() instanceof AbstractContainerScreen<?> screen)) {
                throw new AssertionError("Expected an open container screen");
            }
            var slot = client.player.containerMenu.getSlot(slotIndex);
            double scale = client.getWindow().getGuiScale();
            return new double[]{(screenIntField(screen, "leftPos") + slot.x + 8.0) * scale,
                    (screenIntField(screen, "topPos") + slot.y + 8.0) * scale};
        });
    }

    private static void toggleRecipeBook(ClientGameTestContext context, boolean visible) {
        InventoryScreen screen = context.computeOnClient(client -> (InventoryScreen) client.gui.screen());
        double[] point = context.computeOnClient(client -> {
            Object button = findRecipeToggle(screen, screen,
                    Collections.newSetFromMap(new IdentityHashMap<>()), 0);
            try {
                double x = ((Number) button.getClass().getMethod("getX").invoke(button)).doubleValue();
                double y = ((Number) button.getClass().getMethod("getY").invoke(button)).doubleValue();
                double scale = client.getWindow().getGuiScale();
                return new double[]{(x + 10) * scale, (y + 10) * scale};
            } catch (ReflectiveOperationException exception) {
                throw new AssertionError("Could not locate recipe-book toggle", exception);
            }
        });
        context.getInput().setCursorPos(point[0], point[1]);
        context.getInput().pressMouse(LEFT_BUTTON);
        context.runOnClient(client -> {
            if (recipeBookVisible(screen) != visible) {
                throw new AssertionError("Recipe book visibility did not change to " + visible);
            }
            assertAccessoryPanel(client.player.inventoryMenu, "while toggling recipe book");
        });
    }

    private static Object findRecipeToggle(Object root, InventoryScreen screen, Set<Object> visited, int depth) {
        if (root == null || depth > 6 || !visited.add(root)) return null;
        if (root.getClass().getSimpleName().endsWith("Button")) {
            try {
                int x = ((Number) root.getClass().getMethod("getX").invoke(root)).intValue();
                int y = ((Number) root.getClass().getMethod("getY").invoke(root)).intValue();
                int left = screenIntField(screen, "leftPos");
                int top = screenIntField(screen, "topPos");
                if (x >= left + 80 && x <= left + 140
                        && y >= top + 40 && y <= top + 100) return root;
            } catch (RuntimeException | ReflectiveOperationException ignored) {
                // Not the inventory recipe button.
            }
        }
        if (root instanceof Iterable<?> values) {
            for (Object value : values) {
                Object match = findRecipeToggle(value, screen, visited, depth + 1);
                if (match != null) return match;
            }
        }
        for (Class<?> type = root.getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()
                        || field.getType().isEnum() || field.getType() == String.class) continue;
                try {
                    field.setAccessible(true);
                    Object match = findRecipeToggle(field.get(root), screen, visited, depth + 1);
                    if (match != null) return match;
                } catch (RuntimeException | IllegalAccessException ignored) {
                    // Skip inaccessible implementation details.
                }
            }
        }
        return null;
    }

    private static boolean recipeBookVisible(InventoryScreen screen) {
        Object component = findNamedComponent(screen, "RecipeBookComponent");
        if (component == null) throw new AssertionError("Inventory recipe-book component was not found");
        for (Class<?> type = component.getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (field.getType() == boolean.class && field.getName().toLowerCase().contains("visible")) {
                    try {
                        field.setAccessible(true);
                        return field.getBoolean(component);
                    } catch (ReflectiveOperationException exception) {
                        throw new AssertionError("Could not read recipe-book visibility", exception);
                    }
                }
            }
        }
        throw new AssertionError("Recipe-book visibility field was not found");
    }

    private static Object findNamedComponent(Object root, String classPart) {
        return findObject(root, value -> value.getClass().getSimpleName().contains(classPart),
                Collections.newSetFromMap(new IdentityHashMap<>()), 0);
    }

    private static Object findObject(Object root, java.util.function.Predicate<Object> match,
                                     Set<Object> visited, int depth) {
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
                    // Skip inaccessible implementation details.
                }
            }
        }
        return null;
    }

    private static int screenIntField(AbstractContainerScreen<?> screen, String name) {
        for (Class<?> type = screen.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.getInt(screen);
            } catch (NoSuchFieldException ignored) {
                // Continue through the screen hierarchy.
            } catch (ReflectiveOperationException exception) {
                throw new AssertionError("Could not read screen field " + name, exception);
            }
        }
        throw new AssertionError("Screen field not found: " + name);
    }

    private static void assertAccessoryPanel(net.minecraft.world.inventory.InventoryMenu menu, String scenario) {
        if (menu.slots.size() < 52) throw new AssertionError("Vanilla inventory menu lost accessory slots " + scenario);
        AccessoryDefinition[] items = {AccessoryDefinition.STAR_COVENANT, AccessoryDefinition.EMBER_RING,
                AccessoryDefinition.SEED_OF_LIFE, AccessoryDefinition.SNOW_CHARM,
                AccessoryDefinition.WHISPER_OF_THE_SEA};
        for (int slot = 0; slot < items.length; slot++) {
            if (menu.getSlot(47 + slot).getItem().getItem() != ModItems.ACCESSORIES.get(items[slot])) {
                throw new AssertionError("Accessory menu slot " + slot + " changed " + scenario);
            }
        }
    }

    private static void assertClientServerMenusConverged(ClientGameTestContext context,
                                                          net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext server) {
        var serverStacks = server.computeOnServer(minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            return player.inventoryMenu.slots.stream().map(slot -> slot.getItem().copy()).toList();
        });
        context.runOnClient(client -> {
            for (int slot = 0; slot < serverStacks.size(); slot++) {
                if (!ItemStack.matches(serverStacks.get(slot), client.player.inventoryMenu.getSlot(slot).getItem())) {
                    throw new AssertionError("Vanilla/accessory inventory mismatch at slot " + slot);
                }
            }
        });
    }
}
