package dev.itzgamesgd.moreminecrafts.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.itzgamesgd.moreminecrafts.progression.ResearchConsecutiveSweep;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Player.class)
abstract class ResearchConsecutiveSweepMixin {
    @WrapOperation(method = "attack", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;hurtOrSimulate(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean moreminecrafts$recordSuccessfulSwordHit(Entity target, DamageSource source,
            float damage, Operation<Boolean> vanilla) {
        boolean hit = vanilla.call(target, source, damage);
        if (hit && (Object) this instanceof ServerPlayer player)
            ResearchConsecutiveSweep.recordHit(player, target);
        return hit;
    }

    @ModifyArg(method = "doSweepAttack", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"),
            index = 2)
    private float moreminecrafts$multiplyOnlySweepDamage(float vanilla) {
        return (Object) this instanceof ServerPlayer player
                ? ResearchConsecutiveSweep.sweepDamage(player, vanilla) : vanilla;
    }
}
