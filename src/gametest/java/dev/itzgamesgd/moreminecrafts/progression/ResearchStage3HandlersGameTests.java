package dev.itzgamesgd.moreminecrafts.progression;

import java.lang.reflect.Method;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;

/** Server integration tests for Stage 3 handlers (.13, .14, .46, .63, combat V4, tool switch). */
public final class ResearchStage3HandlersGameTests implements CustomTestMethodInvoker {
    @GameTest
    public void cropCultivationAndMagicalMaterialBlockDropsArePurchaseGated(GameTestHelper ctx) {
        ServerPlayer unpaid = ctx.makeMockServerPlayerInLevel();
        ServerPlayer paid = ctx.makeMockServerPlayerInLevel();
        unpaid.setGameMode(GameType.SURVIVAL);
        paid.setGameMode(GameType.SURVIVAL);
        KnowledgeRuntime.updateState(paid, ResearchProgressState.empty()
                .with("research.v3.research.14", 3)
                .with("research.v3.research.63", 3));

        var matureWheat = Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7);
        var baseWheat = List.of(new ItemStack(Items.WHEAT, 1));
        ctx.assertTrue(ResearchContentRuntime.applyCropCultivationResearch(unpaid, matureWheat, baseWheat, () -> 0.0D).size() == 1,
                "unpurchased player gets no extra crop yield");
        var paidWheat = ResearchContentRuntime.applyCropCultivationResearch(paid, matureWheat, baseWheat, () -> 0.0D);
        int wheatCount = paidWheat.stream().filter(s -> s.is(Items.WHEAT)).mapToInt(ItemStack::getCount).sum();
        ctx.assertTrue(wheatCount == 3, "L3 Crop Cultivation adds +2 wheat (got " + wheatCount + ")");

        var netherWart = Blocks.NETHER_WART.defaultBlockState();
        var baseWart = List.of(new ItemStack(Items.NETHER_WART, 2));
        var paidWart = ResearchContentRuntime.applyMagicalMaterialBlockResearch(paid, netherWart, baseWart, () -> 0.0D);
        int wartCount = paidWart.stream().filter(s -> s.is(Items.NETHER_WART)).mapToInt(ItemStack::getCount).sum();
        ctx.assertTrue(wartCount == 3, "L3 Magical Material Study adds +1 tagged brewing ingredient (got " + wartCount + ")");
        ctx.succeed();
    }

    @GameTest
    public void rareMobResearch46AppliesToTaggedRareMobsOnly(GameTestHelper ctx) {
        ServerPlayer paid = ctx.makeMockServerPlayerInLevel();
        KnowledgeRuntime.updateState(paid, ResearchProgressState.empty()
                .with("research.v3.research.46", 3)
                .with("knowledge.51", 1));
        var evoker = ctx.spawn(EntityTypes.EVOKER, new BlockPos(2, 2, 2));
        var wither = ctx.spawn(EntityTypes.WITHER, new BlockPos(4, 2, 2));
        ctx.assertTrue(Math.abs(ResearchEcologyDamage.bonus(paid, evoker) - 0.17D) < 1.0E-8D,
                "L3 rare mob (+12%) + knowledge 51 (+5%) = +17%");
        ctx.assertTrue(Math.abs(ResearchEcologyDamage.bonus(paid, wither) - 0.0D) < 1.0E-8D,
                "bosses are excluded from rare mob research bonus");
        ctx.succeed();
    }

    @GameTest
    public void chestSortConsolidatesAndSortsLosslesslyOnlyWhenResearched(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        SimpleContainer chest = new SimpleContainer(27);
        chest.setItem(0, new ItemStack(Items.COBBLESTONE, 10));
        chest.setItem(5, new ItemStack(Items.DIAMOND_PICKAXE, 1));
        chest.setItem(10, new ItemStack(Items.COBBLESTONE, 12));
        chest.setItem(15, new ItemStack(Items.BREAD, 5));
        player.containerMenu = ChestMenu.threeRows(91, player.getInventory(), chest);

        ctx.assertTrue(!ChestSortRuntime.sortOpenChest(player, ChestSortRules.MODE_CATEGORY),
                "unpurchased player cannot sort chest");

        KnowledgeRuntime.updateState(player, ResearchProgressState.empty().with("research.v3.research.13", 1));
        ctx.assertTrue(ChestSortRuntime.sortOpenChest(player, ChestSortRules.MODE_CATEGORY),
                "purchased v3.research.13 player sorts open chest");
        ctx.assertTrue(chest.getItem(0).is(Items.DIAMOND_PICKAXE), "tools sort first in category mode");
        ctx.assertTrue(chest.getItem(1).is(Items.COBBLESTONE) && chest.getItem(1).getCount() == 22,
                "identical cobblestone stacks merge losslessly to 22");
        ctx.assertTrue(chest.getItem(2).is(Items.BREAD) && chest.getItem(2).getCount() == 5,
                "food sorts after blocks in category mode");
        ctx.succeed();
    }

    @GameTest
    public void automaticToolSwitchSelectsBestHotbarPickaxeOnStone(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.getInventory().setItem(0, new ItemStack(Items.STICK));
        player.getInventory().setItem(3, new ItemStack(Items.DIAMOND_PICKAXE));
        player.getInventory().setSelectedSlot(0);

        ResearchCombatV4Runtime.tryAutoToolSwitch(player, Blocks.STONE.defaultBlockState());
        ctx.assertTrue(player.getInventory().getSelectedSlot() == 0,
                "unpurchased player does not auto-switch tools");

        KnowledgeRuntime.updateState(player, ResearchProgressState.empty().with("research.automatic_tool_switch", 1));
        ResearchCombatV4Runtime.tryAutoToolSwitch(player, Blocks.STONE.defaultBlockState());
        ctx.assertTrue(player.getInventory().getSelectedSlot() == 3,
                "purchased automatic_tool_switch switches to highest-efficiency hotbar pickaxe");
        ctx.succeed();
    }

    @Override
    public void invokeTestMethod(GameTestHelper ctx, Method method) throws ReflectiveOperationException {
        method.invoke(this, ctx);
    }
}
