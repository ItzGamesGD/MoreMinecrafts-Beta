package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.accessory.T1AccessoryRuntime;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
abstract class T1AccessoryEffectMixin {
    @org.spongepowered.asm.mixin.injection.Inject(method = "setHealth", at = @At("HEAD"))
    private void moreminecrafts$observeHpLoss(float health, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        dev.itzgamesgd.moreminecrafts.accessory.T1ComboRuntime.healthChanging((LivingEntity) (Object) this, health);
    }
    @ModifyVariable(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"), argsOnly = true)
    private MobEffectInstance moreminecrafts$t1Shorten(MobEffectInstance incoming) {
        return (Object) this instanceof ServerPlayer player ? T1AccessoryRuntime.shortenEffect(player, incoming) : incoming;
    }
}
