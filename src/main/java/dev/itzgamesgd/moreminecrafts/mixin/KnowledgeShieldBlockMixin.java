package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.progression.KnowledgeEventCollectors;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ShieldItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** LivingEntity calls this only after an incoming hit has been reduced by the blocking item. */
@Mixin(LivingEntity.class)
abstract class KnowledgeShieldBlockMixin {
    @Unique private boolean moreminecrafts$blockedWithShield;

    @Inject(method = "blockedByItem", at = @At("HEAD"))
    private void moreminecrafts$captureBlockingItem(LivingEntity attacker, DamageSource source,
            float blockedDamage, boolean fullyBlocked, CallbackInfo ci) {
        Object self = this;
        moreminecrafts$blockedWithShield = self instanceof ServerPlayer player
                && player.getItemBlockingWith().getItem() instanceof ShieldItem;
    }

    @Inject(method = "blockedByItem", at = @At("TAIL"))
    private void moreminecrafts$recordShieldBlock(LivingEntity attacker, DamageSource source,
            float blockedDamage, boolean fullyBlocked, CallbackInfo ci) {
        Object self = this;
        // Fully absorbed hits can report zero residual blocked damage; blockedByItem is only
        // called after vanilla confirms that the active blocking item intercepted the hit.
        if ((blockedDamage > 0.0F || fullyBlocked) && moreminecrafts$blockedWithShield
                && self instanceof ServerPlayer player)
            KnowledgeEventCollectors.shieldBlocked(player);
        moreminecrafts$blockedWithShield = false;
    }
}
