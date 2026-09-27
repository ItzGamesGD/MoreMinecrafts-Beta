package dev.itzgamesgd.moreminecrafts.registry;

import dev.itzgamesgd.moreminecrafts.ability.AbilityAttachments;
import dev.itzgamesgd.moreminecrafts.ability.EquipmentAbilityBindings;
import dev.itzgamesgd.moreminecrafts.ability.EquipmentSkillInput;
import dev.itzgamesgd.moreminecrafts.special.flame.FlameCleaveAbility;
import dev.itzgamesgd.moreminecrafts.special.wind.WindBladeAbility;
import dev.itzgamesgd.moreminecrafts.special.wind.WindStepAbility;
import dev.itzgamesgd.moreminecrafts.worldgen.ModOreGeneration;
import dev.itzgamesgd.moreminecrafts.fishing.FishingChallenges;

public final class ModRegistries {
    private ModRegistries() {
    }

    public static void initialize() {
        ModBlocks.initialize();
        ModMenuTypes.initialize();
        AbilityAttachments.initialize();
        ModDataComponents.initialize();
        ModItems.initialize();
        EquipmentAbilityBindings.bind(ModItems.FLAME_SWORD, EquipmentSkillInput.PRIMARY, FlameCleaveAbility.INSTANCE);
        EquipmentAbilityBindings.bind(ModItems.WIND_SWORD, EquipmentSkillInput.PRIMARY, WindBladeAbility.INSTANCE);
        EquipmentAbilityBindings.bind(ModItems.WIND_SWORD, EquipmentSkillInput.SECONDARY, WindStepAbility.INSTANCE);
        ModOreGeneration.initialize();
        FishingChallenges.initialize();
    }
}
