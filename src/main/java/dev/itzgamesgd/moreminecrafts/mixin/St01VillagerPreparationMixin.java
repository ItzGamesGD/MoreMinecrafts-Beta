package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01SearchPreparation;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ST-01 P8-D6 C6 triggers: a villager's profession or level changed, or a player opened its trading screen. Each only
 * enqueues background preparation (no search, no chunk access); trades are generated exactly as before.
 */
@Mixin(Villager.class)
abstract class St01VillagerPreparationMixin {
    @Inject(method = "setVillagerData", at = @At("TAIL"))
    private void moreminecrafts$st01PrepareOnVillagerData(VillagerData data, CallbackInfo ci) {
        St01SearchPreparation.onVillagerChanged((Villager) (Object) this);
    }

    @Inject(method = "setTradingPlayer", at = @At("TAIL"))
    private void moreminecrafts$st01PrepareOnTrading(Player player, CallbackInfo ci) {
        if (player != null) St01SearchPreparation.onVillagerChanged((Villager) (Object) this);
    }
}
