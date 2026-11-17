package dev.itzgamesgd.moreminecrafts.pyramid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.itzgamesgd.moreminecrafts.pyramid.RoyalChestRewardModel.Reward;
import dev.itzgamesgd.moreminecrafts.pyramid.RoyalChestRewardModel.Stack;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;

/** D13-03-I06: probabilities, quantities, rare-pool single selection, accessory/catalyst rules and stack splitting. */
class RoyalChestRewardModelTest {
    /** Replays a fixed script of doubles and ints so boundary rolls are exact. */
    private static final class Script implements RandomGenerator {
        private final List<Object> values;
        private int next;
        Script(Object... values) { this.values = List.of(values); }
        @Override public long nextLong() { throw new UnsupportedOperationException(); }
        @Override public double nextDouble() { return (Double) values.get(next++); }
        @Override public int nextInt(int bound) { int v = (Integer) values.get(next++); assertTrue(v >= 0 && v < bound, "pick " + v + " outside [0," + bound + ")"); return v; }
        int consumed() { return next; }
    }

    private static Map<String, Integer> totals(List<Reward> rewards) {
        Map<String, Integer> m = new HashMap<>();
        for (Reward r : rewards) m.merge(r.itemId(), r.count(), Integer::sum);
        return m;
    }

    @Test
    void tablesMatchTheAuthorityDocument() {
        assertEquals(List.of("minecraft:gold_ingot", "minecraft:diamond", "minecraft:emerald", "moreminecrafts:mythril"),
                RoyalChestRewardModel.PRIMARY_ORDINARY.stream().map(RoyalChestRewardModel.Entry::itemId).toList());
        assertEquals(100, RoyalChestRewardModel.PRIMARY_RARE.stream().mapToInt(RoyalChestRewardModel.RareEntry::weightPercent).sum());
        assertEquals(List.of(20, 10, 35, 35), RoyalChestRewardModel.PRIMARY_RARE.stream().map(RoyalChestRewardModel.RareEntry::weightPercent).toList());
        assertEquals(List.of("moreminecrafts:enhancement_stone", "minecraft:redstone", "minecraft:lapis_lazuli", "minecraft:diamond",
                "moreminecrafts:titanium_ingot"),
                RoyalChestRewardModel.SECONDARY_MATERIALS.stream().map(RoyalChestRewardModel.Entry::itemId).toList());
        // Q45 B: ingots only, never raw ores; Q33: 32-64.
        for (var e : RoyalChestRewardModel.SECONDARY_MATERIALS) assertFalse(e.itemId().contains("raw_"));
        var titanium = RoyalChestRewardModel.SECONDARY_MATERIALS.get(4);
        assertEquals(32, titanium.min());
        assertEquals(64, titanium.max());
        // rare items never appear in the ordinary or secondary pools
        for (var rare : RoyalChestRewardModel.PRIMARY_RARE) {
            assertTrue(RoyalChestRewardModel.PRIMARY_ORDINARY.stream().noneMatch(e -> e.itemId().equals(rare.itemId())));
            assertTrue(RoyalChestRewardModel.SECONDARY_MATERIALS.stream().noneMatch(e -> e.itemId().equals(rare.itemId())));
        }
    }

    @Test
    void primaryChanceBoundariesAreStrictlyBelowTheChance() {
        // gold(1.0) diamond(.85) emerald(.75) mythril(.25) gate(.10): just under succeeds, exactly at fails.
        List<Reward> under = RoyalChestRewardModel.rollMaterials(RoyalChestRole.PRIMARY,
                new Script(0.9999999D, 0, 0.8499999D, 0, 0.7499999D, 0, 0.2499999D, 0, 0.5D));
        assertEquals(Map.of("minecraft:gold_ingot", 64, "minecraft:diamond", 10, "minecraft:emerald", 12, "moreminecrafts:mythril", 2), totals(under));
        List<Reward> at = RoyalChestRewardModel.rollMaterials(RoyalChestRole.PRIMARY, new Script(0.9999999D, 0, 0.85D, 0.75D, 0.25D, 0.10D));
        assertEquals(Map.of("minecraft:gold_ingot", 64), totals(at));
    }

