package dev.itzgamesgd.moreminecrafts.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Shared server-side research attribution state for block entities (Auto Farm, Fish Trap, Brewing
 * Stand). Actual inventory/device interaction supersedes passive placement or GUI opening, matching
 * {@code FurnaceResearchState}.
 */
public final class BlockEntityResearchAttribution {
    public static final int DATA_VERSION = 1;

    private static final Codec<UUID> UUID_CODEC = Codec.STRING.comapFlatMap(value -> {
        try {
            return DataResult.success(UUID.fromString(value));
        } catch (IllegalArgumentException malformed) {
            return DataResult.error(() -> "Malformed player UUID");
        }
    }, UUID::toString);

    public record State(Optional<UUID> attributedPlayer, boolean actualInteraction) {
        public static final Codec<State> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                UUID_CODEC.optionalFieldOf("attributed_player").forGetter(State::attributedPlayer),
                Codec.BOOL.optionalFieldOf("actual_interaction", false).forGetter(State::actualInteraction)
        ).apply(instance, State::new));

        public State {
            attributedPlayer = attributedPlayer == null ? Optional.empty() : attributedPlayer;
        }

        public static State empty() {
            return new State(Optional.empty(), false);
        }

        public State openedBy(UUID player) {
            if (player == null || actualInteraction) return this;
            return new State(Optional.of(player), false);
        }

        public State interactedBy(UUID player) {
            if (player == null) return this;
            return new State(Optional.of(player), true);
        }
    }

    private BlockEntityResearchAttribution() {}

    public static void save(ValueOutput output, String key, State state) {
        if (state == null || state.equals(State.empty())) return;
        output.putInt(key + "Version", DATA_VERSION);
        output.store(key, State.CODEC, state);
    }

    public static State load(ValueInput input, String key) {
        if (input.getIntOr(key + "Version", 0) != DATA_VERSION) return State.empty();
        return input.read(key, State.CODEC).orElse(State.empty());
    }

    public static int resolveLevel(MinecraftServer server, State state, String researchId) {
        if (server == null || state == null || state.attributedPlayer().isEmpty()) return 0;
        return ResearchProgressLookup.researchLevel(server, state.attributedPlayer().get(), researchId);
    }
}
