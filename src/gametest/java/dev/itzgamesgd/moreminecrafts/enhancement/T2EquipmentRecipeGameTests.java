package dev.itzgamesgd.moreminecrafts.enhancement;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.registry.ModDataComponents;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerRecipeBook;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * T2 Wave D: the four implemented T2 equipment recipes (section 18.3) through the real crafting menu, their
 * core-gated recipe-book unlocks (section 18.5), and non-inheritance of the base weapon's state. Wind Sword and Wind Bow
 * stay BLOCKED_DEPENDENCY until Sky Feather is registered.
 */
public final class T2EquipmentRecipeGameTests {
    private record Case(String name, Item result, String[] rows, Map<Character, Item> key, Item core) {}

    private static final List<Case> CASES = List.of(
            new Case("flame_sword", ModItems.FLAME_SWORD, new String[] {"G H", " A ", "E  "}, Map.of('G', Items.GOLDEN_SWORD,
                    'H', ModItems.BURNING_HEART, 'A', ModItems.ADAMANTIUM_INGOT, 'E', ModItems.ETHERIUM_INGOT), ModItems.BURNING_HEART),
            new Case("flowing_water_sword", ModItems.FLOWING_WATER_SWORD, new String[] {"SPP", "PCH", "PPE"},
                    Map.of('S', Items.DIAMOND_SWORD, 'P', Items.PRISMARINE_SHARD, 'C', ModItems.HEART_OF_CURRENTS,
                            'H', Items.HEART_OF_THE_SEA, 'E', ModItems.ETHERIUM_INGOT), ModItems.HEART_OF_CURRENTS),
            new Case("wind_sword", ModItems.WIND_SWORD, new String[] {"FTF", "BSB", "BEB"}, Map.of('F', ModItems.SKY_FEATHER,
                    'T', ModItems.EYE_OF_TYPHOON, 'B', Items.BREEZE_ROD, 'S', Items.IRON_SWORD, 'E', ModItems.ETHERIUM_INGOT),
                    ModItems.EYE_OF_TYPHOON),
            new Case("wind_archers_bow", ModItems.WIND_ARCHERS_BOW, new String[] {"FGF", "BWB", "BEB"}, Map.of('F', ModItems.SKY_FEATHER,
                    'G', ModItems.GALE_SHARD, 'B', Items.BREEZE_ROD, 'W', Items.BOW, 'E', ModItems.ETHERIUM_INGOT), ModItems.GALE_SHARD),
            new Case("earth_mace", ModItems.EARTH_MACE, new String[] {"DHD", " A ", " E "}, Map.of('D', Items.DIAMOND_BLOCK,
                    'H', Items.HEAVY_CORE, 'A', ModItems.ANCIENT_HEART, 'E', ModItems.ETHERIUM_INGOT), ModItems.ANCIENT_HEART),
            new Case("lightning_sword", ModItems.LIGHTNING_SWORD, new String[] {" S ", "GTN", " E "}, Map.of('S', Items.DIAMOND_SWORD,
                    'G', Items.GOLD_BLOCK, 'T', ModItems.HEART_OF_THUNDER, 'N', Items.NETHERITE_INGOT, 'E', ModItems.ETHERIUM_INGOT),
                    ModItems.HEART_OF_THUNDER));

    private static ResourceKey<Recipe<?>> recipeKey(String name) {
        return ResourceKey.create(Registries.RECIPE, ModIds.id(name));
    }

    private static CraftingMenu menu(GameTestHelper ctx, ServerPlayer player) {
        return new CraftingMenu(1, player.getInventory(), ContainerLevelAccess.create(ctx.getLevel(), ctx.absolutePos(new BlockPos(1, 1, 1))));
    }

