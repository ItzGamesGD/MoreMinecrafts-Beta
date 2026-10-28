package dev.itzgamesgd.moreminecrafts.progression;

import dev.itzgamesgd.moreminecrafts.client.EnchantingProgressionScreen;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/**
 * Diagnostic only (never fails on layout): measures the progression enchanting screen at GUI scales 1-4 and takes
 * screenshots, so the reported GUI-scale-1 usability problem is documented with real-client evidence instead of
 * guesses. Writes ENCHANT_UI lines that the workflow uploads with the other logs.
 */
@SuppressWarnings("UnstableApiUsage")
public final class EnchantingGuiScaleDiagnosticClientGameTest implements FabricClientGameTest {
    private static final int SDL_MOUSE_BUTTON_LEFT = 1;

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestDedicatedServerContext server = context.worldBuilder().createServer();
             TestDedicatedServerConnection connection = server.connect()) {
            context.waitFor(client -> client.player != null && client.level != null
                    && client.getConnection() != null && client.getConnection().getConnection().isConnected());
            context.waitTicks(200);
            try {
                for (int scale : List.of(1, 2, 3, 4)) {
                    context.runOnClient(client -> {
                        client.gui.setScreen(null);
                        client.options.guiScale().set(scale);
                    });
                    context.waitTick();
                    open(context, server, connection);
                    measure(context, scale, "tree");
                    capture(context, "enchant-scale-" + scale + "-tree");
                    click(context, tabCenter(context, Component.translatable("menu.moreminecrafts.book_option").getString()));
                    context.waitTicks(8);
                    measure(context, scale, "book-options");
                    capture(context, "enchant-scale-" + scale + "-book-options");
                    context.runOnClient(client -> client.gui.setScreen(null));
                    context.waitForScreen(null);
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

    private static void open(ClientGameTestContext context, TestDedicatedServerContext server,
                             TestDedicatedServerConnection connection) {
        server.runOnServer(minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            player.setGameMode(GameType.SURVIVAL);
            player.closeContainer();
            player.getInventory().clearContent();
            player.giveExperiencePoints(100_000);
            var world = player.level();
            BlockPos pos = player.blockPosition().east(2);
            world.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
            world.setBlock(pos, Blocks.ENCHANTING_TABLE.defaultBlockState(), 3);
            player.openMenu(Blocks.ENCHANTING_TABLE.defaultBlockState().getMenuProvider(world, pos));
            player.containerMenu.getSlot(0).set(new ItemStack(Items.DIAMOND_SWORD));
            player.containerMenu.broadcastChanges();
        });
        connection.waitForClientboundPackets();
        context.waitForScreen(EnchantingProgressionScreen.class);
        context.waitTicks(10);
    }

    private static void measure(ClientGameTestContext context, int scale, String mode) {
        context.runOnClient(client -> {
            EnchantingProgressionScreen screen = (EnchantingProgressionScreen) client.gui.screen();
            int guiW = client.getWindow().getGuiScaledWidth();
            int guiH = client.getWindow().getGuiScaledHeight();
            int left = intField(screen, "leftPos");
            int top = intField(screen, "topPos");
            int width = intField(screen, "imageWidth");
            int height = intField(screen, "imageHeight");
            List<String> widgetsOffScreen = new ArrayList<>();
            int smallestButtonPixels = Integer.MAX_VALUE;
            int visibleButtons = 0;
            for (var child : screen.children()) {
                if (!(child instanceof AbstractWidget widget) || !widget.visible) continue;
                visibleButtons++;
                smallestButtonPixels = Math.min(smallestButtonPixels, Math.min(widget.getWidth(), widget.getHeight()) * scale);
                if (widget.getX() < 0 || widget.getY() < 0 || widget.getX() + widget.getWidth() > guiW
                        || widget.getY() + widget.getHeight() > guiH) {
                    widgetsOffScreen.add(widget.getMessage().getString() + "@" + widget.getX() + "," + widget.getY());
                }
            }
            List<Integer> slotsOffScreen = new ArrayList<>();
            for (Slot slot : client.player.containerMenu.slots) {
                if (!slot.isActive()) continue;
                int x = left + slot.x;
                int y = top + slot.y;
                if (x < 0 || y < 0 || x + 16 > guiW || y + 16 > guiH) slotsOffScreen.add(slot.index);
            }
            System.out.println("ENCHANT_UI scale=" + scale + " mode=" + mode + " window=" + client.getWindow().getWidth()
                    + "x" + client.getWindow().getHeight() + " gui=" + guiW + "x" + guiH
                    + " panel=" + width + "x" + height + " at " + left + "," + top
                    + " panelFits=" + (left >= 0 && top >= 0 && left + width <= guiW && top + height <= guiH)
                    + " panelPhysicalPx=" + (width * scale) + "x" + (height * scale)
                    + " visibleWidgets=" + visibleButtons + " smallestWidgetPhysicalPx=" + smallestButtonPixels
                    + " widgetsOffScreen=" + widgetsOffScreen + " slotsOffScreen=" + slotsOffScreen);
        });
    }

    private static double[] tabCenter(ClientGameTestContext context, String label) {
        return context.computeOnClient(client -> {
            EnchantingProgressionScreen screen = (EnchantingProgressionScreen) client.gui.screen();
            double guiScale = client.getWindow().getGuiScale();
            for (var child : screen.children()) {
                if (child instanceof Button button && button.getMessage().getString().equals(label)) {
                    return new double[]{(button.getX() + button.getWidth() / 2.0) * guiScale,
                            (button.getY() + button.getHeight() / 2.0) * guiScale};
                }
            }
            throw new AssertionError("Enchanting tab not found: " + label);
        });
    }

    private static void click(ClientGameTestContext context, double[] point) {
        context.getInput().setCursorPos(point[0], point[1]);
        context.getInput().pressMouse(SDL_MOUSE_BUTTON_LEFT);
        context.waitTicks(5);
    }

    private static void capture(ClientGameTestContext context, String name) {
        context.getInput().setCursorPos(2, 2);
        context.waitTick();
        context.takeScreenshot(name);
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
