package dev.itzgamesgd.moreminecrafts.bounty;

import static org.junit.jupiter.api.Assertions.*;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

/**
 * Seeded checks of whole generated contracts against the approved tables in BOUNTY_CONTRACT_APPROVED_2026-10-22.md:
 * Q27/Q32 reward rarity, Q31/Q35 quantities with item caps, Q34 payout, the per-objective count bands, and the 5%
 * Advanced-to-Special conversion. Tolerances are five standard errors of the sample, so a correct generator does not
 * fail by chance while a shifted threshold does.
 */
class ContractDistributionTest {
    private static final String ID = "f87002f8-508e-4d35-84a7-4e4834ce0204";
    private static final int SAMPLES = 200_000;
    private static final List<ContractData.Tier> ORDINARY_TIERS = List.of(ContractData.Tier.BEGINNER,
            ContractData.Tier.INTERMEDIATE, ContractData.Tier.ADVANCED);

    private static final Map<String, ContractCatalog.Strength> STRENGTH = new HashMap<>();
    static {
        Stream.of(ContractCatalog.VANILLA_MOBS, ContractCatalog.MM_ORDINARY_MOBS, ContractCatalog.MM_STRONG_MOBS)
                .flatMap(List::stream).forEach(mob -> STRENGTH.put(mob.id(), mob.strength()));
    }

    private static void assertShare(double expected, int hits, int samples, String label) {
        double observed = hits / (double) samples;
        double tolerance = Math.max(5 * Math.sqrt(expected * (1 - expected) / samples), 1.0 / samples);
        assertEquals(expected, observed, tolerance, label + " observed " + observed);
    }

    private static ContractEconomy.Rarity rarityOf(String item) {
        for (ContractEconomy.Rarity rarity : ContractEconomy.Rarity.values())
            if (ContractRewards.pool(rarity).contains(item)) return rarity;
        throw new AssertionError("reward item outside the approved pool: " + item);
    }

    @Test void sameSeedGeneratesTheSameContract() {
        for (ContractData.Tier tier : ContractData.Tier.values()) {
            for (long seed = 0; seed < 200; seed++) {
                ContractData first = ContractGenerator.generate(ID, tier, RandomSource.create(seed));
                ContractData second = ContractGenerator.generate(ID, tier, RandomSource.create(seed));
                assertEquals(first, second, tier + " seed " + seed);
            }
        }
        Set<ContractData> distinct = new HashSet<>();
        for (long seed = 0; seed < 200; seed++)
            distinct.add(ContractGenerator.generate(ID, ContractData.Tier.ADVANCED, RandomSource.create(seed)));
        assertTrue(distinct.size() > 190, "different seeds give different contracts: " + distinct.size());
    }

    @Test void rewardPoolsAreDisjointSoRarityIsRecoverableFromTheItem() {
        Set<String> seen = new HashSet<>();
        for (ContractEconomy.Rarity rarity : ContractEconomy.Rarity.values())
            for (String item : ContractRewards.pool(rarity)) assertTrue(seen.add(item), item + " is in two rarities");
    }

    @Test void rewardRarityFollowsQ27AndQ32() {
        Map<ContractData.Tier, double[]> approved = Map.of(
                ContractData.Tier.BEGINNER, new double[]{0.80, 0.17, 0.028, 0.002},
                ContractData.Tier.INTERMEDIATE, new double[]{0.60, 0.30, 0.09, 0.01},
                ContractData.Tier.ADVANCED, new double[]{0.40, 0.40, 0.17, 0.03},
                ContractData.Tier.SPECIAL, new double[]{0.20, 0.45, 0.30, 0.05});
        RandomSource random = RandomSource.create(20261025L);
        for (var row : approved.entrySet()) {
            Map<ContractEconomy.Rarity, Integer> counts = new EnumMap<>(ContractEconomy.Rarity.class);
            for (int i = 0; i < SAMPLES; i++)
                counts.merge(rarityOf(ContractGenerator.generate(ID, row.getKey(), random).reward().item()), 1, Integer::sum);
            for (ContractEconomy.Rarity rarity : ContractEconomy.Rarity.values())
                assertShare(row.getValue()[rarity.ordinal()], counts.getOrDefault(rarity, 0), SAMPLES, row.getKey() + "/" + rarity);
        }
    }

