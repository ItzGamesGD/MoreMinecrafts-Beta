package dev.itzgamesgd.moreminecrafts.progression;

import dev.itzgamesgd.moreminecrafts.network.ProgressionDeltaPayload;
import dev.itzgamesgd.moreminecrafts.network.ProgressionSnapshotPayload;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/** Private per-player progression synchronization; packets are sent only to their owner. */
public final class ProgressionSyncRuntime {
    private static final Map<UUID, Identifier> LAST_DIMENSION = new HashMap<>();
    private ProgressionSyncRuntime() {}
    public static void initialize() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> snapshot(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                LAST_DIMENSION.remove(handler.player.getUUID()));
        net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.AFTER_RESPAWN.register(
                (oldPlayer, newPlayer, alive) -> snapshot(newPlayer));
        ServerTickEvents.END_SERVER_TICK.register(server -> server.getPlayerList().getPlayers().forEach(player -> {
            Identifier dimension = player.level().dimension().identifier();
            Identifier previous = LAST_DIMENSION.put(player.getUUID(), dimension);
            if (previous != null && !previous.equals(dimension)) snapshot(player);
        }));
    }
    public static void snapshot(ServerPlayer player) {
        ResearchProgressState state = KnowledgeRuntime.state(player);
        ServerPlayNetworking.send(player, new ProgressionSnapshotPayload(
                state.get("progression.revision"), state.counters()));
    }
    public static void delta(ServerPlayer player, ResearchProgressState before, ResearchProgressState after) {
        Map<String,Integer> changes = new HashMap<>();
        after.counters().forEach((key,value) -> {
            if (!java.util.Objects.equals(before.counters().get(key), value)) changes.put(key,value);
        });
        before.counters().keySet().stream().filter(key -> !after.counters().containsKey(key))
                .forEach(key -> changes.put(key,0));
        if (!changes.isEmpty()) ServerPlayNetworking.send(player,
                new ProgressionDeltaPayload(after.get("progression.revision"), changes));
    }
    public static void actionResult(ServerPlayer player, String actionId, int resultCode) {
        ServerPlayNetworking.send(player, new dev.itzgamesgd.moreminecrafts.network.ProgressionActionResultPayload(
                actionId, resultCode, KnowledgeRuntime.state(player).get("progression.revision")));
    }
}
