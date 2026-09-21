package dev.itzgamesgd.moreminecrafts.client;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import net.fabricmc.api.ClientModInitializer;

public final class MoreMinecraftsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MoreMinecrafts.LOGGER.info("MoreMinecrafts client initialization complete.");
    }
}
