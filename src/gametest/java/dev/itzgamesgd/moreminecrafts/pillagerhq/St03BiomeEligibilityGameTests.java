package dev.itzgamesgd.moreminecrafts.pillagerhq;

import dev.itzgamesgd.moreminecrafts.pillagerhq.worldgen.St03BiomeRules;
import dev.itzgamesgd.moreminecrafts.pillagerhq.worldgen.St03HeadquartersStructure;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biomes;

/** Live-registry check of the owner-approved 19-biome ST-03 list: every id exists, eligibility is exact. */
public final class St03BiomeEligibilityGameTests {
    @GameTest public void everyApprovedBiomeExistsInTheRegistryAndIsEligible(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
        List<String> missing = new ArrayList<>(), notEligible = new ArrayList<>();
        for (String id : St03BiomeRules.ELIGIBLE_BIOME_IDS) {
            var holder = registry.get(ResourceKey.create(Registries.BIOME, Identifier.parse(id)));
            if (holder.isEmpty()) {missing.add(id);continue;}
            if (!St03HeadquartersStructure.isEligibleBiome(holder.get())) notEligible.add(id);
        }
        helper.assertTrue(St03BiomeRules.ELIGIBLE_BIOME_IDS.size() == 19, "exactly 19 approved biomes");
        helper.assertTrue(missing.isEmpty(), "approved biome ids missing from the registry: " + missing);
        helper.assertTrue(notEligible.isEmpty(), "approved biomes rejected by isEligibleBiome: " + notEligible);
        helper.succeed();
    }

    @GameTest public void excludedBiomesStayRejectedAndEdgeBiomesStayEdge(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
        var excluded = List.of(Biomes.DESERT, Biomes.SWAMP, Biomes.JUNGLE, Biomes.BADLANDS, Biomes.ERODED_BADLANDS,
                Biomes.OCEAN, Biomes.DEEP_OCEAN, Biomes.RIVER, Biomes.BEACH, Biomes.CHERRY_GROVE, Biomes.ICE_SPIKES,
                Biomes.MANGROVE_SWAMP, Biomes.BAMBOO_JUNGLE, Biomes.WINDSWEPT_HILLS, Biomes.STONY_PEAKS);
        for (var key : excluded)
            helper.assertTrue(!St03HeadquartersStructure.isEligibleBiome(registry.getOrThrow(key)),
                    key.identifier() + " must not be eligible");
        for (var key : List.of(Biomes.BEACH, Biomes.SNOWY_BEACH, Biomes.STONY_SHORE, Biomes.RIVER, Biomes.FROZEN_RIVER))
            helper.assertTrue(St03HeadquartersStructure.isEdgeBiome(registry.getOrThrow(key)),
                    key.identifier() + " stays an edge-neutral biome");
        helper.succeed();
    }
}
