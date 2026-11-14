package dev.itzgamesgd.moreminecrafts.colosseum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import dev.itzgamesgd.moreminecrafts.colosseum.RewardLedger.ClaimResult;
import dev.itzgamesgd.moreminecrafts.colosseum.WaveRecord.KeyState;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** D2, Q113-B, Q114-C, Q115-B: the ledger is keyed by (trial, round), append-only, and personal data is separate. */
class ColosseumLedgerTest {
    private final Scenario s = new Scenario();

    @Test
    void anotherTrialKeepsTheEarlierTrialsRecordsAndClearsItsOwnRoundOne() {
        UUID player = s.start(1);
        UUID trialA = s.state.trialId();
        s.world.tick(s.state, 30);
        s.world.clearRound(s.state, 4000);
        assertTrue(s.state.waveCleared(trialA, 1));
        ColosseumController.forceReset(s.state, s.world); // trial A ends; its record must stay
        assertEquals(1, s.state.ledger().waves().size());
        ColosseumController.clearCooldown(s.state);
        s.world.present.clear();
        s.world.present.add(player);
        s.world.atActivation = true;
        s.world.tick(s.state, 1);
        s.world.atActivation = false;
        UUID trialB = s.state.trialId();
        assertFalse(trialA.equals(trialB));
        assertFalse(s.state.waveCleared(trialB, 1), "A's round-1 record must not count as B's");
        s.world.tick(s.state, 30);
        s.world.clearRound(s.state, 4000);
        assertEquals(Phase.INTERMISSION, s.state.phase(), "B's round 1 completes normally");
        assertEquals(2, s.state.ledger().waves().size());
        assertTrue(s.state.waveCleared(trialA, 1) && s.state.waveCleared(trialB, 1));
        assertEquals(2, s.world.waves.size(), "WAVE_CLEARED fires once per (trial, round)");
        assertEquals(List.of(trialA, trialB), s.world.waves.stream().map(WaveRecord::trialId).toList());
    }

    @Test
    void aWaveCanOnlyBeRecordedOncePerTrialAndRound() {
        s.start(1);
        s.world.tick(s.state, 30);
        s.world.clearRound(s.state, 4000);
        WaveRecord first = s.state.ledger().waves().get(0);
        WaveRecord duplicate = WaveRecord.cleared(first.trialId(), first.serial(), first.round(), List.of(), List.of(), 9L,
                KeyState.PENDING);
        assertFalse(s.state.ledger().addWave(duplicate));
        assertEquals(1, s.state.ledger().waves().size());
    }

    @Test
    void noEndingEverDeletesLedgerRecords() {
        for (var ending : List.of("reset", "timeout", "party")) {
            Scenario t = new Scenario();
            UUID p = t.start(1);
            t.world.tick(t.state, 30);
            t.world.clearRound(t.state, 4000);
            int waves = t.state.ledger().waves().size();
            int elig = t.state.ledger().eligibility().size();
            t.world.tick(t.state, 5);
            switch (ending) {
                case "reset" -> ColosseumController.forceReset(t.state, t.world);
                case "timeout" -> {
                    t.state.trialRemaining = 1;
                    t.world.tick(t.state, 1);
                }
                default -> {
                    ColosseumController.onPlayerDeath(t.state, p);
                    t.world.tick(t.state, 1);
                    ColosseumController.onPlayerDeath(t.state, p);
                }
            }
            assertEquals(waves, t.state.ledger().waves().size(), ending);
            assertEquals(elig, t.state.ledger().eligibility().size(), ending);
            ColosseumController.clearCooldown(t.state);
            t.world.atActivation = true;
            t.world.tick(t.state, 1);
            assertEquals(waves, t.state.ledger().waves().size(), ending + ": a new trial start keeps the history");
        }
    }

    @Test
    void eligibilityNeedsRoundStartRegistrationAndPresenceWhenTheRoundClears() {
        UUID stays = s.start(2);
        UUID leaves = s.world.present.stream().filter(p -> !p.equals(stays)).findFirst().orElseThrow();
        s.world.tick(s.state, 30);
        UUID lateJoiner = s.world.join(); // enters after the lock: not registered for this round
        s.world.present.remove(leaves);   // registered, but gone when the round clears
        s.world.clearRound(s.state, 4000);
        UUID trial = s.state.trialId();
        assertTrue(s.state.ledger().isEligible(trial, 1, stays));
        assertFalse(s.state.ledger().isEligible(trial, 1, leaves), "registered but absent at the clear");
        assertFalse(s.state.ledger().isEligible(trial, 1, lateJoiner), "present at the clear but not registered");
        assertEquals(List.of(stays), s.state.ledger().eligibility().stream().map(RewardLedger.EligibilityRecord::player).toList());
        assertEquals(RewardLedger.BASIS_Q114_C, s.state.ledger().eligibility().get(0).basis());
    }

