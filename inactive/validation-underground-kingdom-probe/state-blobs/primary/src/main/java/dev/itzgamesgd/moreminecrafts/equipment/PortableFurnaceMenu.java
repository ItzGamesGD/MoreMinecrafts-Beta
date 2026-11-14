package dev.itzgamesgd.moreminecrafts.equipment;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;

/** Ephemeral furnace session; only attribution and fractional research state live on the opener item. */
public final class PortableFurnaceMenu extends FurnaceMenu {
    private final PortableContainer contents;
    private final SimpleContainerData state;
    private final ItemStack opener;
    private boolean closed;

    public PortableFurnaceMenu(int id, Inventory playerInventory, PortableContainer contents) {
        this(id, playerInventory, contents, ItemStack.EMPTY, new SimpleContainerData(4));
    }

    public PortableFurnaceMenu(int id, Inventory playerInventory, PortableContainer contents, ItemStack opener) {
        this(id, playerInventory, contents, opener, new SimpleContainerData(4));
    }

    private PortableFurnaceMenu(int id, Inventory playerInventory, PortableContainer contents,
                                ItemStack opener, SimpleContainerData state) {
        super(id, playerInventory, contents, state);
        this.contents = contents;
        this.state = state;
        this.opener = opener;
        state.set(3, LowerEquipmentRules.PORTABLE_FURNACE_COOK_TIME);
    }

    public java.util.List<ItemStack> researchInventorySnapshot() {
        return java.util.List.of(contents.getItem(0).copy(), contents.getItem(1).copy(), contents.getItem(2).copy());
    }

    public boolean researchInventoryChanged(java.util.List<ItemStack> before) {
        if (before.size() != 3) return false;
        for (int slot = 0; slot < 3; slot++)
            if (!ItemStack.matches(before.get(slot), contents.getItem(slot))) return true;
        return false;
    }

    public void recordResearchInteraction(ServerPlayer player) {
        updateResearchState(state -> dev.itzgamesgd.moreminecrafts.furnace.FurnaceResearchAttribution
                .interactedState(state, player.getUUID()));
        player.getInventory().setChanged();
    }

    private void updateResearchState(java.util.function.UnaryOperator<
            dev.itzgamesgd.moreminecrafts.furnace.FurnaceResearchState> update) {
        if (opener.isEmpty()) return;
        var component = dev.itzgamesgd.moreminecrafts.registry.ModDataComponents.PORTABLE_FURNACE_RESEARCH_STATE;
        var before = opener.getOrDefault(component,
                dev.itzgamesgd.moreminecrafts.furnace.FurnaceResearchState.empty());
        var after = update.apply(before);
        if (!after.equals(before)) opener.set(component, after);
    }

    public void serverTick(ServerPlayer player) {
        if (closed) return;
        if (state.get(0) > 0) state.set(0, state.get(0) - 1);
        ItemStack input = contents.getItem(0);
        var recipe = input.isEmpty() ? java.util.Optional.<net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.SmeltingRecipe>>empty()
                : player.level().getServer().getRecipeManager().getRecipeFor(
                        RecipeType.SMELTING, new SingleRecipeInput(input), player.level());
        if (recipe.isEmpty() || !canAccept(recipe.get().value().assemble(new SingleRecipeInput(input)))) {
            state.set(2, 0); broadcastChanges(); return;
        }
        if (state.get(0) <= 0 && !consumeFuel(player)) { state.set(2, 0); broadcastChanges(); return; }
        state.set(2, state.get(2) + 1);
        if (state.get(2) >= LowerEquipmentRules.PORTABLE_FURNACE_COOK_TIME) {
            ItemStack result = recipe.get().value().assemble(new SingleRecipeInput(input));
            input.shrink(1);
            contents.setItem(0, input);
            ItemStack output = contents.getItem(2);
            if (output.isEmpty()) contents.setItem(2, result.copy()); else output.grow(result.getCount());
            state.set(2, 0);
        }
        broadcastChanges();
    }

    private boolean consumeFuel(ServerPlayer player) {
        ItemStack fuel = contents.getItem(1);
        int vanillaTicks = vanillaFuelTicks((ServerLevel) player.level(), player, fuel);
        if (vanillaTicks <= 0) return false;
        ItemStackTemplate remainderTemplate = fuel.getItem().getCraftingRemainder();
        ItemStack remainder = remainderTemplate == null ? ItemStack.EMPTY : remainderTemplate.create();
        fuel.shrink(1);
        if (!remainder.isEmpty()) {
            if (fuel.isEmpty()) fuel = remainder;
            else if (!player.getInventory().add(remainder))
                player.spawnAtLocation((ServerLevel) player.level(), remainder);
        }
        contents.setItem(1, fuel);
        int accelerated = Math.max(1, vanillaTicks / LowerEquipmentRules.PORTABLE_FURNACE_FUEL_TICK_MULTIPLIER);
        state.set(0, Math.max(0, accelerated - 1)); state.set(1, accelerated);
        return true;
    }

    private boolean canAccept(ItemStack result) {
        return canAcceptResult(contents.getItem(2), result, contents.getMaxStackSize(result));
    }

    static boolean canAcceptResult(ItemStack output, ItemStack result, int slotLimit) {
        if (result.isEmpty() || slotLimit < 1 || result.getCount() > slotLimit) return false;
        return output.isEmpty() || ItemStack.isSameItemSameComponents(output, result)
                && output.getCount() + result.getCount() <= slotLimit;
    }

    static int vanillaFuelTicks(ServerLevel level, Player player, ItemStack fuel) {
        if (fuel.isEmpty()) return 0;
        // Provider values depend on the furnace context, including fast-cooking predicates.
        LootContext context = new LootContext.Builder(new LootParams.Builder(level)
                .withParameter(LootContextParams.BLOCK_STATE, Blocks.FURNACE.defaultBlockState())
                .withParameter(LootContextParams.ORIGIN, player.position())
                .create(LootContextParamSets.BLOCK_INTERACT)).create(Optional.empty());
        return ResolvableInt.getFromItem(fuel, DataComponents.COOKING_FUEL,
                CookingFuel::burnTime, context, 0);
    }

    @Override public void removed(Player player) {
        if (!closed && !player.level().isClientSide()) {
            closed = true;
            var serverLevel = (ServerLevel) player.level();
            for (ItemStack stack : contents.removeAllItems())
                if (!stack.isEmpty()) player.spawnAtLocation(serverLevel, stack);
        }
        super.removed(player);
    }

    public static final class PortableContainer extends SimpleContainer {
        public PortableContainer() { super(3); }
        @Override public int getMaxStackSize() { return LowerEquipmentRules.PORTABLE_FURNACE_STACK_LIMIT; }
        @Override public int getMaxStackSize(ItemStack stack) {
            return stack.getMaxStackSize() == 64 ? LowerEquipmentRules.PORTABLE_FURNACE_STACK_LIMIT
                    : stack.getMaxStackSize();
        }
    }
}
