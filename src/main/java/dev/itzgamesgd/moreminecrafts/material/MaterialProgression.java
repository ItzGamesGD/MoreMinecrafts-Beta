package dev.itzgamesgd.moreminecrafts.material;

import java.util.List;

/** Tunable Stage 6 progression and placement values, kept independent of registries. */
public final class MaterialProgression {
    public static final Definition TITANIUM = new Definition("titanium", 1, -16, 80, 32, 9, 14);
    public static final Definition TUNGSTEN = new Definition("tungsten", 2, -48, 32, -16, 8, 12);
    public static final Definition MYTHRIL = new Definition("mythril", 3, -64, 0, -48, 6, 1);
    public static final Definition ETHERIUM = new Definition("etherium", 3, -32, 64, 16, 6, 1);
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
