package dev.itzgamesgd.moreminecrafts.accessory;

import dev.itzgamesgd.moreminecrafts.equipment.EquipmentAttachments;
import dev.itzgamesgd.moreminecrafts.registry.ModDataComponents;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;

/** Rendered-client checks for accessory state across disconnect, dimension, death, and menu input. */
@SuppressWarnings("UnstableApiUsage")
public final class AccessoryClientLifecycleGameTest implements FabricClientGameTest {
    private static final int LEFT_MOUSE_BUTTON = 1;
    private static final int DEFENSE_MENU_SLOT = 48;
    private static final int RING_INVENTORY_SLOT = 11;
    private static final AccessoryDefinition[] SLOT_ITEMS = {
            AccessoryDefinition.SOUL_CHAIN,
            AccessoryDefinition.EMBER_RING,
            AccessoryDefinition.SEED_OF_LIFE,
            AccessoryDefinition.SNOW_CHARM,
            AccessoryDefinition.WHISPER_OF_THE_SEA
    };

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestDedicatedServerContext server = context.worldBuilder().createServer()) {
            TestDedicatedServerConnection connection = server.connect();
            try {
                awaitConnected(context);
                installFullFixture(server);
                connection.waitForClientboundPackets();
                assertFullFixture(context, server);
                verifyRapidClicksAndConvergence(context, server, connection);

                connection = verifyReconnect(context, server, connection);
                connection = verifyDimensions(context, server, connection);
                connection = verifyNormalDeathAndRespawn(context, server, connection);
                connection = verifyKeepInventoryRespawn(context, server, connection);

                context.runOnClient(client -> client.gui.setScreen(null));
                context.waitForScreen(null);
            } finally {
                connection.close();
            }
        }
    }

    private static TestDedicatedServerConnection verifyReconnect(ClientGameTestContext context,
                                                                  TestDedicatedServerContext server,
                                                                  TestDedicatedServerConnection connection) {
        System.out.println("ACCESSORY_RECONNECT_BEGIN");
        connection.close();
        context.waitFor(client -> client.player == null || client.getConnection() == null
                || !client.getConnection().getConnection().isConnected());
        connection = server.connect();
        awaitConnected(context);
        connection.waitForClientboundPackets();
        assertFullFixture(context, server);
        System.out.println("ACCESSORY_RECONNECT_PASS");
        return connection;
    }

    private static TestDedicatedServerConnection verifyDimensions(ClientGameTestContext context,
                                                                   TestDedicatedServerContext server,
                                                                   TestDedicatedServerConnection connection) {
        System.out.println("ACCESSORY_DIMENSIONS_BEGIN");
        for (var dimension : List.of(Level.NETHER, Level.END, Level.OVERWORLD)) {
            server.runOnServer(minecraftServer -> {
                ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
                var target = minecraftServer.getLevel(dimension);
                if (target == null) throw new AssertionError("Required dimension is unavailable: " + dimension);
                if (!player.teleportTo(target, 0.5D, 80.0D, 0.5D, Set.<Relative>of(),
                        player.getYRot(), player.getXRot(), true)) {
                    throw new AssertionError("Dimension transfer failed: " + dimension);
                }
            });
            connection.waitForClientboundPackets();
            context.waitFor(client -> client.level != null && client.level.dimension().equals(dimension));
            assertFullFixture(context, server);
            assertClientServerConverged(context, server);
            System.out.println("ACCESSORY_DIMENSION_PASS dimension=" + dimension.identifier());
        }
        return connection;
    }

    private static TestDedicatedServerConnection verifyNormalDeathAndRespawn(ClientGameTestContext context,
                                                                              TestDedicatedServerContext server,
                                                                              TestDedicatedServerConnection connection) {
        System.out.println("ACCESSORY_NORMAL_DEATH_BEGIN");
        server.runOnServer(minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            player.setGameMode(GameType.SURVIVAL);
            player.die(player.level().damageSources().generic());
            List<ItemEntity> drops = player.level().getEntitiesOfClass(ItemEntity.class,
                    new AABB(player.blockPosition()).inflate(16));
            List<ItemStack> accessoryDrops = drops.stream().map(ItemEntity::getItem)
                    .filter(stack -> isFixtureAccessory(stack)).map(ItemStack::copy).toList();
            if (accessoryDrops.size() != SLOT_ITEMS.length) {
                throw new AssertionError("Normal client death produced " + accessoryDrops.size()
                        + " accessory drops; expected " + SLOT_ITEMS.length);
            }
            for (AccessoryDefinition definition : SLOT_ITEMS) {
                long count = accessoryDrops.stream().filter(stack -> stack.getItem()
                        == ModItems.ACCESSORIES.get(definition)).count();
                if (count != 1) throw new AssertionError("Normal death must drop "
                        + definition.registryId() + " once; found " + count);
            }
            drops.forEach(ItemEntity::discard);
            ServerPlayer respawned = minecraftServer.getPlayerList().respawn(
                    player, false, Entity.RemovalReason.KILLED);
            if (!respawned.getAttachedOrElse(EquipmentAttachments.ACCESSORIES,
                    AccessoryLoadout.empty()).isEmpty()) {
                throw new AssertionError("Normal respawn retained equipped accessories");
            }
            respawned.inventoryMenu.broadcastChanges();
        });
        connection.waitForClientboundPackets();
        context.waitFor(client -> client.player != null && client.player.getAttachedOrElse(
                EquipmentAttachments.ACCESSORIES, AccessoryLoadout.empty()).isEmpty());
        assertClientServerConverged(context, server);
        System.out.println("ACCESSORY_NORMAL_DEATH_RESPAWN_PASS");
        return connection;
    }

    private static TestDedicatedServerConnection verifyKeepInventoryRespawn(ClientGameTestContext context,
                                                                             TestDedicatedServerContext server,
                                                                             TestDedicatedServerConnection connection) {
        System.out.println("ACCESSORY_KEEP_INVENTORY_BEGIN");
        server.runOnServer(minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            var rules = player.level().getGameRules();
            rules.set(GameRules.KEEP_INVENTORY, true, minecraftServer);
            equipFullFixture(player);
            player.die(player.level().damageSources().generic());
            List<ItemEntity> drops = player.level().getEntitiesOfClass(ItemEntity.class,
                    new AABB(player.blockPosition()).inflate(16));
            if (drops.stream().anyMatch(drop -> isFixtureAccessory(drop.getItem()))) {
                throw new AssertionError("keepInventory death dropped an equipped accessory");
            }
            ServerPlayer respawned = minecraftServer.getPlayerList().respawn(
                    player, false, Entity.RemovalReason.KILLED);
            if (respawned.getAttachedOrElse(EquipmentAttachments.ACCESSORIES,
                    AccessoryLoadout.empty()).occupiedSlots() != SLOT_ITEMS.length) {
                throw new AssertionError("keepInventory respawn did not retain all five accessories");
            }
            respawned.inventoryMenu.broadcastChanges();
        });
        connection.waitForClientboundPackets();
        context.waitFor(client -> client.player != null && client.player.getAttachedOrElse(
                EquipmentAttachments.ACCESSORIES, AccessoryLoadout.empty()).occupiedSlots() == SLOT_ITEMS.length);
        assertFullFixture(context, server);
        assertClientServerConverged(context, server);
        System.out.println("ACCESSORY_KEEP_INVENTORY_PASS");
        return connection;
    }

    private static void verifyRapidClicksAndConvergence(ClientGameTestContext context,
                                                          TestDedicatedServerContext server,
                                                          TestDedicatedServerConnection connection) {
        System.out.println("ACCESSORY_RAPID_CLICKS_BEGIN");
        server.runOnServer(minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            var loadout = player.getAttachedOrElse(EquipmentAttachments.ACCESSORIES, AccessoryLoadout.empty())
                    .with(AccessoryCategory.DEFENSE_SURVIVAL.ordinal(), ItemStack.EMPTY);
            player.setAttached(EquipmentAttachments.ACCESSORIES, loadout);
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                if (player.getInventory().getItem(slot).getItem()
                        == ModItems.ACCESSORIES.get(AccessoryDefinition.RING_OF_LIFE)) {
                    player.getInventory().setItem(slot, ItemStack.EMPTY);
                }
            }
            player.getInventory().setItem(RING_INVENTORY_SLOT,
                    new ItemStack(ModItems.ACCESSORIES.get(AccessoryDefinition.RING_OF_LIFE)));
            player.inventoryMenu.broadcastChanges();
        });
        connection.waitForClientboundPackets();
        assertServerAccessoryCount(server, AccessoryDefinition.RING_OF_LIFE, 1);
        context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
        context.waitForScreen(InventoryScreen.class);
        context.waitTicks(2);
        context.waitFor(client -> client.player.inventoryMenu.getSlot(RING_INVENTORY_SLOT).getItem().getItem()
                == ModItems.ACCESSORIES.get(AccessoryDefinition.RING_OF_LIFE));
        context.getInput().releaseMouse(LEFT_MOUSE_BUTTON);
        context.waitTick();

        for (int cycle = 0; cycle < 8; cycle++) {
            int cycleIndex = cycle;
            clickRapidMenuSlot(context, RING_INVENTORY_SLOT);
            connection.waitForServerboundPackets();
            connection.waitForClientboundPackets();
            context.runOnClient(client -> System.out.println("ACCESSORY_RAPID_FIRST_CLIENT menu="
                    + matchingMenuSlots(client.player.inventoryMenu)
                    + " carried=" + client.player.containerMenu.getCarried()
                    + " attachment=" + client.player.getAttachedOrElse(
                    EquipmentAttachments.ACCESSORIES, AccessoryLoadout.empty()).get(
                    AccessoryCategory.DEFENSE_SURVIVAL.ordinal())));
            server.computeOnServer(minecraftServer -> {
                ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
                System.out.println("ACCESSORY_RAPID_FIRST_SERVER menu=" + matchingMenuSlots(player.inventoryMenu)
                        + " carried=" + player.containerMenu.getCarried()
                        + " inventory=" + matchingInventorySlots(player));
                return null;
            });
            context.waitFor(client -> client.player.inventoryMenu.getSlot(RING_INVENTORY_SLOT).getItem().isEmpty()
                    && client.player.containerMenu.getCarried().getItem()
                    == ModItems.ACCESSORIES.get(AccessoryDefinition.RING_OF_LIFE));

            clickRapidMenuSlot(context, DEFENSE_MENU_SLOT);
            connection.waitForServerboundPackets();
            connection.waitForClientboundPackets();
            context.waitFor(client -> client.player.getAttachedOrElse(
                    EquipmentAttachments.ACCESSORIES, AccessoryLoadout.empty())
                    .get(AccessoryCategory.DEFENSE_SURVIVAL.ordinal()).getItem()
                    == ModItems.ACCESSORIES.get(AccessoryDefinition.RING_OF_LIFE)
                    && client.player.containerMenu.getCarried().isEmpty());

            clickRapidMenuSlot(context, DEFENSE_MENU_SLOT);
            connection.waitForServerboundPackets();
            connection.waitForClientboundPackets();
            context.waitFor(client -> client.player.getAttachedOrElse(
                    EquipmentAttachments.ACCESSORIES, AccessoryLoadout.empty())
                    .get(AccessoryCategory.DEFENSE_SURVIVAL.ordinal()).isEmpty()
                    && client.player.containerMenu.getCarried().getItem()
                    == ModItems.ACCESSORIES.get(AccessoryDefinition.RING_OF_LIFE));

            clickRapidMenuSlot(context, RING_INVENTORY_SLOT);
            connection.waitForServerboundPackets();
            connection.waitForClientboundPackets();
            context.waitFor(client -> client.player.inventoryMenu.getSlot(RING_INVENTORY_SLOT).getItem().getItem()
                    == ModItems.ACCESSORIES.get(AccessoryDefinition.RING_OF_LIFE)
                    && client.player.containerMenu.getCarried().isEmpty());
            context.runOnClient(client -> {
                ItemStack equipped = client.player.inventoryMenu.getSlot(DEFENSE_MENU_SLOT).getItem();
                ItemStack inventory = client.player.inventoryMenu.getSlot(RING_INVENTORY_SLOT).getItem();
                if (!equipped.isEmpty() || inventory.getItem()
                        != ModItems.ACCESSORIES.get(AccessoryDefinition.RING_OF_LIFE)) {
                    throw new AssertionError("Rapid equip/unequip left a ghost or divergent accessory slot at cycle "
                            + cycleIndex + ": equip=" + equipped + " source=" + inventory);
                }
                if (!client.player.containerMenu.getCarried().isEmpty()) {
                    throw new AssertionError("Rapid click operation left a ghost cursor stack at cycle " + cycleIndex);
                }
            });
            assertClientServerConverged(context, server);
        }
        assertServerAccessoryCount(server, AccessoryDefinition.RING_OF_LIFE, 1);
        System.out.println("ACCESSORY_RAPID_CLICKS_CONVERGENCE_PASS cycles=8 equip_unequip_clicks=32");
    }

    private static void installFullFixture(TestDedicatedServerContext server) {
        server.runOnServer(minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            equipFullFixture(player);
            player.inventoryMenu.broadcastChanges();
        });
    }

    private static void equipFullFixture(ServerPlayer player) {
        List<ItemStack> stacks = new ArrayList<>(SLOT_ITEMS.length);
        for (int slot = 0; slot < SLOT_ITEMS.length; slot++) {
            ItemStack stack = new ItemStack(ModItems.ACCESSORIES.get(SLOT_ITEMS[slot]));
            stack.set(DataComponents.CUSTOM_NAME, Component.literal("lifecycle-slot-" + slot));
            stack.set(ModDataComponents.ENHANCEMENT_LEVEL, slot + 1);
            stacks.add(stack);
        }
        player.setAttached(EquipmentAttachments.ACCESSORIES, new AccessoryLoadout(stacks));
    }

    private static void assertFullFixture(ClientGameTestContext context, TestDedicatedServerContext server) {
        context.runOnClient(client -> {
            AccessoryLoadout loadout = client.player.getAttachedOrElse(
                    EquipmentAttachments.ACCESSORIES, AccessoryLoadout.empty());
            if (loadout.occupiedSlots() != SLOT_ITEMS.length) {
                throw new AssertionError("Client expected five synchronized accessories, found " + loadout.occupiedSlots());
            }
            for (int slot = 0; slot < SLOT_ITEMS.length; slot++) {
                ItemStack stack = loadout.get(slot);
                if (stack.getItem() != ModItems.ACCESSORIES.get(SLOT_ITEMS[slot])
                        || !Component.literal("lifecycle-slot-" + slot).equals(stack.get(DataComponents.CUSTOM_NAME))
                        || !Integer.valueOf(slot + 1).equals(stack.get(ModDataComponents.ENHANCEMENT_LEVEL))) {
                    throw new AssertionError("Client attachment differs at accessory slot " + slot + ": " + stack);
                }
                ItemStack menuStack = client.player.inventoryMenu.getSlot(47 + slot).getItem();
                if (!ItemStack.matches(stack, menuStack)) {
                    throw new AssertionError("Rendered menu slot " + (47 + slot)
                            + " differs from its synchronized attachment at slot " + slot);
                }
            }
        });
        assertServerFullFixture(server);
    }

    private static void assertServerFullFixture(TestDedicatedServerContext server) {
        server.computeOnServer(minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            for (int slot = 0; slot < SLOT_ITEMS.length; slot++) {
                ItemStack stack = player.getAttachedOrElse(
                        EquipmentAttachments.ACCESSORIES, AccessoryLoadout.empty()).get(slot);
                if (stack.getItem() != ModItems.ACCESSORIES.get(SLOT_ITEMS[slot])
                        || !Component.literal("lifecycle-slot-" + slot).equals(stack.get(DataComponents.CUSTOM_NAME))
                        || !Integer.valueOf(slot + 1).equals(stack.get(ModDataComponents.ENHANCEMENT_LEVEL))) {
                    throw new AssertionError("Server fixture differs at accessory slot " + slot + ": " + stack);
                }
            }
            return null;
        });
    }

    private static void assertClientServerConverged(ClientGameTestContext context,
                                                     TestDedicatedServerContext server) {
        List<ItemStack> serverSlots = server.computeOnServer(minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            List<ItemStack> snapshot = new ArrayList<>();
            for (int slot = 0; slot < player.inventoryMenu.slots.size(); slot++) {
                snapshot.add(player.inventoryMenu.getSlot(slot).getItem().copy());
            }
            snapshot.add(player.inventoryMenu.getCarried().copy());
            snapshot.addAll(player.getAttachedOrElse(EquipmentAttachments.ACCESSORIES,
                    AccessoryLoadout.empty()).stacks());
            return List.copyOf(snapshot);
        });
        context.runOnClient(client -> {
            int expectedSize = client.player.inventoryMenu.slots.size() + 1 + AccessoryLoadout.SIZE;
            if (serverSlots.size() != expectedSize) {
                throw new AssertionError("Client/server convergence fixture has different menu sizes: "
                        + serverSlots.size() + "/" + expectedSize);
            }
            int index = 0;
            for (; index < client.player.inventoryMenu.slots.size(); index++) {
                if (!ItemStack.matches(serverSlots.get(index),
                        client.player.inventoryMenu.getSlot(index).getItem())) {
                    throw new AssertionError("Client/server menu divergence at slot " + index + ": server="
                            + serverSlots.get(index) + " client=" + client.player.inventoryMenu.getSlot(index).getItem());
                }
            }
            if (!ItemStack.matches(serverSlots.get(index++), client.player.inventoryMenu.getCarried())) {
                throw new AssertionError("Client/server cursor divergence");
            }
            AccessoryLoadout clientLoadout = client.player.getAttachedOrElse(
                    EquipmentAttachments.ACCESSORIES, AccessoryLoadout.empty());
            for (int slot = 0; slot < AccessoryLoadout.SIZE; slot++) {
                if (!ItemStack.matches(serverSlots.get(index++), clientLoadout.get(slot))) {
                    throw new AssertionError("Client/server accessory attachment divergence at slot " + slot);
                }
            }
        });
    }

    private static void assertServerAccessoryCount(TestDedicatedServerContext server,
                                                   AccessoryDefinition definition, int expected) {
        server.computeOnServer(minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            int count = 0;
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                ItemStack stack = player.getInventory().getItem(slot);
                if (stack.getItem() == ModItems.ACCESSORIES.get(definition)) count += stack.getCount();
            }
            for (ItemStack stack : player.getAttachedOrElse(EquipmentAttachments.ACCESSORIES,
                    AccessoryLoadout.empty()).stacks()) {
                if (stack.getItem() == ModItems.ACCESSORIES.get(definition)) count += stack.getCount();
            }
            if (player.inventoryMenu.getCarried().getItem() == ModItems.ACCESSORIES.get(definition)) {
                count += player.inventoryMenu.getCarried().getCount();
            }
            if (count != expected) throw new AssertionError("Server accessory conservation failed for "
                    + definition.registryId() + ": " + count + "/" + expected);
            return null;
        });
    }

    private static void awaitConnected(ClientGameTestContext context) {
        context.waitFor(client -> client.player != null && client.level != null
                && client.getConnection() != null && client.getConnection().getConnection().isConnected());
    }

    private static void clickRapidMenuSlot(ClientGameTestContext context, int menuSlot) {
        double[] point = context.computeOnClient(client -> {
            var screen = (InventoryScreen) client.gui.screen();
            var slot = client.player.inventoryMenu.getSlot(menuSlot);
            double scale = client.getWindow().getGuiScale();
            return new double[]{(screenIntField(screen, "leftPos") + slot.x + 8.0) * scale,
                    (screenIntField(screen, "topPos") + slot.y + 8.0) * scale};
        });
        context.getInput().setCursorPos(point[0], point[1]);
        context.getInput().holdMouse(LEFT_MOUSE_BUTTON);
        context.waitTick();
        context.getInput().releaseMouse(LEFT_MOUSE_BUTTON);
        context.waitTick();
        context.runOnClient(client -> {
            System.out.println("ACCESSORY_RAPID_CLICK slot=" + menuSlot + " shift=" + client.hasShiftDown()
                    + " source=" + client.player.inventoryMenu.getSlot(RING_INVENTORY_SLOT).getItem()
                    + " equipped=" + client.player.inventoryMenu.getSlot(DEFENSE_MENU_SLOT).getItem()
                    + " cursor=" + client.player.containerMenu.getCarried());
        });
    }

    private static List<String> matchingMenuSlots(net.minecraft.world.inventory.AbstractContainerMenu menu) {
        List<String> slots = new ArrayList<>();
        for (int slot = 0; slot < menu.slots.size(); slot++) {
            ItemStack stack = menu.getSlot(slot).getItem();
            if (stack.getItem() == ModItems.ACCESSORIES.get(AccessoryDefinition.RING_OF_LIFE)) {
                slots.add(slot + "=" + stack);
            }
        }
        return slots;
    }

    private static List<String> matchingInventorySlots(ServerPlayer player) {
        List<String> slots = new ArrayList<>();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.getItem() == ModItems.ACCESSORIES.get(AccessoryDefinition.RING_OF_LIFE)) {
                slots.add(slot + "=" + stack);
            }
        }
        return slots;
    }

    private static int screenIntField(InventoryScreen screen, String name) {
        for (Class<?> type = screen.getClass(); type != null; type = type.getSuperclass()) {
            try {
                var field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.getInt(screen);
            } catch (NoSuchFieldException ignored) {
                // Continue through the screen hierarchy.
            } catch (ReflectiveOperationException exception) {
                throw new AssertionError("Could not read screen field " + name, exception);
            }
        }
        throw new AssertionError("InventoryScreen field not found: " + name);
    }

    private static boolean isFixtureAccessory(ItemStack stack) {
        for (AccessoryDefinition definition : SLOT_ITEMS) {
            if (stack.getItem() == ModItems.ACCESSORIES.get(definition)) return true;
        }
        return false;
    }
}
