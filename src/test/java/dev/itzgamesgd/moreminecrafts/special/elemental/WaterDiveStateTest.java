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
        assertEquals(WaterDiveState.Phase.PILLAR, state.tick(true).phase());
        assertEquals(WaterDiveState.Phase.CLEANUP, state.tick(true).tick(true).phase());
    }

    @Test
    void launchAndDiveMovementAreIsolatedAndMateriallyAboveStage18Values() {
        assertEquals(1.25D, WaterDiveMotion.LAUNCH_VERTICAL_SPEED, 0.0D);
        assertTrue(WaterDiveMotion.LAUNCH_VERTICAL_SPEED > 0.85D);
        assertEquals(1.35D, WaterDiveMotion.DIVE_HORIZONTAL_SPEED, 0.0D);
        assertTrue(WaterDiveMotion.DIVE_HORIZONTAL_SPEED > 0.9D);

        assertEquals(new net.minecraft.world.phys.Vec3(0.0D, 1.25D, 0.0D),
                WaterDiveMotion.launchVelocity(new net.minecraft.world.phys.Vec3(0.0D, 1.0D, 0.0D)));
        net.minecraft.world.phys.Vec3 dive = WaterDiveMotion.diveVelocity(new net.minecraft.world.phys.Vec3(0.6D, -0.5D, 0.8D));
        assertEquals(1.35D, Math.sqrt(dive.x * dive.x + dive.z * dive.z), 1.0E-12D);
        assertEquals(-1.4D, dive.y, 0.0D);
    }

    @Test
    void lifetimePolicyExpiresOnDisconnectDeathDimensionChangeOrTimeout() {
        assertFalse(WaterDiveState.shouldExpire(true, true, true, 79, 80));
        assertTrue(WaterDiveState.shouldExpire(false, false, false, 0, 80));
        assertTrue(WaterDiveState.shouldExpire(true, false, true, 0, 80));
        assertTrue(WaterDiveState.shouldExpire(true, true, false, 0, 80));
        assertTrue(WaterDiveState.shouldExpire(true, true, true, 80, 80));
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
