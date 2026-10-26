package dev.itzgamesgd.moreminecrafts.progression;

import com.mojang.serialization.JsonOps;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MonumentRewardDataTest {
    @Test
    void legacyClaimListStillLoadsAfterDeferredStateIsAdded() {
        long monumentKey = 0x12345678L;
        var legacy = com.mojang.serialization.Codec.LONG.listOf()
                .encodeStart(JsonOps.INSTANCE, java.util.List.of(monumentKey)).getOrThrow();

        MonumentRewardData restored = MonumentRewardData.TYPE.codec().parse(JsonOps.INSTANCE, legacy).getOrThrow();

        assertTrue(restored.isClaimed(monumentKey));
        assertFalse(restored.claim(monumentKey));
    }

    @Test
    void monumentCompletionClaimPersistsAndCannotBeRepeated() {
        MonumentRewardData data = new MonumentRewardData();
        assertTrue(data.claim(0x12345678L));
        assertFalse(data.claim(0x12345678L));

        var encoded = MonumentRewardData.TYPE.codec().encodeStart(JsonOps.INSTANCE, data).getOrThrow();
        MonumentRewardData restored = MonumentRewardData.TYPE.codec().parse(JsonOps.INSTANCE, encoded).getOrThrow();
        assertFalse(restored.claim(0x12345678L), "saved world state prevents re-entry rewards");
        assertTrue(restored.claim(0x23456789L), "a different Ocean Monument can award its own core");
    }

    @Test
    void deferredCompletionWaitsForLoadedMonumentAndClaimsExactlyOnce() {
        MonumentRewardData data = new MonumentRewardData();
        BoundingBox bounds = new BoundingBox(-15, 20, 4, 17, 55, 23);
        data.defer(0x3456789AL, bounds, new Vec3(-2.5, 22.0, 5.5));
        Vec3 rewardPosition = new Vec3(3.5, 25.25, 7.75);
        data.defer(0x3456789AL, bounds, rewardPosition);

        var encoded = MonumentRewardData.TYPE.codec().encodeStart(JsonOps.INSTANCE, data).getOrThrow();
        MonumentRewardData restored = MonumentRewardData.TYPE.codec().parse(JsonOps.INSTANCE, encoded).getOrThrow();
        assertEquals(bounds, restored.pending(0x3456789AL).bounds());
        assertEquals(rewardPosition, restored.pending(0x3456789AL).rewardPosition());

        assertEquals(MonumentRewardData.CompletionResult.WAITING_FOR_CHUNKS,
                restored.finishIfReady(0x3456789AL, false, false));
        assertNotNull(restored.pending(0x3456789AL), "unloaded chunks must leave a retry queued");

        assertEquals(MonumentRewardData.CompletionResult.CLAIMED,
                restored.finishIfReady(0x3456789AL, true, false));
        assertNull(restored.pending(0x3456789AL));
        assertFalse(restored.claim(0x3456789AL), "a retry must not issue a second reward");
        assertEquals(MonumentRewardData.CompletionResult.NOT_PENDING,
                restored.finishIfReady(0x3456789AL, true, false));
    }

    @Test
    void deferredCompletionWithAnotherGuardianWaitsForTheNextDeathEvent() {
        MonumentRewardData data = new MonumentRewardData();
        data.defer(0x456789ABL, new BoundingBox(0, 20, 0, 15, 55, 15), new Vec3(4.5, 30.25, 4.75));

        assertEquals(MonumentRewardData.CompletionResult.GUARDIANS_REMAIN,
                data.finishIfReady(0x456789ABL, true, true));
        assertNull(data.pending(0x456789ABL));
        assertFalse(data.isClaimed(0x456789ABL));

        data.defer(0x456789ABL, new BoundingBox(0, 20, 0, 15, 55, 15), new Vec3(4.5, 30.25, 4.75));
        assertEquals(MonumentRewardData.CompletionResult.CLAIMED,
                data.finishIfReady(0x456789ABL, true, false));
    }

    @Test
    void participantEvidenceAndOfflineCreditsPersistAndStayBounded() {
        java.util.UUID elder = new java.util.UUID(0, 1);
        java.util.UUID combat = new java.util.UUID(0, 2);
        MonumentRewardData data = new MonumentRewardData();
        data.recordParticipation(9L, elder, 10L, true);
        data.recordParticipation(9L, combat, 100L, false);
        data.addPendingCredit(combat, 9L);
        var encoded = MonumentRewardData.TYPE.codec().encodeStart(JsonOps.INSTANCE, data).getOrThrow();
        MonumentRewardData restored = MonumentRewardData.TYPE.codec().parse(JsonOps.INSTANCE, encoded).getOrThrow();
        assertTrue(restored.participant(9L, elder).elderDamage());
        assertEquals(100L, restored.participant(9L, combat).lastCombatTick());
        assertEquals(java.util.List.of(elder, combat), restored.qualifiedParticipants(9L, 100L + 12_000L, 12_000L));
        assertEquals(java.util.List.of(elder), restored.qualifiedParticipants(9L, 100L + 12_001L, 12_000L));
        assertTrue(restored.hasPendingCredit(combat, 9L));
        assertEquals(java.util.List.of(9L), restored.takePendingCredits(combat));
        assertTrue(restored.takePendingCredits(combat).isEmpty(), "a pending credit is taken once");

        for (int i = 0; i < 200; i++) data.recordParticipation(10L, new java.util.UUID(1, i), i, false);
        assertTrue(java.util.stream.IntStream.range(0, 200).mapToObj(i -> data.participant(10L, new java.util.UUID(1, i)))
                .filter(java.util.Objects::nonNull).count() <= MonumentRewardData.MAX_PARTICIPANTS_PER_MONUMENT);
    }

    @Test
    void completedMonumentsAcceptNoNewEvidenceAndClearTheirParticipants() {
        java.util.UUID player = new java.util.UUID(0, 5);
        MonumentRewardData data = new MonumentRewardData();
        data.recordParticipation(11L, player, 1L, true);
        assertTrue(data.claim(11L));
        data.clearParticipants(11L);
        data.recordParticipation(11L, player, 2L, true);
        assertNull(data.participant(11L, player), "no evidence is recorded for an already completed monument");
    }

    @Test
    void savesWithoutParticipantFieldsStillLoad() {
        var oldState = new com.google.gson.JsonObject();
        oldState.add("claimed", new com.google.gson.Gson().toJsonTree(java.util.List.of(5L)));
        MonumentRewardData restored = MonumentRewardData.TYPE.codec().parse(JsonOps.INSTANCE, oldState).getOrThrow();
        assertTrue(restored.isClaimed(5L));
        assertTrue(restored.qualifiedParticipants(5L, 0L, 12_000L).isEmpty());
    }
}
