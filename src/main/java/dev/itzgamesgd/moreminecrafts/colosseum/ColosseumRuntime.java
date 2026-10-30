package dev.itzgamesgd.moreminecrafts.colosseum;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import dev.itzgamesgd.moreminecrafts.colosseum.ColosseumOwnership.Kind;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import org.jetbrains.annotations.Nullable;

/**
 * Wires the pure controller to the server: one controller tick per arena per level tick (whether or not the arena's
 * chunks are loaded, so the absence grace and cooldown keep running), trial-mob death/removal/unload accounting,
 * the I-ID2 load decisions for mobs, the boss and shared keys, and participant deaths.
 */
public final class ColosseumRuntime {
    private ColosseumRuntime() {}

    private static boolean initialized;
    private static volatile boolean controllerHeld;

    /**
     * Test/operator interface: while held, the controller does not tick (entity event handling continues). It is
     * never persisted and defaults to false. The restart tests hold from server start so that the first controller
     * tick after a restart happens exactly when the test releases it, with no wall-clock dependence.
     */
    public static void setControllerHeld(boolean held) { controllerHeld = held; }

    public static boolean controllerHeld() { return controllerHeld; }

    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        ColosseumOwnership.initialize();
        ColosseumDebugCommand.initialize();
        ServerTickEvents.END_LEVEL_TICK.register(ColosseumRuntime::tickLevel);
        net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
            if (level instanceof ServerLevel serverLevel) ColosseumIntegration.onPlayerBlockRemoved(serverLevel, pos);
        });
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
            if (tag == null || tag.kind() != Kind.MOB) return; // a boss death is reported by its handler, never inferred
            ColosseumArena arena = owningArena(level, tag);
            if (arena == null) return;
            boolean present = !new ColosseumServerPort(level, arena).playersInCombatZone().isEmpty();
            ColosseumController.onTrialMobDeath(arena.state(), tag.trial(), tag.allocation(), entity.getUUID(), present);
            ColosseumIntegration.data(level).setDirty();
        });
        ServerEntityEvents.ALLOW_LOAD.register(ColosseumRuntime::allowLoad);
        ServerEntityEvents.ENTITY_UNLOAD.register(ColosseumRuntime::onUnload);
    }

    private static void tickLevel(ServerLevel level) {
        if (controllerHeld) return;
        ColosseumArenaData data = ColosseumIntegration.data(level);
        if (data.readOnly()) return;
        for (ColosseumArena arena : data.arenas()) {
            ColosseumTrialState.Phase before = arena.state().phase();
            ColosseumController.tick(arena.state(), new ColosseumServerPort(level, arena));
            if (before != ColosseumTrialState.Phase.READY || arena.state().phase() != before || arena.state().hasOpenWork()
                    || !arena.state().ledger().isEmpty()) data.setDirty();
        }
    }

    private static boolean allowLoad(Entity entity, ServerLevel level, EntitySpawnReason reason, boolean ignored) {
        ColosseumOwnership.Tag tag = ColosseumOwnership.tag(entity);
        ColosseumOwnership.KeyTag keyTag = ColosseumOwnership.keyTag(entity);
        if (tag == null && keyTag == null) return true;
        ColosseumArenaData data = ColosseumIntegration.data(level);
        // Data written by a newer build is untouched: never judge (and so never delete) what it may own.
        if (data.readOnly()) return true;
        if (tag != null && !allowTagged(entity, level, data, tag)) return false;
        if (keyTag != null && !allowKey(entity, data, keyTag, reason == EntitySpawnReason.LOAD)) return false;
        return true;
    }

    private static boolean allowTagged(Entity entity, ServerLevel level, ColosseumArenaData data, ColosseumOwnership.Tag tag) {
        ColosseumArena arena = owningArena(data, tag);
        if (arena == null) {
            // An archived arena still remembers which trials timed out: its ordinary mobs stay (Q116), its boss never loads.
            RewardLedger archived = data.archived().get(tag.instance());
            if (archived != null && archived.isReleased(tag.trial()) && tag.kind() == Kind.MOB) {
                ColosseumOwnership.release(entity);
                return true;
            }
            MoreMinecrafts.LOGGER.info("ST-05 rejected {} {} of unknown arena {}.", tag.kind(), entity.getUUID(), tag.instance());
            return false;
        }
        ColosseumController.LoadDecision decision = ColosseumController.decideLoad(arena.state(), tag.trial(), tag.kind(),
                tag.allocation(), entity.getUUID());
        switch (decision) {
            case ACCEPT:
                return true;
            case ACCEPT_RELEASED:
                ColosseumOwnership.release(entity); // a time-out released it: from now on a plain external mob
                data.setDirty();
                return true;
            default:
                if (tag.kind() == Kind.BOSS)
                    ColosseumController.onBossLoadRejected(arena.state(), new ColosseumServerPort(level, arena),
                            tag.trial(), entity.getUUID());
                var allocation = arena.state().allocation(tag.allocation());
                MoreMinecrafts.LOGGER.info("ST-05 rejected orphan trial {} {} ({}) of arena {} trial {} allocation {} ({}).",
                        tag.kind(), entity.getUUID(), entity.getType().builtInRegistryHolder().key().identifier(),
                        arena.geometry().center().toShortString(), tag.trial(), tag.allocation(),
                        allocation == null ? "no allocation" : allocation.status());
                data.setDirty();
                return false;
        }
    }

    private static boolean allowKey(Entity entity, ColosseumArenaData data, ColosseumOwnership.KeyTag keyTag, boolean fromStorage) {
        ColosseumArena arena = data.arenaByInstance(keyTag.instance());
        boolean ok = arena != null
                && ColosseumController.decideKeyLoad(arena.state(), keyTag.trial(), keyTag.round(), entity.getUUID(), fromStorage);
        if (!ok) MoreMinecrafts.LOGGER.info("ST-05 rejected duplicate or stale shared key {} (trial {}, round {}).",
                entity.getUUID(), keyTag.trial(), keyTag.round());
        else data.setDirty();
        return ok;
    }

    private static void onUnload(Entity entity, ServerLevel level) {
        ColosseumOwnership.Tag tag = ColosseumOwnership.tag(entity);
        ColosseumOwnership.KeyTag keyTag = ColosseumOwnership.keyTag(entity);
        if (tag == null && keyTag == null) return;
        ColosseumArenaData data = ColosseumIntegration.data(level);
        if (data.readOnly()) return;
        Entity.RemovalReason reason = entity.getRemovalReason();
        boolean toStorage = reason == null || reason == Entity.RemovalReason.UNLOADED_TO_CHUNK
                || reason == Entity.RemovalReason.UNLOADED_WITH_PLAYER;
        if (keyTag != null) {
            ColosseumArena arena = data.arenaByInstance(keyTag.instance());
            if (arena != null) {
                ColosseumController.onKeyUnloaded(arena.state(), keyTag.trial(), keyTag.round(), entity.getUUID(),
                        entity.chunkPosition().pack(), toStorage);
                data.setDirty();
            }
        }
        if (tag == null) return;
        ColosseumArena arena = owningArena(data, tag);
        if (arena == null) return;
        ColosseumTrialState state = arena.state();
        if (tag.kind() == Kind.BOSS) {
            if (toStorage) ColosseumController.onBossUnloaded(state, tag.trial(), entity.getUUID(), entity.chunkPosition().pack());
        } else if (toStorage) {
            ColosseumController.onTrialMobUnloaded(state, tag.trial(), tag.allocation(), entity.getUUID(),
                    entity.chunkPosition().pack());
        } else if (reason == Entity.RemovalReason.KILLED) {
            // Normally already settled by AFTER_DEATH; a direct KILLED removal is evaluated as a death here.
            boolean present = !new ColosseumServerPort(level, arena).playersInCombatZone().isEmpty();
            ColosseumController.onTrialMobDeath(state, tag.trial(), tag.allocation(), entity.getUUID(), present);
        } else {
            ColosseumController.onTrialMobLost(state, tag.trial(), tag.allocation(), entity.getUUID());
        }
        data.setDirty();
    }

    @Nullable private static ColosseumArena owningArena(ServerLevel level, ColosseumOwnership.Tag tag) {
        return owningArena(ColosseumIntegration.data(level), tag);
    }

    /** New tags name the structure instance; tags written by the pre-schema build name the packed arena center. */
    @Nullable private static ColosseumArena owningArena(ColosseumArenaData data, ColosseumOwnership.Tag tag) {
        if (!tag.instance().isEmpty()) return data.arenaByInstance(tag.instance());
        return tag.legacyArena() == 0L ? null : data.arena(tag.legacyArena());
    }
}