    /** Fills the 3x3 grid (menu slots 1..9) and returns the crafted result slot content. */
    private static ItemStack craft(CraftingMenu menu, List<ItemStack> grid) {
        for (int i = 0; i < 9; i++) menu.getSlot(1 + i).set(grid.get(i).copy());
        menu.slotsChanged(menu.getSlot(1).container);
        return menu.getSlot(0).getItem();
    }

    private static List<ItemStack> grid(Case c, String[] rows) {
        List<ItemStack> cells = new ArrayList<>();
        for (String row : rows) for (char ch : row.toCharArray()) cells.add(ch == ' ' ? ItemStack.EMPTY : new ItemStack(c.key().get(ch)));
        return cells;
    }

    private static String[] mirrored(String[] rows) {
        String[] result = new String[3];
        for (int i = 0; i < 3; i++) result[i] = new StringBuilder(rows[i]).reverse().toString();
        return result;
    }

    private static String[] flipped(String[] rows) {
        return new String[] {rows[2], rows[1], rows[0]};
    }

    private static String[] rotated(String[] rows) {
        String[] result = new String[3];
        for (int r = 0; r < 3; r++) {
            StringBuilder row = new StringBuilder();
            for (int c = 0; c < 3; c++) row.append(rows[2 - c].charAt(r));
            result[r] = row.toString();
        }
        return result;
    }

    private static boolean crafts(CraftingMenu menu, Case c, List<ItemStack> cells) {
        return craft(menu, cells).is(c.result());
    }

    @GameTest
    public void exactLayoutsCraftThroughTheRealMenuWithoutAnyUnlock(GameTestHelper ctx) {
        for (Case c : CASES) {
            ServerPlayer player = EnhancementTransactionGameTests.survival(ctx);
            ctx.assertTrue(!player.getRecipeBook().contains(recipeKey(c.name())), c.name() + " starts locked");
            CraftingMenu menu = menu(ctx, player);
            ItemStack result = craft(menu, grid(c, c.rows())).copy();
            ctx.assertTrue(result.is(c.result()) && result.getCount() == 1, c.name() + " crafts manually while locked");
            ctx.assertTrue(result.has(DataComponents.UNBREAKABLE) && !result.isDamageableItem(), c.name() + " result keeps infinite durability");
            ctx.assertTrue(EnhancementOperation.level(result) == 0 && EnchantmentHelper.getEnchantmentsForCrafting(result).isEmpty()
                    && EnhancementCatalogProbe.maxLevel(result) == 20, c.name() + " result is fresh and stays +20 enhanceable");
            menu.clicked(0, 0, ContainerInput.PICKUP, player);
            ctx.assertTrue(menu.getCarried().is(c.result()), c.name() + " result can be taken");
            for (int i = 1; i <= 9; i++) ctx.assertTrue(menu.getSlot(i).getItem().isEmpty(), c.name() + " consumes one of each ingredient (slot " + i + ")");
        }
        ctx.succeed();
    }

    @GameTest
    public void horizontalMirrorCraftsButVerticalFlipAndRotationDoNot(GameTestHelper ctx) {
        ServerPlayer player = EnhancementTransactionGameTests.survival(ctx);
        for (Case c : CASES) {
            ctx.assertTrue(crafts(menu(ctx, player), c, grid(c, mirrored(c.rows()))), c.name() + " accepts the horizontal mirror");
            ctx.assertTrue(!crafts(menu(ctx, player), c, grid(c, flipped(c.rows()))), c.name() + " rejects a vertical flip");
            ctx.assertTrue(!crafts(menu(ctx, player), c, grid(c, rotated(c.rows()))), c.name() + " rejects a 90 degree rotation");
            ctx.assertTrue(!crafts(menu(ctx, player), c, grid(c, rotated(rotated(c.rows())))), c.name() + " rejects a 180 degree rotation");
        }
        ctx.succeed();
    }

