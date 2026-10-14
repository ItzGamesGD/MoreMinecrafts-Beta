package dev.itzgamesgd.moreminecrafts.equipment;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public final class EquipmentRuntime {
    private EquipmentRuntime() {}
    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (var player : server.getPlayerList().getPlayers())
                if (player.containerMenu instanceof PortableFurnaceMenu menu) menu.serverTick(player);
        });
    }
}
