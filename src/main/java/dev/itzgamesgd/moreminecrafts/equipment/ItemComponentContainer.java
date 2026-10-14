package dev.itzgamesgd.moreminecrafts.equipment;

import java.util.ArrayList;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/** A fixed-size, server-owned view of the container component on one item stack. */
public final class ItemComponentContainer extends SimpleContainer {
    private final ItemStack owner;
    private boolean loading;

    public ItemComponentContainer(ItemStack owner, int size) {
        super(size);
        this.owner = owner;
        loading = true;
        var loaded = NonNullList.withSize(size, ItemStack.EMPTY);
        owner.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(loaded);
        for (int i = 0; i < size; i++) super.setItem(i, loaded.get(i));
        loading = false;
    }

    @Override public void setChanged() {
        super.setChanged();
        if (!loading) owner.set(DataComponents.CONTAINER,
                ItemContainerContents.fromItems(new ArrayList<>(getItems())));
    }
}
