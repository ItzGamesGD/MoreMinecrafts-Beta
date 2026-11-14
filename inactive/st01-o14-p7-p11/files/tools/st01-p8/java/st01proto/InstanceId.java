package st01proto;

/** Structure-instance identity: dimension + anchor (packed block position of the royal chest). Mirrors TombGuardianEncounters.tombKey + per-dimension SavedData. */
public record InstanceId(String dimension, long anchor) {
    @Override public String toString() { return dimension + "@" + anchor; }
}
