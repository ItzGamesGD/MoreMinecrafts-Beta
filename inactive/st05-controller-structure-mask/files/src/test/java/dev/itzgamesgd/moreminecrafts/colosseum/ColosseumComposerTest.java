package dev.itzgamesgd.moreminecrafts.colosseum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ColosseumComposerTest {
    private static final int SEEDS = 250;

    @Test
    void budgetsMatchTheApprovedTable() {
        int[][] expected = {{20, 32, 44, 56}, {32, 51, 70, 90}, {48, 77, 106, 134}};
        for (int round = 1; round <= 6; round++)
            for (int n = 1; n <= 4; n++)
                assertEquals(expected[(round - 1) / 2][n - 1], ColosseumRules.budget(round, n), "R" + round + " n" + n);
        assertEquals(17, ColosseumRules.normalFillMin(20));
        assertEquals(77, ColosseumRules.normalFillMin(90));
        assertEquals(21, ColosseumRules.exceptionalFillMax(20));
        assertEquals(140, ColosseumRules.exceptionalFillMax(134));
        assertEquals(4, ColosseumRules.scoreCap(2));
        assertEquals(7, ColosseumRules.scoreCap(3));
        assertEquals(10, ColosseumRules.scoreCap(6));
    }

    @Test
    void rosterIsExactlyTheFifteenActiveAndEightInactiveQ109Entries() {
        assertEquals(15, ColosseumMob.values().length);
        int[] scores = Arrays.stream(ColosseumMob.values()).mapToInt(ColosseumMob::threat).toArray();
        assertEquals("[3, 2, 3, 4, 4, 5, 7, 3, 3, 4, 4, 6, 6, 7, 7]", Arrays.toString(scores));
        assertEquals(8, ColosseumConditionalCandidate.values().length);
        for (ColosseumConditionalCandidate candidate : ColosseumConditionalCandidate.values())
            assertFalse(candidate.active(), candidate + " must stay inactive");
        Set<String> active = EnumSet.allOf(ColosseumMob.class).stream().map(Enum::name).collect(java.util.stream.Collectors.toSet());
        for (ColosseumConditionalCandidate candidate : ColosseumConditionalCandidate.values())
            assertFalse(active.contains(candidate.name()), candidate + " leaked into the active roster");
    }

    @Test
    void everyRoundAndPartySizeFillsTheNormalBandWithEligibleMobsOnly() {
        for (int seed = 0; seed < SEEDS; seed++)
            for (int n = 1; n <= 4; n++)
                for (int round = 1; round <= 6; round++) {
                    var c = ColosseumComposer.compose(round, n, new Random(seed * 1000L + n * 10L + round));
                    int budget = ColosseumRules.budget(round, n);
                    assertEquals(ColosseumComposer.Band.NORMAL, c.band(), "seed " + seed + " R" + round + " n" + n);
                    assertTrue(c.total() >= ColosseumRules.normalFillMin(budget) && c.total() <= budget);
                    assertEquals(c.total(), c.picks().stream().mapToInt(p -> p.mob().threat()).sum());
                    int cap = ColosseumRules.scoreCap(round);
                    assertTrue(c.picks().stream().allMatch(p -> p.mob().threat() <= cap), "score cap " + cap);
                    if (round <= 2) assertEquals(0, c.eliteCount(), "R1-R2 never draw elites");
                }
    }

    @Test
    void roundThreeHasExactlyOneFocalEliteAndAllThreeRoles() {
        Set<ColosseumMob> focalSeen = EnumSet.noneOf(ColosseumMob.class);
        for (int seed = 0; seed < SEEDS; seed++)
            for (int n = 1; n <= 4; n++) {
                var c = ColosseumComposer.compose(3, n, new Random(seed * 17L + n));
                assertEquals(1, c.eliteCount(), "exactly one score>=7 mob in R3");
                var focal = c.picks().stream().filter(ColosseumComposer.Pick::focal).toList();
                assertEquals(1, focal.size());
                assertTrue(focal.get(0).mob().isElite());
                focalSeen.add(focal.get(0).mob());
                for (ColosseumMob.Role role : ColosseumMob.Role.values()) assertTrue(c.covers(role), "R3 covers " + role);
                assertTrue(c.total() <= ColosseumRules.budget(3, n), "the elite sits inside the budget");
            }
        assertEquals(EnumSet.of(ColosseumMob.BREEZE, ColosseumMob.ARMORED_SKELETON, ColosseumMob.BANDIT_WARRIOR), focalSeen,
                "any score-7 member may be the focal elite");
    }

    @Test
    void lateRoundsCanIncludeElitesAndPreferStrongerScores() {
        int withElite = 0, total = 0;
        double r1Mean = 0, r6Mean = 0;
        for (int seed = 0; seed < SEEDS; seed++) {
            for (int round = 5; round <= 6; round++) {
                var c = ColosseumComposer.compose(round, 1 + seed % 4, new Random(seed + round * 100000L));
                total++;
                if (c.eliteCount() > 0) withElite++;
            }
            var early = ColosseumComposer.compose(1, 1, new Random(seed));
            var late = ColosseumComposer.compose(6, 1, new Random(seed));
            r1Mean += early.picks().stream().mapToInt(p -> p.mob().threat()).average().orElseThrow();
            r6Mean += late.picks().stream().mapToInt(p -> p.mob().threat()).average().orElseThrow();
        }
        assertTrue(withElite > total / 2, "R5-R6 regularly include elites: " + withElite + "/" + total);
        assertTrue(r6Mean / SEEDS > r1Mean / SEEDS + 1.5, "late rounds favour stronger scores");
        assertTrue(ColosseumComposer.weight(6, ColosseumMob.BABY_ZOMBIE) > 0, "low scores are never excluded later");
    }

    @Test
    void compositionIsDeterministicForASeed() {
        var a = ColosseumComposer.compose(4, 3, new Random(99));
        var b = ColosseumComposer.compose(4, 3, new Random(99));
        assertEquals(a, b);
    }
}
