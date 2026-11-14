package dev.itzgamesgd.moreminecrafts.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.itzgamesgd.moreminecrafts.progression.QuestWitherSkullProvenance;
import dev.itzgamesgd.moreminecrafts.progression.QuestWitherSkullTransactions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemEntity.class)
abstract class QuestWitherSkullPickupMixin {
    /**
     * Executes genuine skull pickup through the P3 crash-safe transaction protocol.
     * Inventory.add may delete creative overflow; only actual inventory increase proves receipt.
     */
    @WrapOperation(method = "playerTouch", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Inventory;add(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean moreminecrafts$creditGenuineSkull(Inventory inventory, ItemStack stack,
            Operation<Boolean> original, @Local(argsOnly = true) Player player) {
        ItemEntity self = (ItemEntity) (Object) this;
        boolean genuine = QuestWitherSkullProvenance.isGenuine(self);
        if (!genuine || !(player instanceof ServerPlayer serverPlayer)) {
            return original.call(inventory, stack);
        }

        ItemStack identity = stack.copy();
        int offered = stack.getCount();

        final boolean[] added = new boolean[1];
        boolean transactionSucceeded = QuestWitherSkullTransactions.pickup(serverPlayer, self, identity, offered, () -> {
            int before = moreminecrafts$countMatching(inventory, identity);
            added[0] = original.call(inventory, stack);
            return Math.min(offered, Math.max(0, moreminecrafts$countMatching(inventory, identity) - before));
        });

        return transactionSucceeded || added[0];
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
