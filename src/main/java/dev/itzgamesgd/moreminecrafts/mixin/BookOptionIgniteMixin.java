package dev.itzgamesgd.moreminecrafts.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.itzgamesgd.moreminecrafts.progression.T1Operations;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.item.enchantment.effects.Ignite;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(Ignite.class)
abstract class BookOptionIgniteMixin {
    @WrapOperation(method="apply",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;igniteForSeconds(F)V"))
    private void moreminecrafts$fire(Entity target,float seconds,Operation<Void> original,ServerLevel level,int enchantmentLevel,EnchantedItemInUse item,Entity entity,Vec3 pos){
        original.call(target,seconds+2*T1Operations.option(item.itemStack(),"fire_aspect"));
    }
}
