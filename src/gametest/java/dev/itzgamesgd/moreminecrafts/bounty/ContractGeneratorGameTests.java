package dev.itzgamesgd.moreminecrafts.bounty;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;

/** Property checks over seeded samples; fixed seeds make every run deterministic. */
public final class ContractGeneratorGameTests implements CustomTestMethodInvoker {
    private static final int SAMPLES = 3000;
    private static final String ID = "00000000-0000-4000-8000-000000000001";

    private static ContractCatalog.Dimension dimensionOf(String id) {
        for (ContractCatalog.Mob mob : ContractCatalog.VANILLA_MOBS) if (mob.id().equals(id)) return mob.dimension();
        for (ContractCatalog.Mob mob : ContractCatalog.MM_ORDINARY_MOBS) if (mob.id().equals(id)) return mob.dimension();
        for (ContractCatalog.Mob mob : ContractCatalog.MM_STRONG_MOBS) if (mob.id().equals(id)) return mob.dimension();
        throw new IllegalStateException("combat target not in catalog: " + id);
    }

    private static boolean isStrong(String id) {
        return ContractCatalog.MM_STRONG_MOBS.stream().anyMatch(m -> m.id().equals(id))
                || ContractCatalog.VANILLA_MOBS.stream().anyMatch(m -> m.id().equals(id) && m.strength() == ContractCatalog.Strength.STRONG);
    }

    @GameTest
    public void slotCountsFollowTierRules(GameTestHelper ctx) {
        RandomSource random = RandomSource.create(1);
        for (int i = 0; i < SAMPLES; i++) {
            ctx.assertTrue(ContractGenerator.generate(ID, ContractData.Tier.BEGINNER, random).objectives().size() == 1, "beginner slots");
            int intermediate = ContractGenerator.generate(ID, ContractData.Tier.INTERMEDIATE, random).objectives().size();
            ctx.assertTrue(intermediate >= 1 && intermediate <= 2, "intermediate slots " + intermediate);
            int advanced = ContractGenerator.generate(ID, ContractData.Tier.ADVANCED, random).objectives().size();
            ctx.assertTrue(advanced >= 2 && advanced <= 3, "advanced slots " + advanced);
        }
        ctx.succeed();
    }

    @GameTest
    public void beginnerCombatIsOverworldOnly(GameTestHelper ctx) {
        RandomSource random = RandomSource.create(2);
        for (int i = 0; i < SAMPLES; i++)
            for (ContractData.Objective objective : ContractGenerator.generate(ID, ContractData.Tier.BEGINNER, random).objectives())
                if (objective.category() == ContractData.Category.COMBAT)
                    ctx.assertTrue(dimensionOf(objective.target()) == ContractCatalog.Dimension.OVERWORLD,
                            "beginner target outside overworld: " + objective.target());
        ctx.succeed();
    }

    @GameTest
    public void combatTargetsInOneContractShareOneDimension(GameTestHelper ctx) {
        RandomSource random = RandomSource.create(3);
        for (int i = 0; i < SAMPLES; i++) {
            Set<ContractCatalog.Dimension> dimensions = new HashSet<>();
            for (ContractData.Objective objective : ContractGenerator.generate(ID, ContractData.Tier.ADVANCED, random).objectives())
                if (objective.category() == ContractData.Category.COMBAT) dimensions.add(dimensionOf(objective.target()));
            ctx.assertTrue(dimensions.size() <= 1, "mixed-dimension combat bundle: " + dimensions);
        }
        ctx.succeed();
    }

    @GameTest
    public void beginnerOrdinaryShareMatchesProvisionalWeight(GameTestHelper ctx) {
        RandomSource random = RandomSource.create(4);
        int ordinary = 0;
        int total = 0;
        for (int i = 0; i < 20000; i++) {
            ContractData.Objective objective = ContractGenerator.combat(ContractData.Tier.BEGINNER,
                    ContractCatalog.Dimension.OVERWORLD, random);
            total++;
            if (!isStrong(objective.target())) ordinary++;
        }
        double share = ordinary / (double) total;
        ctx.assertTrue(Math.abs(share - 0.90) <= 0.02, "beginner ordinary share " + share);
        ctx.succeed();
    }

