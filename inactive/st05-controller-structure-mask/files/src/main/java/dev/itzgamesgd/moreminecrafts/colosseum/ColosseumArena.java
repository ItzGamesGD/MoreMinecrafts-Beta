package dev.itzgamesgd.moreminecrafts.colosseum;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;

/**
 * One registered arena: its structure-instance id, geometry and persistent controller state. The record written to
 * disk has the sections {@code instance_id, geometry, trial, ledger, boss_vanish, restore_journal, structure_mask}
 * (schema v1; the mask is optional for older saves). The ledger, vanish tasks and journal are held by {@link #state()}
 * but stored beside the trial because they outlive it.
 */
public final class ColosseumArena {
    /** Placeholder for the dimension in ids migrated from v0; replaced when the owning level is known. */
    static final String UNKNOWN_DIMENSION = "?";

    public static final Codec<ColosseumArena> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("instance_id").forGetter(ColosseumArena::instanceId),
            ColosseumArenaGeometry.CODEC.fieldOf("geometry").forGetter(ColosseumArena::geometry),
            ColosseumTrialState.CODEC.fieldOf("trial").forGetter(ColosseumArena::state),
            RewardLedger.CODEC.optionalFieldOf("ledger", new RewardLedger()).forGetter(a -> a.state.ledger),
            BossVanish.CODEC.listOf().optionalFieldOf("boss_vanish", List.of()).forGetter(a -> List.copyOf(a.state.bossVanish)),
            RestoreJournal.CODEC.optionalFieldOf("restore_journal", new RestoreJournal()).forGetter(a -> a.state.journal),
            StructureMaskSnapshot.CODEC.optionalFieldOf("structure_mask")
                    .forGetter(a -> Optional.of(a.structureMask))
    ).apply(i, (instance, geometry, state, ledger, vanish, journal, structureMask) -> {
        state.ledger = ledger;
        state.bossVanish.addAll(vanish);
        state.journal = journal;
        return new ColosseumArena(instance, geometry, state, structureMask.orElseGet(StructureMaskSnapshot::new));
    }));

    private String instanceId;
    private final ColosseumArenaGeometry geometry;
    private final ColosseumTrialState state;
    private final StructureMaskSnapshot structureMask;

    public ColosseumArena(String instanceId, ColosseumArenaGeometry geometry, ColosseumTrialState state) {
        this(instanceId, geometry, state, new StructureMaskSnapshot());
    }

    public ColosseumArena(String instanceId, ColosseumArenaGeometry geometry, ColosseumTrialState state,
                          StructureMaskSnapshot structureMask) {
        this.instanceId = instanceId;
        this.geometry = geometry;
        this.state = state;
        this.structureMask = structureMask;
    }

    /** Id of a test arena built without a natural structure: {@code test|<dimension>|<packed center>}. */
    public static String testInstanceId(String dimension, BlockPos center) {
        return "test|" + dimension + "|" + center.asLong();
    }

    public String instanceId() { return instanceId; }

    void instanceId(String instanceId) { this.instanceId = instanceId; }

    /** The packed exact center: a per-dimension lookup key, not the identity (that is {@link #instanceId()}). */
    public long key() { return geometry.center().asLong(); }

    public ColosseumArenaGeometry geometry() { return geometry; }

    public ColosseumTrialState state() { return state; }

    public StructureMaskSnapshot structureMask() { return structureMask; }
}
