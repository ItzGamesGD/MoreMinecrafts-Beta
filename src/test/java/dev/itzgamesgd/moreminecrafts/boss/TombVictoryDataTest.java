package dev.itzgamesgd.moreminecrafts.boss;

import static org.junit.jupiter.api.Assertions.*;

import com.mojang.serialization.JsonOps;
import dev.itzgamesgd.moreminecrafts.boss.TombEncounterData.GuardianPresence;
import dev.itzgamesgd.moreminecrafts.boss.TombEncounterData.StartDecision;
import dev.itzgamesgd.moreminecrafts.boss.TombVictoryPolicy.DeathCause;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** ST-01 P9 CR-1/CR-2: death vs victory separation, legacy record semantics and the (unapproved-number) participation judgement. */
class TombVictoryDataTest {
    private static final long TOMB = 777L;
    private static final UUID GUARDIAN = UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e");
    private static final UUID P1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID P2 = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @AfterEach void clearParams() { TombVictoryPolicy.install(null); }

    private static TombEncounterData roundTrip(TombEncounterData data) {
        var encoded = TombEncounterData.TYPE.codec().encodeStart(JsonOps.INSTANCE, data).getOrThrow();
        return TombEncounterData.TYPE.codec().parse(JsonOps.INSTANCE, encoded).getOrThrow();
    }

    // ---- CR-1 data semantics
    @Test void validVictoryCompletesTheTombAndIsRecordedAsVictory() {
        TombEncounterData d = new TombEncounterData();
        d.bind(TOMB, GUARDIAN, 1L, 1L);
        assertTrue(d.markVictory(TOMB, GUARDIAN));
        assertFalse(d.markVictory(TOMB, GUARDIAN), "a victory (reward, seal release, entrance) can be granted only once");
        assertTrue(d.isDefeated(TOMB) && d.isVictory(TOMB));
        assertEquals(StartDecision.ALREADY_DEFEATED, d.decideStart(TOMB, GuardianPresence.GONE));
    }

    @Test void invalidDeathReleasesTheBindingAndLeavesTheTombChallengeable() {
        TombEncounterData d = new TombEncounterData();
        d.bind(TOMB, GUARDIAN, 1L, 1L);
        assertTrue(d.release(TOMB, GUARDIAN), "what an invalid death does");
        assertFalse(d.isDefeated(TOMB) || d.isVictory(TOMB), "no completion, no victory");
        assertEquals(StartDecision.START, d.decideStart(TOMB, GuardianPresence.GONE), "a new attempt is possible");
        assertTrue(d.bind(TOMB, UUID.randomUUID(), 2L, 2L));
    }

    @Test void legacyDefeatedRecordsStayHonouredButAreNeverConvertedToVictories() {
        var json = com.google.gson.JsonParser.parseString("{\"defeated\":[5,6],\"active\":[]}");
        TombEncounterData d = TombEncounterData.TYPE.codec().parse(JsonOps.INSTANCE, json).getOrThrow();
        assertTrue(d.isDefeated(5L) && d.isDefeated(6L), "legacy completed tombs stay completed (compatibility)");
        assertFalse(d.isVictory(5L) || d.isVictory(6L), "but they are NOT victories: the entrance never progresses from them");
        assertEquals(java.util.Set.of(5L, 6L), d.legacyOnlyDefeats(), "migration report");
        TombEncounterData again = roundTrip(d);
        assertFalse(again.isVictory(5L), "a save round trip does not convert them");
        assertTrue(again.adoptLegacyAsVictory(5L), "explicit, opt-in adoption exists");
        assertTrue(again.isVictory(5L) && !again.isVictory(6L));
        assertEquals(java.util.Set.of(6L), again.legacyOnlyDefeats());
    }

