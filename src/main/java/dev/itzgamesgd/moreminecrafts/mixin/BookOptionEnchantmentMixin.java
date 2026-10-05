package dev.itzgamesgd.moreminecrafts.mixin;
import dev.itzgamesgd.moreminecrafts.progression.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.tags.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(EnchantmentHelper.class)
abstract class BookOptionEnchantmentMixin {
    @Inject(method="modifyDurabilityToRepairFromXp",at=@At("RETURN"),cancellable=true)
    private static void moreminecrafts$mending(ServerLevel level,ItemStack stack,int durability,CallbackInfoReturnable<Integer> cir){if(T1Operations.option(stack,"mending")>0)cir.setReturnValue(cir.getReturnValue()*2);}
    @Inject(method="modifyCrossbowChargingTime",at=@At("RETURN"),cancellable=true)
    private static void moreminecrafts$charge(ItemStack stack,LivingEntity holder,float time,CallbackInfoReturnable<Float> cir){cir.setReturnValue(Math.max(.05f,cir.getReturnValue()-.1f*T1Operations.option(stack,"quick_charge")));}
    @Inject(method="getPiercingCount",at=@At("RETURN"),cancellable=true)
    private static void moreminecrafts$piercing(ServerLevel level,ItemStack stack,ItemStack ammo,CallbackInfoReturnable<Integer> cir){cir.setReturnValue(cir.getReturnValue()+T1Operations.option(stack,"piercing"));}
    @Inject(method="modifyKnockback",at=@At("RETURN"),cancellable=true)
    private static void moreminecrafts$knockback(ServerLevel level,ItemStack stack,Entity target,DamageSource source,float base,CallbackInfoReturnable<Float> cir){cir.setReturnValue(cir.getReturnValue()+.5f*T1Operations.option(stack,"knockback"));}
    @Inject(method="modifyDamage",at=@At("RETURN"),cancellable=true)
    private static void moreminecrafts$damage(ServerLevel level,ItemStack stack,Entity target,DamageSource source,float base,CallbackInfoReturnable<Float> cir){
        float result=cir.getReturnValue();
        if(stack.is(Items.BOW)||EquipmentTreeResolver.resolve(stack)==EnchantmentTrees.Equipment.BOW){
            if(source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile)
                result+=base*.055f*T1Operations.option(stack,"power");
        }
        else {
            int tiers=T1Operations.option(stack,"sharpness");
            if(target.getType().builtInRegistryHolder().is(EntityTypeTags.SENSITIVE_TO_SMITE))tiers+=T1Operations.option(stack,"smite");
            if(target.getType().builtInRegistryHolder().is(EntityTypeTags.SENSITIVE_TO_BANE_OF_ARTHROPODS))tiers+=T1Operations.option(stack,"bane_of_arthropods");
            result+=(float)(tiers*T1Balance.DAMAGE_OPTION_PER_LEVEL);
        }cir.setReturnValue(result);
    }
    @Inject(method="processDurabilityChange",at=@At("RETURN"),cancellable=true)
    private static void moreminecrafts$unbreaking(ServerLevel level,ItemStack stack,int amount,CallbackInfoReturnable<Integer> cir){
        int tiers=T1Operations.option(stack,"unbreaking"),consumed=cir.getReturnValue();if(tiers==0||consumed<=0)return;
        int unbreaking=T1Operations.enchantments(stack).getOrDefault("minecraft:unbreaking",0);
        // 26.3 vanilla: armor saves 2L/(5L+5); other equipment saves L/(L+1).
        double vanillaConsume=stack.is(ItemTags.ARMOR_ENCHANTABLE)?1-2.0*unbreaking/(5.0*unbreaking+5):1.0/(unbreaking+1);
        double extraSave=Math.min(1,.05*tiers/vanillaConsume);
        int saved=0;for(int i=0;i<consumed;i++)if(level.getRandom().nextDouble()<extraSave)saved++;
        cir.setReturnValue(consumed-saved);
    }
}
