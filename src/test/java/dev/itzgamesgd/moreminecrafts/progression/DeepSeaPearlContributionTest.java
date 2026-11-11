package dev.itzgamesgd.moreminecrafts.progression;

import static dev.itzgamesgd.moreminecrafts.progression.DeepSeaPearlContribution.*;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DeepSeaPearlContributionTest {
    private final UUID elderA = UUID.randomUUID(), elderB = UUID.randomUUID();
    private final UUID p1 = UUID.randomUUID(), p2 = UUID.randomUUID(), p3 = UUID.randomUUID();

    @Test void singleValidHitQualifiesAndBoundaryIs60sInclusive() {
        Tracker t = new Tracker();
        t.recordHit(elderA, p1, 1000);
        assertEquals(Set.of(p1), t.settle(elderA, 1000));
        Tracker b = new Tracker();
        b.recordHit(elderA, p1, 1000); b.recordHit(elderA, p2, 999);
        assertEquals(Set.of(p1), b.settle(elderA, 2200), "exactly 1200 ticks is in, 1201 is out");
    }

    @Test void hitOlderThan60sExcludedAndLatestHitCounts() {
        Tracker t = new Tracker();
        t.recordHit(elderA, p1, 0);
        t.recordHit(elderA, p2, 0); t.recordHit(elderA, p2, 5000);
        assertEquals(Set.of(p2), t.settle(elderA, 5100));
    }

    @Test void playersRollIndependentlyWithDeterministicRolls() {
        Set<UUID> all = Set.of(p1, p2, p3);
        List<UUID> won = winners(all, p -> p.equals(p2) ? 0.1999999 : 0.2);
        assertEquals(List.of(p2), won, "roll < 0.20 wins, 0.20 loses");
        assertEquals(3, winners(all, p -> 0.0).size());
        assertTrue(winners(all, p -> 0.9999).isEmpty());
    }

    @Test void deathDoesNotRemoveContribution() {
        Tracker t = new Tracker();
        t.recordHit(elderA, p1, 100); // no hook on player death exists; log untouched
        assertEquals(Set.of(p1), t.settle(elderA, 200));
    }

    @Test void logoutErasesAndReconnectDoesNotRestore() {
        Tracker t = new Tracker();
        t.recordHit(elderA, p1, 100); t.recordHit(elderA, p2, 100); t.recordHit(elderB, p1, 100);
        t.forget(p1);
        assertEquals(Set.of(p2), t.settle(elderA, 200));
        assertTrue(t.settle(elderB, 200).isEmpty(), "reconnecting adds nothing back");
    }

    @Test void eldersAreIndependent() {
        Tracker t = new Tracker();
        t.recordHit(elderA, p1, 100); t.recordHit(elderB, p2, 100);
        assertEquals(Set.of(p1), t.settle(elderA, 150));
        assertEquals(Set.of(p2), t.settle(elderB, 150));
    }

    @Test void duplicateDeathEventAndLateHitsGrantNothing() {
        Tracker t = new Tracker();
        t.recordHit(elderA, p1, 100);
        assertEquals(Set.of(p1), t.settle(elderA, 150));
        assertTrue(t.settle(elderA, 150).isEmpty());
        t.recordHit(elderA, p1, 151);
        assertTrue(t.settle(elderA, 152).isEmpty());
    }

    @Test void deliveryInsertsThenDropsOnlyTheRemainder() {
        int[] dropped = {0};
        deliver(1, new Sink() { public int insert(int c) { return c; } public void drop(int c) { dropped[0] += c; } });
        assertEquals(0, dropped[0]);
        deliver(3, new Sink() { public int insert(int c) { return 1; } public void drop(int c) { dropped[0] += c; } });
        assertEquals(2, dropped[0]);
    }

    @Test void existingMonumentQuestWindowAndRewardsUntouched() throws Exception {
        String q = Files.readString(Path.of("src/main/java/dev/itzgamesgd/moreminecrafts/progression/QuestMonumentParticipation.java"));
        assertTrue(q.contains("WINDOW_TICKS = 12_000L"));
        assertTrue(q.contains("addPendingCredit"));
        String c = Files.readString(Path.of("src/main/java/dev/itzgamesgd/moreminecrafts/progression/T2CoreAcquisition.java"));
        assertTrue(c.contains("ANCIENT_HEART, 0.03F"));
        String main = Files.readString(Path.of("src/main/java/dev/itzgamesgd/moreminecrafts/MoreMinecrafts.java"));
        assertEquals(1, main.split("DeepSeaPearlContribution\\.initialize\\(\\)", -1).length - 1);
    }
}
