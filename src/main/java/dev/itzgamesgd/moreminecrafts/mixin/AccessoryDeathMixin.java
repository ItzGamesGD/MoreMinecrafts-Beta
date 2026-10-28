package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.accessory.AccessoryDeathDrops;
import java.util.List;
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
abstract class AccessoryDeathMixin {
    @Inject(method = "die", at = @At("HEAD"))
    private void moreminecrafts$dropEquippedAccessories(DamageSource source, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        boolean keepInventory = player.level().getGameRules().get(GameRules.KEEP_INVENTORY);
        List<ItemStack> drops = AccessoryDeathDrops.detachForDeath(player, keepInventory);
        ServerLevel level = (ServerLevel) player.level();
        for (ItemStack stack : drops) player.spawnAtLocation(level, stack);
    }
}
