package dev.itzgamesgd.moreminecrafts.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/** Item-backed contents and in-progress work for one portable furnace. */
public record PortableFurnaceContents(List<ItemStack> items, int burnTime, int burnDuration,
                                      int cookTime, int cookDuration) {
    private static final int SLOT_COUNT = 3;
    private static final Codec<List<ItemStack>> ITEMS_CODEC = ItemStack.OPTIONAL_CODEC.listOf(0, SLOT_COUNT);

    public static final Codec<PortableFurnaceContents> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ITEMS_CODEC.fieldOf("items").forGetter(PortableFurnaceContents::items),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("burn_time", 0)
                    .forGetter(PortableFurnaceContents::burnTime),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("burn_duration", 0)
                    .forGetter(PortableFurnaceContents::burnDuration),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("cook_time", 0)
                    .forGetter(PortableFurnaceContents::cookTime),
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("cook_duration",
                    LowerEquipmentRules.PORTABLE_FURNACE_COOK_TIME)
                    .forGetter(PortableFurnaceContents::cookDuration)
    ).apply(instance, PortableFurnaceContents::new));

    public PortableFurnaceContents {
        var normalized = new ArrayList<ItemStack>(SLOT_COUNT);
        for (int i = 0; i < SLOT_COUNT; i++)
            normalized.add(i < items.size() ? items.get(i).copy() : ItemStack.EMPTY);
        items = List.copyOf(normalized);
        if (burnTime < 0 || burnDuration < 0 || cookTime < 0 || cookDuration < 1)
            throw new IllegalArgumentException("Invalid portable furnace progress");
    }

    public static PortableFurnaceContents empty(int cookDuration) {
        return new PortableFurnaceContents(List.of(), 0, 0, 0, cookDuration);
    }

    public static PortableFurnaceContents from(PortableFurnaceMenu.PortableContainer container,
                                               int burnTime, int burnDuration, int cookTime, int cookDuration) {
        return new PortableFurnaceContents(List.of(container.getItem(0), container.getItem(1), container.getItem(2)),
                burnTime, burnDuration, cookTime, cookDuration);
    }
}
