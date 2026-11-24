package dev.itzgamesgd.moreminecrafts.progression;

import com.mojang.serialization.JsonOps;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResearchStage3RulesTest {
    @Test
    void cropCultivationMatchesExactLevelDistribution() {
        assertEquals(0, ResearchStage3HarvestRules.cropCultivationExtra(0, 0.0D));
        assertEquals(1, ResearchStage3HarvestRules.cropCultivationExtra(1, 0.99D));
        assertEquals(2, ResearchStage3HarvestRules.cropCultivationExtra(2, 0.39D));
        assertEquals(1, ResearchStage3HarvestRules.cropCultivationExtra(2, 0.40D));
        assertEquals(2, ResearchStage3HarvestRules.cropCultivationExtra(3, 0.99D));
        assertEquals(3, ResearchStage3HarvestRules.cropCultivationExtra(4, 0.29D));
        assertEquals(2, ResearchStage3HarvestRules.cropCultivationExtra(4, 0.30D));
        assertEquals(3, ResearchStage3HarvestRules.cropCultivationExtra(5, 0.49D));
        assertEquals(2, ResearchStage3HarvestRules.cropCultivationExtra(5, 0.50D));
    }

    @Test
    void magicalMaterialAndCatalystPreservationChances() {
        assertEquals(0.0D, ResearchStage3HarvestRules.magicalMaterialChance(0));
        assertEquals(0.30D, ResearchStage3HarvestRules.magicalMaterialChance(3), 1.0E-9);
        assertTrue(ResearchStage3HarvestRules.magicalMaterialExtraSucceeds(1, 0.09D));
        assertFalse(ResearchStage3HarvestRules.magicalMaterialExtraSucceeds(1, 0.10D));

        assertEquals(0.0D, ResearchStage3HarvestRules.catalystPreservationChance(0));
        assertEquals(0.15D, ResearchStage3HarvestRules.catalystPreservationChance(3), 1.0E-9);
        assertTrue(ResearchStage3HarvestRules.catalystPreserved(2, 0.099D));
        assertFalse(ResearchStage3HarvestRules.catalystPreserved(2, 0.10D));
    }

    @Test
    void ecologyAiRulesMatchApprovedNumbers() {
        assertEquals(0.25D, ResearchEcologyAiRules.zombieReinforcementReduction(1), 1.0E-9);
        assertEquals(0.75D, ResearchEcologyAiRules.zombieReinforcementReduction(3), 1.0E-9);
        assertTrue(ResearchEcologyAiRules.skeletonAimResetSucceeds(1, 0.19D));
        assertFalse(ResearchEcologyAiRules.skeletonAimResetSucceeds(1, 0.20D));
        assertTrue(ResearchEcologyAiRules.skeletonAimResetSucceeds(3, 0.39D));
        assertEquals(-0.12D, ResearchEcologyAiRules.illagerSpeedModifier(1), 1.0E-9);
        assertEquals(0.0D, ResearchEcologyAiRules.illagerSpeedModifier(0), 1.0E-9);
        assertTrue(ResearchEcologyAiRules.piglinAngerPrevented(1, 0.29D));
        assertFalse(ResearchEcologyAiRules.piglinAngerPrevented(1, 0.30D));
        assertTrue(ResearchEcologyAiRules.piglinAngerPrevented(3, 0.59D));
    }

    @Test
    void combatV4RulesValidateCooldownsAndToolSelection() {
        assertTrue(ResearchCombatV4Rules.isShieldCounterWindowActive(130L, 130L));
        assertFalse(ResearchCombatV4Rules.isShieldCounterWindowActive(131L, 130L));
        assertTrue(ResearchCombatV4Rules.isFallingAxeEligible(true, true, false, 1.6F, 0));
        assertFalse(ResearchCombatV4Rules.isFallingAxeEligible(true, true, false, 1.5F, 0));
        assertFalse(ResearchCombatV4Rules.isFallingAxeEligible(true, true, false, 3.0F, 1));
        assertEquals(10, ResearchCombatV4Rules.weaponSwapPrefillTicker(0, 20.0F));
        assertEquals(15, ResearchCombatV4Rules.weaponSwapPrefillTicker(15, 20.0F));

        assertEquals(3, ResearchCombatV4Rules.selectBestHotbarTool(1.0F,
                new float[]{1.0F, 4.0F, 1.0F, 8.0F, 6.0F, 1.0F, 1.0F, 1.0F, 1.0F}, 0));
        assertEquals(-1, ResearchCombatV4Rules.selectBestHotbarTool(6.0F,
                new float[]{6.0F, 8.0F}, 0));
    }

    @Test
    void worldLootRulesMatchArchaeologyAndPiglinBarterSpecs() {
        assertEquals(30, ResearchWorldLootRules.archaeologyBrushReductionPercent(3));
        assertEquals(0.09D, ResearchWorldLootRules.archaeologyRareBonusChance(3), 1.0E-9);
        assertEquals(0.30D, ResearchWorldLootRules.archaeologyThematicExtraChance(3), 1.0E-9);
        assertTrue(ResearchWorldLootRules.isArchaeologyStructureChest("chests/desert_pyramid"));
        assertFalse(ResearchWorldLootRules.isArchaeologyStructureChest("chests/village/village_plains_house"));
        assertEquals(0.15D, ResearchWorldLootRules.piglinBarterRareChance(3), 1.0E-9);
    }

    @Test
    void hopperFilterStateEnforcesWhitelistBlacklistAndCodec() {
        var emptyWhite = HopperFilterState.emptyWhitelist();
        assertFalse(emptyWhite.allows("minecraft:iron_ingot", 0));

        var emptyBlack = new HopperFilterState(false, List.of());
        assertTrue(emptyBlack.allows("minecraft:iron_ingot", 0));

        var filtered = new HopperFilterState(true, List.of(
                new HopperFilterState.Entry("minecraft:diamond", false, 0),
                new HopperFilterState.Entry("minecraft:iron_sword", true, 42)));
        assertTrue(filtered.allows("minecraft:diamond", 999));
        assertTrue(filtered.allows("minecraft:iron_sword", 42));
        assertFalse(filtered.allows("minecraft:iron_sword", 99));

        var encoded = HopperFilterState.CODEC.encodeStart(JsonOps.INSTANCE, filtered).getOrThrow();
        var decoded = HopperFilterState.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        assertEquals(filtered, decoded);
    }

    @Test
    void potionMixingAndConcentrationObeyStage22Section6() {
        var speed = new PotionMixingRules.EffectSpec("minecraft:speed", 3600, 0, false, 1);
        var strength = new PotionMixingRules.EffectSpec("minecraft:strength", 3600, 0, false, 1);

        // Delivery mismatch rejected
        assertEquals(PotionMixingRules.OperationKind.REJECTED,
                PotionMixingRules.resolve("drinkable", List.of(speed), false,
                        "splash", List.of(strength), false).kind());

        // Mix two distinct effects -> 70% duration each
        var mixed = PotionMixingRules.resolve("drinkable", List.of(speed), false,
                "drinkable", List.of(strength), false);
        assertEquals(PotionMixingRules.OperationKind.MIX, mixed.kind());
        assertEquals(2, mixed.effects().size());
        assertEquals(2520, mixed.effects().get(0).durationTicks());
        assertEquals(2520, mixed.effects().get(1).durationTicks());

        // Concentrate identical effects -> +1 amplifier, min(d1,d2)*0.70, reject second concentration
        var speedShort = new PotionMixingRules.EffectSpec("minecraft:speed", 1800, 0, false, 1);
        var conc = PotionMixingRules.resolve("drinkable", List.of(speed), false,
                "drinkable", List.of(speedShort), false);
        assertEquals(PotionMixingRules.OperationKind.CONCENTRATE, conc.kind());
        assertEquals(1, conc.effects().getFirst().amplifier());
        assertEquals(1260, conc.effects().getFirst().durationTicks());

        assertEquals(PotionMixingRules.OperationKind.REJECTED,
                PotionMixingRules.resolve("drinkable", conc.effects(), true,
                        "drinkable", conc.effects(), false).kind());

        // Reject > 4 combined effects
        var e1 = new PotionMixingRules.EffectSpec("minecraft:a", 1000, 0, false, 1);
        var e2 = new PotionMixingRules.EffectSpec("minecraft:b", 1000, 0, false, 1);
        var e3 = new PotionMixingRules.EffectSpec("minecraft:c", 1000, 0, false, 1);
        var e4 = new PotionMixingRules.EffectSpec("minecraft:d", 1000, 0, false, 1);
        var e5 = new PotionMixingRules.EffectSpec("minecraft:e", 1000, 0, false, 1);
        assertEquals(PotionMixingRules.OperationKind.REJECTED,
                PotionMixingRules.resolve("drinkable", List.of(e1, e2, e3), false,
                        "drinkable", List.of(e4, e5), false).kind());
    }
}
