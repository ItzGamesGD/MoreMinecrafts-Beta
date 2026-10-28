package dev.itzgamesgd.moreminecrafts.accessory;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AccessoryEnhancementCatalogTest {
    @Test void everyAccessoryHasAnIndependentLiteralElevenLevelTable() {
        assertEquals(12, AccessoryDefinition.ALL.size());
        assertEquals(List.of(0,1,2,3,4,5,6,7,8,9,10), AccessoryEnhancementCatalog.levels());
        for (AccessoryDefinition accessory : AccessoryDefinition.ALL) {
            assertFalse(AccessoryEnhancementCatalog.table(accessory).isEmpty(), accessory.name());
            for (var series : AccessoryEnhancementCatalog.table(accessory).entrySet()) {
                assertEquals(11, series.getValue().size(), accessory + ":" + series.getKey());
            }
        }
        assertEquals(3, AccessoryEnhancementCatalog.table(AccessoryDefinition.PHANTOM_MIRROR).size());
    }

    @Test void combatTablesMatchTheApprovedPerLevelValues() {
        table(AccessoryDefinition.PHANTOM_MIRROR, Map.of(
                "clone_hp_percent", v(50,60,60,60,70,70,70,80,80,90,90),
                "melee_damage_percent", v(40,40,45,45,50,50,55,55,60,60,65),
                "duration_seconds", v(12,12,12,13,13,14,14,15,15,15,16)));
        table(AccessoryDefinition.RING_OF_TIME, Map.of(
                "radius_blocks", v(5,5.5,5.5,6,6,6.5,6.5,7,7,7.5,8),
                "duration_seconds", v(3,3,3.5,3.5,4,4,4.5,4.5,5,5,5),
                "target_time_progression_percent", v(20,20,20,20,20,20,20,20,20,20,20)));
        table(AccessoryDefinition.BLOOD_COVENANT, Map.of(
                "damage_bonus_percent", v(35,37.5,37.5,40,40,42.5,42.5,45,47.5,47.5,50),
                "duration_seconds", v(12,12,13,13,14,14,15,15,15,16,16)));
        table(AccessoryDefinition.HEART_OF_GRAVITY, Map.of(
                "pull_radius_blocks", v(5,5,5.5,5.5,6,6,6,6.5,7,7.5,8),
                "duration_seconds", v(4,4,4,4.5,4.5,4.5,5,5,5.5,5.5,6),
                "target_cap", v(12,14,14,14,14,16,16,16,16,18,18)));
        table(AccessoryDefinition.STAR_COVENANT, Map.of(
                "melee_damage_bonus_percent", v(8,9,10,10,11,12,13,13,14,15,16),
                "hp_activation_threshold_percent", v(50,50,50,55,55,55,55,60,60,60,60)));
        table(AccessoryDefinition.SOUL_CHAIN, Map.of(
                "max_linked_mobs_including_primary", v(3,4,4,4,5,5,6,6,7,7,8),
                "direct_hit_propagation_percent_per_other_target", v(25,25,28,28,28,32,32,32,32,36,40),
                "link_reach_blocks", v(8,8,8,10,10,10,10,12,12,12,12)));
    }

    @Test void defenseAndSupportTablesMatchTheApprovedPerLevelValues() {
        table(AccessoryDefinition.EMBER_RING, Map.of(
                "post_fire_hit_damage_reduction_percent", v(40,45,45,50,50,55,55,60,60,65,65),
                "protection_duration_seconds", v(3,3,3.4,3.4,3.8,3.8,4.2,4.2,4.6,4.6,5)));
        table(AccessoryDefinition.RING_OF_LIFE, Map.of(
                "regeneration_i_duration_seconds", v(4,4.5,4.5,5,5,5.5,5.5,6,6,6.5,7),
                "cooldown_seconds", v(120,120,115,115,110,110,105,105,100,100,100)));
        table(AccessoryDefinition.SEED_OF_LIFE, Map.of(
                "heal_hp_per_2_seconds", v(3,3,3,4,4,4,4,4,5,5,5),
                "radius_blocks", v(5,5.5,5.5,5.5,6,6,6.5,6.5,6.5,7,7),
                "duration_seconds", v(12,12,13,13,13,14,14,15,15,15,16)));
    }

    @Test void explorationAndLifestyleTablesMatchTheApprovedPerLevelValues() {
        table(AccessoryDefinition.SNOW_CHARM, Map.of(
                "powder_snow_sinking_speed_reduction_percent", v(70,75,75,80,80,85,85,90,90,95,95),
                "additional_slipping_reduction_percent", v(0,0,4,4,8,8,12,12,16,16,20)));
        table(AccessoryDefinition.DEEP_SEA_PEARL, Map.of(
                "water_breathing_seconds", v(8,9,9,11,11,13,13,14,14,16,16),
                "cooldown_seconds", v(45,45,42,42,39,39,36,36,33,33,30)));
        table(AccessoryDefinition.WHISPER_OF_THE_SEA, Map.of(
                "fishing_success_zone_expansion_percent", v(10,12,12,14,16,16,18,20,22,24,25),
                "additional_input_timing_tolerance_seconds", v(0,0,0.05,0.05,0.05,0.10,0.10,0.10,0.10,0.10,0.10)));
    }

    @Test void lookupsFailClosedForUnknownStatAndOutOfRangeLevel() {
        assertEquals(50, AccessoryEnhancementCatalog.valueAt(AccessoryDefinition.PHANTOM_MIRROR,
                "clone_hp_percent", 0).orElseThrow());
        assertEquals(90, AccessoryEnhancementCatalog.valueAt(AccessoryDefinition.PHANTOM_MIRROR,
                "clone_hp_percent", 10).orElseThrow());
        assertTrue(AccessoryEnhancementCatalog.valueAt(AccessoryDefinition.PHANTOM_MIRROR,
                "unknown", 5).isEmpty());
        assertTrue(AccessoryEnhancementCatalog.valueAt(AccessoryDefinition.PHANTOM_MIRROR,
                "clone_hp_percent", 11).isEmpty());
    }

    private static void table(AccessoryDefinition accessory, Map<String, List<Double>> expected) {
        assertEquals(expected, AccessoryEnhancementCatalog.table(accessory), accessory.name());
    }

    private static List<Double> v(double... values) {
        return java.util.Arrays.stream(values).boxed().toList();
    }
}
