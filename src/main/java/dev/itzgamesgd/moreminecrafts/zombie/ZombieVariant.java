package dev.itzgamesgd.moreminecrafts.zombie;

public enum ZombieVariant {
    NORMAL,
    BOMB,
    LEAP,
    MINING_GIANT;

    public static ZombieVariant fromSavedName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException exception) {
            return NORMAL;
        }
    }
}
