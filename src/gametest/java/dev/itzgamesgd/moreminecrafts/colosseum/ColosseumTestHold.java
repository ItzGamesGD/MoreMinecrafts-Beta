package dev.itzgamesgd.moreminecrafts.colosseum;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

/**
 * GameTest-only: with {@code -Dmoreminecrafts.st05.hold=true} the controller is held from the moment the server starts
 * (before any level is ticked), so a restart test controls exactly when the first controller tick happens and nothing
 * depends on how fast the runner is.
 */
public final class ColosseumTestHold implements ModInitializer {
    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            if (Boolean.getBoolean("moreminecrafts.st05.hold")) ColosseumRuntime.setControllerHeld(true);
        });
    }
}
