package dev.itzgamesgd.moreminecrafts.pyramid.entrance;

/** Progress of one structure instance's secret entrance (P7 state machine). Progress belongs to the INSTANCE, never to a player. */
public enum EntranceState {
    /** Before the Tomb Guardian is defeated. */
    SEALED,
    /** Tomb Guardian defeat recorded; the fallback timer is running. */
    GUARDIAN_DEFEATED,
    /** The gold guide golem exists and is leading the way. */
    GOLEM_GUIDING,
    /** Opened because the golem reached the arrival point. Terminal. */
    ENTRANCE_OPEN,
    /** Opened because the fallback time ran out. Terminal. */
    FALLBACK_OPEN;

    public boolean isOpen() { return this == ENTRANCE_OPEN || this == FALLBACK_OPEN; }
}
