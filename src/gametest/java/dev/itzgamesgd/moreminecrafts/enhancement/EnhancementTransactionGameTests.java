package dev.itzgamesgd.moreminecrafts.enhancement;

import dev.itzgamesgd.moreminecrafts.progression.RawXp;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import java.lang.reflect.Method;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Enhancement raw-XP cost and transactional rollback (Q1-A2). */
public final class EnhancementTransactionGameTests implements CustomTestMethodInvoker {
    private static boolean xp(ServerPlayer player, int expected) {
        return RawXp.available(player) == expected && player.totalExperience == expected;
    }

    /** Mock players default to a mode with infinite materials, which keeps XP costs free by design. */
    static ServerPlayer survival(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        return player;
    }

    @GameTest
    public void costIsFixedRawXpRegardlessOfLevel(GameTestHelper ctx) {
        ServerPlayer player = survival(ctx);
        for (int total : new int[]{40, 160, 2_920, 100_000}) {
            RawXp.setTotal(player, total);
            ItemStack sword = new ItemStack(Items.IRON_SWORD);
            ItemStack stones = new ItemStack(ModItems.ENHANCEMENT_STONE, 4);
            ctx.assertTrue(EnhancementOperation.apply(player, sword, stones), "+1 succeeds at " + total);
            ctx.assertTrue(xp(player, total - 40) && EnhancementOperation.level(sword) == 1 && stones.getCount() == 3,
                    "+1 costs exactly 40 XP and 1 stone at balance " + total);
        }
        RawXp.setTotal(player, 1_900);
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        ItemStack stones = new ItemStack(ModItems.ENHANCEMENT_STONE, 24);
        for (int i = 0; i < 20; i++) ctx.assertTrue(EnhancementOperation.apply(player, sword, stones), "step " + (i + 1));
        ctx.assertTrue(xp(player, 0) && EnhancementOperation.level(sword) == 20 && stones.isEmpty(),
                "+0→+20 costs 1,900 XP and 24 stones");
        ctx.succeed();
    }

    @GameTest
    public void insufficientXpOrMaterialChangesNothing(GameTestHelper ctx) {
        ServerPlayer player = survival(ctx);
        RawXp.setTotal(player, 39);
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        ItemStack stones = new ItemStack(ModItems.ENHANCEMENT_STONE, 4);
        ctx.assertTrue(!EnhancementOperation.apply(player, sword, stones) && xp(player, 39)
                && EnhancementOperation.level(sword) == 0 && stones.getCount() == 4, "39 XP refused, nothing changed");
        RawXp.setTotal(player, 500);
        sword.set(dev.itzgamesgd.moreminecrafts.registry.ModDataComponents.ENHANCEMENT_LEVEL, 4);
        ItemStack oneStone = new ItemStack(ModItems.ENHANCEMENT_STONE, 1);
        ctx.assertTrue(!EnhancementOperation.apply(player, sword, oneStone) && xp(player, 500)
                && EnhancementOperation.level(sword) == 4 && oneStone.getCount() == 1, "milestone without 2 stones refused");
        ctx.succeed();
    }

    @GameTest
    public void failureAfterMutationRollsBackXpMaterialAndItem(GameTestHelper ctx) {
        ServerPlayer player = survival(ctx);
        for (int startLevel : new int[]{0, 9}) {
            RawXp.setTotal(player, 1_507);
            ItemStack sword = new ItemStack(Items.IRON_SWORD);
            if (startLevel > 0) sword.set(dev.itzgamesgd.moreminecrafts.registry.ModDataComponents.ENHANCEMENT_LEVEL, startLevel);
            ItemStack stones = new ItemStack(ModItems.ENHANCEMENT_STONE, 5);
            ItemStack swordBefore = sword.copy();
            EnhancementOperation.failureInjectionForTests = () -> { throw new IllegalStateException("injected"); };
            boolean applied;
            try {
                applied = EnhancementOperation.apply(player, sword, stones);
            } finally {
                EnhancementOperation.failureInjectionForTests = null;
            }
            ctx.assertTrue(!applied, "failure reported from level " + startLevel);
            ctx.assertTrue(xp(player, 1_507), "XP restored from level " + startLevel);
            ctx.assertTrue(stones.getCount() == 5, "material restored from level " + startLevel);
            ctx.assertTrue(ItemStack.isSameItemSameComponents(sword, swordBefore), "item restored from level " + startLevel);
        }
        ctx.succeed();
    }

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
