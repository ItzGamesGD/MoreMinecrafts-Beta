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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Server-owned ore provenance for XP Quest mining (owner policy 1: only verified natural ore counts).
 *
 * <p>A chunk is <em>tracked</em> once it has passed natural biome decoration with this mod installed;
 * ore features run inside that step, so every ore present then is natural. After that, any tracked-ore
 * write through {@code Level.setBlock} outside natural decoration (player placement, piston arrival,
 * commands, structure templates) marks the position non-natural. A chunk without the attachment
 * (generated before the update) is <em>unknown</em> and never counts.</p>
 */
public final class QuestOreProvenance {
    public static final int SCHEMA_VERSION = 1;
    public static final TagKey<Block> TRACKED_ORES = TagKey.create(Registries.BLOCK, ModIds.id("quest_tracked_ores"));

    public enum Verdict { NATURAL, NON_NATURAL, UNKNOWN }

    public record ChunkData(int schemaVersion, boolean tracked, Set<Long> nonNatural) {
        public ChunkData { nonNatural = Set.copyOf(nonNatural); }
        static final ChunkData TRACKED = new ChunkData(SCHEMA_VERSION, true, Set.of());
        ChunkData withNonNatural(long pos, boolean present) {
            if (nonNatural.contains(pos) == present) return this;
            Set<Long> next = new HashSet<>(nonNatural);
            if (present) next.add(pos); else next.remove(pos);
            return new ChunkData(schemaVersion, tracked, next);
        }
    }

    public static final Codec<ChunkData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("schema").forGetter(ChunkData::schemaVersion),
            Codec.BOOL.fieldOf("tracked").forGetter(ChunkData::tracked),
            Codec.LONG.listOf().xmap(Set::copyOf, List::copyOf).fieldOf("non_natural").forGetter(ChunkData::nonNatural)
    ).apply(instance, ChunkData::new));

    /** Holder so the pure verdict logic loads without the Fabric attachment registry (unit tests). */
    public static final class Storage {
        public static final AttachmentType<ChunkData> ATTACHMENT = AttachmentRegistry.create(
                ModIds.id("quest_ore_provenance_v1"), builder -> builder.persistent(CODEC));
        private Storage() {}
    }

    private QuestOreProvenance() {}

    /** Forces attachment registration during mod initialization. */
    public static void initialize() { java.util.Objects.requireNonNull(Storage.ATTACHMENT); }

    public static boolean isTrackedOre(BlockState state) { return state.is(TRACKED_ORES); }

    /** Called after natural biome decoration of a chunk (ore features run inside it). */
    public static void markTrackedAfterNaturalDecoration(ChunkAccess chunk) {
        ChunkData current = chunk.getAttached(Storage.ATTACHMENT);
        if (current == null) chunk.setAttached(Storage.ATTACHMENT, ChunkData.TRACKED);
        else if (!current.tracked()) chunk.setAttached(Storage.ATTACHMENT, new ChunkData(SCHEMA_VERSION, true, current.nonNatural()));
    }

    /** A tracked ore was written outside natural decoration: it is not natural from now on. */
    public static void markNonNatural(Level level, BlockPos pos) {
        if (level.isClientSide()) return;
        ChunkAccess chunk = level.getChunk(pos);
        ChunkData current = chunk.getAttached(Storage.ATTACHMENT);
        if (current == null || !current.tracked()) return; // unknown chunks stay unknown
        chunk.setAttached(Storage.ATTACHMENT, current.withNonNatural(pos.asLong(), true));
    }

    public static Verdict verdict(Level level, BlockPos pos) {
        ChunkData data = level.getChunk(pos).getAttached(Storage.ATTACHMENT);
        return verdict(data, pos.asLong());
    }

    static Verdict verdict(ChunkData data, long pos) {
        if (data == null || !data.tracked()) return Verdict.UNKNOWN;
        return data.nonNatural().contains(pos) ? Verdict.NON_NATURAL : Verdict.NATURAL;
    }

    /** The block is gone; drop its mark so the attachment does not grow without bound. */
    public static void clear(Level level, BlockPos pos) {
        ChunkAccess chunk = level.getChunk(pos);
        ChunkData current = chunk.getAttached(Storage.ATTACHMENT);
        if (current != null) chunk.setAttached(Storage.ATTACHMENT, current.withNonNatural(pos.asLong(), false));
    }
}
