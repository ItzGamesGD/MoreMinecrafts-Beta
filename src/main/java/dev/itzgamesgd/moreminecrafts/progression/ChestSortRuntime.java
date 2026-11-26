package dev.itzgamesgd.moreminecrafts.progression;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;

/** Server-authoritative atomic in-place chest sorting for `v3.research.13`. */
public final class ChestSortRuntime {
    private ChestSortRuntime() {}

    public static boolean sortOpenChest(ServerPlayer player, int mode) {
        if (player == null || ResearchRewards.level(player, "v3.research.13", "chest_sort") <= 0) {
            return false;
        }
        if (!(player.containerMenu instanceof ChestMenu menu)) return false;
        Container container = menu.getContainer();
        int size = container.getContainerSize();
        if (size <= 1) return false;

        List<ItemStack> snapshot = new ArrayList<>(size);
        int totalBefore = 0;
        for (int i = 0; i < size; i++) {
            ItemStack copy = container.getItem(i).copy();
            snapshot.add(copy);
            totalBefore += copy.getCount();
        }

        List<ItemStack> sorted = ChestSortRules.sort(snapshot, mode);
        int totalAfter = 0;
        for (ItemStack s : sorted) totalAfter += s.getCount();
        if (totalAfter != totalBefore) return false; // conservation guard

        // Verify no concurrent mutation occurred before committing.
        for (int i = 0; i < size; i++) {
            if (!ItemStack.matches(snapshot.get(i), container.getItem(i))) return false;
        }

        for (int i = 0; i < size; i++) {
            container.setItem(i, sorted.get(i));
        }
        container.setChanged();
        menu.broadcastChanges();
        return true;
    }
}
