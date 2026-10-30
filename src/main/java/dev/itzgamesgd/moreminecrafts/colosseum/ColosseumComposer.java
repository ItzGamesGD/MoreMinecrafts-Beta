package dev.itzgamesgd.moreminecrafts.colosseum;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Draws one R1-R6 composition from the active roster under the approved threat-budget rules:
 * cumulative score eligibility, the normal ceil(85%)-100% fill band, the exceptional floor(105%) band only when
 * no normal composition exists, and otherwise the highest valid total at or below budget. R3 always holds exactly
 * one focal elite plus melee, ranged and special-ability coverage inside its budget.
 * <p>
 * Every reachable total is decided exactly (unbounded-knapsack reachability), so the band choice never depends on
 * random retries. The per-mob preference weights in {@link #weight} are provisional placeholders for the approved
 * qualitative rule (favor low scores early, medium in the middle, strong late, never excluding low scores);
 * their numbers are balance work, not authority.
 */
public final class ColosseumComposer {
    private ColosseumComposer() {}

    public enum Band { NORMAL, EXCEPTIONAL, BELOW_NORMAL }

    public record Pick(ColosseumMob mob, boolean focal) {}

    public record Composition(int round, int participants, int budget, int total, Band band, List<Pick> picks) {
        public int eliteCount() { return (int) picks.stream().filter(p -> p.mob().isElite()).count(); }

        public boolean covers(ColosseumMob.Role role) { return picks.stream().anyMatch(p -> p.mob().hasRole(role)); }
    }

    /** Thrown when the active pool cannot produce any valid composition: a data-validation failure. */
    public static final class InvalidRosterException extends IllegalStateException {
        InvalidRosterException(String message) { super(message); }
    }

    public static Composition compose(int round, int participants, Random random) {
        int budget = ColosseumRules.budget(round, participants);
        int cap = ColosseumRules.scoreCap(round);
        List<ColosseumMob> pool = new ArrayList<>();
        for (ColosseumMob mob : ColosseumMob.values()) if (mob.threat() >= 1 && mob.threat() <= cap) pool.add(mob);
        if (pool.isEmpty()) throw new InvalidRosterException("round " + round + " has an empty eligible pool");

        List<List<ColosseumMob>> mandatorySets = new ArrayList<>();
        List<ColosseumMob> fillPool;
        if (round == 3) {
            List<ColosseumMob> elites = pool.stream().filter(ColosseumMob::isElite).toList();
            fillPool = pool.stream().filter(mob -> !mob.isElite()).toList();
            if (elites.isEmpty() || fillPool.isEmpty())
                throw new InvalidRosterException("round 3 needs an elite and non-elite support");
            for (ColosseumMob elite : elites) mandatorySets.addAll(coverSets(elite, fillPool));
        } else {
            fillPool = pool;
            mandatorySets.add(List.of());
        }

        int ceiling = ColosseumRules.exceptionalFillMax(budget);
        boolean[] reachable = reachable(fillPool, ceiling);
        Candidates normal = new Candidates(), exceptional = new Candidates(), below = new Candidates();
        int bestBelow = -1;
        for (List<ColosseumMob> mandatory : mandatorySets) {
            int fixed = mandatory.stream().mapToInt(ColosseumMob::threat).sum();
            for (int total = Math.max(fixed, 1); total <= ceiling; total++) {
                if (!reachable[total - fixed]) continue;
                if (total >= ColosseumRules.normalFillMin(budget) && total <= budget) normal.add(mandatory, total);
                else if (total > budget) exceptional.add(mandatory, total);
                else if (total >= bestBelow) {
                    if (total > bestBelow) { below.clear(); bestBelow = total; }
                    below.add(mandatory, total);
                }
            }
        }
        Band band;
        Candidates chosen;
        if (!normal.isEmpty()) { band = Band.NORMAL; chosen = normal; }
        else if (!exceptional.isEmpty()) { band = Band.EXCEPTIONAL; chosen = exceptional; }
        else if (!below.isEmpty()) { band = Band.BELOW_NORMAL; chosen = below; }
        else throw new InvalidRosterException("round " + round + " has no valid composition for budget " + budget);

        int index = random.nextInt(chosen.size());
        List<ColosseumMob> mandatory = chosen.sets.get(index);
        int total = chosen.totals.get(index);
        List<Pick> picks = new ArrayList<>();
        for (ColosseumMob mob : mandatory) picks.add(new Pick(mob, round == 3 && mob.isElite()));
        int remaining = total - mandatory.stream().mapToInt(ColosseumMob::threat).sum();
        while (remaining > 0) {
            List<ColosseumMob> options = new ArrayList<>();
            double weightSum = 0.0D;
            for (ColosseumMob mob : fillPool) {
                if (mob.threat() <= remaining && reachable[remaining - mob.threat()]) {
                    options.add(mob);
                    weightSum += weight(round, mob);
                }
            }
            double roll = random.nextDouble() * weightSum;
            ColosseumMob selected = options.get(options.size() - 1);
            for (ColosseumMob mob : options) {
                roll -= weight(round, mob);
                if (roll < 0.0D) { selected = mob; break; }
            }
            picks.add(new Pick(selected, false));
            remaining -= selected.threat();
        }
        Collections.shuffle(picks, random);
        return new Composition(round, participants, budget, total, band, List.copyOf(picks));
    }

    /** Provisional preference: R1-R2 lean low (2), R3-R4 medium (5), R5-R6 strong (8); every eligible mob keeps weight. */
    static double weight(int round, ColosseumMob mob) {
        int preferred = round <= 2 ? 2 : round <= 4 ? 5 : 8;
        return 1.0D / (1.0D + Math.abs(mob.threat() - preferred));
    }

    /** Smallest support sets that, with the elite, cover melee, ranged and special-ability roles. */
    private static List<List<ColosseumMob>> coverSets(ColosseumMob elite, List<ColosseumMob> support) {
        List<ColosseumMob.Role> missing = new ArrayList<>();
        for (ColosseumMob.Role role : ColosseumMob.Role.values()) if (!elite.hasRole(role)) missing.add(role);
        Set<List<ColosseumMob>> sets = new LinkedHashSet<>();
        if (missing.isEmpty()) sets.add(List.of(elite));
        for (ColosseumMob first : missing.isEmpty() ? List.<ColosseumMob>of() : support) {
            if (!first.hasRole(missing.get(0))) continue;
            EnumSet<ColosseumMob.Role> covered = EnumSet.copyOf(elite.roles());
            covered.addAll(first.roles());
            if (covered.size() == ColosseumMob.Role.values().length) {
                sets.add(List.of(elite, first));
                continue;
            }
            for (ColosseumMob second : support) {
                EnumSet<ColosseumMob.Role> all = EnumSet.copyOf(covered);
                all.addAll(second.roles());
                if (all.size() < ColosseumMob.Role.values().length) continue;
                List<ColosseumMob> set = new ArrayList<>(List.of(first, second));
                set.sort(null);
                set.add(0, elite);
                sets.add(List.copyOf(set));
            }
        }
        return new ArrayList<>(sets);
    }

    private static boolean[] reachable(List<ColosseumMob> pool, int max) {
        boolean[] reachable = new boolean[max + 1];
        reachable[0] = true;
        for (int amount = 1; amount <= max; amount++)
            for (ColosseumMob mob : pool)
                if (mob.threat() <= amount && reachable[amount - mob.threat()]) { reachable[amount] = true; break; }
        return reachable;
    }

    private static final class Candidates {
        final List<List<ColosseumMob>> sets = new ArrayList<>();
        final List<Integer> totals = new ArrayList<>();

        void add(List<ColosseumMob> set, int total) { sets.add(set); totals.add(total); }
        void clear() { sets.clear(); totals.clear(); }
        boolean isEmpty() { return sets.isEmpty(); }
        int size() { return sets.size(); }
    }
}
