package dev.itzgamesgd.moreminecrafts.registry;

public final class ModRegistries {
    private ModRegistries() {
    }

    public static void initialize() {
        ModDataComponents.initialize();
        ModItems.initialize();
    }
}
