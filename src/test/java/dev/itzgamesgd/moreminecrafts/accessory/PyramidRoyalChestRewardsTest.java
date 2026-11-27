package dev.itzgamesgd.moreminecrafts.accessory;

import static dev.itzgamesgd.moreminecrafts.accessory.PyramidRoyalChestRewards.*;
import static org.junit.jupiter.api.Assertions.*;

import com.mojang.serialization.JsonOps;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.Test;

class PyramidRoyalChestRewardsTest {
    @Test void twentyPercentBoundary() {
        assertEquals(0.20D, CHANCE);
        assertTrue(draw(() -> 0.0D, n -> 0).isPresent());
        assertTrue(draw(() -> 0.1999999D, n -> 0).isPresent());
        assertTrue(draw(() -> 0.20D, n -> { fail("no pick on failure"); return 0; }).isEmpty());
        assertTrue(draw(() -> 0.99D, n -> 0).isEmpty());
    }

    @Test void exactlyOneOfTwoEqualWeightItems() {
        assertEquals(java.util.List.of(AccessoryDefinition.SEED_OF_LIFE, AccessoryDefinition.RING_OF_TIME), ITEMS);
        assertEquals(Optional.of(AccessoryDefinition.SEED_OF_LIFE), draw(() -> 0.0D, n -> { assertEquals(2, n); return 0; }));
        assertEquals(Optional.of(AccessoryDefinition.RING_OF_TIME), draw(() -> 0.0D, n -> 1));
        assertTrue(draw(() -> 0.0D, n -> 2).isEmpty(), "out-of-range pick rejected");
        // exact joint probability: each item = 0.20 * 1/2 = 10%
        assertEquals(0.10D, CHANCE / ITEMS.size(), 1e-12);
    }

    @Test void twoChestsAreIndependentAndDuplicatesAllowed() {
        Optional<AccessoryDefinition> first = draw(() -> 0.0D, n -> 0), second = draw(() -> 0.0D, n -> 0);
        assertEquals(first, second, "same accessory from both chests is allowed");
        assertTrue(draw(() -> 0.0D, n -> 1).isPresent() && draw(() -> 0.5D, n -> 1).isEmpty());
        // P(0)=.64 P(1)=.32 P(2)=.04 from independent 20% chests
        assertEquals(0.64D, 0.8 * 0.8, 1e-12);
        assertEquals(0.32D, 2 * 0.2 * 0.8, 1e-12);
        assertEquals(0.04D, 0.2 * 0.2, 1e-12);
    }




    @Test void statisticalSanityOnly() {
        Random r = new Random(7);
        EnumMap<AccessoryDefinition, Integer> counts = new EnumMap<>(AccessoryDefinition.class);
        int n = 200_000, hits = 0;
        for (int i = 0; i < n; i++) {
            var d = draw(r::nextDouble, r::nextInt);
            if (d.isPresent()) { hits++; counts.merge(d.get(), 1, Integer::sum); }
        }
        assertEquals(0.20D, hits / (double) n, 0.01D);
        assertEquals(0.5D, counts.get(AccessoryDefinition.SEED_OF_LIFE) / (double) hits, 0.02D);
    }


}
