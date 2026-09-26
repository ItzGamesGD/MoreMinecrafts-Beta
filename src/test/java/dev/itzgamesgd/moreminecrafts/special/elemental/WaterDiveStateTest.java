package dev.itzgamesgd.moreminecrafts.special.elemental;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WaterDiveStateTest {
    @Test
    void neverResolvesBeforeRealAirborneThenGroundTransition() {
        WaterDiveState state = WaterDiveState.launched();
        for (int tick = 0; tick < 6; tick++) state = state.tick(true);
        assertEquals(WaterDiveState.Phase.DIVING, state.phase());
        state = state.tick(true);
        assertEquals(WaterDiveState.Phase.WAITING_FOR_GROUND, state.phase());
        state = state.tick(true);
        assertEquals(WaterDiveState.Phase.WAITING_FOR_GROUND, state.phase());
        state = state.tick(false);
        assertEquals(WaterDiveState.Phase.WAITING_FOR_GROUND, state.phase());
        state = state.tick(true);
        assertEquals(WaterDiveState.Phase.RESOLVE, state.phase());
        assertEquals(WaterDiveState.Phase.CLEANUP, state.tick(true).phase());
    }
}
