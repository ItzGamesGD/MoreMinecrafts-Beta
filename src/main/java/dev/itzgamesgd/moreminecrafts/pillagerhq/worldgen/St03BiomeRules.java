package dev.itzgamesgd.moreminecrafts.pillagerhq.worldgen;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

/**
 * Natural-generation biomes of the ST-03 Pillager Headquarters (owner approved 2026-11-17: 19 biomes).
 *
 * <p>This list is the single source for the center-biome and footprint-biome checks ({@link St03HeadquartersStructure#
 * isEligibleBiome}) and must stay identical to the {@code biomes} array of
 * {@code worldgen/structure/st03_pillager_headquarters.json}. No other biome may be added without owner approval.
 */
public final class St03BiomeRules {
    private St03BiomeRules() {}

    /** The original eight biomes, kept unchanged. */
    public static final List<String> ORIGINAL_BIOME_IDS = List.of(
            "minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow", "minecraft:forest",
            "minecraft:flower_forest", "minecraft:birch_forest", "minecraft:old_growth_birch_forest",
            "minecraft:dark_forest");

    /** The eleven biomes added by the 2026-11-17 owner approval. */
    public static final List<String> ADDED_BIOME_IDS = List.of(
            "minecraft:savanna", "minecraft:savanna_plateau", "minecraft:taiga", "minecraft:snowy_plains",
            "minecraft:snowy_taiga", "minecraft:dappled_forest", "minecraft:pale_garden",
            "minecraft:old_growth_pine_taiga", "minecraft:old_growth_spruce_taiga", "minecraft:sparse_jungle",
            "minecraft:wooded_badlands");

    public static final List<String> ELIGIBLE_BIOME_IDS = concat(ORIGINAL_BIOME_IDS, ADDED_BIOME_IDS);

    private static final Set<String> ELIGIBLE = Set.copyOf(ELIGIBLE_BIOME_IDS);

    public static boolean isEligible(Holder<Biome> biome) {
        return biome.unwrapKey().map(key -> ELIGIBLE.contains(key.identifier().toString())).orElse(false);
    }

    private static List<String> concat(List<String> first, List<String> second) {
        Set<String> seen = new HashSet<>(first);
        for (String id : second) if (!seen.add(id)) throw new IllegalStateException("Duplicate ST-03 biome " + id);
        var out = new java.util.ArrayList<>(first);
        out.addAll(second);
        return List.copyOf(out);
    }
}
