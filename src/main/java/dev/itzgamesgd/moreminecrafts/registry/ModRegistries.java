package dev.itzgamesgd.moreminecrafts.registry;

import dev.itzgamesgd.moreminecrafts.ability.AbilityAttachments;
import dev.itzgamesgd.moreminecrafts.alchemy.ModAlchemy;
import dev.itzgamesgd.moreminecrafts.worldgen.ModOreGeneration;
import dev.itzgamesgd.moreminecrafts.fishing.FishingChallenges;
import dev.itzgamesgd.moreminecrafts.special.elemental.WaterDiveRuntime;

public final class ModRegistries {
    private ModRegistries() {
    }

    public static void initialize() {
        ModAlchemy.initialize();
        ModBlocks.initialize();
        AbilityAttachments.initialize();
        ModDataComponents.initialize();
        ModItems.initialize();
        ModOreGeneration.initialize();
        FishingChallenges.initialize();
        WaterDiveRuntime.initialize();
    }
}