    @Test
    void quantityBoundsAreInclusive() {
        List<Reward> max = RoyalChestRewardModel.rollMaterials(RoyalChestRole.PRIMARY,
                new Script(0.0D, 64, 0.0D, 8, 0.0D, 12, 0.0D, 1, 0.99D));
        assertEquals(Map.of("minecraft:gold_ingot", 128, "minecraft:diamond", 18, "minecraft:emerald", 24, "moreminecrafts:mythril", 3), totals(max));
        List<Reward> min = RoyalChestRewardModel.rollMaterials(RoyalChestRole.SECONDARY,
                new Script(0.0D, 0, 0.0D, 0, 0.0D, 0, 0.0D, 0, 0.0D, 0));
        assertEquals(Map.of("moreminecrafts:enhancement_stone", 64, "minecraft:redstone", 64, "minecraft:lapis_lazuli", 64,
                "minecraft:diamond", 10, "moreminecrafts:titanium_ingot", 32), totals(min));
        List<Reward> top = RoyalChestRewardModel.rollMaterials(RoyalChestRole.SECONDARY,
                new Script(0.0D, 32, 0.0D, 64, 0.0D, 64, 0.0D, 8, 0.0D, 32));
        assertEquals(96, totals(top).get("moreminecrafts:enhancement_stone"));
        assertEquals(128, totals(top).get("minecraft:redstone"));
        assertEquals(64, totals(top).get("moreminecrafts:titanium_ingot"));
    }

    @Test
    void secondaryNeverRollsTheRarePoolAndHasNoGate() {
        Script script = new Script(0.0D, 0, 0.0D, 0, 0.0D, 0, 0.0D, 0, 0.99D);
        RoyalChestRewardModel.rollMaterials(RoyalChestRole.SECONDARY, script);
        assertEquals(9, script.consumed(), "five entries consume four hit pairs and one miss; no gate draw");
    }

    /** Gold always hits (chance, qty), then diamond/emerald/mythril miss, then the gate and the rare picks. */
    private static Script rareScript(double gate, Object... rest) {
        List<Object> v = new ArrayList<>(List.of(0.5D, 0, 0.99D, 0.99D, 0.99D, gate));
        v.addAll(List.of(rest));
        return new Script(v.toArray());
    }

    private static List<Reward> rareOf(List<Reward> rewards) {
        return rewards.stream().filter(r -> !r.itemId().equals("minecraft:gold_ingot")).toList();
    }

    @Test
    void rareGateIsOneSuccessThenExactlyOneItemType() {
        int[] picks = {0, 19, 20, 29, 30, 64, 65, 99};
        String[] expected = {"moreminecrafts:adamantium_ingot", "moreminecrafts:adamantium_ingot", "minecraft:netherite_ingot",
                "minecraft:netherite_ingot", "minecraft:netherite_scrap", "minecraft:netherite_scrap", "minecraft:ancient_debris", "minecraft:ancient_debris"};
        for (int i = 0; i < picks.length; i++) {
            List<Reward> rare = rareOf(RoyalChestRewardModel.rollMaterials(RoyalChestRole.PRIMARY, rareScript(0.0999999D, picks[i], 0)));
            assertEquals(1, rare.size(), "exactly one rare item type, pick " + picks[i]);
            assertEquals(expected[i], rare.get(0).itemId());
        }
        // the gate is strict: exactly 10% fails and nothing is awarded
        assertEquals(0, rareOf(RoyalChestRewardModel.rollMaterials(RoyalChestRole.PRIMARY, new Script(0.5D, 0, 0.99D, 0.99D, 0.99D, 0.10D))).size());
        // quantities: adamantium and netherite ingot x1; scrap and debris 1-2 inclusive
        assertEquals(1, rareOf(RoyalChestRewardModel.rollMaterials(RoyalChestRole.PRIMARY, rareScript(0.0D, 0))).get(0).count());
        assertEquals(1, rareOf(RoyalChestRewardModel.rollMaterials(RoyalChestRole.PRIMARY, rareScript(0.0D, 20))).get(0).count());
        assertEquals(1, rareOf(RoyalChestRewardModel.rollMaterials(RoyalChestRole.PRIMARY, rareScript(0.0D, 30, 0))).get(0).count());
        assertEquals(2, rareOf(RoyalChestRewardModel.rollMaterials(RoyalChestRole.PRIMARY, rareScript(0.0D, 30, 1))).get(0).count());
        assertEquals(2, rareOf(RoyalChestRewardModel.rollMaterials(RoyalChestRole.PRIMARY, rareScript(0.0D, 65, 1))).get(0).count());
    }

