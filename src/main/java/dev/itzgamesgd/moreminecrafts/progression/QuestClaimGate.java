package dev.itzgamesgd.moreminecrafts.progression;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Client request suppression only. The existing server transaction remains the reward authority. */
public final class QuestClaimGate {
    private static final long TIMEOUT_MS = 5_000;
    private final Map<String, Long> pending = new HashMap<>();

    public boolean begin(String id, boolean complete, boolean claimed, long now) {
        if (id == null || id.isBlank() || !complete || claimed || pending(id, now)) return false;
        pending.put(id, now);
        return true;
    }
    public boolean pending(String id, long now) {
        Long started = pending.get(id);
        if (started != null && now - started >= TIMEOUT_MS) pending.remove(id);
        return pending.containsKey(id);
    }
    public void result(String id) { pending.remove(id); }
    public void synchronizedState(Map<String, Integer> values) {
        pending.keySet().removeIf(id -> values.getOrDefault("quest.claimed." + id, 0) > 0);
    }
    public void reset() { pending.clear(); }

    /** Called only after an ordered delta was applied, never for snapshots or stale/gapped deltas. */
    public static List<String> newCompletions(Map<String, Integer> before, Map<String, Integer> changes) {
        String prefix = "quest.complete.";
        return changes.entrySet().stream()
                .filter(e -> e.getKey().startsWith(prefix) && e.getValue() > 0
                        && before.getOrDefault(e.getKey(), 0) == 0)
                .map(e -> e.getKey().substring(prefix.length())).sorted().toList();
    }
}
