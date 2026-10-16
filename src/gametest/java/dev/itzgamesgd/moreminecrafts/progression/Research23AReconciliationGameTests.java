package dev.itzgamesgd.moreminecrafts.progression;

import dev.itzgamesgd.moreminecrafts.fishing.FishingChallenges;
import java.lang.reflect.Method;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Integration regression checks for the nonduplicated PR #93 harvest corrections. */
public final class Research23AReconciliationGameTests implements CustomTestMethodInvoker {
    @GameTest
    public void lumberingRunsBeforeProcessingWhileOreCopiesFinalProcessedDrop(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.getAbilities().instabuild = false; // The mock player can retain creative abilities after mode setup.
        var logState = Blocks.OAK_LOG.defaultBlockState();
        var oreState = Blocks.IRON_ORE.defaultBlockState();
        var baseLogs = List.of(new ItemStack(Items.OAK_LOG));
        var baseOre = List.of(new ItemStack(Items.RAW_IRON));
        var processedOre = List.of(new ItemStack(Items.IRON_INGOT));

        ctx.assertTrue(ResearchContentRuntime.applyLumberingResearch(player, logState, baseLogs, () -> 0.0D)
                        .size() == 1, "unowned Lumbering cannot add bonus logs");
        ctx.assertTrue(ResearchContentRuntime.applyOreResearch(player, oreState, processedOre, () -> 0.0D)
                        .size() == 1, "unowned Ore Mining cannot add bonus items");

        KnowledgeRuntime.updateState(player, KnowledgeRuntime.state(player)
                .with("research.v3.research.01", 5).with("research.v3.research.02", 5));

        var withLumbering = ResearchContentRuntime.applyLumberingResearch(player, logState, baseLogs, () -> 0.0D);
        int lumberLevel = ResearchRewards.level(player, "v3.research.02", "lumbering_extra_drops");
        boolean logBlockTagged = logState.is(net.minecraft.tags.BlockTags.LOGS);
        boolean logItemTagged = baseLogs.getFirst().is(net.minecraft.tags.ItemTags.LOGS);
        ctx.assertTrue(withLumbering.size() == 2 && withLumbering.get(1).is(Items.OAK_LOG),
                "Lumbering before the enchantment pass: paidLevel=" + lumberLevel
                        + ", creative=" + player.isCreative()
                        + ", blockTag=" + logBlockTagged + ", itemTag=" + logItemTagged
                        + ", drops=" + withLumbering);
        var logAtOreStage = ResearchContentRuntime.applyOreResearch(player, logState, withLumbering, () -> 0.0D);
        ctx.assertTrue(logAtOreStage.size() == 2,
                "Ore Mining must not generate a second bonus for a log");

        var unprocessedOre = ResearchContentRuntime.applyLumberingResearch(player, oreState, baseOre, () -> 0.0D);
        ctx.assertTrue(unprocessedOre.size() == 1 && unprocessedOre.getFirst().is(Items.RAW_IRON),
                "ore must remain unchanged in the pre-processing Lumbering stage");
        var withOreBonus = ResearchContentRuntime.applyOreResearch(player, oreState, processedOre, () -> 0.0D);
        ctx.assertTrue(withOreBonus.size() == 2
                        && withOreBonus.getFirst().is(Items.IRON_INGOT)
                        && withOreBonus.get(1).is(Items.IRON_INGOT),
                "Ore Mining must duplicate final processed ingot rather than raw iron");
        ctx.succeed();
    }

    @GameTest
    public void beehivePurchaseAndSuccessfulShearsHarvestAreRequired(GameTestHelper ctx)
            throws ReflectiveOperationException {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
        BlockPos pos = ctx.absolutePos(new BlockPos(2, 2, 2));
        var full = Blocks.BEEHIVE.defaultBlockState().setValue(BeehiveBlock.HONEY_LEVEL, 5);
        ctx.getLevel().setBlock(pos, full, 3);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        var method = Blocks.BEEHIVE.getClass().getDeclaredMethod("useItemOn",
                ItemStack.class, net.minecraft.world.level.block.state.BlockState.class,
                net.minecraft.world.level.Level.class, BlockPos.class,
                net.minecraft.world.entity.player.Player.class, InteractionHand.class, BlockHitResult.class);
        method.setAccessible(true);
        InteractionResult result = (InteractionResult) method.invoke(Blocks.BEEHIVE, player.getMainHandItem(),
                full, ctx.getLevel(), pos, player, InteractionHand.MAIN_HAND, hit);
        ctx.assertTrue(result.consumesAction(), "vanilla completes the unowned harvest");
        int combCount = ctx.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1.5D))
                .stream().filter(e -> e.getItem().is(Items.HONEYCOMB)).mapToInt(e -> e.getItem().getCount()).sum();
        ctx.assertTrue(combCount == 3, "unowned beekeeping must award only the three vanilla honeycombs");
        ctx.assertTrue(!ResearchBeehiveHarvestGuard.succeeded(full, Blocks.AIR.defaultBlockState(), true),
                "if another mod replaces the hive, skip safely without a missing-property read");
        ctx.succeed();
    }

    @Override
    public void invokeTestMethod(GameTestHelper ctx, Method method) throws ReflectiveOperationException {
        method.invoke(this, ctx);
    }
}