    @Test
    void sampledRatesMatchTheApprovedProbabilities() {
        int n = 400_000;
        SplittableRandom random = new SplittableRandom(20261107L);
        int rare = 0, mythril = 0, diamond = 0, emerald = 0, adamantium = 0, ingot = 0, scrap = 0, debris = 0, titanium = 0;
        for (int i = 0; i < n; i++) {
            Map<String, Integer> primary = totals(RoyalChestRewardModel.rollMaterials(RoyalChestRole.PRIMARY, random));
            assertTrue(primary.containsKey("minecraft:gold_ingot"));
            if (primary.containsKey("minecraft:diamond")) diamond++;
            if (primary.containsKey("minecraft:emerald")) emerald++;
            if (primary.containsKey("moreminecrafts:mythril")) mythril++;
            int rareTypes = 0;
            if (primary.containsKey("moreminecrafts:adamantium_ingot")) { adamantium++; rareTypes++; }
            if (primary.containsKey("minecraft:netherite_ingot")) { ingot++; rareTypes++; }
            if (primary.containsKey("minecraft:netherite_scrap")) { scrap++; rareTypes++; }
            if (primary.containsKey("minecraft:ancient_debris")) { debris++; rareTypes++; }
            assertTrue(rareTypes <= 1, "at most one rare item type");
            rare += rareTypes;
            Map<String, Integer> secondary = totals(RoyalChestRewardModel.rollMaterials(RoyalChestRole.SECONDARY, random));
            assertEquals(4, secondary.keySet().stream().filter(k -> !k.contains("ingot")).count());
            if (secondary.containsKey("moreminecrafts:titanium_ingot")) titanium++;
        }
        assertRate(rare, n, 0.10D); assertRate(adamantium, n, 0.02D); assertRate(ingot, n, 0.01D);
        assertRate(scrap, n, 0.035D); assertRate(debris, n, 0.035D);
        assertRate(diamond, n, 0.85D); assertRate(emerald, n, 0.75D); assertRate(mythril, n, 0.25D);
        assertRate(titanium, n, 0.75D);
    }

    private static void assertRate(int hits, int n, double p) {
        double sigma = Math.sqrt(p * (1 - p) / n);
        assertEquals(p, hits / (double) n, 5 * sigma, "rate " + hits + "/" + n + " vs " + p);
    }

    @Test
    void accessoryIsTwentyPercentFiftyFiftyAndIndependentOfMaterials() {
        assertEquals(Optional.empty(), RoyalChestRewardModel.rollAccessory(new Script(0.2D)));
        assertEquals(Optional.of("moreminecrafts:seed_of_life"), RoyalChestRewardModel.rollAccessory(new Script(0.1999999D, 0)));
        assertEquals(Optional.of("moreminecrafts:ring_of_time"), RoyalChestRewardModel.rollAccessory(new Script(0.0D, 1)));
        int n = 400_000, seed = 0, ring = 0, both = 0, neither = 0, sameType = 0;
        SplittableRandom random = new SplittableRandom(7L);
        for (int i = 0; i < n; i++) {
            Optional<String> a = RoyalChestRewardModel.rollAccessory(random), b = RoyalChestRewardModel.rollAccessory(random);
            for (Optional<String> x : List.of(a, b)) { if (x.isPresent()) { if (x.get().endsWith("seed_of_life")) seed++; else ring++; } }
            if (a.isPresent() && b.isPresent()) { both++; if (a.equals(b)) sameType++; }
            if (a.isEmpty() && b.isEmpty()) neither++;
        }
        assertRate(seed + ring, 2 * n, 0.20D);
        assertRate(seed, 2 * n, 0.10D); assertRate(ring, 2 * n, 0.10D);
        assertRate(neither, n, 0.64D); assertRate(both, n, 0.04D); assertRate(sameType, n, 0.02D);
    }

