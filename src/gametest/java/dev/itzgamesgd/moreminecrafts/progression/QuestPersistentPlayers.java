package dev.itzgamesgd.moreminecrafts.progression;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueInput;

/** Joins fixed-UUID players, optionally restoring their saved record from disk (two-process restart tests). */
final class QuestPersistentPlayers {
    private QuestPersistentPlayers() {}

    /** A server-connected mock player in {@code level} whose game mode is fixed to {@code mode}. */
    static ServerPlayer mock(ServerLevel level, GameType mode) {
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(
                new GameProfile(UUID.randomUUID(), "xpq-mock-" + mode.getName()), false);
        ServerPlayer player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation()) {
            @Override public GameType gameMode() { return mode; }
        };
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        return player;
    }

    static ServerPlayer join(GameTestHelper ctx, UUID id, String name, boolean loadFromDisk) {
        ServerLevel level = ctx.getLevel();
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(id, name), false);
        ServerPlayer player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation());
        if (loadFromDisk) {
            var saved = level.getServer().getPlayerList().loadPlayerData(player.nameAndId());
            ctx.assertTrue(saved.isPresent(), "player data for " + name + " was saved by the first JVM");
            player.load(TagValueInput.create(ProblemReporter.DISCARDING, player.registryAccess(), saved.orElseThrow()));
        }
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        return player;
    }
}
