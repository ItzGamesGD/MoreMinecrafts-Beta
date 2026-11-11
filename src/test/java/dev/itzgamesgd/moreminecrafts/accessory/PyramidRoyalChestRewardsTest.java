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

    @Test void chestDecidedOnceAndNeverRerolled() {
        Ledger l = new Ledger();
        assertTrue(l.claim("pyramid-A:royal-1"));
        assertFalse(l.claim("pyramid-A:royal-1"), "re-evaluation cannot decide again");
        assertTrue(l.claim("pyramid-A:royal-2"));
        assertTrue(l.claim("pyramid-B:royal-1"));
    }

    @Test void restoredLedgerBlocksDuplicates() {
        Ledger l = new Ledger();
        l.claim("a"); l.claim("b");
        var json = IDS_CODEC.encodeStart(JsonOps.INSTANCE, l.ids()).getOrThrow();
        Ledger restored = Ledger.of(IDS_CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
        assertTrue(restored.isDecided("a") && restored.isDecided("b"));
        assertFalse(restored.claim("a"));
        assertTrue(restored.claim("c"));
    }

    @Test void effectRadiusBoundaryIs16Blocks() {
        assertTrue(inEffectRange(16.0 * 16.0));
        assertFalse(inEffectRange(16.0 * 16.0 + 0.01));
        assertEquals(16.0D, EFFECT_RADIUS);
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

    @Test void sourceGuardsAndPreservation() throws Exception {
        String s = Files.readString(Path.of("src/main/java/dev/itzgamesgd/moreminecrafts/accessory/PyramidRoyalChestRewards.java"));
        assertEquals(1, s.split("table\\.withPool\\(", -1).length - 1, "single additive pool");
        assertFalse(s.contains("table.pools"));
        assertTrue(s.contains(".setWeight(1)") && s.contains("randomChance((float) CHANCE)"));
        assertTrue(s.indexOf(".when(() -> FirstEvaluation.INSTANCE)") < s.indexOf("randomChance((float) CHANCE)"),
                "claim precedes the roll");
        assertFalse(s.contains("LootTableEvents"), "attached to no table until I06");
        assertFalse(s.contains("spawnAtLocation") || s.contains("ClientboundSetTitle") || s.contains("sendSystemMessage"));
        assertFalse(s.toLowerCase().contains("luck") && s.contains("getLuck"), "no luck input");
        String main = Files.readString(Path.of("src/main/java/dev/itzgamesgd/moreminecrafts/MoreMinecrafts.java"));
        assertEquals(1, main.split("PyramidRoyalChestRewards\\.initialize\\(\\)", -1).length - 1);
        assertTrue(Files.readString(Path.of("src/main/java/dev/itzgamesgd/moreminecrafts/accessory/RelicCatalystSources.java")).contains("CATALYST_CHANCE = 0.05D"));
        assertTrue(Files.readString(Path.of("src/main/java/dev/itzgamesgd/moreminecrafts/progression/T2CoreAcquisition.java")).contains("ANCIENT_HEART, 0.03F"));
    }

}
