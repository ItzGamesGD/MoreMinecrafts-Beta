package dev.itzgamesgd.moreminecrafts.colosseum;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jetbrains.annotations.Nullable;

/** Per-dimension, world-persistent registry of colosseum arenas and their controller state. */
public final class ColosseumArenaData extends SavedData {
    private static final Codec<ColosseumArenaData> CODEC = ColosseumArena.CODEC.listOf()
            .xmap(ColosseumArenaData::new, data -> new ArrayList<>(data.arenas.values()));

    public static final SavedDataType<ColosseumArenaData> TYPE = new SavedDataType<>(
            Identifier.parse("moreminecrafts:st05_colosseum_arenas"), ColosseumArenaData::new, CODEC, DataFixTypes.LEVEL);

    private final Map<Long, ColosseumArena> arenas = new LinkedHashMap<>();

    public ColosseumArenaData() {}

    private ColosseumArenaData(List<ColosseumArena> loaded) {
        for (ColosseumArena arena : loaded) arenas.put(arena.key(), arena);
    }

    @Nullable public ColosseumArena arena(long key) { return arenas.get(key); }

    public Collection<ColosseumArena> arenas() { return arenas.values(); }

    /** Idempotent: an arena already registered at this center keeps its geometry and state. */
    public ColosseumArena register(ColosseumArenaGeometry geometry) {
        ColosseumArena arena = arenas.computeIfAbsent(geometry.center().asLong(),
                key -> new ColosseumArena(geometry, new ColosseumTrialState()));
        setDirty();
        return arena;
    }

    @Nullable public ColosseumArena remove(long key) {
        ColosseumArena removed = arenas.remove(key);
        if (removed != null) setDirty();
        return removed;
    }
}
