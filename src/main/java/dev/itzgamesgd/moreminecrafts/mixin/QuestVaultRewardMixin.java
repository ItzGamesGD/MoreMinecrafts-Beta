package dev.itzgamesgd.moreminecrafts.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.itzgamesgd.moreminecrafts.progression.QuestVaultRewards;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.block.entity.vault.VaultConfig;
import net.minecraft.world.level.block.entity.vault.VaultServerData;
import net.minecraft.world.level.block.entity.vault.VaultSharedData;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Observes only the vanilla successful reward call, with the vault's real position and state. */
@Mixin(VaultBlockEntity.Server.class)
abstract class QuestVaultRewardMixin {
    @WrapOperation(method = "tryInsertKey", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/entity/vault/VaultServerData;addToRewardedPlayers(Lnet/minecraft/world/entity/player/Player;)V"))
    private static void moreminecrafts$recordReward(VaultServerData data, Player player,
            Operation<Void> original, ServerLevel level, BlockPos pos, BlockState state,
            VaultConfig config, VaultServerData methodData, VaultSharedData shared,
            Player methodPlayer, ItemStack key) {
        original.call(data, player);
        if (player instanceof ServerPlayer serverPlayer)
            QuestVaultRewards.rewarded(serverPlayer, level, pos, state);
    }
}
