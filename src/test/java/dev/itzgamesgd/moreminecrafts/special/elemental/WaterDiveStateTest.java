package dev.itzgamesgd.moreminecrafts.special.elemental;

import org.junit.jupiter.api.Test;

import java.util.UUID;

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

    @Test
    void serverStopClearsAllTrackedDiveState() {
        WaterDiveRuntime.trackDive(UUID.randomUUID());
        WaterDiveRuntime.trackDive(UUID.randomUUID());
        assertEquals(2, WaterDiveRuntime.activeDiveCount());

        WaterDiveRuntime.onServerStopped(null);

        assertEquals(0, WaterDiveRuntime.activeDiveCount());
    }

    @Test
    void serverStopClearIsSafeWhenAlreadyEmpty() {
        WaterDiveRuntime.clearRuntimeState();
        assertEquals(0, WaterDiveRuntime.activeDiveCount());

        WaterDiveRuntime.onServerStopped(null);
        WaterDiveRuntime.onServerStopped(null);

        assertEquals(0, WaterDiveRuntime.activeDiveCount());
    }

    @Test
    void newServerSessionStartsWithoutPreviousDiveState() {
        WaterDiveRuntime.trackDive(UUID.randomUUID());
        assertEquals(1, WaterDiveRuntime.activeDiveCount());

        WaterDiveRuntime.onServerStopped(null);
        WaterDiveRuntime.onServerStarting(null);

        assertEquals(0, WaterDiveRuntime.activeDiveCount());
        WaterDiveRuntime.trackDive(UUID.randomUUID());
        assertEquals(1, WaterDiveRuntime.activeDiveCount());
        WaterDiveRuntime.clearRuntimeState();
    }
}
