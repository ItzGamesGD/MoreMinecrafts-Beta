package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.equipment.PortableFurnaceMenu;
import dev.itzgamesgd.moreminecrafts.furnace.FurnaceResearchAttribution;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Credits only committed player clicks which change a furnace input, fuel, or result slot. */
@Mixin(AbstractContainerMenu.class)
abstract class FurnaceMenuInteractionMixin {
    @Unique private AbstractFurnaceBlockEntity moreminecrafts$clickedFurnace;
    @Unique private List<ItemStack> moreminecrafts$furnaceBefore = List.of();
    @Unique private PortableFurnaceMenu moreminecrafts$clickedPortableFurnace;
    @Unique private List<ItemStack> moreminecrafts$portableBefore = List.of();

    @Inject(method = "clicked", at = @At("HEAD"))
    private void moreminecrafts$captureFurnaceInventory(int slotId, int button, ContainerInput clickType,
                                                        Player player, CallbackInfo ci) {
        moreminecrafts$clickedFurnace = null;
        moreminecrafts$furnaceBefore = List.of();
        moreminecrafts$clickedPortableFurnace = null;
        moreminecrafts$portableBefore = List.of();
        if (!(player instanceof ServerPlayer) || !((Object) this instanceof FurnaceMenu menu)
                || menu.slots.isEmpty()) return;
        Container container = menu.slots.getFirst().container;
        if (container instanceof AbstractFurnaceBlockEntity furnace) {
            moreminecrafts$clickedFurnace = furnace;
            moreminecrafts$furnaceBefore = FurnaceResearchAttribution.snapshot(furnace);
        } else if ((Object) this instanceof PortableFurnaceMenu portable) {
            moreminecrafts$clickedPortableFurnace = portable;
            moreminecrafts$portableBefore = portable.researchInventorySnapshot();
        }
    }

    @Inject(method = "clicked", at = @At("TAIL"))
    private void moreminecrafts$attributeCommittedFurnaceInventoryChange(int slotId, int button,
            ContainerInput clickType, Player player, CallbackInfo ci) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        AbstractFurnaceBlockEntity furnace = moreminecrafts$clickedFurnace;
        if (furnace != null && FurnaceResearchAttribution.inventoryChanged(furnace,
                moreminecrafts$furnaceBefore))
            FurnaceResearchAttribution.interacted(furnace, serverPlayer);
        PortableFurnaceMenu portable = moreminecrafts$clickedPortableFurnace;
        if (portable != null && portable.researchInventoryChanged(moreminecrafts$portableBefore))
            portable.recordResearchInteraction(serverPlayer);
    }
}
