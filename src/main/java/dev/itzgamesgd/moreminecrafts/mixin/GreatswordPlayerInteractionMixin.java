package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.equipment.GreatswordItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Runs the Greatsword before trades, taming, and other consuming entity interactions. */
@Mixin(Player.class)
abstract class GreatswordPlayerInteractionMixin {
    @Inject(method = "interactOn", at = @At("HEAD"), cancellable = true)
    private void moreminecrafts$sweepBeforeEntityInteraction(Entity entity, InteractionHand hand, Vec3 hit,
                                                              CallbackInfoReturnable<InteractionResult> cir) {
        Player player = (Player) (Object) this;
        if (player.isSpectator() || !(entity instanceof LivingEntity)) return;
        var stack = player.getItemInHand(hand);
        if (stack.getItem() instanceof GreatswordItem sword)
            cir.setReturnValue(sword.useSweep(player.level(), player, hand));
    }
}
