package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.*;

import com.mojang.serialization.JsonOps;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class QuestDragonRematchDataTest {
    private static final UUID DRAGON = UUID.fromString("00000000-0000-0000-0000-0000000000d1");
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-0000000000a1");

    private static QuestDragonRematchData roundTrip(QuestDragonRematchData data) {
        var encoded = QuestDragonRematchData.TYPE.codec().encodeStart(JsonOps.INSTANCE, data).getOrThrow();
        return QuestDragonRematchData.TYPE.codec().parse(JsonOps.INSTANCE, encoded).getOrThrow();
    }

    @Test void creditCandidateAndPendingCreditSurviveASave() {
        QuestDragonRematchData data = new QuestDragonRematchData();
        data.putCandidate(DRAGON, PLAYER);
        data.addPending(PLAYER);
        QuestDragonRematchData restored = roundTrip(data);
        assertEquals(PLAYER, restored.candidate(DRAGON));
        assertTrue(restored.hasPending(PLAYER));
    }

    @Test void candidatesAreConsumedExactlyOnceAndPendingCreditIsAppliedOnce() {
        QuestDragonRematchData data = new QuestDragonRematchData();
        data.putCandidate(DRAGON, PLAYER);
        assertEquals(PLAYER, data.takeCandidate(DRAGON));
        assertNull(data.takeCandidate(DRAGON));
        data.addPending(PLAYER);
        assertTrue(data.takePending(PLAYER));
        assertFalse(data.takePending(PLAYER));
        assertFalse(roundTrip(data).hasPending(PLAYER));
    }

    @Test void candidateTableIsBounded() {
        QuestDragonRematchData data = new QuestDragonRematchData();
        for (int i = 0; i < 400; i++) data.putCandidate(new UUID(0, i), PLAYER);
        assertNull(data.candidate(new UUID(0, 0)), "oldest entries are evicted");
        assertEquals(PLAYER, data.candidate(new UUID(0, 399)));
    }
}
