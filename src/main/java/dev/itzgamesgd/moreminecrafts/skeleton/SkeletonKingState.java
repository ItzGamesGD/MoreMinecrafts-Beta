package dev.itzgamesgd.moreminecrafts.skeleton;

import java.util.UUID;
import net.minecraft.server.level.ServerLevel;

/** Native Skeleton state exposed only for the Stage 9A command and owned guards. */
public interface SkeletonKingState {
    boolean moreminecrafts$isSkeletonKing();

    void moreminecrafts$makeSkeletonKing();

    UUID moreminecrafts$getGuardOwner();

    void moreminecrafts$setGuardOwner(UUID owner);

    void moreminecrafts$onKingUnload(ServerLevel level, boolean destroyed);
}
