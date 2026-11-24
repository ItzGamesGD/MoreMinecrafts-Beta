package dev.itzgamesgd.moreminecrafts.trade;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.ToIntFunction;

/**
 * Data-driven gate for the structured Piglin trade. No unlock prerequisite is approved yet,
 * so the shipped default is fail-closed: with no {@code required_research} nobody is unlocked.
 */
public record PiglinTradeConfig(Optional<String> requiredResearch) {
    public static final PiglinTradeConfig CLOSED = new PiglinTradeConfig(Optional.empty());
    public static final Codec<PiglinTradeConfig> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("required_research").forGetter(PiglinTradeConfig::requiredResearch)
    ).apply(i, PiglinTradeConfig::new));

    /** True only when a research id is configured and the player's level for it is at least 1. */
    public boolean unlocked(ToIntFunction<String> researchLevel) {
        return requiredResearch.filter(id -> !id.isBlank())
                .map(id -> researchLevel.applyAsInt(id) >= 1).orElse(false);
    }
}
