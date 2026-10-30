package dev.itzgamesgd.moreminecrafts.colosseum;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Public surface for the later ST-05 stages. The structure registers its arena geometry and protection mask; the R7
 * Immortal Gladiator, the R3/R5 supply chests and the wave-key/reward system attach through handlers and events.
 * Nothing here invents those features: without a boss handler the trial waits in BOSS, without a supply handler no
 * chests appear, and {@link #WAVE_CLEARED} only reports the once-only cleared-wave record.
 */
public final class ColosseumIntegration {
    private ColosseumIntegration() {}

    /** R7 boss integration. The first handler that {@link #handles} an arena owns its boss. */
    public interface BossHandler {
        boolean handles(ServerLevel level, ColosseumArena arena);

        /**
         * Materialize the boss for {@code trialId}. Call {@code bind(entityUuid, chunk)} <em>before</em> adding the
         * entity to the world (as the mob spawn does), so the controller can track it and accept its own load.
         * Return false to retry next tick. Report the boss' death via {@link #reportBossDefeated}.
         */
        boolean start(ServerLevel level, ColosseumArena arena, UUID trialId, List<UUID> lockedParticipants,
                      ColosseumWorldPort.Binder bind);

        /**
         * Called after the boss left the world or its trial ended, to clean up outside the entity (boss bar,
         * summons). It must not create loot, XP, a death presentation or any reward. Removing the boss entity itself
         * is the controller's job, never the handler's.
         */
        void onRemoved(ServerLevel level, ColosseumArena arena, UUID trialId, ColosseumWorldPort.RemovalCause cause);
    }

    /** R3/R5 public supply chests (contents and placement are a later stage). */
    public interface SupplyHandler {
        boolean handles(ServerLevel level, ColosseumArena arena);

        List<BlockPos> place(ServerLevel level, ColosseumArena arena, UUID trialId, int clearedRound, int chestCount);

        /** Remove only this trial's generated chests and leftovers, never player chests. */
        void cleanup(ServerLevel level, ColosseumArena arena, UUID trialId, List<BlockPos> positions);
    }

    /**
     * Q91 protection: which blocks the boss must never break (original structure blocks, floor, walls, stands,
     * decorations, Vaults, the crystal, unbreakable blocks, player construction). Block type alone is not enough, so
     * the structure stage (C4) supplies this; {@link #journalBreak} refuses to work without one.
     */
    @FunctionalInterface
    public interface StructureMask {
        /** Additional runtime protection; durable source and player positions are stored by {@link StructureMaskSnapshot}. */
        boolean isProtected(ServerLevel level, ColosseumArena arena, BlockPos pos, BlockState state);
    }

    @FunctionalInterface
    public interface WaveCleared {
        void onWaveCleared(ServerLevel level, ColosseumArena arena, WaveRecord record);
    }

    @FunctionalInterface
    public interface TrialEnded {
        void onTrialEnded(ServerLevel level, ColosseumArena arena, UUID trialId, ColosseumTrialState.Outcome outcome);
    }

    public enum EndingReason { TIME_OUT }

    /** Q117: the central crystal's ending presentation. A notification only: it cannot change or delay anything. */
    @FunctionalInterface
    public interface CrystalEnding {
        void onCrystalEnding(ServerLevel level, ColosseumArena arena, UUID trialId, EndingReason reason);
    }

    /** A boss vanish work item finished (the boss is gone). */
    @FunctionalInterface
    public interface BossVanished {
        void onBossVanished(ServerLevel level, ColosseumArena arena, UUID trialId);
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

    public static final Event<CrystalEnding> CRYSTAL_ENDING = EventFactory.createArrayBacked(CrystalEnding.class,
            listeners -> (level, arena, trialId, reason) -> {
                for (CrystalEnding listener : listeners) listener.onCrystalEnding(level, arena, trialId, reason);
            });

    public static final Event<BossVanished> BOSS_VANISHED = EventFactory.createArrayBacked(BossVanished.class,
            listeners -> (level, arena, trialId) -> {
                for (BossVanished listener : listeners) listener.onBossVanished(level, arena, trialId);
            });

    static final List<BossHandler> BOSS_HANDLERS = new CopyOnWriteArrayList<>();
    static final List<SupplyHandler> SUPPLY_HANDLERS = new CopyOnWriteArrayList<>();
    /** Optional runtime extension for integrations; durable provenance lives on each saved arena. */
    @Nullable private static volatile StructureMask structureMask;

    public static void registerBossHandler(BossHandler handler) { BOSS_HANDLERS.add(handler); }

    public static void registerSupplyHandler(SupplyHandler handler) { SUPPLY_HANDLERS.add(handler); }

    /** Optional runtime extension layered on top of each arena's complete persisted source mask. */
    public static void setStructureMask(@Nullable StructureMask mask) { structureMask = mask; }

    @Nullable static BossHandler bossHandler(ServerLevel level, ColosseumArena arena) {
        for (BossHandler handler : BOSS_HANDLERS) if (handler.handles(level, arena)) return handler;
        return null;
    }

    @Nullable static SupplyHandler supplyHandler(ServerLevel level, ColosseumArena arena) {
        for (SupplyHandler handler : SUPPLY_HANDLERS) if (handler.handles(level, arena)) return handler;
        return null;
    }

    public static ColosseumArenaData data(ServerLevel level) {
        ColosseumArenaData data = level.getDataStorage().computeIfAbsent(ColosseumArenaData.TYPE);
        data.adoptDimension(level.dimension().identifier().toString());
        return data;
    }

    /** Registers (idempotently) a test arena whose exact center is {@code geometry.center()}. */
    public static ColosseumArena registerArena(ServerLevel level, ColosseumArenaGeometry geometry) {
        return registerArena(level, geometry,
                ColosseumArena.testInstanceId(level.dimension().identifier().toString(), geometry.center()));
    }

    /** Registers (idempotently) an arena of the structure instance {@code instanceId} (worldgen, stage C4). */
    public static ColosseumArena registerArena(ServerLevel level, ColosseumArenaGeometry geometry, String instanceId) {
        return data(level).register(geometry, instanceId);
    }

    /**
     * C4 registration contract: persist every position occupied by the placed source structure. The list must come
     * from the actual placement/template result, not inferred from block types or arena dimensions.
     */
    public static ColosseumArena registerArena(ServerLevel level, ColosseumArenaGeometry geometry, String instanceId,
                                               Collection<BlockPos> sourceStructurePositions) {
        ColosseumArena arena = data(level).register(geometry, instanceId);
        recordStructureMask(level, arena, sourceStructurePositions);
        return arena;
    }

    /** Completes or extends source occupancy from the actual structure placement result. */
    public static void recordStructureMask(ServerLevel level, ColosseumArena arena,
                                           Collection<BlockPos> sourceStructurePositions) {
        if (data(level).arenaByInstance(arena.instanceId()) != arena)
            throw new IllegalArgumentException("arena is not registered in this level");
        arena.structureMask().recordStructure(sourceStructurePositions);
        data(level).setDirty();
    }

    /** Records a confirmed player placement or other player modification in the persistent protection mask. */
    public static boolean protectArenaBlock(ServerLevel level, ColosseumArena arena, BlockPos pos) {
        if (data(level).arenaByInstance(arena.instanceId()) != arena) return false;
        if (!arena.structureMask().protect(pos)) return false;
        data(level).setDirty();
        return true;
    }

    @Nullable public static ColosseumArena arena(ServerLevel level, BlockPos center) {
        return data(level).arena(center.asLong());
    }

    public enum UnregisterResult {
        REMOVED, NOT_FOUND,
        /** The ledger holds records; they must never be deleted (Q115-B). Use {@link #archiveAndUnregister}. */
        REFUSED_HAS_RECORDS,
        /** A boss vanish or restore-journal entry is unfinished; removing the arena would lose it. */
        REFUSED_OPEN_WORK
    }

    /**
     * Removes an arena that holds nothing that must survive. An arena with ledger records or unfinished work is
     * refused and left untouched (D3); an active trial of a removable arena is conclusively reset first.
     */
    public static UnregisterResult unregisterArena(ServerLevel level, BlockPos center) {
        ColosseumArena arena = arena(level, center);
        if (arena == null) return UnregisterResult.NOT_FOUND;
        if (arena.state().hasOpenWork()) return UnregisterResult.REFUSED_OPEN_WORK;
        if (!arena.state().ledger().isEmpty()) return UnregisterResult.REFUSED_HAS_RECORDS;
        ColosseumController.forceReset(arena.state(), new ColosseumServerPort(level, arena));
        data(level).remove(arena);
        return UnregisterResult.REMOVED;
    }

    /**
     * Operator path for removing an arena that has records: the ledger moves to the archive under the arena's
     * instance id and is restored if the same instance registers again. Unfinished work still blocks removal.
     */
    public static UnregisterResult archiveAndUnregister(ServerLevel level, BlockPos center) {
        ColosseumArena arena = arena(level, center);
        if (arena == null) return UnregisterResult.NOT_FOUND;
        if (arena.state().hasOpenWork()) return UnregisterResult.REFUSED_OPEN_WORK;
        ColosseumController.forceReset(arena.state(), new ColosseumServerPort(level, arena));
        if (arena.state().hasOpenWork()) return UnregisterResult.REFUSED_OPEN_WORK;
        data(level).archiveAndRemove(arena);
        return UnregisterResult.REMOVED;
    }

    public enum JournalResult {
        RECORDED, ALREADY_RECORDED,
        /** Q91: structure, Vault, crystal, unbreakable, block-entity or masked block: the boss must not break it. */
        PROTECTED,
        /** The trial is not the arena's active trial. */
        NOT_ACTIVE
    }

    /**
     * Called by the boss <em>before</em> it destroys a block. Records the block's state for restoration after the
     * trial (Q91, Q117) and tells the boss whether it may break it. Never record after breaking: a crash in between
     * must find an entry for an intact block (a harmless no-op), not a broken block without an entry.
     *
     * @throws IllegalStateException until either a complete persisted C4 position snapshot or a runtime mask is
     *         available; block type alone cannot tell original structure blocks from external natural blocks (Q91)
     */
    public static JournalResult journalBreak(ServerLevel level, ColosseumArena arena, UUID trialId, BlockPos pos,
                                             BlockState prior, @Nullable BlockState left) {
        StructureMask mask = structureMask;
        if (!arena.structureMask().complete())
            throw new IllegalStateException("no complete ST-05 structure mask registered; refusing to journal "
                    + "boss block destruction (Q91 requires source-position provenance)");
        ColosseumTrialState state = arena.state();
        if (state.trialId() == null || !state.trialId().equals(trialId) || !state.phase().active()) return JournalResult.NOT_ACTIVE;
        if (prior.is(Blocks.VAULT) || prior.getDestroySpeed(level, pos) < 0.0F || level.getBlockEntity(pos) != null
                || arena.structureMask().protects(pos)
                || (mask != null && mask.isProtected(level, arena, pos, prior))) return JournalResult.PROTECTED;
        var added = state.journal().add(trialId, pos.asLong(), NbtUtils.writeBlockState(prior),
                left == null ? null : NbtUtils.writeBlockState(left));
        data(level).setDirty();
        return added == RestoreJournal.AddResult.RECORDED ? JournalResult.RECORDED : JournalResult.ALREADY_RECORDED;
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
     * (timers advance, entities do not). Used to check 10/35/60-second, 65-minute and cooldown timing without
     * waiting. Independent of the controller hold used by the restart tests.
     */
    public static void advanceController(ServerLevel level, BlockPos center, int ticks) {
        ColosseumArena arena = arena(level, center);
        if (arena == null) return;
        ColosseumServerPort port = new ColosseumServerPort(level, arena);
        for (int i = 0; i < ticks; i++) ColosseumController.tick(arena.state(), port);
        data(level).setDirty();
    }
}
