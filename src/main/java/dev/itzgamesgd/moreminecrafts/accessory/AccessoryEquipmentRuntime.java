package dev.itzgamesgd.moreminecrafts.accessory;

import dev.itzgamesgd.moreminecrafts.equipment.EquipmentAttachments;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;

/** Restores owner synchronization at the lifecycle boundaries where the Player entity is replaced. */
public final class AccessoryEquipmentRuntime {
    private AccessoryEquipmentRuntime() {}

    public static void initialize() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> synchronize(handler.player));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> synchronize(newPlayer));
    }

    public static void synchronize(ServerPlayer player) {
        AccessoryLoadout state = player.getAttachedOrElse(EquipmentAttachments.ACCESSORIES, AccessoryLoadout.empty());
        player.setAttached(EquipmentAttachments.ACCESSORIES, new AccessoryLoadout(state.stacks()));
    }
}
