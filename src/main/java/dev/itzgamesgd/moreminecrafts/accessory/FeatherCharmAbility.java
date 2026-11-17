package dev.itzgamesgd.moreminecrafts.accessory;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;

/** Feather Charm passive: fall damage reduction. The air jump lives in {@link T1AccessoryRuntime#onJump}. */
public final class FeatherCharmAbility {
    private FeatherCharmAbility() {}

    /** Ender pearl, stalagmite and falling stalactite damage: same percent table, 10 second cooldown. */
    private static final Identifier ENDER_PEARL_TYPE = Identifier.withDefaultNamespace("ender_pearl");
    private static final Identifier STALAGMITE_TYPE = Identifier.withDefaultNamespace("stalagmite");
    private static final Identifier FALLING_STALACTITE_TYPE = Identifier.withDefaultNamespace("falling_stalactite");

    /** Percent reduction at the equipped level, 0 when no valid Feather Charm is equipped. */
    public static double fallReductionPercent(ServerPlayer player) {
        var stack = AccessoryAbilities.equipped(player, AccessoryDefinition.FEATHER_CHARM);
        return stack.isEmpty() ? 0 : AccessoryEnhancementCatalog.valueAt(AccessoryDefinition.FEATHER_CHARM,
                FeatherCharmRules.FALL_REDUCTION_STAT, AccessoryEnhancementOperation.level(stack)).orElse(0);
    }

    /** Ender pearl and stalactite/stalagmite sources, which share the 10 second reduction cooldown. */
    static boolean isCooldownSource(DamageSource source) {
        if (source.getDirectEntity() != null && source.getDirectEntity().getType() == EntityType.ENDER_PEARL) return true;
        return isType(source, ENDER_PEARL_TYPE) || isType(source, STALAGMITE_TYPE) || isType(source, FALLING_STALACTITE_TYPE);
    }

    private static boolean isType(DamageSource source, Identifier id) {
        return source.typeHolder().is(ResourceKey.create(Registries.DAMAGE_TYPE, id));
    }

    /**
     * Reduces Feather Charm sources. Vanilla fall damage is reduced every time. Ender pearl and stalactite/stalagmite
     * damage is reduced only when the 10 second cooldown is ready, and using it starts the cooldown.
     */
    public static float reduceFallDamage(ServerPlayer player, DamageSource source, float amount) {
        if (!player.isAlive()) return amount;
        if (isCooldownSource(source)) {
            double reduction = fallReductionPercent(player);
            if (reduction <= 0 || !AccessoryCooldownRuntime.start(player, AccessoryDefinition.FEATHER_CHARM,
                    FeatherCharmRules.NON_FALL_COOLDOWN_TICKS)) return amount;
            return (float) (amount * (1 - reduction / 100));
        }
        if (!source.is(DamageTypes.FALL)) return amount;
        double reduction = fallReductionPercent(player);
        return reduction > 0 ? (float) (amount * (1 - reduction / 100)) : amount;
    }
}
