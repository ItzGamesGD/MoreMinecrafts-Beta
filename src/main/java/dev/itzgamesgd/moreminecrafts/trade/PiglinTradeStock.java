package dev.itzgamesgd.moreminecrafts.trade;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * World-wide stock shared by every Piglin. Rule (villager-style): once per {@link #PERIOD_TICKS} of game
 * time, offers that are SOLD OUT are restored to full. Offers that still have stock are left untouched,
 * because their restock rule has not been approved. Evaluated lazily when a trade screen opens, which
 * gives the same result as a timer without any tick work.
 */
public final class PiglinTradeStock extends SavedData {
    /** 5 in-game days. */
    public static final long PERIOD_TICKS = 5L * 24_000L;

    private static final Codec<PiglinTradeStock> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("uses", Map.of())
                    .forGetter(s -> Map.copyOf(s.uses)),
            Codec.LONG.optionalFieldOf("cycle", -1L).forGetter(s -> s.cycle)
    ).apply(i, (uses, cycle) -> {
        PiglinTradeStock s = new PiglinTradeStock();
        s.uses.putAll(uses);
        s.cycle = cycle;
        return s;
    }));

    static final SavedDataType<PiglinTradeStock> TYPE = new SavedDataType<>(
            Identifier.parse("moreminecrafts:piglin_trade_stock"), PiglinTradeStock::new, CODEC, DataFixTypes.LEVEL);

    private final Map<String, Integer> uses = new HashMap<>();
    private long cycle = -1L;

    /** Restock cycle index for a game time. */
    public static long cycleOf(long gameTime) {
        return Math.floorDiv(gameTime, PERIOD_TICKS);
    }

    /**
     * Restores sold-out offers when a new cycle has begun. {@code maxUses} lists every catalog offer by id.
     * Returns true if anything changed.
     */
    public boolean restockIfDue(long gameTime, Map<String, Integer> maxUses) {
        long current = cycleOf(gameTime);
        if (current == cycle) return false;
        cycle = current;
        boolean changed = false;
        for (Map.Entry<String, Integer> max : maxUses.entrySet()) {
            Integer used = uses.get(max.getKey());
            if (used != null && used >= max.getValue() && used != 0) {
                uses.put(max.getKey(), 0);
                changed = true;
            }
        }
        setDirty();
        return changed;
    }

    /** Test hook: forget all recorded uses. */
    public void clearForTest() {
        uses.clear();
        cycle = -1L;
        setDirty();
    }

    public int usesOf(String offerId) {
        return Math.max(0, uses.getOrDefault(offerId, 0));
    }

    public void recordUse(String offerId, int totalUses) {
        uses.put(offerId, totalUses);
        setDirty();
    }

    /** Shared instance for the overworld; created on first access. */
    public static PiglinTradeStock get(net.minecraft.server.level.ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(TYPE);
    }
}
