package dev.itzgamesgd.moreminecrafts.enhancement;

import java.util.List;

/** Immutable continuous-growth and deterministic-milestone metadata used by runtime adapters. */
public enum EnhancementProfile {
    SWORD(effects(EnhancementEffect.Target.ATTACK_DAMAGE, GrowthCurve.SWORD_DAMAGE), List.of()),
    MACE(effects(EnhancementEffect.Target.MACE_SMASH_DAMAGE, GrowthCurve.NONE), milestones(
            MilestoneEffect.Kind.MACE_SMASH_SCALING_SEAM)),
    RANGED(effects(EnhancementEffect.Target.RANGED_DAMAGE, GrowthCurve.RANGED_DAMAGE), List.of(
            milestone(MilestoneEffect.Kind.BOW_DRAW_SPEED_CANDIDATE),
            milestone(MilestoneEffect.Kind.CROSSBOW_RELOAD_SPEED_CANDIDATE))),
    SPEAR(effects(EnhancementEffect.Target.ATTACK_DAMAGE, GrowthCurve.SPEAR_DAMAGE), List.of()),
    ARMOR(effects(EnhancementEffect.Target.DAMAGE_REDUCTION, GrowthCurve.ARMOR_REDUCTION), milestones(
            MilestoneEffect.Kind.ARMOR_UTILITY_CANDIDATE)),
    TOOL(effects(EnhancementEffect.Target.TOOL_EFFICIENCY, GrowthCurve.TOOL_SPEED), List.of(
            milestone(MilestoneEffect.Kind.TOOL_DURABILITY_CANDIDATE),
            milestone(MilestoneEffect.Kind.TOOL_UTILITY_CANDIDATE))),
    AXE(List.of(
            new EnhancementEffect(EnhancementEffect.Target.TOOL_EFFICIENCY, GrowthCurve.TOOL_SPEED),
            new EnhancementEffect(EnhancementEffect.Target.ATTACK_DAMAGE, GrowthCurve.AXE_DAMAGE)), List.of(
            milestone(MilestoneEffect.Kind.TOOL_DURABILITY_CANDIDATE),
            milestone(MilestoneEffect.Kind.TOOL_UTILITY_CANDIDATE))),
    ELYTRA(effects(EnhancementEffect.Target.NONE, GrowthCurve.NONE), List.of());

    private final List<EnhancementEffect> effects;
    private final List<MilestoneEffect> milestoneEffects;

    EnhancementProfile(List<EnhancementEffect> effects, List<MilestoneEffect> milestoneEffects) {
        this.effects = List.copyOf(effects);
        this.milestoneEffects = List.copyOf(milestoneEffects);
    }

    public List<EnhancementEffect> effects() { return effects; }
    public List<MilestoneEffect> milestoneEffects() { return milestoneEffects; }

    public double valueAt(int level, EnhancementEffect.Target target) {
        return effects.stream().filter(effect -> effect.target() == target)
                .mapToDouble(effect -> effect.curve().valueAt(level)).findFirst().orElse(0.0D);
    }

    private static List<EnhancementEffect> effects(EnhancementEffect.Target target, GrowthCurve curve) {
        return List.of(new EnhancementEffect(target, curve));
    }
    private static List<MilestoneEffect> milestones(MilestoneEffect.Kind kind) { return List.of(milestone(kind)); }
    private static MilestoneEffect milestone(MilestoneEffect.Kind kind) { return new MilestoneEffect(kind, 5, 5); }
}
