package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.registry.ModDataComponents;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
abstract class BackpackDetachMixin {
    @Shadow public NonNullList<Slot> slots;
    @Shadow public abstract ItemStack getCarried();
    @Shadow public abstract void setCarried(ItemStack stack);

    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void moreminecrafts$detachBackpack(int slotId, int button, ContainerInput input, Player player,
                                               CallbackInfo ci) {
        if (input != ContainerInput.PICKUP || button != 1 || !getCarried().isEmpty()
                || slotId < 0 || slotId >= slots.size()) return;
        Slot slot = slots.get(slotId);
        ItemStack armor = slot.getItem();
        ItemStack attached = armor.get(ModDataComponents.ATTACHED_BACKPACK);
        if (attached == null || attached.isEmpty()) return;
        setCarried(attached.copy());
        armor.remove(ModDataComponents.ATTACHED_BACKPACK);
        slot.setChanged();
        ci.cancel();
    }
}
