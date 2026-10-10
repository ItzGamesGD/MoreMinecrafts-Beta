package dev.itzgamesgd.moreminecrafts.worldgen;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public final class ModOreGeneration {
    private ModOreGeneration() {}

    public static void initialize() {
        if ("1".equals(System.getenv("MOREMINECRAFTS_VISUAL_VALIDATION"))) return;
        addOverworld("ore_titanium");
        addOverworld("ore_tungsten");
        addOverworld("ore_tungsten_upper");
        addOverworld("ore_mythril");
        addOverworld("ore_mythril_upper");
        addOverworld("ore_etherium");
        BiomeModifications.addFeature(BiomeSelectors.foundInTheNether(),
                GenerationStep.Decoration.UNDERGROUND_DECORATION, key("ore_adamantium"));
    }

    private static void addOverworld(String path) {
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),
                GenerationStep.Decoration.UNDERGROUND_ORES, key(path));
    }

    private static ResourceKey<PlacedFeature> key(String path) {
        return ResourceKey.create(Registries.PLACED_FEATURE, ModIds.id(path));
    }
}
