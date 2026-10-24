package dev.itzgamesgd.moreminecrafts.mixin;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.animal.wolf.WolfSoundVariant;
import net.minecraft.world.entity.animal.wolf.WolfVariant;
import net.minecraft.world.item.DyeColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Wolf.class)
public interface WolfMixinAccessors {
    @Invoker("getVariant")
    Holder<WolfVariant> moreminecrafts$invokeGetVariant();

    @Invoker("setVariant")
    void moreminecrafts$invokeSetVariant(Holder<WolfVariant> variant);

    @Invoker("setSoundVariant")
    void moreminecrafts$invokeSetSoundVariant(Holder<WolfSoundVariant> variant);

    @Invoker("setCollarColor")
    void moreminecrafts$invokeSetCollarColor(DyeColor color);
}
