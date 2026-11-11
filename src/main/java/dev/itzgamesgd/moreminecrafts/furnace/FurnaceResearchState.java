package dev.itzgamesgd.moreminecrafts.furnace;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.UUID;

/** Persistent attribution and fixed-point carry state reserved for future furnace research effects. */
public record FurnaceResearchState(Optional<UUID> attributedPlayer,
                                   double fuelSavingRemainder,
                                   double cookingProgressRemainder,
                                   boolean actualInteraction,
                                   int cookingCredit) {
    private static final Codec<UUID> UUID_CODEC = Codec.STRING.comapFlatMap(value -> {
        try {
            return DataResult.success(UUID.fromString(value));
        } catch (IllegalArgumentException malformed) {
            return DataResult.error(() -> "Malformed player UUID");
        }
    }, UUID::toString);

    private static final Codec<Double> REMAINDER_CODEC = Codec.DOUBLE.validate(value ->
            Double.isFinite(value) && value >= 0.0D && value < 1.0D
                    ? DataResult.success(value)
                    : DataResult.error(() -> "Furnace fractional remainder must be in [0, 1)"));

    public static final Codec<FurnaceResearchState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUID_CODEC.optionalFieldOf("attributed_player").forGetter(FurnaceResearchState::attributedPlayer),
            REMAINDER_CODEC.optionalFieldOf("fuel_saving_remainder", 0.0D)
                    .forGetter(FurnaceResearchState::fuelSavingRemainder),
            REMAINDER_CODEC.optionalFieldOf("cooking_progress_remainder", 0.0D)
                    .forGetter(FurnaceResearchState::cookingProgressRemainder),
            Codec.BOOL.optionalFieldOf("actual_interaction", false)
                    .forGetter(FurnaceResearchState::actualInteraction),
            Codec.intRange(0, 1)
                    .optionalFieldOf("cooking_credit", 0).forGetter(FurnaceResearchState::cookingCredit)
    ).apply(instance, FurnaceResearchState::new));

    public FurnaceResearchState(Optional<UUID> attributedPlayer, double fuelSavingRemainder,
                                double cookingProgressRemainder) {
        this(attributedPlayer, fuelSavingRemainder, cookingProgressRemainder, false, 0);
    }

    public FurnaceResearchState {
        attributedPlayer = attributedPlayer == null ? Optional.empty() : attributedPlayer;
        if (!validRemainder(fuelSavingRemainder) || !validRemainder(cookingProgressRemainder))
            throw new IllegalArgumentException("Furnace remainder must be finite and in [0, 1)");
        if (cookingCredit < 0 || cookingCredit > 1)
            throw new IllegalArgumentException("Invalid cooking credit");
    }

    public static FurnaceResearchState empty() {
        return new FurnaceResearchState(Optional.empty(), 0.0D, 0.0D, false, 0);
    }

    public FurnaceResearchState attributedTo(UUID player) {
        return new FurnaceResearchState(Optional.of(player), fuelSavingRemainder, cookingProgressRemainder,
                actualInteraction, cookingCredit);
    }

    public FurnaceResearchState interactedBy(UUID player) {
        return new FurnaceResearchState(Optional.of(player), fuelSavingRemainder, cookingProgressRemainder,
                true, cookingCredit);
    }

    public FurnaceResearchState withFuelRemainder(double value) {
        return new FurnaceResearchState(attributedPlayer, value, cookingProgressRemainder,
                actualInteraction, cookingCredit);
    }

    public FurnaceResearchState withCooking(double remainder, int credit) {
        return new FurnaceResearchState(attributedPlayer, fuelSavingRemainder, remainder,
                actualInteraction, credit);
    }

    private static boolean validRemainder(double value) {
        return Double.isFinite(value) && value >= 0.0D && value < 1.0D;
    }
}
