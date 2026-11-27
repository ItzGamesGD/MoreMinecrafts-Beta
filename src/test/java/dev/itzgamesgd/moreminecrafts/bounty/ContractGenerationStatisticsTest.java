package dev.itzgamesgd.moreminecrafts.bounty;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

/** Large seeded sample of the real catalog; writes build/reports/bc03-generation-statistics.txt. */
class ContractGenerationStatisticsTest {
    private static final int CALLS = 200_000;
    private static final int SETS = 100_000;
    private static final List<ContractData.Tier> TIERS = List.of(ContractData.Tier.BEGINNER,
            ContractData.Tier.INTERMEDIATE, ContractData.Tier.ADVANCED);

    @Test void observedWeightsMatchIntentAcrossTiersAndDimensions() throws Exception {
        StringBuilder report = new StringBuilder("Combat target class by tier x dimension (" + CALLS + " draws each)\n");
        report.append("tier,dimension,strongPool,ordinaryPool,intendedStrong,observedStrong,observedOrdinary,rejected\n");
        RandomSource random = RandomSource.create(20261023L);
        for (ContractData.Tier tier : TIERS) {
            for (ContractCatalog.Dimension dim : ContractCatalog.Dimension.values()) {
                Set<String> strong = new TreeSet<>(ContractGenerator.mobPool(tier, dim, ContractCatalog.Strength.STRONG));
                Set<String> ordinary = new TreeSet<>(ContractGenerator.mobPool(tier, dim, ContractCatalog.Strength.ORDINARY));
                int s = 0, o = 0, rejected = 0;
                for (int i = 0; i < CALLS; i++) {
                    Optional<ContractData.Objective> objective = ContractGenerator.combat(tier, dim, random);
                    if (objective.isEmpty()) { rejected++; continue; }
                    String target = objective.get().target();
                    if (strong.contains(target)) s++;
                    else { assertTrue(ordinary.contains(target), target); o++; }
                }
                double intended = ordinary.isEmpty() && strong.isEmpty() ? Double.NaN
                        : strong.isEmpty() ? 0.0 : ContractCatalog.strongPercent(tier) / 100.0;
                int accepted = CALLS - rejected;
                double observed = accepted == 0 ? Double.NaN : s / (double) accepted;
                report.append(String.format("%s,%s,%d,%d,%.4f,%.4f,%.4f,%d%n", tier, dim, strong.size(), ordinary.size(),
                        intended, observed, accepted == 0 ? Double.NaN : o / (double) accepted, rejected));
                if (strong.isEmpty() && ordinary.isEmpty()) assertEquals(CALLS, rejected);
                else {
                    assertEquals(0, rejected);
                    assertEquals(intended, observed, 0.005, tier + "/" + dim);
                }
            }
        }

        report.append("\nObjective sets via tryObjectives (").append(SETS).append(" per tier)\n");
        report.append("tier,rejectedSets,combatObjectives,combatOverworld,combatNether,combatEnd,strongShareOfCombat\n");
        for (ContractData.Tier tier : TIERS) {
            int rejected = 0, combat = 0, strongCount = 0;
            Map<ContractCatalog.Dimension, Integer> dims = new EnumMap<>(ContractCatalog.Dimension.class);
            for (int i = 0; i < SETS; i++) {
                List<ContractData.Objective> set = ContractGenerator.tryObjectives(tier, random);
                if (set == null) { rejected++; continue; }
                for (ContractData.Objective objective : set) {
                    if (objective.category() != ContractData.Category.COMBAT) continue;
                    combat++;
                    for (ContractCatalog.Dimension d : ContractCatalog.Dimension.values())
                        if (ContractGenerator.mobPool(tier, d, ContractCatalog.Strength.STRONG).contains(objective.target())) {
                            strongCount++;
                        }
                    for (ContractCatalog.Dimension d : ContractCatalog.Dimension.values())
                        if (ContractGenerator.mobPool(tier, d, ContractCatalog.Strength.STRONG).contains(objective.target())
                                || ContractGenerator.mobPool(tier, d, ContractCatalog.Strength.ORDINARY).contains(objective.target()))
                            dims.merge(d, 1, Integer::sum);
                }
            }
            report.append(String.format("%s,%d,%d,%d,%d,%d,%.4f%n", tier, rejected, combat,
                    dims.getOrDefault(ContractCatalog.Dimension.OVERWORLD, 0), dims.getOrDefault(ContractCatalog.Dimension.NETHER, 0),
                    dims.getOrDefault(ContractCatalog.Dimension.END, 0), strongCount / (double) combat));
            assertEquals(0, rejected, "no rejected sets for " + tier);
        }
        Path out = Path.of("build/reports/bc03-generation-statistics.txt");
        Files.createDirectories(out.getParent());
        Files.writeString(out, report.toString());
    }
}
