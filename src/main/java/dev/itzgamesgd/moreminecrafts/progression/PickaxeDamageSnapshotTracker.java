package dev.itzgamesgd.moreminecrafts.progression;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.item.Item;

/** Short-lived before/after block-break snapshots, keyed by the active server player. */
public final class PickaxeDamageSnapshotTracker {
    record Snapshot(Item item, int damage, int maximumDamage) {}

    private static final class Frame {
        private final Snapshot snapshot;
        private boolean active = true;

        private Frame(Snapshot snapshot) { this.snapshot = snapshot; }
    }

    public static final class Scope implements AutoCloseable {
        private final PickaxeDamageSnapshotTracker tracker;
        private final UUID playerId;
        private final Frame frame;
        private boolean closed;

        private Scope(PickaxeDamageSnapshotTracker tracker, UUID playerId, Frame frame) {
            this.tracker = tracker;
            this.playerId = playerId;
            this.frame = frame;
        }

        Snapshot complete() {
            return closeAndReturnSnapshot();
        }

        @Override public void close() {
            closeAndReturnSnapshot();
        }

        private Snapshot closeAndReturnSnapshot() {
            if (closed) return null;
            closed = true;
            return tracker.remove(playerId, frame) ? frame.snapshot : null;
        }
    }

    Scope begin(UUID playerId, Item item, int damage, int maximumDamage) {
        if (item == null || maximumDamage <= 0) return new Scope(this, playerId, null);
        Frame frame = new Frame(new Snapshot(item, damage, maximumDamage));
        snapshots.computeIfAbsent(playerId, ignored -> new ArrayDeque<>()).push(frame);
        return new Scope(this, playerId, frame);
    }

    private final Map<UUID, Deque<Frame>> snapshots = new HashMap<>();

    void playerDisconnected(UUID playerId) {
        snapshots.remove(playerId);
    }

    int pendingCount() {
        return snapshots.values().stream().mapToInt(Deque::size).sum();
    }

    private boolean remove(UUID playerId, Frame frame) {
        if (frame == null || !frame.active) return false;
        Deque<Frame> pending = snapshots.get(playerId);
        // A disconnect may already have removed every pending frame.
        if (pending == null) {
            frame.active = false;
            return false;
        }
        boolean removed = pending.removeFirstOccurrence(frame);
        frame.active = false;
        if (pending.isEmpty()) snapshots.remove(playerId);
        return removed;
    }
}
