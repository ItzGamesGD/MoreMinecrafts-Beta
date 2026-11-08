package dev.itzgamesgd.moreminecrafts.boss;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Integration surface for the future royal tomb structure. The structure owns the royal chest block and
 * its placement; this class owns the encounter rules (owner-approved option B):
 * <ol>
 *     <li>The royal chest stays sealed until the guardian is killed ({@link #isRoyalChestSealed}).</li>
 *     <li>A guardian that disappears without dying, or an encounter that is ended, can be retried.</li>
 *     <li>After a normal kill the tomb never summons again.</li>
 *     <li>At most one guardian per tomb and at most one completion per tomb.</li>
 *     <li>The defeated record is world-persistent across chunk unload, relog and restart.</li>
 *     <li>An interrupted, undefeated encounter is resolved on the next royal chest interaction.</li>
 * </ol>
 * A tomb is identified by its dimension plus an anchor position (intended: the royal chest position).
 */
public final class TombGuardianEncounters {
    private TombGuardianEncounters() {}

    /** Fired once per tomb, after the bound guardian dies in a VALID victory (P9: invalid deaths no longer fire it) and the tomb is recorded DEFEATED. */
    public static final Event<Defeated> DEFEATED = EventFactory.createArrayBacked(Defeated.class,
            listeners -> (level, tombAnchor, killer) -> {
                for (Defeated listener : listeners) listener.onDefeated(level, tombAnchor, killer);
            });

    /** Fired once per tomb right after {@link #DEFEATED}, carrying the participants. Both events mean a VALID victory (P9). */
    public static final Event<Victory> VICTORY = EventFactory.createArrayBacked(Victory.class,
            listeners -> (level, tombAnchor, verdict) -> {
                for (Victory listener : listeners) listener.onVictory(level, tombAnchor, verdict);
            });

    /** Fired when the bound guardian died WITHOUT a valid victory; the tomb is released for a new attempt. */
    public static final Event<InvalidDeath> INVALID_DEATH = EventFactory.createArrayBacked(InvalidDeath.class,
            listeners -> (level, tombAnchor, verdict) -> {
                for (InvalidDeath listener : listeners) listener.onInvalidDeath(level, tombAnchor, verdict);
            });

    @FunctionalInterface
    public interface Victory { void onVictory(ServerLevel level, BlockPos tombAnchor, TombVictoryPolicy.Verdict verdict); }

    @FunctionalInterface
    public interface InvalidDeath { void onInvalidDeath(ServerLevel level, BlockPos tombAnchor, TombVictoryPolicy.Verdict verdict); }

    @FunctionalInterface
    public interface Defeated {
        void onDefeated(ServerLevel level, BlockPos tombAnchor, @Nullable LivingEntity killer);
    }

    public enum StartResult {
        /** A new guardian was spawned and begins awakening. */
        STARTED,
        /** A previous guardian vanished without dying; a fresh guardian was spawned (retry). */
        RESTARTED,
        /** A living guardian is already bound to this tomb; nothing was spawned. */
        ALREADY_ACTIVE,
        /** The bound guardian's chunk entities are still loading; nothing was spawned, interact again shortly. */
        GUARDIAN_NOT_LOADED,
        /** The guardian was defeated; the tomb never summons again (the chest is unsealed). */
        ALREADY_DEFEATED,
        /** The guardian entity could not be created or added; nothing was bound. */
        SPAWN_FAILED;

        public boolean spawned() { return this == STARTED || this == RESTARTED; }
    }

    public static TombEncounterData data(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TombEncounterData.TYPE);
    }

    public static long tombKey(BlockPos tombAnchor) { return tombAnchor.asLong(); }

    /** Royal chest seal: sealed until this tomb's guardian has been killed. */
    public static boolean isRoyalChestSealed(ServerLevel level, BlockPos tombAnchor) {
        return !data(level).isDefeated(tombKey(tombAnchor));
    }

    public static boolean isDefeated(ServerLevel level, BlockPos tombAnchor) {
        return data(level).isDefeated(tombKey(tombAnchor));
    }

    /**
     * Royal chest interaction entry point. Idempotent: repeated or concurrent interactions while a guardian
     * is bound never spawn a second guardian. {@code spawnPos} is the structure's prevalidated emergence point.
     */
    public static StartResult onRoyalChestInteraction(ServerLevel level, BlockPos tombAnchor, BlockPos spawnPos,
                                                      @Nullable ServerPlayer player) {
        TombEncounterData data = data(level);
        long tomb = tombKey(tombAnchor);
        TombEncounterData.StartDecision decision = data.decideStart(tomb, presence(level, data.active(tomb)));
        switch (decision) {
            case ALREADY_DEFEATED: return StartResult.ALREADY_DEFEATED;
            case ALREADY_ACTIVE: return StartResult.ALREADY_ACTIVE;
            case GUARDIAN_NOT_LOADED: return StartResult.GUARDIAN_NOT_LOADED;
            default: break;
        }
        TombGuardian guardian = TombGuardianEntities.TOMB_GUARDIAN.create(level, EntitySpawnReason.EVENT);
        if (guardian == null) return StartResult.SPAWN_FAILED;
        guardian.beginEncounter(tombAnchor, spawnPos, player == null ? 0.0F : player.getYRot() + 180.0F);
        if (!data.bind(tomb, guardian.getUUID(), guardian.chunkPosition().pack(), level.getGameTime())) {
            guardian.discard();
            return StartResult.SPAWN_FAILED;
        }
        if (!level.addFreshEntity(guardian)) {
            data.release(tomb, guardian.getUUID());
            return StartResult.SPAWN_FAILED;
        }
        MoreMinecrafts.LOGGER.info("Tomb Guardian encounter {} at {} in {} (guardian {}).",
                decision == TombEncounterData.StartDecision.RESTART_AFTER_LOSS ? "restarted" : "started",
                tombAnchor.toShortString(), level.dimension().identifier(), guardian.getUUID());
        return decision == TombEncounterData.StartDecision.RESTART_AFTER_LOSS ? StartResult.RESTARTED : StartResult.STARTED;
    }

    /**
     * Ends an undefeated encounter without a kill (for example when the structure decides the fight is over):
     * removes the bound guardian if it is loaded and releases the binding so the tomb can be retried.
     * Returns false when nothing was bound or the bound guardian is not loaded.
     */
    public static boolean endEncounter(ServerLevel level, BlockPos tombAnchor) {
        TombEncounterData data = data(level);
        long tomb = tombKey(tombAnchor);
        TombEncounterData.Active binding = data.active(tomb);
        if (binding == null) return false;
        Entity entity = level.getEntity(binding.guardian());
        if (entity instanceof TombGuardian guardian && guardian.isAlive()) {
            guardian.discard(); // the guardian releases its own binding on non-death removal
            return true;
        }
        if (presence(level, binding) == TombEncounterData.GuardianPresence.GONE) {
            data.release(tomb, binding.guardian());
            return true;
        }
        return false;
    }

    static TombEncounterData.GuardianPresence presence(ServerLevel level, @Nullable TombEncounterData.Active binding) {
        if (binding == null) return TombEncounterData.GuardianPresence.GONE;
        Entity entity = level.getEntity(binding.guardian());
        if (entity instanceof TombGuardian guardian && guardian.isAlive()) return TombEncounterData.GuardianPresence.ALIVE;
        return level.areEntitiesLoaded(binding.guardianChunk())
                ? TombEncounterData.GuardianPresence.GONE
                : TombEncounterData.GuardianPresence.NOT_LOADED;
    }

    // Guardian lifecycle callbacks.

    /**
     * Death of the bound guardian. {@code verdict} decides whether this is a VICTORY (valid): only then is the tomb completed, the chest
     * unsealed and {@link #DEFEATED}/{@link #VICTORY} fired. An invalid death releases the binding so the tomb can be challenged again; the
     * chest stays sealed, no reward drops (suppressed by the guardian before its death loot) and {@link #INVALID_DEATH} is fired.
     */
    static void guardianDied(ServerLevel level, TombGuardian guardian, BlockPos tombAnchor, @Nullable LivingEntity killer,
                             TombVictoryPolicy.Verdict verdict) {
        long tomb = tombKey(tombAnchor);
        if (!verdict.valid()) {
            if (!data(level).release(tomb, guardian.getUUID())) return;
            MoreMinecrafts.LOGGER.info("Tomb Guardian at {} in {} died without a valid victory ({}); the chest stays sealed and the tomb can be retried.",
                    tombAnchor.toShortString(), level.dimension().identifier(), verdict.reason());
            INVALID_DEATH.invoker().onInvalidDeath(level, tombAnchor, verdict);
            return;
        }
        if (!data(level).markVictory(tomb, guardian.getUUID())) return;
        MoreMinecrafts.LOGGER.info("Tomb Guardian defeated at {} in {}; royal chest unsealed.",
                tombAnchor.toShortString(), level.dimension().identifier());
        DEFEATED.invoker().onDefeated(level, tombAnchor, killer);
        VICTORY.invoker().onVictory(level, tombAnchor, verdict);
    }

    static void guardianLost(ServerLevel level, TombGuardian guardian, BlockPos tombAnchor) {
        if (data(level).release(tombKey(tombAnchor), guardian.getUUID()))
            MoreMinecrafts.LOGGER.info("Tomb Guardian at {} left without dying; the encounter can be retried.",
                    tombAnchor.toShortString());
    }
}
