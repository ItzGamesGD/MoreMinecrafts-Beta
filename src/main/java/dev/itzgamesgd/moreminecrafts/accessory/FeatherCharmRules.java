package dev.itzgamesgd.moreminecrafts.accessory;

import java.util.List;

/**
 * Feather Charm (깃털 부적) numbers, all in one place so the owner can retune them.
 *
 * <p>Owner-decided structure: always-on passive, cooldown 0, fall damage reduction plus one air jump per airtime that
 * is restored on landing (docs/design/SKY_ISLANDS_T2_CONTEXT_2026-10-27.md, Feather Charm entry). The per-level numbers
 * are NOT in any document: every value below is PROVISIONAL and awaits an owner decision.
 */
public final class FeatherCharmRules {
    private FeatherCharmRules() {}

    public static final String FALL_REDUCTION_STAT = "fall_damage_reduction_percent";
    public static final String COOLDOWN_STAT = "cooldown_ticks";

    /**
     * PROVISIONAL, levels +0..+10. Copies Ember Ring's "post_fire_hit_damage_reduction_percent" series, the closest
     * existing T2 damage-reduction percent table (+5 every second level).
     */
    public static final List<Double> FALL_DAMAGE_REDUCTION_PERCENT =
            List.of(40d, 45d, 45d, 50d, 50d, 55d, 55d, 60d, 60d, 65d, 65d);

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
