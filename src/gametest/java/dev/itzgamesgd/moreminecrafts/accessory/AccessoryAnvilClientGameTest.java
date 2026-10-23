package dev.itzgamesgd.moreminecrafts.accessory;

import dev.itzgamesgd.moreminecrafts.client.T1AnvilScreen;
import dev.itzgamesgd.moreminecrafts.enhancement.EnhancementOperation;
import dev.itzgamesgd.moreminecrafts.progression.RawXp;
import dev.itzgamesgd.moreminecrafts.progression.T1AnvilModes;
import dev.itzgamesgd.moreminecrafts.registry.ModDataComponents;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/**
 * Real-client acceptance for the anvil's accessory Enhancement interface at GUI scales 2, 3 and 4. Every
 * interaction uses Fabric Client GameTest mouse input: the catalyst slot is picked up and returned, Enhance is
 * pressed, and the server-side transaction (XP, stones, catalyst, level) is checked. Also covers insufficient
 * resources, the +10 maximum, ordinary equipment, and a vanilla Anvil-tab repair taken from the result slot.
 * The catalyst icon is the clearly marked test-only fixture from src/gametest/resources.
 */
@SuppressWarnings("UnstableApiUsage")
public final class AccessoryAnvilClientGameTest implements FabricClientGameTest {
    private static final int SDL_MOUSE_BUTTON_LEFT = 1;

