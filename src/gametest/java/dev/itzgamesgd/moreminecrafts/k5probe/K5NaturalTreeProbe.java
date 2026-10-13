package dev.itzgamesgd.moreminecrafts.k5probe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;

/** GameTest-only diagnostic storage. It never updates K5 or any player progression. */
public final class K5NaturalTreeProbe {
    public static final int SCHEMA_VERSION = 1;
    private static final Codec<List<Long>> POSITIONS_CODEC = Codec.LONG.listOf();
    public static final Codec<ChunkData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("schema_version").forGetter(ChunkData::schemaVersion),
            POSITIONS_CODEC.fieldOf("natural_tree_log_positions").forGetter(data -> List.copyOf(data.positions()))
    ).apply(instance, (version, positions) -> new ChunkData(version, Set.copyOf(positions))));
    public static final AttachmentType<ChunkData> NATURAL_TREE_LOGS = AttachmentRegistry.create(
            ModIds.id("k5_natural_tree_probe_v1"), builder -> builder
                    .initializer(() -> new ChunkData(SCHEMA_VERSION, Set.of()))
                    .persistent(CODEC));

    private static final ThreadLocal<Integer> BIOME_DECORATION_DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final AtomicLong BIOME_DECORATION_CALLS = new AtomicLong();
    private static final AtomicLong NATURAL_TRUNK_WRITES = new AtomicLong();

    private K5NaturalTreeProbe() {}

    public static void enterNaturalBiomeDecoration() {
        BIOME_DECORATION_CALLS.incrementAndGet();
        BIOME_DECORATION_DEPTH.set(BIOME_DECORATION_DEPTH.get() + 1);
    }

    public static long biomeDecorationCalls() {
        return BIOME_DECORATION_CALLS.get();
    }

    public static void exitNaturalBiomeDecoration() {
        int depth = BIOME_DECORATION_DEPTH.get() - 1;
        if (depth <= 0) BIOME_DECORATION_DEPTH.remove();
        else BIOME_DECORATION_DEPTH.set(depth);
    }

    public static boolean isNaturalBiomeDecoration() {
        return BIOME_DECORATION_DEPTH.get() > 0;
    }

    public static long naturalTrunkWrites() {
        return NATURAL_TRUNK_WRITES.get();
    }

    /** Called only by TreeFeature's actual trunk setter while natural biome decoration is active. */
    public static void recordTrunkWrite(WorldGenLevel level, BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        if (!isNaturalBiomeDecoration() || !state.is(BlockTags.LOGS)
                || !level.getBlockState(pos).equals(state)) return;
        NATURAL_TRUNK_WRITES.incrementAndGet();
        ChunkAccess chunk = level.getChunk(pos);
        if (!(chunk instanceof AttachmentTarget target)) return;
        ChunkData previous = target.getAttached(NATURAL_TREE_LOGS);
        if (previous == null || previous.schemaVersion() != SCHEMA_VERSION) {
            previous = new ChunkData(SCHEMA_VERSION, Set.of());
        }
        Set<Long> positions = new HashSet<>(previous.positions());
        if (positions.add(pos.asLong()))
            target.setAttached(NATURAL_TREE_LOGS, new ChunkData(SCHEMA_VERSION, Set.copyOf(positions)));
    }

    public static ChunkData data(ChunkAccess chunk) {
        if (!(chunk instanceof AttachmentTarget target)) return null;
        return target.getAttached(NATURAL_TREE_LOGS);
    }

    public record ChunkData(int schemaVersion, Set<Long> positions) {}
}
