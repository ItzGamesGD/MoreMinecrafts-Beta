package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.enhancement.EnhancementOperation;
import dev.itzgamesgd.moreminecrafts.enhancement.MaceEnhancement;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.MaceItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Changes only the fall-distance read used by vanilla smash damage; ordinary melee is untouched. */
@Mixin(MaceItem.class)
abstract class MaceItemMixin {
    @Redirect(method = "getAttackDamageBonus", at = @At(value = "FIELD",
            target = "Lnet/minecraft/world/entity/LivingEntity;fallDistance:D"))
    private double moreminecrafts$effectiveSmashFallDistance(LivingEntity attacker) {
        return MaceEnhancement.effectiveFallDistance(attacker.fallDistance,
                EnhancementOperation.level(attacker.getWeaponItem()));
    }
}
