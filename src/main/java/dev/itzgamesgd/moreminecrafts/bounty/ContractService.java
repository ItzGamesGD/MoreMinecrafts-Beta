package dev.itzgamesgd.moreminecrafts.bounty;

import dev.itzgamesgd.moreminecrafts.registry.ModDataComponents;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** All mutating operations run on the server thread and locate the live stack by ID. */
public final class ContractService {
    public enum Action { ACCEPT, COMPLETE, ABANDON }
    private ContractService() {}

    /**
     * Acts on exactly the item in {@code slot}, and only if that slot still holds the contract with {@code id}. A stale
     * client therefore cannot touch a different copy of the same ID that moved to another slot.
     */
    public static boolean act(ServerPlayer player, int slot, String id, Action action) {
        if (id == null || id.length() > 40 || action == null) return false;
        Inventory inventory = player.getInventory();
        if (slot < 0 || slot >= inventory.getContainerSize()) return false;
        ItemStack stack = inventory.getItem(slot);
        ContractData data = ContractStacks.read(stack);
        if (data == null || !data.id().equals(id)) return false;
        return switch (action) {
            case ACCEPT -> {
                if (data.status() != ContractData.Status.OFFERED) yield false;
                stack.set(ModDataComponents.CONTRACT, data.accept());
                yield true;
            }
            case ABANDON -> {
                // Abandonment destroys only this item. Redeemed IDs are refused so a duplicate copy cannot be
                // destroyed as if it were still open; no reward is granted and no completion is recorded.
                if (ledger(player).blocked(id)) yield false;
                stack.shrink(1);
                yield true;
            }
            case COMPLETE -> complete(player, stack, data);
        };
    }

    private static ContractLedger ledger(ServerPlayer player) {
        return ContractLedger.of((ServerLevel) player.level());
    }

    private static boolean complete(ServerPlayer player, ItemStack stack, ContractData data) {
        if (data.status() != ContractData.Status.ACCEPTED) return false;
        Inventory inventory = player.getInventory();
        if (!data.ready(quantities(inventory))) return false;
        Item bonus = BuiltInRegistries.ITEM.getValue(Identifier.parse(data.reward().item()));
        if (bonus == Items.AIR) return false;
        // Pending and committed IDs both refuse: a copy cannot redeem while the original owner's transaction is unresolved.
        if (ledger(player).blocked(data.id())) return false;
        // The server thread serializes requests; durability across a crash is ContractTransactions' job.
        return ContractTransactions.redeem(player, stack, data, bonus);
    }

    public static Map<String, Integer> quantities(Inventory inventory) {
        Map<String, Integer> quantities = new HashMap<>();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty()) quantities.merge(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.getCount(), Integer::sum);
        }
        return quantities;
    }

    public static void advance(ServerPlayer player, ContractData.Category category, String target) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            ContractData data = ContractStacks.read(stack);
            if (data == null || data.status() != ContractData.Status.ACCEPTED) continue;
            ContractData next = data.advance(category, target);
            if (!next.equals(data)) stack.set(ModDataComponents.CONTRACT, next);
        }
    }

    public static void enterStructure(ServerPlayer player, String target, String instance) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            ContractData data = ContractStacks.read(stack);
            if (data == null || data.status() != ContractData.Status.ACCEPTED) continue;
            ContractData next = data.enterStructure(target, instance);
            if (!next.equals(data)) stack.set(ModDataComponents.CONTRACT, next);
        }
    }
}
