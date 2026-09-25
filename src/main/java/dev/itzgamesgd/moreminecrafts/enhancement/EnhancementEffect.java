package dev.itzgamesgd.moreminecrafts.enhancement;

/** A continuous runtime effect and its centrally-tunable progression curve. */
public record EnhancementEffect(Target target, GrowthCurve curve) {
    public enum Target {
        ATTACK_DAMAGE,
        RANGED_DAMAGE,
        DAMAGE_REDUCTION,
        TOOL_EFFICIENCY,
        MACE_SMASH_DAMAGE,
        NONE
    }
}
