package dev.itzgamesgd.moreminecrafts.bounty;

import net.minecraft.world.item.Item;

/** Physical unstackable contract; its data component travels with the stack on transfer and death. */
public final class ContractItem extends Item {
    private final boolean special;

    public ContractItem(boolean special, Properties properties) {
        super(properties);
        this.special = special;
    }

    public boolean isSpecial() { return special; }
}
