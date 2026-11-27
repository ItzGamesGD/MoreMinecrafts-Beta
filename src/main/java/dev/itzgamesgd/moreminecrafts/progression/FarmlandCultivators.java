package dev.itzgamesgd.moreminecrafts.progression;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Research-effect attribution only: remembers the last player who tilled each farmland block. It is
 * stored in the owning chunk's persistent attachment and never grants access or ownership.
 */
public final class FarmlandCultivators {
    public static final AttachmentType<FarmlandCultivatorData> ATTACHMENT = AttachmentRegistry.create(
            ModIds.id("farmland_cultivators_v1"), builder -> builder.persistent(FarmlandCultivatorData.CODEC));

    private FarmlandCultivators() {}

    public static void initialize() { /* Force attachment registration during mod initialization. */ }

    /** Records a successful tilling; never loads a chunk and ignores non-farmland positions. */
    public static void record(ServerLevel level, BlockPos pos, UUID cultivator) {
        LevelChunk chunk = loadedChunk(level, pos);
        if (chunk == null || cultivator == null || !isFarmland(chunk, pos)) return;
        FarmlandCultivatorData data = data(chunk);
        write(chunk, data.with(pos.asLong(), cultivator));
    }

    /** Returns the recorded cultivator, dropping the record first if its farmland no longer exists. */
    public static Optional<UUID> cultivator(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = loadedChunk(level, pos);
        if (chunk == null) return Optional.empty();
        FarmlandCultivatorData data = data(chunk);
        UUID cultivator = data.cultivators().get(pos.asLong());
        if (cultivator == null) return Optional.empty();
        if (!isFarmland(chunk, pos)) {
            write(chunk, data.without(pos.asLong()));
            return Optional.empty();
        }
        return Optional.of(cultivator);
    }

    /** Called when a block is replaced; removes the record of farmland that no longer exists. */
    public static void clear(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        LevelChunk chunk = loadedChunk(serverLevel, pos);
        if (chunk == null) return;
        FarmlandCultivatorData data = data(chunk);
        if (data.cultivators().containsKey(pos.asLong())) write(chunk, data.without(pos.asLong()));
    }

    public static FarmlandCultivatorData data(LevelChunk chunk) {
        FarmlandCultivatorData attached = ((AttachmentTarget) chunk).getAttached(ATTACHMENT);
        return attached == null || attached.schemaVersion() != FarmlandCultivatorData.SCHEMA_VERSION
                ? FarmlandCultivatorData.EMPTY : attached;
    }

    private static void write(LevelChunk chunk, FarmlandCultivatorData updated) {
        AttachmentTarget target = (AttachmentTarget) chunk;
        if (updated.isEmpty()) target.removeAttached(ATTACHMENT);
        else target.setAttached(ATTACHMENT, updated);
        chunk.markUnsaved();
    }

    private static LevelChunk loadedChunk(ServerLevel level, BlockPos pos) {
        return level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
    }

    private static boolean isFarmland(LevelChunk chunk, BlockPos pos) {
        return chunk.getBlockState(pos).getBlock() instanceof FarmlandBlock;
    }
}
