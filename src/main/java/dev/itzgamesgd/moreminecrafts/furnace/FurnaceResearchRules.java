package dev.itzgamesgd.moreminecrafts.furnace;

/** Deterministic, server-side arithmetic for the three approved furnace research effects. */
public final class FurnaceResearchRules {
    private FurnaceResearchRules() {}

    public record Fuel(int burnTicks, double remainder) {}
    public record Cooking(int timer, double remainder, int credit) {}

    public static Fuel fuelDuration(int baseTicks, int level, double remainder) {
        if (baseTicks <= 0 || level <= 0) return new Fuel(baseTicks, remainder);
        int reduction = Math.min(3, level) * 850;
        double exact = baseTicks * 10000.0D / (10000 - reduction) + remainder;
        // Minecraft 26.3 persists the burn timer as an int. Preserve oversized modded fuel
        // values instead of overflowing if their adjusted duration is not representable.
        if (exact > Integer.MAX_VALUE) return new Fuel(baseTicks, remainder);
        int ticks = (int) Math.floor(exact);
        return new Fuel(ticks, exact - ticks);
    }

    public static Cooking cookingTick(int vanillaTimer, int totalTime, int level,
                                       double remainder, int credit) {
        if (totalTime <= 0 || vanillaTimer <= 0) return new Cooking(vanillaTimer, remainder, credit);
        double exact = remainder + Math.min(3, Math.max(0, level)) * 0.1D;
        int bonus = (int) Math.floor(exact + 1.0E-9D);
        double nextRemainder = exact - bonus;
        if (nextRemainder < 0) nextRemainder = 0;
        int progress = vanillaTimer + credit + bonus;
        // A one-tick recipe cannot finish more than once per server tick. Discard an unusable
        // whole-tick surplus there, and bound all other cross-recipe credit to one tick.
        int nextCredit = totalTime == 1 ? 0 : Math.min(1, Math.max(0, progress - totalTime));
        return new Cooking(Math.min(totalTime, progress), nextRemainder, nextCredit);
    }

    public static int precisionChanceBasisPoints(int level) {
        return Math.min(5, Math.max(0, level)) * 750;
    }

    public static boolean precisionBonus(int level, int rollBasisPoints) {
        return rollBasisPoints >= 0 && rollBasisPoints < precisionChanceBasisPoints(level);
    }

    public static boolean fitsPossibleBonus(int occupied, int baseResult, int maxStack) {
        return occupied >= 0 && baseResult > 0 && occupied + baseResult + 1 <= maxStack;
    }
}
