package dev.itzgamesgd.moreminecrafts.bounty;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

class ContractCombatPoolTest {
    private static final ContractData.Tier B = ContractData.Tier.BEGINNER;
    private static final ContractData.Tier I = ContractData.Tier.INTERMEDIATE;
    private static final ContractData.Tier A = ContractData.Tier.ADVANCED;
    private static final ContractCatalog.Strength STRONG = ContractCatalog.Strength.STRONG;
    private static final ContractCatalog.Strength ORDINARY = ContractCatalog.Strength.ORDINARY;

    private static List<ContractCatalog.Mob> all() {
        List<ContractCatalog.Mob> all = new ArrayList<>(ContractCatalog.VANILLA_MOBS);
        all.addAll(ContractCatalog.MM_ORDINARY_MOBS);
        all.addAll(ContractCatalog.MM_STRONG_MOBS);
        return all;
    }

    private static List<String> everyPooled(ContractData.Tier tier) {
        List<String> ids = new ArrayList<>();
        for (ContractCatalog.Dimension d : ContractCatalog.Dimension.values())
            for (ContractCatalog.Strength s : ContractCatalog.Strength.values()) ids.addAll(ContractGenerator.mobPool(tier, d, s));
        return ids;
    }

    @Test void approvedClassWeights() {
        assertEquals(0, ContractCatalog.strongPercent(B));
        assertEquals(25, ContractCatalog.strongPercent(I));
        assertEquals(50, ContractCatalog.strongPercent(A));
        assertEquals(50, ContractCatalog.strongPercent(ContractData.Tier.SPECIAL));
    }

    @Test void sampledSharesMatchWeights() {
        RandomSource random = RandomSource.create(11);
        for (ContractData.Tier tier : List.of(B, I, A)) {
            int strong = 0;
            int total = 20000;
            for (int i = 0; i < total; i++) {
                String target = ContractGenerator.combat(tier, ContractCatalog.Dimension.OVERWORLD, random).orElseThrow().target();
                if (ContractGenerator.mobPool(tier, ContractCatalog.Dimension.OVERWORLD, STRONG).contains(target)) strong++;
            }
            assertEquals(ContractCatalog.strongPercent(tier) / 100.0, strong / (double) total, 0.02, tier.name());
        }
    }

    @Test void beginnerNeverDrawsStrong() {
        RandomSource random = RandomSource.create(12);
        for (int i = 0; i < 5000; i++) {
            ContractData.Objective o = ContractGenerator.combat(B, ContractCatalog.Dimension.OVERWORLD, random).orElseThrow();
            assertTrue(o.required() >= 8 && o.required() <= 16, "ordinary band: " + o.required());
        }
    }

    @Test void nineApprovedStrongMmMobsPreserved() {
        assertEquals(Set.of("moreminecrafts:large_zombie", "moreminecrafts:armored_skeleton",
                        "moreminecrafts:lesser_wither_skeleton", "moreminecrafts:large_spider", "moreminecrafts:swamp_spider",
                        "moreminecrafts:lesser_magma_cube", "moreminecrafts:deep_sea_drowned", "moreminecrafts:bandit_warrior",
                        "moreminecrafts:cave_creeper"),
                new HashSet<>(ContractCatalog.MM_STRONG_MOBS.stream().map(ContractCatalog.Mob::id).toList()));
    }

    @Test void intermediateOrHigherRestrictions() {
        List<String> restricted = List.of("minecraft:guardian", "minecraft:breeze", "minecraft:silverfish",
                "minecraft:phantom", "moreminecrafts:lesser_phantom", "minecraft:vindicator");
        for (String id : restricted) {
            assertFalse(everyPooled(B).contains(id), "beginner excludes " + id);
            assertTrue(everyPooled(I).contains(id), "intermediate allows " + id);
            assertTrue(everyPooled(A).contains(id), "advanced allows " + id);
        }
    }

    @Test void shulkerAndEndTargetsAreAdvancedOnly() {
        for (ContractData.Tier tier : List.of(B, I)) {
            assertFalse(everyPooled(tier).contains("minecraft:shulker"));
            for (ContractCatalog.Strength s : ContractCatalog.Strength.values())
                assertTrue(ContractGenerator.mobPool(tier, ContractCatalog.Dimension.END, s).isEmpty(), "End closed below Advanced");
        }
        assertTrue(ContractGenerator.mobPool(A, ContractCatalog.Dimension.END, ORDINARY).contains("minecraft:shulker"));
        for (ContractCatalog.Mob mob : all())
            if (mob.dimension() == ContractCatalog.Dimension.END) assertEquals(A, mob.minTier(), mob.id());
    }

