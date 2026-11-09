package dev.itzgamesgd.moreminecrafts.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

/** Positive, persistent evidence of tracked ore written by a natural ore feature. */
public final class QuestOreProvenance {
    public static final int SCHEMA_VERSION = 2;
    public static final TagKey<Block> TRACKED_ORES = TagKey.create(Registries.BLOCK, ModIds.id("quest_tracked_ores"));

    public enum Verdict { NATURAL, NON_NATURAL, UNKNOWN }

    public record ChunkData(int schemaVersion, Set<Long> natural) {
        public ChunkData { natural = Set.copyOf(natural); }
        ChunkData withNatural(long pos, boolean present) {
            if (natural.contains(pos) == present) return this;
            Set<Long> next = new HashSet<>(natural);
            if (present) next.add(pos); else next.remove(pos);
            return new ChunkData(schemaVersion, next);
        }
    }

    public static final Codec<ChunkData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("schema").forGetter(ChunkData::schemaVersion),
            Codec.LONG.listOf().xmap(Set::copyOf, List::copyOf).fieldOf("natural").forGetter(ChunkData::natural)
    ).apply(instance, ChunkData::new));

    /** The v1 negative-evidence attachment is deliberately ignored on all existing saves. */
    public static final class Storage {
        public static final AttachmentType<ChunkData> ATTACHMENT = AttachmentRegistry.create(
                ModIds.id("quest_ore_provenance_v2"), builder -> builder.persistent(CODEC));
        private Storage() {}
    }

    private QuestOreProvenance() {}

    public static void initialize() { java.util.Objects.requireNonNull(Storage.ATTACHMENT); }

    public static boolean isTrackedOre(BlockState state) { return state.is(TRACKED_ORES); }

    /** Called only after a tracked ore feature's actual write succeeded during biome decoration. */
    public static void recordGenerated(WorldGenLevel level, BlockPos pos, BlockState state) {
        if (!K5NaturalTreeProvenance.isNaturalBiomeDecoration()) return;
        update(level.getChunk(pos), pos, isTrackedOre(state));
    }

    /** Any different block at a recorded position consumes its origin, including piston departure. */
    public static void invalidate(Level level, BlockPos pos) { update(level.getChunk(pos), pos, false); }

    /** WorldGenRegion writes do not pass through Level.setBlock. */
    public static void invalidate(ChunkAccess chunk, BlockPos pos) { update(chunk, pos, false); }

    private static void update(ChunkAccess chunk, BlockPos pos, boolean natural) {
        synchronized (chunk) {
            ChunkData current = chunk.getAttached(Storage.ATTACHMENT);
            if (current != null && current.schemaVersion() != SCHEMA_VERSION) return;
            if (current == null && !natural) return;
            if (current == null) current = new ChunkData(SCHEMA_VERSION, Set.of());
            ChunkData next = current.withNatural(pos.asLong(), natural);
            if (next != current) chunk.setAttached(Storage.ATTACHMENT, next);
        }
    }

    public static Verdict verdict(Level level, BlockPos pos) {
        return verdict(level.getChunk(pos).getAttached(Storage.ATTACHMENT), pos.asLong());
    }

    static Verdict verdict(ChunkData data, long pos) {
        if (data == null || data.schemaVersion() != SCHEMA_VERSION) return Verdict.UNKNOWN;
        return data.natural().contains(pos) ? Verdict.NATURAL : Verdict.NON_NATURAL;
    }
}
