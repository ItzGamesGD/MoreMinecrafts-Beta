package dev.itzgamesgd.moreminecrafts.progression;

/** Shared nonzero-deadline check for the short wall-jump and fall-roll windows. */
public final class ResearchMovementRules {
    private ResearchMovementRules() {}
    public static boolean inGraceWindow(long now, long deadline) {
        return deadline > 0L && deadline >= now;
    }
}
