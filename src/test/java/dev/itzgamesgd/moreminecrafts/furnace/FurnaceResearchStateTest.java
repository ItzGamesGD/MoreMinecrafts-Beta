package dev.itzgamesgd.moreminecrafts.furnace;

import com.mojang.serialization.JsonOps;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FurnaceResearchStateTest {
    @Test
    void guiOpenEstablishesAttributionAndARealInteractionCanReplaceIt() {
        UUID opener = UUID.randomUUID();
        UUID interactingPlayer = UUID.randomUUID();
        FurnaceResearchState opened = FurnaceResearchAttribution.openedState(
                FurnaceResearchState.empty(), opener);
        assertEquals(opener, opened.attributedPlayer().orElseThrow());

        FurnaceResearchState interacted = FurnaceResearchAttribution.interactedState(opened, interactingPlayer);
        assertEquals(interactingPlayer, interacted.attributedPlayer().orElseThrow());
        assertEquals(interacted, FurnaceResearchAttribution.interactedState(interacted, interactingPlayer));
    }

    @Test
    void latestOpenOrActualInventoryInteractionWinsAndClosingHasNoResetOperation() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID third = UUID.randomUUID();
        FurnaceResearchState state = FurnaceResearchAttribution.openedState(FurnaceResearchState.empty(), first);
        state = FurnaceResearchAttribution.interactedState(state, second);
        assertEquals(second, state.attributedPlayer().orElseThrow());
        state = FurnaceResearchAttribution.openedState(state, third);
        assertEquals(third, state.attributedPlayer().orElseThrow());
    }

    @Test
    void hopperOnlyInventoryMutationDoesNotSelectAPlayer() {
        FurnaceResearchState before = FurnaceResearchState.empty();
        FurnaceResearchState afterHopperTransfer = before;
        assertEquals(before, afterHopperTransfer);
        assertTrue(afterHopperTransfer.attributedPlayer().isEmpty());
    }

    @Test
    void versionedStateCodecRoundTripsAndOldFurnacesUseEmptyDefaults() {
        UUID player = UUID.randomUUID();
        FurnaceResearchState original = new FurnaceResearchState(java.util.Optional.of(player), 0.25D, 0.75D);
        var encoded = FurnaceResearchState.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
        FurnaceResearchState restored = FurnaceResearchState.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        assertEquals(original, restored);
        assertEquals(FurnaceResearchState.empty(), FurnaceResearchState.empty());
    }

    @Test
    void malformedUuidAndOutOfRangeRemaindersAreRejected() {
        assertTrue(FurnaceResearchState.CODEC.parse(JsonOps.INSTANCE,
                com.google.gson.JsonParser.parseString("{\"attributed_player\":\"bad\"}"))
                .error().isPresent());
        assertThrows(IllegalArgumentException.class,
                () -> new FurnaceResearchState(java.util.Optional.empty(), 1.0D, 0.0D));
    }
}
