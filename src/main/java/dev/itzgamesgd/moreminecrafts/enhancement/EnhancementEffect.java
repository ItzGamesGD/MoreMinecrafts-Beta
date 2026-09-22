package dev.itzgamesgd.moreminecrafts.enhancement;

public record EnhancementEffect(Target target, double additiveValuePerLevel) {
    public enum Target {
        ATTACK_DAMAGE,
        RANGED_DAMAGE,
        DAMAGE_REDUCTION,
        TOOL_EFFICIENCY,
        NONE
    }
}
