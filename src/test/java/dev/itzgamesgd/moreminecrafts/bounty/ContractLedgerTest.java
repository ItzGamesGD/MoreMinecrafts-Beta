package dev.itzgamesgd.moreminecrafts.bounty;

import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ContractLedgerTest {
    private static final String ID = "f87002f8-508e-4d35-84a7-4e4834ce0204";

    @Test void transactionStatesFollowTheRedemptionRules() {
        ContractLedger ledger = new ContractLedger();
        assertTrue(ledger.prepare(ID, "tx", "player"));
        assertFalse(ledger.prepare(ID, "other", "someone"), "a second claim on the same ID is refused");
        assertTrue(ledger.blocked(ID), "pending blocks every copy");
        assertFalse(ledger.commit(ID, "other"), "only the owning transaction can commit");
        assertFalse(ledger.abort(ID, "other"), "only the owning transaction can abort");
        assertEquals(1, ledger.pendingFor("player").size());
        assertTrue(ledger.commit(ID, "tx"));
        assertTrue(ledger.commit(ID, "tx"), "committing twice is harmless");
        assertFalse(ledger.abort(ID, "tx"), "a committed redemption can never be rolled back");
        assertTrue(ledger.pendingFor("player").isEmpty());
        assertTrue(ledger.blocked(ID));
    }

    @Test void abortReleasesAPendingIdOnly() {
        ContractLedger ledger = new ContractLedger();
        ledger.prepare(ID, "tx", "player");
        assertTrue(ledger.abort(ID, "tx"));
        assertFalse(ledger.blocked(ID));
        assertTrue(ledger.prepare(ID, "tx2", "player"), "the contract is redeemable again after a rollback");
    }

    @Test void healNeverOverwritesAnExistingEntry() {
        ContractLedger ledger = new ContractLedger();
        ledger.prepare(ID, "tx", "player");
        assertFalse(ledger.heal(ID, "tx-other", "player"));
        assertEquals(ContractLedger.State.PENDING, ledger.get(ID).state());
        assertTrue(ledger.heal("another-id", "tx-3", "player"));
        assertEquals(ContractLedger.State.COMMITTED, ledger.get("another-id").state());
    }

    @Test void statesSurviveCodecRoundTrip() {
        ContractLedger ledger = new ContractLedger();
        ledger.prepare(ID, "tx", "player");
        ledger.prepare("committed-id", "tx2", "player");
        ledger.commit("committed-id", "tx2");
        var encoded = ContractLedger.CODEC.encodeStart(JsonOps.INSTANCE, ledger).getOrThrow();
        ContractLedger restored = ContractLedger.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        assertEquals(ContractLedger.State.PENDING, restored.get(ID).state());
        assertEquals("tx", restored.get(ID).tx());
        assertEquals("player", restored.get(ID).player());
        assertEquals(ContractLedger.State.COMMITTED, restored.get("committed-id").state());
        assertFalse(restored.prepare("committed-id", "tx9", "x"), "duplicate redemption stays refused after a reload");
    }

    @Test void earlierBareIdFormatReadsAsCommitted() {
        JsonArray legacy = new JsonArray();
        legacy.add(new JsonPrimitive("old-id"));
        ContractLedger restored = ContractLedger.CODEC.parse(JsonOps.INSTANCE, legacy).getOrThrow();
        assertEquals(ContractLedger.State.COMMITTED, restored.get("old-id").state());
        assertTrue(restored.blocked("old-id"));
    }
}
