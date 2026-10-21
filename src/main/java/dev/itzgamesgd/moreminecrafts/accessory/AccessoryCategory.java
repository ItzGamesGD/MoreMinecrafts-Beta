package dev.itzgamesgd.moreminecrafts.accessory;

/** The five finalized accessory categories and their corresponding equipment slots. */
public enum AccessoryCategory {
    COMBAT(true),
    DEFENSE_SURVIVAL(false),
    SUPPORT_RECOVERY(false),
    EXPLORATION_MOBILITY(false),
    LIFESTYLE_GATHERING(false);

    private final boolean manuallyActive;

    AccessoryCategory(boolean manuallyActive) {
        this.manuallyActive = manuallyActive;
    }

    public boolean allowsManualActivation() {
        return manuallyActive;
    }
}
