package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.itzgamesgd.moreminecrafts.trade.PiglinTradeStock;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PiglinTradeStockTest {
    private static final long DAY = 24_000L;

    @Test void periodIsFiveInGameDays() {
        assertEquals(5 * DAY, PiglinTradeStock.PERIOD_TICKS);
        assertEquals(0, PiglinTradeStock.cycleOf(5 * DAY - 1));
        assertEquals(1, PiglinTradeStock.cycleOf(5 * DAY));
    }

    @Test void soldOutOffersRestockAtTheNextCycleOnly() {
        PiglinTradeStock stock = new PiglinTradeStock();
        Map<String, Integer> max = Map.of("a", 2, "b", 4);
        stock.restockIfDue(0, max); // establish cycle 0 first
        stock.recordUse("a", 2);
        stock.recordUse("b", 1);
        assertFalse(stock.restockIfDue(5 * DAY - 1, max), "same cycle: no reroll");
        assertEquals(2, stock.usesOf("a"));
        assertTrue(stock.restockIfDue(5 * DAY, max), "new cycle: sold-out item rerolls");
        assertEquals(0, stock.usesOf("a"));
        assertEquals(1, stock.usesOf("b"), "item with stock left is untouched (rule not approved)");
    }

    @Test void skippedCyclesCollapseIntoOneRestock() {
        PiglinTradeStock stock = new PiglinTradeStock();
        Map<String, Integer> max = Map.of("a", 1);
        stock.restockIfDue(0, max);
        stock.recordUse("a", 1);
        stock.restockIfDue(100 * DAY, max);
        assertEquals(0, stock.usesOf("a"));
    }
}
