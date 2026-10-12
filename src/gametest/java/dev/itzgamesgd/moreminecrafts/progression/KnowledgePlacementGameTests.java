package dev.itzgamesgd.moreminecrafts.progression;

import java.lang.reflect.Method;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class KnowledgePlacementGameTests implements CustomTestMethodInvoker {
    @GameTest
    public void oneRedstoneDustPlacementCountsOnce(GameTestHelper ctx) {
        var player = ctx.makeMockServerPlayerInLevel();
        BlockPos base = new BlockPos(1, 1, 1);
        ctx.setBlock(base, Blocks.STONE);
        BlockPos worldBase = ctx.absolutePos(base);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.REDSTONE, 2));

        var hit = new BlockHitResult(Vec3.atCenterOf(worldBase), Direction.UP, worldBase, false);
        var result = player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));

        ctx.assertTrue(result.consumesAction(), "redstone dust placement succeeds");
        ctx.assertTrue(KnowledgeRuntime.state(player).conditionProgress("redstone_components_placed") == 1,
                "one placement increments K19 once");
        ctx.succeed();
    }

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
