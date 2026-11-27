package dev.itzgamesgd.moreminecrafts.progression;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.VaultBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Records a player-specific successful vanilla vault reward by vault identity. */
public final class QuestVaultRewards {
    private QuestVaultRewards() {}

    public static void rewarded(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state) {
        if (player == null || level == null || pos == null || state == null
                || !(state.getBlock() instanceof VaultBlock)) return;
        String key = state.getValue(VaultBlock.OMINOUS) ? "set.ominous_vault_rewards" : "set.normal_vault_rewards";
        String vault = level.dimension().identifier() + ":" + pos.asLong();
        ResearchProgressState before = KnowledgeRuntime.state(player);
        ResearchProgressState next = QuestRules.distinctCount(before, key) >= 5 ? before
                : QuestRules.observeDistinct(before, key, vault);
        if (!next.equals(before)) {
            KnowledgeRuntime.updateState(player, next);
            XpQuestTracking.reconcile(player);
        }
    }
}
