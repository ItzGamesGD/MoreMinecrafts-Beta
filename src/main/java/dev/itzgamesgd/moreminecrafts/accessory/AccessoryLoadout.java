package dev.itzgamesgd.moreminecrafts.accessory;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/** The single five-stack equipment state persisted and synchronized for each player. */
public final class AccessoryLoadout {
    public static final int SIZE = AccessoryCategory.values().length;
    public static final Codec<AccessoryLoadout> CODEC = ItemStack.OPTIONAL_CODEC.listOf(SIZE, SIZE)
            .xmap(AccessoryLoadout::new, AccessoryLoadout::stacks);

    private final List<ItemStack> stacks;

    public AccessoryLoadout(List<ItemStack> stacks) {
        if (stacks.size() != SIZE) throw new IllegalArgumentException("Accessory loadout must have exactly five slots");
        ArrayList<ItemStack> copy = new ArrayList<>(SIZE);
        for (int slot = 0; slot < SIZE; slot++) {
            ItemStack stack = stacks.get(slot);
            ItemStack stored = stack == null ? ItemStack.EMPTY : stack.copy();
            if (!stored.isEmpty() && !AccessorySlotRules.accepts(slot, stored)) {
                throw new IllegalArgumentException("Invalid accessory stack in category slot " + slot);
            }
            copy.add(stored);
        }
        this.stacks = List.copyOf(copy);
    }

    public static AccessoryLoadout empty() {
        return new AccessoryLoadout(java.util.Collections.nCopies(SIZE, ItemStack.EMPTY));
    }

    public ItemStack get(int slot) {
        checkSlot(slot);
        return stacks.get(slot).copy();
    }

    /** Returns an immutable deep snapshot suitable for death drops and transactions. */
    public List<ItemStack> stacks() {
        return stacks.stream().map(ItemStack::copy).toList();
    }

    public AccessoryLoadout with(int slot, ItemStack stack) {
        checkSlot(slot);
        if (stack == null || (!stack.isEmpty() && !AccessorySlotRules.accepts(slot, stack))) {
            throw new IllegalArgumentException("Invalid stack for accessory slot " + slot);
        }
        ArrayList<ItemStack> changed = new ArrayList<>(stacks());
        changed.set(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
        return new AccessoryLoadout(changed);
    }

    public boolean canManuallyActivate(int slot) {
        checkSlot(slot);
        return slot == AccessoryCategory.COMBAT.ordinal() && !stacks.get(slot).isEmpty();
    }

    public boolean isEmpty() {
        return stacks.stream().allMatch(ItemStack::isEmpty);
    }

    public int occupiedSlots() {
        return (int) stacks.stream().filter(stack -> !stack.isEmpty()).count();
    }

    public static void checkSlot(int slot) {
        if (slot < 0 || slot >= SIZE) throw new IndexOutOfBoundsException("Accessory slot " + slot);
    }
}
