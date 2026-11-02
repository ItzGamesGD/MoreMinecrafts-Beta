package dev.itzgamesgd.moreminecrafts.colosseum;

import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RewardDeliveryTest {
    private static final UUID CHALLENGE = UUID.fromString("f0050000-0000-4000-8000-000000000001");

    @Test void sharedKeyIdsAreStableAndBoundToChallengeRoundAndKind() {
        assertEquals(RewardDelivery.sharedKeyId(CHALLENGE, 2, false), RewardDelivery.sharedKeyId(CHALLENGE, 2, false));
        assertNotEquals(RewardDelivery.sharedKeyId(CHALLENGE, 1, false), RewardDelivery.sharedKeyId(CHALLENGE, 2, false));
        assertNotEquals(RewardDelivery.sharedKeyId(CHALLENGE, 7, false), RewardDelivery.sharedKeyId(CHALLENGE, 7, true));
        assertNotEquals(RewardDelivery.sharedKeyId(CHALLENGE, 7, true), RewardDelivery.sharedKeyId(UUID.randomUUID(), 7, true));
    }

    @Test void plannedWithoutEffectIsSafeToRetryButIncompleteInsertIsAmbiguous() {
        RewardDelivery plan = RewardDelivery.sharedKey(CHALLENGE, 1, false);
        assertEquals(RewardDelivery.Reconcile.RETRY, plan.reconcile(0));
        plan.begin(UUID.randomUUID());
        assertEquals(RewardDelivery.Reconcile.QUARANTINE, plan.reconcile(0));
    }

    @Test void observedEntityCompletesOnceAndDuplicatesQuarantine() {
        RewardDelivery plan = RewardDelivery.sharedKey(CHALLENGE, 7, true);
        plan.begin(UUID.randomUUID());
        assertEquals(RewardDelivery.Reconcile.CONFIRMED, plan.reconcile(1));
        assertEquals(RewardDelivery.State.DELIVERED, plan.state);
        assertEquals(RewardDelivery.Reconcile.ALREADY_DELIVERED, plan.reconcile(0));
        assertEquals(RewardDelivery.Reconcile.QUARANTINE, plan.reconcile(2));
    }

    @Test void eligibilityDoesNotCreateAReceipt() {
        ArenaAttempt attempt = ArenaAttempt.start(CHALLENGE, 1, java.util.Set.of(UUID.randomUUID()), a -> null, new java.util.ArrayList<>());
        assertTrue(attempt.entitlements().isEmpty());
        assertTrue(attempt.rewardDeliveries().isEmpty());
    }

    @Test void plannedReceiptAndExistingKeyHistorySurviveControllerSerialization() {
        Sim s=new Sim();s.inside.add(Sim.A);s.tick();s.clearRound();
        var attempt=s.c.attempt();
        assertTrue(attempt.keysAwarded().contains(1));
        var reward=attempt.rewardDeliveries().get(RewardDelivery.sharedKeyId(attempt.attemptId,1,false));
        assertNotNull(reward);
        var restored=ArenaController.restore("arena-1",s.c.toJson(),null,s.now);
        assertFalse(restored.quarantined());
        assertTrue(restored.attempt().keysAwarded().contains(1));
        assertEquals(RewardDelivery.State.PLANNED,restored.attempt().rewardDeliveries().get(reward.rewardId).state);
        assertEquals(s.c.entitlements(),restored.entitlements());
        assertEquals(s.c.claims,restored.claims);
    }
}
