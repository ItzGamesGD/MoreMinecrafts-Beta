package dev.itzgamesgd.moreminecrafts.mixin;

import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** ST-01 P8-D6: read-only access to a trade's given-item modifier, to find ST-01 explorer-map trades in the data. */
@Mixin(VillagerTrade.class)
public interface St01VillagerTradeAccessor {
    @Accessor("givenItemModifier")
    Optional<Holder<LootItemFunction>> moreminecrafts$givenItemModifier();
}
