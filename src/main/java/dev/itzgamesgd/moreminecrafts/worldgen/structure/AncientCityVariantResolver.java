package dev.itzgamesgd.moreminecrafts.worldgen.structure;

import net.minecraft.world.level.ChunkPos;

/**
 * Pure deterministic city-scoped variant selection.
 *
 * <p>The stable identity is the Ancient City structure-start chunk. The world
 * seed is mixed with that identity and a fixed domain salt, so selection does
 * not depend on generation order, mutable global state, or chunk load order.</p>
 */
public final class AncientCityVariantResolver {\n    // Architecture probe only: this class does not alter Ancient City generation by itself.
    private static final long DOMAIN_SALT = 0x4D4D5F414E434954L;
    private static final long X_MIX = 0x9E3779B97F4A7C15L;
    private static final long Z_MIX = 0xC2B2AE3D27D4EB4FL;

    private AncientCityVariantResolver() {}

    public static AncientCityVariant resolve(long worldSeed, ChunkPos structureStartChunk) {
        long value = worldSeed ^ DOMAIN_SALT;
        value ^= ((long) structureStartChunk.x()) * X_MIX;
        value ^= ((long) structureStartChunk.z()) * Z_MIX;
        long mixed = mix64(value);
        int index = (int) Math.floorMod(mixed, AncientCityVariant.values().length);
        return AncientCityVariant.values()[index];
    }

    public static long cityIdentity(ChunkPos structureStartChunk) {
        return structureStartChunk.pack();
    }

    static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }
}
