package dev.itzgamesgd.moreminecrafts.accessory;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Real key edges, remote authority, client motion and native fall damage on a dedicated server. */
public final class T1AccessoryClientGameTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        try (var server = context.worldBuilder().createServer(); var connection = server.connect()) {
            context.waitFor(client -> client.player != null && client.level != null && client.gui.screen() == null);
            server.runOnServer(mc -> {
                var player = mc.getPlayerList().getPlayers().getFirst();
                var level = player.level();
                for (int x = 0; x <= 16; x++) for (int z = 0; z <= 16; z++)
                    level.setBlock(new BlockPos(x, 80, z), Blocks.STONE.defaultBlockState(), 3);
                player.setGameMode(GameType.SURVIVAL);
                player.getAbilities().flying = false;
                player.getAbilities().invulnerable = false;
                player.onUpdateAbilities();
                player.getFoodData().setFoodLevel(17);
                T1AccessoryGameTests.equip(player, AccessoryDefinition.T1_DOUBLE_JUMP, 0);
                player.connection.teleport(8.5, 81, 8.5, 0, 0);
            });
            connection.waitForClientboundPackets();
            context.waitFor(client -> client.player.onGround());
            // First physical press must stay a normal ground jump.
            context.getInput().holdKeyFor(options -> options.keyJump, 1);
            context.waitTicks(3);
            server.runOnServer(mc -> {
                var player = mc.getPlayerList().getPlayers().getFirst();
                if (AccessoryCooldownRuntime.remaining(player, AccessoryDefinition.T1_DOUBLE_JUMP) != 0)
                    throw new AssertionError("ground jump consumed air jump");
            });
            context.getInput().holdKeyFor(options -> options.keyJump, 1);
            context.waitTicks(3);
            server.runOnServer(mc -> {
                var player = mc.getPlayerList().getPlayers().getFirst();
                long remaining = AccessoryCooldownRuntime.remaining(player, AccessoryDefinition.T1_DOUBLE_JUMP);
                if (remaining <= 590 || remaining > 600) throw new AssertionError("air key did not start server cooldown: " + remaining);
            });
            context.waitFor(client -> client.player.onGround());
            // A falling remote player with large prior fallDistance must start a new native fall at activation.
            server.runOnServer(mc -> {
                var player = mc.getPlayerList().getPlayers().getFirst();
                T1AccessoryRuntime.onDeath(player); // reset the test ability, preserving old T2 entries
                player.setHealth(20);
                player.setInvulnerableTime(0);
                player.connection.teleport(8.5, 91, 8.5, 0, 0);
            });
            connection.waitForClientboundPackets();
            context.waitTicks(3);
            context.runOnClient(client -> { client.player.fallDistance = 25; client.player.setDeltaMovement(Vec3.ZERO); });
            server.runOnServer(mc -> mc.getPlayerList().getPlayers().getFirst().fallDistance = 25);
            double start = context.computeOnClient(client -> client.player.getY());
            context.getInput().holdKeyFor(options -> options.keyJump, 1);
            double peak = start;
            for (int tick = 0; tick < 14; tick++) {
                context.waitTicks(1);
                peak = Math.max(peak, context.computeOnClient(client -> client.player.getY()));
            }
            double rise = peak - start;
            if (rise < 1.25 || rise > 1.7) throw new AssertionError("remote extra rise out of range: " + rise);
            context.takeScreenshot("d01-double-jump-real-client");
            context.waitFor(client -> client.player.onGround());
            context.waitTicks(4);
            double health = context.computeOnClient(client -> (double) client.player.getHealth());
            if (health <= 0 || health >= 20) throw new AssertionError("expected native fall damage after reset, health=" + health);
            System.out.println("D01_CLIENT_PASS ground_jump_not_consumed=true extra_rise=" + rise + " landed_health=" + health);
        }
    }
}
