package dev.itzgamesgd.moreminecrafts.ability;

import com.mojang.serialization.Codec;

import java.util.HashMap;
import java.util.Map;

/** Immutable, persistent player cooldowns: legacy abilities use deadlines; accessory keys use remaining online ticks. */
public record AbilityCooldowns(Map<String, Long> readyAtTicks) {
    private static final String ACCESSORY_PREFIX = "moreminecrafts:accessory/";
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

    /** Accessory entries store remaining online ticks in the existing persistent player attachment. */
    public long onlineRemaining(String abilityId) {
        requireAccessory(abilityId);
        return Math.max(0L, readyAtTicks.getOrDefault(abilityId, 0L));
    }

    public AbilityCooldowns startOnline(String abilityId, long ticks) {
        requireAccessory(abilityId);
        var updated = new HashMap<>(readyAtTicks);
        if (ticks <= 0) updated.remove(abilityId);
        else updated.put(abilityId, ticks);
        return new AbilityCooldowns(updated);
    }

    public AbilityCooldowns tickOnline() {
        if (readyAtTicks.keySet().stream().noneMatch(key -> key.startsWith(ACCESSORY_PREFIX))) return this;
        var updated = new HashMap<>(readyAtTicks);
        readyAtTicks.forEach((key, ticks) -> {
            if (key.startsWith(ACCESSORY_PREFIX)) {
                if (ticks <= 1) updated.remove(key);
                else updated.put(key, ticks - 1);
            }
        });
        return new AbilityCooldowns(updated);
    }

    private static void requireAccessory(String abilityId) {
        if (!abilityId.startsWith(ACCESSORY_PREFIX)) throw new IllegalArgumentException("Not an accessory ability ID");
    }
}
