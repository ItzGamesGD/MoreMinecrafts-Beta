package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.enchantment.EnchantmentRuntime;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
abstract class ItemEntityMixin {
    @Inject(method = "<init>(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/item/ItemStack;DDD)V", at = @At("RETURN"))
    private void moreminecrafts$autoSmeltBlockDrop(Level level, double x, double y, double z, ItemStack stack,
            double velocityX, double velocityY, double velocityZ, CallbackInfo callbackInfo) {
        ((ItemEntity) (Object) this).setItem(EnchantmentRuntime.autoSmeltDrop(level, stack));
    }
}
