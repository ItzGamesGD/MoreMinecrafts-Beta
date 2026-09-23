package dev.itzgamesgd.moreminecrafts.skeleton;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.world.entity.monster.skeleton.Skeleton;

/** Lifecycle bridge for the concrete Skeleton King; ordinary entities are ignored. */
public final class SkeletonKingLifecycle {
    private SkeletonKingLifecycle() {
    }

    public static void initialize() {
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            if (entity instanceof Skeleton skeleton) {
                SkeletonKingState state = (SkeletonKingState) skeleton;
                if (state.moreminecrafts$isSkeletonKing()) {
                    var reason = skeleton.getRemovalReason();
                    state.moreminecrafts$onKingUnload(level, reason != null && reason.shouldDestroy());
                }
            }
        });
    }
}
