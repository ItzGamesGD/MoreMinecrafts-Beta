package dev.itzgamesgd.moreminecrafts.progression;

import java.util.HashMap;
import java.util.Map;

/** Revision checks for private progression snapshots and ordered deltas. */
public final class ProgressionSyncRules {
    public enum Status { APPLIED, STALE, NEED_SNAPSHOT }
    public record State(int revision, Map<String,Integer> values, Status status) {
        public State { values = Map.copyOf(values); }
    }
    private ProgressionSyncRules() {}
    public static State snapshot(int currentRevision, Map<String,Integer> current,
                                 int incomingRevision, Map<String,Integer> incoming) {
        if (incomingRevision < currentRevision) return new State(currentRevision,current,Status.STALE);
        return new State(incomingRevision,incoming,Status.APPLIED);
    }
    public static State delta(int currentRevision, Map<String,Integer> current,
                              int incomingRevision, Map<String,Integer> changes) {
        if (incomingRevision <= currentRevision) return new State(currentRevision,current,Status.STALE);
        if (currentRevision < 0 || incomingRevision != currentRevision + 1)
            return new State(currentRevision,current,Status.NEED_SNAPSHOT);
        Map<String,Integer> updated = new HashMap<>(current);
        changes.forEach((key,value) -> { if (value == 0) updated.remove(key); else updated.put(key,value); });
        return new State(incomingRevision,updated,Status.APPLIED);
    }
}
