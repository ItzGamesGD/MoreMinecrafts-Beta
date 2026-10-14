package dev.itzgamesgd.moreminecrafts.progression;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;

/** Per-player, saved and copied-on-death progression counters, knowledge, and paid unlocks. */
public record ResearchProgressState(Map<String, Integer> counters) {
    /** Keep the flat map codec so saves written by the original V4 attachment remain readable. */
    public static final Codec<ResearchProgressState> CODEC = Codec.unboundedMap(Codec.STRING, Codec.INT)
            .xmap(ResearchProgressState::new, ResearchProgressState::counters);

    public ResearchProgressState {
        counters = Map.copyOf(counters);
    }

    public static ResearchProgressState empty() { return new ResearchProgressState(Map.of()); }

    public int get(String key) { return counters.getOrDefault(key, 0); }

    public boolean hasKnowledge(int number) { return number >= 1 && number <= 70 && get("knowledge." + number) > 0; }
    public int researchLevel(String id) { return Math.max(0, get("research." + id)); }
    public int conditionProgress(String id) { return Math.max(0, get("condition." + id)); }
    public int questProgress(String id) { return Math.max(0, get("quest.progress." + id)); }
    public boolean questComplete(String id) { return get("quest.complete." + id) > 0; }
    public boolean questClaimed(String id) { return get("quest.claimed." + id) > 0; }

    public ResearchProgressState addProgress(String id, int amount, int maximum) {
        if (amount <= 0 || maximum <= 0) return this;
        int current = conditionProgress(id);
        return with("condition." + id, (int) Math.min(maximum, (long) current + amount));
    }

    public ResearchProgressState withQuestProgress(String id, int progress) {
        return with("quest.progress." + id, Math.max(0, progress));
    }

    public ResearchProgressState completeQuest(String id) {
        return questClaimed(id) ? this : with("quest.complete." + id, 1);
    }

    public ResearchProgressState claimQuest(String id) {
        return questComplete(id) ? with("quest.claimed." + id, 1) : this;
    }

    public ResearchProgressState with(String key, int count) {
        var updated = new HashMap<>(counters);
        updated.put(key, count);
        return new ResearchProgressState(updated);
    }
}
