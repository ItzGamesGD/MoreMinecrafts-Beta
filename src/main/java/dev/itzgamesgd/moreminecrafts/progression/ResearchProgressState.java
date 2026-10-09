package dev.itzgamesgd.moreminecrafts.progression;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;

/** Per-player, saved and copied-on-death progression counters, knowledge, and paid unlocks. */
public record ResearchProgressState(Map<String, Integer> counters) {
    public static final Codec<ResearchProgressState> CODEC = Codec.unboundedMap(Codec.STRING, Codec.INT)
            .xmap(ResearchProgressState::new, ResearchProgressState::counters);

    public ResearchProgressState {
        counters = Map.copyOf(counters);
    }

    public static ResearchProgressState empty() { return new ResearchProgressState(Map.of()); }

    public int get(String key) { return counters.getOrDefault(key, 0); }

    public ResearchProgressState with(String key, int count) {
        var updated = new HashMap<>(counters);
        updated.put(key, count);
        return new ResearchProgressState(updated);
    }
}
