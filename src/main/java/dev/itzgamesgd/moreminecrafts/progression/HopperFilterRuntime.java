package dev.itzgamesgd.moreminecrafts.progression;

import dev.itzgamesgd.moreminecrafts.network.HopperFilterPayload;
import dev.itzgamesgd.moreminecrafts.network.HopperFilterSyncPayload;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

/** Server runtime for Hopper Filter (`hopper_filter`). */
public final class HopperFilterRuntime {
    private HopperFilterRuntime() {}

    public static void onHopperOpened(HopperBlockEntity hopper, ServerPlayer player) {
        if (!(hopper instanceof HopperFilterAccess access)) return;
        var current = access.moreminecrafts$getResearchAttribution();
        var next = current.openedBy(player.getUUID());
        if (!next.equals(current)) access.moreminecrafts$setResearchAttribution(next);
        syncToPlayer(player, access.moreminecrafts$getFilterState());
    }

    public static boolean handleAction(ServerPlayer player, HopperFilterPayload payload) {
        if (player == null || ResearchRewards.level(player, "hopper_filter", "hopper_filter") <= 0) return false;
        if (!(player.containerMenu instanceof HopperMenu menu) || menu.slots.isEmpty()) return false;
        if (!(menu.slots.getFirst().container instanceof HopperBlockEntity hopper)
                || !(hopper instanceof HopperFilterAccess access)) return false;

        HopperFilterState state = access.moreminecrafts$getFilterState();
        List<HopperFilterState.Entry> entries = new ArrayList<>(state.entries());
        boolean whitelist = state.whitelist();

        switch (payload.action()) {
            case HopperFilterPayload.ACTION_TOGGLE_MODE -> whitelist = !whitelist;
            case HopperFilterPayload.ACTION_CLEAR -> entries.clear();
            case HopperFilterPayload.ACTION_REMOVE_SLOT -> {
                int idx = payload.slotIndex();
                if (idx >= 0 && idx < entries.size()) entries.remove(idx);
            }
            case HopperFilterPayload.ACTION_SET_OR_TOGGLE_SLOT -> {
                int idx = payload.slotIndex();
                ItemStack carried = menu.getCarried();
                if (!carried.isEmpty()) {
                    String itemId = String.valueOf(BuiltInRegistries.ITEM.getKey(carried.getItem()));
                    var entry = new HopperFilterState.Entry(itemId, false, carried.getComponents().hashCode());
                    if (idx >= 0 && idx < entries.size()) {
                        entries.set(idx, entry);
                    } else if (entries.size() < HopperFilterState.MAX_ENTRIES) {
                        entries.add(entry);
                    }
                } else if (idx >= 0 && idx < entries.size()) {
                    var existing = entries.get(idx);
                    entries.set(idx, new HopperFilterState.Entry(
                            existing.itemId(), !existing.matchComponents(), existing.componentHash()));
                }
            }
            default -> {
                return false;
            }
        }

        HopperFilterState updated = new HopperFilterState(whitelist, entries);
        access.moreminecrafts$setFilterState(updated);
        access.moreminecrafts$setResearchAttribution(
                access.moreminecrafts$getResearchAttribution().interactedBy(player.getUUID()));
        syncToPlayer(player, updated);
        return true;
    }

    private static void syncToPlayer(ServerPlayer player, HopperFilterState state) {
        if (ServerPlayNetworking.canSend(player, HopperFilterSyncPayload.TYPE)) {
            ServerPlayNetworking.send(player, new HopperFilterSyncPayload(state));
        }
    }
}
