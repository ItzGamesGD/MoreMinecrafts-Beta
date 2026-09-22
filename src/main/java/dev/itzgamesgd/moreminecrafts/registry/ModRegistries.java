package dev.itzgamesgd.moreminecrafts.registry;

import dev.itzgamesgd.moreminecrafts.ability.AbilityAttachments;

public final class ModRegistries {
    private ModRegistries() {
    }

    public static void initialize() {
        AbilityAttachments.initialize();
        ModDataComponents.initialize();
        ModItems.initialize();
    }
}
