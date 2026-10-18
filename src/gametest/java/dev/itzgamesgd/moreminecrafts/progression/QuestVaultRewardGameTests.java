package dev.itzgamesgd.moreminecrafts.progression;

import java.lang.reflect.Method;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VaultBlock;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.block.entity.vault.VaultConfig;
import net.minecraft.world.level.block.entity.vault.VaultState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

/** C2 real vanilla key-insertion and per-player reward source coverage. */
public final class QuestVaultRewardGameTests implements CustomTestMethodInvoker {
    private static int seen(ServerPlayer player, String key) {
        return QuestRules.distinctCount(KnowledgeRuntime.state(player), key);
    }

    private static VaultBlockEntity vault(GameTestHelper ctx, BlockPos local, boolean ominous) {
        ctx.setBlock(local, Blocks.VAULT.defaultBlockState()
                .setValue(VaultBlock.STATE, VaultState.ACTIVE)
                .setValue(VaultBlock.OMINOUS, ominous));
        var blockEntity = ctx.getLevel().getBlockEntity(ctx.absolutePos(local));
        ctx.assertTrue(blockEntity instanceof VaultBlockEntity, "real vault block entity exists");
        VaultBlockEntity vault = (VaultBlockEntity) blockEntity;
        if (ominous) vault.setConfig(new VaultConfig(BuiltInLootTables.TRIAL_CHAMBERS_REWARD_OMINOUS,
                4.0D, 4.5D, new ItemStack(Items.OMINOUS_TRIAL_KEY), Optional.empty()));
        return vault;
    }

    private static void insert(GameTestHelper ctx, BlockPos local, VaultBlockEntity vault,
            ServerPlayer player, ItemStack key) {
        BlockPos pos = ctx.absolutePos(local);
        VaultBlockEntity.Server.tryInsertKey(ctx.getLevel(), pos, ctx.getLevel().getBlockState(pos),
                vault.getConfig(), vault.getServerData(), vault.getSharedData(), player, key);
    }

    @GameTest
    public void normalVaultRewardsOnlyTheSuccessfulKeyUser(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        ServerPlayer other = ctx.makeMockServerPlayerInLevel();
        BlockPos pos = new BlockPos(1, 1, 1);
        VaultBlockEntity vault = vault(ctx, pos, false);
        insert(ctx, pos, vault, player, new ItemStack(Items.STICK));
        ctx.assertTrue(seen(player, "set.normal_vault_rewards") == 0, "invalid key gives no reward evidence");
        insert(ctx, pos, vault, player, new ItemStack(Items.TRIAL_KEY));
        ctx.assertTrue(seen(player, "set.normal_vault_rewards") == 1
                && seen(player, "set.ominous_vault_rewards") == 0,
                "successful normal vault rewards its key user only");
        ctx.assertTrue(seen(other, "set.normal_vault_rewards") == 0, "nearby player gets no credit");
        ctx.succeed();
    }

    @GameTest
    public void ominousVaultEvidenceIsSeparateAndDeduplicated(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        BlockPos first = new BlockPos(1, 1, 1);
        VaultBlockEntity vault = vault(ctx, first, true);
        insert(ctx, first, vault, player, new ItemStack(Items.OMINOUS_TRIAL_KEY));
        ctx.assertTrue(seen(player, "set.ominous_vault_rewards") == 1
                && seen(player, "set.normal_vault_rewards") == 0,
                "ominous reward has its own evidence set");
        insert(ctx, first, vault, player, new ItemStack(Items.OMINOUS_TRIAL_KEY));
        ctx.assertTrue(seen(player, "set.ominous_vault_rewards") == 1,
                "same player cannot count the same vault twice");
        ctx.succeed();
    }

    @Override public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
