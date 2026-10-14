package dev.itzgamesgd.moreminecrafts.fishing;

import java.util.Set;
import net.minecraft.util.RandomSource;

/** Tunable species data. Length and Weight measurements are tenths of cm/kg; expected Weight follows Length. */
public enum FishSpecies {
    CRUCIAN_CARP("crucian_carp", 110, 1, 1, 1, 10, 120, 520, 2, 38, Ecology.FRESHWATER),
    COMMON_CARP("common_carp", 100, 1, 1, 1, 13, 250, 1100, 15, 400, Ecology.FRESHWATER),
    AMUR_CATFISH("amur_catfish", 88, 1, 2, 2, 16, 300, 1300, 20, 450, Ecology.FRESHWATER),
    RAINBOW_TROUT("rainbow_trout", 82, 1, 1, 2, 22, 200, 900, 8, 180, Ecology.COOL_FRESHWATER),
    LARGEMOUTH_BASS("largemouth_bass", 68, 2, 1, 2, 25, 250, 1000, 10, 220, Ecology.FRESHWATER),
    NORTHERN_PIKE("northern_pike", 56, 2, 2, 3, 30, 350, 1500, 20, 500, Ecology.COOL_FRESHWATER),
    ATLANTIC_SALMON("atlantic_salmon", 48, 2, 2, 3, 42, 400, 1500, 25, 480, Ecology.COASTAL_OR_RIVER),
    YELLOWFIN_TUNA("yellowfin_tuna", 40, 2, 3, 3, 55, 700, 2200, 80, 2000, Ecology.OCEAN),
    ARAPAIMA("arapaima", 28, 3, 2, 4, 75, 1200, 3200, 250, 2200, Ecology.WARM_FRESHWATER),
    SWORDFISH("swordfish", 22, 3, 3, 4, 100, 1200, 4500, 300, 5000, Ecology.OCEAN),
    BLUE_MARLIN("blue_marlin", 16, 3, 3, 5, 140, 1500, 5000, 400, 7500, Ecology.OCEAN),
    BELUGA_STURGEON("beluga_sturgeon", 10, 3, 3, 5, 200, 1000, 6000, 500, 12000, Ecology.COASTAL_OR_RIVER);

    public enum Ecology { FRESHWATER, COOL_FRESHWATER, WARM_FRESHWATER, COASTAL_OR_RIVER, OCEAN }

    private final String id;
    private final int encounterWeight, requiredReelTier, requiredMinnowTier, fishingDifficulty, baseValue;
    private final MeasurementDistribution length, weight;
    private final Ecology ecology;

    FishSpecies(String id, int encounterWeight, int requiredReelTier, int requiredMinnowTier,
                int fishingDifficulty, int baseValue, int minLengthTenthsCm, int maxLengthTenthsCm,
                int minWeightTenthsKg, int maxWeightTenthsKg, Ecology ecology) {
        this.id = id;
        this.encounterWeight = encounterWeight;
        this.requiredReelTier = FishingEquipmentTier.requireValid(requiredReelTier);
        this.requiredMinnowTier = FishingEquipmentTier.requireValid(requiredMinnowTier);
        if (fishingDifficulty < 1 || fishingDifficulty > 5) throw new IllegalArgumentException("Difficulty must be 1-5");
        this.fishingDifficulty = fishingDifficulty;
        this.baseValue = baseValue;
        this.length = new MeasurementDistribution(minLengthTenthsCm, maxLengthTenthsCm);
        this.weight = new MeasurementDistribution(minWeightTenthsKg, maxWeightTenthsKg);
        this.ecology = ecology;
    }

    public String id() { return id; }
    public int encounterWeight() { return encounterWeight; }
    public int requiredReelTier() { return requiredReelTier; }
    public int requiredMinnowTier() { return requiredMinnowTier; }
    public int fishingDifficulty() { return fishingDifficulty; }
    public int baseValue() { return baseValue; }
    public MeasurementDistribution lengthDistribution() { return length; }
    public MeasurementDistribution weightDistribution() { return weight; }
    public Ecology ecology() { return ecology; }

    /** Expected weight rises nonlinearly with length while remaining species-specific. */
    public int expectedWeightForLength(int lengthTenthsCm) {
        double normalizedLength = length.percentile(lengthTenthsCm);
        double sizeCurve = normalizedLength * normalizedLength;
        return weight.minimum() + (int) Math.round(sizeCurve * (weight.maximum() - weight.minimum()));
    }

    /** Applies bounded individual variation around the expected weight; variation must be in [-1, 1]. */
    public int weightForLength(int lengthTenthsCm, double variation) {
        if (!Double.isFinite(variation) || variation < -1.0 || variation > 1.0)
            throw new IllegalArgumentException("Weight variation must be in [-1, 1]");
        int spread = Math.max(1, (int) Math.round((weight.maximum() - weight.minimum()) * 0.04));
        int varied = expectedWeightForLength(lengthTenthsCm) + (int) Math.round(variation * spread);
        return Math.clamp(varied, weight.minimum(), weight.maximum());
    }

    /** A bounded triangular distribution: extremes are possible but less common. */
    public record MeasurementDistribution(int minimum, int maximum) {
        public MeasurementDistribution {
            if (minimum < 1 || maximum <= minimum) throw new IllegalArgumentException("Invalid measurement bounds");
        }
        public int roll(RandomSource random) {
            double percentile = (random.nextDouble() + random.nextDouble()) / 2.0;
            return minimum + (int) Math.round(percentile * (maximum - minimum));
        }
        public double percentile(int value) {
            return Math.clamp((value - minimum) / (double) (maximum - minimum), 0.0, 1.0);
        }
    }
}