    @Test
    void catalystIsOneStructureRollUnifiedOntoPrimaryAndAtMostOne() {
        assertEquals(Optional.empty(), RoyalChestRewardModel.assignCatalyst(new Script(0.05D)));
        assertEquals(Optional.of(RoyalChestRole.PRIMARY), RoyalChestRewardModel.assignCatalyst(new Script(0.0499999D)));
        assertEquals(Optional.of(RoyalChestRole.PRIMARY), RoyalChestRewardModel.assignCatalyst(new Script(0.0D)));
        int n = 1_000_000, none = 0, primary = 0, secondary = 0;
        SplittableRandom random = new SplittableRandom(99L);
        for (int i = 0; i < n; i++) {
            Optional<RoyalChestRole> role = RoyalChestRewardModel.assignCatalyst(random);
            if (role.isEmpty()) none++; else if (role.get() == RoyalChestRole.PRIMARY) primary++; else secondary++;
        }
        assertRate(none, n, 0.95D);
        assertRate(primary, n, 0.05D);
        assertEquals(0, secondary, "no catalyst assigned to secondary in single primary model");
    }

    @Test
    void catalystIsOnlyDeliveredToItsAssignedChestAndDoesNotReplaceOtherRewards() {
        List<Reward> assigned = RoyalChestRewardModel.primaryRewards(new SplittableRandom(5L), true);
        List<Reward> unassigned = RoyalChestRewardModel.primaryRewards(new SplittableRandom(5L), false);
        assertEquals(1, assigned.stream().filter(r -> r.itemId().equals(RoyalChestRewardModel.CATALYST_ID)).count());
        assertEquals(0, unassigned.stream().filter(r -> r.itemId().equals(RoyalChestRewardModel.CATALYST_ID)).count());
        List<Reward> withoutCatalyst = new ArrayList<>(assigned);
        withoutCatalyst.removeIf(r -> r.itemId().equals(RoyalChestRewardModel.CATALYST_ID));
        assertEquals(unassigned, withoutCatalyst, "the catalyst is additive");
    }

    @Test
    void aChestResultDependsOnlyOnItsOwnStreamSoOpeningOrderCannotMatter() {
        for (long seed = 0; seed < 50; seed++) {
            String id = "pyramid-" + seed;
            var primaryFirst = new ArrayList<List<Reward>>();
            for (RoyalChestRole role : List.of(RoyalChestRole.PRIMARY, RoyalChestRole.SECONDARY))
                primaryFirst.add(RoyalChestRewardModel.chestRewards(role, RoyalChestRuntime.chestRandom(seed, id, role), false));
            var secondaryFirst = new ArrayList<List<Reward>>();
            for (RoyalChestRole role : List.of(RoyalChestRole.SECONDARY, RoyalChestRole.PRIMARY))
                secondaryFirst.add(RoyalChestRewardModel.chestRewards(role, RoyalChestRuntime.chestRandom(seed, id, role), false));
            assertEquals(primaryFirst.get(0), secondaryFirst.get(1));
            assertEquals(primaryFirst.get(1), secondaryFirst.get(0));
        }
    }

