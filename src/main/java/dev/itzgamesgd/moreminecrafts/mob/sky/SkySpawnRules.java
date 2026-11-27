package dev.itzgamesgd.moreminecrafts.mob.sky;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;

/** Opt-in gate for future C2 ground variants, in addition to their vanilla species checks. */
public final class SkySpawnRules {
    private SkySpawnRules() {}
    public static boolean allows(ServerLevel level, BlockPos feet, EntitySpawnReason reason) {
        return reason == EntitySpawnReason.NATURAL && SkyIslandHabitat.find(level, feet).isPresent();
    }
}
