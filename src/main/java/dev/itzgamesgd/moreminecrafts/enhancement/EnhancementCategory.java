package dev.itzgamesgd.moreminecrafts.enhancement;

/** The cap policy attached to an actual item/profile association. */
public enum EnhancementCategory {
    VANILLA(true, EnhancementRules.ORDINARY_MAX_LEVEL),
    ELEMENTAL(true, EnhancementRules.ORDINARY_MAX_LEVEL),
    SPECIAL(false, 0),
    ENDGAME(false, 0),
    UNSUPPORTED(false, 0);

    private final boolean enhanceable;
    private final int maximumLevel;

    EnhancementCategory(boolean enhanceable, int maximumLevel) {
        this.enhanceable = enhanceable;
        this.maximumLevel = maximumLevel;
    }

    public boolean enhanceable() {
        return enhanceable;
    }

    public int maximumLevel() {
        return maximumLevel;
    }

    public boolean allowsTransitionTo(int targetLevel) {
        return enhanceable && targetLevel > 0 && targetLevel <= maximumLevel;
    }
}
