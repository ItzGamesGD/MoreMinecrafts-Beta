package dev.itzgamesgd.moreminecrafts.progression;

import java.lang.reflect.Method;
import net.minecraft.nbt.CompoundTag;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Action-level tests for provenance production, extraction, and output replacement. */
public final class KnowledgeOutputLifecycleGameTests implements CustomTestMethodInvoker {
    @GameTest(maxTicks = 240)
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

    @GameTest(maxTicks = 240)
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
        AbstractFurnaceBlockEntity reloaded = new FurnaceBlockEntity(pos, Blocks.FURNACE.defaultBlockState());
        loadAdditional(reloaded, saveAdditional(furnace, ctx), ctx);
        ItemStack taken = reloaded.removeItem(2, 1);
        new Slot(reloaded, 2, 0, 0).onTake(player, taken.copy());
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
        BrewingStandBlockEntity reloaded = new BrewingStandBlockEntity(pos,
                Blocks.BREWING_STAND.defaultBlockState());
        loadAdditional(reloaded, saveAdditional(stand, ctx), ctx);
        ItemStack taken = reloaded.removeItem(0, 1);
        new Slot(reloaded, 0, 0, 0).onTake(player, taken.copy());
        ctx.assertTrue(KnowledgeRuntime.state(player).conditionProgress("potions_brewed") == 0,
                "a manually inserted potion is excluded");
        ctx.succeed();
    }

    @GameTest(maxTicks = 430)
    public void vanillaBrewOutputCreditsThePlayerWhoTakesIt(GameTestHelper ctx) {
        BlockPos pos = ctx.absolutePos(new BlockPos(2, 2, 2));
        ctx.setBlock(new BlockPos(2, 2, 2), Blocks.BREWING_STAND);
        BrewingStandBlockEntity stand = (BrewingStandBlockEntity) ctx.getLevel().getBlockEntity(pos);
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        ItemStack waterPotion = PotionContents.createItemStack(Items.POTION, Potions.WATER);
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

    @GameTest(maxTicks = 430)
    public void removedBrewOutputCannotCreditIdenticalManualReplacement(GameTestHelper ctx) {
        BlockPos pos = ctx.absolutePos(new BlockPos(2, 2, 2));
        ctx.setBlock(new BlockPos(2, 2, 2), Blocks.BREWING_STAND);
        BrewingStandBlockEntity stand = (BrewingStandBlockEntity) ctx.getLevel().getBlockEntity(pos);
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        ItemStack waterPotion = PotionContents.createItemStack(Items.POTION, Potions.WATER);
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

    @GameTest(maxTicks = 720)
    public void furnaceProvenanceSurvivesSaveLoadAndPartialTakesStayWithEachPlayer(GameTestHelper ctx) {
        BlockPos pos = ctx.absolutePos(new BlockPos(2, 2, 2));
        ctx.setBlock(new BlockPos(2, 2, 2), Blocks.FURNACE);
        AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity) ctx.getLevel().getBlockEntity(pos);
        ServerPlayer first = ctx.makeMockServerPlayerInLevel();
        ServerPlayer second = ctx.makeMockServerPlayerInLevel();
        furnace.setItem(0, new ItemStack(Items.RAW_IRON, 3));
        furnace.setItem(1, new ItemStack(Items.COAL, 3));

        ctx.runAfterDelay(680, () -> {
            ctx.assertTrue(furnace.getItem(2).is(Items.IRON_INGOT) && furnace.getItem(2).getCount() == 3,
                    "three ingots are products of completed vanilla furnace burns");
            AbstractFurnaceBlockEntity reloaded = new FurnaceBlockEntity(pos, Blocks.FURNACE.defaultBlockState());
            loadAdditional(reloaded, saveAdditional(furnace, ctx), ctx);
            ctx.assertTrue(reloaded.getItem(2).is(Items.IRON_INGOT) && reloaded.getItem(2).getCount() == 3,
                    "the block-entity save/load round trip restores the pending output");

            ItemStack firstTake = reloaded.removeItem(2, 1);
            new Slot(reloaded, 2, 0, 0).onTake(first, firstTake.copy());
            ItemStack secondTake = reloaded.removeItem(2, 1);
            new Slot(reloaded, 2, 0, 0).onTake(second, secondTake.copy());
            ItemStack repeatedTake = reloaded.removeItem(2, 1);
            new Slot(reloaded, 2, 0, 0).onTake(first, repeatedTake.copy());

            ctx.assertTrue(KnowledgeRuntime.state(first).conditionProgress("ore_types_smelted") == 1,
                    "partial and repeated collection credits the first player once for the distinct ore family");
            ctx.assertTrue(KnowledgeRuntime.state(second).conditionProgress("ore_types_smelted") == 1,
                    "the second player is credited only for the output they actually take");
            ctx.assertTrue(KnowledgeRuntime.state(first).get("condition.ore_types_smelted.seen.minecraft.iron") == 1,
                    "the real result-slot hook records the persisted iron provenance token");
            ctx.succeed();
        });
    }

    @GameTest(maxTicks = 430)
    public void brewingProvenanceSurvivesSaveLoadAndUsesTheCollectingPlayer(GameTestHelper ctx) {
        BlockPos pos = ctx.absolutePos(new BlockPos(2, 2, 2));
        ctx.setBlock(new BlockPos(2, 2, 2), Blocks.BREWING_STAND);
        BrewingStandBlockEntity stand = (BrewingStandBlockEntity) ctx.getLevel().getBlockEntity(pos);
        ServerPlayer first = ctx.makeMockServerPlayerInLevel();
        ServerPlayer second = ctx.makeMockServerPlayerInLevel();
        ItemStack waterPotion = PotionContents.createItemStack(Items.POTION, Potions.WATER);
        stand.setItem(0, waterPotion.copy());
        stand.setItem(1, waterPotion.copy());
        stand.setItem(2, waterPotion.copy());
        stand.setItem(3, new ItemStack(Items.NETHER_WART));
        stand.setItem(4, new ItemStack(Items.BLAZE_POWDER));

        ctx.runAfterDelay(420, () -> {
            for (int slot = 0; slot < 3; slot++)
                ctx.assertTrue(stand.getItem(slot).is(Items.POTION)
                        && !ItemStack.matches(waterPotion, stand.getItem(slot)),
                        "vanilla brew transforms potion slot " + slot);
            BrewingStandBlockEntity reloaded = new BrewingStandBlockEntity(pos,
                    Blocks.BREWING_STAND.defaultBlockState());
            loadAdditional(reloaded, saveAdditional(stand, ctx), ctx);

            ItemStack firstTake = reloaded.removeItem(0, 1);
            new Slot(reloaded, 0, 0, 0).onTake(first, firstTake.copy());
            ItemStack secondTake = reloaded.removeItem(1, 1);
            new Slot(reloaded, 1, 0, 0).onTake(second, secondTake.copy());
            ItemStack repeatTake = reloaded.removeItem(2, 1);
            new Slot(reloaded, 2, 0, 0).onTake(first, repeatTake.copy());

            ctx.assertTrue(KnowledgeRuntime.state(first).conditionProgress("potions_brewed") == 2,
                    "the first player is credited for the two brewed outputs they collect");
            ctx.assertTrue(KnowledgeRuntime.state(second).conditionProgress("potions_brewed") == 1,
                    "the second player is credited only for their collected output");
            ctx.succeed();
        });
    }

    @GameTest(maxTicks = 430)
    public void brewingOutputTakenByHopperDoesNotCreditNearbyPlayers(GameTestHelper ctx) {
        BlockPos standPos = ctx.absolutePos(new BlockPos(2, 2, 2));
        BlockPos hopperPos = ctx.absolutePos(new BlockPos(2, 1, 2));
        ctx.setBlock(new BlockPos(2, 2, 2), Blocks.BREWING_STAND);
        ctx.setBlock(new BlockPos(2, 1, 2), Blocks.HOPPER);
        BrewingStandBlockEntity stand = (BrewingStandBlockEntity) ctx.getLevel().getBlockEntity(standPos);
        var hopper = (net.minecraft.world.level.block.entity.HopperBlockEntity) ctx.getLevel().getBlockEntity(hopperPos);
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        stand.setItem(0, PotionContents.createItemStack(Items.POTION, Potions.WATER));
        stand.setItem(3, new ItemStack(Items.NETHER_WART));
        stand.setItem(4, new ItemStack(Items.BLAZE_POWDER));

        ctx.runAfterDelay(420, () -> {
            ctx.assertTrue(hopper.getItem(0).is(Items.POTION) || hopper.getItem(1).is(Items.POTION)
                    || hopper.getItem(2).is(Items.POTION) || hopper.getItem(3).is(Items.POTION)
                    || hopper.getItem(4).is(Items.POTION),
                    "a live hopper extracts a potion produced by the vanilla brewing tick");
            ctx.assertTrue(stand.getItem(0).isEmpty(), "automated extraction removes the brewing output");
            ctx.assertTrue(KnowledgeRuntime.state(player).conditionProgress("potions_brewed") == 0,
                    "automated extraction does not attribute brewing progress to a nearby player");
            ctx.succeed();
        });
    }

    private static CompoundTag saveAdditional(net.minecraft.world.level.block.entity.BlockEntity entity,
            GameTestHelper ctx) {
        try {
            TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING,
                    ctx.getLevel().registryAccess());
            Method save = findDeclaredMethod(entity.getClass(), "saveAdditional", ValueOutput.class);
            save.setAccessible(true);
            save.invoke(entity, output);
            return output.buildResult();
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("could not execute block-entity save hook", exception);
        }
    }

    private static void loadAdditional(net.minecraft.world.level.block.entity.BlockEntity entity, CompoundTag data,
            GameTestHelper ctx) {
        try {
            Method load = findDeclaredMethod(entity.getClass(), "loadAdditional", ValueInput.class);
            load.setAccessible(true);
            load.invoke(entity, TagValueInput.create(ProblemReporter.DISCARDING,
                    ctx.getLevel().registryAccess(), data));
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("could not execute block-entity load hook", exception);
        }
    }

    private static Method findDeclaredMethod(Class<?> type, String name, Class<?> argument)
            throws NoSuchMethodException {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                return current.getDeclaredMethod(name, argument);
            } catch (NoSuchMethodException ignored) {
                // Continue until the vanilla superclass that owns the block-entity codec hooks.
            }
        }
        throw new NoSuchMethodException(type.getName() + "." + name);
    }

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
