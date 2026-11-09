package dev.itzgamesgd.moreminecrafts.bounty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Recent actual health loss by player, scoped to one living mob. */
public final class DamageCredit {
    public static final int WINDOW_TICKS = 600;
    private record Hit(UUID player, int tick, float damage) {}
    private final Map<UUID, List<Hit>> hits = new HashMap<>();

    public void record(UUID mob, UUID player, int tick, float actualDamage, float maxHealth) {
        if (mob == null || player == null || actualDamage <= 0 || maxHealth <= 0) return;
        hits.computeIfAbsent(mob, ignored -> new ArrayList<>())
                .add(new Hit(player, tick, Math.min(actualDamage, maxHealth)));
    }

    public Set<UUID> onDeath(UUID mob, UUID finalBlow, int tick, float maxHealth) {
        Set<UUID> credited = new HashSet<>();
        if (finalBlow != null) credited.add(finalBlow);
        List<Hit> recent = hits.remove(mob);
        if (recent == null || maxHealth <= 0) return credited;
        Map<UUID, Float> totals = new HashMap<>();
        for (Hit hit : recent)
            if (hit.tick <= tick && tick - hit.tick <= WINDOW_TICKS)
                totals.merge(hit.player, hit.damage, Float::sum);
        for (var entry : totals.entrySet())
            if (entry.getValue() >= maxHealth * 0.2F) credited.add(entry.getKey());
        return Set.copyOf(credited);
    }

    public void expire(int tick) {
        hits.values().forEach(list -> list.removeIf(hit -> tick - hit.tick > WINDOW_TICKS));
        hits.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }
}
