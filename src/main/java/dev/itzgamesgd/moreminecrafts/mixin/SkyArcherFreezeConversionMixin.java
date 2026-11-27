package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.mob.sky.SkyArcher;
import net.minecraft.world.entity.ConversionTracker;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Group B Sky Archer: skip the powder-snow Stray conversion tracker; vanilla Skeletons are untouched. */
@Mixin(Skeleton.class)
public abstract class SkyArcherFreezeConversionMixin {
    @Redirect(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/ConversionTracker;tick()V"))
    private void moreminecrafts$skipSkyArcherStrayConversion(ConversionTracker<?> tracker) {
        if (!((Object) this instanceof SkyArcher)) tracker.tick();
    }
}
