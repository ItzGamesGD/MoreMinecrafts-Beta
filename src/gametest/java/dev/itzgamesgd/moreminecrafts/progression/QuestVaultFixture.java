package dev.itzgamesgd.moreminecrafts.progression;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VaultBlock;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.block.entity.vault.VaultConfig;
import net.minecraft.world.level.block.entity.vault.VaultState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

/** Real vanilla vault block entities and the real key-insertion entrypoint, shared by the C2 tests. */
final class QuestVaultFixture {
    private QuestVaultFixture() {}

    static VaultBlockEntity place(ServerLevel level, BlockPos pos, boolean ominous, VaultState state) {
        level.setBlock(pos, Blocks.VAULT.defaultBlockState()
                .setValue(VaultBlock.STATE, state)
                .setValue(VaultBlock.OMINOUS, ominous), 3);
        VaultBlockEntity vault = (VaultBlockEntity) level.getBlockEntity(pos);
        if (vault == null) throw new IllegalStateException("vault block entity missing at " + pos);
        if (ominous) vault.setConfig(new VaultConfig(BuiltInLootTables.TRIAL_CHAMBERS_REWARD_OMINOUS,
                4.0D, 4.5D, new ItemStack(Items.OMINOUS_TRIAL_KEY), Optional.empty()));
        return vault;
    }

    static ItemStack key(boolean ominous) {
        return new ItemStack(ominous ? Items.OMINOUS_TRIAL_KEY : Items.TRIAL_KEY);
    }

    static void insert(ServerLevel level, BlockPos pos, ServerPlayer player, ItemStack key) {
        VaultBlockEntity vault = (VaultBlockEntity) level.getBlockEntity(pos);
        if (vault == null) throw new IllegalStateException("vault block entity missing at " + pos);
        VaultBlockEntity.Server.tryInsertKey(level, pos, level.getBlockState(pos), vault.getConfig(),
                vault.getServerData(), vault.getSharedData(), player, key);
    }
}
