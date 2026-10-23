package dev.itzgamesgd.moreminecrafts.accessory;

import dev.itzgamesgd.moreminecrafts.equipment.EquipmentAttachments;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;

/**
 * Real-client acceptance of the integrated accessory column: geometry against the survival recipe book at GUI
 * scales 2/3/4 (open and closed, repeated toggles, resize/reinit, vanilla narrow fallback), slot usability beside
 * an open recipe book, border-click safety, and tooltips. Input is Fabric Client GameTest input.
 */
@SuppressWarnings("UnstableApiUsage")
public final class AccessoryInventoryLayoutClientGameTest implements FabricClientGameTest {
    private static final int LEFT = 1;
    private static final int ACCESSORY_FIRST_MENU_SLOT = 47;
    private static final int RING_MENU_SLOT = 11;
    private static final int RECIPE_BOOK_WIDTH = 147;
    private static final int RECIPE_BOOK_HEIGHT = 166;
    private static final Path REPORT = Path.of("accessory-ui-geometry.txt");

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestDedicatedServerContext server = context.worldBuilder().createServer();
             TestDedicatedServerConnection connection = server.connect()) {
            context.waitFor(client -> client.player != null && client.level != null
                    && client.getConnection() != null && client.getConnection().getConnection().isConnected());
            prepareFixture(server);
            connection.waitForClientboundPackets();
            context.waitFor(client -> client.player.getAttachedOrElse(
                    EquipmentAttachments.ACCESSORIES, AccessoryLoadout.empty()).get(1).isEmpty()
                    && client.player.getInventory().getItem(RING_MENU_SLOT).getItem()
                    == ModItems.ACCESSORIES.get(AccessoryDefinition.RING_OF_LIFE));
            openInventory(context);
            context.waitTicks(200); // let join toasts clear
            report("# scale | state | gui WxH | leftPos,topPos | recipe book x,y,w,h | accessory cells x (column) | issues");
            for (int scale : List.of(2, 3, 4)) {
                setScaleAndOpenInventory(context, scale);
                int closedLeft = geometry(context, "scale " + scale + " closed", false);
                context.takeScreenshot("ui-scale-" + scale + "-recipe-closed");

                toggleRecipeBook(context, true);
                geometry(context, "scale " + scale + " open", true);
                context.takeScreenshot("ui-scale-" + scale + "-recipe-open");

                for (int cycle = 1; cycle <= 5; cycle++) {
                    toggleRecipeBook(context, false);
                    int left = geometry(context, "scale " + scale + " close#" + cycle, false);
                    if (left != closedLeft) throw new AssertionError("Inventory drifted after toggle " + cycle + ": " + left + " vs " + closedLeft);
                    toggleRecipeBook(context, true);
                    geometry(context, "scale " + scale + " open#" + cycle, true);
                }

                // Reinitialisation while open: a resize to the same size must reproduce the geometry exactly.
                int[] gui = guiSize(context);
                context.runOnClient(client -> client.gui.screen().resize(gui[0], gui[1]));
                context.waitTicks(2);
                geometry(context, "scale " + scale + " open after resize", true);
                // Shrink below the vanilla 379px threshold: the book falls back to vanilla's overlay mode.
                context.runOnClient(client -> client.gui.screen().resize(340, 200));
                context.waitTicks(2);
                geometry(context, "scale " + scale + " narrow fallback (gui 340x200)", true);
                context.takeScreenshot("ui-scale-" + scale + "-narrow-fallback");
                context.runOnClient(client -> client.gui.screen().resize(gui[0], gui[1]));
                context.waitTicks(2);
                geometry(context, "scale " + scale + " restored", true);
                toggleRecipeBook(context, false);
                if (geometry(context, "scale " + scale + " closed again", false) != closedLeft) {
                    throw new AssertionError("Inventory position changed after resize/restore at scale " + scale);
                }
            }

            // Usability beside an OPEN recipe book at the tightest layout (scale 4).
            setScaleAndOpenInventory(context, 4);
            toggleRecipeBook(context, true);
            geometry(context, "usability open", true);
            clickMenuSlot(context, RING_MENU_SLOT);
            double[] border = cellBorderCursor(context, 0);
            context.getInput().setCursorPos(border[0], border[1]);
            context.getInput().pressMouse(LEFT); // border pixel between accessory cells must not drop the held stack
            context.waitTicks(2);
            context.runOnClient(client -> {
                if (client.player.containerMenu.getCarried().getItem() != ModItems.ACCESSORIES.get(AccessoryDefinition.RING_OF_LIFE)) {
                    throw new AssertionError("Clicking an accessory cell border dropped or lost the carried stack");
                }
            });
            assertNoDroppedItems(server);
            clickMenuSlot(context, ACCESSORY_FIRST_MENU_SLOT + 1);
            context.waitTicks(2);
            context.runOnClient(client -> {
                var loadout = client.player.getAttachedOrElse(EquipmentAttachments.ACCESSORIES, AccessoryLoadout.empty());
                if (loadout.get(1).getItem() != ModItems.ACCESSORIES.get(AccessoryDefinition.RING_OF_LIFE)
                        || !client.player.containerMenu.getCarried().isEmpty()) {
                    throw new AssertionError("Click-equip beside the open recipe book failed");
                }
            });
            context.takeScreenshot("ui-usability-equipped-recipe-open");
            clickMenuSlot(context, ACCESSORY_FIRST_MENU_SLOT + 1);
            clickMenuSlot(context, RING_MENU_SLOT);
            context.waitTicks(2);
            context.runOnClient(client -> {
                if (!client.player.getAttachedOrElse(EquipmentAttachments.ACCESSORIES, AccessoryLoadout.empty()).get(1).isEmpty()
                        || client.player.getInventory().getItem(RING_MENU_SLOT).getItem()
                        != ModItems.ACCESSORIES.get(AccessoryDefinition.RING_OF_LIFE)) {
                    throw new AssertionError("Click-unequip beside the open recipe book did not conserve the ring");
                }
            });
            assertNoDroppedItems(server);
            toggleRecipeBook(context, false);

            // Tooltips: category once on an item; category name on an empty slot (visual).
            context.runOnClient(client -> {
                ItemStack ring = new ItemStack(ModItems.ACCESSORIES.get(AccessoryDefinition.RING_OF_LIFE));
                List<Component> lines = ring.getTooltipLines(Item.TooltipContext.of(client.level), client.player, TooltipFlag.NORMAL);
                String expected = Component.translatable("tooltip.moreminecrafts.accessory_category",
                        AccessoryCategory.DEFENSE_SURVIVAL.displayName()).getString();
                long matches = lines.stream().filter(line -> line.getString().equals(expected)).count();
                if (matches != 1) throw new AssertionError("Expected exactly one category tooltip line, saw " + matches + " in " + lines);
                System.out.println("ACCESSORY_TOOLTIP_LINES " + lines.stream().map(Component::getString).toList());
            });
            hoverMenuSlot(context, ACCESSORY_FIRST_MENU_SLOT + 1); // empty Defense / Survival slot
            context.waitTicks(10);
            context.takeScreenshot("ui-tooltip-empty-defense-slot");
            hoverMenuSlot(context, ACCESSORY_FIRST_MENU_SLOT); // occupied Combat slot
            context.waitTicks(10);
            context.takeScreenshot("ui-tooltip-occupied-combat-slot");

            context.runOnClient(client -> {
                client.gui.setScreen(null);
                client.options.guiScale().set(0);
            });
            context.waitForScreen(null);
        }
    }

    private static void prepareFixture(TestDedicatedServerContext server) {
        server.runOnServer(minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            player.setGameMode(GameType.SURVIVAL);
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                player.getInventory().setItem(slot, ItemStack.EMPTY);
            }
            player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
            player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
            player.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
            player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
            // Combat, Support, Exploration and Lifestyle equipped; Defense / Survival left empty for the usability checks.
            player.setAttached(EquipmentAttachments.ACCESSORIES, new AccessoryLoadout(List.of(
                    new ItemStack(ModItems.ACCESSORIES.get(AccessoryDefinition.STAR_COVENANT)),
                    ItemStack.EMPTY,
                    new ItemStack(ModItems.ACCESSORIES.get(AccessoryDefinition.SEED_OF_LIFE)),
                    new ItemStack(ModItems.ACCESSORIES.get(AccessoryDefinition.SNOW_CHARM)),
                    new ItemStack(ModItems.ACCESSORIES.get(AccessoryDefinition.WHISPER_OF_THE_SEA)))));
            player.getInventory().setItem(RING_MENU_SLOT, new ItemStack(ModItems.ACCESSORIES.get(AccessoryDefinition.RING_OF_LIFE)));
            player.inventoryMenu.broadcastChanges();
        });
    }

    private static void assertNoDroppedItems(TestDedicatedServerContext server) {
        server.runOnServer(minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            if (!player.level().getEntitiesOfClass(ItemEntity.class, new AABB(player.blockPosition()).inflate(8)).isEmpty()) {
                throw new AssertionError("A click dropped an item entity into the world");
            }
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
                throw new AssertionError("Requested GUI scale " + scale + " but effective scale is " + client.getWindow().getGuiScale());
            }
        });
        openInventory(context);
    }

    private static int[] guiSize(ClientGameTestContext context) {
        return context.computeOnClient(client -> new int[]{client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight()});
    }

    /** Asserts the layout contract and returns leftPos. */
    private static int geometry(ClientGameTestContext context, String label, boolean expectBookOpen) {
        return context.computeOnClient(client -> {
            InventoryScreen screen = (InventoryScreen) client.gui.screen();
            var menu = client.player.inventoryMenu;
            int left = intField(screen, "leftPos");
            int top = intField(screen, "topPos");
            int width = intField(screen, "width");
            int height = intField(screen, "height");
            boolean narrow = width < 379;
            Object book = findObject(screen, value -> value.getClass().getSimpleName().contains("RecipeBookComponent"),
                    Collections.newSetFromMap(new IdentityHashMap<>()), 0);
            if (book == null) throw new AssertionError("Recipe book component not found (" + label + ")");
            boolean visible = (boolean) invoke(book, "isVisible");
            if (visible != expectBookOpen) throw new AssertionError("Recipe book visible=" + visible + " at " + label);

            if (menu.slots.size() < ACCESSORY_FIRST_MENU_SLOT + AccessoryLoadout.SIZE) throw new AssertionError("Missing accessory slots at " + label);
            for (int index = 0; index < 4; index++) {
                Slot armor = menu.getSlot(5 + index);
                if (armor.x != 8 || armor.y != 8 + index * 18) throw new AssertionError("Vanilla armor slot " + index + " moved at " + label);
            }
            Slot offhand = menu.getSlot(45);
            if (offhand.x != 77 || offhand.y != 62) throw new AssertionError("Vanilla offhand slot moved at " + label);
            if (intField(screen, "imageWidth") != 176 || intField(screen, "imageHeight") != 166) {
                throw new AssertionError("Inventory dimensions are not vanilla at " + label);
            }
            for (int index = 0; index < AccessoryLoadout.SIZE; index++) {
                Slot slot = menu.getSlot(ACCESSORY_FIRST_MENU_SLOT + index);
                if (slot.x != AccessoryUiLayout.SLOT_X || slot.y != AccessoryUiLayout.slotY(index)) {
                    throw new AssertionError("Accessory slot " + index + " misplaced at " + label + ": " + slot.x + "," + slot.y);
                }
            }

            List<int[]> items = new ArrayList<>();
            for (int index = 0; index < menu.slots.size(); index++) {
                Slot slot = menu.getSlot(index);
                int x = left + slot.x;
                int y = top + slot.y;
                if (x - 1 < 0 || y - 1 < 0 || x + 17 > width || y + 17 > height) throw new AssertionError("Slot " + index + " off-screen at " + label);
                for (int[] other : items) {
                    if (x < other[0] + 16 && other[0] < x + 16 && y < other[1] + 16 && other[1] < y + 16) {
                        throw new AssertionError("Slot " + index + " overlaps slot " + other[2] + " at " + label);
                    }
                }
                items.add(new int[]{x, y, index});
            }

            String bookText = "closed";
            if (visible && !narrow) {
                int bx = (int) invoke(book, "getXOrigin");
                int by = (int) invoke(book, "getYOrigin");
                bookText = bx + "," + by + "," + RECIPE_BOOK_WIDTH + "," + RECIPE_BOOK_HEIGHT;
                for (int[] item : items) {
                    // 1px cell border around every 16px slot item.
                    int sx = item[0] - 1, sy = item[1] - 1;
                    if (sx < bx + RECIPE_BOOK_WIDTH && bx < sx + 18 && sy < by + RECIPE_BOOK_HEIGHT && by < sy + 18) {
                        throw new AssertionError("Recipe book overlaps slot " + item[2] + " at " + label + " book=" + bookText);
                    }
                }
                if (bx < 0) throw new AssertionError("Recipe book is off-screen at " + label + " x=" + bx);
            } else if (visible) {
                bookText = "narrow overlay (vanilla)";
            }
            String line = label + " | gui " + width + "x" + height + " | " + left + "," + top + " | book " + bookText
                    + " | accessory cells x=" + (left + AccessoryUiLayout.COLUMN_LEFT) + ".." + (left - 1) + " | ok";
            report(line);
            System.out.println("ACCESSORY_UI_GEOMETRY " + line);
            return left;
        });
    }

    private static void toggleRecipeBook(ClientGameTestContext context, boolean open) {
        double[] point = context.computeOnClient(client -> {
            InventoryScreen screen = (InventoryScreen) client.gui.screen();
            double[] center = widgetCenter(findRecipeToggle(screen));
            double scale = client.getWindow().getGuiScale();
            return new double[]{center[0] * scale, center[1] * scale};
        });
        context.getInput().setCursorPos(point[0], point[1]);
        context.getInput().pressMouse(LEFT);
        context.waitTicks(2);
        boolean visible = context.computeOnClient(client -> {
            Object book = findObject(client.gui.screen(), value -> value.getClass().getSimpleName().contains("RecipeBookComponent"),
                    Collections.newSetFromMap(new IdentityHashMap<>()), 0);
            return (boolean) invoke(book, "isVisible");
        });
        if (visible != open) throw new AssertionError("Recipe book did not " + (open ? "open" : "close") + " after click");
    }

    private static void hoverMenuSlot(ClientGameTestContext context, int menuSlot) {
        double[] point = slotCursor(context, menuSlot);
        context.getInput().setCursorPos(point[0], point[1]);
    }

    private static void clickMenuSlot(ClientGameTestContext context, int menuSlot) {
        double[] point = slotCursor(context, menuSlot);
        context.getInput().setCursorPos(point[0], point[1]);
        context.getInput().pressMouse(LEFT);
        context.waitTicks(1);
    }

    private static double[] slotCursor(ClientGameTestContext context, int menuSlot) {
        return context.computeOnClient(client -> {
            InventoryScreen screen = (InventoryScreen) client.gui.screen();
            Slot slot = client.player.containerMenu.getSlot(menuSlot);
            double scale = client.getWindow().getGuiScale();
            return new double[]{(intField(screen, "leftPos") + slot.x + 8.0) * scale, (intField(screen, "topPos") + slot.y + 8.0) * scale};
        });
    }

    /** Window-pixel position on the 1px border between accessory cells 0 and 1 (outside any slot's 16px hit box). */
    private static double[] cellBorderCursor(ClientGameTestContext context, int upperCell) {
        return context.computeOnClient(client -> {
            InventoryScreen screen = (InventoryScreen) client.gui.screen();
            double scale = client.getWindow().getGuiScale();
            double x = (intField(screen, "leftPos") + AccessoryUiLayout.SLOT_X + 8.0) * scale;
            double y = (intField(screen, "topPos") + AccessoryUiLayout.slotY(upperCell) + 16.5) * scale;
            return new double[]{x, y};
        });
    }

    private static void report(String line) {
        try {
            Files.writeString(REPORT, line + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
            // The console copy of the line is authoritative if the run directory is not writable.
        }
    }

    private static Object invoke(Object target, String name) {
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Method method = type.getDeclaredMethod(name);
                method.setAccessible(true);
                return method.invoke(target);
            } catch (NoSuchMethodException ignored) {
                // Search the parent type.
            } catch (ReflectiveOperationException error) {
                throw new AssertionError("Could not call " + name, error);
            }
        }
        throw new AssertionError("Method not found: " + name);
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

    private static double[] widgetCenter(Object widget) {
        try {
            double x = ((Number) widget.getClass().getMethod("getX").invoke(widget)).doubleValue();
            double y = ((Number) widget.getClass().getMethod("getY").invoke(widget)).doubleValue();
            double w = ((Number) widget.getClass().getMethod("getWidth").invoke(widget)).doubleValue();
            double h = ((Number) widget.getClass().getMethod("getHeight").invoke(widget)).doubleValue();
            return new double[]{x + w / 2.0, y + h / 2.0};
        } catch (ReflectiveOperationException error) {
            throw new AssertionError("Could not read recipe-book toggle position", error);
        }
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
                    // Skip inaccessible implementation details.
                }
            }
        }
        return null;
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
