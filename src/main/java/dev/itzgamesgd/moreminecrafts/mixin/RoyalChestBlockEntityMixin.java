package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.pyramid.RoyalChestRuntime;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import org.spongepowered.asm.mixin.Mixin;

/**
 * The real contents of a royal chest live in saved data, so its vanilla chest block entity must stay empty. Fabric's
 * Transfer API reaches block entities through {@code ItemStorage.SIDED} (its own hopper and dropper hooks do not use
 * the vanilla container lookup), so insertion is also refused at the block entity itself.
 */
@Mixin(ChestBlockEntity.class)
abstract class RoyalChestBlockEntityMixin {
    public boolean canPlaceItem(int slot, ItemStack stack) {
        ChestBlockEntity self = (ChestBlockEntity) (Object) this;
        return !RoyalChestRuntime.isGuarded(self.getLevel(), self.getBlockPos());
    }
}
