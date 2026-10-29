package dev.itzgamesgd.moreminecrafts.colosseum;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * Public surface for the later ST-05 stages. The structure registers its arena geometry; the R7 Immortal Gladiator,
 * the R3/R5 supply chests and the wave-key/reward system attach through handlers and events. Nothing here invents
 * those features: without a boss handler the trial waits in BOSS, without a supply handler no chests appear, and
 * {@link #WAVE_CLEARED} only reports the once-only cleared-wave record.
 */
public final class ColosseumIntegration {
    private ColosseumIntegration() {}

    /** R7 boss integration. The first handler that {@link #handles} an arena owns its boss. */
    public interface BossHandler {
        boolean handles(ServerLevel level, ColosseumArena arena);

        /** Materialize the boss for {@code trialId}; return false to retry next tick. Report death via {@link #reportBossDefeated}. */
        boolean start(ServerLevel level, ColosseumArena arena, UUID trialId, List<UUID> lockedParticipants);

        /** Remove this trial's boss (failure/reset) without loot or XP; also called after success. */
        void cleanup(ServerLevel level, ColosseumArena arena, UUID trialId);
    }

    /** R3/R5 public supply chests (contents and placement are a later stage). */
    public interface SupplyHandler {
        boolean handles(ServerLevel level, ColosseumArena arena);

        List<BlockPos> place(ServerLevel level, ColosseumArena arena, UUID trialId, int clearedRound, int chestCount);

        /** Remove only this trial's generated chests and leftovers, never player chests. */
        void cleanup(ServerLevel level, ColosseumArena arena, UUID trialId, List<BlockPos> positions);
    }

    @FunctionalInterface
    public interface WaveCleared {
        void onWaveCleared(ServerLevel level, ColosseumArena arena, ColosseumTrialState.WaveRecord record);
    }

    @FunctionalInterface
    public interface TrialEnded {
        void onTrialEnded(ServerLevel level, ColosseumArena arena, UUID trialId, ColosseumTrialState.Outcome outcome);
    }

    /** Exactly once per (trial, round) after the wave's payout record is stored. Reward systems hook here. */
    public static final Event<WaveCleared> WAVE_CLEARED = EventFactory.createArrayBacked(WaveCleared.class,
            listeners -> (level, arena, record) -> {
                for (WaveCleared listener : listeners) listener.onWaveCleared(level, arena, record);
            });

    public static final Event<TrialEnded> TRIAL_ENDED = EventFactory.createArrayBacked(TrialEnded.class,
            listeners -> (level, arena, trialId, outcome) -> {
                for (TrialEnded listener : listeners) listener.onTrialEnded(level, arena, trialId, outcome);
            });

    static final List<BossHandler> BOSS_HANDLERS = new CopyOnWriteArrayList<>();
    static final List<SupplyHandler> SUPPLY_HANDLERS = new CopyOnWriteArrayList<>();

    public static void registerBossHandler(BossHandler handler) { BOSS_HANDLERS.add(handler); }

    public static void registerSupplyHandler(SupplyHandler handler) { SUPPLY_HANDLERS.add(handler); }

    @Nullable static BossHandler bossHandler(ServerLevel level, ColosseumArena arena) {
        for (BossHandler handler : BOSS_HANDLERS) if (handler.handles(level, arena)) return handler;
        return null;
    }

    @Nullable static SupplyHandler supplyHandler(ServerLevel level, ColosseumArena arena) {
        for (SupplyHandler handler : SUPPLY_HANDLERS) if (handler.handles(level, arena)) return handler;
        return null;
    }

    public static ColosseumArenaData data(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(ColosseumArenaData.TYPE);
    }

    /** Registers (idempotently) the arena whose exact center is {@code geometry.center()}. */
    public static ColosseumArena registerArena(ServerLevel level, ColosseumArenaGeometry geometry) {
        return data(level).register(geometry);
    }

    @Nullable public static ColosseumArena arena(ServerLevel level, BlockPos center) {
        return data(level).arena(center.asLong());
    }

    /** Removes an arena; an active trial is conclusively reset first so its mobs and chests are cleaned up. */
    public static boolean unregisterArena(ServerLevel level, BlockPos center) {
        ColosseumArena arena = arena(level, center);
        if (arena == null) return false;
        ColosseumController.forceReset(arena.state(), new ColosseumServerPort(level, arena));
        data(level).remove(arena.key());
        return true;
    }

    /** The R7 boss died: success at boss death, then the 78,000-tick cooldown. */
    public static boolean reportBossDefeated(ServerLevel level, BlockPos center, UUID trialId) {
        ColosseumArena arena = arena(level, center);
        if (arena == null) return false;
        boolean done = ColosseumController.onBossDefeated(arena.state(), new ColosseumServerPort(level, arena), trialId);
        if (done) data(level).setDirty();
        return done;
    }

    public static boolean forceReset(ServerLevel level, BlockPos center) {
        ColosseumArena arena = arena(level, center);
        if (arena == null) return false;
        boolean reset = ColosseumController.forceReset(arena.state(), new ColosseumServerPort(level, arena));
        if (reset) data(level).setDirty();
        return reset;
    }

    /** Operator/test helper: ends the cooldown now. */
    public static boolean clearCooldown(ServerLevel level, BlockPos center) {
        ColosseumArena arena = arena(level, center);
        if (arena == null) return false;
        boolean cleared = ColosseumController.clearCooldown(arena.state());
        if (cleared) data(level).setDirty();
        return cleared;
    }

    /**
     * Test interface: runs {@code ticks} extra controller ticks for one arena within the current server tick
     * (timers advance, entities do not). Used to check 10/35/60-second and cooldown timing without waiting.
     */
    public static void advanceController(ServerLevel level, BlockPos center, int ticks) {
        ColosseumArena arena = arena(level, center);
        if (arena == null) return;
        ColosseumServerPort port = new ColosseumServerPort(level, arena);
        for (int i = 0; i < ticks; i++) ColosseumController.tick(arena.state(), port);
        data(level).setDirty();
    }
}
