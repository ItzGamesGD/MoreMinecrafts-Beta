package dev.itzgamesgd.moreminecrafts.accessory;

import dev.itzgamesgd.moreminecrafts.client.T1AnvilScreen;
import dev.itzgamesgd.moreminecrafts.registry.ModDataComponents;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/**
 * Captures the integrated accessory Enhancement interface in the real anvil screen: a +4 accessory ready for the
 * +4 to +5 milestone (stones, catalyst and raw XP present), and a +10 accessory at its maximum.
 */
@SuppressWarnings("UnstableApiUsage")
public final class AccessoryAnvilClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestDedicatedServerContext server = context.worldBuilder().createServer();
             TestDedicatedServerConnection connection = server.connect()) {
            context.waitFor(client -> client.player != null && client.level != null
                    && client.getConnection() != null && client.getConnection().getConnection().isConnected());
            context.runOnClient(client -> client.options.guiScale().set(3));
            context.waitTicks(200);
            capture(context, server, connection, 4, "anvil-accessory-plus4-to-plus5");
            capture(context, server, connection, 10, "anvil-accessory-plus10-maximum");
            context.runOnClient(client -> {
                client.gui.setScreen(null);
                client.options.guiScale().set(0);
            });
            context.waitForScreen(null);
        }
    }

    private static void capture(ClientGameTestContext context, TestDedicatedServerContext server,
                                TestDedicatedServerConnection connection, int level, String name) {
        server.runOnServer(minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            player.setGameMode(GameType.SURVIVAL);
            player.closeContainer();
            player.giveExperiencePoints(100_000);
            var world = player.level();
            BlockPos pos = player.blockPosition().above(2);
            world.setBlock(pos, Blocks.ANVIL.defaultBlockState(), 3);
            player.openMenu(Blocks.ANVIL.defaultBlockState().getMenuProvider(world, pos));
            AnvilMenu menu = (AnvilMenu) player.containerMenu;
            ItemStack charm = new ItemStack(ModItems.ACCESSORIES.get(AccessoryDefinition.SNOW_CHARM));
            charm.set(ModDataComponents.ENHANCEMENT_LEVEL, level);
            menu.getSlot(0).set(charm);
            menu.getSlot(1).set(new ItemStack(ModItems.ENHANCEMENT_STONE, 16));
            var modes = (dev.itzgamesgd.moreminecrafts.progression.T1AnvilModes) menu;
            menu.getSlot(modes.moreminecrafts$catalystSlotIndex()).set(new ItemStack(ModItems.RELIC_CATALYST, 4));
            menu.broadcastChanges();
        });
        connection.waitForClientboundPackets();
        context.waitForScreen(T1AnvilScreen.class);
        context.waitTicks(10);
        context.getInput().setCursorPos(2, 2);
        context.waitTick();
        context.takeScreenshot(name);
        context.runOnClient(client -> client.gui.setScreen(null));
        context.waitForScreen(null);
    }
}
