package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.pyramid.maptrade.St01MapTrade;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** ST-01 P8-D8: after vanilla generated a villager's trades for its level, a level-4 cartographer may add the ST-01 map trade. */
@Mixin(Villager.class)
abstract class St01VillagerMapTradeMixin {
    @Inject(method = "updateTrades", at = @At("TAIL"))
    private void moreminecrafts$st01MapTrade(ServerLevel level, CallbackInfo ci) {
        St01MapTrade.onTradesGenerated((Villager) (Object) this);
    }
}
