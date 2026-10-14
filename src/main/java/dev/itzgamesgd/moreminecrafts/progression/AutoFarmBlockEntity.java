package dev.itzgamesgd.moreminecrafts.progression;

import dev.itzgamesgd.moreminecrafts.registry.ModBlockEntities;
import dev.itzgamesgd.moreminecrafts.registry.ModMenuTypes;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Data-safe one-layer 9×9 automatic crop farm. 25 seed + 25 output slots. */
public final class AutoFarmBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int STOCK = 25, TOTAL = 50;
    private static final int[] INPUT_SLOTS = new int[STOCK];
    private static final int[] OUTPUT_SLOTS = new int[STOCK];
    static { for (int i = 0; i < STOCK; i++) { INPUT_SLOTS[i] = i; OUTPUT_SLOTS[i] = STOCK + i; } }

    private NonNullList<ItemStack> items = NonNullList.withSize(TOTAL, ItemStack.EMPTY);
    private int cursor;

    public AutoFarmBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AUTO_FARM, pos, state);
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, AutoFarmBlockEntity farm) {
        if (level.getGameTime() % 20 != 0 || !level.hasNeighborSignal(pos)) return;
        // One operation per tick interval, one crop layer only, never force chunk loading.
        for (int tested = 0; tested < 81; tested++) {
            int index = (farm.cursor++ % 81);
            int dx = index % 9 - 4, dz = index / 9 - 4;
            BlockPos crop = pos.offset(dx, 2, dz);
            if (!level.hasChunkAt(crop)) continue;
            BlockState current = level.getBlockState(crop);
            if (!(current.getBlock() instanceof CropBlock block) || !block.isMaxAge(current)) continue;
            if (farm.harvest(level, crop, current, block)) break;
        }
    }

    private boolean harvest(ServerLevel level, BlockPos pos, BlockState current, CropBlock crop) {
        Item seed = cropSeed(current);
        if (seed == null) return false; // Add an explicit adapter before supporting other modded crops.
        NonNullList<ItemStack> staging = NonNullList.withSize(TOTAL, ItemStack.EMPTY);
        for (int i = 0; i < TOTAL; i++) staging.set(i, items.get(i).copy());
        int seedSlot = -1;
        for (int i = 0; i < STOCK; i++) if (staging.get(i).is(seed)) { seedSlot = i; break; }
        if (seedSlot == -1) return false; // Cannot replant -> do not destroy original crop.
        staging.get(seedSlot).shrink(1);
        List<ItemStack> drops = Block.getDrops(current, level, pos, level.getBlockEntity(pos));
        // Simulate the entire output insertion before changing either the world or inventory.
        for (ItemStack drop : drops) {
            if (drop.isEmpty()) continue;
            ItemStack remainder = drop.copy();
            if (remainder.is(seed)) insert(staging, 0, STOCK, remainder);
            if (!remainder.isEmpty()) insert(staging, STOCK, TOTAL, remainder);
            if (!remainder.isEmpty()) return false;
        }
        // No external threads can modify the server inventory or crop in the same tick.
        if (!level.getBlockState(pos).equals(current)) return false;
        if (!level.setBlockAndUpdate(pos, crop.getStateForAge(0))) return false;
        items = staging;
        setChanged();
        return true;
    }

    private static Item cropSeed(BlockState state) {
        Block block = state.getBlock();
        if (block == Blocks.WHEAT) return Items.WHEAT_SEEDS;
        if (block == Blocks.BEETROOTS) return Items.BEETROOT_SEEDS;
        if (block == Blocks.CARROTS) return Items.CARROT;
        if (block == Blocks.POTATOES) return Items.POTATO;
        return null;
    }

    /** A complete staged insertion; never mutates real inventory on failure. */
    private static void insert(NonNullList<ItemStack> copy, int start, int end, ItemStack incoming) {
        for (int i = start; i < end && !incoming.isEmpty(); i++) {
            ItemStack slot = copy.get(i);
            if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, incoming)) {
                int moved = Math.min(incoming.getCount(), slot.getMaxStackSize() - slot.getCount());
                if (moved > 0) { slot.grow(moved); incoming.shrink(moved); }
            }
        }
        for (int i = start; i < end && !incoming.isEmpty(); i++) if (copy.get(i).isEmpty()) {
            int moved = Math.min(incoming.getCount(), incoming.getMaxStackSize());
            copy.set(i, incoming.copyWithCount(moved));
            incoming.shrink(moved);
        }
    }

    @Override public int[] getSlotsForFace(Direction direction) {
        return direction == Direction.DOWN ? OUTPUT_SLOTS.clone() : INPUT_SLOTS.clone();
    }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return slot >= 0 && slot < STOCK && direction != Direction.DOWN;
    }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return slot >= STOCK && slot < TOTAL && direction == Direction.DOWN;
    }
    @Override protected Component getDefaultName() { return Component.translatable("container.moreminecrafts.auto_farm"); }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> items) { this.items = items; }
    @Override public int getContainerSize() { return TOTAL; }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new AutoFarmMenu(id, inventory, this);
    }
    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input); ContainerHelper.loadAllItems(input, items);
        cursor = Math.floorMod(input.getIntOr("Cursor", 0), 81);
    }
    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output); ContainerHelper.saveAllItems(output, items);
        output.putInt("Cursor", cursor);
    }
}
