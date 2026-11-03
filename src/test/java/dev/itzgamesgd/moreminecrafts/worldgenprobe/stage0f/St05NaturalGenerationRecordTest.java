package dev.itzgamesgd.moreminecrafts.worldgenprobe.stage0f;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.itzgamesgd.moreminecrafts.worldgenprobe.stage0f.St05NaturalGeneration.RegionFile;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The persisted region record of the natural generation driver: the writer's output must be accepted unmodified, and a larger-universe rescan must only ever resolve BLOCKED decisions. */
class St05NaturalGenerationRecordTest {
    private static RegionFile file(int radius, String... lines) {
        RegionFile f = new RegionFile();
        f.seed = 7001L; f.rx = -4; f.rz = -5; f.radius = radius;
        for (String l : lines) {
            f.lines.add(l);
            String[] t = l.split(" ");
            if (t[1].equals(St05NaturalGeneration.BLOCKED)) f.blocked++;
            if (t[1].equals("PLACEMENT_RESERVED")) f.reservations.add(new St05Stage0FLive.Res(t[0], -837, -1174, -25));
        }
        return f;
    }

    @Test
    void writerOutputIsReadBackWithoutModification(@TempDir Path dir) throws Exception {
        RegionFile w = file(2, "20d89ee7a8aa754c PLACEMENT_RESERVED -", "312b4819e63b6f31 BLOCKED_BY_UNRESOLVED UNRESOLVED_GROUP_NEARBY");
        w.retryDone = true;
        Path p = dir.resolve("r_-4_-5.txt");
        St05NaturalGeneration.writeRegion(p, w);
        RegionFile r = St05NaturalGeneration.readRegion(p, 7001L);
        assertNotNull(r, "a record the writer just produced must not be treated as damaged");
        assertEquals(1, r.reservations.size());
        assertEquals("20d89ee7a8aa754c", r.reservations.get(0).id());
        assertEquals(1, r.blocked);
        assertEquals(2, r.radius);
        assertTrue(r.retryDone);
        assertEquals(2, r.lines.size());
    }

    @Test
    void emptyRegionRoundTrips(@TempDir Path dir) throws Exception {
        Path p = dir.resolve("r_0_0.txt");
        St05NaturalGeneration.writeRegion(p, file(2));
        assertNotNull(St05NaturalGeneration.readRegion(p, 7001L));
    }

    @Test
    void tamperedOrForeignSeedRecordIsRejected(@TempDir Path dir) throws Exception {
        Path p = dir.resolve("r_-4_-5.txt");
        St05NaturalGeneration.writeRegion(p, file(2, "20d89ee7a8aa754c PLACEMENT_RESERVED -"));
        assertNull(St05NaturalGeneration.readRegion(p, 7002L));
        Files.writeString(p, Files.readString(p).replace("-837", "-838"));
        assertNull(St05NaturalGeneration.readRegion(p, 7001L));
    }

    @Test
    void largerUniverseMayResolveBlockedDecisionsAndKeepsReservations() {
        RegionFile old = file(2, "aaaa PLACEMENT_RESERVED -", "bbbb BLOCKED_BY_UNRESOLVED HIGHER_PRIORITY_NEIGHBOUR_UNRESOLVED");
        RegionFile fresh = file(3, "aaaa PLACEMENT_RESERVED -", "bbbb PLACEMENT_RESERVED -");
        RegionFile m = St05NaturalGeneration.merge(old, fresh);
        assertSame(fresh, m);
        assertEquals(2, m.reservations.size());
        assertEquals(0, m.blocked);
    }

    @Test
    void disagreementOnAnUnblockedDecisionKeepsTheEarlierRecordAndStopsRetries() {
        RegionFile old = file(2, "aaaa PLACEMENT_RESERVED -", "bbbb BLOCKED_BY_UNRESOLVED UNRESOLVED_GROUP_NEARBY");
        RegionFile fresh = file(3, "aaaa ROLL_FAILED -", "bbbb BLOCKED_BY_UNRESOLVED UNRESOLVED_GROUP_NEARBY");
        RegionFile m = St05NaturalGeneration.merge(old, fresh);
        assertSame(old, m);
        assertTrue(m.retryDone);
        assertEquals("aaaa", m.reservations.get(0).id());
    }

    @Test
    void sameChambersBlockedUnderALargerUniverseEndsTheRetries() {
        RegionFile old = file(2, "bbbb BLOCKED_BY_UNRESOLVED UNRESOLVED_GROUP_NEARBY");
        RegionFile fresh = file(3, "bbbb BLOCKED_BY_UNRESOLVED UNRESOLVED_GROUP_NEARBY");
        assertTrue(St05NaturalGeneration.merge(old, fresh).retryDone);
        RegionFile partial = file(3, "bbbb BLOCKED_BY_UNRESOLVED UNRESOLVED_GROUP_NEARBY", "cccc ROLL_FAILED -");
        assertFalse(St05NaturalGeneration.merge(file(2, "bbbb BLOCKED_BY_UNRESOLVED X", "cccc BLOCKED_BY_UNRESOLVED X"), partial).retryDone);
    }

    @Test
    void firstScanIsKeptAsIs() {
        RegionFile fresh = file(2, "aaaa PLACEMENT_RESERVED -");
        assertSame(fresh, St05NaturalGeneration.merge(null, fresh));
    }
}
