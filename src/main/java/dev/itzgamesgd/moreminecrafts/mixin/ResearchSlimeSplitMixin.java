package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.progression.ResearchRewards;
import dev.itzgamesgd.moreminecrafts.progression.ResearchSlimeSplitRuntime;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import net.minecraft.world.entity.monster.cubemob.Slime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Suppresses Slime splitting when killed by a researcher with `v3.research.41`. */
@Mixin(AbstractCubeMob.class)
abstract class ResearchSlimeSplitMixin {
    @Unique
    private boolean moreminecrafts$splitHandled;

    @Inject(method = "remove(Lnet/minecraft/world/entity/Entity$RemovalReason;)V", at = @At("HEAD"))
    private void moreminecrafts$preventSlimeSplit(Entity.RemovalReason reason, CallbackInfo ci) {
        if (moreminecrafts$splitHandled || reason != Entity.RemovalReason.KILLED) return;
        if (!((Object) this instanceof Slime slime)) return;
        if (!(slime.level() instanceof ServerLevel) || slime.getSize() <= 1 || !slime.isDeadOrDying()) return;
        if (!(slime.getKillCredit() instanceof ServerPlayer killer)) return;
        if (ResearchRewards.level(killer, "v3.research.41", "slime_split_prevention") <= 0) return;
        moreminecrafts$splitHandled = true;
        ResearchSlimeSplitRuntime.handleSplitPrevention(slime, killer);
    }
}
