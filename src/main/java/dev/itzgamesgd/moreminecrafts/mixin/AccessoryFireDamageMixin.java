package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.accessory.AccessoryAbilities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
abstract class AccessoryFireDamageMixin {
    @ModifyVariable(method = "actuallyHurt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float moreminecrafts$emberReduction(float amount, ServerLevel level, DamageSource source) {
        return (Object) this instanceof ServerPlayer player
                ? AccessoryAbilities.reduceFireDamage(player, source, amount) : amount;
    }

    @Inject(method = "hurtServer", at = @At("RETURN"))
    private void moreminecrafts$emberTrigger(ServerLevel level, DamageSource source, float amount,
                                              CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() && (Object) this instanceof ServerPlayer player)
            AccessoryAbilities.afterDamage(player, source);
    }
}
