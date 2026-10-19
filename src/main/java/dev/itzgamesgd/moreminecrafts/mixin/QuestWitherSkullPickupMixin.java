package dev.itzgamesgd.moreminecrafts.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.itzgamesgd.moreminecrafts.progression.QuestWitherSkullProvenance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemEntity.class)
abstract class QuestWitherSkullPickupMixin {
    /** Inventory.add may delete creative overflow; only a matching inventory increase proves receipt. */
    @WrapOperation(method = "playerTouch", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Inventory;add(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean moreminecrafts$creditGenuineSkull(Inventory inventory, ItemStack stack,
            Operation<Boolean> original, @Local(argsOnly = true) Player player) {
        ItemEntity self = (ItemEntity) (Object) this;
        boolean genuine = QuestWitherSkullProvenance.isGenuine(self);
        ItemStack identity = genuine ? stack.copy() : ItemStack.EMPTY;
        int offered = stack.getCount();
        int before = genuine ? moreminecrafts$countMatching(inventory, identity) : 0;
        boolean added = original.call(inventory, stack);
        if (genuine && player instanceof ServerPlayer serverPlayer) {
            int received = Math.min(offered, Math.max(0, moreminecrafts$countMatching(inventory, identity) - before));
            QuestWitherSkullProvenance.pickedUp(serverPlayer, self, received);
        }
        return added;
    }

    private static int moreminecrafts$countMatching(Inventory inventory, ItemStack identity) {
        int count = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack held = inventory.getItem(slot);
            if (ItemStack.isSameItemSameComponents(held, identity)) count += held.getCount();
        }
        return count;
    }
}
