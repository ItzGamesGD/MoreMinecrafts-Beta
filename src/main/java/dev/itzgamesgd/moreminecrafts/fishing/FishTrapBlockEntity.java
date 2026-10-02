package dev.itzgamesgd.moreminecrafts.fishing;

import dev.itzgamesgd.moreminecrafts.equipment.LowerEquipmentRules;
import dev.itzgamesgd.moreminecrafts.registry.ModBlockEntities;
import dev.itzgamesgd.moreminecrafts.registry.ModDataComponents;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class FishTrapBlockEntity extends BaseContainerBlockEntity {
    private NonNullList<ItemStack> items = NonNullList.withSize(LowerEquipmentRules.FISH_TRAP_SLOTS, ItemStack.EMPTY);
    private int productionTicks;

    public FishTrapBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.FISH_TRAP, pos, state); }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, FishTrapBlockEntity trap) {
        if (trap.isFull()) return; // timer is intentionally paused, not reset, while full
        trap.productionTicks++;
        trap.setChanged();
        if (trap.productionTicks < LowerEquipmentRules.FISH_TRAP_INTERVAL_TICKS) return;
        trap.productionTicks = 0;
        var random = level.getRandom();
        FishSpecies species = FishingLogic.selectSpecies(java.util.List.of(FishSpecies.values()), random);
        int grade = FishingLogic.rollGrade(0, random);
        int length = FishingLogic.rollLength(species, random);
        ItemStack fish = new ItemStack(ModItems.fishItem(species));
        fish.set(ModDataComponents.FISH_GRADE, grade);
        fish.set(ModDataComponents.FISH_LENGTH_TENTHS_CM, length);
        fish.set(ModDataComponents.FISH_WEIGHT_TENTHS_KG, FishingLogic.rollWeight(species, length, random));
        for (int i = 0; i < trap.items.size(); i++) if (trap.items.get(i).isEmpty()) {
            trap.items.set(i, fish); trap.setChanged(); return;
        }
    }

    private boolean isFull() { for (ItemStack item : items) if (item.isEmpty()) return false; return true; }
    public int productionTicks() { return productionTicks; }

    @Override protected Component getDefaultName() { return Component.translatable("container.moreminecrafts.fish_trap"); }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> items) { this.items = items; }
    @Override public int getContainerSize() { return LowerEquipmentRules.FISH_TRAP_SLOTS; }
    @Override public int getMaxStackSize() { return 1; }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new ChestMenu(net.minecraft.world.inventory.MenuType.GENERIC_9x1, id, inventory, this, 1);
    }
    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input); ContainerHelper.loadAllItems(input, items);
        productionTicks = Math.max(0, Math.min(LowerEquipmentRules.FISH_TRAP_INTERVAL_TICKS - 1,
                input.getIntOr("ProductionTicks", 0)));
    }
    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output); ContainerHelper.saveAllItems(output, items);
        output.putInt("ProductionTicks", productionTicks);
    }
}
