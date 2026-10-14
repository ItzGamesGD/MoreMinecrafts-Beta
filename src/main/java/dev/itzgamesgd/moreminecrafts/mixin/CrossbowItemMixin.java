package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.enchantment.CrossbowBarrageRuntime;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CrossbowItem.class)
abstract class CrossbowItemMixin {
    @Inject(method = "performShooting", at = @At("TAIL"))
    private void moreminecrafts$queueBarrage(Level level, LivingEntity shooter, InteractionHand hand,
            ItemStack crossbow, float power, float divergence, LivingEntity target, CallbackInfo ci) {
        CrossbowBarrageRuntime.afterShot(level, shooter, hand, crossbow);
    }
}
