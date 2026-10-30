package dev.itzgamesgd.moreminecrafts.colosseum;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import org.jetbrains.annotations.Nullable;

/**
 * Q117 work item: the Immortal Gladiator of a timed-out trial must vanish (removed without dying, so no death
 * event, loot, XP or key). It stays {@code PENDING} while the boss' chunk is unloaded and completes when the boss is
 * removed, rejected on load, or confirmed gone. It is a work item, not a record: it is dropped once complete.
 */
public final class BossVanish {
    public enum State {
        PENDING,
        /** Transient: the boss is confirmed gone; the item is pruned in the same pass and is never persisted. */
        REMOVED
    }

    private final UUID trial;
    @Nullable private final UUID entity;
    private long chunk;
    private State state = State.PENDING;
    private final long startedGameTime;

    BossVanish(UUID trial, @Nullable UUID entity, long chunk, long startedGameTime) {
        this.trial = trial;
        this.entity = entity;
        this.chunk = chunk;
        this.startedGameTime = startedGameTime;
    }

    public UUID trial() { return trial; }
    @Nullable public UUID entity() { return entity; }
    public long chunk() { return chunk; }
    public State state() { return state; }
    public long startedGameTime() { return startedGameTime; }

    void chunk(long chunk) { this.chunk = chunk; }
    void state(State state) { this.state = state; }

    static final Codec<BossVanish> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("trial").forGetter(BossVanish::trial),
            UUIDUtil.CODEC.optionalFieldOf("entity").forGetter(v -> Optional.ofNullable(v.entity)),
            Codec.LONG.optionalFieldOf("chunk", 0L).forGetter(BossVanish::chunk),
            Codec.LONG.optionalFieldOf("started_game_time", 0L).forGetter(BossVanish::startedGameTime)
    ).apply(i, (trial, entity, chunk, started) -> new BossVanish(trial, entity.orElse(null), chunk, started)));
}
