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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;

/** Positive, persistent evidence for crops whose mature state turns into a placeable flower (Torchflower). */
public final class QuestNaturalCropProvenance {
    public static final int SCHEMA_VERSION = 1;

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

    public static final AttachmentType<ChunkData> ATTACHMENT = AttachmentRegistry.create(
            ModIds.id("quest_natural_crops_v1"), builder -> builder.persistent(CODEC));

    private QuestNaturalCropProvenance() {}

    public static void initialize() { java.util.Objects.requireNonNull(ATTACHMENT); }

    public static void mark(Level level, BlockPos pos) {
        if (!level.isClientSide()) update(level.getChunk(pos), pos, true);
    }

    public static void invalidate(Level level, BlockPos pos) {
        if (!level.isClientSide()) update(level.getChunk(pos), pos, false);
    }

    public static boolean isNatural(Level level, BlockPos pos) {
        ChunkData data = level.getChunk(pos).getAttached(ATTACHMENT);
        return data != null && data.schemaVersion() == SCHEMA_VERSION && data.natural().contains(pos.asLong());
    }

    private static void update(ChunkAccess chunk, BlockPos pos, boolean present) {
        synchronized (chunk) {
            ChunkData current = chunk.getAttached(ATTACHMENT);
            if (current != null && current.schemaVersion() != SCHEMA_VERSION) return;
            if (current == null && !present) return;
            if (current == null) current = new ChunkData(SCHEMA_VERSION, Set.of());
            ChunkData next = current.withNatural(pos.asLong(), present);
            if (next != current) chunk.setAttached(ATTACHMENT, next);
        }
    }
}
