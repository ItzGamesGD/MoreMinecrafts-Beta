package dev.itzgamesgd.moreminecrafts.ability;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** A server-side active ability, independent of its eventual input or equipment slot. */
public interface ActiveEquipmentAbility {
    Identifier id();

    int cooldownTicks();

    default boolean canActivate(ServerPlayer player, ItemStack source) {
        return true;
    }

    /** @return whether activation succeeded and should begin the cooldown */
    boolean activate(ServerPlayer player, ItemStack source);
}