    @Test
    void streamsAreStableAcrossRepeatedDerivationAndDifferPerInstanceAndRole() {
        var a = RoyalChestRewardModel.chestRewards(RoyalChestRole.PRIMARY, RoyalChestRuntime.chestRandom(1L, "x", RoyalChestRole.PRIMARY), false);
        var again = RoyalChestRewardModel.chestRewards(RoyalChestRole.PRIMARY, RoyalChestRuntime.chestRandom(1L, "x", RoyalChestRole.PRIMARY), false);
        assertEquals(a, again, "recomputing after a crash yields the identical draw, never a re-roll");
        assertTrue(RoyalChestRuntime.mix(1L, "x", 0x10L) != RoyalChestRuntime.mix(1L, "x", 0x11L));
        assertTrue(RoyalChestRuntime.mix(1L, "x", 0x10L) != RoyalChestRuntime.mix(1L, "y", 0x10L));
        assertTrue(RoyalChestRuntime.mix(1L, "x", 0x10L) != RoyalChestRuntime.mix(2L, "x", 0x10L));
    }

    @Test
    void splitUsesTheRealStackSizeAndNeverTruncates() {
        List<Reward> rewards = List.of(new Reward("minecraft:gold_ingot", 128), new Reward("minecraft:diamond", 18),
                new Reward("moreminecrafts:relic_catalyst", 1), new Reward("minecraft:redstone", 100));
        List<Stack> stacks = RoyalChestRewardModel.split(rewards, id -> id.equals("moreminecrafts:relic_catalyst") ? 1 : 64);
        assertEquals(List.of(new Stack("minecraft:gold_ingot", 64), new Stack("minecraft:gold_ingot", 64), new Stack("minecraft:diamond", 18),
                new Stack("moreminecrafts:relic_catalyst", 1), new Stack("minecraft:redstone", 64), new Stack("minecraft:redstone", 36)), stacks);
        // a smaller real stack size splits further
        List<Stack> sixteen = RoyalChestRewardModel.split(List.of(new Reward("x:y", 33)), id -> 16);
        assertEquals(List.of(16, 16, 1), sixteen.stream().map(Stack::count).toList());
        assertEquals(33, sixteen.stream().mapToInt(Stack::count).sum());
    }

    @Test
    void stacksArePlacedInDistinctRandomSlotsWithoutLoss() {
        List<Stack> stacks = List.of(new Stack("minecraft:gold_ingot", 64),
                new Stack("minecraft:gold_ingot", 17), new Stack("minecraft:diamond", 10));
        List<RoyalChestRewardModel.Placement> placements = RoyalChestRewardModel.placeRandomly(stacks,
                new Script(17, 0, 2));
        assertEquals(List.of(new RoyalChestRewardModel.Placement(17, stacks.get(0)),
                new RoyalChestRewardModel.Placement(0, stacks.get(1)),
                new RoyalChestRewardModel.Placement(3, stacks.get(2))), placements);
        assertEquals(placements.size(), placements.stream().map(RoyalChestRewardModel.Placement::slot).distinct().count());
        assertEquals(totals(List.of(new Reward("minecraft:gold_ingot", 64),
                new Reward("minecraft:gold_ingot", 17), new Reward("minecraft:diamond", 10))),
                totals(placements.stream().map(p -> new Reward(p.stack().itemId(), p.stack().count())).toList()));
    }

    @Test
    void rewardAndSlotLayoutReplayFromTheSameChestRandomStream() {
        for (long seed = 0; seed < 100; seed++) {
            String instance = "slot-layout-" + seed;
            List<RoyalChestRewardModel.Placement> first = rollAndPlace(seed, instance);
            List<RoyalChestRewardModel.Placement> replay = rollAndPlace(seed, instance);
            assertEquals(first, replay);
        }
    }

    private static List<RoyalChestRewardModel.Placement> rollAndPlace(long seed, String instance) {
        RandomGenerator random = RoyalChestRuntime.chestRandom(seed, instance, RoyalChestRole.PRIMARY);
        List<Reward> rewards = RoyalChestRewardModel.chestRewards(RoyalChestRole.PRIMARY, random, false);
        List<Stack> stacks = RoyalChestRewardModel.split(rewards, id -> id.equals("moreminecrafts:seed_of_life")
                || id.equals("moreminecrafts:ring_of_time") || id.contains("catalyst") ? 1 : 64);
        return RoyalChestRewardModel.placeRandomly(stacks, random);
    }

