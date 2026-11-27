package dev.itzgamesgd.moreminecrafts.progression;

import java.util.List;

/** Deterministic purchase-level and material-priority rules for Precision Crafting. */
public final class PrecisionCraftingRules {
    public static final int ROLL_RANGE = 1000;
    public static final int MAX_LEVEL = 5;

    public enum MaterialClass {
        ORDINARY(0, new int[]{0, 100, 150, 200, 250, 300}),
        PROCESSED(1, new int[]{0, 70, 105, 140, 175, 210}),
        PRECIOUS(2, new int[]{0, 40, 60, 80, 100, 120}),
        RARE(3, new int[]{0, 20, 30, 40, 50, 60});

        private final int priority;
        private final int[] chancePermille;

        MaterialClass(int priority, int[] chancePermille) {
            this.priority = priority;
            this.chancePermille = chancePermille;
        }

        public int chancePermille(int purchasedLevel) {
            int level = Math.max(0, Math.min(purchasedLevel, MAX_LEVEL));
            return chancePermille[level];
        }
    }

    private PrecisionCraftingRules() {}

    /** Returns the earliest slot in the rarest eligible class, or -1 when no slot is eligible. */
    public static int selectedSlot(List<MaterialClass> slots) {
        int selected = -1;
        int highestPriority = -1;
        for (int slot = 0; slot < slots.size(); slot++) {
            MaterialClass materialClass = slots.get(slot);
            if (materialClass != null && materialClass.priority > highestPriority) {
                selected = slot;
                highestPriority = materialClass.priority;
            }
        }
        return selected;
    }

    /** Uses 0.1-percentage-point fixed precision, sufficient for every approved chance. */
    public static boolean succeeds(MaterialClass materialClass, int purchasedLevel, int rollPermille) {
        if (rollPermille < 0 || rollPermille >= ROLL_RANGE)
            throw new IllegalArgumentException("Precision Crafting roll must be in [0, 999]");
        return materialClass != null && rollPermille < materialClass.chancePermille(purchasedLevel);
    }
}
