package dev.itzgamesgd.moreminecrafts.registry;

import dev.itzgamesgd.moreminecrafts.ability.AbilityAttachments;
import dev.itzgamesgd.moreminecrafts.enchantment.EnchantmentRuntime;
import dev.itzgamesgd.moreminecrafts.alchemy.ModAlchemy;
import dev.itzgamesgd.moreminecrafts.worldgen.ModOreGeneration;
import dev.itzgamesgd.moreminecrafts.fishing.FishingChallenges;
import dev.itzgamesgd.moreminecrafts.special.elemental.WaterDiveRuntime;
import dev.itzgamesgd.moreminecrafts.special.ElementalHitProgress;
import dev.itzgamesgd.moreminecrafts.equipment.EquipmentAttachments;
import dev.itzgamesgd.moreminecrafts.equipment.EquipmentRuntime;

public final class ModRegistries {
    private ModRegistries() {
    }

    public static void initialize() {
        ModAlchemy.initialize();
        EnchantmentRuntime.initialize();
        ModBlocks.initialize();
        ModBlockEntities.initialize();
        ModMenuTypes.initialize();
        AbilityAttachments.initialize();
        ModDataComponents.initialize();
        ModItems.initialize();
        ModOreGeneration.initialize();
        FishingChallenges.initialize();
        WaterDiveRuntime.initialize();
        ElementalHitProgress.initialize();
        EquipmentAttachments.initialize();
        EquipmentRuntime.initialize();
    }
}
