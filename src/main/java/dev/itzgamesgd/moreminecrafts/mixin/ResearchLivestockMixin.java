package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.progression.ResearchLivestockRuntime;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Extends only a completed vanilla Animal breeding operation. */
@Mixin(Animal.class)
abstract class ResearchLivestockMixin {
    @Inject(method = "spawnChildFromBreeding", at = @At("RETURN"))
    private void moreminecrafts$addResearchedOffspring(ServerLevel level, Animal partner, CallbackInfo ci) {
        ResearchLivestockRuntime.afterVanillaBreeding((Animal) (Object) this, level, partner);
    }
}
