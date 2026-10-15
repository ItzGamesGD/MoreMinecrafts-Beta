package dev.itzgamesgd.moreminecrafts.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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

        var saved = server.getPlayerList().loadPlayerData(new NameAndId(playerId, ""));
        if (saved.isEmpty()) return Optional.empty();

        return decodePlayerData(server, saved.orElseThrow());
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
        return state(server, playerId).map(saved -> Math.max(0, saved.researchLevel(researchId))).orElse(0);
    }
}
