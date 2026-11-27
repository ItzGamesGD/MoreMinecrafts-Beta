package dev.itzgamesgd.moreminecrafts.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.HashMap;
import java.util.WeakHashMap;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.util.ProblemReporter;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;

/** Reads the existing persistent player attachment without constructing an offline ServerPlayer. */
public final class ResearchProgressLookup {
    private static final int OFFLINE_CACHE_TICKS = 200;
    /** Server-thread-only, short-lived snapshots; the player attachment remains the sole saved authority. */
    private static final Map<MinecraftServer, Map<UUID, CachedState>> OFFLINE_CACHE = new WeakHashMap<>();
    private record CachedState(Optional<ResearchProgressState> state, long expiresAt) {}
    private static final Codec<Map<Identifier, Dynamic<?>>> SAVED_ATTACHMENTS =
            Codec.unboundedMap(Identifier.CODEC, Codec.PASSTHROUGH);

    private ResearchProgressLookup() {}

    /**
     * Resolves the authoritative progression attachment on the server thread. Online players use
     * their live attachment. Offline players are decoded from Minecraft's normal player-data file
     * with the same attachment codec and key used by Fabric persistence.
     */
    public static Optional<ResearchProgressState> state(MinecraftServer server, UUID playerId) {
        if (server == null || playerId == null || !server.isSameThread()) return Optional.empty();
        var online = server.getPlayerList().getPlayer(playerId);
        if (online != null) return Optional.of(KnowledgeRuntime.state(online));

        var byPlayer = OFFLINE_CACHE.computeIfAbsent(server, ignored -> new HashMap<>());
        CachedState cached = byPlayer.get(playerId);
        if (cached != null && cached.expiresAt() >= server.getTickCount()) return cached.state();

        Optional<ResearchProgressState> resolved;
        try {
            var saved = server.getPlayerList().loadPlayerData(new NameAndId(playerId, ""));
            resolved = saved.flatMap(data -> decodePlayerData(server, data));
        } catch (RuntimeException malformedPlayerData) {
            resolved = Optional.empty();
        }
        byPlayer.put(playerId, new CachedState(resolved, (long) server.getTickCount() + OFFLINE_CACHE_TICKS));
        return resolved;
    }

    public static void playerJoined(MinecraftServer server, UUID playerId) {
        if (server != null && server.isSameThread() && playerId != null)
            OFFLINE_CACHE.computeIfAbsent(server, ignored -> new HashMap<>()).remove(playerId);
    }

    public static void playerDisconnected(MinecraftServer server, net.minecraft.server.level.ServerPlayer player) {
        if (server != null && server.isSameThread() && player != null)
            OFFLINE_CACHE.computeIfAbsent(server, ignored -> new HashMap<>()).put(player.getUUID(),
                    new CachedState(Optional.of(KnowledgeRuntime.state(player)),
                            (long) server.getTickCount() + OFFLINE_CACHE_TICKS));
    }

    public static Optional<ResearchProgressState> decodePlayerData(MinecraftServer server, CompoundTag playerData) {
        if (server == null || playerData == null || !server.isSameThread()) return Optional.empty();
        ValueInput input = TagValueInput.create(ProblemReporter.DISCARDING, server.registryAccess(), playerData);
        Map<Identifier, Dynamic<?>> attachments = input
                .read(AttachmentTarget.NBT_ATTACHMENT_KEY, SAVED_ATTACHMENTS)
                .orElse(Map.of());
        Dynamic<?> encodedState = attachments.get(KnowledgeRuntime.STATE.identifier());
        if (encodedState == null) return Optional.empty();
        var codec = KnowledgeRuntime.STATE.persistenceCodec();
        return codec == null ? Optional.empty()
                : codec.parse(encodedState).result();
    }

    /** Returns zero for missing, malformed, or unpurchased saved data; it never invents a level. */
    public static int researchLevel(MinecraftServer server, UUID playerId, String researchId) {
        if (researchId == null || researchId.isBlank()) return 0;
        ResearchDefinition definition = ResearchCatalog.byId(researchId);
        if (definition == null) return 0;
        return state(server, playerId)
                .map(saved -> Math.min(definition.maximumLevel(), saved.researchLevel(researchId)))
                .orElse(0);
    }
}
