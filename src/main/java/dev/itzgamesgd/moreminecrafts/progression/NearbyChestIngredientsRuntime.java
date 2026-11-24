package dev.itzgamesgd.moreminecrafts.progression;

import dev.itzgamesgd.moreminecrafts.network.NearbyChestPullPayload;
import dev.itzgamesgd.moreminecrafts.network.NearbyChestSyncPayload;
import dev.itzgamesgd.moreminecrafts.pyramid.RoyalChestRuntime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Server runtime for V4 `nearby_chest_ingredients`: scans & pulls from nearby unlocked containers. */
public final class NearbyChestIngredientsRuntime {
    private static final int RADIUS_XZ = 4;
    private static final int RADIUS_Y = 2;

    private NearbyChestIngredientsRuntime() {}

    public static List<NearbyChestSyncPayload.Entry> summarize(ServerLevel level, BlockPos center) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Container container : findContainers(level, center)) {
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                ItemStack stack = container.getItem(slot);
                if (stack.isEmpty()) continue;
                String id = String.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem()));
                counts.merge(id, stack.getCount(), Integer::sum);
            }
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .limit(12)
                .map(e -> new NearbyChestSyncPayload.Entry(e.getKey(), e.getValue()))
                .toList();
    }

    public static void syncForPlayer(ServerPlayer player, BlockPos center) {
        if (player == null || ResearchRewards.level(player, "nearby_chest_ingredients", "chest_ingredient_reference") <= 0) {
            return;
        }
        if (ServerPlayNetworking.canSend(player, NearbyChestSyncPayload.TYPE)) {
            ServerPlayNetworking.send(player, new NearbyChestSyncPayload(summarize(player.level(), center)));
        }
    }

    public static int pullItem(ServerPlayer player, BlockPos center, NearbyChestPullPayload payload) {
        if (player == null || payload == null
                || ResearchRewards.level(player, "nearby_chest_ingredients", "chest_ingredient_reference") <= 0) {
            return 0;
        }
        Identifier id = Identifier.tryParse(payload.itemId());
        if (id == null) return 0;
        Item target = BuiltInRegistries.ITEM.getValue(id);
        if (target == null || target == Items.AIR) return 0;

        int remaining = Math.max(1, Math.min(64, payload.count()));
        int movedTotal = 0;
        for (Container container : findContainers(player.level(), center)) {
            for (int slot = 0; slot < container.getContainerSize() && remaining > 0; slot++) {
                ItemStack stack = container.getItem(slot);
                if (stack.isEmpty() || stack.getItem() != target) continue;
                int take = Math.min(remaining, stack.getCount());
                ItemStack extracted = stack.copyWithCount(take);
                if (player.getInventory().add(extracted)) {
                    int actuallyAdded = take - extracted.getCount();
                    if (actuallyAdded > 0) {
                        stack.shrink(actuallyAdded);
                        if (stack.isEmpty()) container.setItem(slot, ItemStack.EMPTY);
                        container.setChanged();
                        remaining -= actuallyAdded;
                        movedTotal += actuallyAdded;
                    }
                }
            }
        }
        syncForPlayer(player, center);
        return movedTotal;
    }

    private static List<Container> findContainers(ServerLevel level, BlockPos center) {
        List<Container> result = new ArrayList<>();
        if (level == null || center == null) return result;
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-RADIUS_XZ, -RADIUS_Y, -RADIUS_XZ),
                center.offset(RADIUS_XZ, RADIUS_Y, RADIUS_XZ))) {
            if (!level.hasChunkAt(pos) || RoyalChestRuntime.isGuarded(level, pos)) continue;
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof Container container) result.add(container);
        }
        return result;
    }
}
