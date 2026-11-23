package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.itzgamesgd.moreminecrafts.trade.PiglinTradeConfig;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PiglinTradeConfigTest {
    @Test void closedByDefaultEvenForMaxedPlayers() {
        assertFalse(PiglinTradeConfig.CLOSED.unlocked(id -> 99));
    }
    @Test void blankIdIsClosed() {
        assertFalse(new PiglinTradeConfig(Optional.of(" ")).unlocked(id -> 99));
    }
    @Test void requiresLevelOfConfiguredResearchOnly() {
        PiglinTradeConfig c = new PiglinTradeConfig(Optional.of("x"));
        assertFalse(c.unlocked(id -> id.equals("x") ? 0 : 5));
        assertTrue(c.unlocked(id -> id.equals("x") ? 1 : 0));
    }
}
