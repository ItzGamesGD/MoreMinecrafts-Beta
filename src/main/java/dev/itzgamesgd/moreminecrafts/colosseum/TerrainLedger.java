package dev.itzgamesgd.moreminecrafts.colosseum;
import java.util.*;
/** Exact prior block-state strings; completed/conflicting positions never get restored twice (Q91/Q118). */
final class TerrainLedger {
    final Map<Long,String> pending = new LinkedHashMap<>();
    final Set<Long> conflicts = new LinkedHashSet<>();
    void record(long pos, String state) { if (!conflicts.contains(pos)) pending.putIfAbsent(pos,state); }
    String restore(long pos, boolean empty) {
        String state = pending.remove(pos);
        if (state == null) return null;
        if (!empty) { conflicts.add(pos); return null; }
        return state;
    }
}
