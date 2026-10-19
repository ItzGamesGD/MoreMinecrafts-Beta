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
    /** Credit is the count that actually left the item entity into the inventory, so duplicates cannot recount. */
    @WrapOperation(method = "playerTouch", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Inventory;add(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean moreminecrafts$creditGenuineSkull(Inventory inventory, ItemStack stack,
            Operation<Boolean> original, @Local(argsOnly = true) Player player) {
        ItemEntity self = (ItemEntity) (Object) this;
        boolean genuine = QuestWitherSkullProvenance.isGenuine(self);
        int before = stack.getCount();
        boolean added = original.call(inventory, stack);
        if (genuine && player instanceof ServerPlayer serverPlayer)
            QuestWitherSkullProvenance.pickedUp(serverPlayer, self, before - stack.getCount());
        return added;
    }
}
