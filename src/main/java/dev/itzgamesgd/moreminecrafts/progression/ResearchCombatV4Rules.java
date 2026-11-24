package dev.itzgamesgd.moreminecrafts.progression;

/**
 * Pure deterministic rules for V4 combat & tool research:
 * - `shield_counter`: 30-tick window, +20% melee damage + knockback shockwave, 80-tick cooldown.
 * - `falling_axe_smash`: airborne & fallDistance > 1.5F with axe -> +25% damage + shield disable + shockwave, 60-tick persisted cooldown.
 * - `weapon_swap_handling`: 50% attack cooldown prefill on melee swap, 40-tick internal cooldown.
 * - `automatic_tool_switch`: best hotbar tool selection on mining start, 10-tick cooldown.
 */
public final class ResearchCombatV4Rules {
    public static final int SHIELD_COUNTER_WINDOW_TICKS = 30;
    public static final int SHIELD_COUNTER_COOLDOWN_TICKS = 80;
    public static final double SHIELD_COUNTER_DAMAGE_BONUS = 0.20D;
    public static final double SHIELD_COUNTER_SHOCKWAVE_RADIUS = 3.5D;
    public static final double SHIELD_COUNTER_KNOCKBACK = 0.75D;

    public static final float FALLING_AXE_MIN_FALL = 1.5F;
    public static final int FALLING_AXE_COOLDOWN_TICKS = 60;
    public static final double FALLING_AXE_DAMAGE_BONUS = 0.25D;
    public static final double FALLING_AXE_SHOCKWAVE_RADIUS = 2.5D;
    public static final int FALLING_AXE_SHIELD_DISABLE_TICKS = 100;

    public static final int WEAPON_SWAP_COOLDOWN_TICKS = 40;
    public static final float WEAPON_SWAP_PREFILL_RATIO = 0.50F;

    public static final int AUTO_TOOL_SWITCH_COOLDOWN_TICKS = 10;

    private ResearchCombatV4Rules() {}

    public static boolean isShieldCounterWindowActive(long now, long windowUntil) {
        return now <= windowUntil && windowUntil > 0L;
    }

    public static boolean isOffCooldown(long now, long cooldownUntil) {
        return now >= cooldownUntil;
    }

    public static boolean isFallingAxeEligible(boolean researched, boolean holdingAxe,
                                               boolean onGround, float fallDistance, int persistedCooldownTicks) {
        return researched && holdingAxe && !onGround
                && fallDistance > FALLING_AXE_MIN_FALL && persistedCooldownTicks <= 0;
    }

    public static int weaponSwapPrefillTicker(int currentTicker, float fullAttackDelay) {
        if (fullAttackDelay <= 0.0F) return currentTicker;
        int half = Math.max(1, Math.round(fullAttackDelay * WEAPON_SWAP_PREFILL_RATIO));
        return Math.max(currentTicker, half);
    }

    public static int selectBestHotbarTool(float currentSpeed, float[] hotbarSpeeds, int currentSlot) {
        if (currentSpeed > 1.0F || hotbarSpeeds == null || hotbarSpeeds.length == 0) return -1;
        int bestSlot = -1;
        float bestSpeed = 1.0F;
        int limit = Math.min(9, hotbarSpeeds.length);
        for (int slot = 0; slot < limit; slot++) {
            if (slot == currentSlot) continue;
            if (hotbarSpeeds[slot] > bestSpeed) {
                bestSpeed = hotbarSpeeds[slot];
                bestSlot = slot;
            }
        }
        return bestSlot;
    }
}
