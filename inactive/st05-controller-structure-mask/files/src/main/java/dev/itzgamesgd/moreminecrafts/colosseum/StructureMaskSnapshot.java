package dev.itzgamesgd.moreminecrafts.colosseum;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;

/** Persistent position provenance supplied by the structure placement and confirmed arena modifications. */
public final class StructureMaskSnapshot {
    public static final Codec<StructureMaskSnapshot> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.optionalFieldOf("complete", false).forGetter(StructureMaskSnapshot::complete),
            Codec.LONG.listOf().optionalFieldOf("protected_positions", List.of())
                    .forGetter(mask -> List.copyOf(mask.protectedPositions)),
            Codec.LONG.listOf().optionalFieldOf("player_positions", List.of())
                    .forGetter(mask -> List.copyOf(mask.playerPositions))
    ).apply(i, StructureMaskSnapshot::new));

    private boolean complete;
    /** Source-template occupancy (and legacy saves' positions); never released by player actions. */
    private final Set<Long> protectedPositions = new LinkedHashSet<>();
    /** Positions of blocks a player placed; released when that block is removed. */
    private final Set<Long> playerPositions = new LinkedHashSet<>();

    public StructureMaskSnapshot() {}

    private StructureMaskSnapshot(boolean complete, List<Long> positions, List<Long> playerPlaced) {
        this.complete = complete;
        protectedPositions.addAll(positions);
        playerPositions.addAll(playerPlaced);
    }

    public boolean complete() { return complete; }

    public boolean protects(BlockPos pos) {
        return protectedPositions.contains(pos.asLong()) || playerPositions.contains(pos.asLong());
    }

    public boolean playerPlaced(BlockPos pos) { return playerPositions.contains(pos.asLong()); }

    public Set<Long> protectedPositions() { return Set.copyOf(protectedPositions); }

    /** Stores the complete source template occupancy; repeated registration only adds positions. */
    void recordStructure(Collection<BlockPos> sourcePositions) {
        for (BlockPos pos : sourcePositions) protectedPositions.add(pos.asLong());
        complete = true;
    }

    /** Called after a player placement or other confirmed arena modification, never before placement succeeds. */
    boolean protect(BlockPos pos) { return playerPositions.add(pos.asLong()); }

    /** The player-placed block at {@code pos} was removed or replaced; source occupancy stays protected. */
    boolean release(BlockPos pos) { return playerPositions.remove(pos.asLong()); }
}