    @Test void excludedPendingAndSpecialOnlyTargetsAreNeverPooled() {
        List<String> ids = new ArrayList<>(ContractCatalog.MM_EXCLUDED);
        ids.addAll(ContractCatalog.SPECIAL_ONLY_MOBS);
        ids.addAll(ContractCatalog.MM_PENDING_VERIFICATION);
        assertTrue(ids.containsAll(List.of("moreminecrafts:wild_piglin", "minecraft:elder_guardian", "minecraft:warden",
                "minecraft:ghast", "moreminecrafts:feral_wolf", "moreminecrafts:cave_bat", "moreminecrafts:giant_bee")));
        for (ContractData.Tier tier : List.of(B, I, A)) {
            List<String> pooled = everyPooled(tier);
            for (String id : ids) assertFalse(pooled.contains(id), id + " pooled for " + tier);
        }
        for (ContractCatalog.Mob mob : all()) assertFalse(ids.contains(mob.id()), mob.id());
    }

    @Test void metadataCorrections() {
        assertTrue(ContractCatalog.VANILLA_EXCLUDED.stream().anyMatch(s -> s.startsWith("minecraft:piglin_brute: always hostile")));
        assertTrue(ContractCatalog.VANILLA_EXCLUDED.stream().anyMatch(s -> s.startsWith("minecraft:zombie_villager: spawns naturally")));
        assertTrue(ContractCatalog.VANILLA_EXCLUDED.stream().noneMatch(s -> s.contains("conversion-only spawning") && s.contains("zombie_villager")));
        assertTrue(ContractCatalog.VANILLA_EXCLUDED.stream().noneMatch(s -> s.startsWith("minecraft:piglin_brute: neutral")));
    }

    private static ContractCatalog.Mob mob(String id, ContractCatalog.Dimension d, ContractCatalog.Strength s) {
        return new ContractCatalog.Mob(id, d, s);
    }

    private static final ContractCatalog.Dimension OW = ContractCatalog.Dimension.OVERWORLD;

