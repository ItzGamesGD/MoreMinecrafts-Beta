package dev.itzgamesgd.moreminecrafts.accessory;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;

/** Feather Charm passive: fall damage reduction. The air jump lives in {@link T1AccessoryRuntime#onJump}. */
public final class FeatherCharmAbility {
    private FeatherCharmAbility() {}

    /** Percent reduction at the equipped level, 0 when no valid Feather Charm is equipped. */
    public static double fallReductionPercent(ServerPlayer player) {
        var stack = AccessoryAbilities.equipped(player, AccessoryDefinition.FEATHER_CHARM);
        return stack.isEmpty() ? 0 : AccessoryEnhancementCatalog.valueAt(AccessoryDefinition.FEATHER_CHARM,
                FeatherCharmRules.FALL_REDUCTION_STAT, AccessoryEnhancementOperation.level(stack)).orElse(0);
    }

    /** Applies only to vanilla fall damage; every other damage type passes through unchanged. */
    public static float reduceFallDamage(ServerPlayer player, DamageSource source, float amount) {
        if (!source.is(DamageTypes.FALL) || !player.isAlive()) return amount;
        double reduction = fallReductionPercent(player);
        return reduction > 0 ? (float) (amount * (1 - reduction / 100)) : amount;
    }
}
