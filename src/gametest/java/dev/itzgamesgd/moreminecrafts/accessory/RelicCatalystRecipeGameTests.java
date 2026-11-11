package dev.itzgamesgd.moreminecrafts.accessory;

import dev.itzgamesgd.moreminecrafts.registry.ModDataComponents;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;

/** Live crafting checks for the Relic Catalyst nine-slot shapeless recipe through vanilla recipe matching and the crafting menu. */
public final class RelicCatalystRecipeGameTests {
    private static final int GRID = 9;

    private static ItemStack catalyst() {
        return new ItemStack(ModItems.RELIC_CATALYST);
    }

    private static ItemStack t1(AccessoryDefinition definition, int enhancement) {
        ItemStack stack = new ItemStack(ModItems.ACCESSORIES.get(definition));
        if (enhancement > 0) stack.set(ModDataComponents.ENHANCEMENT_LEVEL, enhancement);
        return stack;
    }

    /** The approved multiset: one T1, two Mithril, one Etherium ingot, five Lapis; order is free. */
    private static List<ItemStack> validGrid(ItemStack accessory) {
        List<ItemStack> grid = new ArrayList<>();
        grid.add(accessory);
        grid.add(new ItemStack(ModItems.MYTHRIL));
        grid.add(new ItemStack(ModItems.MYTHRIL));
        grid.add(new ItemStack(ModItems.ETHERIUM_INGOT));
        for (int i = 0; i < 5; i++) grid.add(new ItemStack(Items.LAPIS_LAZULI));
        return grid;
    }

