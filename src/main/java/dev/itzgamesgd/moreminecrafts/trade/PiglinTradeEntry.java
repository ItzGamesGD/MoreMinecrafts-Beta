package dev.itzgamesgd.moreminecrafts.trade;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.ToIntFunction;
import net.minecraft.world.item.trading.MerchantOffer;

/**
 * One datapack offer: a vanilla {@link MerchantOffer} plus an optional research that must be owned
 * (level &gt;= 1) before the offer is listed. Offers without {@code required_research} are listed to
 * every player who passed the structured-trade unlock in {@link PiglinTradeConfig}.
 */
public record PiglinTradeEntry(Optional<String> requiredResearch, MerchantOffer offer) {
    public static final Codec<PiglinTradeEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("required_research").forGetter(PiglinTradeEntry::requiredResearch),
            MerchantOffer.CODEC.fieldOf("offer").forGetter(PiglinTradeEntry::offer)
    ).apply(i, PiglinTradeEntry::new));

    public boolean listedFor(ToIntFunction<String> researchLevel) {
        return requiredResearch.filter(id -> !id.isBlank()).map(id -> researchLevel.applyAsInt(id) >= 1).orElse(true);
    }
}
