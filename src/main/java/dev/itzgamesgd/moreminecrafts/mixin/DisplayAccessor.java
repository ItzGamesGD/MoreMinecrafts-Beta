package dev.itzgamesgd.moreminecrafts.mixin;

import com.mojang.math.Transformation;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Server-side setters for the Tomb Guardian's temporary display-entity telegraphs. */
@Mixin(Display.class)
public interface DisplayAccessor {
    @Invoker("setTransformation")
    void moreminecrafts$setTransformation(Transformation transformation);

    @Invoker("setTransformationInterpolationDuration")
    void moreminecrafts$setTransformationInterpolationDuration(int ticks);

    @Invoker("setTransformationInterpolationDelay")
    void moreminecrafts$setTransformationInterpolationDelay(int ticks);

    @Invoker("setBrightnessOverride")
    void moreminecrafts$setBrightnessOverride(Brightness brightness);

    @Invoker("setViewRange")
    void moreminecrafts$setViewRange(float range);
}
