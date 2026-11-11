package dev.itzgamesgd.moreminecrafts.colosseum;

import static org.junit.jupiter.api.Assertions.*;
import dev.itzgamesgd.moreminecrafts.colosseum.ArenaAttempt.*;
import org.junit.jupiter.api.Test;

class CombatRecoveryTest {
    @Test void unverifiedChunksHoldDeathEventsAndIntermission() {
        Sim s = new Sim(); s.inside.add(Sim.A); s.ticks(25);
        Alloc a = s.c.attempt().allocations().stream().filter(x -> x.status == AllocStatus.SPAWNED).findFirst().orElseThrow();
        s.loaded = false; s.kill(a); s.tick();
        assertEquals(AllocStatus.SPAWNED, a.status, "Q40: no death decision before entity verification");
        s.loaded = true; s.tick(); assertEquals(AllocStatus.KILLED, a.status);
        s.clearRound(); int left = s.c.attempt().intermissionLeft;
        s.loaded = false; s.ticks(10); assertEquals(left, s.c.attempt().intermissionLeft);
    }
    @Test void priorGraceDeadlineWinsOverLaterAttemptDeadline() {
        Sim s = new Sim(); s.inside.add(Sim.A); s.tick();
        s.c.setTiming(new RoundPlan.Timing(400, 100));
        s.inside.clear(); s.tick(); s.now += 1000; s.tick();
        assertEquals(Outcome.ABSENCE_EXPIRED, s.c.attempt().outcome());
    }
    @Test void duplicateAllocationIdentityIsQuarantined() {
        Sim s = new Sim(); s.inside.add(Sim.A); s.tick();
        String json = s.c.toJson();
        var dto = ArenaController.GSON.fromJson(json, ArenaController.Dto.class);
        dto.attempt.allocs.get(1).id = dto.attempt.allocs.get(0).id;
        String bad = ArenaController.GSON.toJson(dto);
        assertTrue(ArenaController.restore("arena-1", bad, ArenaController.checksum(bad), s.now).quarantined());
    }
}
