package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.progression.ResearchRidingRuntime;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Horse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractHorse.class)
abstract class ResearchHorseRidingMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void moreminecrafts$updateRiderBonuses(CallbackInfo ci) {
        if ((Object) this instanceof Horse horse) ResearchRidingRuntime.update(horse);
    }
}
