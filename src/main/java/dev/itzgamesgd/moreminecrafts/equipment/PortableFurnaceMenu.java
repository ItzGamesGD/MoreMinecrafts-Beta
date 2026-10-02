package dev.itzgamesgd.moreminecrafts.equipment;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;

/** Ephemeral furnace session. Nothing in this menu is serialized into its opener item. */
public final class PortableFurnaceMenu extends FurnaceMenu {
    private final PortableContainer contents;
    private final SimpleContainerData state;
    private boolean closed;

    public PortableFurnaceMenu(int id, Inventory playerInventory, PortableContainer contents) {
        this(id, playerInventory, contents, new SimpleContainerData(4));
    }

    private PortableFurnaceMenu(int id, Inventory playerInventory, PortableContainer contents, SimpleContainerData state) {
        super(id, playerInventory, contents, state);
        this.contents = contents;
        this.state = state;
        state.set(3, LowerEquipmentRules.PORTABLE_FURNACE_COOK_TIME);
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
        int vanillaTicks = vanillaFuelTicks(fuel);
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
        ItemStack output = contents.getItem(2);
        return output.isEmpty() || ItemStack.isSameItemSameComponents(output, result)
                && output.getCount() + result.getCount() <= contents.getMaxStackSize(result);
    }

    static int vanillaFuelTicks(ItemStack fuel) {
        if (fuel.isEmpty()) return 0;
        var component = fuel.get(DataComponents.COOKING_FUEL);
        if (component == null) return 0;
        ResolvableInt burnTime = component.burnTime();
        if (burnTime instanceof ResolvableInt.Constant constant) return constant.value();
        if (fuel.is(Items.BAMBOO)) return 50;
        if (fuel.is(Items.SCAFFOLDING)) return 300;
        if (burnTime instanceof ResolvableInt.Reference reference) {
            return vanillaFuelTicksForProvider(reference.key().identifier().getPath());
        }
        return 0;
    }

    static int vanillaFuelTicksForProvider(String providerPath) {
        return switch (providerPath) {
            case "time_lava_bucket" -> 20_000;
            case "time_coal_block" -> 16_000;
            case "time_dried_kelp_block" -> 4_001;
            case "time_blaze_rod" -> 2_400;
            case "time_coal" -> 1_600;
            case "time_boats" -> 1_200;
            case "time_hanging_signs" -> 800;
            case "time_wood_items_small", "time_wood_blocks", "time_roots" -> 300;
            case "time_wood_items_large" -> 200;
            case "time_wood_slabs" -> 150;
            case "time_wool", "time_dry_plants", "time_wood_items_extra_small" -> 100;
            case "time_wool_carpets" -> 67;
            case "time_wool_slabs", "time_bamboo" -> 50;
            default -> 0;
        };
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
        @Override public int getMaxStackSize(ItemStack stack) {
            return stack.getMaxStackSize() == 64 ? LowerEquipmentRules.PORTABLE_FURNACE_STACK_LIMIT
                    : stack.getMaxStackSize();
        }
    }
}
