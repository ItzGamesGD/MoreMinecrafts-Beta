package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.progression.QuestWitherSkullProvenance;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
abstract class QuestWitherSkullMergeMixin {
    @Inject(method = "tryToMerge", at = @At("HEAD"), cancellable = true)
    private void moreminecrafts$keepProvenanceSeparate(ItemEntity other, CallbackInfo ci) {
        if (!QuestWitherSkullProvenance.mayMerge((ItemEntity) (Object) this, other)) ci.cancel();
    }
}
