package dev.itzgamesgd.moreminecrafts.progression;

import java.util.HashMap;
import java.util.Map;

/** Keeps duplicate client requests locked until the server result and its state revision arrive. */
public final class ProgressionPurchaseGate {
    private record Pending(int responseRevision) {}
    private final Map<String, Pending> pending = new HashMap<>();
    private int synchronizedRevision = -1;

    public synchronized boolean begin(String actionId, int currentRevision) {
        if (actionId == null || actionId.isBlank() || pending.containsKey(actionId)) return false;
        synchronizedRevision = Math.max(synchronizedRevision, currentRevision);
        pending.put(actionId, new Pending(-1));
        return true;
    }

    public synchronized void actionResult(String actionId, int responseRevision) {
        if (!pending.containsKey(actionId)) return;
        pending.put(actionId, new Pending(responseRevision));
        releaseSynchronized();
    }

    public synchronized void synchronizedAt(int revision) {
        synchronizedRevision = Math.max(synchronizedRevision, revision);
        releaseSynchronized();
    }

    public synchronized void reset() {
        pending.clear();
        synchronizedRevision = -1;
    }

    public synchronized boolean pending(String actionId) { return pending.containsKey(actionId); }

    private void releaseSynchronized() {
        pending.entrySet().removeIf(entry -> entry.getValue().responseRevision() >= 0
                && synchronizedRevision >= entry.getValue().responseRevision());
    }
}