    @Test
    void theLargestPossibleChestsFitIn54SlotsAndOversizeFailsLoudly() {
        // worst case: every roll hits its maximum, plus accessory and catalyst
        List<Reward> primary = RoyalChestRewardModel.chestRewards(RoyalChestRole.PRIMARY,
                new Script(0.0D, 64, 0.0D, 8, 0.0D, 12, 0.0D, 1, 0.0D, 30, 1, 0.0D, 1, 0.0D, 0.0D /* two sap rolls */, 0.0D /* soul glass */), true);
        assertTrue(RoyalChestRewardModel.split(primary, id -> id.contains("catalyst") || id.contains("of_") ? 1 : 64).size() <= 54);
        List<Reward> secondary = RoyalChestRewardModel.chestRewards(RoyalChestRole.SECONDARY,
                new Script(0.0D, 32, 0.0D, 64, 0.0D, 64, 0.0D, 8, 0.0D, 32, 0.0D, 0), true);
        assertTrue(RoyalChestRewardModel.split(secondary, id -> id.contains("catalyst") || id.contains("of_") ? 1 : 64).size() <= 54);
        assertThrows(IllegalStateException.class, () -> RoyalChestRewardModel.split(List.of(new Reward("a:b", 55)), id -> 1));
    }

    @Test
    void accessoryDetectionDrivesTheRevealEffectOnly() {
        assertTrue(RoyalChestRewardModel.hasAccessory(List.of(new Reward("minecraft:gold_ingot", 64), new Reward("moreminecrafts:seed_of_life", 1))));
        assertTrue(RoyalChestRewardModel.hasAccessory(List.of(new Reward("moreminecrafts:ring_of_time", 1))));
        assertFalse(RoyalChestRewardModel.hasAccessory(List.of(new Reward("minecraft:gold_ingot", 64), new Reward("moreminecrafts:relic_catalyst", 1))));
        assertFalse(RoyalChestRewardModel.hasAccessory(List.of()));
    }

    @Test void sapIsTwoIndependentRollsAtOneHundredAndFortyFivePercentOneEachMaxTwo() {
        assertEquals(List.of(1.00D, 0.45D), RoyalChestRewardModel.SAP_ROLL_CHANCES);
        assertEquals("moreminecrafts:ancient_world_tree_sap", RoyalChestRewardModel.SAP_ID);
        // first roll always succeeds; second succeeds strictly below 0.45
        assertEquals(1, RoyalChestRewardModel.rollSap(new Script(0.9999999D, 0.45D)).orElseThrow().count());
        assertEquals(2, RoyalChestRewardModel.rollSap(new Script(0.9999999D, 0.4499999D)).orElseThrow().count());
        assertEquals(2, RoyalChestRewardModel.rollSap(new Script(0.0D, 0.0D)).orElseThrow().count());
        Script s = new Script(0.0D, 0.0D);
        RoyalChestRewardModel.rollSap(s);
        assertEquals(2, s.consumed(), "exactly one chance draw per roll");
    }

    @Test void primaryChestGetsSapAndSecondaryNeverDoesAndEarlierDrawsAreUnchanged() {
        var with = RoyalChestRewardModel.chestRewards(RoyalChestRole.PRIMARY, new java.util.Random(7), false);
        var base = new java.util.ArrayList<>(RoyalChestRewardModel.rollMaterials(RoyalChestRole.PRIMARY, new java.util.Random(7)));
        // same seed: materials identical to the pre-existing draw sequence
        assertTrue(with.containsAll(base));
        assertTrue(with.stream().anyMatch(r -> r.itemId().equals(RoyalChestRewardModel.SAP_ID)), "first roll is 100%");
        for (int seed = 0; seed < 200; seed++)
            assertTrue(RoyalChestRewardModel.chestRewards(RoyalChestRole.SECONDARY, new java.util.Random(seed), false).stream()
                    .noneMatch(r -> r.itemId().equals(RoyalChestRewardModel.SAP_ID)));
        int primaryWithTwo = 0, n = 20000;
        java.util.Random random = new java.util.Random(12345);
        for (int i = 0; i < n; i++) {
            int sap = totals(RoyalChestRewardModel.chestRewards(RoyalChestRole.PRIMARY, random, false)).getOrDefault(RoyalChestRewardModel.SAP_ID, 0);
            assertTrue(sap == 1 || sap == 2);
            if (sap == 2) primaryWithTwo++;
        }
        assertEquals(0.45D, primaryWithTwo / (double) n, 0.02D);
    }

