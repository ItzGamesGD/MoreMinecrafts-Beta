package dev.itzgamesgd.moreminecrafts.ability;

import com.mojang.serialization.Codec;

import java.util.HashMap;
import java.util.Map;

/** Immutable, player-owned ready-at ticks keyed by stable ability identifier strings. */
public record AbilityCooldowns(Map<String, Long> readyAtTicks) {
    public static final AbilityCooldowns EMPTY = new AbilityCooldowns(Map.of());
    public static final Codec<AbilityCooldowns> CODEC = Codec.unboundedMap(Codec.STRING, Codec.LONG)
            .xmap(AbilityCooldowns::new, AbilityCooldowns::readyAtTicks);

    public AbilityCooldowns {
        readyAtTicks = Map.copyOf(readyAtTicks);
    }

    public boolean isReady(String abilityId, long currentTick) {
        return remainingTicks(abilityId, currentTick) == 0;
    }

    public long remainingTicks(String abilityId, long currentTick) {
        return Math.max(0L, readyAtTicks.getOrDefault(abilityId, currentTick) - currentTick);
    }

    /** Replaces this ability's previous deadline. Non-positive durations leave it ready. */
    public AbilityCooldowns start(String abilityId, long currentTick, long cooldownTicks) {
        var updated = new HashMap<>(readyAtTicks);
        if (cooldownTicks <= 0) {
            updated.remove(abilityId);
        } else {
            updated.put(abilityId, Math.addExact(currentTick, cooldownTicks));
        }
        return new AbilityCooldowns(updated);
    }
}
