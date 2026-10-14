package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.enchantment.ModEnchantments;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Extends the same predicate vanilla uses for mace impact/knockback, once at the primary victim. */
@Mixin(MaceItem.class)
abstract class ExplosiveMaceMixin {
    @Inject(method = "hurtEnemy", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/MaceItem;knockback(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;)V",
            shift = At.Shift.AFTER))
    private void moreminecrafts$explodeOnVanillaSmash(ItemStack weapon, LivingEntity victim,
            LivingEntity attacker, CallbackInfo ci) {
        if (attacker.level() instanceof ServerLevel level
                && ModEnchantments.level(level, weapon, ModEnchantments.EXPLOSIVE_MACE) > 0) {
            level.explode(attacker, victim.getX(), victim.getY(), victim.getZ(), 3.0F, Level.ExplosionInteraction.NONE);
        }
    }
}
