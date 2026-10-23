package dev.itzgamesgd.moreminecrafts.accessory;

import dev.itzgamesgd.moreminecrafts.client.T1AnvilScreen;
import dev.itzgamesgd.moreminecrafts.progression.T1AnvilModes;
import dev.itzgamesgd.moreminecrafts.registry.ModDataComponents;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/**
 * Real-client anvil screen comparison. Captures the integrated accessory Enhancement interface for every
 * candidate catalyst-slot layout at GUI scales 2, 3 and 4, plus the ordinary-equipment screen and +10 maximum
 * state. No layout is approved; {@link AnvilCatalystLayout#CURRENT} stays the shipped default and is restored
 * afterwards. The catalyst icon is the clearly marked test-only fixture from src/gametest/resources.
 */
@SuppressWarnings("UnstableApiUsage")
public final class AccessoryAnvilClientGameTest implements FabricClientGameTest {
    private static final int SDL_MOUSE_BUTTON_LEFT = 1;

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestDedicatedServerContext server = context.worldBuilder().createServer();
             TestDedicatedServerConnection connection = server.connect()) {
            context.waitFor(client -> client.player != null && client.level != null
                    && client.getConnection() != null && client.getConnection().getConnection().isConnected());
            context.waitTicks(200);
            try {
                for (AnvilCatalystLayout layout : AnvilCatalystLayout.values()) {
                    AnvilCatalystLayout.useForComparison(layout);
                    for (int scale : List.of(2, 3, 4)) {
                        setScale(context, scale);
                        open(context, server, connection, AccessoryDefinition.SNOW_CHARM, 4);
                        verify(context, layout, scale);
                        capture(context, "anvil-" + layout.name().toLowerCase() + "-scale-" + scale + "-accessory-4-to-5");
                        pickUpAndReturnCatalyst(context, layout, scale);
                        close(context);
                    }
                    setScale(context, 3);
                    open(context, server, connection, AccessoryDefinition.SNOW_CHARM, 10);
                    capture(context, "anvil-" + layout.name().toLowerCase() + "-scale-3-accessory-10-maximum");
                    close(context);
                    openEquipment(context, server, connection);
                    capture(context, "anvil-" + layout.name().toLowerCase() + "-scale-3-ordinary-equipment");
                    close(context);
                }
            } finally {
                AnvilCatalystLayout.useForComparison(AnvilCatalystLayout.CURRENT);
                context.runOnClient(client -> {
                    client.gui.setScreen(null);
                    client.options.guiScale().set(0);
                });
                context.waitForScreen(null);
            }
        }
    }

    private static void setScale(ClientGameTestContext context, int scale) {
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
    }

    private static void open(ClientGameTestContext context, TestDedicatedServerContext server,
                             TestDedicatedServerConnection connection, AccessoryDefinition accessory, int level) {
        openMenu(context, server, connection, () -> {
            ItemStack charm = new ItemStack(ModItems.ACCESSORIES.get(accessory));
            charm.set(ModDataComponents.ENHANCEMENT_LEVEL, level);
            return charm;
        }, new ItemStack(ModItems.ENHANCEMENT_STONE, 16));
    }

    private static void openEquipment(ClientGameTestContext context, TestDedicatedServerContext server,
                                      TestDedicatedServerConnection connection) {
        openMenu(context, server, connection, () -> new ItemStack(ModItems.FLAME_SWORD),
                new ItemStack(ModItems.ENHANCEMENT_STONE, 16));
    }

    private static void openMenu(ClientGameTestContext context, TestDedicatedServerContext server,
                                 TestDedicatedServerConnection connection,
                                 java.util.function.Supplier<ItemStack> target, ItemStack stones) {
        server.runOnServer(minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            player.setGameMode(GameType.SURVIVAL);
            player.closeContainer();
            player.getInventory().clearContent();
            player.giveExperiencePoints(100_000);
            var world = player.level();
            // Solid support so the anvil does not fall and invalidate the menu.
            BlockPos pos = player.blockPosition().east(2);
            world.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
            world.setBlock(pos, Blocks.ANVIL.defaultBlockState(), 3);
            player.openMenu(Blocks.ANVIL.defaultBlockState().getMenuProvider(world, pos));
            AnvilMenu menu = (AnvilMenu) player.containerMenu;
            menu.getSlot(0).set(target.get());
            menu.getSlot(1).set(stones);
            var modes = (T1AnvilModes) menu;
            menu.getSlot(modes.moreminecrafts$catalystSlotIndex()).set(new ItemStack(ModItems.RELIC_CATALYST, 1));
            menu.broadcastChanges();
            if (!menu.stillValid(player)) throw new AssertionError("Anvil menu is not valid for the player");
        });
        connection.waitForClientboundPackets();
        context.waitForScreen(T1AnvilScreen.class);
        context.waitTicks(10);
    }

    private static void capture(ClientGameTestContext context, String name) {
        context.getInput().setCursorPos(2, 2);
        context.waitTick();
        context.takeScreenshot(name);
    }

    private static void close(ClientGameTestContext context) {
        context.runOnClient(client -> client.gui.setScreen(null));
        context.waitForScreen(null);
    }

    /** Logs the catalyst slot geometry; comparison layouts must stay inside the panel without overlaps. */
    private static void verify(ClientGameTestContext context, AnvilCatalystLayout layout, int scale) {
        context.runOnClient(client -> {
            T1AnvilScreen screen = (T1AnvilScreen) client.gui.screen();
            var menu = client.player.containerMenu;
            int left = intField(screen, "leftPos");
            int top = intField(screen, "topPos");
            int imageWidth = intField(screen, "imageWidth");
            int imageHeight = intField(screen, "imageHeight");
            int catalystIndex = ((T1AnvilModes) menu).moreminecrafts$catalystSlotIndex();
            Slot catalyst = menu.getSlot(catalystIndex);
            int[] frame = {left + catalyst.x - 1, top + catalyst.y - 1, 18, 18};
            boolean inside = catalyst.x - 1 >= 0 && catalyst.y - 1 >= 0
                    && catalyst.x + 17 <= imageWidth && catalyst.y + 17 <= imageHeight;
            boolean onScreen = frame[0] >= 0 && frame[1] >= 0
                    && frame[0] + 18 <= client.getWindow().getGuiScaledWidth()
                    && frame[1] + 18 <= client.getWindow().getGuiScaledHeight();
            List<String> overlaps = new ArrayList<>();
            for (int index = 0; index < menu.slots.size(); index++) {
                if (index == catalystIndex || index == 2) continue; // the result slot is hidden in Enhance mode
                Slot other = menu.getSlot(index);
                if (intersects(frame, new int[]{left + other.x - 1, top + other.y - 1, 18, 18})) overlaps.add("slot" + index);
            }
            for (var child : screen.children()) {
                if (child instanceof AbstractWidget widget && widget.visible
                        && intersects(frame, new int[]{widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight()})) {
                    overlaps.add("widget@" + widget.getX() + "," + widget.getY());
                }
            }
            System.out.println("ANVIL_LAYOUT variant=" + layout + " scale=" + scale + " slot=" + catalyst.x + "," + catalyst.y
                    + " panel=" + imageWidth + "x" + imageHeight + " insidePanel=" + inside + " onScreen=" + onScreen
                    + " overlaps=" + overlaps);
            if (layout != AnvilCatalystLayout.CURRENT && (!inside || !onScreen || !overlaps.isEmpty())) {
                throw new AssertionError("Candidate layout " + layout + " is outside the panel or overlaps at scale "
                        + scale + ": " + overlaps);
            }
            if (!onScreen) throw new AssertionError("Catalyst slot is off-screen for " + layout + " at scale " + scale);
        });
    }

    /** Proves the slot is clickable: pick the catalyst stack up with the cursor, then put it back. */
    private static void pickUpAndReturnCatalyst(ClientGameTestContext context, AnvilCatalystLayout layout, int scale) {
        double[] point = context.computeOnClient(client -> {
            T1AnvilScreen screen = (T1AnvilScreen) client.gui.screen();
            Slot catalyst = client.player.containerMenu.getSlot(
                    ((T1AnvilModes) client.player.containerMenu).moreminecrafts$catalystSlotIndex());
            double guiScale = client.getWindow().getGuiScale();
            return new double[]{(intField(screen, "leftPos") + catalyst.x + 8) * guiScale,
                    (intField(screen, "topPos") + catalyst.y + 8) * guiScale};
        });
        context.getInput().setCursorPos(point[0], point[1]);
        context.getInput().pressMouse(SDL_MOUSE_BUTTON_LEFT);
        context.waitTicks(3);
        boolean pickedUp = context.computeOnClient(client ->
                client.player.containerMenu.getCarried().getItem() == ModItems.RELIC_CATALYST);
        if (!pickedUp) {
            // Recorded, not thrown, for the shipped layout: vanilla treats a click right of the panel as a
            // click outside the window, which is exactly what this comparison is meant to expose.
            System.out.println("ANVIL_CATALYST_CLICK_FAILED variant=" + layout + " scale=" + scale);
            if (layout != AnvilCatalystLayout.CURRENT) {
                throw new AssertionError("Catalyst slot was not clickable for " + layout + " at scale " + scale);
            }
            return;
        }
        context.getInput().pressMouse(SDL_MOUSE_BUTTON_LEFT);
        context.waitTicks(3);
        context.runOnClient(client -> {
            var menu = client.player.containerMenu;
            if (!menu.getCarried().isEmpty() || menu.getSlot(((T1AnvilModes) menu).moreminecrafts$catalystSlotIndex())
                    .getItem().getItem() != ModItems.RELIC_CATALYST) {
                throw new AssertionError("Catalyst did not return to its slot for " + layout + " at scale " + scale);
            }
        });
        System.out.println("ANVIL_CATALYST_CLICK_OK variant=" + layout + " scale=" + scale);
    }

    private static boolean intersects(int[] a, int[] b) {
        return a[0] < b[0] + b[2] && b[0] < a[0] + a[2] && a[1] < b[1] + b[3] && b[1] < a[1] + a[3];
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
