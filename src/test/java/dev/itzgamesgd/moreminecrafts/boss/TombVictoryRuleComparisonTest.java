package dev.itzgamesgd.moreminecrafts.boss;

import static org.junit.jupiter.api.Assertions.*;

import dev.itzgamesgd.moreminecrafts.boss.TombVictoryPolicy.DeathCause;
import dev.itzgamesgd.moreminecrafts.boss.TombVictoryPolicy.Participation;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * ST-01 P10 O-9 analysis support (NO production change): runs the SAME synthetic fights through
 *   rule A = the shipped structural minimum (killing blow attributed to a participating player), and
 *   rule B = "enough player participation, whatever the killing blow" with TEST-ONLY candidate numbers (damage share, last-player-hit window).

 * The expected-outcome column is the ANALYST'S ILLUSTRATIVE LABEL for discussion; it is not design authority and not an approval.
 * B's numbers are candidates for this comparison only; they are installed per assertion and removed afterwards. Nothing here changes the production default.
 */
class TombVictoryRuleComparisonTest {
    private static final double MAX = 1000.0D;
    private static final UUID P1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID P2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID P3 = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @AfterEach void reset() { TombVictoryPolicy.install(null); }

    record Scenario(String id, String story, DeathCause cause, long deathTick, Consumer<Participation> fight, boolean intendedWin) {}

    static List<Scenario> scenarios() {
        List<Scenario> s = new ArrayList<>();
        s.add(new Scenario("S01", "player does all damage and lands the killing blow", DeathCause.PLAYER, 2000, p -> p.onPlayerDamage(P1, MAX, 1990), true));
        s.add(new Scenario("S02", "two players share the damage, one lands the killing blow", DeathCause.PLAYER, 2000, p -> { p.onPlayerDamage(P1, 600, 1500); p.onPlayerDamage(P2, 400, 1990); }, true));
        s.add(new Scenario("S03", "player deals 95%, lava/void finishes the last 5% immediately", DeathCause.ENVIRONMENT, 2000, p -> { p.onPlayerDamage(P1, 950, 1995); p.onOtherDamage(50); }, true));
        s.add(new Scenario("S04", "player deals 95%, a hostile mob (skeleton arrow) lands the killing blow", DeathCause.MOB, 2000, p -> { p.onPlayerDamage(P1, 950, 1995); p.onOtherDamage(50); }, true));
        s.add(new Scenario("S05", "player deals 95%, then the boss is left to die to an environment trap 30 s later", DeathCause.ENVIRONMENT, 2600, p -> { p.onPlayerDamage(P1, 950, 2000); p.onOtherDamage(50); }, true));
        s.add(new Scenario("S06", "player tags the boss once (1%), a lava/cactus trap does the other 99%", DeathCause.ENVIRONMENT, 4000, p -> { p.onPlayerDamage(P1, 10, 100); p.onOtherDamage(990); }, false));
        s.add(new Scenario("S07", "player tags the boss once (1%), a mob grinder / other mobs do 99%", DeathCause.MOB, 4000, p -> { p.onPlayerDamage(P1, 10, 100); p.onOtherDamage(990); }, false));
        s.add(new Scenario("S08", "environment does 90%, a player finishes the last 10% (kill steal / late join)", DeathCause.PLAYER, 3000, p -> { p.onOtherDamage(900); p.onPlayerDamage(P1, 100, 2990); }, false));
        s.add(new Scenario("S09", "player deals 50% then walks away; boss dies to environment minutes later", DeathCause.ENVIRONMENT, 9000, p -> { p.onPlayerDamage(P1, 500, 1000); p.onOtherDamage(500); }, false));
        s.add(new Scenario("S10", "player deals 99%, a second player lands a 1% killing blow", DeathCause.PLAYER, 2000, p -> { p.onPlayerDamage(P1, 990, 1900); p.onPlayerDamage(P2, 10, 1990); }, true));
        s.add(new Scenario("S11", "three players 40/35/15% plus 10% other, a mob finishes", DeathCause.MOB, 2000, p -> { p.onPlayerDamage(P1, 400, 1800); p.onPlayerDamage(P2, 350, 1850); p.onPlayerDamage(P3, 150, 1900); p.onOtherDamage(100); }, true));
        s.add(new Scenario("S12", "administrative /kill after genuine participation", DeathCause.COMMAND_KILL, 2000, p -> { p.onPlayerDamage(P1, 500, 1990); p.onOtherDamage(500); }, false));
        s.add(new Scenario("S13", "no player ever damaged the boss (pure environment/mob death)", DeathCause.ENVIRONMENT, 2000, p -> p.onOtherDamage(MAX), false));
        s.add(new Scenario("S14", "owned wolves/arrows (attributed to the owner) deal 100%, a wolf lands the killing blow", DeathCause.PLAYER, 2000, p -> p.onPlayerDamage(P1, MAX, 1990), true));
        return s;
    }

