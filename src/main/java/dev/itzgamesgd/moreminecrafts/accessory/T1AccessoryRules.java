package dev.itzgamesgd.moreminecrafts.accessory;

import java.util.List;
import java.util.Map;

/** Six-level D01 growth contract; registry IDs survive future display-name changes. */
public final class T1AccessoryRules {
    private T1AccessoryRules() {}

    public static Map<String, List<Double>> table(AccessoryDefinition accessory) {
        return switch (accessory) {
            case T1_COMBO -> Map.of("extra_damage_percent", List.of(35d, 38d, 41d, 44d, 47d, 50d));
            case T1_CRISIS_ESCAPE -> Map.of("radius_blocks", List.of(4d, 4.4d, 4.8d, 5.2d, 5.6d, 6d));
            case T1_HEAVY_BUFFER -> Map.of("damage_reduction_percent", List.of(40d, 44d, 48d, 52d, 56d, 60d));
            case T1_STATUS_RESILIENCE -> Map.of("duration_reduction_percent", List.of(20d, 24d, 28d, 32d, 36d, 40d));
            case T1_SUSTAINED_RECOVERY -> Map.of("heal_interval_ticks", List.of(60d, 56d, 52d, 48d, 44d, 40d));
            case T1_SPRINT_ACCELERATION -> Map.of("speed_bonus_percent", List.of(15d, 19d, 23d, 27d, 31d, 35d));
            case T1_DOUBLE_JUMP -> Map.of("cooldown_ticks", List.of(600d, 540d, 480d, 420d, 360d, 300d));
            default -> Map.of();
        };
    }
}
