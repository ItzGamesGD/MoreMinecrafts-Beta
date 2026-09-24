package dev.itzgamesgd.moreminecrafts;

import dev.itzgamesgd.moreminecrafts.registry.ModRegistries;
import dev.itzgamesgd.moreminecrafts.network.ModNetworking;
import dev.itzgamesgd.moreminecrafts.zombie.BombZombieEffects;
import dev.itzgamesgd.moreminecrafts.skeleton.SkeletonKingCommand;
import dev.itzgamesgd.moreminecrafts.skeleton.SkeletonKingLifecycle;
import dev.itzgamesgd.moreminecrafts.test.LiveTestCommand;
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
        BombZombieEffects.initialize();
        SkeletonKingCommand.initialize();
        SkeletonKingLifecycle.initialize();
        LiveTestCommand.initialize();
        LOGGER.info("MoreMinecrafts common initialization complete.");
    }
}
