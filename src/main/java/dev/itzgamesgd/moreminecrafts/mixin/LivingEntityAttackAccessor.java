package dev.itzgamesgd.moreminecrafts.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes LivingEntity.attackStrengthTicker for V4 weapon_swap_handling. */
@Mixin(LivingEntity.class)
public interface LivingEntityAttackAccessor {
    @Accessor("attackStrengthTicker")
    int moreminecrafts$getAttackStrengthTicker();

    @Accessor("attackStrengthTicker")
    void moreminecrafts$setAttackStrengthTicker(int ticker);
}
