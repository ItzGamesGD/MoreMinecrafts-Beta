package dev.itzgamesgd.moreminecrafts.equipment;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;

public record BackpackStorage(List<ItemStack> items) {
    public static final int SIZE = 54;
    public static final Codec<BackpackStorage> CODEC = ItemStack.OPTIONAL_CODEC.listOf(0, SIZE)
            .xmap(BackpackStorage::new, BackpackStorage::items);

    public BackpackStorage {
        var normalized = new ArrayList<ItemStack>(SIZE);
        for (int i = 0; i < SIZE; i++) normalized.add(i < items.size() ? items.get(i) : ItemStack.EMPTY);
        items = normalized;
    }

    public static BackpackStorage empty() { return new BackpackStorage(List.of()); }
}
