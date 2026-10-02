package dev.itzgamesgd.moreminecrafts.equipment;

/** Centralized, deterministic constants for the finalized lower-equipment contract. */
public final class LowerEquipmentRules {
    public static final int INJECTOR_SLOTS = 9;
    public static final int PORTABLE_FURNACE_STACK_LIMIT = 16;
    public static final int PORTABLE_FURNACE_COOK_TIME = 67;
    public static final int PORTABLE_FURNACE_FUEL_TICK_MULTIPLIER = 3;
    public static final int DRILL_COAL_FP = 8;
    public static final int DRILL_COAL_BLOCK_FP = 72;
    public static final int DRILL_FUEL_SLOT_LIMIT = 64;
    public static final int[] BACKPACK_CAPACITIES = {9, 27, 54};
    public static final int FISH_TRAP_SLOTS = 9;
    public static final int FISH_TRAP_INTERVAL_TICKS = 5 * 60 * 20;
    public static final float DAGGER_REACH = 2.5F;
    public static final float GREATSWORD_REACH = 3.75F;
    public static final double GREATSWORD_SWEEP_HALF_WIDTH = 1.5D;
    public static final float GAUNTLET_REACH = 3.0F;
    public static final float GAUNTLET_DIRECT_DAMAGE = 2.0F;
    public static final float GAUNTLET_SECONDARY_DAMAGE = 0.5F;
    public static final double GAUNTLET_REGION_LENGTH = 3.5D;
    public static final double GAUNTLET_REGION_WIDTH = 1.7D;
    /** Grounded impulse tuned for roughly two blocks of horizontal travel, not 2 blocks/tick velocity. */
    public static final double GAUNTLET_HORIZONTAL_KNOCKBACK_SPEED = 0.8D;

    private LowerEquipmentRules() {}

    public static int drillFuelPoints(boolean coalBlock) {
        return coalBlock ? DRILL_COAL_BLOCK_FP : DRILL_COAL_FP;
    }

    public static int firstOccupiedPotionSlot(boolean[] validPotionSlots) {
        if (validPotionSlots.length != INJECTOR_SLOTS) {
            throw new IllegalArgumentException("Injector must have exactly nine slots");
        }
        for (int i = 0; i < validPotionSlots.length; i++) if (validPotionSlots[i]) return i;
        return -1;
    }

    public static boolean fishTrapCanProduce(boolean[] occupied) {
        if (occupied.length != FISH_TRAP_SLOTS) throw new IllegalArgumentException("Fish Trap must have nine slots");
        for (boolean slot : occupied) if (!slot) return true;
        return false;
    }
}
