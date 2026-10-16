package dev.itzgamesgd.moreminecrafts.equipment;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class PortableFurnaceItem extends Item {
    public PortableFurnaceItem(Properties properties) { super(properties); }

    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        var contents = new PortableFurnaceMenu.PortableContainer();
        ItemStack opener = player.getItemInHand(hand);
        var stateComponent = dev.itzgamesgd.moreminecrafts.registry.ModDataComponents.PORTABLE_FURNACE_RESEARCH_STATE;
        var state = opener.getOrDefault(stateComponent,
                dev.itzgamesgd.moreminecrafts.furnace.FurnaceResearchState.empty());
        opener.set(stateComponent, dev.itzgamesgd.moreminecrafts.furnace.FurnaceResearchAttribution
                .openedState(state, player.getUUID()));
        player.getInventory().setChanged();
        ((ServerPlayer) player).openMenu(new SimpleMenuProvider(
                (id, inventory, ignored) -> new PortableFurnaceMenu(id, inventory, contents, opener),
                Component.translatable("container.moreminecrafts.portable_furnace")));
        return InteractionResult.SUCCESS_SERVER;
    }
}
