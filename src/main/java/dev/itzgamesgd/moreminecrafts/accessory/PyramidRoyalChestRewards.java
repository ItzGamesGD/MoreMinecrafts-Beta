package dev.itzgamesgd.moreminecrafts.accessory;

import java.util.List;
import java.util.Optional;
import java.util.function.DoubleSupplier;
import java.util.function.IntUnaryOperator;

/** Isolated I05 pure draw; the custom royal chest runtime owns generation and persistence. */
public final class PyramidRoyalChestRewards {
    public static final double CHANCE = 0.20D;
    public static final List<AccessoryDefinition> ITEMS = List.of(AccessoryDefinition.SEED_OF_LIFE, AccessoryDefinition.RING_OF_TIME);
    private PyramidRoyalChestRewards() {}

    /** One chance roll, then (on success only) one uniform pick among the two equal-weight items. */
    public static Optional<AccessoryDefinition> draw(DoubleSupplier chanceRoll, IntUnaryOperator uniformPick) {
        if (!(chanceRoll.getAsDouble() < CHANCE)) return Optional.empty();
        int index = uniformPick.applyAsInt(ITEMS.size());
        return index >= 0 && index < ITEMS.size() ? Optional.of(ITEMS.get(index)) : Optional.empty();
    }


}
