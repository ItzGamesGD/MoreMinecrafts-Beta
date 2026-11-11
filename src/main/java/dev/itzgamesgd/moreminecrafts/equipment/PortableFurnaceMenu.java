package dev.itzgamesgd.moreminecrafts.equipment;

import dev.itzgamesgd.moreminecrafts.furnace.FurnaceResearchRules;
import dev.itzgamesgd.moreminecrafts.furnace.FurnaceResearchRuntime;
import dev.itzgamesgd.moreminecrafts.furnace.FurnaceResearchState;
import dev.itzgamesgd.moreminecrafts.progression.ResearchProgressLookup;
import dev.itzgamesgd.moreminecrafts.registry.ModDataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
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

/** Portable furnace whose inventory and processing state follow its single-stack opener item. */
public final class PortableFurnaceMenu extends FurnaceMenu {
    private final PortableContainer contents;
    private final SimpleContainerData state;
    private final ItemStack opener;
    private final Inventory playerInventory;
    private final ServerPlayer openerOwner;
    private boolean closed;

    public PortableFurnaceMenu(int id, Inventory playerInventory, PortableContainer contents) {
        this(id, playerInventory, contents, ItemStack.EMPTY,
                PortableFurnaceContents.empty(LowerEquipmentRules.PORTABLE_FURNACE_COOK_TIME),
                new SimpleContainerData(4), null);
    }

    public PortableFurnaceMenu(int id, Inventory playerInventory, PortableContainer contents, ItemStack opener) {
        this(id, playerInventory, contents, opener,
                PortableFurnaceContents.empty(LowerEquipmentRules.PORTABLE_FURNACE_COOK_TIME),
                new SimpleContainerData(4), null);
    }

    public PortableFurnaceMenu(int id, Inventory playerInventory, PortableContainer contents, ItemStack opener,
                               PortableFurnaceContents saved) {
        this(id, playerInventory, contents, opener, saved, new SimpleContainerData(4), null);
    }

    public PortableFurnaceMenu(int id, Inventory playerInventory, PortableContainer contents, ItemStack opener,
                               PortableFurnaceContents saved, ServerPlayer openerOwner) {
        this(id, playerInventory, contents, opener, saved, new SimpleContainerData(4), openerOwner);
    }

