package dev.itzgamesgd.moreminecrafts.bounty;

import dev.itzgamesgd.moreminecrafts.registry.ModDataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Rolls pending trade and loot contracts into full contracts when they reach a player's inventory. Objectives and
 * rewards are therefore fixed at acquisition and never shown to a seller or in a container before pickup.
 */
public final class ContractMaterializer {
    private ContractMaterializer() {}

    public static void materialize(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            ContractData.Tier pending = stack.get(ModDataComponents.PENDING_CONTRACT);
            if (pending == null) continue;
            inventory.setItem(slot, ContractStacks.create(ContractGenerator.acquire(pending, player.getRandom())));
        }
    }
}
