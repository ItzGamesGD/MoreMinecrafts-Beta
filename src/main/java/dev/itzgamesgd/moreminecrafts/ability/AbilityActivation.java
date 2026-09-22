package dev.itzgamesgd.moreminecrafts.ability;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Performs only the server-authoritative check, activation, and successful cooldown start. */
public final class AbilityActivation {
    private AbilityActivation() {
    }

    public static Result tryActivate(ServerPlayer player, ItemStack source, ActiveEquipmentAbility ability) {
        long currentTick = player.serverLevel().getGameTime();
        AbilityCooldowns cooldowns = player.getAttachedOrCreate(AbilityAttachments.COOLDOWNS);
        String abilityId = ability.id().toString();

        if (!cooldowns.isReady(abilityId, currentTick)) {
            return Result.ON_COOLDOWN;
        }
        if (!ability.canActivate(player, source)) {
            return Result.INELIGIBLE;
        }
        if (!ability.activate(player, source)) {
            return Result.FAILED;
        }

        player.setAttached(
                AbilityAttachments.COOLDOWNS,
                cooldowns.start(abilityId, currentTick, ability.cooldownTicks())
        );
        return Result.ACTIVATED;
    }

    public enum Result {
        ACTIVATED,
        ON_COOLDOWN,
        INELIGIBLE,
        FAILED
    }
}
