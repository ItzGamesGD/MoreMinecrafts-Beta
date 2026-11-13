package dev.itzgamesgd.moreminecrafts.st01d7;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.world.entity.npc.villager.Villager;

/** ST-01 P8-D7 experiment wiring (gametest source set only; everything is inert unless St01D7Placeholders.enabled). */
public final class St01D7Init implements ModInitializer {
    @Override
    public void onInitialize() {
        St01D7Placeholders.registerTicket();
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> { if (entity instanceof Villager v) St01D7Placeholders.onLoad(v, level); });
        ServerTickEvents.END_SERVER_TICK.register(server -> St01D7Placeholders.tick());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> St01D7Placeholders.reset());
    }
}
