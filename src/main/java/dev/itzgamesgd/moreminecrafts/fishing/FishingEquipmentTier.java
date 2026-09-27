package dev.itzgamesgd.moreminecrafts.fishing;

/** Equipment progression is deliberately independent from five-level fish Grade. */
public final class FishingEquipmentTier {
    public static final int MIN = 1;
    public static final int MAX = 3;
    private FishingEquipmentTier() {}
    public static int requireValid(int tier) {
        if (tier < MIN || tier > MAX) throw new IllegalArgumentException("Fishing equipment tier must be 1-3");
        return tier;
    }
}
