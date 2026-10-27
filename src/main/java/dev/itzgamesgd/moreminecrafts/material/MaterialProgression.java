package dev.itzgamesgd.moreminecrafts.material;

import java.util.List;

/** Tunable Stage 6 progression and placement values, kept independent of registries. */
public final class MaterialProgression {
    /**
     * Nominal diamond ore blocks per chunk in vanilla: sum of count x vein size over ore_diamond (7x4),
     * ore_diamond_medium (2x8), ore_diamond_buried (4x8) and ore_diamond_large (1/9 x 12), ignoring air-exposure discard.
     */
    public static final double VANILLA_DIAMOND_NOMINAL_ORES_PER_CHUNK = 7 * 4 + 2 * 8 + 4 * 8 + 12 / 9.0;
    /** T2 §18.1: etherium ore volume is one quarter of diamond, in single-ore veins. */
    public static final int ETHERIUM_ATTEMPTS_PER_CHUNK = 19;
    public static final Definition TITANIUM = new Definition("titanium", 1, -16, 80, 32, 9, 14);
    public static final Definition TUNGSTEN = new Definition("tungsten", 2, -48, 32, -16, 8, 12);
    public static final Definition MYTHRIL = new Definition("mythril", 3, -64, 0, -48, 6, 1);
    public static final Definition ETHERIUM = new Definition("etherium", 3, -32, 64, 16, 1, ETHERIUM_ATTEMPTS_PER_CHUNK);
    public static final Definition ADAMANTIUM = new Definition("adamantium", 4, 8, 24, 16, 3, 1);
    public static final List<Definition> ORDERED = List.of(TITANIUM, TUNGSTEN, MYTHRIL, ETHERIUM, ADAMANTIUM);

    private MaterialProgression() {}

    public record Definition(String name, int progressionTier, int minimumY, int maximumY,
            int peakY, int veinSize, int attemptsPerChunk) {
        public Definition {
            if (minimumY > peakY || peakY > maximumY || veinSize < 1 || attemptsPerChunk < 1) {
                throw new IllegalArgumentException("Invalid mineral definition: " + name);
            }
        }
    }
}
