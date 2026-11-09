package dev.itzgamesgd.moreminecrafts.k5probe.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Test-only observation of the action delivered by the real client screen. */
@Mixin(AbstractContainerMenu.class)
abstract class AccessoryMenuInputProbeMixin {
    @Inject(method = "clicked", at = @At("HEAD"))
    private void moreminecrafts$traceAccessoryInput(int slot, int button, ContainerInput input,
                                                   Player player, CallbackInfo ci) {
        if ((Object) this instanceof InventoryMenu && (slot == 11 || slot == 48)) {
            System.out.println("ACCESSORY_MENU_INPUT side="
                    + (player instanceof ServerPlayer ? "server" : "client")
                    + " slot=" + slot + " button=" + button + " action=" + input);
        }
    }
}
