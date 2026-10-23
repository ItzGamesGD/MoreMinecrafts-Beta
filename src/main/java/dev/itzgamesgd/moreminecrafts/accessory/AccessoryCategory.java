package dev.itzgamesgd.moreminecrafts.accessory;

import net.minecraft.network.chat.Component;

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

    /** Player-facing category name; the two-part categories read "Defense / Survival" etc. */
    public Component displayName() {
        return switch (this) {
            case COMBAT -> name("combat");
            case DEFENSE_SURVIVAL -> pair("defense", "survival");
            case SUPPORT_RECOVERY -> pair("support", "recovery");
            case EXPLORATION_MOBILITY -> pair("exploration", "mobility");
            case LIFESTYLE_GATHERING -> pair("lifestyle", "gathering");
        };
    }

    private static Component name(String key) {
        return Component.translatable("accessory.category." + key);
    }

    private static Component pair(String first, String second) {
        return Component.empty().append(name(first)).append(" / ").append(name(second));
    }
}
