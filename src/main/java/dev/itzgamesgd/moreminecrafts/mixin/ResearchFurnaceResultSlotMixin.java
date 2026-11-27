package dev.itzgamesgd.moreminecrafts.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.itzgamesgd.moreminecrafts.progression.ResearchXpOrbContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

/** Keeps the smelting XP attribution scope around vanilla's synchronous XP-orb payout. */
@Mixin(FurnaceResultSlot.class)
abstract class ResearchFurnaceResultSlotMixin {
    @WrapMethod(method = "onTake")
    private void moreminecrafts$scopeSmeltingXp(Player player, ItemStack taken, Operation<Void> original) {
        if (player instanceof ServerPlayer researcher) {
            try (var ignored = ResearchXpOrbContext.enter(researcher,
                    "production_root", "smelting_xp", "production_smelt")) {
                original.call(player, taken);
            }
            return;
        }
        original.call(player, taken);
    }
}
