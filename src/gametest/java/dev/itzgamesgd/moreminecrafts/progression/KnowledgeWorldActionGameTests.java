package dev.itzgamesgd.moreminecrafts.progression;

import java.lang.reflect.Method;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionResult;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Runtime tests for placement, tilling, irrigation, and food result-slot observers. */
public final class KnowledgeWorldActionGameTests implements CustomTestMethodInvoker {
    @GameTest
    public void hopperPlacementRequiresSuccessfulBlockPlacement(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.HOPPER));
        BlockPos base = new BlockPos(1, 1, 1);
        ctx.setBlock(base, Blocks.STONE);
        InteractionResult placed = useOnTop(ctx, player, base);
        ctx.assertTrue(placed.consumesAction(), "vanilla places a hopper on the supported location");
        ctx.assertTrue(KnowledgeRuntime.state(player).conditionProgress("hoppers_placed") == 1,
                "successful hopper placement reaches the installed BlockItem hook");

        BlockPos blocked = new BlockPos(3, 1, 1);
        ctx.setBlock(blocked, Blocks.STONE);
        ctx.setBlock(blocked.above(), Blocks.STONE);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.HOPPER));
        InteractionResult failed = useOnTop(ctx, player, blocked);
        ctx.assertTrue(!failed.consumesAction(), "occupied placement target rejects a hopper");
        ctx.assertTrue(KnowledgeRuntime.state(player).conditionProgress("hoppers_placed") == 1,
                "failed placement adds no hopper progress");
        ctx.succeed();
    }

    @GameTest
    public void allApprovedRedstonePlacementItemsCountOnceAndFailedPlacementDoesNot(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        net.minecraft.world.item.Item[] components = {
                Items.REDSTONE, Items.REPEATER, Items.COMPARATOR, Items.OBSERVER, Items.PISTON, Items.STICKY_PISTON
        };
        for (int i = 0; i < components.length; i++) {
            BlockPos base = new BlockPos(1 + i * 2, 1, 1);
            ctx.setBlock(base, Blocks.STONE);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(components[i]));
            InteractionResult result = useOnTop(ctx, player, base);
            ctx.assertTrue(result.consumesAction(), "approved component placement succeeds: " + components[i]);
            ctx.assertTrue(KnowledgeRuntime.state(player).conditionProgress("redstone_components_placed") == i + 1,
                    "each approved component placement contributes exactly once");
        }

        BlockPos blocked = new BlockPos(15, 1, 1);
        ctx.setBlock(blocked, Blocks.STONE);
        ctx.setBlock(blocked.above(), Blocks.STONE);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.REPEATER));
        InteractionResult failed = useOnTop(ctx, player, blocked);
        ctx.assertTrue(!failed.consumesAction(), "occupied target rejects a redstone component");
        ctx.assertTrue(KnowledgeRuntime.state(player).conditionProgress("redstone_components_placed") == 6,
                "failed placement adds no redstone progress");
        ctx.succeed();
    }

    @GameTest
    public void farmlandCultivationRequiresHoeToCreateFarmland(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        BlockPos dirt = new BlockPos(1, 1, 1);
        ctx.setBlock(dirt, Blocks.DIRT);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WOODEN_HOE));
        InteractionResult result = useOnTop(ctx, player, dirt);
        ctx.assertTrue(result.consumesAction() && ctx.getBlockState(dirt).is(Blocks.FARMLAND),
                "successful hoe use tills the dirt block");
        ctx.assertTrue(KnowledgeRuntime.state(player).conditionProgress("farmland_tilled") == 1,
                "the installed item-use hook records actual farmland cultivation");

        BlockPos stone = new BlockPos(3, 1, 1);
        ctx.setBlock(stone, Blocks.STONE);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WOODEN_HOE));
        InteractionResult noOp = useOnTop(ctx, player, stone);
        ctx.assertTrue(!noOp.consumesAction(), "hoe use on stone is unsuccessful");
        ctx.assertTrue(KnowledgeRuntime.state(player).conditionProgress("farmland_tilled") == 1,
                "failed hoe use does not add progress");
        ctx.succeed();
    }

    @GameTest
    public void irrigationRequiresSourceWaterWithinFarmlandHydrationReach(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        BlockPos support = new BlockPos(1, 1, 1);
        BlockPos farmland = new BlockPos(2, 1, 1);
        BlockPos source = support.above();
        ctx.setBlock(support, Blocks.STONE);
        ctx.setBlock(farmland, Blocks.FARMLAND);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        InteractionResult result = useOnTop(ctx, player, support);
        ctx.assertTrue(result.consumesAction(), "water bucket action succeeds on the supported block");
        ctx.assertTrue(ctx.getLevel().getFluidState(ctx.absolutePos(source)).isSource(),
                "water bucket places a source block beside nearby farmland");
        ctx.assertTrue(KnowledgeRuntime.state(player).conditionProgress("farmland_watered") == 1,
                "the installed item-use hook records nearby farmland irrigation");

        BlockPos farSupport = new BlockPos(10, 1, 1);
        BlockPos farmlandBeyondReach = new BlockPos(16, 1, 1);
        ctx.setBlock(farSupport, Blocks.STONE);
        ctx.setBlock(farmlandBeyondReach, Blocks.FARMLAND);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        InteractionResult distant = useOnTop(ctx, player, farSupport);
        ctx.assertTrue(distant.consumesAction(), "water placement itself succeeds at the distant location");
        ctx.assertTrue(KnowledgeRuntime.state(player).conditionProgress("farmland_watered") == 1,
                "water outside farmland hydration reach adds no progress");
        ctx.succeed();
    }

    @GameTest
    public void foodCraftingResultSlotCountsDistinctFoodIdsOnce(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var menu = new net.minecraft.world.inventory.CraftingMenu(1, player.getInventory(),
                net.minecraft.world.inventory.ContainerLevelAccess.create(ctx.getLevel(),
                        ctx.absolutePos(new BlockPos(1, 1, 1))));
        for (int craft = 0; craft < 2; craft++) {
            menu.getSlot(4).set(new ItemStack(Items.WHEAT));
            menu.getSlot(5).set(new ItemStack(Items.WHEAT));
            menu.getSlot(6).set(new ItemStack(Items.WHEAT));
            menu.slotsChanged(menu.getSlot(1).container);
            ItemStack output = menu.getSlot(0).getItem().copy();
            ctx.assertTrue(output.is(Items.BREAD), "vanilla bread recipe produces the food result");
            menu.getSlot(0).onTake(player, output);
            ctx.assertTrue(KnowledgeRuntime.state(player).conditionProgress("foods_crafted_or_cooked") == 1,
                    "repeated bread crafts remain one distinct food ID");
        }
        ctx.succeed();
    }

    private static net.minecraft.world.InteractionResult useOnTop(GameTestHelper ctx, ServerPlayer player,
            BlockPos position) {
        BlockPos world = ctx.absolutePos(position);
        var hit = new BlockHitResult(Vec3.atCenterOf(world), Direction.UP, world, false);
        return player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    }

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
