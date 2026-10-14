package dev.itzgamesgd.moreminecrafts.progression;

import java.lang.reflect.Method;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/** Action-level tests for the K8 block-break observer and K14 crafting result observer. */
public final class KnowledgeAcquisitionGameTests implements CustomTestMethodInvoker {
    @GameTest
    public void k8CountsDistinctAllowlistedRocksFromSuccessfulSurvivalMining(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
        for (int i = 0; i < 8; i++) {
            BlockPos pos = ctx.absolutePos(new BlockPos(1 + i, 1, 1));
            ctx.getLevel().setBlock(pos, KnowledgeEligibleRosters.rockTypes().get(i).block().defaultBlockState(), 3);
            ctx.assertTrue(player.gameMode.destroyBlock(pos), "survival player mines eligible rock " + i);
            ctx.assertTrue(KnowledgeRuntime.state(player).conditionProgress("rock_types_mined") == i + 1,
                    "each distinct eligible rock type counts once");
        }
        ctx.assertTrue(KnowledgeRuntime.knows(player, 8), "eight distinct mined rock types unlock K8");

        BlockPos duplicate = ctx.absolutePos(new BlockPos(1, 2, 1));
        ctx.getLevel().setBlock(duplicate, Blocks.GRANITE.defaultBlockState(), 3);
        ctx.assertTrue(player.gameMode.destroyBlock(duplicate), "same rock type can be mined again");
        ctx.assertTrue(KnowledgeRuntime.state(player).conditionProgress("rock_types_mined") == 8,
                "duplicate block type does not add progress");

        BlockPos creativePos = ctx.absolutePos(new BlockPos(2, 2, 1));
        ServerPlayer creative = ctx.makeMockServerPlayerInLevel();
        creative.setGameMode(GameType.CREATIVE);
        ctx.getLevel().setBlock(creativePos, Blocks.BLACKSTONE.defaultBlockState(), 3);
        ctx.assertTrue(creative.gameMode.destroyBlock(creativePos), "creative player can remove the test block");
        ctx.assertTrue(KnowledgeRuntime.state(creative).conditionProgress("rock_types_mined") == 0,
                "creative block removal is not mining progress");

        BlockPos excluded = ctx.absolutePos(new BlockPos(3, 2, 1));
        ctx.getLevel().setBlock(excluded, Blocks.STONE_BRICKS.defaultBlockState(), 3);
        ctx.assertTrue(player.gameMode.destroyBlock(excluded), "unlisted stone product can be mined");
        ctx.assertTrue(KnowledgeRuntime.state(player).conditionProgress("rock_types_mined") == 8,
                "unlisted blocks do not count");
        BlockPos empty = ctx.absolutePos(new BlockPos(4, 2, 1));
        player.gameMode.destroyBlock(empty);
        ctx.assertTrue(KnowledgeRuntime.state(player).conditionProgress("rock_types_mined") == 8,
                "failed mining attempt adds no progress");
        ctx.succeed();
    }

    @GameTest
    public void k10PickaxeDurabilityCountsNestedVeinMiningBreaks(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
        EnchantmentHelper.updateEnchantments(pickaxe, enchantments -> enchantments.set(
                T1Operations.enchantment(player, "moreminecrafts:vein_mining").orElseThrow(), 1));
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, pickaxe);

        BlockPos origin = ctx.absolutePos(new BlockPos(1, 1, 1));
        BlockPos adjacent = ctx.absolutePos(new BlockPos(2, 1, 1));
        ctx.getLevel().setBlock(origin, Blocks.COPPER_ORE.defaultBlockState(), 3);
        ctx.getLevel().setBlock(adjacent, Blocks.COPPER_ORE.defaultBlockState(), 3);

