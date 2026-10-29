package dev.itzgamesgd.moreminecrafts.colosseum;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * Wires the pure controller to the server: one controller tick per arena per level tick (whether or not the arena's
 * chunks are loaded, so the absence grace and cooldown keep running), trial-mob death/removal/unload accounting,
 * orphan rejection on load, and participant deaths.
 */
public final class ColosseumRuntime {
    private ColosseumRuntime() {}

    private static boolean initialized;

    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        ColosseumOwnership.initialize();
        ColosseumDebugCommand.initialize();
        ServerTickEvents.END_LEVEL_TICK.register(ColosseumRuntime::tickLevel);
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity.level() instanceof ServerLevel level)) return;
            if (entity instanceof ServerPlayer player) {
                for (ServerLevel any : level.getServer().getAllLevels()) {
                    ColosseumArenaData data = ColosseumIntegration.data(any);
                    for (ColosseumArena arena : data.arenas()) {
                        ColosseumController.onPlayerDeath(arena.state(), player.getUUID());
                        data.setDirty();
                    }
                }
                return;
            }
            ColosseumOwnership.Tag tag = ColosseumOwnership.tag(entity);
            ColosseumArena arena = tag == null ? null : owningArena(level, tag);
            if (arena == null) return;
            boolean present = !new ColosseumServerPort(level, arena).playersInCombatZone().isEmpty();
            ColosseumController.onTrialMobDeath(arena.state(), tag.trial(), tag.allocation(), entity.getUUID(), present);
            ColosseumIntegration.data(level).setDirty();
        });
        ServerEntityEvents.ALLOW_LOAD.register((entity, level, reason, fresh) -> {
            ColosseumOwnership.Tag tag = ColosseumOwnership.tag(entity);
            if (tag == null) return true;
            ColosseumArena arena = owningArena(level, tag);
            boolean owned = arena != null
                    && ColosseumController.ownsEntity(arena.state(), tag.trial(), tag.allocation(), entity.getUUID());
            if (!owned) {
                var allocation = arena == null ? null : arena.state().allocation(tag.allocation());
                MoreMinecrafts.LOGGER.info("ST-05 rejected orphan trial mob {} ({}) of arena {} trial {} allocation {} ({}).",
                        entity.getUUID(), entity.getType().builtInRegistryHolder().key().identifier(),
                        arena == null ? "<none>" : arena.geometry().center().toShortString(), tag.trial(),
                        tag.allocation(), allocation == null ? "no allocation" : allocation.status());
            }
            return owned;
        });
        ServerEntityEvents.ENTITY_UNLOAD.register(ColosseumRuntime::onUnload);
    }

    private static void tickLevel(ServerLevel level) {
        ColosseumArenaData data = ColosseumIntegration.data(level);
        for (ColosseumArena arena : data.arenas()) {
            ColosseumTrialState.Phase before = arena.state().phase();
            ColosseumController.tick(arena.state(), new ColosseumServerPort(level, arena));
            if (before != ColosseumTrialState.Phase.READY || arena.state().phase() != before) data.setDirty();
        }
    }

    private static void onUnload(Entity entity, ServerLevel level) {
        ColosseumOwnership.Tag tag = ColosseumOwnership.tag(entity);
        if (tag == null) return;
        ColosseumArena arena = owningArena(level, tag);
        if (arena == null) return;
        Entity.RemovalReason reason = entity.getRemovalReason();
        ColosseumTrialState state = arena.state();
        if (reason == null || reason == Entity.RemovalReason.UNLOADED_TO_CHUNK
                || reason == Entity.RemovalReason.UNLOADED_WITH_PLAYER) {
            ColosseumController.onTrialMobUnloaded(state, tag.trial(), tag.allocation(), entity.getUUID(),
                    entity.chunkPosition().pack());
        } else if (reason == Entity.RemovalReason.KILLED) {
            // Normally already settled by AFTER_DEATH; a direct KILLED removal is evaluated as a death here.
            boolean present = !new ColosseumServerPort(level, arena).playersInCombatZone().isEmpty();
            ColosseumController.onTrialMobDeath(state, tag.trial(), tag.allocation(), entity.getUUID(), present);
        } else {
            ColosseumController.onTrialMobLost(state, tag.trial(), tag.allocation(), entity.getUUID());
        }
        ColosseumIntegration.data(level).setDirty();
    }

    @Nullable private static ColosseumArena owningArena(ServerLevel level, ColosseumOwnership.Tag tag) {
        return ColosseumIntegration.data(level).arena(tag.arena());
    }
}
