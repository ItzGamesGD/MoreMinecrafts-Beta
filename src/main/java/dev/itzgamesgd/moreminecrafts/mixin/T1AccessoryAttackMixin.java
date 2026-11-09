package dev.itzgamesgd.moreminecrafts.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.itzgamesgd.moreminecrafts.accessory.T1ComboRuntime;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
abstract class T1AccessoryAttackMixin {
    @Unique private T1ComboRuntime.Attack moreminecrafts$t1Attack;
    @WrapMethod(method = "attack")
    private void moreminecrafts$captureAttack(Entity target, Operation<Void> original) {
        if (!((Object) this instanceof ServerPlayer player)) { original.call(target); return; }
        var previous = moreminecrafts$t1Attack;
        moreminecrafts$t1Attack = T1ComboRuntime.capture(player);
        try { original.call(target); } finally { moreminecrafts$t1Attack = previous; }
    }
    @WrapOperation(method = "attack", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;hurtOrSimulate(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean moreminecrafts$combinedPrimaryHit(Entity target, DamageSource source, float damage, Operation<Boolean> original) {
        LivingEntity living = target instanceof LivingEntity body ? body
                : target instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragonPart part ? part.parentMob : null;
        if (!((Object) this instanceof ServerPlayer player) || living == null
                || moreminecrafts$t1Attack == null || !T1ComboRuntime.eligible(player, living, moreminecrafts$t1Attack))
            return original.call(target, source, damage);
        float extra = T1ComboRuntime.reachesHealth(living, source, damage)
                ? T1ComboRuntime.extraDamage(player, moreminecrafts$t1Attack) : 0;
        try (var observed = T1ComboRuntime.observe(living)) {
            boolean hit = original.call(target, source, damage + extra);
            if (hit && observed.lostHp()) T1ComboRuntime.successfulHit(player, extra > 0);
            return hit;
        }
    }
}
