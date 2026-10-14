package dev.itzgamesgd.moreminecrafts.progression;

import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;

/**
 * Stateless reward lookup. Consumers query the purchased level when applying an effect, so
 * reconnects never re-apply or stack an effect.
 */
public final class ResearchRewards {
    public enum Kind { PASSIVE_PERCENT, NUMERICAL_LEVEL, OPERATION_IMPROVEMENT, RECIPE_UNLOCK, DEVICE_ELIGIBILITY }
    public record Effect(String id, Kind kind, double valuePerLevel, int maximumLevel) {}
    private static final Map<String, List<Effect>> EFFECTS = Map.ofEntries(
            Map.entry("combat_root", List.of(new Effect("hostile_damage", Kind.PASSIVE_PERCENT, 0.01, 1))),
            Map.entry("ecology_root", List.of(new Effect("hostile_xp_orbs", Kind.PASSIVE_PERCENT, 0.01, 1))),
            Map.entry("climbing", List.of(new Effect("wall_climb", Kind.OPERATION_IMPROVEMENT, 1.0, 1))),
            Map.entry("wall_jump", List.of(new Effect("wall_jump", Kind.OPERATION_IMPROVEMENT, 1.0, 1))),
            Map.entry("fall_roll", List.of(new Effect("fall_roll", Kind.OPERATION_IMPROVEMENT, 1.0, 1))),
            Map.entry("nearby_chest_ingredients", List.of(new Effect("chest_ingredient_reference", Kind.OPERATION_IMPROVEMENT, 1.0, 1))),
            Map.entry("hopper_filter", List.of(new Effect("hopper_filtering", Kind.OPERATION_IMPROVEMENT, 1.0, 1))),
            Map.entry("automatic_tool_switch", List.of(new Effect("automatic_tool_switch", Kind.OPERATION_IMPROVEMENT, 1.0, 1))),
            Map.entry("shield_counter", List.of(new Effect("shield_counter", Kind.OPERATION_IMPROVEMENT, 1.0, 1))),
            Map.entry("ballistic_control", List.of(new Effect("ballistic_impact_marker", Kind.OPERATION_IMPROVEMENT, 1.0, 1))),
            Map.entry("bow_charge_mobility", List.of(new Effect("bow_charge_mobility", Kind.OPERATION_IMPROVEMENT, 1.0, 1))),
            Map.entry("falling_axe_smash", List.of(new Effect("falling_axe_smash", Kind.OPERATION_IMPROVEMENT, 1.0, 1))),
            Map.entry("weapon_swap_handling", List.of(new Effect("weapon_swap_handling", Kind.OPERATION_IMPROVEMENT, 1.0, 1))),
            Map.entry("mob_aggro_redirection", List.of(new Effect("mob_aggro_redirection", Kind.OPERATION_IMPROVEMENT, 1.0, 1))),
            Map.entry("mob_capture", List.of(new Effect("mob_capture_eligibility", Kind.DEVICE_ELIGIBILITY, 1.0, 1))),
            Map.entry("piglin_barter", List.of(new Effect("piglin_barter_improvement", Kind.OPERATION_IMPROVEMENT, 1.0, 1))),
            Map.entry("throwable_pickaxe", List.of(new Effect("throwable_pickaxe", Kind.OPERATION_IMPROVEMENT, 1.0, 1)))
    );

    private ResearchRewards() {}
    public static List<Effect> effects(String researchId) { return EFFECTS.getOrDefault(researchId, List.of()); }
    public static int level(ServerPlayer player, String researchId, String effectId) {
        int level = KnowledgeRuntime.state(player).researchLevel(researchId);
        return effects(researchId).stream().filter(effect -> effect.id().equals(effectId))
                .mapToInt(effect -> Math.min(level, effect.maximumLevel())).findFirst().orElse(0);
    }
    public static double value(ServerPlayer player, String researchId, String effectId) {
        int level = level(player, researchId, effectId);
        return effects(researchId).stream().filter(effect -> effect.id().equals(effectId))
                .mapToDouble(effect -> effect.valuePerLevel() * level).findFirst().orElse(0.0);
    }
}