    @Test void rewardQuantitiesStayInsideQ31BandsAndItemCapsAndReachBothEnds() {
        RandomSource random = RandomSource.create(31L);
        Map<String, int[]> observed = new HashMap<>();
        for (ContractData.Tier tier : ContractData.Tier.values()) {
            for (int i = 0; i < SAMPLES / 4; i++) {
                ContractData.Reward reward = ContractGenerator.generate(ID, tier, random).reward();
                int[] allowed = ContractRewards.quantityRange(reward.item(), rarityOf(reward.item()));
                assertTrue(reward.count() >= allowed[0] && reward.count() <= allowed[1], reward + " outside " + allowed[0] + "-" + allowed[1]);
                observed.computeIfAbsent(reward.item(), ignored -> new int[]{Integer.MAX_VALUE, 0});
                observed.get(reward.item())[0] = Math.min(observed.get(reward.item())[0], reward.count());
                observed.get(reward.item())[1] = Math.max(observed.get(reward.item())[1], reward.count());
            }
        }
        assertEquals(2, ContractRewards.quantityRange("minecraft:diamond", ContractEconomy.Rarity.RARE)[1], "diamond cap");
        assertEquals(4, ContractRewards.quantityRange("moreminecrafts:titanium_ingot", ContractEconomy.Rarity.RARE)[1], "titanium cap");
        assertEquals(1, ContractRewards.quantityRange("moreminecrafts:adamantium_ingot", ContractEconomy.Rarity.LEGENDARY)[1], "adamantium cap");
        for (ContractEconomy.Rarity rarity : ContractEconomy.Rarity.values()) {
            for (String item : ContractRewards.pool(rarity)) {
                int[] range = ContractRewards.quantityRange(item, rarity);
                assertArrayEquals(range, observed.get(item), item + " covers its whole allowed range");
            }
        }
    }

    @Test void emeraldPayoutCoversExactlyTheQ34Band() {
        RandomSource random = RandomSource.create(34L);
        for (ContractData.Tier tier : ContractData.Tier.values()) {
            int[] band = ContractEconomy.band(tier);
            int min = Integer.MAX_VALUE, max = 0;
            for (int i = 0; i < 20_000; i++) {
                int emeralds = ContractGenerator.generate(ID, tier, random).reward().emeralds();
                min = Math.min(min, emeralds);
                max = Math.max(max, emeralds);
            }
            assertEquals(band[2], min, tier + " minimum payout");
            assertEquals(band[3], max, tier + " maximum payout");
        }
    }

    @Test void everyObjectiveStaysInsideItsApprovedCountBand() {
        RandomSource random = RandomSource.create(101L);
        for (ContractData.Tier tier : ContractData.Tier.values()) {
            ContractData.Tier shape = tier == ContractData.Tier.SPECIAL ? ContractData.Tier.ADVANCED : tier;
            Map<String, int[]> seen = new HashMap<>();
            for (int i = 0; i < 50_000; i++) {
                ContractData data = ContractGenerator.generate(ID, tier, random);
                assertEquals(ContractData.Status.OFFERED, data.status(), "generated contracts start unaccepted");
                for (ContractData.Objective objective : data.objectives()) {
                    assertEquals(0, objective.progress(), "generated progress starts at zero");
                    if (objective.category() == ContractData.Category.COMBAT)
                        assertNotNull(STRENGTH.get(objective.target()), "combat target is catalogued: " + objective.target());
                    int[] band = switch (objective.category()) {
                        case COMBAT -> STRENGTH.get(objective.target()) == ContractCatalog.Strength.STRONG
                                ? ContractGenerator.strongKillBand(shape) : ContractGenerator.ordinaryKillBand(shape);
                        case DELIVERY -> objective.target().equals("minecraft:diamond") ? new int[]{2, 4}
                                : objective.target().equals("moreminecrafts:adamantium_ingot") ? new int[]{1, 1}
                                : ContractCatalog.DELIVERY_RARE.contains(objective.target())
                                ? ContractGenerator.rareDeliveryBand(shape) : ContractGenerator.ordinaryDeliveryBand(shape);
                        case EXPLORATION -> ContractGenerator.explorationBand(shape);
                        case SPECIAL -> throw new AssertionError("special objectives are not generated");
                    };
                    assertTrue(objective.required() >= band[0] && objective.required() <= band[1],
                            tier + " " + objective + " outside " + band[0] + "-" + band[1]);
                    String key = objective.category() + ":" + band[0] + "-" + band[1];
                    seen.computeIfAbsent(key, ignored -> new int[]{Integer.MAX_VALUE, 0});
                    seen.get(key)[0] = Math.min(seen.get(key)[0], objective.required());
                    seen.get(key)[1] = Math.max(seen.get(key)[1], objective.required());
                }
            }
            for (var entry : seen.entrySet()) {
                String[] range = entry.getKey().substring(entry.getKey().indexOf(':') + 1).split("-");
                assertEquals(Integer.parseInt(range[0]), entry.getValue()[0], tier + " " + entry.getKey() + " minimum reached");
                assertEquals(Integer.parseInt(range[1]), entry.getValue()[1], tier + " " + entry.getKey() + " maximum reached");
            }
        }
    }

