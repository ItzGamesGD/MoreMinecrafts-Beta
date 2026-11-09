package dev.itzgamesgd.moreminecrafts.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.itzgamesgd.moreminecrafts.accessory.T1AccessoryRuntime;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
abstract class T1AccessoryDamageMixin {
    @ModifyExpressionValue(method = "actuallyHurt", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"))
    private float moreminecrafts$bufferFinalDamage(float amount) {
        return (Object) this instanceof ServerPlayer player ? T1AccessoryRuntime.bufferDamage(player, amount) : amount;
    }
    @WrapMethod(method = "hurtServer")
    private boolean moreminecrafts$afterActualDamage(ServerLevel level, DamageSource source, float amount, Operation<Boolean> original) {
        Player player = (Player) (Object) this;
        float before = player.getHealth() + player.getAbsorptionAmount();
        boolean accepted = original.call(level, source, amount);
        if (accepted && player instanceof ServerPlayer server && player.isAlive()
                && player.getHealth() + player.getAbsorptionAmount() < before)
            T1AccessoryRuntime.afterDamage(server);
        return accepted;
    }
}
