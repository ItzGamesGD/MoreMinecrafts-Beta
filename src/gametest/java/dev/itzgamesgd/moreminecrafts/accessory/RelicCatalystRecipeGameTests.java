package dev.itzgamesgd.moreminecrafts.accessory;

import dev.itzgamesgd.moreminecrafts.registry.ModDataComponents;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * Live crafting checks for the Relic Catalyst 3x3 shaped recipe through vanilla recipe matching and the crafting menu.
 * Approved layout (slots 1..9): Enhancement Stone / Mythril / Enhancement Stone, Lapis / Etherium Shard / Lapis,
 * Enhancement Stone / Mythril / Enhancement Stone.
 */
public final class RelicCatalystRecipeGameTests {
    private static final int GRID = 9;

    private static ItemStack stone() {
        return new ItemStack(ModItems.ENHANCEMENT_STONE);
    }

    private static ItemStack mythril() {
        return new ItemStack(ModItems.MYTHRIL);
    }

    private static ItemStack lapis() {
        return new ItemStack(Items.LAPIS_LAZULI);
    }

    private static ItemStack shard() {
        return new ItemStack(ModItems.ETHERIUM_SHARD);
    }

    /** The approved layout in slot order 1..9. */
    private static List<ItemStack> validGrid() {
        List<ItemStack> grid = new ArrayList<>();
        grid.add(stone());
        grid.add(mythril());
        grid.add(stone());
        grid.add(lapis());
        grid.add(shard());
        grid.add(lapis());
        grid.add(stone());
        grid.add(mythril());
        grid.add(stone());
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
        ctx.assertTrue(!result.has(ModDataComponents.ENHANCEMENT_LEVEL), message + " carries no enhancement data");
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
    public void approvedLayoutCraftsOneCatalyst(GameTestHelper ctx) {
        assertCatalyst(ctx, assemble(ctx, validGrid()), "the approved layout");
        ctx.succeed();
    }

    @GameTest
    public void swappedOrMisplacedIngredientsAreRejected(GameTestHelper ctx) {
        List<ItemStack> mythrilAndLapisSwapped = validGrid();
        mythrilAndLapisSwapped.set(1, lapis());
        mythrilAndLapisSwapped.set(3, mythril());
        ctx.assertTrue(assemble(ctx, mythrilAndLapisSwapped).isEmpty(), "Mythril and Lapis in each other's slots is rejected");

        List<ItemStack> shardInCorner = validGrid();
        shardInCorner.set(0, shard());
        shardInCorner.set(4, stone());
        ctx.assertTrue(assemble(ctx, shardInCorner).isEmpty(), "the shard must sit in the centre slot");

        List<ItemStack> unordered = validGrid();
        java.util.Collections.reverse(unordered.subList(0, 4));
        ctx.assertTrue(assemble(ctx, unordered).isEmpty(), "a shapeless arrangement of the same items is not accepted");
        ctx.succeed();
    }

    @GameTest
    public void wrongOrMissingIngredientsShowNoResult(GameTestHelper ctx) {
        List<ItemStack> missingShard = validGrid();
        missingShard.set(4, ItemStack.EMPTY);
        ctx.assertTrue(assemble(ctx, missingShard).isEmpty(), "no centre shard is not craftable");

        List<ItemStack> ingotInsteadOfShard = validGrid();
        ingotInsteadOfShard.set(4, new ItemStack(ModItems.ETHERIUM_INGOT));
        ctx.assertTrue(assemble(ctx, ingotInsteadOfShard).isEmpty(), "an Etherium ingot in place of the shard is rejected");

        List<ItemStack> diamondForMythril = validGrid();
        diamondForMythril.set(1, new ItemStack(Items.DIAMOND));
        ctx.assertTrue(assemble(ctx, diamondForMythril).isEmpty(), "a Diamond in place of Mythril is rejected");

        List<ItemStack> dirtForStone = validGrid();
        dirtForStone.set(8, new ItemStack(Items.DIRT));
        ctx.assertTrue(assemble(ctx, dirtForStone).isEmpty(), "a foreign item in an Enhancement Stone slot is rejected");

        List<ItemStack> accessoryInsteadOfLapis = validGrid();
        accessoryInsteadOfLapis.set(3, new ItemStack(ModItems.ACCESSORIES.get(AccessoryDefinition.T1_COMBO)));
        ctx.assertTrue(assemble(ctx, accessoryInsteadOfLapis).isEmpty(), "an accessory is no longer a catalyst ingredient");
        ctx.succeed();
    }

    @GameTest
    public void craftingMenuShowsResultOnlyForValidGridAndConsumesExactlyOneOfEach(GameTestHelper ctx) {
        var player = ctx.makeMockServerPlayerInLevel();

        CraftingMenu shortMenu = menu(ctx, player);
        List<ItemStack> shortGrid = validGrid();
        shortGrid.set(8, ItemStack.EMPTY);
        ctx.assertTrue(fillMenu(shortMenu, shortGrid).isEmpty(), "menu shows no result for a short grid");

        CraftingMenu menu = menu(ctx, player);
        List<ItemStack> grid = validGrid();
        grid.set(8, new ItemStack(ModItems.ENHANCEMENT_STONE, 3)); // surplus stone in one slot: one is consumed, two remain
        assertCatalyst(ctx, fillMenu(menu, grid), "the menu preview");

        menu.clicked(0, 0, ContainerInput.PICKUP, player);
        assertCatalyst(ctx, menu.getCarried(), "the picked-up result");
        for (int slot = 1; slot <= GRID; slot++) {
            ItemStack remaining = menu.getSlot(slot).getItem();
            if (slot == 9) {
                ctx.assertTrue(remaining.is(ModItems.ENHANCEMENT_STONE) && remaining.getCount() == 2,
                        "exactly one Enhancement Stone consumed from the surplus slot");
            } else {
                ctx.assertTrue(remaining.isEmpty(), "grid slot " + slot + " consumed exactly its one ingredient");
            }
        }
        ctx.succeed();
    }
}
