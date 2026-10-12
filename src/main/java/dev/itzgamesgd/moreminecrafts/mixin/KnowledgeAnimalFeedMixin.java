package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.progression.KnowledgeRuntime;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Records a species only after a successful animal interaction consumes its breeding food. */
@Mixin(Animal.class)
abstract class KnowledgeAnimalFeedMixin {
    @Inject(method = "mobInteract", at = @At("RETURN"))
    private void moreminecrafts$recordSuccessfulFeed(Player player, InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> result) {
        if (!(player instanceof ServerPlayer serverPlayer) || player.level().isClientSide()
                || !result.getReturnValue().consumesAction()) return;
        ItemStack after = player.getItemInHand(hand);
        if (after.getCount() >= after.getCount() + 1) return;
        // A successful survival feed consumes one item. Creative interactions without consumption
        // are deliberately excluded from durable progression credit.
        Animal animal = (Animal) (Object) this;
        KnowledgeRuntime.recordDistinctCondition(serverPlayer, "animal_types_fed",
                String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(animal.getType())));
    }
}
