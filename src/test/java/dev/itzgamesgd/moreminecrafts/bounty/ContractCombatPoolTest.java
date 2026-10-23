package dev.itzgamesgd.moreminecrafts.bounty;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ContractCombatPoolTest {
    /** ContractData and its nested Objective codec initialize circularly; production always touches ContractData first. */
    @BeforeAll static void initContractDataFirst() {
        assertNotNull(ContractData.CODEC);
    }

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

    @Test void missingStrongCandidateIsNeverSubstitutedByAnOrdinaryTarget() {
        // Advanced End has an ordinary pool but no strong pool: a strong draw must yield nothing, not an ordinary mob.
        assertFalse(ContractGenerator.mobPool(A, ContractCatalog.Dimension.END, ORDINARY).isEmpty());
        assertTrue(ContractGenerator.mobPool(A, ContractCatalog.Dimension.END, STRONG).isEmpty());
        RandomSource random = RandomSource.create(13);
        int empty = 0;
        int present = 0;
        for (int i = 0; i < 4000; i++) {
            Optional<ContractData.Objective> o = ContractGenerator.combat(A, ContractCatalog.Dimension.END, random);
            if (o.isEmpty()) empty++;
            else {
                present++;
                assertTrue(ContractGenerator.mobPool(A, ContractCatalog.Dimension.END, ORDINARY).contains(o.get().target()));
                assertTrue(o.get().required() >= 40 && o.get().required() <= 80, "ordinary band only: " + o.get().required());
            }
        }
        assertEquals(0.5, empty / 4000.0, 0.05);
        assertTrue(present > 0);
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

    @Test void everyTierDimensionPairCanStillProduceAContract() {
        RandomSource random = RandomSource.create(15);
        for (ContractData.Tier shape : List.of(B, I, A)) {
            int produced = 0;
            for (int i = 0; i < 200; i++) if (ContractGenerator.tryObjectives(shape, random) != null) produced++;
            assertTrue(produced > 0, shape.name());
        }
    }
}
