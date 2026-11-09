package dev.itzgamesgd.moreminecrafts.bounty;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Physical unstackable contract; its data component travels with the stack on transfer and death. */
public final class ContractItem extends Item {
    private final boolean special;

    public ContractItem(boolean special, Properties properties) {
        super(properties);
        this.special = special;
    }

    public boolean isSpecial() { return special; }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            ContractData data = ContractStacks.read(player.getItemInHand(hand));
            if (data != null) ContractUiBridge.open(data.id());
        }
        return InteractionResult.SUCCESS;
    }
}
