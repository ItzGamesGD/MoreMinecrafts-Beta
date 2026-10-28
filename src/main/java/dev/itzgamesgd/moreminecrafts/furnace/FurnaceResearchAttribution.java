package dev.itzgamesgd.moreminecrafts.furnace;

import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

/** Server-only event bridge for furnace GUI opens and committed inventory changes. */
public final class FurnaceResearchAttribution {
    private FurnaceResearchAttribution() {}

    public static void opened(AbstractFurnaceBlockEntity furnace, ServerPlayer player) {
        setAttributedPlayer(furnace, openedState(stateOf(furnace), player.getUUID()));
    }

    public static void interacted(AbstractFurnaceBlockEntity furnace, ServerPlayer player) {
        setAttributedPlayer(furnace, interactedState(stateOf(furnace), player.getUUID()));
    }

    public static FurnaceResearchState openedState(FurnaceResearchState state, UUID player) {
        return state.actualInteraction() ? state : state.attributedTo(player);
    }

    public static FurnaceResearchState interactedState(FurnaceResearchState state, UUID player) {
        return state.interactedBy(player);
    }

    private static FurnaceResearchState stateOf(AbstractFurnaceBlockEntity furnace) {
        return furnace instanceof FurnaceResearchStateAccess stateAccess
                ? stateAccess.moreminecrafts$getResearchState() : FurnaceResearchState.empty();
    }

    private static void setAttributedPlayer(AbstractFurnaceBlockEntity furnace, FurnaceResearchState state) {
        if (!(furnace instanceof FurnaceResearchStateAccess stateAccess)) return;
        FurnaceResearchState before = stateAccess.moreminecrafts$getResearchState();
        if (!state.equals(before)) stateAccess.moreminecrafts$setResearchState(state);
    }

    public static List<net.minecraft.world.item.ItemStack> snapshot(
            AbstractFurnaceBlockEntity furnace) {
        return List.of(furnace.getItem(0).copy(), furnace.getItem(1).copy(), furnace.getItem(2).copy());
    }

    public static boolean inventoryChanged(AbstractFurnaceBlockEntity furnace,
                                           List<net.minecraft.world.item.ItemStack> before) {
        if (before.size() != 3) return false;
        for (int slot = 0; slot < 3; slot++)
            if (!net.minecraft.world.item.ItemStack.matches(before.get(slot), furnace.getItem(slot))) return true;
        return false;
    }
}
