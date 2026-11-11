package dev.itzgamesgd.moreminecrafts.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.itzgamesgd.moreminecrafts.skytower.SkyTowerVaultRewards;
import java.util.List;
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
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Ensures Sky Tower vaults use the authoritative custom reward pool at runtime,
 * queues any successful rooftop special rewards into the vanilla Vault ejection list,
 * and notifies the unified Sky Tower vault reward framework upon key insertion.
 */
@Mixin(VaultBlockEntity.Server.class)
abstract class SkyTowerVaultRewardMixin {
    @ModifyVariable(method = "tryInsertKey", at = @At("HEAD"), argsOnly = true)
    private static VaultConfig moreminecrafts$scopeSkyTowerVaultConfig(VaultConfig config,
            ServerLevel level, BlockPos pos) {
        return SkyTowerVaultRewards.resolveVaultConfig(level, pos, config);
    }

    @Inject(method = "resolveItemsToEject", at = @At("RETURN"), cancellable = true)
    private static void moreminecrafts$appendRooftopSpecialRewards(
            ServerLevel level, VaultConfig config, BlockPos pos, Player player, ItemStack key,
            CallbackInfoReturnable<List<ItemStack>> cir) {
        if (player instanceof ServerPlayer serverPlayer) {
            List<ItemStack> items = cir.getReturnValue();
            List<ItemStack> updated = SkyTowerVaultRewards.appendSpecialRewards(serverPlayer, level, pos, items);
            if (updated != items) {
                cir.setReturnValue(updated);
            }
        }
    }

    @WrapOperation(method = "tryInsertKey", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/entity/vault/VaultServerData;addToRewardedPlayers(Lnet/minecraft/world/entity/player/Player;)V"))
    private static void moreminecrafts$onSkyTowerVaultReward(VaultServerData data, Player player,
            Operation<Void> original, ServerLevel level, BlockPos pos, BlockState state,
            VaultConfig config, VaultServerData methodData, VaultSharedData shared,
            Player methodPlayer, ItemStack key) {
        original.call(data, player);
        if (player instanceof ServerPlayer serverPlayer) {
            SkyTowerVaultRewards.onVaultRewarded(serverPlayer, level, pos, config);
        }
    }
}