    @Test void victoriesSurviveSaveRestartAndVictoryImpliesDefeated() {
        TombEncounterData d = new TombEncounterData();
        d.bind(TOMB, GUARDIAN, 1L, 1L); d.markVictory(TOMB, GUARDIAN);
        TombEncounterData r = roundTrip(roundTrip(d));
        assertTrue(r.isVictory(TOMB) && r.isDefeated(TOMB) && r.legacyOnlyDefeats().isEmpty());
        var inconsistent = com.google.gson.JsonParser.parseString("{\"defeated\":[],\"active\":[],\"victories\":[9]}");
        TombEncounterData i = TombEncounterData.TYPE.codec().parse(JsonOps.INSTANCE, inconsistent).getOrThrow();
        assertTrue(i.isVictory(9L) && i.isDefeated(9L), "a victory without a defeated entry is repaired on load, never dropped");
    }

    // ---- CR-2 judgement
    @Test void withoutInstalledParamsOnlyAPlayerKillingBlowIsAVictory() {
        assertNull(TombVictoryPolicy.installed(), "no numeric default exists in production");
        var p = new TombVictoryPolicy.Participation();
        p.onPlayerDamage(P1, 150.0, 10L);
        assertTrue(p.judge(DeathCause.PLAYER, 20L, 200.0).valid(), "structural minimum: killing blow by a participating player");
        assertFalse(p.judge(DeathCause.ENVIRONMENT, 20L, 200.0).valid(), "environmental death is never an automatic victory");
        assertFalse(p.judge(DeathCause.MOB, 20L, 200.0).valid());
        assertFalse(p.judge(DeathCause.COMMAND_KILL, 20L, 200.0).valid(), "administrative kill is never a victory");
        assertFalse(new TombVictoryPolicy.Participation().judge(DeathCause.PLAYER, 1L, 200.0).valid(), "no participant, no victory");
    }

    @Test void numericRuleActivatesOnlyWhenParamsAreInstalledTestOnly() {
        TombVictoryPolicy.install(new TombVictoryPolicy.Params(0.5, 100L));   // TEST-ONLY values
        var weak = new TombVictoryPolicy.Participation(); weak.onPlayerDamage(P1, 20.0, 5L);
        assertFalse(weak.judge(DeathCause.ENVIRONMENT, 20L, 200.0).valid(), "10% share");
        var strongOld = new TombVictoryPolicy.Participation(); strongOld.onPlayerDamage(P1, 120.0, 5L);
        assertFalse(strongOld.judge(DeathCause.ENVIRONMENT, 500L, 200.0).valid(), "share ok but the last hit is outside the window");
        assertTrue(strongOld.judge(DeathCause.ENVIRONMENT, 50L, 200.0).valid(), "share ok and within the window");
        var shared = new TombVictoryPolicy.Participation(); shared.onPlayerDamage(P1, 60.0, 5L); shared.onPlayerDamage(P2, 60.0, 6L);
        var v = shared.judge(DeathCause.PLAYER, 7L, 200.0);
        assertTrue(v.valid() && v.participants().equals(java.util.Set.of(P1, P2)), "joint contribution counts together");
        assertFalse(weak.judge(DeathCause.COMMAND_KILL, 6L, 200.0).valid());
    }

    @Test void participationSurvivesAGuardianSaveAndLoad() {
        var p = new TombVictoryPolicy.Participation();
        p.onPlayerDamage(P1, 33.0, 10L); p.onPlayerDamage(P2, 7.0, 11L); p.onOtherDamage(5.0);
        var encoded = TombVictoryPolicy.Participation.CODEC.encodeStart(JsonOps.INSTANCE, p.toState()).getOrThrow();
        var q = new TombVictoryPolicy.Participation();
        q.fromState(TombVictoryPolicy.Participation.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow());
        assertEquals(p.participants(), q.participants());
        assertEquals(40.0, q.playerDamage(), 1e-9);
        assertEquals(5.0, q.otherDamage(), 1e-9);
        assertEquals(11L, q.lastPlayerHitTick());
    }

    @Test void theLethalBlowIsFoldedInBeforeJudging() {
        var p = new TombVictoryPolicy.Participation();
        p.onOtherDamage(190.0);
        p.foldKillingBlow(null, 200.0, 9L);          // environmental killing blow: remainder goes to "other"
        assertEquals(200.0, p.recordedTotal(), 1e-9);
        assertTrue(p.participants().isEmpty());
    }
}