    private static ItemStack assemble(GameTestHelper ctx, List<ItemStack> grid) {
        CraftingInput input = CraftingInput.of(3, 3, grid);
        return ctx.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, ctx.getLevel())
                .map(holder -> holder.value().assemble(input))
                .orElse(ItemStack.EMPTY);
    }

    private static void assertCatalyst(GameTestHelper ctx, ItemStack result, String message) {
        ctx.assertTrue(result.is(ModItems.RELIC_CATALYST) && result.getCount() == 1, message + " yields one Relic Catalyst");
        ctx.assertTrue(!result.has(ModDataComponents.ENHANCEMENT_LEVEL), message + " does not inherit enhancement data");
        ctx.assertTrue(!result.has(DataComponents.CUSTOM_NAME), message + " does not inherit the consumed accessory's name");
    }

    private static CraftingMenu menu(GameTestHelper ctx, net.minecraft.world.entity.player.Player player) {
        return new CraftingMenu(1, player.getInventory(),
                ContainerLevelAccess.create(ctx.getLevel(), ctx.absolutePos(new BlockPos(1, 1, 1))));
    }

    /** Places nine stacks into the 3x3 grid (slots 1..9) and returns the menu's computed result (slot 0). */
    private static ItemStack fillMenu(CraftingMenu menu, List<ItemStack> grid) {
        for (int i = 0; i < GRID; i++) menu.getSlot(i + 1).set(grid.get(i));
        menu.slotsChanged(menu.getSlot(1).container);
        return menu.getSlot(0).getItem();
    }

    @GameTest
    public void approvedNineSlotMultisetCraftsOneCatalystInAnySlotOrder(GameTestHelper ctx) {
        ItemStack accessory = t1(AccessoryDefinition.T1_COMBO, 0);
        List<ItemStack> grid = validGrid(accessory);
        assertCatalyst(ctx, assemble(ctx, grid), "fresh T1 in the default order");

        List<ItemStack> reversed = new ArrayList<>(grid);
        java.util.Collections.reverse(reversed);
        assertCatalyst(ctx, assemble(ctx, reversed), "reversed slot order");

        List<ItemStack> rotated = new ArrayList<>(grid.subList(4, GRID));
        rotated.addAll(grid.subList(0, 4));
        assertCatalyst(ctx, assemble(ctx, rotated), "rotated slot order");
        ctx.succeed();
    }

    @GameTest
    public void everyRegisteredT1CraftsFreshAndEnhancedButNeverAnyT2(GameTestHelper ctx) {
        for (AccessoryDefinition definition : AccessoryDefinition.ALL) {
            if (!definition.isT1()) {
                ctx.assertTrue(assemble(ctx, validGrid(t1(definition, 0))).isEmpty(),
                        definition.registryId() + " (T2) is not a catalyst ingredient");
                continue;
            }
            for (int enhancement : new int[] {0, 5}) {
                assertCatalyst(ctx, assemble(ctx, validGrid(t1(definition, enhancement))),
                        definition.registryId() + " at +" + enhancement);
            }
        }
        ctx.succeed();
    }

    @GameTest
    public void shortagesAndWrongIngredientsShowNoResult(GameTestHelper ctx) {
        ItemStack accessory = t1(AccessoryDefinition.T1_DOUBLE_JUMP, 0);

        List<ItemStack> fourLapis = validGrid(accessory);
        fourLapis.set(8, ItemStack.EMPTY);
        ctx.assertTrue(assemble(ctx, fourLapis).isEmpty(), "four Lapis plus an empty slot is not craftable");

        List<ItemStack> diamondForMithril = validGrid(accessory);
        diamondForMithril.set(1, new ItemStack(Items.DIAMOND));
        ctx.assertTrue(assemble(ctx, diamondForMithril).isEmpty(), "a Diamond in place of Mithril is rejected");

        List<ItemStack> oneMithril = validGrid(accessory);
        oneMithril.set(2, new ItemStack(Items.DIRT));
        ctx.assertTrue(assemble(ctx, oneMithril).isEmpty(), "a foreign item in a Mithril slot is rejected");

        List<ItemStack> t2Accessory = validGrid(new ItemStack(ModItems.ACCESSORIES.get(AccessoryDefinition.SNOW_CHARM)));
        ctx.assertTrue(assemble(ctx, t2Accessory).isEmpty(), "a T2 accessory is rejected");

        List<ItemStack> noEtherium = validGrid(accessory);
        noEtherium.set(3, new ItemStack(ModItems.MYTHRIL));
        ctx.assertTrue(assemble(ctx, noEtherium).isEmpty(), "Etherium ingot must be present exactly once");
        ctx.succeed();
    }

    @GameTest
    public void craftingMenuShowsResultOnlyForValidGridAndConsumesExactlyOneOfEach(GameTestHelper ctx) {
        var player = ctx.makeMockServerPlayerInLevel();

        CraftingMenu short4 = menu(ctx, player);
        List<ItemStack> shortGrid = validGrid(t1(AccessoryDefinition.T1_COMBO, 0));
        shortGrid.set(8, ItemStack.EMPTY);
        ctx.assertTrue(fillMenu(short4, shortGrid).isEmpty(), "menu shows no result for a short grid");

        CraftingMenu menu = menu(ctx, player);
        ItemStack enhanced = t1(AccessoryDefinition.T1_HEAVY_BUFFER, 3);
        enhanced.set(DataComponents.CUSTOM_NAME, Component.literal("Enhanced Buffer"));
        List<ItemStack> grid = validGrid(enhanced);
        grid.set(8, new ItemStack(Items.LAPIS_LAZULI, 3)); // surplus Lapis in one slot: one is consumed, two remain
        ItemStack previewed = fillMenu(menu, grid);
        assertCatalyst(ctx, previewed, "enhanced T1 in the menu");

        menu.clicked(0, 0, ContainerInput.PICKUP, player);
        assertCatalyst(ctx, menu.getCarried(), "picked-up result");
        for (int slot = 1; slot <= GRID; slot++) {
            ItemStack remaining = menu.getSlot(slot).getItem();
            if (slot == 9) {
                ctx.assertTrue(remaining.is(Items.LAPIS_LAZULI) && remaining.getCount() == 2,
                        "exactly one Lapis consumed from the surplus slot");
            } else {
                ctx.assertTrue(remaining.isEmpty(), "grid slot " + slot + " consumed exactly its one ingredient");
            }
        }
        ctx.succeed();
    }
}
