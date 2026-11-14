package dev.itzgamesgd.moreminecrafts.mixin;

import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** ST-01 P8-D6: whether a merchant's offers are still ungenerated, without calling getOffers (which would generate them). */
@Mixin(AbstractVillager.class)
public interface St01MerchantOffersAccessor {
    @Accessor("offers")
    MerchantOffers moreminecrafts$offers();
}
