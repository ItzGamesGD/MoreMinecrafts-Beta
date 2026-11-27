package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.progression.ResearchBatch2Runtime;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FireworkRocketEntity.class)
abstract class ResearchFlightMixin {
    @Shadow private int lifetime;
    // Only the attached propulsion constructor; placed, crossbow and restored rockets are excluded.
    @Inject(method = "<init>(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)V",
            at = @At("RETURN"))
    private void moreminecrafts$extendPropulsion(Level world, ItemStack stack, LivingEntity boosted, CallbackInfo ci) {
        if (!world.isClientSide()) lifetime = ResearchBatch2Runtime.flightLifetime(boosted, lifetime);
    }
}
