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

    private static final String NORMAL = "set.normal_vault_rewards";
    private static final String OMINOUS = "set.ominous_vault_rewards";
    private static final String NORMAL_QUEST = "xpq.dim.trial_vaults_5";
    private static final String OMINOUS_QUEST = "xpq.dim.ominous_vaults_5";

    private static boolean complete(ServerPlayer player, String quest) {
        return KnowledgeRuntime.state(player).questComplete(quest);
    }

    /** Places and rewards {@code count} fresh vaults at x = first..first+count-1 (row z = 3 normal, 5 ominous). */
    private static void reward(GameTestHelper ctx, ServerPlayer player, int first, int count, boolean ominous) {
        for (int x = first; x < first + count; x++) {
            BlockPos local = new BlockPos(x, 1, ominous ? 5 : 3);
            QuestVaultFixture.place(ctx.getLevel(), ctx.absolutePos(local), ominous, VaultState.ACTIVE);
            QuestVaultFixture.insert(ctx.getLevel(), ctx.absolutePos(local), player, QuestVaultFixture.key(ominous));
        }
    }

    @GameTest
    public void fiveDistinctNormalVaultsCompleteOnlyTheTrialQuest(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        ServerPlayer bystander = ctx.makeMockServerPlayerInLevel();
        reward(ctx, player, 1, 4, false);
        ctx.assertTrue(seen(player, NORMAL) == 4 && !complete(player, NORMAL_QUEST), "four vaults are insufficient");
        reward(ctx, player, 5, 1, false);
        ctx.assertTrue(seen(player, NORMAL) == 5 && complete(player, NORMAL_QUEST), "fifth distinct vault completes");
        ctx.assertTrue(seen(player, OMINOUS) == 0 && !complete(player, OMINOUS_QUEST),
                "normal vaults never count toward the ominous condition");
        ctx.assertTrue(seen(bystander, NORMAL) == 0 && !complete(bystander, NORMAL_QUEST), "bystander unaffected");
        ctx.succeed();
    }

    @GameTest
    public void fiveDistinctOminousVaultsCompleteOnlyTheOminousQuest(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        ServerPlayer bystander = ctx.makeMockServerPlayerInLevel();
        reward(ctx, player, 1, 4, true);
        ctx.assertTrue(seen(player, OMINOUS) == 4 && !complete(player, OMINOUS_QUEST), "four vaults are insufficient");
        reward(ctx, player, 5, 1, true);
        ctx.assertTrue(seen(player, OMINOUS) == 5 && complete(player, OMINOUS_QUEST), "fifth distinct vault completes");
        ctx.assertTrue(seen(player, NORMAL) == 0 && !complete(player, NORMAL_QUEST),
                "ominous vaults never count toward the normal condition");
        ctx.assertTrue(seen(bystander, OMINOUS) == 0 && !complete(bystander, OMINOUS_QUEST), "bystander unaffected");
        ctx.succeed();
    }

    @GameTest
    public void repeatedUseOfOneVaultNeverAddsDistinctProgress(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        BlockPos pos = ctx.absolutePos(new BlockPos(1, 1, 3));
        QuestVaultFixture.place(ctx.getLevel(), pos, false, VaultState.ACTIVE);
        for (int i = 0; i < 6; i++) QuestVaultFixture.insert(ctx.getLevel(), pos, player, QuestVaultFixture.key(false));
        ctx.assertTrue(seen(player, NORMAL) == 1 && !complete(player, NORMAL_QUEST),
                "one vault is one distinct entry however often it is used");
        ctx.succeed();
    }

    @GameTest
    public void wrongKeyTypeAndUnsuccessfulInteractionsGiveNoCredit(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        BlockPos normal = ctx.absolutePos(new BlockPos(1, 1, 3));
        BlockPos ominous = ctx.absolutePos(new BlockPos(2, 1, 3));
        BlockPos inactive = ctx.absolutePos(new BlockPos(3, 1, 3));
        BlockPos empty = ctx.absolutePos(new BlockPos(4, 1, 3));
        QuestVaultFixture.place(ctx.getLevel(), normal, false, VaultState.ACTIVE);
        QuestVaultFixture.place(ctx.getLevel(), ominous, true, VaultState.ACTIVE);
        QuestVaultFixture.place(ctx.getLevel(), inactive, false, VaultState.INACTIVE);
        var emptyVault = QuestVaultFixture.place(ctx.getLevel(), empty, false, VaultState.ACTIVE);
        emptyVault.setConfig(new VaultConfig(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                net.minecraft.resources.Identifier.parse("moreminecrafts:test/missing_vault_reward")),
                4.0D, 4.5D, new ItemStack(Items.TRIAL_KEY), Optional.empty()));
        ItemStack ominousKey = QuestVaultFixture.key(true);
        ItemStack normalKey = QuestVaultFixture.key(false);
        QuestVaultFixture.insert(ctx.getLevel(), normal, player, ominousKey);
        QuestVaultFixture.insert(ctx.getLevel(), ominous, player, normalKey);
        QuestVaultFixture.insert(ctx.getLevel(), normal, player, new ItemStack(Items.STICK));
        QuestVaultFixture.insert(ctx.getLevel(), normal, player, ItemStack.EMPTY);
        QuestVaultFixture.insert(ctx.getLevel(), inactive, player, QuestVaultFixture.key(false));
        QuestVaultFixture.insert(ctx.getLevel(), empty, player, QuestVaultFixture.key(false));
        ctx.assertTrue(seen(player, NORMAL) == 0 && seen(player, OMINOUS) == 0,
                "wrong key, wrong vault type, inactive vault and empty reward produce no evidence");
        ctx.assertTrue(!ominousKey.isEmpty() && !normalKey.isEmpty(), "rejected keys are not consumed");
        QuestVaultFixture.insert(ctx.getLevel(), normal, player, QuestVaultFixture.key(false));
        QuestVaultFixture.insert(ctx.getLevel(), ominous, player, QuestVaultFixture.key(true));
        ctx.assertTrue(seen(player, NORMAL) == 1 && seen(player, OMINOUS) == 1,
                "the matching key still works afterwards and each type has its own set");
        ctx.succeed();
    }

    @GameTest
    public void fourPlusFourAcrossTypesCompletesNeither(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        reward(ctx, player, 1, 4, false);
        reward(ctx, player, 1, 4, true);
        ctx.assertTrue(seen(player, NORMAL) == 4 && seen(player, OMINOUS) == 4
                && !complete(player, NORMAL_QUEST) && !complete(player, OMINOUS_QUEST),
                "four normal plus four ominous cannot be pooled into either condition");
        ctx.succeed();
    }

    @GameTest
    public void twoPlayersRewardedByTheSameVaultKeepSeparateEvidence(GameTestHelper ctx) {
        ServerPlayer first = ctx.makeMockServerPlayerInLevel();
        ServerPlayer second = ctx.makeMockServerPlayerInLevel();
        ServerPlayer bystander = ctx.makeMockServerPlayerInLevel();
        BlockPos shared = ctx.absolutePos(new BlockPos(1, 1, 3));
        BlockPos other = ctx.absolutePos(new BlockPos(2, 1, 3));
        QuestVaultFixture.place(ctx.getLevel(), shared, false, VaultState.ACTIVE);
        QuestVaultFixture.place(ctx.getLevel(), other, false, VaultState.ACTIVE);
        QuestVaultFixture.insert(ctx.getLevel(), shared, first, QuestVaultFixture.key(false));
        // The vault left ACTIVE while ejecting; the second player's reward goes through the same vault block entity.
        ctx.getLevel().setBlock(shared, ctx.getLevel().getBlockState(shared).setValue(VaultBlock.STATE, VaultState.ACTIVE), 3);
        QuestVaultFixture.insert(ctx.getLevel(), shared, second, QuestVaultFixture.key(false));
        QuestVaultFixture.insert(ctx.getLevel(), other, second, QuestVaultFixture.key(false));
        ctx.assertTrue(seen(first, NORMAL) == 1 && seen(second, NORMAL) == 2,
                "each rewarded player holds only their own vault identities");
        ctx.assertTrue(seen(bystander, NORMAL) == 0, "a player who inserted nothing has no evidence");
        ctx.succeed();
    }

    @GameTest
    public void evidenceSurvivesPlayerSaveAndLoad(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        reward(ctx, player, 1, 5, false);
        reward(ctx, player, 1, 3, true);
        var out = net.minecraft.world.level.storage.TagValueOutput.createWithContext(
                net.minecraft.util.ProblemReporter.DISCARDING, ctx.getLevel().registryAccess());
        player.saveWithoutId(out);
        ServerPlayer reloaded = ctx.makeMockServerPlayerInLevel();
        reloaded.load(net.minecraft.world.level.storage.TagValueInput.create(
                net.minecraft.util.ProblemReporter.DISCARDING, ctx.getLevel().registryAccess(), out.buildResult()));
        ctx.assertTrue(seen(reloaded, NORMAL) == 5 && seen(reloaded, OMINOUS) == 3
                && complete(reloaded, NORMAL_QUEST) && !complete(reloaded, OMINOUS_QUEST),
                "player reload restores both evidence sets and only the completed quest");
        ctx.succeed();
    }

    @GameTest
    public void completionIsOnceOnlyAndClaimRetriesCannotDuplicateXp(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        reward(ctx, player, 1, 5, false);
        ctx.assertTrue(complete(player, NORMAL_QUEST), "quest completes");
        int before = RawXp.available(player);
        reward(ctx, player, 6, 2, false);
        ctx.assertTrue(seen(player, NORMAL) == 5, "evidence is bounded at the target after completion");
        ctx.assertTrue(QuestProgressService.claim(player, NORMAL_QUEST) == QuestProgressService.ClaimResult.CLAIMED,
                "first claim succeeds");
        ctx.assertTrue(RawXp.available(player) == before + 500, "exactly the approved 500 raw XP is granted");
        reward(ctx, player, 8, 2, false);
        for (int i = 0; i < 3; i++)
            ctx.assertTrue(QuestProgressService.claim(player, NORMAL_QUEST) == QuestProgressService.ClaimResult.ALREADY_CLAIMED,
                    "claim retry " + i + " is rejected");
        ctx.assertTrue(RawXp.available(player) == before + 500 && KnowledgeRuntime.state(player).questClaimed(NORMAL_QUEST),
                "post-completion vault events and claim retries grant nothing more");
        ctx.assertTrue(QuestProgressService.claim(player, OMINOUS_QUEST) == QuestProgressService.ClaimResult.INCOMPLETE,
                "the other vault quest stays unclaimable");
        ctx.succeed();
    }

    @Override public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
