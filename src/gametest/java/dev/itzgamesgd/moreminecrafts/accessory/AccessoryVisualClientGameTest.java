package dev.itzgamesgd.moreminecrafts.accessory;

import dev.itzgamesgd.moreminecrafts.equipment.BackpackStorage;
import dev.itzgamesgd.moreminecrafts.equipment.EquipmentAttachments;
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
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/**
 * Visual acceptance fixture: renders all twelve accessories in real inventory slots at GUI scales 2/3/4,
 * with the recipe book closed and open, alongside vanilla armor, offhand and the Backpack screen.
 * Each screenshot is preceded by VISUAL_SLOT lines (window-pixel slot rectangles) that the evidence script
 * uses to crop native-size icons. Input is Fabric Client GameTest input, never OS pointer input.
 */
@SuppressWarnings("UnstableApiUsage")
public final class AccessoryVisualClientGameTest implements FabricClientGameTest {
    private static final int SDL_MOUSE_BUTTON_LEFT = 1;
    private static final int FIRST_GRID_MENU_SLOT = 9;
    private static final int ACCESSORY_FIRST_MENU_SLOT = 47;
    private static final List<AccessoryDefinition> COMBAT = byCategory(AccessoryCategory.COMBAT);
    private static final List<AccessoryDefinition> DEFENSE = byCategory(AccessoryCategory.DEFENSE_SURVIVAL);
    private static final List<AccessoryDefinition> EXPLORATION = byCategory(AccessoryCategory.EXPLORATION_MOBILITY);

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestDedicatedServerContext server = context.worldBuilder().createServer();
             TestDedicatedServerConnection connection = server.connect()) {
            context.waitFor(client -> client.player != null && client.level != null
                    && client.getConnection() != null && client.getConnection().getConnection().isConnected());
            prepareFixture(server);
            connection.waitForClientboundPackets();
            setLoadout(context, server, 0);

            openInventory(context);
            // Let join toasts clear so screenshots show the complete panels.
            context.waitTicks(200);
            for (int scale : List.of(2, 3, 4)) {
                setScaleAndOpenInventory(context, scale);
                assertLayout(context, "scale " + scale + " recipe closed");
                capture(context, "visual-scale-" + scale + "-recipe-closed");
                toggleRecipeBook(context, true);
                assertLayout(context, "scale " + scale + " recipe open");
                capture(context, "visual-scale-" + scale + "-recipe-open");
                toggleRecipeBook(context, false);
            }

            // Rotate every accessory through its real equipment slot (Combat has six candidates).
            setScaleAndOpenInventory(context, 4);
            for (int round = 0; round < COMBAT.size(); round++) {
                setLoadout(context, server, round);
                context.waitTicks(2);
                assertLayout(context, "equipped round " + round);
                capture(context, "visual-equipped-round-" + round);
            }
            context.runOnClient(client -> client.gui.setScreen(null));
            context.waitForScreen(null);

            for (int scale : List.of(2, 3, 4)) {
                captureBackpack(context, server, connection, scale);
            }
            context.runOnClient(client -> {
                client.gui.setScreen(null);
                client.options.guiScale().set(0);
            });
            context.waitForScreen(null);
        }
    }

    private static List<AccessoryDefinition> byCategory(AccessoryCategory category) {
        return AccessoryDefinition.ALL.stream().filter(definition -> definition.category() == category).toList();
    }

    private static ItemStack stack(AccessoryDefinition definition) {
        return new ItemStack(ModItems.ACCESSORIES.get(definition));
    }

    private static List<ItemStack> loadoutFor(int round) {
        return List.of(
                stack(COMBAT.get(round % COMBAT.size())),
                stack(DEFENSE.get(round % DEFENSE.size())),
                stack(AccessoryDefinition.SEED_OF_LIFE),
                stack(EXPLORATION.get(round % EXPLORATION.size())),
                stack(AccessoryDefinition.WHISPER_OF_THE_SEA));
    }

    private static void prepareFixture(TestDedicatedServerContext server) {
        server.runOnServer(minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            player.setGameMode(GameType.SURVIVAL);
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                player.getInventory().setItem(slot, ItemStack.EMPTY);
            }
            // All twelve accessories as ordinary inventory stacks in the 3x9 main grid (menu slots 9-20).
            int slot = FIRST_GRID_MENU_SLOT;
            for (AccessoryDefinition definition : AccessoryDefinition.ALL) {
                player.getInventory().setItem(slot++, stack(definition));
            }
            // Vanilla armor and offhand occupants for layout regression.
            player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
            player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
            player.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
            player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
            player.inventoryMenu.broadcastChanges();
        });
    }

    private static void setLoadout(ClientGameTestContext context, TestDedicatedServerContext server, int round) {
        List<ItemStack> stacks = loadoutFor(round);
        server.runOnServer(minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            player.setAttached(EquipmentAttachments.ACCESSORIES, new AccessoryLoadout(stacks));
            player.inventoryMenu.broadcastChanges();
        });
        context.waitFor(client -> {
            AccessoryLoadout loadout = client.player.getAttachedOrElse(
                    EquipmentAttachments.ACCESSORIES, AccessoryLoadout.empty());
            for (int slot = 0; slot < AccessoryLoadout.SIZE; slot++) {
                if (loadout.get(slot).getItem() != stacks.get(slot).getItem()) return false;
            }
            return true;
        });
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
        context.runOnClient(client -> {
            if (client.getWindow().getGuiScale() != scale) {
                throw new AssertionError("Requested GUI scale " + scale + " but effective scale is "
                        + client.getWindow().getGuiScale());
            }
        });
        openInventory(context);
    }

    /** Logs window-pixel slot rectangles, then takes the screenshot. */
    private static void capture(ClientGameTestContext context, String name) {
        context.runOnClient(client -> {
            if (!(client.gui.screen() instanceof AbstractContainerScreen<?> screen)) {
                throw new AssertionError("Expected a container screen while capturing " + name);
            }
            int left = intField(screen, "leftPos");
            int top = intField(screen, "topPos");
            int scale = (int) client.getWindow().getGuiScale();
            var menu = client.player.containerMenu;
            for (int index = 0; index < menu.slots.size(); index++) {
                Slot slot = menu.getSlot(index);
                ItemStack item = slot.getItem();
                if (item.isEmpty()) continue;
                System.out.println("VISUAL_SLOT name=" + name + " menuSlot=" + index + " scale=" + scale
                        + " x=" + (left + slot.x) * scale + " y=" + (top + slot.y) * scale
                        + " size=" + 16 * scale + " item=" + net.minecraft.core.registries.BuiltInRegistries.ITEM
                        .getKey(item.getItem()));
            }
        });
        context.takeScreenshot(name);
    }

    private static void assertLayout(ClientGameTestContext context, String scenario) {
        context.runOnClient(client -> {
            InventoryScreen screen = (InventoryScreen) client.gui.screen();
            var menu = client.player.inventoryMenu;
            int left = intField(screen, "leftPos");
            int top = intField(screen, "topPos");
            int guiWidth = client.getWindow().getGuiScaledWidth();
            int guiHeight = client.getWindow().getGuiScaledHeight();
            if (menu.slots.size() < ACCESSORY_FIRST_MENU_SLOT + AccessoryLoadout.SIZE) {
                throw new AssertionError("Missing accessory slots " + scenario);
            }
            // Vanilla armor (5-8) and offhand (45) keep their vanilla relative positions.
            for (int index = 0; index < 4; index++) {
                Slot armor = menu.getSlot(5 + index);
                if (armor.x != 8 || armor.y != 8 + index * 18) {
                    throw new AssertionError("Armor slot " + index + " moved " + scenario);
                }
            }
            Slot offhand = menu.getSlot(45);
            if (offhand.x != 77 || offhand.y != 62) throw new AssertionError("Offhand slot moved " + scenario);
            if (menu.getSlot(5).getItem().getItem() != Items.IRON_HELMET
                    || menu.getSlot(6).getItem().getItem() != Items.IRON_CHESTPLATE
                    || menu.getSlot(7).getItem().getItem() != Items.IRON_LEGGINGS
                    || menu.getSlot(8).getItem().getItem() != Items.IRON_BOOTS
                    || offhand.getItem().getItem() != Items.SHIELD) {
                throw new AssertionError("Armor/offhand contents changed " + scenario);
            }
            for (int offset = 0; offset < AccessoryDefinition.ALL.size(); offset++) {
                ItemStack expected = stack(AccessoryDefinition.ALL.get(offset));
                if (menu.getSlot(FIRST_GRID_MENU_SLOT + offset).getItem().getItem() != expected.getItem()) {
                    throw new AssertionError("Grid slot " + offset + " does not hold "
                            + AccessoryDefinition.ALL.get(offset).registryId() + " " + scenario);
                }
            }
            // Every slot stays inside the GUI and no two slots overlap.
            List<int[]> rectangles = new ArrayList<>();
            for (int index = 0; index < menu.slots.size(); index++) {
                Slot slot = menu.getSlot(index);
                int x = left + slot.x;
                int y = top + slot.y;
                if (x < 0 || y < 0 || x + 16 > guiWidth || y + 16 > guiHeight) {
                    throw new AssertionError("Slot " + index + " is off-screen " + scenario);
                }
                for (int[] other : rectangles) {
                    if (x < other[0] + 16 && other[0] < x + 16 && y < other[1] + 16 && other[1] < y + 16) {
                        throw new AssertionError("Slot " + index + " overlaps slot " + other[2] + " " + scenario);
                    }
                }
                rectangles.add(new int[]{x, y, index});
            }
            System.out.println("VISUAL_LAYOUT_OK " + scenario + " gui=" + guiWidth + "x" + guiHeight
                    + " scale=" + client.getWindow().getGuiScale());
        });
    }

    private static void captureBackpack(ClientGameTestContext context, TestDedicatedServerContext server,
                                        TestDedicatedServerConnection connection, int scale) {
        server.runOnServer(minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            BackpackStorage storage = BackpackStorage.empty();
            storage.items().set(0, new ItemStack(Items.EMERALD));
            storage.items().set(8, new ItemStack(Items.DIAMOND));
            player.setAttached(EquipmentAttachments.BACKPACK, storage);
            player.getInventory().setItem(0, new ItemStack(ModItems.BACKPACK));
            player.getInventory().setSelectedSlot(0);
            player.inventoryMenu.broadcastChanges();
        });
        connection.waitForClientboundPackets();
        context.runOnClient(client -> {
            client.gui.setScreen(null);
            client.options.guiScale().set(scale);
        });
        context.waitForScreen(null);
        context.waitTick();
        context.getInput().pressKey(options -> options.keyUse);
        context.waitFor(client -> client.player.containerMenu instanceof ChestMenu);
        context.waitForScreen(ContainerScreen.class);
        connection.waitForClientboundPackets();
        context.waitTicks(2);
        context.runOnClient(client -> {
            ContainerScreen screen = (ContainerScreen) client.gui.screen();
            var menu = client.player.containerMenu;
            if (client.getWindow().getGuiScale() != scale) throw new AssertionError("Backpack scale mismatch");
            Slot first = menu.getSlot(0);
            Slot last = menu.getSlot(26);
            if (first.x != 8 || first.y != 18 || last.x != 152 || last.y != 54) {
                throw new AssertionError("Backpack chest grid layout changed at scale " + scale);
            }
            int left = intField(screen, "leftPos");
            int top = intField(screen, "topPos");
            for (int index = 0; index < menu.slots.size(); index++) {
                Slot slot = menu.getSlot(index);
                if (left + slot.x < 0 || top + slot.y < 0
                        || left + slot.x + 16 > client.getWindow().getGuiScaledWidth()
                        || top + slot.y + 16 > client.getWindow().getGuiScaledHeight()) {
                    throw new AssertionError("Backpack slot " + index + " is off-screen at scale " + scale);
                }
            }
            System.out.println("VISUAL_LAYOUT_OK backpack scale " + scale);
        });
        capture(context, "visual-backpack-scale-" + scale);
        context.runOnClient(client -> client.gui.setScreen(null));
        context.waitForScreen(null);
    }

    private static void toggleRecipeBook(ClientGameTestContext context, boolean visible) {
        InventoryScreen screen = context.computeOnClient(client -> (InventoryScreen) client.gui.screen());
        double[] point = context.computeOnClient(client -> {
            double[] center = widgetCenter(findRecipeToggle(screen));
            double scale = client.getWindow().getGuiScale();
            return new double[]{center[0] * scale, center[1] * scale};
        });
        context.getInput().setCursorPos(point[0], point[1]);
        context.getInput().pressMouse(SDL_MOUSE_BUTTON_LEFT);
        context.waitTicks(2);
        if (recipeBookVisible(context.computeOnClient(client -> (InventoryScreen) client.gui.screen())) != visible) {
            throw new AssertionError("Recipe book visibility did not change to " + visible);
        }
    }

    private static Object findRecipeToggle(InventoryScreen screen) {
        int left = intField(screen, "leftPos");
        int top = intField(screen, "topPos");
        Object found = findObject(screen, value -> {
            if (!value.getClass().getSimpleName().endsWith("Button")) return false;
            try {
                int x = ((Number) value.getClass().getMethod("getX").invoke(value)).intValue();
                int y = ((Number) value.getClass().getMethod("getY").invoke(value)).intValue();
                return x >= left + 80 && x <= left + 140 && y >= top + 40 && y <= top + 100;
            } catch (ReflectiveOperationException ignored) {
                return false;
            }
        }, Collections.newSetFromMap(new IdentityHashMap<>()), 0);
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

    private static double[] widgetCenter(Object widget) {
        try {
            double x = ((Number) widget.getClass().getMethod("getX").invoke(widget)).doubleValue();
            double y = ((Number) widget.getClass().getMethod("getY").invoke(widget)).doubleValue();
            double width = ((Number) widget.getClass().getMethod("getWidth").invoke(widget)).doubleValue();
            double height = ((Number) widget.getClass().getMethod("getHeight").invoke(widget)).doubleValue();
            return new double[]{x + width / 2.0, y + height / 2.0};
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
