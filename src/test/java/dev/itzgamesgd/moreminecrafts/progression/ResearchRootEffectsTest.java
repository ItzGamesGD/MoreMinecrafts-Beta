package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

final class ResearchRootEffectsTest {
    private static final String[][] ROOTS = {
            {"resources_root", "ore_mining_xp"},
            {"production_root", "smelting_xp"},
            {"living_root", "crop_growth_probability"},
            {"combat_root", "hostile_damage"},
            {"ecology_root", "hostile_xp_orbs"},
            {"world_root", "ground_movement_speed"},
            {"alchemy_root", "beneficial_potion_duration"}
    };

    @Test void allSevenRootsHaveOneSingleLevelPurchaseGatedPassive() {
        ResearchProgressState unpurchased = ResearchProgressState.empty();
        ResearchProgressState purchased = unpurchased;
        for (String[] root : ROOTS) {
            String id = root[0];
            var catalogEntry = ResearchCatalog.byId(id);
            assertNotNull(catalogEntry, id);
            assertEquals(1, catalogEntry.maximumLevel(), id);
            assertEquals(1, ResearchRewards.effects(id).size(), id);
            assertEquals(1, ResearchRewards.effects(id).getFirst().maximumLevel(), id);
            assertEquals(0.0D, ResearchRewards.value(unpurchased, id, root[1]), id + " is inactive until purchased");
            purchased = purchased.with("research." + id, 1);
            assertEquals(0.01D, ResearchRewards.value(purchased, id, root[1]), id + " activates at level one");
        }
        assertEquals(0.0D, ResearchRewards.value(ResearchProgressState.empty(), "combat_root", "unknown_effect"));
        assertEquals(0.0D, ResearchRewards.value(ResearchProgressState.empty(), "resources_root", "smelting_xp"));
    }

    @Test void xpBonusesAccumulateAcrossRepeatedOneXpEventsWithoutPlayerLeakage() {
        String[] fractionKeys = {"resources_ore", "production_smelt", "ecology_hostile"};
        for (String fractionKey : fractionKeys) {
            int remainder = 0;
            int bonusXp = 0;
            for (int i = 0; i < 100; i++) {
                var result = ResearchPassiveMath.accumulatePercent(1, remainder, 1);
                bonusXp += result.wholeBonus();
                remainder = result.remainder();
            }
            assertEquals(1, bonusXp, fractionKey);
            assertEquals(0, remainder, fractionKey);
        }

        ResearchProgressState firstPlayer = ResearchProgressState.empty().with("research.xp_fraction.resources_ore", 42);
        ResearchProgressState secondPlayer = ResearchProgressState.empty();
        assertNotEquals(firstPlayer.get("research.xp_fraction.resources_ore"),
                secondPlayer.get("research.xp_fraction.resources_ore"), "XP fractions remain player-private");
    }

    @Test void cropGrowthUsesConditionalRollToProduceExactlyOnePercentRelativeIncrease() {
        for (double probability : new double[]{0.01D, 0.125D, 0.5D, 0.99D}) {
            double extra = ResearchPassiveMath.conditionalExtraProbability(probability, 0.01D);
            double overall = probability + (1.0D - probability) * extra;
            assertEquals(Math.min(1.0D, probability * 1.01D), overall, 1.0E-12D);
        }
        assertEquals(0.0D, ResearchPassiveMath.conditionalExtraProbability(1.0D, 0.01D));
    }

    @Test void combatAndWorldBonusesScaleTheVanillaBaseByOnePercent() {
        assertEquals(5.05D, ResearchPassiveMath.scaleByBonus(5.0D,
                ResearchRewards.value(ResearchProgressState.empty().with("research.combat_root", 1),
                        "combat_root", "hostile_damage")), 1.0E-12D);
        assertEquals(0.101D, ResearchPassiveMath.scaleByBonus(0.1D,
                ResearchRewards.value(ResearchProgressState.empty().with("research.world_root", 1),
                        "world_root", "ground_movement_speed")), 1.0E-12D);
    }

    @Test void alchemyDurationIsRoundedOnceAndCappedAtIntegerRange() {
        double bonus = ResearchRewards.value(ResearchProgressState.empty().with("research.alchemy_root", 1),
                "alchemy_root", "beneficial_potion_duration");
        assertEquals(101, ResearchPassiveMath.increasedDuration(100, (int) (bonus * 100)));
        assertEquals(1010, ResearchPassiveMath.increasedDuration(1000, (int) (bonus * 100)));
        assertEquals(Integer.MAX_VALUE, ResearchPassiveMath.increasedDuration(Integer.MAX_VALUE, 1));
    }

    @Test void allFourAuditedLowComplexityEffectsUseTheApprovedPerLevelValues() {
        assertEquals(0.02D, ResearchRewards.effects("v3.research.27").getFirst().valuePerLevel());
        assertEquals(0.10D, ResearchRewards.effects("v3.research.30").getFirst().valuePerLevel());
        assertEquals(0.05D, ResearchRewards.effects("v3.research.31").getFirst().valuePerLevel());
        assertEquals(0.06D, ResearchRewards.effects("v3.research.32").getFirst().valuePerLevel());
        ResearchProgressState purchased = ResearchProgressState.empty()
                .with("research.v3.research.27", 1)
                .with("research.v3.research.30", 1)
                .with("research.v3.research.31", 1)
                .with("research.v3.research.32", 1);
        assertEquals(0.02D, ResearchRewards.value(purchased, "v3.research.27", "swordsmanship_damage"));
        assertEquals(0.10D, ResearchRewards.value(purchased, "v3.research.30", "shield_durability_preservation"));
        assertEquals(0.05D, ResearchRewards.value(purchased, "v3.research.31", "armor_knockback_reduction"));
        assertEquals(0.06D, ResearchRewards.value(purchased, "v3.research.32", "thrown_projectile_speed"));
        assertEquals(0.0D, ResearchRewards.value(ResearchProgressState.empty(), "v3.research.27", "swordsmanship_damage"));
    }

    @Test void fractionalResearchXpRemainderSurvivesProgressStateSerialization() {
        var original = ResearchProgressState.empty()
                .with("research.xp_fraction.resources_ore", 63)
                .with("research.xp_fraction.production_smelt", 17)
                .with("research.xp_fraction.ecology_hostile", 99);
        var encoded = ResearchProgressState.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, original)
                .getOrThrow();
        var restored = ResearchProgressState.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, encoded)
                .getOrThrow();
        assertEquals(original, restored);
    }
}
