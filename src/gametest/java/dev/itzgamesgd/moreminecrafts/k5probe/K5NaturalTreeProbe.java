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
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FallenTreeFeature;
import net.minecraft.world.level.levelgen.feature.RandomSelectorFeature;

/** GameTest observations of production hooks. No parallel marker store or worldgen wrappers. */
public final class K5NaturalTreeProbe {
    public static final int SCHEMA_VERSION = K5NaturalTreeProvenance.SCHEMA_VERSION;
    public static final Codec<ChunkData> CODEC = K5NaturalTreeProvenance.CODEC;
    public static final AttachmentType<ChunkData> NATURAL_TREE_LOGS = K5NaturalTreeProvenance.NATURAL_TREE_LOGS;
    private static final AtomicLong DECORATION_CALLS = new AtomicLong();
    private static final ThreadLocal<Long> THREAD_DECORATION_CALLS = ThreadLocal.withInitial(() -> 0L);
    private static final AtomicLong TRUNK_CALLBACKS = new AtomicLong();
    private static final AtomicLong TRUNK_WRITES = new AtomicLong();
    private static final AtomicLong SAVED_CHUNKS = new AtomicLong();
    private static final ThreadLocal<Set<Identifier>> FEATURE_IDS = ThreadLocal.withInitial(HashSet::new);
    private static final ThreadLocal<Set<Long>> CALLBACK_POSITIONS = ThreadLocal.withInitial(HashSet::new);
    private static final ThreadLocal<List<FeaturePlacementObservation>> FEATURE_PLACEMENTS =
            ThreadLocal.withInitial(ArrayList::new);
    private static final ThreadLocal<List<String>> TREE_GROWER_ATTEMPTS =
            ThreadLocal.withInitial(ArrayList::new);
    /**
     * Every marked-chunk unload Minecraft performed, kept per chunk instance. A single last-writer-wins slot lost
     * the awaited unload whenever another marked chunk unloaded before the polling test looked.
     */
    private static final java.util.Deque<ChunkUnloadObservation> UNLOADS = new java.util.concurrent.ConcurrentLinkedDeque<>();
    private static final int UNLOAD_HISTORY = 512;
    private static final AtomicReference<Identifier> FORCED_FALLEN_BRANCH = new AtomicReference<>();
    private static final AtomicLong FORCED_FALLEN_BRANCH_COUNT = new AtomicLong();
    private static volatile Registry<Feature> FEATURE_REGISTRY;

    static {
        K5NaturalTreeProvenance.setDiagnosticListener(new K5NaturalTreeProvenance.DiagnosticListener() {
            @Override public void naturalDecorationEntered() {
                DECORATION_CALLS.incrementAndGet();
                THREAD_DECORATION_CALLS.set(THREAD_DECORATION_CALLS.get() + 1);
                FEATURE_IDS.get().clear();
                CALLBACK_POSITIONS.get().clear();
                FEATURE_PLACEMENTS.get().clear();
            }
            @Override public void trunkCallback(Identifier featureId, BlockPos pos) {
                TRUNK_CALLBACKS.incrementAndGet();
                if (featureId != null) FEATURE_IDS.get().add(featureId);
                CALLBACK_POSITIONS.get().add(pos.asLong());
            }
            @Override public void treeFeaturePlacement(Identifier placedFeatureId, Identifier featureId,
                    boolean succeeded, Set<Long> successfulLogPositions) {
                FEATURE_PLACEMENTS.get().add(new FeaturePlacementObservation(
                        placedFeatureId, featureId, succeeded, Set.copyOf(successfulLogPositions)));
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
    public static long currentThreadBiomeDecorationCalls() { return THREAD_DECORATION_CALLS.get(); }
    public static long naturalTreeTrunkCallbacks() { return TRUNK_CALLBACKS.get(); }
    public static long naturalTrunkWrites() { return TRUNK_WRITES.get(); }
    public static Set<Identifier> lastNaturalTreeFeatureIds() { return Set.copyOf(FEATURE_IDS.get()); }
    public static Set<Long> lastNaturalTreeCallbackPositions() { return Set.copyOf(CALLBACK_POSITIONS.get()); }
    public static List<FeaturePlacementObservation> lastNaturalTreeFeaturePlacements() {
        return List.copyOf(FEATURE_PLACEMENTS.get());
    }
    public static void armDeterministicFallenBranch(Identifier featureId, Registry<Feature> registry) {
        FORCED_FALLEN_BRANCH_COUNT.set(0L);
        FEATURE_REGISTRY = registry;
        FORCED_FALLEN_BRANCH.set(featureId);
    }
    public static void disarmDeterministicFallenBranch() {
        FORCED_FALLEN_BRANCH.set(null);
        FEATURE_REGISTRY = null;
    }
    public static long forcedFallenBranchCount() { return FORCED_FALLEN_BRANCH_COUNT.get(); }
    public static boolean consumeDeterministicFallenBranch(RandomSelectorFeature selector) {
        Identifier wanted = FORCED_FALLEN_BRANCH.get();
        if (wanted == null || selector.features().isEmpty()) return false;
        var first = selector.features().getFirst().feature().value();
        boolean firstIsRequestedFallenFeature = first.getFeatures().anyMatch(holder -> {
            Feature feature = holder.value();
            return feature instanceof FallenTreeFeature
                    && FEATURE_REGISTRY != null && wanted.equals(FEATURE_REGISTRY.getKey(feature));
        });
        if (!firstIsRequestedFallenFeature || !FORCED_FALLEN_BRANCH.compareAndSet(wanted, null)) return false;
        FORCED_FALLEN_BRANCH_COUNT.incrementAndGet();
        return true;
    }
    public record FeaturePlacementObservation(Identifier placedFeatureId, Identifier featureId,
            boolean succeeded, Set<Long> successfulLogPositions) {}
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
    /** Drops observations of this chunk position only; unrelated unloads recorded by other tests are kept. */
    public static void clearMarkedChunkUnloads(ServerLevel level, ChunkPos pos) {
        UNLOADS.removeIf(e -> e.dimension().equals(level.dimension().identifier()) && e.chunkPos().equals(pos));
    }
    public static void recordMarkedChunkUnload(ServerLevel level, LevelChunk chunk) {
        UNLOADS.addLast(new ChunkUnloadObservation(level.dimension().identifier(), chunk.getPos(),
                new WeakReference<>(chunk)));
        while (UNLOADS.size() > UNLOAD_HISTORY) UNLOADS.pollFirst();
    }
    public static boolean observedMarkedChunkUnload(ServerLevel level, ChunkPos pos, LevelChunk instance) {
        for (ChunkUnloadObservation event : UNLOADS)
            if (event.dimension().equals(level.dimension().identifier()) && event.chunkPos().equals(pos)
                    && event.chunk().get() == instance) return true;
        return false;
    }
    private record ChunkUnloadObservation(Identifier dimension, ChunkPos chunkPos,
            WeakReference<LevelChunk> chunk) {}
}
