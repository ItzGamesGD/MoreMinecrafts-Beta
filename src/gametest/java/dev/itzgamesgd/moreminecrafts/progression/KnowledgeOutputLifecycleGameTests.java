package dev.itzgamesgd.moreminecrafts.progression;

import java.lang.reflect.Method;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;

/** Action-level tests for provenance production, extraction, and output replacement. */
public final class KnowledgeOutputLifecycleGameTests implements CustomTestMethodInvoker {
    @GameTest
    public void furnaceSmeltCreditsThePlayerWhoTakesTheProducedOutput(GameTestHelper ctx) {
        BlockPos pos = ctx.absolutePos(new BlockPos(2, 2, 2));
        ctx.setBlock(new BlockPos(2, 2, 2), Blocks.FURNACE);
        AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity) ctx.getLevel().getBlockEntity(pos);
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        furnace.setItem(0, new ItemStack(Items.RAW_IRON));
        furnace.setItem(1, new ItemStack(Items.COAL));

        ctx.runAfterDelay(230, () -> {
            ctx.assertTrue(furnace.getItem(2).is(Items.IRON_INGOT), "vanilla furnace completes a raw-iron smelt");
            ItemStack taken = furnace.removeItem(2, 1);
            new Slot(furnace, 2, 0, 0).onTake(player, taken.copy());
            ctx.assertTrue(KnowledgeRuntime.state(player).conditionProgress("ore_types_smelted") == 1,
                    "the result-slot take reaches the installed provenance collector");
            ctx.succeed();
        });
    }

    @GameTest
    public void hopperRemovedFurnaceOutputCannotCreditIdenticalManualReplacement(GameTestHelper ctx) {
        BlockPos furnacePos = ctx.absolutePos(new BlockPos(2, 2, 2));
        BlockPos hopperPos = ctx.absolutePos(new BlockPos(2, 1, 2));
        ctx.setBlock(new BlockPos(2, 2, 2), Blocks.FURNACE);
        ctx.setBlock(new BlockPos(2, 1, 2), Blocks.HOPPER);
        AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity) ctx.getLevel().getBlockEntity(furnacePos);
        var hopper = (net.minecraft.world.level.block.entity.HopperBlockEntity) ctx.getLevel().getBlockEntity(hopperPos);
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        furnace.setItem(0, new ItemStack(Items.RAW_IRON));
        furnace.setItem(1, new ItemStack(Items.COAL));

        ctx.runAfterDelay(230, () -> {
            ItemStack automated = hopper.getItem(0).copy();
            ctx.assertTrue(automated.is(Items.IRON_INGOT), "the hopper extracts the legitimately smelted output");
            ctx.assertTrue(KnowledgeRuntime.state(player).conditionProgress("ore_types_smelted") == 0,
                    "automated extraction has no player attribution");

            hopper.removeItem(0, automated.getCount());
            furnace.setItem(2, automated.copy());
            ItemStack manuallyTaken = furnace.removeItem(2, automated.getCount());
            new Slot(furnace, 2, 0, 0).onTake(player, manuallyTaken.copy());
            ctx.assertTrue(KnowledgeRuntime.state(player).conditionProgress("ore_types_smelted") == 0,
                    "replacement with identical manually inserted output cannot inherit stale provenance");
            ctx.succeed();
        });
    }

    @GameTest
    public void manuallyInsertedFurnaceOutputHasNoSmeltingProvenance(GameTestHelper ctx) {
        BlockPos pos = ctx.absolutePos(new BlockPos(2, 2, 2));
        ctx.setBlock(new BlockPos(2, 2, 2), Blocks.FURNACE);
        AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity) ctx.getLevel().getBlockEntity(pos);
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        furnace.setItem(2, new ItemStack(Items.IRON_INGOT));
        ItemStack taken = furnace.removeItem(2, 1);
        new Slot(furnace, 2, 0, 0).onTake(player, taken.copy());
        ctx.assertTrue(KnowledgeRuntime.state(player).conditionProgress("ore_types_smelted") == 0,
                "a manually inserted output is excluded");
        ctx.succeed();
    }

    @GameTest
    public void manuallyInsertedPotionHasNoBrewingProvenance(GameTestHelper ctx) {
        BlockPos pos = ctx.absolutePos(new BlockPos(2, 2, 2));
        ctx.setBlock(new BlockPos(2, 2, 2), Blocks.BREWING_STAND);
        BrewingStandBlockEntity stand = (BrewingStandBlockEntity) ctx.getLevel().getBlockEntity(pos);
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        stand.setItem(0, new ItemStack(Items.POTION));
        ItemStack taken = stand.removeItem(0, 1);
        new Slot(stand, 0, 0, 0).onTake(player, taken.copy());
        ctx.assertTrue(KnowledgeRuntime.state(player).conditionProgress("potions_brewed") == 0,
                "a manually inserted potion is excluded");
        ctx.succeed();
    }

    @GameTest
    public void vanillaBrewOutputCreditsThePlayerWhoTakesIt(GameTestHelper ctx) {
        BlockPos pos = ctx.absolutePos(new BlockPos(2, 2, 2));
        ctx.setBlock(new BlockPos(2, 2, 2), Blocks.BREWING_STAND);
        BrewingStandBlockEntity stand = (BrewingStandBlockEntity) ctx.getLevel().getBlockEntity(pos);
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        ItemStack waterPotion = new ItemStack(Items.POTION);
        stand.setItem(0, waterPotion.copy());
        stand.setItem(3, new ItemStack(Items.NETHER_WART));
        stand.setItem(4, new ItemStack(Items.BLAZE_POWDER));

        ctx.runAfterDelay(420, () -> {
            ItemStack output = stand.getItem(0).copy();
            ctx.assertTrue(output.is(Items.POTION) && !ItemStack.matches(waterPotion, output),
                    "vanilla brewing transforms the water potion after consuming nether wart");
            ctx.assertTrue(stand.getItem(3).isEmpty(), "the brewing ingredient was consumed by vanilla");
            ItemStack taken = stand.removeItem(0, 1);
            new Slot(stand, 0, 0, 0).onTake(player, taken.copy());
            ctx.assertTrue(KnowledgeRuntime.state(player).conditionProgress("potions_brewed") == 1,
                    "taking the completed brew reaches the installed output-slot collector");
            ctx.succeed();
        });
    }

    @GameTest
    public void removedBrewOutputCannotCreditIdenticalManualReplacement(GameTestHelper ctx) {
        BlockPos pos = ctx.absolutePos(new BlockPos(2, 2, 2));
        ctx.setBlock(new BlockPos(2, 2, 2), Blocks.BREWING_STAND);
        BrewingStandBlockEntity stand = (BrewingStandBlockEntity) ctx.getLevel().getBlockEntity(pos);
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        ItemStack waterPotion = new ItemStack(Items.POTION);
        stand.setItem(0, waterPotion.copy());
        stand.setItem(3, new ItemStack(Items.NETHER_WART));
        stand.setItem(4, new ItemStack(Items.BLAZE_POWDER));

        ctx.runAfterDelay(420, () -> {
            ItemStack produced = stand.getItem(0).copy();
            ctx.assertTrue(produced.is(Items.POTION) && !ItemStack.matches(waterPotion, produced),
                    "the test starts from an actually brewed output");
            ItemStack automated = stand.removeItem(0, 1);
            ctx.assertTrue(KnowledgeRuntime.state(player).conditionProgress("potions_brewed") == 0,
                    "non-player extraction has no player attribution");

            stand.setItem(0, automated.copy());
            ItemStack manuallyTaken = stand.removeItem(0, 1);
            new Slot(stand, 0, 0, 0).onTake(player, manuallyTaken.copy());
            ctx.assertTrue(KnowledgeRuntime.state(player).conditionProgress("potions_brewed") == 0,
                    "identical replacement output cannot inherit stale brew provenance");
            ctx.succeed();
        });
    }

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
