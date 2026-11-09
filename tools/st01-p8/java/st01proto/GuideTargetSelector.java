package st01proto;

import java.util.*;

/** Candidates for "whom does the golem follow" (decision O-10). Offline players are never candidates; participants only (presence alone is not evidence). */
public interface GuideTargetSelector {
    record P(UUID id, double x, double z, boolean online) {}
    Optional<UUID> choose(double golemX, double golemZ, List<P> participants);

    /** Always the nearest online participant. */
    final class Nearest implements GuideTargetSelector {
        public Optional<UUID> choose(double gx, double gz, List<P> ps) {
            return ps.stream().filter(P::online).min(Comparator.comparingDouble(p -> Math.hypot(p.x() - gx, p.z() - gz))).map(P::id);
        }
    }
    /** Keeps the current target until another online participant is closer by a margin (TEST-ONLY margin). */
    final class StickyNearest implements GuideTargetSelector {
        private UUID current; private final double margin;
        public StickyNearest(double margin) { this.margin = margin; }
        public Optional<UUID> choose(double gx, double gz, List<P> ps) {
            List<P> on = ps.stream().filter(P::online).toList(); if (on.isEmpty()) { current = null; return Optional.empty(); }
            P best = on.stream().min(Comparator.comparingDouble(p -> Math.hypot(p.x() - gx, p.z() - gz))).get();
            P cur = on.stream().filter(p -> p.id().equals(current)).findFirst().orElse(null);
            if (cur == null || Math.hypot(best.x() - gx, best.z() - gz) + margin < Math.hypot(cur.x() - gx, cur.z() - gz)) current = best.id();
            return Optional.of(current);
        }
    }
    /** Follows the farthest online participant (nobody gets left behind; the golem waits at the rear). */
    final class Farthest implements GuideTargetSelector {
        public Optional<UUID> choose(double gx, double gz, List<P> ps) {
            return ps.stream().filter(P::online).max(Comparator.comparingDouble(p -> Math.hypot(p.x() - gx, p.z() - gz))).map(P::id);
        }
    }
}
