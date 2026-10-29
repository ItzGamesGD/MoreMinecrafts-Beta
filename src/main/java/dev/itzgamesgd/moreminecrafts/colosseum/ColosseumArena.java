package dev.itzgamesgd.moreminecrafts.colosseum;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** One registered arena: its key (packed center position), geometry and persistent controller state. */
public final class ColosseumArena {
    public static final Codec<ColosseumArena> CODEC = RecordCodecBuilder.create(i -> i.group(
            ColosseumArenaGeometry.CODEC.fieldOf("geometry").forGetter(ColosseumArena::geometry),
            ColosseumTrialState.CODEC.fieldOf("state").forGetter(ColosseumArena::state)
    ).apply(i, ColosseumArena::new));

    private final ColosseumArenaGeometry geometry;
    private final ColosseumTrialState state;

    public ColosseumArena(ColosseumArenaGeometry geometry, ColosseumTrialState state) {
        this.geometry = geometry;
        this.state = state;
    }

    public long key() { return geometry.center().asLong(); }

    public ColosseumArenaGeometry geometry() { return geometry; }

    public ColosseumTrialState state() { return state; }
}
