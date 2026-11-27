package dev.itzgamesgd.moreminecrafts.accessory;

import java.util.List;

/**
 * Feather Charm (깃털 부적) numbers, all in one place so the owner can retune them.
 *
 * <p>Owner-decided structure: always-on passive, cooldown 0, fall damage reduction plus one air jump per airtime that
 * is restored on landing (docs/design/SKY_ISLANDS_T2_CONTEXT_2026-10-27.md, Feather Charm entry). The per-level numbers
 * values below are owner-decided (2026-11-17) and recorded in docs/design/SKY_ISLANDS_T2_CONTEXT_2026-10-27.md section 23.
 */
public final class FeatherCharmRules {
    private FeatherCharmRules() {}

    public static final String FALL_REDUCTION_STAT = "fall_damage_reduction_percent";
    public static final String COOLDOWN_STAT = "cooldown_ticks";

    /**
     * Owner-decided (2026-11-17): 40% + 2% per level, levels +0..+10 (40 .. 60). Applies to vanilla fall damage and,
     * with the 10 second cooldown, to ender pearl / stalagmite / falling stalactite damage.
     */
    public static final List<Double> FALL_DAMAGE_REDUCTION_PERCENT =
            List.of(40d, 42d, 44d, 46d, 48d, 50d, 52d, 54d, 56d, 58d, 60d);

    /**
     * Owner-decided (2026-11-17): ender pearl and stalactite/stalagmite damage use the same percent table, but the
     * reduction for those sources is limited by a 10 second cooldown (200 ticks). Vanilla fall damage has no cooldown.
     */
    public static final long NON_FALL_COOLDOWN_TICKS = 200L;

    /** Owner-decided: no cooldown at any level for vanilla fall damage. */
    public static final List<Double> COOLDOWN_TICKS =
            List.of(0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d);

    /** Same upward velocity as the T1 double jump (reuses its structure; not a new value). */
    public static final double AIR_JUMP_VELOCITY = T1AccessoryRuntime.JUMP_VELOCITY;

    /** One extra jump per airtime; landing restores it (the shared T1_AIR_JUMP_USED reset on ground). */
    public static final int AIR_JUMPS_PER_AIRTIME = 1;
}
