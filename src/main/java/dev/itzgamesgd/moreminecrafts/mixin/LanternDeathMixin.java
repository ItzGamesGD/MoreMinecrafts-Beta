package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.equipment.EquipmentAttachments;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
abstract class LanternDeathMixin {
    @Inject(method = "die", at = @At("HEAD"))
    private void moreminecrafts$dropLanternNormally(DamageSource source, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (player.level().getGameRules().get(GameRules.KEEP_INVENTORY)) return;
        ItemStack lantern = player.getAttachedOrElse(EquipmentAttachments.LANTERN, ItemStack.EMPTY);
        if (!lantern.isEmpty()) player.spawnAtLocation((ServerLevel) player.level(), lantern.copy());
        player.setAttached(EquipmentAttachments.LANTERN, ItemStack.EMPTY);
    }
}
