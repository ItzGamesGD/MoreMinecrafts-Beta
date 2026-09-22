package dev.itzgamesgd.moreminecrafts.enhancement;

import java.util.List;

/** Immutable stat metadata consumed by the enhancement runtime adapters. */
public enum EnhancementProfile {
    SWORD(new EnhancementEffect(EnhancementEffect.Target.ATTACK_DAMAGE, 0.08D)),
    MELEE(new EnhancementEffect(EnhancementEffect.Target.ATTACK_DAMAGE, 0.10D)),
    RANGED(new EnhancementEffect(EnhancementEffect.Target.RANGED_DAMAGE, 0.08D)),
    SPEAR(new EnhancementEffect(EnhancementEffect.Target.ATTACK_DAMAGE, 0.09D)),
    ARMOR(new EnhancementEffect(EnhancementEffect.Target.DAMAGE_REDUCTION, 0.001D)),
    TOOL(new EnhancementEffect(EnhancementEffect.Target.TOOL_EFFICIENCY, 0.65D)),
    AXE(
            new EnhancementEffect(EnhancementEffect.Target.TOOL_EFFICIENCY, 0.65D),
            new EnhancementEffect(EnhancementEffect.Target.ATTACK_DAMAGE, 0.10D)
    ),
    ELYTRA(new EnhancementEffect(EnhancementEffect.Target.NONE, 0.0D));

    private final List<EnhancementEffect> effects;

    EnhancementProfile(EnhancementEffect... effects) {
        this.effects = List.of(effects);
    }

    public List<EnhancementEffect> effects() {
        return effects;
    }

    public double valuePerLevel(EnhancementEffect.Target target) {
        return effects.stream()
                .filter(effect -> effect.target() == target)
                .mapToDouble(EnhancementEffect::additiveValuePerLevel)
                .findFirst()
                .orElse(0.0D);
    }
}
