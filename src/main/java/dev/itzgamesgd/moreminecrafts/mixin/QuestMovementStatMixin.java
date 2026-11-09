package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.progression.XpQuestHooks;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Mirrors vanilla movement statistics awarded in the Nether into a per-dimension quest counter. */
@Mixin(ServerPlayer.class)
abstract class QuestMovementStatMixin {
    @Inject(method = "awardStat(Lnet/minecraft/stats/Stat;I)V", at = @At("HEAD"))
    private void moreminecrafts$netherMovement(Stat<?> stat, int amount, CallbackInfo ci) {
        if (stat.getType() == Stats.CUSTOM && stat.getValue() instanceof Identifier id && id.getPath().endsWith("_one_cm")) {
            XpQuestHooks.netherMovement((ServerPlayer) (Object) this, amount);
        }
    }
}