    @Test void existingPrimaryProbabilitiesAndRareGateAreUnchanged() {
        assertEquals(List.of(1.00D, 0.85D, 0.75D, 0.25D), RoyalChestRewardModel.PRIMARY_ORDINARY.stream().map(RoyalChestRewardModel.Entry::chance).toList());
        assertEquals(0.10D, RoyalChestRewardModel.RARE_GATE_CHANCE);
        assertEquals(List.of(20, 10, 35, 35), RoyalChestRewardModel.PRIMARY_RARE.stream().map(RoyalChestRewardModel.RareEntry::weightPercent).toList());
    }

    @Test void soulGlassIsOneIndependentFortyPercentRollForOneOnPrimaryOnly() {
        assertEquals(0.40D, RoyalChestRewardModel.SOUL_GLASS_CHANCE);
        assertEquals(1, RoyalChestRewardModel.SOUL_GLASS_COUNT);
        assertEquals("moreminecrafts:shimmering_soul_glass", RoyalChestRewardModel.SOUL_GLASS_ID);
        assertEquals(Optional.empty(), RoyalChestRewardModel.rollSoulGlass(new Script(0.40D)));
        assertEquals(1, RoyalChestRewardModel.rollSoulGlass(new Script(0.3999999D)).orElseThrow().count());
        Script s = new Script(0.0D);
        RoyalChestRewardModel.rollSoulGlass(s);
        assertEquals(1, s.consumed(), "exactly one chance draw");
        for (int seed = 0; seed < 500; seed++)
            assertTrue(RoyalChestRewardModel.chestRewards(RoyalChestRole.SECONDARY, new java.util.Random(seed), false).stream()
                    .noneMatch(r -> r.itemId().equals(RoyalChestRewardModel.SOUL_GLASS_ID)));
    }

    /** Same statistical style as the sap test: fixed seed, large n, tolerance; plus an exact replay of the earlier draws. */
    @Test void soulGlassRollDoesNotShiftExistingPrimaryDrawsOrProbabilities() {
        int n = 20000, withGlass = 0;
        java.util.Random random = new java.util.Random(54321);
        for (int i = 0; i < n; i++) {
            long seed = random.nextLong();
            var rewards = RoyalChestRewardModel.chestRewards(RoyalChestRole.PRIMARY, new java.util.Random(seed), false);
            var t = totals(rewards);
            int glass = t.getOrDefault(RoyalChestRewardModel.SOUL_GLASS_ID, 0);
            assertTrue(glass == 0 || glass == 1);
            withGlass += glass;
            // Everything except the glass is exactly what the pre-existing draw sequence (materials, accessory, sap) gives.
            java.util.Random replay = new java.util.Random(seed);
            var expected = new java.util.ArrayList<>(RoyalChestRewardModel.rollMaterials(RoyalChestRole.PRIMARY, replay));
            RoyalChestRewardModel.rollAccessory(replay).ifPresent(id -> expected.add(new Reward(id, 1)));
            RoyalChestRewardModel.rollSap(replay).ifPresent(expected::add);
            var actual = new java.util.ArrayList<>(rewards);
            actual.removeIf(r -> r.itemId().equals(RoyalChestRewardModel.SOUL_GLASS_ID));
            assertEquals(expected, actual);
        }
        assertEquals(0.40D, withGlass / (double) n, 0.02D);
    }
}