    static boolean ruleA(Scenario sc) {
        TombVictoryPolicy.install(null);
        Participation p = new Participation(); sc.fight().accept(p);
        return p.judge(sc.cause(), sc.deathTick(), MAX).valid();
    }

    static boolean ruleB(Scenario sc, double minShare, long window) {
        TombVictoryPolicy.install(new TombVictoryPolicy.Params(minShare, window));
        try { Participation p = new Participation(); sc.fight().accept(p); return p.judge(sc.cause(), sc.deathTick(), MAX).valid(); }
        finally { TombVictoryPolicy.install(null); }
    }

    @Test void ruleAIsTheShippedDefaultAndNeedsNoNumbers() {
        assertNull(TombVictoryPolicy.installed());
        for (Scenario sc : scenarios()) if (sc.cause() == DeathCause.PLAYER) assertTrue(ruleA(sc) || sc.fight() == null, sc.id() + " player-attributed killing blow is valid under A");
        for (Scenario sc : scenarios()) if (sc.cause() != DeathCause.PLAYER) assertFalse(ruleA(sc), sc.id() + " a non-player killing blow is never valid under A");
    }

    @Test void ruleBExposesTheTradeOffAndProducesTheComparisonMatrix() throws IOException {
        double[] shares = {0.25D, 0.5D, 0.75D};
        long[] windows = {100L, 200L, 600L, 1200L};
        // structural claims the analysis relies on
        Scenario s03 = scenarios().get(2), s06 = scenarios().get(5), s08 = scenarios().get(7), s12 = scenarios().get(11);
        assertFalse(ruleA(s03) , "A rejects the 95%-then-lava win");
        assertTrue(ruleB(s03, 0.5, 200), "B accepts it");
        assertFalse(ruleB(s06, 0.25, 1200), "B rejects a 1% tag even with the loosest candidate numbers");
        assertTrue(ruleA(s08) && !ruleB(s08, 0.25, 200), "kill steal: A accepts a late finisher, B rejects it (share too low)");
        assertFalse(ruleA(s12) || ruleB(s12, 0.25, 1200), "admin kill is invalid under both rules for every candidate");
        StringBuilder md = new StringBuilder("scenario,story,death_cause,analyst_label_win,rule_A");
        for (double sh : shares) for (long w : windows) md.append(",B(share>=").append(sh).append(" window<=").append(w).append(")");
        md.append('\n');
        int[] bWins = new int[shares.length * windows.length]; int aWins = 0, aMatches = 0; int[] bMatches = new int[bWins.length];
        for (Scenario sc : scenarios()) {
            boolean a = ruleA(sc); if (a) aWins++; if (a == sc.intendedWin()) aMatches++;
            md.append(sc.id()).append(",\"").append(sc.story()).append("\",").append(sc.cause()).append(',').append(sc.intendedWin()).append(',').append(a);
            int k = 0;
            for (double sh : shares) for (long w : windows) { boolean b = ruleB(sc, sh, w); if (b) bWins[k]++; if (b == sc.intendedWin()) bMatches[k]++; md.append(',').append(b); k++; }
            md.append('\n');
        }
        md.append("TOTAL valid wins,,,,").append(aWins);
        for (int b : bWins) md.append(',').append(b);
        md.append("\nmatches the analyst label,,,,").append(aMatches);
        for (int b : bMatches) md.append(',').append(b);
        md.append('\n');
        System.out.println(md);
        String out = System.getProperty("moreminecrafts.p10.out");
        if (out != null && !out.isBlank()) { Files.createDirectories(Path.of(out)); Files.writeString(Path.of(out, "o9_rule_matrix.csv"), md.toString()); }
        assertEquals(scenarios().size(), 14);
    }
}
