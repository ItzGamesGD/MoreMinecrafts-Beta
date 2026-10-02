package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.equipment.GreatswordItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
abstract class ShieldItemMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void moreminecrafts$blockShieldWithGreatsword(Level level, Player player, InteractionHand hand,
                                                           CallbackInfoReturnable<InteractionResult> cir) {
        if ((Object) this instanceof ShieldItem && player.getMainHandItem().getItem() instanceof GreatswordItem)
            cir.setReturnValue(InteractionResult.FAIL);
    }
}
