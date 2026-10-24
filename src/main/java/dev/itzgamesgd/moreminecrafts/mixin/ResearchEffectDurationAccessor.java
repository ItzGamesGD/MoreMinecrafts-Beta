package dev.itzgamesgd.moreminecrafts.mixin;

import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MobEffectInstance.class)
public interface ResearchEffectDurationAccessor {
    @Accessor("hiddenEffect") MobEffectInstance moreminecrafts$getHiddenEffect();
    @Accessor("hiddenEffect") void moreminecrafts$setHiddenEffect(MobEffectInstance hidden);
    @Accessor("duration") void moreminecrafts$setDuration(int duration);
}