    @GameTest
    public void missingWrongOrExtraIngredientsAreRejected(GameTestHelper ctx) {
        ServerPlayer player = EnhancementTransactionGameTests.survival(ctx);
        for (Case c : CASES) {
            List<ItemStack> full = grid(c, c.rows());
            for (int i = 0; i < 9; i++) {
                if (full.get(i).isEmpty()) {
                    List<ItemStack> extra = new ArrayList<>(full);
                    extra.set(i, new ItemStack(Items.DIRT));
                    ctx.assertTrue(!crafts(menu(ctx, player), c, extra), c.name() + " rejects an extra item in cell " + i);
                    continue;
                }
                List<ItemStack> missing = new ArrayList<>(full);
                missing.set(i, ItemStack.EMPTY);
                ctx.assertTrue(!crafts(menu(ctx, player), c, missing), c.name() + " rejects a missing ingredient in cell " + i);
                List<ItemStack> wrong = new ArrayList<>(full);
                wrong.set(i, new ItemStack(Items.STICK));
                ctx.assertTrue(!crafts(menu(ctx, player), c, wrong), c.name() + " rejects a wrong ingredient in cell " + i);
            }
        }
        ctx.succeed();
    }

    @GameTest
    public void damagedEnchantedEnhancedBaseWeaponsAreAcceptedAndNotInherited(GameTestHelper ctx) {
        ServerPlayer player = EnhancementTransactionGameTests.survival(ctx);
        var holder = ctx.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS);
        for (Case c : CASES) {
            List<ItemStack> cells = grid(c, c.rows());
            for (ItemStack cell : cells) {
                if (cell.isEmpty() || !(cell.getItem() == Items.GOLDEN_SWORD || cell.getItem() == Items.DIAMOND_SWORD
                        || cell.getItem() == Items.IRON_SWORD || cell.getItem() == Items.BOW)) continue;
                cell.set(DataComponents.DAMAGE, 40);
                EnchantmentHelper.updateEnchantments(cell, enchantments -> enchantments.set(holder, 5));
                cell.set(ModDataComponents.ENHANCEMENT_LEVEL, 12);
                cell.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Old Blade"));
            }
            ItemStack result = craft(menu(ctx, player), cells);
            ctx.assertTrue(result.is(c.result()), c.name() + " crafts from a damaged, enchanted, enhanced base weapon");
            ctx.assertTrue(EnchantmentHelper.getEnchantmentsForCrafting(result).isEmpty(), c.name() + " does not inherit enchantments");
            ctx.assertTrue(EnhancementOperation.level(result) == 0, c.name() + " does not inherit Enhancement");
            ctx.assertTrue(result.getDamageValue() == 0 && !result.isDamaged() && !result.has(DataComponents.CUSTOM_NAME),
                    c.name() + " does not inherit damage or name");
            ctx.assertTrue(result.has(DataComponents.UNBREAKABLE), c.name() + " is infinite-durability");
        }
        ctx.succeed();
    }

    @GameTest
    public void eachCoreUnlocksOnlyItsOwnRecipeAndTheUnlockPersists(GameTestHelper ctx) {
        ServerPlayer player = EnhancementTransactionGameTests.survival(ctx);
        List<ResourceKey<Recipe<?>>> others = List.of(recipeKey("etherium_ingot"), recipeKey("gale_shard"),
                recipeKey("etherium_block"), recipeKey("etherium_shard_from_smelting"));
        for (ResourceKey<Recipe<?>> other : others) ctx.assertTrue(!player.getRecipeBook().contains(other), other.identifier() + " starts locked");
        ServerRecipeBook book = player.getRecipeBook();
        for (Case c : CASES) {
            ctx.assertTrue(!book.contains(recipeKey(c.name())), c.name() + " locked before its core");
            player.getInventory().add(new ItemStack(c.core()));
            CriteriaTriggers.INVENTORY_CHANGED.trigger(player, player.getInventory(), new ItemStack(c.core()));
            ctx.assertTrue(book.contains(recipeKey(c.name())), c.name() + " unlocks on first acquisition of " + BuiltInRegistries.ITEM.getKey(c.core()));
            for (Case other : CASES) {
                boolean shouldBeUnlocked = CASES.indexOf(other) <= CASES.indexOf(c);
                ctx.assertTrue(book.contains(recipeKey(other.name())) == shouldBeUnlocked,
                        "only cores acquired so far unlock: " + other.name() + " after " + c.name());
            }
            for (ResourceKey<Recipe<?>> other : others) ctx.assertTrue(!book.contains(other), c.name() + " core does not unlock " + other.identifier());
        }

        // Reconnect: the saved recipe book restores the unlocks, losing none.
        var ops = RegistryOps.create(NbtOps.INSTANCE, ctx.getLevel().registryAccess());
        var saved = ServerRecipeBook.Packed.CODEC.encodeStart(ops, book.pack()).getOrThrow();
        for (Case c : CASES) book.remove(recipeKey(c.name()));
        for (Case c : CASES) ctx.assertTrue(!book.contains(recipeKey(c.name())), "simulated fresh connection starts empty");
        book.loadUntrusted(ServerRecipeBook.Packed.CODEC.parse(ops, saved).getOrThrow(), key -> true);
        for (Case c : CASES) ctx.assertTrue(book.contains(recipeKey(c.name())), c.name() + " unlock survives save/load");
        ctx.succeed();
    }

    @GameTest
    public void skyFeatherIsRegisteredAndSixRecipesLoad(GameTestHelper ctx) {
        ctx.assertTrue(BuiltInRegistries.ITEM.getValue(Identifier.parse("moreminecrafts:sky_feather")) == ModItems.SKY_FEATHER
                && ModItems.SKY_FEATHER != Items.AIR, "Sky Feather is registered as moreminecrafts:sky_feather");
        ctx.assertTrue(CASES.size() == 6, "all six T2 equipment recipes are covered");
        for (Case c : CASES) {
            ctx.assertTrue(ctx.getLevel().recipeAccess().byKey(recipeKey(c.name())).isPresent(), c.name() + " recipe is loaded");
        }
        ctx.succeed();
    }

    @GameTest
    public void windPairHasIndependentUnlockConditions(GameTestHelper ctx) {
        ServerPlayer player = EnhancementTransactionGameTests.survival(ctx);
        ServerRecipeBook book = player.getRecipeBook();
        player.getInventory().add(new ItemStack(ModItems.GALE_SHARD));
        CriteriaTriggers.INVENTORY_CHANGED.trigger(player, player.getInventory(), new ItemStack(ModItems.GALE_SHARD));
        ctx.assertTrue(book.contains(recipeKey("wind_archers_bow")) && !book.contains(recipeKey("wind_sword")),
                "Gale Shard unlocks only the wind bow");
        player.getInventory().add(new ItemStack(ModItems.SKY_FEATHER, 2));
        CriteriaTriggers.INVENTORY_CHANGED.trigger(player, player.getInventory(), new ItemStack(ModItems.SKY_FEATHER, 2));
        ctx.assertTrue(!book.contains(recipeKey("wind_sword")), "Sky Feather alone unlocks nothing");
        player.getInventory().add(new ItemStack(ModItems.EYE_OF_TYPHOON));
        CriteriaTriggers.INVENTORY_CHANGED.trigger(player, player.getInventory(), new ItemStack(ModItems.EYE_OF_TYPHOON));
        ctx.assertTrue(book.contains(recipeKey("wind_sword")), "Eye of the Typhoon unlocks the wind sword");
        ctx.succeed();
    }

    /** Keeps the +20 check on the production catalog without depending on Enhancement internals. */
    private static final class EnhancementCatalogProbe {
        static int maxLevel(ItemStack stack) {
            return EnhancementCatalog.definition(stack.getItem())
                    .map(d -> d.category().maximumLevel()).orElse(0);
        }
    }
}
