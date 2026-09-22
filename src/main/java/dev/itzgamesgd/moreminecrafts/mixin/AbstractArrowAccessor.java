package dev.itzgamesgd.moreminecrafts.mixin;

import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes the native arrow base-damage field without introducing parallel projectile state. */
@Mixin(AbstractArrow.class)
public interface AbstractArrowAccessor {
    @Accessor("baseDamage")
    double moreminecrafts$getBaseDamage();
}
