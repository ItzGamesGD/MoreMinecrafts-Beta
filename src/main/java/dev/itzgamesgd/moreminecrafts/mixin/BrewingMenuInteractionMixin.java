package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.progression.BrewingResearchRuntime;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Credits committed player clicks that change a brewing stand's bottle, ingredient, or fuel slots. */
@Mixin(AbstractContainerMenu.class)
abstract class BrewingMenuInteractionMixin {
    @Unique private BrewingStandBlockEntity moreminecrafts$clickedStand;
    @Unique private List<ItemStack> moreminecrafts$standBefore = List.of();

    @Inject(method = "clicked", at = @At("HEAD"))
    private void moreminecrafts$captureBrewStandInventory(int slotId, int button, ContainerInput clickType,
                                                          Player player, CallbackInfo ci) {
        moreminecrafts$clickedStand = null;
        moreminecrafts$standBefore = List.of();
        if (!(player instanceof ServerPlayer) || !((Object) this instanceof BrewingStandMenu menu)
                || menu.slots.isEmpty()) return;
        Container container = menu.slots.getFirst().container;
        if (container instanceof BrewingStandBlockEntity stand) {
            moreminecrafts$clickedStand = stand;
            moreminecrafts$standBefore = BrewingResearchRuntime.snapshot(stand);
        }
    }

    @Inject(method = "clicked", at = @At("TAIL"))
    private void moreminecrafts$attributeCommittedBrewStandChange(int slotId, int button,
            ContainerInput clickType, Player player, CallbackInfo ci) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        BrewingStandBlockEntity stand = moreminecrafts$clickedStand;
        if (stand != null && BrewingResearchRuntime.inventoryChanged(stand, moreminecrafts$standBefore)) {
            BrewingResearchRuntime.interacted(stand, serverPlayer);
        }
    }
}