    @Test void zeroStrongCandidatesDrawsFromOrdinaryPoolAtOrdinaryBand() {
        List<ContractCatalog.Mob> catalog = List.of(mob("a:o1", OW, ORDINARY), mob("a:o2", OW, ORDINARY));
        RandomSource random = RandomSource.create(21);
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 4000; i++) {
            ContractData.Objective o = ContractGenerator.combat(A, OW, random, catalog).orElseThrow();
            seen.add(o.target());
            assertTrue(o.required() >= 40 && o.required() <= 80, "ordinary band: " + o.required());
        }
        assertEquals(Set.of("a:o1", "a:o2"), seen);
    }

    @Test void oneStrongCandidateRestoresNormalWeights() {
        List<ContractCatalog.Mob> catalog = List.of(mob("a:o1", OW, ORDINARY), mob("a:s1", OW, STRONG));
        assertEquals(0.25, strongShare(I, catalog, 31), 0.02);
        assertEquals(0.50, strongShare(A, catalog, 32), 0.02);
        assertEquals(0.0, strongShare(B, catalog, 33), 0.0);
    }

    @Test void multipleStrongCandidatesKeepWeightsAndSpreadUniformly() {
        List<ContractCatalog.Mob> catalog = List.of(mob("a:o1", OW, ORDINARY), mob("a:s1", OW, STRONG),
                mob("a:s2", OW, STRONG), mob("a:s3", OW, STRONG));
        assertEquals(0.50, strongShare(A, catalog, 34), 0.02);
        RandomSource random = RandomSource.create(35);
        java.util.Map<String, Integer> counts = new java.util.HashMap<>();
        for (int i = 0; i < 30000; i++) {
            String t = ContractGenerator.combat(A, OW, random, catalog).orElseThrow().target();
            counts.merge(t, 1, Integer::sum);
        }
        for (String id : List.of("a:s1", "a:s2", "a:s3")) assertEquals(5000.0, counts.get(id), 400.0, id);
    }

    @Test void strongCandidateIgnoredWhenTierOrDimensionIneligibleStillFallsBack() {
        // The strong mob needs Advanced; at Intermediate the strong pool is empty, so ordinary is drawn.
        List<ContractCatalog.Mob> catalog = List.of(mob("a:o1", OW, ORDINARY),
                new ContractCatalog.Mob("a:s1", OW, STRONG, A));
        assertEquals(0.0, strongShare(I, catalog, 36), 0.0);
        assertEquals(0.5, strongShare(A, catalog, 37), 0.02);
        // A strong mob in another dimension never leaks in.
        List<ContractCatalog.Mob> other = List.of(mob("a:o1", OW, ORDINARY),
                mob("a:s1", ContractCatalog.Dimension.NETHER, STRONG));
        assertEquals(0.0, strongShare(A, other, 38), 0.0);
    }

    @Test void bothPoolsEmptyRejectsTheObjective() {
        RandomSource random = RandomSource.create(22);
        for (int i = 0; i < 200; i++) {
            assertTrue(ContractGenerator.combat(A, OW, random, List.of()).isEmpty());
            assertTrue(ContractGenerator.combat(B, ContractCatalog.Dimension.END, random).isEmpty(), "End closed below Advanced");
        }
    }

    @Test void exceptionIsNotHardcodedToTheEnd() {
        // Today the End has no strong mob; the same rule must hold for any empty strong pool, and lapse once one exists.
        assertTrue(ContractGenerator.mobPool(A, ContractCatalog.Dimension.END, STRONG).isEmpty());
        List<ContractCatalog.Mob> end = List.of(mob("a:e1", ContractCatalog.Dimension.END, ORDINARY));
        assertEquals(0.0, strongShareIn(A, ContractCatalog.Dimension.END, end, 41), 0.0);
        List<ContractCatalog.Mob> withStrong = List.of(mob("a:e1", ContractCatalog.Dimension.END, ORDINARY),
                mob("a:e2", ContractCatalog.Dimension.END, STRONG));
        assertEquals(0.5, strongShareIn(A, ContractCatalog.Dimension.END, withStrong, 42), 0.02);
        assertFalse(ContractGenerator.mobPool(A, OW, STRONG).isEmpty());
        assertEquals(0.5, strongShare(A, List.of(mob("a:o1", OW, ORDINARY), mob("a:s1", OW, STRONG)), 43), 0.02);
    }

    private static double strongShare(ContractData.Tier tier, List<ContractCatalog.Mob> catalog, long seed) {
        return strongShareIn(tier, OW, catalog, seed);
    }

    private static double strongShareIn(ContractData.Tier tier, ContractCatalog.Dimension dim,
                                        List<ContractCatalog.Mob> catalog, long seed) {
        RandomSource random = RandomSource.create(seed);
        int strong = 0;
        int total = 20000;
        Set<String> strongIds = new HashSet<>(ContractGenerator.mobPool(ContractData.Tier.ADVANCED, dim, STRONG, catalog));
        for (int i = 0; i < total; i++)
            if (strongIds.contains(ContractGenerator.combat(tier, dim, random, catalog).orElseThrow().target())) strong++;
        return strong / (double) total;
    }

    @Test void generatedObjectiveSetsNeverContainInvalidCombatTargets() {
        RandomSource random = RandomSource.create(14);
        for (ContractData.Tier shape : List.of(B, I, A)) {
            Set<String> allowed = new HashSet<>(everyPooled(shape));
            for (int i = 0; i < 3000; i++) {
                List<ContractData.Objective> set = null;
                int attempts = 0;
                while (set == null) {
                    set = ContractGenerator.tryObjectives(shape, random);
                    assertTrue(++attempts < 1000);
                }
                for (ContractData.Objective o : set)
                    if (o.category() == ContractData.Category.COMBAT) assertTrue(allowed.contains(o.target()), o.target());
            }
        }
    }

    @Test void realCatalogNeverRejectsAnObjectiveSetForAllowedDimensions() {
        RandomSource random = RandomSource.create(16);
        for (ContractData.Tier shape : List.of(B, I, A))
            for (int i = 0; i < 20000; i++) assertNotNull(ContractGenerator.tryObjectives(shape, random), shape.name());
    }

    @Test void everyTierDimensionPairCanStillProduceAContract() {
        RandomSource random = RandomSource.create(15);
        for (ContractData.Tier shape : List.of(B, I, A)) {
            int produced = 0;
            for (int i = 0; i < 200; i++) if (ContractGenerator.tryObjectives(shape, random) != null) produced++;
            assertTrue(produced > 0, shape.name());
        }
    }
}
