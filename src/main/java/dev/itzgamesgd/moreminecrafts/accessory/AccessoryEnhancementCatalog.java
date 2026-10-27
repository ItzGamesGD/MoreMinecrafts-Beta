package dev.itzgamesgd.moreminecrafts.accessory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

/** Literal, accessory-specific growth tables from the approved 2026-10-17 authority. */
public final class AccessoryEnhancementCatalog {
    public static final int MIN_LEVEL = 0;
    public static final int MAX_LEVEL = 10;
    private static final List<Integer> LEVELS = List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    private static final Map<AccessoryDefinition, Map<String, List<Double>>> TABLES = Map.ofEntries(
            Map.entry(AccessoryDefinition.PHANTOM_MIRROR, table(
                    s("clone_hp_percent", 50,60,60,60,70,70,70,80,80,90,90),
                    s("melee_damage_percent", 40,40,45,45,50,50,55,55,60,60,65),
                    s("duration_seconds", 12,12,12,13,13,14,14,15,15,15,16))),
            Map.entry(AccessoryDefinition.RING_OF_TIME, table(
                    s("radius_blocks", 5,5.5,5.5,6,6,6.5,6.5,7,7,7.5,8),
                    s("duration_seconds", 3,3,3.5,3.5,4,4,4.5,4.5,5,5,5),
                    s("target_time_progression_percent", 20,20,20,20,20,20,20,20,20,20,20))),
            Map.entry(AccessoryDefinition.BLOOD_COVENANT, table(
                    s("damage_bonus_percent", 35,37.5,37.5,40,40,42.5,42.5,45,47.5,47.5,50),
                    s("duration_seconds", 12,12,13,13,14,14,15,15,15,16,16))),
            Map.entry(AccessoryDefinition.HEART_OF_GRAVITY, table(
                    s("pull_radius_blocks", 5,5,5.5,5.5,6,6,6,6.5,7,7.5,8),
                    s("duration_seconds", 4,4,4,4.5,4.5,4.5,5,5,5.5,5.5,6),
                    s("target_cap", 12,14,14,14,14,16,16,16,16,18,18))),
            Map.entry(AccessoryDefinition.STAR_COVENANT, table(
                    s("melee_damage_bonus_percent", 8,9,10,10,11,12,13,13,14,15,16),
                    s("hp_activation_threshold_percent", 50,50,50,55,55,55,55,60,60,60,60))),
            Map.entry(AccessoryDefinition.SOUL_CHAIN, table(
                    s("max_linked_mobs_including_primary", 3,4,4,4,5,5,6,6,7,7,8),
                    s("direct_hit_propagation_percent_per_other_target", 25,25,28,28,28,32,32,32,32,36,40),
                    s("link_reach_blocks", 8,8,8,10,10,10,10,12,12,12,12))),
            Map.entry(AccessoryDefinition.EMBER_RING, table(
                    s("post_fire_hit_damage_reduction_percent", 40,45,45,50,50,55,55,60,60,65,65),
                    s("protection_duration_seconds", 3,3,3.4,3.4,3.8,3.8,4.2,4.2,4.6,4.6,5))),
            Map.entry(AccessoryDefinition.RING_OF_LIFE, table(
                    s("regeneration_i_duration_seconds", 4,4.5,4.5,5,5,5.5,5.5,6,6,6.5,7),
                    s("cooldown_seconds", 120,120,115,115,110,110,105,105,100,100,100))),
            Map.entry(AccessoryDefinition.SEED_OF_LIFE, table(
                    s("heal_hp_per_2_seconds", 3,3,3,4,4,4,4,4,5,5,5),
                    s("radius_blocks", 5,5.5,5.5,5.5,6,6,6.5,6.5,6.5,7,7),
                    s("duration_seconds", 12,12,13,13,13,14,14,15,15,15,16))),
            Map.entry(AccessoryDefinition.SNOW_CHARM, table(
                    s("powder_snow_sinking_speed_reduction_percent", 70,75,75,80,80,85,85,90,90,95,95),
                    s("additional_slipping_reduction_percent", 0,0,4,4,8,8,12,12,16,16,20))),
            Map.entry(AccessoryDefinition.DEEP_SEA_PEARL, table(
                    s("water_breathing_seconds", 8,9,9,11,11,13,13,14,14,16,16),
                    s("cooldown_seconds", 45,45,42,42,39,39,36,36,33,33,30))),
            Map.entry(AccessoryDefinition.WHISPER_OF_THE_SEA, table(
                    s("fishing_success_zone_expansion_percent", 10,12,12,14,16,16,18,20,22,24,25),
                    s("additional_input_timing_tolerance_seconds", 0,0,0.05,0.05,0.05,0.10,0.10,0.10,0.10,0.10,0.10))));

    private AccessoryEnhancementCatalog() {}

    public static List<Integer> levels() { return LEVELS; }

    public static Map<String, List<Double>> table(AccessoryDefinition accessory) {
        return TABLES.getOrDefault(accessory, Map.of());
    }

    public static OptionalDouble valueAt(AccessoryDefinition accessory, String stat, int level) {
        if (level < MIN_LEVEL || level > MAX_LEVEL) return OptionalDouble.empty();
        List<Double> values = table(accessory).get(stat);
        return values == null ? OptionalDouble.empty() : OptionalDouble.of(values.get(level));
    }

    private static Map<String, List<Double>> table(Series... series) {
        Map<String, List<Double>> result = new LinkedHashMap<>();
        for (Series entry : series) {
            if (entry.values().size() != LEVELS.size()) {
                throw new IllegalArgumentException("Every accessory growth series must define levels +0 through +10");
            }
            if (result.put(entry.stat(), entry.values()) != null) {
                throw new IllegalArgumentException("Duplicate accessory growth stat: " + entry.stat());
            }
        }
        return Map.copyOf(result);
    }

    private static Series s(String stat, double... values) {
        return new Series(stat, java.util.Arrays.stream(values).boxed().toList());
    }

    private record Series(String stat, List<Double> values) {}
}
