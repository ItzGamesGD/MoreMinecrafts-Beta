package dev.itzgamesgd.moreminecrafts.furnace;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.UUID;

/** Persistent attribution and fixed-point carry state reserved for future furnace research effects. */
public record FurnaceResearchState(Optional<UUID> attributedPlayer,
                                   double fuelSavingRemainder,
                                   double cookingProgressRemainder) {
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
                    .forGetter(FurnaceResearchState::cookingProgressRemainder)
    ).apply(instance, FurnaceResearchState::new));

    public FurnaceResearchState {
        attributedPlayer = attributedPlayer == null ? Optional.empty() : attributedPlayer;
        if (!validRemainder(fuelSavingRemainder) || !validRemainder(cookingProgressRemainder))
            throw new IllegalArgumentException("Furnace remainder must be finite and in [0, 1)");
    }

    public static FurnaceResearchState empty() {
        return new FurnaceResearchState(Optional.empty(), 0.0D, 0.0D);
    }

    public FurnaceResearchState attributedTo(UUID player) {
        return new FurnaceResearchState(Optional.of(player), fuelSavingRemainder, cookingProgressRemainder);
    }

    private static boolean validRemainder(double value) {
        return Double.isFinite(value) && value >= 0.0D && value < 1.0D;
    }
}
