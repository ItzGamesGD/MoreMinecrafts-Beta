package dev.itzgamesgd.moreminecrafts.k5probe;

import com.mojang.serialization.Codec;
import dev.itzgamesgd.moreminecrafts.progression.K5NaturalTreeProvenance;
import dev.itzgamesgd.moreminecrafts.progression.K5NaturalTreeProvenance.ChunkData;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.feature.Feature;

/** GameTest observations of production hooks. No parallel marker store or worldgen wrappers. */
public final class K5NaturalTreeProbe {
    public static final int SCHEMA_VERSION = K5NaturalTreeProvenance.SCHEMA_VERSION;
    public static final Codec<ChunkData> CODEC = K5NaturalTreeProvenance.CODEC;
    public static final AttachmentType<ChunkData> NATURAL_TREE_LOGS = K5NaturalTreeProvenance.NATURAL_TREE_LOGS;
    private static final AtomicLong DECORATION_CALLS = new AtomicLong();
    private static final AtomicLong TRUNK_CALLBACKS = new AtomicLong();
    private static final AtomicLong TRUNK_WRITES = new AtomicLong();
    private static final AtomicLong SAVED_CHUNKS = new AtomicLong();
    private static final ThreadLocal<Set<Identifier>> FEATURE_IDS = ThreadLocal.withInitial(HashSet::new);
    private static final ThreadLocal<Set<Long>> CALLBACK_POSITIONS = ThreadLocal.withInitial(HashSet::new);
    private static final ThreadLocal<List<String>> TREE_GROWER_ATTEMPTS =
            ThreadLocal.withInitial(ArrayList::new);
    private static final AtomicReference<ChunkUnloadObservation> LAST_UNLOAD = new AtomicReference<>();

    static {
        K5NaturalTreeProvenance.setDiagnosticListener(new K5NaturalTreeProvenance.DiagnosticListener() {
            @Override public void naturalDecorationEntered() {
                DECORATION_CALLS.incrementAndGet();
                FEATURE_IDS.get().clear();
                CALLBACK_POSITIONS.get().clear();
            }
            @Override public void trunkCallback(Identifier featureId, BlockPos pos) {
                TRUNK_CALLBACKS.incrementAndGet();
                if (featureId != null) FEATURE_IDS.get().add(featureId);
                CALLBACK_POSITIONS.get().add(pos.asLong());
            }
            @Override public void eligibleTrunkRecorded() { TRUNK_WRITES.incrementAndGet(); }
        });
    }

    private K5NaturalTreeProbe() {}
    public static boolean isKnownVanillaTreeFeatureId(Identifier id) {
        return K5NaturalTreeProvenance.isKnownVanillaTreeFeatureId(id);
    }
    public static boolean isEligibleTreeIdentity(Identifier placed, Identifier configured, boolean tree) {
        return K5NaturalTreeProvenance.isEligibleTreeIdentity(placed, configured, tree);
    }
    public static boolean isNaturalBiomeDecoration() {
        return K5NaturalTreeProvenance.isNaturalBiomeDecoration();
    }
    public static long biomeDecorationCalls() { return DECORATION_CALLS.get(); }
    public static long naturalTreeTrunkCallbacks() { return TRUNK_CALLBACKS.get(); }
    public static long naturalTrunkWrites() { return TRUNK_WRITES.get(); }
    public static Set<Identifier> lastNaturalTreeFeatureIds() { return Set.copyOf(FEATURE_IDS.get()); }
    public static Set<Long> lastNaturalTreeCallbackPositions() { return Set.copyOf(CALLBACK_POSITIONS.get()); }
    public static ChunkData data(ChunkAccess chunk) { return K5NaturalTreeProvenance.data(chunk); }

    public static void beginTreeGrowerTrace() { TREE_GROWER_ATTEMPTS.get().clear(); }
    public static void recordTreeGrowerFeatureAttempt(WorldGenLevel level, Feature feature, BlockPos pos,
            boolean succeeded) {
        Identifier key = level.registryAccess().lookupOrThrow(Registries.FEATURE).getKey(feature);
        TREE_GROWER_ATTEMPTS.get().add(key + "@" + pos + " succeeded=" + succeeded);
    }
    public static List<String> treeGrowerFeatureAttempts() { return List.copyOf(TREE_GROWER_ATTEMPTS.get()); }

    public static long savedChunksWithMarkers() { return SAVED_CHUNKS.get(); }
    public static void recordSuccessfulChunkSaveWithMarkers() { SAVED_CHUNKS.incrementAndGet(); }
    public static void clearLastMarkedChunkUnload() { LAST_UNLOAD.set(null); }
    public static void recordMarkedChunkUnload(ServerLevel level, LevelChunk chunk) {
        LAST_UNLOAD.set(new ChunkUnloadObservation(level.dimension().identifier(), chunk.getPos(),
                new WeakReference<>(chunk)));
    }
    public static boolean observedMarkedChunkUnload(ServerLevel level, ChunkPos pos, LevelChunk instance) {
        ChunkUnloadObservation event = LAST_UNLOAD.get();
        return event != null && event.dimension().equals(level.dimension().identifier())
                && event.chunkPos().equals(pos) && event.chunk().get() == instance;
    }
    private record ChunkUnloadObservation(Identifier dimension, ChunkPos chunkPos,
            WeakReference<LevelChunk> chunk) {}
}
