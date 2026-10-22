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

    public static boolean act(ServerPlayer player, String id, Action action) {
        if (id == null || id.length() > 40 || action == null) return false;
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            ContractData data = ContractStacks.read(stack);
            if (data == null || !data.id().equals(id)) continue;
            return switch (action) {
                case ACCEPT -> {
                    if (data.status() != ContractData.Status.OFFERED) yield false;
                    stack.set(ModDataComponents.CONTRACT, data.accept());
                    yield true;
                }
                case ABANDON -> {
                    if (data.status() != ContractData.Status.ACCEPTED) yield false;
                    stack.shrink(1);
                    yield true;
                }
                case COMPLETE -> complete(player, stack, data);
            };
        }
        return false;
    }

    private static boolean complete(ServerPlayer player, ItemStack stack, ContractData data) {
        if (data.status() != ContractData.Status.ACCEPTED) return false;
        Inventory inventory = player.getInventory();
        Map<String, Integer> available = quantities(inventory);
        if (!data.ready(available)) return false;
        Item bonus = BuiltInRegistries.ITEM.getValue(Identifier.parse(data.reward().item()));
        if (bonus == Items.AIR) return false;
        ServerLevel world = ((ServerLevel) player.level()).getServer().overworld();
        ContractLedger ledger = world.getDataStorage().computeIfAbsent(ContractLedger.TYPE);
        if (ledger.contains(data.id())) return false;

        // Server-thread serialization prevents a second request from passing this claim.
        // World and player saves are separate; a process crash between these operations is PARTIAL durability.
        if (!ledger.claim(data.id())) return false;
        for (var need : data.deliveryNeeds().entrySet()) {
            int remaining = need.getValue();
            for (int slot = 0; slot < inventory.getContainerSize() && remaining > 0; slot++) {
                ItemStack material = inventory.getItem(slot);
                if (material.isEmpty() || !BuiltInRegistries.ITEM.getKey(material.getItem()).toString().equals(need.getKey())) continue;
                int taken = Math.min(remaining, material.getCount());
                material.shrink(taken);
                remaining -= taken;
            }
        }
        stack.shrink(1);
        deliver(player, new ItemStack(Items.EMERALD, data.reward().emeralds()));
        deliver(player, new ItemStack(bonus, data.reward().count()));
        inventory.setChanged();
        return true;
    }

    private static void deliver(ServerPlayer player, ItemStack reward) {
        if (reward.isEmpty()) return;
        player.getInventory().add(reward);
        if (!reward.isEmpty()) player.drop(reward, false, net.minecraft.util.Prediction.SERVER_ONLY);
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
}
