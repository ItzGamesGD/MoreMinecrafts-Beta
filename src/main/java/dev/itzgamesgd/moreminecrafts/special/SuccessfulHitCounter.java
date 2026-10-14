package dev.itzgamesgd.moreminecrafts.special;

/**
 * Immutable, runtime-storage-neutral progress for an every-N-successful-hits passive.
 * Callers are responsible for recording only one server-confirmed successful hit.
 */
public record SuccessfulHitCounter(int hitsTowardTrigger) {
    public static final SuccessfulHitCounter EMPTY = new SuccessfulHitCounter(0);

    public SuccessfulHitCounter {
        if (hitsTowardTrigger < 0) {
            throw new IllegalArgumentException("Hit progress cannot be negative");
        }
    }

    public Advance recordSuccessfulHit(int threshold) {
        if (threshold <= 0) {
            throw new IllegalArgumentException("Hit threshold must be positive");
        }
        if (hitsTowardTrigger >= threshold) {
            throw new IllegalStateException("Hit progress must remain below its threshold");
        }

        int next = hitsTowardTrigger + 1;
        boolean triggered = next == threshold;
        return new Advance(triggered ? EMPTY : new SuccessfulHitCounter(next), triggered);
    }

    public record Advance(SuccessfulHitCounter counter, boolean triggered) {
    }
}