    @Test
    void personalClaimsAreSeparateFromTheSharedKeyAndNeverDuplicate() {
        UUID player = s.start(1);
        s.world.tick(s.state, 30);
        s.world.clearRound(s.state, 4000);
        UUID trial = s.state.trialId();
        UUID key = s.state.ledger().wave(trial, 1).key().entity();
        assertNotNull(key);
        s.world.removeKey(s.state, key); // picking up the shared key is not a personal claim
        assertEquals(0, s.state.ledger().claims().size());
        assertEquals(ClaimResult.NOT_ELIGIBLE, s.state.ledger().tryClaim(trial, 1, UUID.randomUUID(), "x", 1L));
        assertEquals(ClaimResult.CLAIMED, s.state.ledger().tryClaim(trial, 1, player, "reward-a", 5L));
        assertEquals(ClaimResult.ALREADY_CLAIMED, s.state.ledger().tryClaim(trial, 1, player, "reward-a", 6L));
        assertEquals(ClaimResult.CLAIMED, s.state.ledger().tryClaim(trial, 1, player, "reward-b", 7L), "another reward is its own claim");
        assertEquals(2, s.state.ledger().claims().size());
        assertEquals(1, s.world.keysSpawned, "personal records never create or multiply the shared key");
        assertEquals(KeyState.CONSUMED, s.state.ledger().wave(trial, 1).key().state());
    }

    @Test
    void ledgerSurvivesSerializationWithEveryListAndKeyState() {
        UUID player = s.start(1);
        s.world.tick(s.state, 30);
        s.world.clearRound(s.state, 4000);
        UUID trial = s.state.trialId();
        s.state.ledger().tryClaim(trial, 1, player, "reward-a", 5L);
        s.state.ledger().release(UUID.randomUUID());
        ColosseumTrialState restored = TestArenas.roundTrip(s.state);
        assertEquals(TestArenas.encode(s.state), TestArenas.encode(restored));
        assertEquals(1, restored.ledger().waves().size());
        assertEquals(1, restored.ledger().eligibility().size());
        assertEquals(1, restored.ledger().claims().size());
        assertEquals(1, restored.ledger().releasedTrials().size());
        assertEquals(KeyState.SPAWNED, restored.ledger().wave(trial, 1).key().state());
    }

    @Test
    void duplicateUniqueKeysOnLoadKeepOnlyTheFirstRecord() {
        UUID trial = UUID.randomUUID(), player = UUID.randomUUID();
        RewardLedger ledger = new RewardLedger();
        ledger.addWave(WaveRecord.cleared(trial, 1, 1, List.of(player), List.of(player), 1L, KeyState.NONE));
        ledger.addEligibility(new RewardLedger.EligibilityRecord(trial, 1, player, RewardLedger.BASIS_Q114_C, 1L));
        JsonObject json = RewardLedger.CODEC.encodeStart(JsonOps.INSTANCE, ledger).getOrThrow().getAsJsonObject();
        json.getAsJsonArray("waves").add(json.getAsJsonArray("waves").get(0).deepCopy());
        json.getAsJsonArray("eligibility").add(json.getAsJsonArray("eligibility").get(0).deepCopy());
        RewardLedger loaded = RewardLedger.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        assertEquals(1, loaded.waves().size());
        assertEquals(1, loaded.eligibility().size());
    }

    @Test
    void absorbMergesAnArchivedLedgerWithoutDuplicatingOrLosingRecords() {
        UUID trial = UUID.randomUUID(), p = UUID.randomUUID();
        RewardLedger a = new RewardLedger(), b = new RewardLedger();
        a.addWave(WaveRecord.cleared(trial, 1, 1, List.of(p), List.of(p), 1L, KeyState.NONE));
        b.addWave(WaveRecord.cleared(trial, 1, 1, List.of(p), List.of(p), 1L, KeyState.NONE));
        b.addWave(WaveRecord.cleared(trial, 1, 2, List.of(p), List.of(p), 2L, KeyState.NONE));
        a.absorb(b);
        assertEquals(2, a.waves().size());
        assertNull(a.wave(UUID.randomUUID(), 1));
    }

    private static final class Phase {
        static final ColosseumTrialState.Phase INTERMISSION = ColosseumTrialState.Phase.INTERMISSION;
    }
}
