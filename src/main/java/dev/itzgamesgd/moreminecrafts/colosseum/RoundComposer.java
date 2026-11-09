package dev.itzgamesgd.moreminecrafts.colosseum;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Deterministic composition of one round's mob allocations from a (seed, round, budget): the plan is made and persisted before any spawn
 * (RESOLVE Q38). Bands per current authority: normal ceil(85%)..100%, exceptional only if no normal composition exists up to floor(105%),
 * and otherwise the highest valid total at or below the base budget.
 */
public final class RoundComposer {
    private RoundComposer() {}

    private static final int ATTEMPTS = 600;

    public static List<String> compose(long seed, int round, int budget) {
        if (round < 1 || round > 6) throw new IllegalArgumentException("round " + round + " has no ordinary composition");
        List<String> pool = MobRoster.eligible(round);
        if (pool.isEmpty()) throw new IllegalStateException("empty roster pool for round " + round);
        int lo = (int) Math.ceil(0.85 * budget), hi = budget, ex = (int) Math.floor(1.05 * budget);
        Random rng = new Random(seed * 31 + round);
        List<String> out = search(rng, pool, round, lo, hi);
        if (out == null) out = search(rng, pool, round, lo, ex);
        if (out == null) out = fallback(rng, pool, round, hi);
        Collections.shuffle(out, rng);
        return out;
    }

    private static List<String> search(Random rng, List<String> pool, int round, int lo, int hi) {
        for (int a = 0; a < ATTEMPTS; a++) {
            List<String> picks = new ArrayList<>();
            int total = 0;
            if (round == 3) {
                List<String> elites = pool.stream().filter(m -> MobRoster.score(m) == MobRoster.ELITE_SCORE).toList();
                String focal = elites.get(rng.nextInt(elites.size()));
                picks.add(focal);
                total += MobRoster.score(focal);
            }
            while (total < lo) {
                List<String> fit = new ArrayList<>();
                for (String m : pool) {
                    int s = MobRoster.score(m);
                    if (total + s > hi) continue;
                    if (round == 3 && s >= MobRoster.ELITE_SCORE) continue;   // exactly one focal elite
                    fit.add(m);
                }
                if (fit.isEmpty()) break;
                picks.add(weighted(rng, fit, round));
                total += MobRoster.score(picks.get(picks.size() - 1));
            }
            if (total < lo || total > hi) continue;
            if (round == 3 && !rolesCovered(picks)) continue;
            if (round >= 5 && picks.stream().noneMatch(m -> MobRoster.score(m) >= MobRoster.ELITE_SCORE)) continue;   // elite rounds
            return picks;
        }
        return null;
    }

    /** Favor lower scores early, medium in the middle, stronger late, never excluding lower scores. */
    private static String weighted(Random rng, List<String> fit, int round) {
        double target = round <= 2 ? 2.5 : round <= 4 ? 4.5 : 6.5;
        double sum = 0;
        double[] w = new double[fit.size()];
        for (int i = 0; i < w.length; i++) { w[i] = 1.0 / (1.0 + Math.abs(MobRoster.score(fit.get(i)) - target)); sum += w[i]; }
        double r = rng.nextDouble() * sum;
        for (int i = 0; i < w.length; i++) { r -= w[i]; if (r <= 0) return fit.get(i); }
        return fit.get(fit.size() - 1);
    }

    static boolean rolesCovered(List<String> picks) {
        return picks.stream().anyMatch(MobRoster.MELEE::contains) && picks.stream().anyMatch(MobRoster.RANGED::contains)
                && picks.stream().anyMatch(MobRoster.SPECIAL::contains);
    }

    private static List<String> fallback(Random rng, List<String> pool, int round, int hi) {
        List<String> picks = new ArrayList<>();
        int total = 0;
        if (round == 3) {
            String focal = pool.stream().filter(m -> MobRoster.score(m) == MobRoster.ELITE_SCORE).findFirst().orElseThrow();
            if (MobRoster.score(focal) <= hi) { picks.add(focal); total += MobRoster.score(focal); }
        }
        boolean progress = true;
        while (progress) {
            progress = false;
            List<String> fit = new ArrayList<>();
            for (String m : pool) {
                int s = MobRoster.score(m);
                if (total + s <= hi && !(round == 3 && s >= MobRoster.ELITE_SCORE)) fit.add(m);
            }
            if (!fit.isEmpty()) { String m = fit.get(rng.nextInt(fit.size())); picks.add(m); total += MobRoster.score(m); progress = true; }
        }
        return picks;
    }
}
