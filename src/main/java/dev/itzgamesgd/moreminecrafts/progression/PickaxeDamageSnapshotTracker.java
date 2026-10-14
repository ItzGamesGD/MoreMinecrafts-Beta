package dev.itzgamesgd.moreminecrafts.progression;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.item.Item;

/** Short-lived before/after block-break snapshots, keyed by the active server player. */
final class PickaxeDamageSnapshotTracker {
    record Snapshot(Item item, int damage, int maximumDamage) {}

    private final Map<UUID, Snapshot> snapshots = new HashMap<>();

    void capture(UUID playerId, Item item, int damage, int maximumDamage) {
        snapshots.put(playerId, new Snapshot(item, damage, maximumDamage));
    }

    Snapshot complete(UUID playerId) {
        return snapshots.remove(playerId);
    }

    void cancelBreak(UUID playerId) {
        snapshots.remove(playerId);
    }

    void playerDisconnected(UUID playerId) {
        snapshots.remove(playerId);
    }

    int pendingCount() {
        return snapshots.size();
    }
}
