package dev.itzgamesgd.moreminecrafts.equipment;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

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

    public static boolean isPickaxeMineable(BlockState state) {
        return state.is(BlockTags.MINEABLE_WITH_PICKAXE);
    }

    public static boolean isWithinGreatswordSweep(Vec3 offset, Vec3 direction) {
        Vec3 horizontalOffset = new Vec3(offset.x, 0.0D, offset.z);
        Vec3 horizontalDirection = new Vec3(direction.x, 0.0D, direction.z).normalize();
        double along = horizontalOffset.dot(horizontalDirection);
        double lateral = horizontalOffset.subtract(horizontalDirection.scale(along)).length();
        return along >= 0.0D && along <= GREATSWORD_REACH && lateral <= GREATSWORD_SWEEP_HALF_WIDTH;
    }

    public static float greatswordDamageMultiplier(float attackStrength) {
        float strength = Math.max(0.0F, Math.min(1.0F, attackStrength));
        return 0.2F + 0.8F * strength * strength;
    }

    public static boolean isInGauntletSecondaryRegion(double longitudinal, double lateral) {
        return longitudinal <= 0.0D && longitudinal >= -GAUNTLET_REGION_LENGTH
                && Math.abs(lateral) <= GAUNTLET_REGION_WIDTH / 2.0D;
    }

    public static int advanceFishTrapTimer(int currentTicks, boolean full) {
        if (full) return currentTicks;
        return Math.min(FISH_TRAP_INTERVAL_TICKS, Math.max(0, currentTicks) + 1);
    }

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
