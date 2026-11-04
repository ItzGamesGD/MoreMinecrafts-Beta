package dev.itzgamesgd.moreminecrafts.accessory;

import com.mojang.authlib.GameProfile;
import dev.itzgamesgd.moreminecrafts.equipment.EquipmentAttachments;
import io.netty.channel.embedded.EmbeddedChannel;
import java.lang.reflect.Method;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;

/** Run write/read in two fresh JVMs against the same playerdata directory. */
public final class T1AccessoryRestartGameTests implements CustomTestMethodInvoker {
    @GameTest public void remainingCooldownsPauseAcrossProcessRestart(GameTestHelper ctx) {
        String phase = System.getProperty("moreminecrafts.d01.restart.phase");
        if (phase == null) { ctx.succeed(); return; }
        var server = ctx.getLevel().getServer();
        var cookie = CommonListenerCookie.createInitial(new GameProfile(
                UUID.fromString("f799496f-797f-4559-bdb6-d7f452914f77"), "d01-restart"), false);
        var player = new ServerPlayer(server, ctx.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        if (phase.equals("read")) {
            var saved = server.getPlayerList().loadPlayerData(player.nameAndId());
            ctx.assertTrue(saved.isPresent(), "previous JVM wrote actual player data");
            player.load(TagValueInput.create(ProblemReporter.DISCARDING, player.registryAccess(), saved.orElseThrow()));
            for (var entry : java.util.Map.of(AccessoryDefinition.T1_CRISIS_ESCAPE, 1799L,
                    AccessoryDefinition.T1_HEAVY_BUFFER, 1199L, AccessoryDefinition.T1_DOUBLE_JUMP, 599L).entrySet()) {
                long remaining = AccessoryCooldownRuntime.remaining(player, entry.getKey());
                ctx.assertTrue(remaining >= entry.getValue() - 1 && remaining <= entry.getValue(),
                        "offline ticks paused for " + entry.getKey() + ": " + remaining);
            }
            ctx.assertTrue(player.getAttachedOrElse(EquipmentAttachments.T1_AIR_JUMP_USED, false),
                    "air-use latch also survives restart until ground contact");
        }
        var connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        server.getPlayerList().placeNewPlayer(connection, player, cookie);
        if (phase.equals("write")) {
            AccessoryCooldownRuntime.start(player, AccessoryDefinition.T1_CRISIS_ESCAPE, 1800);
            AccessoryCooldownRuntime.start(player, AccessoryDefinition.T1_HEAVY_BUFFER, 1200);
            AccessoryCooldownRuntime.start(player, AccessoryDefinition.T1_DOUBLE_JUMP, 600);
            AccessoryCooldownRuntime.tick(player);
            player.setAttached(EquipmentAttachments.T1_AIR_JUMP_USED, true);
        }
        server.getPlayerList().saveAll();
        System.out.println("D01_RESTART_PASS phase=" + phase);
        ctx.succeed();
    }
    @Override public void invokeTestMethod(GameTestHelper ctx, Method method) throws ReflectiveOperationException {
        method.invoke(this, ctx);
    }
}
