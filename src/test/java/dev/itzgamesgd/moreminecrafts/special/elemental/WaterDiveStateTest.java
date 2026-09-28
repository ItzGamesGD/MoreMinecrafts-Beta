package dev.itzgamesgd.moreminecrafts.special.elemental;

import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        WaterDiveRuntime.trackDive(UUID.randomUUID(), Level.OVERWORLD);
        WaterDiveRuntime.trackDive(UUID.randomUUID(), Level.NETHER);
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
        WaterDiveRuntime.trackDive(UUID.randomUUID(), Level.OVERWORLD);
        assertEquals(1, WaterDiveRuntime.activeDiveCount());

        WaterDiveRuntime.onServerStopped(null);
        WaterDiveRuntime.onServerStarting(null);

        assertEquals(0, WaterDiveRuntime.activeDiveCount());
        WaterDiveRuntime.trackDive(UUID.randomUUID(), Level.OVERWORLD);
        assertEquals(1, WaterDiveRuntime.activeDiveCount());
        WaterDiveRuntime.clearRuntimeState();
    }

    @Test
    void activeDiveIsBoundToItsOriginatingDimension() {
        UUID playerId = UUID.randomUUID();
        WaterDiveRuntime.trackDive(playerId, Level.OVERWORLD);

        assertTrue(WaterDiveRuntime.isDiveInOriginLevel(playerId, Level.OVERWORLD));
        assertFalse(WaterDiveRuntime.isDiveInOriginLevel(playerId, Level.NETHER));

        WaterDiveRuntime.clearRuntimeState();
    }
}
