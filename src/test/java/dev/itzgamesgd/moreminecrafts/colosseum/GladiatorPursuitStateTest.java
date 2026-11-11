package dev.itzgamesgd.moreminecrafts.colosseum;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class GladiatorPursuitStateTest {
    @Test void acceptsOnlyThePersistedTwentyTickWindowAndFiniteNonnegativeDistances() {
        assertTrue(new GladiatorPursuitState(0, GladiatorPursuitState.INITIAL_DISTANCE).valid());
        assertTrue(new GladiatorPursuitState(19, 16.25).valid());
        assertFalse(new GladiatorPursuitState(-1, 4).valid());
        assertFalse(new GladiatorPursuitState(20, 4).valid());
        assertFalse(new GladiatorPursuitState(2, Double.NaN).valid());
        assertFalse(new GladiatorPursuitState(2, Double.POSITIVE_INFINITY).valid());
        assertFalse(new GladiatorPursuitState(2, -0.1).valid());
    }

    @Test void rejectsAnInitialDistancePairedWithAnInProgressSample() {
        assertFalse(new GladiatorPursuitState(1, GladiatorPursuitState.INITIAL_DISTANCE).valid());
    }
}
