package dev.itzgamesgd.moreminecrafts.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.itzgamesgd.moreminecrafts.progression.QuestRaidVictory;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.raid.Raid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Observes the vanilla per-hero raid-win statistic award that follows a raid's VICTORY transition. */
@Mixin(Raid.class)
abstract class QuestRaidVictoryMixin {
    @WrapOperation(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;awardStat(Lnet/minecraft/resources/Identifier;)V"))
    private void moreminecrafts$recordVictoryHero(ServerPlayer hero, Identifier stat, Operation<Void> original) {
        original.call(hero, stat);
        if (stat.equals(Stats.RAID_WIN)) QuestRaidVictory.heroRewarded(hero, ((Raid) (Object) this).getRaidOmenLevel());
    }
}