    private PortableFurnaceMenu(int id, Inventory playerInventory, PortableContainer contents,
                                ItemStack opener, PortableFurnaceContents saved, SimpleContainerData state,
                                ServerPlayer openerOwner) {
        super(id, playerInventory, contents, state);
        this.contents = contents;
        this.state = state;
        this.opener = opener;
        this.playerInventory = playerInventory;
        this.openerOwner = openerOwner;
        state.set(0, saved.burnTime());
        state.set(1, saved.burnDuration());
        state.set(2, saved.cookTime());
        state.set(3, saved.cookDuration());
        contents.setChangeListener(this::saveContents);
        contents.itemsChanged();
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

    @Override public void clicked(int slotId, int button, ContainerInput clickType, Player player) {
        if (isOpenerSlot(slotId)
                || clickType == ContainerInput.SWAP && isOpenerHotbarSwap(button)
                || clickType == ContainerInput.PICKUP_ALL && hasOpenerInPlayerInventory()) return;
        super.clicked(slotId, button, clickType, player);
    }

    @Override public ItemStack quickMoveStack(Player player, int slotId) {
        if (isOpenerSlot(slotId)) return ItemStack.EMPTY;
        return super.quickMoveStack(player, slotId);
    }

    private boolean isOpenerSlot(int slotId) {
        return slotId >= 0 && slotId < slots.size()
                && slots.get(slotId).container == playerInventory
                && slots.get(slotId).getItem() == opener;
    }

    private boolean isOpenerHotbarSwap(int inventorySlot) {
        for (Slot slot : slots) {
            if (slot.container == playerInventory && slot.getContainerSlot() == inventorySlot
                    && slot.getItem() == opener) return true;
        }
        return false;
    }

    private boolean hasOpenerInPlayerInventory() {
        for (Slot slot : slots)
            if (slot.container == playerInventory && slot.getItem() == opener) return true;
        return false;
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
        ItemStack result = recipe.isEmpty() ? ItemStack.EMPTY
                : recipe.get().value().assemble(new SingleRecipeInput(input));
        int precisionLevel = researchLevel(player, "v3.research.07");
        boolean precisionEligible = precisionLevel > 0 && FurnaceResearchRuntime.eligibleMineral(input, result);
        boolean bonusFits = !precisionEligible || FurnaceResearchRules.fitsPossibleBonus(
                contents.getItem(2).getCount(), result.getCount(), contents.getMaxStackSize(result));
        if (recipe.isEmpty() || !canAccept(result) || !bonusFits) {
            state.set(2, 0); saveContents(); broadcastChanges(); return;
        }
        if (state.get(0) <= 0 && !consumeFuel(player)) { state.set(2, 0); saveContents(); broadcastChanges(); return; }
        var research = researchState();
        var cooking = FurnaceResearchRules.cookingTick(state.get(2) + 1, state.get(3),
                researchLevel(player, "v3.research.11"), research.cookingProgressRemainder(), research.cookingCredit());
        updateResearchState(current -> current.withCooking(cooking.remainder(), cooking.credit()));
        state.set(2, cooking.timer());
        if (state.get(2) >= state.get(3)) {
            // The possible bonus was reserved before consuming fuel or advancing this unit.
            if (precisionEligible && FurnaceResearchRules.precisionBonus(precisionLevel,
                    player.level().getRandom().nextInt(10000))) result = result.copyWithCount(result.getCount() + 1);
            input.shrink(1);
            contents.setItem(0, input);
            ItemStack output = contents.getItem(2);
            if (output.isEmpty()) contents.setItem(2, result.copy()); else output.grow(result.getCount());
            state.set(2, 0);
        }
        saveContents();
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
        var fuelAccounting = FurnaceResearchRules.fuelDuration(accelerated,
                researchLevel(player, "v3.research.06"), researchState().fuelSavingRemainder());
        updateResearchState(current -> current.withFuelRemainder(fuelAccounting.remainder()));
        state.set(0, Math.max(0, fuelAccounting.burnTicks() - 1));
        state.set(1, fuelAccounting.burnTicks());
        return true;
    }

    private FurnaceResearchState researchState() {
        return opener.getOrDefault(ModDataComponents.PORTABLE_FURNACE_RESEARCH_STATE, FurnaceResearchState.empty());
    }

    private int researchLevel(ServerPlayer processor, String researchId) {
        return researchState().attributedPlayer()
                .map(owner -> ResearchProgressLookup.researchLevel(processor.level().getServer(), owner, researchId))
                .orElse(0);
    }

    private boolean canAccept(ItemStack result) {
        return canAcceptResult(contents.getItem(2), result, contents.getMaxStackSize(result));
    }

    private void saveContents() {
        if (opener.isEmpty()) return;
        opener.set(dev.itzgamesgd.moreminecrafts.registry.ModDataComponents.PORTABLE_FURNACE_CONTENTS,
                PortableFurnaceContents.from(contents, state.get(0), state.get(1), state.get(2), state.get(3)));
        if (openerOwner != null) openerOwner.getInventory().setChanged();
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
            saveContents();
            player.getInventory().setChanged();
        }
        super.removed(player);
    }

    public static final class PortableContainer extends SimpleContainer {
        private Runnable changeListener = () -> {};
        public PortableContainer() { super(3); }
        public PortableContainer(java.util.List<ItemStack> items) {
            super(3);
            for (int i = 0; i < Math.min(3, items.size()); i++) setItem(i, items.get(i).copy());
        }
        void setChangeListener(Runnable listener) { changeListener = listener; }
        void itemsChanged() { changeListener.run(); }
        @Override public void setChanged() { super.setChanged(); changeListener.run(); }
        @Override public int getMaxStackSize() { return LowerEquipmentRules.PORTABLE_FURNACE_STACK_LIMIT; }
        @Override public int getMaxStackSize(ItemStack stack) {
            return stack.getMaxStackSize() == 64 ? LowerEquipmentRules.PORTABLE_FURNACE_STACK_LIMIT
                    : stack.getMaxStackSize();
        }
    }
}
