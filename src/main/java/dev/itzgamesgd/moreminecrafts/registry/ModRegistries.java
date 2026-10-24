package dev.itzgamesgd.moreminecrafts.registry;

import dev.itzgamesgd.moreminecrafts.ability.AbilityAttachments;
import dev.itzgamesgd.moreminecrafts.enchantment.EnchantmentRuntime;
import dev.itzgamesgd.moreminecrafts.alchemy.ModAlchemy;
import dev.itzgamesgd.moreminecrafts.worldgen.ModOreGeneration;
import dev.itzgamesgd.moreminecrafts.fishing.FishingChallenges;
import dev.itzgamesgd.moreminecrafts.fishing.FishermanFishSale;
import dev.itzgamesgd.moreminecrafts.special.elemental.WaterDiveRuntime;
import dev.itzgamesgd.moreminecrafts.special.elemental.EarthSeismicRuntime;
import dev.itzgamesgd.moreminecrafts.special.ElementalHitProgress;
import dev.itzgamesgd.moreminecrafts.equipment.EquipmentAttachments;
import dev.itzgamesgd.moreminecrafts.equipment.EquipmentRuntime;
import dev.itzgamesgd.moreminecrafts.accessory.AccessoryEquipmentRuntime;
import dev.itzgamesgd.moreminecrafts.mob.CustomMobCatalog;
import dev.itzgamesgd.moreminecrafts.progression.T2CoreAcquisition;
import dev.itzgamesgd.moreminecrafts.bounty.ContractObjectives;

public final class ModRegistries {
    private ModRegistries() {
    }

    public static void initialize() {
        CustomMobCatalog.initialize();
        ModAlchemy.initialize();
        EnchantmentRuntime.initialize();
        ModBlocks.initialize();
        ModBlockEntities.initialize();
        ModMenuTypes.initialize();
        AbilityAttachments.initialize();
        ModDataComponents.initialize();
        dev.itzgamesgd.moreminecrafts.progression.KnowledgeRuntime.initialize();
        dev.itzgamesgd.moreminecrafts.progression.XpQuestTracking.initialize();
        dev.itzgamesgd.moreminecrafts.progression.ResearchMovementRuntime.initialize();
        dev.itzgamesgd.moreminecrafts.progression.ResearchDebugCommands.initialize();
        ModItems.initialize();
        T2CoreAcquisition.initialize();
        ModOreGeneration.initialize();
        FishingChallenges.initialize();
        FishermanFishSale.initialize();
        WaterDiveRuntime.initialize();
        EarthSeismicRuntime.initialize();
        ElementalHitProgress.initialize();
        EquipmentAttachments.initialize();
        AccessoryEquipmentRuntime.initialize();
        EquipmentRuntime.initialize();
        ContractObjectives.initialize();
        dev.itzgamesgd.moreminecrafts.bounty.ContractLootIntegration.initialize();
        dev.itzgamesgd.moreminecrafts.equipment.MobCaptureRuntime.initialize();
    }
}
