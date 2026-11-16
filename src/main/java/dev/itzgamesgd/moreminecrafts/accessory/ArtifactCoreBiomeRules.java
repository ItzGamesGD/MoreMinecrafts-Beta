package dev.itzgamesgd.moreminecrafts.accessory;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;

/** Applies the five ordered location rules from the approved relic-core biome specification. */
public final class ArtifactCoreBiomeRules {
    public enum Dimension { OVERWORLD, NETHER, END, OTHER }

    public enum BiomeTag {
        DEEP_DARK, DRIPSTONE_CAVES, LUSH_CAVES,
        BADLANDS, SAVANNA, DESERT, JUNGLE,
        OCEAN, DEEP_OCEAN, RIVER, BEACH, STONY_SHORE, SWAMP, MANGROVE_SWAMP,
        SNOWY, MOUNTAIN, HILL, WINDSWEPT, STONY_PEAKS, MEADOW, CHERRY_GROVE
    }

    public record Location(Dimension dimension, int y, boolean skyVisible, Set<BiomeTag> tags, boolean inTrialChambers) {
        public Location(Dimension dimension, int y, boolean skyVisible, Set<BiomeTag> tags) {
            this(dimension, y, skyVisible, tags, false);
        }
        public Location { tags = Set.copyOf(tags); }
    }

    private ArtifactCoreBiomeRules() {}

    /** Returns null where the source rules do not assign a class, such as an undocumented custom dimension. */
    public static ArtifactCore classify(Location location) {
        if (location.dimension() == Dimension.END) return ArtifactCore.ABYSSAL;
        if (location.dimension() == Dimension.NETHER) return ArtifactCore.BLAZING;
        if (location.dimension() != Dimension.OVERWORLD) return null;

        Set<BiomeTag> tags = location.tags();
        if (hasAny(tags, BiomeTag.DEEP_DARK, BiomeTag.DRIPSTONE_CAVES, BiomeTag.LUSH_CAVES)
                || (!location.skyVisible() && location.y() <= 45)) return ArtifactCore.ABYSSAL;
        if (hasAny(tags, BiomeTag.BADLANDS, BiomeTag.SAVANNA, BiomeTag.DESERT, BiomeTag.JUNGLE))
            return ArtifactCore.BLAZING;
        if (hasAny(tags, BiomeTag.OCEAN, BiomeTag.DEEP_OCEAN, BiomeTag.RIVER, BiomeTag.BEACH, BiomeTag.STONY_SHORE,
                BiomeTag.SWAMP, BiomeTag.MANGROVE_SWAMP)) return ArtifactCore.TIDAL;
        if (hasAny(tags, BiomeTag.SNOWY, BiomeTag.MOUNTAIN, BiomeTag.HILL, BiomeTag.WINDSWEPT, BiomeTag.STONY_PEAKS,
                BiomeTag.MEADOW, BiomeTag.CHERRY_GROVE) || location.inTrialChambers()
                || (location.skyVisible() && location.y() >= 140)) return ArtifactCore.GLACIAL;
        // The owner-approved fallback for undocumented, sky-blocked positions above Y=45.
        return ArtifactCore.EARTHEN;
    }

    /** Reads the real dimension, biome holder tags, surface visibility, and Trial Chambers structure at a position. */
    public static ArtifactCore classify(ServerLevel level, BlockPos pos) {
        Holder<Biome> biome = level.getBiome(pos);
        Identifier biomeId = biome.unwrapKey().map(key -> key.identifier()).orElse(null);
        Set<BiomeTag> tags = new java.util.HashSet<>();
        if (biomeId != null) {
            if (is(biomeId, "deep_dark")) tags.add(BiomeTag.DEEP_DARK);
            if (is(biomeId, "dripstone_caves")) tags.add(BiomeTag.DRIPSTONE_CAVES);
            if (is(biomeId, "lush_caves")) tags.add(BiomeTag.LUSH_CAVES);
            if (is(biomeId, "desert")) tags.add(BiomeTag.DESERT);
            if (is(biomeId, "swamp")) tags.add(BiomeTag.SWAMP);
            if (is(biomeId, "mangrove_swamp")) tags.add(BiomeTag.MANGROVE_SWAMP);
            if (is(biomeId, "stony_shore")) tags.add(BiomeTag.STONY_SHORE);
            if (isAny(biomeId, "snowy_plains", "ice_spikes", "snowy_taiga", "snowy_beach", "frozen_peaks", "jagged_peaks", "snowy_slopes", "grove")) tags.add(BiomeTag.SNOWY);
            if (isAny(biomeId, "windswept_hills", "windswept_forest", "windswept_gravelly_hills")) tags.add(BiomeTag.WINDSWEPT);
            if (is(biomeId, "stony_peaks")) tags.add(BiomeTag.STONY_PEAKS);
            if (is(biomeId, "meadow")) tags.add(BiomeTag.MEADOW);
            if (is(biomeId, "cherry_grove")) tags.add(BiomeTag.CHERRY_GROVE);
        }
        if (biome.is(BiomeTags.IS_BADLANDS)) tags.add(BiomeTag.BADLANDS);
        if (biome.is(BiomeTags.IS_SAVANNA)) tags.add(BiomeTag.SAVANNA);
        if (biome.is(BiomeTags.IS_JUNGLE)) tags.add(BiomeTag.JUNGLE);
        if (biome.is(BiomeTags.IS_OCEAN)) tags.add(BiomeTag.OCEAN);
        if (biome.is(BiomeTags.IS_DEEP_OCEAN)) tags.add(BiomeTag.DEEP_OCEAN);
        if (biome.is(BiomeTags.IS_RIVER)) tags.add(BiomeTag.RIVER);
        if (biome.is(BiomeTags.IS_BEACH)) tags.add(BiomeTag.BEACH);
        if (biome.is(BiomeTags.IS_MOUNTAIN)) tags.add(BiomeTag.MOUNTAIN);
        if (biome.is(BiomeTags.IS_HILL)) tags.add(BiomeTag.HILL);

        Dimension dimension = level.dimension() == Level.OVERWORLD ? Dimension.OVERWORLD
                : level.dimension() == Level.NETHER ? Dimension.NETHER
                : level.dimension() == Level.END ? Dimension.END : Dimension.OTHER;
        boolean trialChambers = level.structureManager().getStructureWithPieceAt(pos,
                holder -> holder.is(BuiltinStructures.TRIAL_CHAMBERS)).isValid();
        return classify(new Location(dimension, pos.getY(), level.canSeeSky(pos), tags, trialChambers));
    }

    private static boolean hasAny(Set<BiomeTag> tags, BiomeTag... candidates) {
        for (BiomeTag candidate : candidates) if (tags.contains(candidate)) return true;
        return false;
    }
    private static boolean is(Identifier id, String path) { return id.getNamespace().equals("minecraft") && id.getPath().equals(path); }
    private static boolean isAny(Identifier id, String... paths) {
        for (String path : paths) if (is(id, path)) return true;
        return false;
    }
}
