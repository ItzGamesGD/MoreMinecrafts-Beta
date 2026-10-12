package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.progression.BrewingOutputProvenance;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Invalidates brewing evidence at the shared default container mutation hook. */
@Mixin(Container.class)
interface KnowledgeBrewingContainerMixin {
    @Inject(method = "setItem", at = @At("HEAD"))
    default void moreminecrafts$discardReplacedBrewOutput(int slot, ItemStack stack, CallbackInfo ci) {
        if ((Object) this instanceof BrewingStandBlockEntity stand && slot >= 0 && slot <= 2)
            BrewingOutputProvenance.discard(stand, slot);
    }
}
