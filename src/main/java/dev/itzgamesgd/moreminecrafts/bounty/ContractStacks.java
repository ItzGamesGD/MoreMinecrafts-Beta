package dev.itzgamesgd.moreminecrafts.bounty;

import dev.itzgamesgd.moreminecrafts.registry.ModDataComponents;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import net.minecraft.world.item.ItemStack;

public final class ContractStacks {
    private ContractStacks() {}

    public static ItemStack create(ContractData data) {
        ItemStack stack = new ItemStack(data.tier() == ContractData.Tier.SPECIAL
                ? ModItems.SPECIAL_CONTRACT : ModItems.BOUNTY_CONTRACT);
        stack.set(ModDataComponents.CONTRACT, data);
        return stack;
    }

    public static ContractData read(ItemStack stack) {
        return stack.getItem() instanceof ContractItem ? stack.get(ModDataComponents.CONTRACT) : null;
    }
}