    @Test void deliveryPoolsAreDisjointAndAtMostOneHighValueDeliveryPerContract() {
        Set<String> ordinary = new HashSet<>(ContractCatalog.DELIVERY_ORDINARY);
        for (String rare : ContractCatalog.DELIVERY_RARE) assertFalse(ordinary.contains(rare), rare + " is in both pools");
        for (String high : ContractCatalog.HIGH_VALUE_DELIVERY) {
            assertFalse(ordinary.contains(high), high + " also ordinary");
            assertFalse(ContractCatalog.DELIVERY_RARE.contains(high), high + " also rare");
        }
        RandomSource random = RandomSource.create(102L);
        for (int i = 0; i < 50_000; i++) {
            long high = ContractGenerator.generate(ID, ContractData.Tier.ADVANCED, random).objectives().stream()
                    .filter(objective -> ContractCatalog.HIGH_VALUE_DELIVERY.contains(objective.target())).count();
            assertTrue(high <= 1, "high-value deliveries in one contract: " + high);
        }
    }

    @Test void advancedAcquisitionConvertsToSpecialAtFivePercentAndOtherTiersNever() {
        RandomSource random = RandomSource.create(42L);
        int specials = 0;
        for (int i = 0; i < SAMPLES; i++) {
            ContractData data = ContractGenerator.acquire(ContractData.Tier.ADVANCED, random);
            if (data.tier() == ContractData.Tier.SPECIAL) specials++;
            else assertEquals(ContractData.Tier.ADVANCED, data.tier());
        }
        assertShare(0.05, specials, SAMPLES, "Advanced to Special conversion");
        for (ContractData.Tier tier : List.of(ContractData.Tier.BEGINNER, ContractData.Tier.INTERMEDIATE))
            for (int i = 0; i < 20_000; i++) assertEquals(tier, ContractGenerator.acquire(tier, random).tier());
    }

    @Test void acquisitionGivesEveryContractAFreshIdentity() {
        RandomSource random = RandomSource.create(43L);
        Set<String> ids = new HashSet<>();
        for (ContractData.Tier tier : ORDINARY_TIERS)
            for (int i = 0; i < 10_000; i++) assertTrue(ids.add(ContractGenerator.acquire(tier, random).id()), "duplicate id");
    }

    @Test void structureTierRollMatchesTheApprovedConditionalDistributions() {
        for (ContractEconomy.Difficulty difficulty : ContractEconomy.Difficulty.values()) {
            int[] weights = ContractEconomy.tierWeights(difficulty);
            assertEquals(100, weights[0] + weights[1] + weights[2], difficulty + " weights total 100");
            int[] counts = new int[3];
            for (int roll = 0; roll < 100; roll++) counts[ContractEconomy.structureTier(difficulty, roll).ordinal()]++;
            assertArrayEquals(weights, counts, difficulty + " structureTier agrees with the loot pool weights");
        }
        assertArrayEquals(new int[]{75, 22, 3}, ContractEconomy.tierWeights(ContractEconomy.Difficulty.EASY));
        assertArrayEquals(new int[]{45, 40, 15}, ContractEconomy.tierWeights(ContractEconomy.Difficulty.MEDIUM));
        assertArrayEquals(new int[]{20, 45, 35}, ContractEconomy.tierWeights(ContractEconomy.Difficulty.HARD));
        int[] appearance = {5, 10, 18};
        for (ContractEconomy.Difficulty difficulty : ContractEconomy.Difficulty.values()) {
            int appears = 0;
            for (int roll = 0; roll < 100; roll++) if (ContractEconomy.appears(difficulty, roll)) appears++;
            assertEquals(appearance[difficulty.ordinal()], appears, difficulty + " appearance");
            assertEquals(appearance[difficulty.ordinal()], ContractEconomy.appearancePercent(difficulty), difficulty + " loot chance");
        }
    }
}
