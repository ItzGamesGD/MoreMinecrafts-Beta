package dev.itzgamesgd.moreminecrafts.fishing;

import dev.itzgamesgd.moreminecrafts.client.FishingRodScreen;
import java.lang.reflect.Field;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;

/** Verifies that a real SDL left press activates the fishing equipment screen's close control. */
@SuppressWarnings("UnstableApiUsage")
public final class FishingRodCloseClientGameTest implements FabricClientGameTest {
    private static final int SDL_MOUSE_BUTTON_LEFT = 1;

    @Override
    public void runTest(ClientGameTestContext context) {
        try (var server = context.worldBuilder().createServer();
             TestDedicatedServerConnection connection = server.connect()) {
            context.waitFor(client -> client.player != null && client.level != null
                    && client.getConnection() != null && client.getConnection().getConnection().isConnected());
            context.setScreen(() -> {
                var inventory = Minecraft.getInstance().player.getInventory();
                var menu = new FishingRodMenu(0, inventory);
                return new FishingRodScreen(menu, inventory, Component.literal("Fishing"));
            });
            context.waitForScreen(FishingRodScreen.class);
            double[] closeButton = context.computeOnClient(client -> {
                var screen = (FishingRodScreen) client.gui.screen();
                double scale = client.getWindow().getGuiScale();
                return new double[]{(field(screen, "leftPos") + 315) * scale,
                        (field(screen, "topPos") + 13) * scale};
            });
            context.getInput().setCursorPos(closeButton[0], closeButton[1]);
            context.getInput().pressMouse(SDL_MOUSE_BUTTON_LEFT);
            context.waitForScreen(null);
            System.out.println("FISHING_ROD_CLOSE_LEFT_CLICK_OK");
        }
    }

    private static int field(AbstractContainerScreen<?> screen, String name) {
        try {
            Field field = AbstractContainerScreen.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.getInt(screen);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not read fishing screen position", exception);
        }
    }
}
