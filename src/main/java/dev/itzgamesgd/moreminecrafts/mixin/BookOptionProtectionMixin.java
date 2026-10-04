package dev.itzgamesgd.moreminecrafts.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.itzgamesgd.moreminecrafts.progression.*;
import net.minecraft.world.entity.*;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(LivingEntity.class)
abstract class BookOptionProtectionMixin {
    @WrapOperation(method="getDamageAfterMagicAbsorb",at=@At(value="INVOKE",target="Lnet/minecraft/world/damagesource/CombatRules;getDamageAfterMagicAbsorb(FF)F"))
    private float moreminecrafts$protection(float damage,float epf,Operation<Float> original,DamageSource source,float incoming){
        double bonus=0;LivingEntity entity=(LivingEntity)(Object)this;
        for(EquipmentSlot slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET}){
            var stack=entity.getItemBySlot(slot);int tiers=T1Operations.option(stack,"protection");
            if(source.is(DamageTypeTags.IS_FIRE))tiers+=T1Operations.option(stack,"fire_protection");
            if(source.is(DamageTypeTags.IS_PROJECTILE))tiers+=T1Operations.option(stack,"projectile_protection");
            if(source.is(DamageTypeTags.IS_EXPLOSION))tiers+=T1Operations.option(stack,"blast_protection");
            bonus+=.025*tiers;
        }
        return (float)BookOptionRules.protectedDamage(damage,original.call(damage,epf),bonus);
    }
}