        ctx.assertTrue(player.gameMode.destroyBlock(origin), "survival player breaks the vein origin");
        ctx.assertTrue(ctx.getLevel().getBlockState(adjacent).isAir(),
                "Vein Mining performs its nested adjacent-ore break");
        ctx.assertTrue(pickaxe.getDamageValue() == 2, "Vein Mining applies aggregate durability once for two blocks");
        ctx.assertTrue(KnowledgeRuntime.state(player).conditionProgress("pickaxe_durability_used") == 2,
                "K10 observes the outer break's durability after nested break callbacks complete");
        ctx.succeed();
    }

    @GameTest
    public void k14ToolCraftCollectorKeepsTwoPlayersProgressSeparate(GameTestHelper ctx) {
        var first = ctx.makeMockServerPlayerInLevel();
        var second = ctx.makeMockServerPlayerInLevel();
        first.setGameMode(GameType.SURVIVAL);
        second.setGameMode(GameType.SURVIVAL);
        for (int i = 0; i < 5; i++)
            KnowledgeEventCollectors.vanillaToolCrafted(first, new ItemStack(Items.WOODEN_PICKAXE));
        for (int i = 0; i < 2; i++)
            KnowledgeEventCollectors.vanillaToolCrafted(second, new ItemStack(Items.IRON_SWORD));
        ctx.assertTrue(KnowledgeRuntime.state(first).conditionProgress("tool_types_crafted") == 5,
                "first player receives the five crafted tools");
        ctx.assertTrue(KnowledgeRuntime.state(second).conditionProgress("tool_types_crafted") == 2,
                "second player receives only their own crafted tools");
        ctx.succeed();
    }

    @GameTest
    public void k14CountsToolsTakenFromTheVanillaCraftingResultSlot(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        CraftingMenu menu = craftingMenu(ctx, player);

        for (int craft = 0; craft < 5; craft++) {
            prepareWoodenPickaxe(menu, 1, 1);
            menu.clicked(0, 0, ContainerInput.PICKUP, player);
            ItemStack output = menu.getCarried().copy();
            ctx.assertTrue(output.is(Items.WOODEN_PICKAXE), "normal server menu click takes the vanilla result");
            ctx.assertTrue(player.getInventory().add(output), "taken result transfers into player inventory");
            menu.setCarried(ItemStack.EMPTY);
            ctx.assertTrue(KnowledgeRuntime.state(player).conditionProgress("tool_types_crafted") == craft + 1,
                    "one successful result take advances K14 exactly once");
        }

        ctx.assertTrue(KnowledgeRuntime.knows(player, 14), "five result-slot tools unlock K14");
        ctx.succeed();
    }

    @GameTest
    public void k14CreativeResultSlotTakesDoNotAdvanceProgress(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.CREATIVE);
        CraftingMenu menu = craftingMenu(ctx, player);
        prepareWoodenPickaxe(menu, 1, 1);
        ItemStack output = menu.getSlot(0).getItem().copy();
        ctx.assertTrue(output.is(Items.WOODEN_PICKAXE), "creative player still has the vanilla craft result");
        menu.clicked(0, 0, ContainerInput.PICKUP, player);
        ctx.assertTrue(player.getStats().getValue(Stats.ITEM_CRAFTED.get(Items.WOODEN_PICKAXE)) > 0,
                "Creative result take reaches vanilla crafted statistics");
        ctx.assertTrue(KnowledgeRuntime.state(player).conditionProgress("tool_types_crafted") == 0,
                "creative result-slot take is excluded from K14");
        ctx.succeed();
    }

    @GameTest
    public void k14ShiftClickAndBulkResultTakesCountEachOutputOnce(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        CraftingMenu menu = craftingMenu(ctx, player);
        prepareWoodenPickaxe(menu, 4, 4);

        for (int crafted = 1; crafted <= 4; crafted++) {
            ItemStack moved = menu.quickMoveStack(player, 0);
            ctx.assertTrue(moved.is(Items.WOODEN_PICKAXE), "shift-click transfers one vanilla result");
            ctx.assertTrue(KnowledgeRuntime.state(player).conditionProgress("tool_types_crafted") == crafted,
                    "bulk shift-click awards exactly once per output take");
        }
        ctx.assertTrue(menu.quickMoveStack(player, 0).isEmpty(), "exhausted ingredients produce no further result");
        ctx.assertTrue(KnowledgeRuntime.state(player).conditionProgress("tool_types_crafted") == 4,
                "an empty follow-up cannot duplicate the last result credit");
        ctx.succeed();
    }

    @GameTest
    public void k14AdventureCraftCountsAndSpectatorTakeIsRejected(GameTestHelper ctx) {
        ServerPlayer adventure = ctx.makeMockServerPlayerInLevel();
        adventure.setGameMode(GameType.ADVENTURE);
        CraftingMenu adventureMenu = craftingMenu(ctx, adventure);
        prepareWoodenPickaxe(adventureMenu, 1, 1);
        adventureMenu.clicked(0, 0, ContainerInput.PICKUP, adventure);
        ctx.assertTrue(KnowledgeRuntime.state(adventure).conditionProgress("tool_types_crafted") == 1,
                "Adventure crafting remains eligible under the existing Creative-only exclusion");

        ServerPlayer spectator = ctx.makeMockServerPlayerInLevel();
        spectator.setGameMode(GameType.SPECTATOR);
        CraftingMenu spectatorMenu = craftingMenu(ctx, spectator);
        prepareWoodenPickaxe(spectatorMenu, 1, 1);
        // This direct menu call is a fail-closed control. Vanilla's server packet handler
        // rejects spectator menu clicks before this result-slot callback is reachable.
        spectatorMenu.clicked(0, 0, ContainerInput.PICKUP, spectator);
        ctx.assertTrue(KnowledgeRuntime.state(spectator).conditionProgress("tool_types_crafted") == 0,
                "Spectator cannot receive K14 even if a result take is invoked internally");
        ctx.succeed();
    }

    @GameTest(maxTicks = 160)
    public void k14CreativeCraftCannotBecomeProgressDuringPeriodicRefresh(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.CREATIVE);
        player.setAttached(KnowledgeRuntime.STATE, ResearchProgressState.empty()
                .with("condition.tool_types_crafted", 3).with("knowledge.14", 1));
        player.awardStat(Stats.ITEM_CRAFTED.get(Items.WOODEN_PICKAXE), 12);

        CraftingMenu menu = craftingMenu(ctx, player);
        prepareWoodenPickaxe(menu, 1, 1);
        menu.clicked(0, 0, ContainerInput.PICKUP, player);
        ctx.assertTrue(player.getStats().getValue(Stats.ITEM_CRAFTED.get(Items.WOODEN_PICKAXE)) > 12,
                "the Creative result take increments the ambiguous vanilla statistic");
        ctx.assertTrue(KnowledgeRuntime.state(player).conditionProgress("tool_types_crafted") == 3,
                "the event preserves existing partial K14 progress");

        ctx.runAfterDelay(110, () -> {
            ctx.assertTrue(KnowledgeRuntime.state(player).conditionProgress("tool_types_crafted") == 3,
                    "periodic refresh cannot import Creative or ambiguous crafted statistics");
            ctx.assertTrue(KnowledgeRuntime.knows(player, 14),
                    "periodic refresh preserves an already unlocked K14");
            ctx.succeed();
        });
    }

    private static CraftingMenu craftingMenu(GameTestHelper ctx, ServerPlayer player) {
        return new CraftingMenu(1, player.getInventory(),
                net.minecraft.world.inventory.ContainerLevelAccess.create(ctx.getLevel(),
                        ctx.absolutePos(new BlockPos(1, 1, 1))));
    }

    private static void prepareWoodenPickaxe(CraftingMenu menu, int plankCount, int stickCount) {
        menu.getSlot(1).set(new ItemStack(Items.OAK_PLANKS, plankCount));
        menu.getSlot(2).set(new ItemStack(Items.OAK_PLANKS, plankCount));
        menu.getSlot(3).set(new ItemStack(Items.OAK_PLANKS, plankCount));
        menu.getSlot(5).set(new ItemStack(Items.STICK, stickCount));
        menu.getSlot(8).set(new ItemStack(Items.STICK, stickCount));
        menu.slotsChanged(menu.getSlot(1).container);
    }

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
