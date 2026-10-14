package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.progression.KnowledgeEventCollectors;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Output slots notify this hook only when a player actually takes the brewed potion. */
@Mixin(Slot.class)
abstract class KnowledgeBrewingOutputMixin {
    @Shadow @Final public net.minecraft.world.Container container;

    @Inject(method = "onTake", at = @At("TAIL"))
    private void moreminecrafts$recordPotionOutput(Player player, ItemStack taken, CallbackInfo ci) {
        Slot slot = (Slot) (Object) this;
        if (player instanceof ServerPlayer serverPlayer && container instanceof BrewingStandBlockEntity
                && slot.getContainerSlot() >= 0 && slot.getContainerSlot() <= 2
                && KnowledgeEventCollectors.isPotionItem(taken))
            KnowledgeEventCollectors.potionCollected(serverPlayer, (BrewingStandBlockEntity) container,
                    slot.getContainerSlot(), taken);
    }
}
