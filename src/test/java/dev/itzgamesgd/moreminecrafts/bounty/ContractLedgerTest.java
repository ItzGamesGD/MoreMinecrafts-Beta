package dev.itzgamesgd.moreminecrafts.bounty;

import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ContractLedgerTest {
    @Test void duplicateRedemptionIsRejectedAndSurvivesCodecRoundTrip() {
        ContractLedger ledger = new ContractLedger();
        String id = "f87002f8-508e-4d35-84a7-4e4834ce0204";
        assertTrue(ledger.claim(id));
        assertFalse(ledger.claim(id));
        var encoded = ContractLedger.CODEC.encodeStart(JsonOps.INSTANCE, ledger).getOrThrow();
        var restored = ContractLedger.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        assertFalse(restored.claim(id));
    }
}
