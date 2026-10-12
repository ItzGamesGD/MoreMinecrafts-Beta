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
        CraftingMenu menu = new CraftingMenu(1, player.getInventory());

        for (int craft = 0; craft < 5; craft++) {
            menu.getSlot(1).set(new ItemStack(Items.OAK_PLANKS));
            menu.getSlot(2).set(new ItemStack(Items.OAK_PLANKS));
            menu.getSlot(3).set(new ItemStack(Items.OAK_PLANKS));
            menu.getSlot(5).set(new ItemStack(Items.STICK));
            menu.getSlot(8).set(new ItemStack(Items.STICK));
            menu.slotsChanged(menu.getCraftSlots());
            ItemStack output = menu.getSlot(0).getItem().copy();
            ctx.assertTrue(output.is(Items.WOODEN_PICKAXE), "vanilla recipe computes a wooden pickaxe output");
            menu.getSlot(0).onTake(player, output);
            ctx.assertTrue(KnowledgeRuntime.state(player).conditionProgress("tool_types_crafted") == craft + 1,
                    "taking each actual result-slot output advances K14 once");
        }

        ctx.assertTrue(KnowledgeRuntime.knows(player, 14), "five result-slot tools unlock K14");
        ctx.succeed();
    }

    @GameTest
    public void k14CreativeResultSlotTakesDoNotAdvanceProgress(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.CREATIVE);
        CraftingMenu menu = new CraftingMenu(1, player.getInventory());
        menu.getSlot(1).set(new ItemStack(Items.OAK_PLANKS));
        menu.getSlot(2).set(new ItemStack(Items.OAK_PLANKS));
        menu.getSlot(3).set(new ItemStack(Items.OAK_PLANKS));
        menu.getSlot(5).set(new ItemStack(Items.STICK));
        menu.getSlot(8).set(new ItemStack(Items.STICK));
        menu.slotsChanged(menu.getCraftSlots());
        ItemStack output = menu.getSlot(0).getItem().copy();
        ctx.assertTrue(output.is(Items.WOODEN_PICKAXE), "creative player still has the vanilla craft result");
        menu.getSlot(0).onTake(player, output);
        ctx.assertTrue(KnowledgeRuntime.state(player).conditionProgress("tool_types_crafted") == 0,
                "creative result-slot take is excluded from K14");
        ctx.succeed();
    }

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