    private record State(int level, int stones, int catalysts, int rawXp, int xpLevels) {}

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestDedicatedServerContext server = context.worldBuilder().createServer();
             TestDedicatedServerConnection connection = server.connect()) {
            context.waitFor(client -> client.player != null && client.level != null
                    && client.getConnection() != null && client.getConnection().getConnection().isConnected());
            context.waitTicks(200);
            try {
                for (int scale : List.of(2, 3, 4)) {
                    setScale(context, scale);
                    accessoryMilestoneTransaction(context, server, connection, scale);
                    insufficientResourcesAreRejected(context, server, connection, scale);
                    maximumLevelIsRejected(context, server, connection, scale);
                    ordinaryEquipmentStillEnhances(context, server, connection, scale);
                    vanillaAnvilTabStillRepairs(context, server, connection, scale);
                }
            } finally {
                context.runOnClient(client -> {
                    client.gui.setScreen(null);
                    client.options.guiScale().set(0);
                });
                context.waitForScreen(null);
            }
        }
    }

    // ---- scenarios ------------------------------------------------------------------------------------------

    private static void accessoryMilestoneTransaction(ClientGameTestContext context, TestDedicatedServerContext server,
                                                      TestDedicatedServerConnection connection, int scale) {
        openAccessory(context, server, connection, 4, 16, 1);
        verifyGeometry(context, scale);
        pickUpAndReturnCatalyst(context, scale);
        capture(context, "anvil-scale-" + scale + "-accessory-4-to-5-before");
        State before = state(server, true);
        clickEnhance(context, true);
        State after = state(server, true);
        check(after.level() == 5, scale, "+4 to +5 raises the level to 5, was " + after.level());
        check(before.stones() - after.stones() == 2, scale, "milestone consumes 2 Enhancement Stones");
        check(before.catalysts() - after.catalysts() == 1, scale, "milestone consumes 1 Relic Catalyst");
        check(before.rawXp() - after.rawXp() == 40, scale, "+4 to +5 consumes 40 raw XP, took "
                + (before.rawXp() - after.rawXp()));
        capture(context, "anvil-scale-" + scale + "-accessory-4-to-5-after");
        System.out.println("ANVIL_ENHANCE_OK scale=" + scale + " " + before + " -> " + after);
        close(context);
    }

    private static void insufficientResourcesAreRejected(ClientGameTestContext context, TestDedicatedServerContext server,
                                                         TestDedicatedServerConnection connection, int scale) {
        // Milestone needs 2 stones and 1 catalyst; provide 1 stone and no catalyst.
        openAccessory(context, server, connection, 4, 1, 0);
        State before = state(server, true);
        boolean active = enhanceButtonActive(context);
        check(!active, scale, "Enhance is inactive without enough stones and catalyst");
        clickEnhance(context, false);
        State after = state(server, true);
        check(before.equals(after), scale, "rejected enhancement changes nothing: " + before + " vs " + after);
        capture(context, "anvil-scale-" + scale + "-accessory-insufficient");
        System.out.println("ANVIL_INSUFFICIENT_REJECTED scale=" + scale);
        close(context);
    }

    private static void maximumLevelIsRejected(ClientGameTestContext context, TestDedicatedServerContext server,
                                               TestDedicatedServerConnection connection, int scale) {
        openAccessory(context, server, connection, 10, 16, 1);
        State before = state(server, true);
        check(!enhanceButtonActive(context), scale, "Enhance is inactive at +10");
        clickEnhance(context, false);
        State after = state(server, true);
        check(before.equals(after) && after.level() == 10, scale, "+10 stays at its maximum unchanged");
        capture(context, "anvil-scale-" + scale + "-accessory-10-maximum");
        System.out.println("ANVIL_MAXIMUM_REJECTED scale=" + scale);
        close(context);
    }

    private static void ordinaryEquipmentStillEnhances(ClientGameTestContext context, TestDedicatedServerContext server,
                                                       TestDedicatedServerConnection connection, int scale) {
        openMenu(context, server, connection, () -> new ItemStack(ModItems.FLAME_SWORD),
                new ItemStack(ModItems.ENHANCEMENT_STONE, 16), new ItemStack(ModItems.RELIC_CATALYST, 1));
        capture(context, "anvil-scale-" + scale + "-ordinary-equipment-before");
        State before = state(server, false);
        var requirement = EnhancementOperation.requirement(new ItemStack(ModItems.FLAME_SWORD)).orElseThrow();
        check(enhanceButtonActive(context), scale, "Enhance is active for ordinary equipment with stones");
        clickEnhance(context, true);
        State after = state(server, false);
        check(after.level() == before.level() + 1, scale, "ordinary equipment gains one level");
        check(before.stones() - after.stones() == requirement.materialCost(), scale,
                "ordinary equipment pays its own stone cost");
        check(after.catalysts() == before.catalysts(), scale, "ordinary equipment never consumes the catalyst");
        capture(context, "anvil-scale-" + scale + "-ordinary-equipment-after");
        System.out.println("ANVIL_ORDINARY_OK scale=" + scale + " " + before + " -> " + after);
        close(context);
    }

    private static void vanillaAnvilTabStillRepairs(ClientGameTestContext context, TestDedicatedServerContext server,
                                                    TestDedicatedServerConnection connection, int scale) {
        ItemStack damaged = new ItemStack(Items.IRON_SWORD);
        damaged.setDamageValue(150);
        openMenu(context, server, connection, () -> damaged.copy(), new ItemStack(Items.IRON_INGOT, 4), ItemStack.EMPTY);
        click(context, tabCenter(context, Component.translatable("menu.moreminecrafts.normal_anvil").getString()));
        context.waitTicks(8);
        context.runOnClient(client -> {
            var menu = client.player.containerMenu;
            if (((T1AnvilModes) menu).moreminecrafts$mode() != 1) throw new AssertionError("Anvil tab did not switch mode");
            Slot first = menu.getSlot(0);
            Slot second = menu.getSlot(1);
            Slot result = menu.getSlot(2);
            if (first.x != 27 || first.y != 47 || second.x != 76 || second.y != 47 || result.x != 134 || result.y != 47) {
                throw new AssertionError("Vanilla anvil slots moved");
            }
            int catalystIndex = ((T1AnvilModes) menu).moreminecrafts$catalystSlotIndex();
            if (menu.getSlot(catalystIndex).mayPlace(new ItemStack(ModItems.RELIC_CATALYST))) {
                throw new AssertionError("Catalyst slot accepts items outside Enhance mode");
            }
            T1AnvilScreen screen = (T1AnvilScreen) client.gui.screen();
            for (var child : screen.children()) {
                if (child instanceof Button button && button.visible
                        && button.getX() == intField(screen, "leftPos") + AnvilCatalystUiLayout.ACTION_X) {
                    throw new AssertionError("Enhance button is visible in Anvil mode");
                }
            }
            if (!result.hasItem()) throw new AssertionError("Vanilla repair produced no result item");
        });
        capture(context, "anvil-scale-" + scale + "-vanilla-anvil-tab");
        // Take the repaired sword out of the real result slot with the mouse.
        click(context, slotCenter(context, 2));
        context.waitTicks(5);
        context.runOnClient(client -> {
            ItemStack carried = client.player.containerMenu.getCarried();
            if (carried.getItem() != Items.IRON_SWORD || carried.getDamageValue() >= 150) {
                throw new AssertionError("Result slot did not hand over the repaired sword: " + carried);
            }
        });
        System.out.println("ANVIL_VANILLA_REPAIR_OK scale=" + scale);
        close(context);
    }

    // ---- fixtures and helpers -------------------------------------------------------------------------------

    private static void openAccessory(ClientGameTestContext context, TestDedicatedServerContext server,
                                      TestDedicatedServerConnection connection, int level, int stones, int catalysts) {
        openMenu(context, server, connection, () -> {
            ItemStack charm = new ItemStack(ModItems.ACCESSORIES.get(AccessoryDefinition.SNOW_CHARM));
            charm.set(ModDataComponents.ENHANCEMENT_LEVEL, level);
            return charm;
        }, new ItemStack(ModItems.ENHANCEMENT_STONE, stones),
                catalysts > 0 ? new ItemStack(ModItems.RELIC_CATALYST, catalysts) : ItemStack.EMPTY);
    }

    private static void openMenu(ClientGameTestContext context, TestDedicatedServerContext server,
                                 TestDedicatedServerConnection connection, Supplier<ItemStack> target,
                                 ItemStack second, ItemStack catalyst) {
        server.runOnServer(minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            player.setGameMode(GameType.SURVIVAL);
            player.closeContainer();
            player.getInventory().clearContent();
            player.setExperienceLevels(0);
            player.giveExperiencePoints(100_000);
            var world = player.level();
            // Solid support so the anvil does not fall and invalidate the menu.
            BlockPos pos = player.blockPosition().east(2);
            world.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
            world.setBlock(pos, Blocks.ANVIL.defaultBlockState(), 3);
            player.openMenu(Blocks.ANVIL.defaultBlockState().getMenuProvider(world, pos));
            AnvilMenu menu = (AnvilMenu) player.containerMenu;
            menu.getSlot(0).set(target.get());
            menu.getSlot(1).set(second.copy());
            menu.getSlot(((T1AnvilModes) menu).moreminecrafts$catalystSlotIndex()).set(catalyst.copy());
            menu.broadcastChanges();
            if (!menu.stillValid(player)) throw new AssertionError("Anvil menu is not valid for the player");
        });
        connection.waitForClientboundPackets();
        context.waitForScreen(T1AnvilScreen.class);
        context.waitTicks(10);
    }

    private static State state(TestDedicatedServerContext server, boolean accessory) {
        AtomicReference<State> ref = new AtomicReference<>();
        server.runOnServer(minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            if (!(player.containerMenu instanceof AnvilMenu menu)) {
                throw new AssertionError("The anvil menu closed unexpectedly");
            }
            ItemStack target = menu.getSlot(0).getItem();
            int level = accessory ? AccessoryEnhancementOperation.level(target) : EnhancementOperation.level(target);
            ref.set(new State(level, menu.getSlot(1).getItem().getCount(),
                    menu.getSlot(((T1AnvilModes) menu).moreminecrafts$catalystSlotIndex()).getItem().getCount(),
                    RawXp.available(player), player.experienceLevel));
        });
        return ref.get();
    }

    private static void check(boolean condition, int scale, String message) {
        if (!condition) throw new AssertionError("[scale " + scale + "] " + message);
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

    private static void capture(ClientGameTestContext context, String name) {
        context.getInput().setCursorPos(2, 2);
        context.waitTick();
        context.takeScreenshot(name);
    }

    private static void close(ClientGameTestContext context) {
        context.runOnClient(client -> client.gui.setScreen(null));
        context.waitForScreen(null);
    }

    private static void click(ClientGameTestContext context, double[] point) {
        context.getInput().setCursorPos(point[0], point[1]);
        context.getInput().pressMouse(SDL_MOUSE_BUTTON_LEFT);
        context.waitTicks(5);
    }

    private static double[] slotCenter(ClientGameTestContext context, int index) {
        return context.computeOnClient(client -> {
            T1AnvilScreen screen = (T1AnvilScreen) client.gui.screen();
            Slot slot = client.player.containerMenu.getSlot(index);
            double guiScale = client.getWindow().getGuiScale();
            return new double[]{(intField(screen, "leftPos") + slot.x + 8) * guiScale,
                    (intField(screen, "topPos") + slot.y + 8) * guiScale};
        });
    }

    private static double[] tabCenter(ClientGameTestContext context, String label) {
        return context.computeOnClient(client -> {
            T1AnvilScreen screen = (T1AnvilScreen) client.gui.screen();
            double guiScale = client.getWindow().getGuiScale();
            for (var child : screen.children()) {
                if (child instanceof Button button && button.getMessage().getString().equals(label)) {
                    return new double[]{(button.getX() + button.getWidth() / 2.0) * guiScale,
                            (button.getY() + button.getHeight() / 2.0) * guiScale};
                }
            }
            throw new AssertionError("Mode tab not found: " + label);
        });
    }

    private static Button enhanceButton(T1AnvilScreen screen) {
        int x = intField(screen, "leftPos") + AnvilCatalystUiLayout.ACTION_X;
        int y = intField(screen, "topPos") + AnvilCatalystUiLayout.ACTION_Y;
        for (var child : screen.children()) {
            if (child instanceof Button button && button.visible && button.getX() == x && button.getY() == y) return button;
        }
        throw new AssertionError("Enhance button not found at " + x + "," + y);
    }

    private static boolean enhanceButtonActive(ClientGameTestContext context) {
        return context.computeOnClient(client -> enhanceButton((T1AnvilScreen) client.gui.screen()).active);
    }

    /** Presses the real Enhance button; an active button must be reachable, an inactive one is pressed anyway. */
    private static void clickEnhance(ClientGameTestContext context, boolean expectActive) {
        double[] point = context.computeOnClient(client -> {
            T1AnvilScreen screen = (T1AnvilScreen) client.gui.screen();
            Button button = enhanceButton(screen);
            if (expectActive && !button.active) throw new AssertionError("Enhance button is not active");
            double guiScale = client.getWindow().getGuiScale();
            return new double[]{(button.getX() + button.getWidth() / 2.0) * guiScale,
                    (button.getY() + button.getHeight() / 2.0) * guiScale};
        });
        click(context, point);
        context.waitTicks(5);
    }

    /** The catalyst slot must be inside the panel, free of overlaps, and genuinely clickable with the mouse. */
    private static void verifyGeometry(ClientGameTestContext context, int scale) {
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
            System.out.println("ANVIL_LAYOUT scale=" + scale + " slot=" + catalyst.x + "," + catalyst.y
                    + " panel=" + imageWidth + "x" + imageHeight + " insidePanel=" + inside + " onScreen=" + onScreen
                    + " overlaps=" + overlaps);
            if (!inside || !onScreen || !overlaps.isEmpty()) {
                throw new AssertionError("[scale " + scale + "] catalyst slot is outside the panel or overlaps: "
                        + overlaps);
            }
        });
    }

    /** Proves the slot is clickable: pick the catalyst up with the cursor, then put it back. */
    private static void pickUpAndReturnCatalyst(ClientGameTestContext context, int scale) {
        int catalystIndex = context.computeOnClient(client ->
                ((T1AnvilModes) client.player.containerMenu).moreminecrafts$catalystSlotIndex());
        click(context, slotCenter(context, catalystIndex));
        context.runOnClient(client -> {
            if (client.player.containerMenu.getCarried().getItem() != ModItems.RELIC_CATALYST) {
                throw new AssertionError("[scale " + scale + "] catalyst slot was not clickable");
            }
        });
        // Wait past the 250ms double-click window so the second click places instead of collecting.
        context.waitTicks(10);
        context.getInput().pressMouse(SDL_MOUSE_BUTTON_LEFT);
        context.waitTicks(5);
        context.runOnClient(client -> {
            var menu = client.player.containerMenu;
            if (!menu.getCarried().isEmpty() || menu.getSlot(catalystIndex).getItem().getItem() != ModItems.RELIC_CATALYST) {
                throw new AssertionError("[scale " + scale + "] catalyst did not return to its slot carried="
                        + menu.getCarried() + " slot=" + menu.getSlot(catalystIndex).getItem());
            }
        });
        System.out.println("ANVIL_CATALYST_CLICK_OK scale=" + scale);
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
