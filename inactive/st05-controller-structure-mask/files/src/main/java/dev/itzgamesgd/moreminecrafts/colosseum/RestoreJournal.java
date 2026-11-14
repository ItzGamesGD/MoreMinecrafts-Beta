package dev.itzgamesgd.moreminecrafts.colosseum;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.UUIDUtil;
import org.jetbrains.annotations.Nullable;

/**
 * Q91 / Q117 / Q118-A restore journal: for every natural block the Immortal Gladiator destroys, the position and the
 * block state it had before the first destruction. Entries are work items (removed when restored), not records.
 * <p>
 * Block states are kept as their NBT form ({@code Name} + {@code Properties}) so this class stays plain data.
 * Q118-A: restoration only fills a position that is still empty; any other block there (a player's building, water
 * that flowed in) is never overwritten, and the conflict is recorded.
 */
public final class RestoreJournal {
    public enum Status {
        PENDING,
        /** Q118-A / O-13: a real collision is retained for history but is never evaluated again. */
        SKIPPED_CONFLICT,
        /** Legacy persisted value. It remains terminal when loading worlds written before O-13. */
        CONFLICT
    }

    /** Result of attempting to restore one entry in the world. */
    public enum RestoreResult { RESTORED, ALREADY_RESTORED, CONFLICT, NOT_LOADED }

    public enum AddResult { RECORDED, ALREADY_RECORDED }

    /** What a restore attempt saw: the result and, for a conflict, the foreign block found (diagnostics). */
    public record Attempt(RestoreResult result, @Nullable CompoundTag found) {
        public static Attempt of(RestoreResult result) { return new Attempt(result, null); }
    }

    public static final class Entry {
        private final UUID trial;
        private final long pos;
        private final CompoundTag prior;
        @Nullable private CompoundTag left;
        private Status status = Status.PENDING;
        private long conflictGameTime;
        @Nullable private CompoundTag found;

        Entry(UUID trial, long pos, CompoundTag prior, @Nullable CompoundTag left) {
            this.trial = trial;
            this.pos = pos;
            this.prior = prior;
            this.left = left;
        }

        public UUID trial() { return trial; }
        public long pos() { return pos; }
        /** The block state before the boss first destroyed it. */
        public CompoundTag prior() { return prior; }
        /** The state the boss left (null means plain air). */
        @Nullable public CompoundTag left() { return left; }
        public Status status() { return status; }
        public boolean terminalConflict() { return status == Status.SKIPPED_CONFLICT || status == Status.CONFLICT; }
        public long conflictGameTime() { return conflictGameTime; }
        /** The foreign block seen at the last conflict, for diagnostics. */
        @Nullable public CompoundTag found() { return found; }

        void markSkippedConflict(long gameTime, @Nullable CompoundTag foundState) {
            status = Status.SKIPPED_CONFLICT;
            conflictGameTime = gameTime;
            found = foundState;
        }
    }

    private final List<Entry> entries = new ArrayList<>();

    public boolean isEmpty() { return entries.isEmpty(); }

    public boolean hasPendingWork() {
        for (Entry entry : entries) if (entry.status == Status.PENDING) return true;
        return false;
    }

    public List<Entry> entries() { return Collections.unmodifiableList(entries); }

    @Nullable public Entry at(long pos) {
        for (Entry entry : entries) if (entry.pos == pos) return entry;
        return null;
    }

    /**
     * I13: at most one open entry per position, holding the state before the position was first destroyed. A repeat
     * call keeps the original {@code prior} and only refreshes {@code left}.
     */
    AddResult add(UUID trial, long pos, CompoundTag prior, @Nullable CompoundTag left) {
        Entry existing = at(pos);
        if (existing != null) {
            existing.left = left;
            return AddResult.ALREADY_RECORDED;
        }
        entries.add(new Entry(trial, pos, prior, left));
        return AddResult.RECORDED;
    }

    void remove(Entry entry) { entries.remove(entry); }

    int countFor(UUID trial) {
        int count = 0;
        for (Entry entry : entries) if (entry.trial.equals(trial)) count++;
        return count;
    }

    private record Data(UUID trial, long pos, CompoundTag prior, Optional<CompoundTag> left, Status status,
                        long conflictGameTime, Optional<CompoundTag> found) {}

    private static final Codec<Data> ENTRY_CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("trial").forGetter(Data::trial),
            Codec.LONG.fieldOf("pos").forGetter(Data::pos),
            CompoundTag.CODEC.fieldOf("prior").forGetter(Data::prior),
            CompoundTag.CODEC.optionalFieldOf("left").forGetter(Data::left),
            Codec.STRING.xmap(Status::valueOf, Status::name).optionalFieldOf("status", Status.PENDING).forGetter(Data::status),
            Codec.LONG.optionalFieldOf("conflict_game_time", 0L).forGetter(Data::conflictGameTime),
            CompoundTag.CODEC.optionalFieldOf("found").forGetter(Data::found)
    ).apply(i, Data::new));

    public static final Codec<RestoreJournal> CODEC = ENTRY_CODEC.listOf().xmap(RestoreJournal::fromData, RestoreJournal::toData);

    private List<Data> toData() {
        List<Data> data = new ArrayList<>();
        for (Entry e : entries)
            data.add(new Data(e.trial, e.pos, e.prior, Optional.ofNullable(e.left), e.status, e.conflictGameTime,
                    Optional.ofNullable(e.found)));
        return data;
    }

    private static RestoreJournal fromData(List<Data> data) {
        RestoreJournal journal = new RestoreJournal();
        for (Data d : data) {
            if (journal.at(d.pos()) != null) continue; // I13: first entry per position wins
            Entry entry = new Entry(d.trial(), d.pos(), d.prior(), d.left().orElse(null));
            entry.status = d.status();
            entry.conflictGameTime = d.conflictGameTime();
            entry.found = d.found().orElse(null);
            journal.entries.add(entry);
        }
        return journal;
    }
}
