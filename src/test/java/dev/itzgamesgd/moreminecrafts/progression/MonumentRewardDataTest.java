package dev.itzgamesgd.moreminecrafts.progression;

import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MonumentRewardDataTest {
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
}
