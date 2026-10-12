package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.progression.KnowledgeEventCollectors;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Credits only recipe outputs with matching completed-smelt provenance when a player takes them. */
@Mixin(Slot.class)
abstract class KnowledgeFurnaceOutputMixin {
    @Shadow @Final public net.minecraft.world.Container container;

    @Inject(method = "onTake", at = @At("TAIL"))
    private void moreminecrafts$recordCollectedOreOutput(Player player, ItemStack taken, CallbackInfo ci) {
        Slot slot = (Slot) (Object) this;
        if (player instanceof ServerPlayer serverPlayer && container instanceof AbstractFurnaceBlockEntity furnace
                && slot.getContainerSlot() == 2)
            KnowledgeEventCollectors.blastFurnaceProductTaken(serverPlayer, furnace, taken);
            KnowledgeEventCollectors.smeltingCollected(serverPlayer, furnace, taken);
    }
}
