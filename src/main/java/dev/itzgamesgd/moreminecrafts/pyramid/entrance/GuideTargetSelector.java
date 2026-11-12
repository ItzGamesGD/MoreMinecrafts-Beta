package dev.itzgamesgd.moreminecrafts.pyramid.entrance;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Chooses whom the golem leads/waits for. StickyNearest: keep the current leader until another participant is closer by a margin. */
public final class GuideTargetSelector {
    public record Candidate(UUID id, double distance) {}

    private final double margin;
    private UUID current;

    public GuideTargetSelector(double margin) { this.margin = margin; }

    /** @param online only ONLINE participants (offline players are never candidates). */
    public Optional<UUID> choose(List<Candidate> online) {
        if (online.isEmpty()) { current = null; return Optional.empty(); }
        Candidate best = online.get(0);
        for (Candidate c : online) if (c.distance() < best.distance()) best = c;
        Candidate cur = null;
        for (Candidate c : online) if (c.id().equals(current)) cur = c;
        if (cur == null || best.distance() + margin < cur.distance()) current = best.id();
        return Optional.of(current);
    }
}
