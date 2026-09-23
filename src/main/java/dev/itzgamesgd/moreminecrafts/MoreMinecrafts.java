package dev.itzgamesgd.moreminecrafts;

import dev.itzgamesgd.moreminecrafts.registry.ModRegistries;
import dev.itzgamesgd.moreminecrafts.network.ModNetworking;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MoreMinecrafts implements ModInitializer {
    public static final String MOD_ID = "moreminecrafts";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModRegistries.initialize();
        ModNetworking.initialize();
        LOGGER.info("MoreMinecrafts common initialization complete.");
    }
}
