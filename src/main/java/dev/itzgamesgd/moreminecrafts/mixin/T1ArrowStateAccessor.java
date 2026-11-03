package dev.itzgamesgd.moreminecrafts.mixin;

import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractArrow.class)
public interface T1ArrowStateAccessor {
    @Invoker("isInGround") boolean moreminecrafts$isInGround();
}
