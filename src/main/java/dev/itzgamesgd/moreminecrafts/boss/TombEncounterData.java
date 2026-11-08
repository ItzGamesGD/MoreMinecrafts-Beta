package dev.itzgamesgd.moreminecrafts.boss;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Per-dimension, world-persistent royal tomb encounter state, keyed by the packed tomb anchor position
 * (the royal chest). A tomb is DORMANT when absent, ACTIVE while exactly one guardian is bound to it and
 * DEFEATED forever after a normal kill. Only DEFEATED is permanent; an ACTIVE binding whose guardian is
 * confirmed gone without dying is cleared so the next royal chest interaction starts a fresh encounter.
 */
public final class TombEncounterData extends SavedData {
    private static final Codec<Active> ACTIVE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("tomb").forGetter(Active::tomb),
            UUIDUtil.CODEC.fieldOf("guardian").forGetter(Active::guardian),
            Codec.LONG.fieldOf("guardian_chunk").forGetter(Active::guardianChunk),
            Codec.LONG.fieldOf("started").forGetter(Active::startedGameTime)
    ).apply(instance, Active::new));

    private static final Codec<SavedState> STATE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.listOf().optionalFieldOf("defeated", List.of()).forGetter(SavedState::defeated),
            ACTIVE_CODEC.listOf().optionalFieldOf("active", List.of()).forGetter(SavedState::active),
            Codec.LONG.listOf().optionalFieldOf("victories", List.of()).forGetter(SavedState::victories)
    ).apply(instance, SavedState::new));

    private static final Codec<TombEncounterData> CODEC = STATE_CODEC.xmap(TombEncounterData::fromState, TombEncounterData::toState);

    public static final SavedDataType<TombEncounterData> TYPE = new SavedDataType<>(
            Identifier.parse("moreminecrafts:tomb_guardian_encounters"), TombEncounterData::new, CODEC, DataFixTypes.LEVEL);

    /**
     * Tombs that are COMPLETED (never summon again, chest unsealed). Before ST-01 P9 this was written for every credited guardian death;
     * since P9 it is written only for a valid victory. Entries without a matching {@link #victories} entry are LEGACY records of unknown
     * validity: they stay honoured (compatibility) but are never converted to victories automatically.
     */
    private final Set<Long> defeated = new LinkedHashSet<>();
    /** Tombs whose guardian was killed in a VALID victory (P9). Subset of {@link #defeated}. Only these advance the secret entrance. */
    private final Set<Long> victories = new LinkedHashSet<>();
    private final Map<Long, Active> active = new LinkedHashMap<>();

    public TombEncounterData() {}

    private static TombEncounterData fromState(SavedState state) {
        TombEncounterData data = new TombEncounterData();
        data.defeated.addAll(state.defeated());
        for (long v : state.victories()) { data.victories.add(v); data.defeated.add(v); }
        for (Active binding : state.active())
            if (!data.defeated.contains(binding.tomb())) data.active.put(binding.tomb(), binding);
        return data;
    }

    private SavedState toState() {
        return new SavedState(new ArrayList<>(defeated), new ArrayList<>(active.values()), new ArrayList<>(victories));
    }

    /** What the server currently knows about an ACTIVE tomb's bound guardian. */
    public enum GuardianPresence {
        /** The bound guardian entity is loaded and alive. */
        ALIVE,
        /** The guardian's last known chunk has its entities loaded and the guardian is not there: it is gone. */
        GONE,
        /** The guardian's chunk entities are not loaded yet; it may still exist on disk. */
        NOT_LOADED
    }

    public enum StartDecision {
        /** No encounter is bound: spawn a new guardian. */
        START,
        /** A previous guardian vanished without dying: its binding was cleared, spawn a new guardian. */
        RESTART_AFTER_LOSS,
        /** The bound guardian is alive; never spawn a second one. */
        ALREADY_ACTIVE,
        /** The bound guardian may still be stored in an unloaded chunk; refuse until it can be resolved. */
        GUARDIAN_NOT_LOADED,
        /** The guardian was killed: the tomb never summons again. */
        ALREADY_DEFEATED
    }

    public boolean isDefeated(long tomb) { return defeated.contains(tomb); }

    /** A VALID victory was recorded (P9). False for legacy-only completed tombs. */
    public boolean isVictory(long tomb) { return victories.contains(tomb); }

    /** Completed tombs that carry no valid-victory record: pre-P9 data of unknown validity (migration report only; never auto-converted). */
    public Set<Long> legacyOnlyDefeats() {
        Set<Long> out = new LinkedHashSet<>(defeated);
        out.removeAll(victories);
        return out;
    }

    /** Explicit, opt-in adoption of a legacy record as a victory (administrative/migration decision; nothing calls this automatically). */
    public boolean adoptLegacyAsVictory(long tomb) {
        if (!defeated.contains(tomb) || !victories.add(tomb)) return false;
        setDirty();
        return true;
    }

    public Active active(long tomb) { return active.get(tomb); }

    /** Decide a royal chest interaction. Clears a confirmed-lost binding as a side effect. */
    public StartDecision decideStart(long tomb, GuardianPresence presence) {
        if (defeated.contains(tomb)) return StartDecision.ALREADY_DEFEATED;
        if (!active.containsKey(tomb)) return StartDecision.START;
        return switch (presence) {
            case ALIVE -> StartDecision.ALREADY_ACTIVE;
            case NOT_LOADED -> StartDecision.GUARDIAN_NOT_LOADED;
            case GONE -> {
                active.remove(tomb);
                setDirty();
                yield StartDecision.RESTART_AFTER_LOSS;
            }
        };
    }

    /** Binds a freshly spawned guardian; refuses if the tomb is defeated or another guardian is bound. */
    public boolean bind(long tomb, UUID guardian, long chunk, long gameTime) {
        if (defeated.contains(tomb) || active.containsKey(tomb)) return false;
        active.put(tomb, new Active(tomb, guardian, chunk, gameTime));
        setDirty();
        return true;
    }

    /** Whether {@code guardian} is the one guardian currently bound to {@code tomb}. */
    public boolean isBound(long tomb, UUID guardian) {
        Active binding = active.get(tomb);
        return binding != null && binding.guardian().equals(guardian);
    }

    /** Tracks the chunk the bound guardian is in so a later presence check looks in the right place. */
    public void trackChunk(long tomb, UUID guardian, long chunk) {
        Active binding = active.get(tomb);
        if (binding == null || !binding.guardian().equals(guardian) || binding.guardianChunk() == chunk) return;
        active.put(tomb, new Active(tomb, guardian, chunk, binding.startedGameTime()));
        setDirty();
    }

    /**
     * Records a normal kill. Returns true exactly once per tomb, and only for the bound guardian, so the
     * completion (seal release and kill reward) cannot be granted twice.
     */
    public boolean markDefeated(long tomb, UUID guardian) {
        if (defeated.contains(tomb) || !isBound(tomb, guardian)) return false;
        active.remove(tomb);
        defeated.add(tomb);
        setDirty();
        return true;
    }

    /**
     * Records a VALID victory (P9). Returns true exactly once per tomb, only for the bound guardian, so the seal release, the guardian
     * reward and the entrance progress cannot be granted twice. An invalid death must NOT call this; it calls {@link #release}.
     */
    public boolean markVictory(long tomb, UUID guardian) {
        if (victories.contains(tomb) || !markDefeated(tomb, guardian)) return false;
        victories.add(tomb);
        setDirty();
        return true;
    }

    /** Ends an undefeated encounter whose guardian left without dying; the tomb can be retried. */
    public boolean release(long tomb, UUID guardian) {
        if (!isBound(tomb, guardian)) return false;
        active.remove(tomb);
        setDirty();
        return true;
    }

    public record Active(long tomb, UUID guardian, long guardianChunk, long startedGameTime) {}

    private record SavedState(List<Long> defeated, List<Active> active, List<Long> victories) {}
}