    @GameTest
    public void combatCountsStayInsideApprovedBands(GameTestHelper ctx) {
        RandomSource random = RandomSource.create(5);
        for (int i = 0; i < SAMPLES; i++) {
            for (ContractData.Objective objective : ContractGenerator.generate(ID, ContractData.Tier.INTERMEDIATE, random).objectives()) {
                if (objective.category() != ContractData.Category.COMBAT) continue;
                int[] band = isStrong(objective.target()) ? new int[]{5, 10} : new int[]{20, 40};
                ctx.assertTrue(objective.required() >= band[0] && objective.required() <= band[1],
                        "kill count " + objective.required() + " for " + objective.target());
            }
        }
        ctx.succeed();
    }

    @GameTest
    public void highValueDeliveryNeverExceedsOnePerContract(GameTestHelper ctx) {
        RandomSource random = RandomSource.create(6);
        for (int i = 0; i < SAMPLES; i++) {
            int highValue = 0;
            for (ContractData.Objective objective : ContractGenerator.generate(ID, ContractData.Tier.ADVANCED, random).objectives()) {
                if (!ContractCatalog.HIGH_VALUE_DELIVERY.contains(objective.target())) continue;
                highValue++;
                boolean diamond = objective.target().equals("minecraft:diamond");
                ctx.assertTrue(objective.required() >= (diamond ? 2 : 1) && objective.required() <= (diamond ? 4 : 1),
                        "high-value count " + objective.required());
            }
            ctx.assertTrue(highValue <= 1, "two high-value deliveries in one contract");
        }
        ctx.succeed();
    }

    @GameTest
    public void rewardItemsAndCapsFollowApprovedPool(GameTestHelper ctx) {
        RandomSource random = RandomSource.create(7);
        Set<String> pool = new HashSet<>();
        for (ContractEconomy.Rarity rarity : ContractEconomy.Rarity.values()) pool.addAll(ContractRewards.pool(rarity));
        for (int i = 0; i < SAMPLES; i++) {
            ContractData.Reward reward = ContractGenerator.generate(ID, ContractData.Tier.BEGINNER, random).reward();
            ctx.assertTrue(pool.contains(reward.item()), "reward outside approved pool: " + reward.item());
            if (reward.item().equals("minecraft:diamond")) ctx.assertTrue(reward.count() <= 2, "diamond cap");
            if (reward.item().equals("moreminecrafts:titanium_ingot")) ctx.assertTrue(reward.count() <= 4, "titanium cap");
            if (reward.item().equals("moreminecrafts:adamantium_ingot")) ctx.assertTrue(reward.count() == 1, "adamantium cap");
        }
        ctx.succeed();
    }

    @GameTest
    public void emeraldPayoutStaysInsideTierBand(GameTestHelper ctx) {
        RandomSource random = RandomSource.create(8);
        for (int i = 0; i < SAMPLES; i++) {
            int emeralds = ContractGenerator.generate(ID, ContractData.Tier.ADVANCED, random).reward().emeralds();
            ctx.assertTrue(emeralds >= 25 && emeralds <= 50, "payout " + emeralds);
        }
        ctx.succeed();
    }

    @GameTest
    public void advancedAcquisitionConvertsAboutFivePercent(GameTestHelper ctx) {
        RandomSource random = RandomSource.create(9);
        int specials = 0;
        for (int i = 0; i < 100000; i++)
            if (ContractGenerator.convertOnAcquisition(ContractData.Tier.ADVANCED, random) == ContractData.Tier.SPECIAL) specials++;
        double share = specials / 100000.0;
        ctx.assertTrue(Math.abs(share - 0.05) <= 0.005, "special conversion share " + share);
        ctx.succeed();
    }

    @GameTest
    public void onlyAdvancedAcquisitionCanConvert(GameTestHelper ctx) {
        RandomSource random = RandomSource.create(10);
        for (int i = 0; i < 10000; i++) {
            ctx.assertTrue(ContractGenerator.convertOnAcquisition(ContractData.Tier.BEGINNER, random) == ContractData.Tier.BEGINNER, "beginner unchanged");
            ctx.assertTrue(ContractGenerator.convertOnAcquisition(ContractData.Tier.INTERMEDIATE, random) == ContractData.Tier.INTERMEDIATE, "intermediate unchanged");
        }
        ctx.succeed();
    }

    @Override public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
