package dev.itzgamesgd.moreminecrafts.accessory;

import dev.itzgamesgd.moreminecrafts.ability.AbilityAttachments;
import dev.itzgamesgd.moreminecrafts.ability.AbilityCooldowns;
import net.minecraft.server.level.ServerPlayer;

/** Accessory entries share the existing persistent player cooldown attachment. */
public final class AccessoryCooldownRuntime {
    private AccessoryCooldownRuntime() {}

    public static String id(AccessoryDefinition ability) {
        return "moreminecrafts:accessory/" + ability.registryId();
    }

    public static long remaining(ServerPlayer player, AccessoryDefinition ability) {
        return player.getAttachedOrElse(AbilityAttachments.COOLDOWNS, AbilityCooldowns.EMPTY)
                .onlineRemaining(id(ability));
    }

    public static boolean start(ServerPlayer player, AccessoryDefinition ability, long ticks) {
        AbilityCooldowns state = player.getAttachedOrElse(AbilityAttachments.COOLDOWNS, AbilityCooldowns.EMPTY);
        if (state.onlineRemaining(id(ability)) > 0) return false;
        player.setAttached(AbilityAttachments.COOLDOWNS, state.startOnline(id(ability), ticks));
        return true;
    }

    public static void tick(ServerPlayer player) {
        AbilityCooldowns state = player.getAttachedOrElse(AbilityAttachments.COOLDOWNS, AbilityCooldowns.EMPTY);
        AbilityCooldowns next = state.tickOnline();
        if (next != state) player.setAttached(AbilityAttachments.COOLDOWNS, next);
    }
}
