package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.progression.XpQuestHooks;
import net.minecraft.advancements.triggers.TameAnimalTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vanilla fires TAME_ANIMAL from TamableAnimal and AbstractHorse taming; foxes never fire it. */
@Mixin(TameAnimalTrigger.class)
abstract class QuestTameAnimalTriggerMixin {
    @Inject(method = "trigger(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/entity/animal/Animal;)V",
            at = @At("HEAD"))
    private void moreminecrafts$recordTaming(ServerPlayer player, Animal animal, CallbackInfo ci) {
        XpQuestHooks.tamed(player, animal);
    }
}
