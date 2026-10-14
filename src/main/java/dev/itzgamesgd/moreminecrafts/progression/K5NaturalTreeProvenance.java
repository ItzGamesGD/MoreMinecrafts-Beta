package dev.itzgamesgd.moreminecrafts.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FallenTreeFeature;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/** Server-owned, position-based provenance for naturally generated vanilla tree trunks. */
public final class K5NaturalTreeProvenance {
    public static final int SCHEMA_VERSION = 1;
    private static final Set<Identifier> VANILLA_TREE_FEATURE_IDS = Set.of(
            Identifier.fromNamespaceAndPath("minecraft", "acacia"),
            Identifier.fromNamespaceAndPath("minecraft", "azalea_tree"),
            Identifier.fromNamespaceAndPath("minecraft", "birch"),
            Identifier.fromNamespaceAndPath("minecraft", "birch_bees_0002"),
            Identifier.fromNamespaceAndPath("minecraft", "birch_bees_0002_leaf_litter"),
            Identifier.fromNamespaceAndPath("minecraft", "birch_bees_002"),
            Identifier.fromNamespaceAndPath("minecraft", "birch_bees_005"),
            Identifier.fromNamespaceAndPath("minecraft", "birch_leaf_litter"),
            Identifier.fromNamespaceAndPath("minecraft", "cherry"),
            Identifier.fromNamespaceAndPath("minecraft", "cherry_bees_005"),
            Identifier.fromNamespaceAndPath("minecraft", "dark_oak"),
            Identifier.fromNamespaceAndPath("minecraft", "dark_oak_leaf_litter"),
            Identifier.fromNamespaceAndPath("minecraft", "fancy_oak"),
            Identifier.fromNamespaceAndPath("minecraft", "fancy_oak_bees"),
            Identifier.fromNamespaceAndPath("minecraft", "fancy_oak_bees_0002_leaf_litter"),
            Identifier.fromNamespaceAndPath("minecraft", "fancy_oak_bees_002"),
            Identifier.fromNamespaceAndPath("minecraft", "fancy_oak_bees_005"),
            Identifier.fromNamespaceAndPath("minecraft", "fancy_oak_leaf_litter"),
            Identifier.fromNamespaceAndPath("minecraft", "jungle_bush"),
            Identifier.fromNamespaceAndPath("minecraft", "jungle_tree"),
            Identifier.fromNamespaceAndPath("minecraft", "jungle_tree_no_vine"),
            Identifier.fromNamespaceAndPath("minecraft", "mangrove"),
            Identifier.fromNamespaceAndPath("minecraft", "mega_jungle_tree"),
            Identifier.fromNamespaceAndPath("minecraft", "mega_pine"),
            Identifier.fromNamespaceAndPath("minecraft", "mega_spruce"),
            Identifier.fromNamespaceAndPath("minecraft", "oak"),
            Identifier.fromNamespaceAndPath("minecraft", "oak_bees_0002_leaf_litter"),
            Identifier.fromNamespaceAndPath("minecraft", "oak_bees_002"),
            Identifier.fromNamespaceAndPath("minecraft", "oak_bees_005"),
            Identifier.fromNamespaceAndPath("minecraft", "oak_leaf_litter"),
            Identifier.fromNamespaceAndPath("minecraft", "orange_poplar"),
            Identifier.fromNamespaceAndPath("minecraft", "orange_poplar_leaf_litter"),
            Identifier.fromNamespaceAndPath("minecraft", "pale_oak"),
            Identifier.fromNamespaceAndPath("minecraft", "pale_oak_bonemeal"),
            Identifier.fromNamespaceAndPath("minecraft", "pale_oak_creaking"),
            Identifier.fromNamespaceAndPath("minecraft", "pine"),
            Identifier.fromNamespaceAndPath("minecraft", "red_poplar"),
            Identifier.fromNamespaceAndPath("minecraft", "red_poplar_leaf_litter"),
            Identifier.fromNamespaceAndPath("minecraft", "spruce"),
            Identifier.fromNamespaceAndPath("minecraft", "super_birch_bees"),
            Identifier.fromNamespaceAndPath("minecraft", "super_birch_bees_0002"),
            Identifier.fromNamespaceAndPath("minecraft", "swamp_oak"),
            Identifier.fromNamespaceAndPath("minecraft", "tall_mangrove"),
            Identifier.fromNamespaceAndPath("minecraft", "yellow_poplar"),
            Identifier.fromNamespaceAndPath("minecraft", "yellow_poplar_leaf_litter")
    );
    private static final Set<Identifier> VANILLA_FALLEN_TREE_FEATURE_IDS = Set.of(
            Identifier.fromNamespaceAndPath("minecraft", "fallen_birch_tree"),
            Identifier.fromNamespaceAndPath("minecraft", "fallen_jungle_tree"),
            Identifier.fromNamespaceAndPath("minecraft", "fallen_oak_tree"),
            Identifier.fromNamespaceAndPath("minecraft", "fallen_poplar_tree"),
            Identifier.fromNamespaceAndPath("minecraft", "fallen_spruce_tree"),
            Identifier.fromNamespaceAndPath("minecraft", "fallen_super_birch_tree")
    );
    public record ChunkData(int schemaVersion, Set<Long> positions) {
        public ChunkData { positions = Set.copyOf(positions); }
    }
    public record TrunkWrite(BlockPos position, BlockState state) {}
    private record PlacedFeatureFrame(Identifier key, boolean eligible) {}
    public record PlacedFeatureScope(boolean pushed, boolean eligible) {}

    public static final Codec<ChunkData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("schema_version").forGetter(ChunkData::schemaVersion),
            Codec.LONG.listOf().fieldOf("natural_tree_log_positions")
                    .forGetter(data -> List.copyOf(data.positions()))
    ).apply(instance, (version, positions) -> new ChunkData(version, Set.copyOf(positions))));
    public static final AttachmentType<ChunkData> NATURAL_TREE_LOGS = AttachmentRegistry.create(
            ModIds.id("k5_natural_tree_logs_v1"), builder -> builder
                    .initializer(() -> new ChunkData(SCHEMA_VERSION, Set.of()))
                    .persistent(CODEC));

    private static final ThreadLocal<Integer> BIOME_DECORATION_DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<RegistryAccess> ACTIVE_REGISTRIES = new ThreadLocal<>();
    private static final ThreadLocal<ArrayDeque<PlacedFeatureFrame>> ACTIVE_PLACED_FEATURE_IDS =
            ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<ArrayDeque<List<TrunkWrite>>> ACTIVE_FALLEN_TREE_WRITES =
            ThreadLocal.withInitial(ArrayDeque::new);
    /** Holds only weak chunk keys; values are the immutable attachment object already validated. */
    private static final Map<ChunkAccess, ChunkData> VALIDATED_CHUNK_DATA =
            Collections.synchronizedMap(new WeakHashMap<>());

    /** Optional observer for runtime diagnostics. It does not participate in eligibility. */
    public interface DiagnosticListener {
        void naturalDecorationEntered();
        void trunkCallback(Identifier featureId, BlockPos position);
        void treeFeaturePlacement(Identifier placedFeatureId, Identifier featureId, boolean succeeded,
                Set<Long> successfulLogPositions);
        void eligibleTrunkRecorded();
    }
    private static volatile DiagnosticListener diagnosticListener;
    public static void setDiagnosticListener(DiagnosticListener listener) { diagnosticListener = listener; }

    private K5NaturalTreeProvenance() {}
    public static void initialize() { /* Force attachment registration during mod initialization. */ }

    public static void enterNaturalBiomeDecoration(RegistryAccess registries) {
        if (BIOME_DECORATION_DEPTH.get() == 0) ACTIVE_REGISTRIES.set(registries);
        BIOME_DECORATION_DEPTH.set(BIOME_DECORATION_DEPTH.get() + 1);
        DiagnosticListener observer = diagnosticListener;
        if (observer != null) observer.naturalDecorationEntered();
    }

    public static void exitNaturalBiomeDecoration() {
        int depth = BIOME_DECORATION_DEPTH.get() - 1;
        if (depth <= 0) {
            BIOME_DECORATION_DEPTH.remove();
            ACTIVE_REGISTRIES.remove();
            ACTIVE_PLACED_FEATURE_IDS.remove();
        } else BIOME_DECORATION_DEPTH.set(depth);
    }

    public static boolean isNaturalBiomeDecoration() { return BIOME_DECORATION_DEPTH.get() > 0; }

    public static PlacedFeatureScope enterPlacedFeature(PlacedFeature placedFeature) {
        if (!isNaturalBiomeDecoration()) return new PlacedFeatureScope(false, false);
        RegistryAccess registries = ACTIVE_REGISTRIES.get();
        Identifier key = registries == null ? null
                : registries.lookupOrThrow(Registries.PLACED_FEATURE).getKey(placedFeature);
        boolean eligible = isVanillaNamespace(key);
        ACTIVE_PLACED_FEATURE_IDS.get().push(new PlacedFeatureFrame(key, eligible));
        return new PlacedFeatureScope(true, eligible);
    }

    public static void exitPlacedFeature(PlacedFeatureScope scope) {
        if (!scope.pushed()) return;
        ArrayDeque<PlacedFeatureFrame> active = ACTIVE_PLACED_FEATURE_IDS.get();
        if (!active.isEmpty()) active.pop();
        if (active.isEmpty()) ACTIVE_PLACED_FEATURE_IDS.remove();
    }

    private static boolean isVanillaNamespace(Identifier key) {
        return key != null && key.getNamespace().equals("minecraft");
    }

    public static boolean isKnownVanillaTreeFeatureId(Identifier key) {
        return key != null && VANILLA_TREE_FEATURE_IDS.contains(key);
    }

    public static boolean isKnownVanillaFallenTreeFeatureId(Identifier key) {
        return key != null && VANILLA_FALLEN_TREE_FEATURE_IDS.contains(key);
    }

    public static boolean isEligibleTreeIdentity(Identifier placedKey, Identifier featureKey, boolean treeFeature) {
        return isVanillaNamespace(placedKey) && treeFeature
                && (isKnownVanillaTreeFeatureId(featureKey) || isKnownVanillaFallenTreeFeatureId(featureKey));
    }

    public static void enterFallenTreeCapture(List<TrunkWrite> writes) {
        ACTIVE_FALLEN_TREE_WRITES.get().push(writes);
    }

    public static void exitFallenTreeCapture(List<TrunkWrite> writes) {
        ArrayDeque<List<TrunkWrite>> active = ACTIVE_FALLEN_TREE_WRITES.get();
        if (!active.isEmpty() && active.peek() == writes) active.pop();
        if (active.isEmpty()) ACTIVE_FALLEN_TREE_WRITES.remove();
    }

    /** Called only after FallenTreeFeature's real setBlockAndUpdate succeeds for a log state. */
    public static void captureFallenTreeWrite(BlockPos pos, BlockState state) {
        ArrayDeque<List<TrunkWrite>> active = ACTIVE_FALLEN_TREE_WRITES.get();
        if (!active.isEmpty() && state.is(BlockTags.LOGS))
            active.peek().add(new TrunkWrite(pos.immutable(), state));
    }

    private static boolean isTreePlacementFeature(Feature feature) {
        return feature instanceof TreeFeature || feature instanceof FallenTreeFeature;
    }

    private static boolean isVanillaLog(BlockState state) {
        return state.is(BlockTags.LOGS) && isVanillaNamespace(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
    }

    /** Persists successful TreeFeature trunk callbacks in one immutable attachment update per chunk. */
    public static void recordTrunkWrites(Feature source, WorldGenLevel level,
            List<TrunkWrite> writes, boolean treePlacementSucceeded) {
        RegistryAccess registries = ACTIVE_REGISTRIES.get();
        Identifier featureKey = registries == null ? null
                : registries.lookupOrThrow(Registries.FEATURE).getKey(source);
        PlacedFeatureFrame activeFeature = ACTIVE_PLACED_FEATURE_IDS.get().peek();
        Identifier placedKey = activeFeature == null || !activeFeature.eligible() ? null : activeFeature.key();
        DiagnosticListener observer = diagnosticListener;
        if (observer != null && isNaturalBiomeDecoration() && activeFeature != null && isTreePlacementFeature(source)) {
            for (TrunkWrite write : writes) observer.trunkCallback(featureKey, write.position());
            observer.treeFeaturePlacement(activeFeature.key(), featureKey, treePlacementSucceeded,
                    writes.stream().map(write -> write.position().asLong()).collect(java.util.stream.Collectors.toUnmodifiableSet()));
        }
        if (!treePlacementSucceeded || !isNaturalBiomeDecoration()
                || !isEligibleTreeIdentity(placedKey, featureKey, isTreePlacementFeature(source))) return;

        Map<ChunkAccess, Map<Long, BlockState>> writesByChunk = new HashMap<>();
        for (TrunkWrite write : writes) {
            ChunkAccess chunk = level.getChunk(write.position());
            writesByChunk.computeIfAbsent(chunk, ignored -> new HashMap<>())
                    .put(write.position().asLong(), write.state());
        }
        for (Map.Entry<ChunkAccess, Map<Long, BlockState>> chunkWrites : writesByChunk.entrySet()) {
            ChunkAccess chunk = chunkWrites.getKey();
            if (!(chunk instanceof AttachmentTarget target)) continue;
            synchronized (chunk) {
                ChunkData previous = data(chunk);
                HashSet<Long> positions = previous == null ? new HashSet<>() : new HashSet<>(previous.positions());
                int added = 0;
                for (Map.Entry<Long, BlockState> write : chunkWrites.getValue().entrySet()) {
                    BlockPos pos = BlockPos.of(write.getKey());
                    BlockState state = write.getValue();
                    if (isVanillaLog(state) && level.getBlockState(pos).equals(state) && positions.add(write.getKey())) {
                        added++;
                    }
                }
                if (added > 0) {
                    ChunkData updated = new ChunkData(SCHEMA_VERSION, positions);
                    target.setAttached(NATURAL_TREE_LOGS, updated);
                    VALIDATED_CHUNK_DATA.put(chunk, updated);
                    if (observer != null) {
                        for (int i = 0; i < added; i++) observer.eligibleTrunkRecorded();
                    }
                }
            }
        }
    }

    public static boolean hasMarker(Level level, BlockPos pos) {
        return isVanillaLog(level.getBlockState(pos)) && containsMarker(level, pos);
    }

    /** Raw stored marker check, used after a block is gone to prove it was consumed. */
    public static boolean containsMarker(Level level, BlockPos pos) {
        ChunkData data = data(level.getChunkAt(pos));
        return data != null && data.positions().contains(pos.asLong());
    }

    /** A successful block change consumes provenance, even if the new block is another log. */
    public static boolean invalidatePosition(Level level, BlockPos pos) {
        return invalidatePosition(level.getChunkAt(pos), pos);
    }

    /** Also used by WorldGenRegion, whose writes bypass Level.setBlock. */
    public static boolean invalidatePosition(ChunkAccess chunk, BlockPos pos) {
        if (!(chunk instanceof AttachmentTarget target)) return false;
        synchronized (chunk) {
            ChunkData previous = data(chunk);
            if (previous == null) return false;
            Set<Long> positions = new HashSet<>(previous.positions());
            if (!positions.remove(pos.asLong())) return false;
            if (positions.isEmpty()) {
                target.removeAttached(NATURAL_TREE_LOGS);
                VALIDATED_CHUNK_DATA.remove(chunk);
            } else {
                ChunkData updated = new ChunkData(SCHEMA_VERSION, positions);
                target.setAttached(NATURAL_TREE_LOGS, updated);
                VALIDATED_CHUNK_DATA.put(chunk, updated);
            }
            return true;
        }
    }

    public static ChunkData data(ChunkAccess chunk) {
        if (!(chunk instanceof AttachmentTarget target)) return null;
        synchronized (chunk) {
            ChunkData attached = target.getAttached(NATURAL_TREE_LOGS);
            if (attached == null) {
                VALIDATED_CHUNK_DATA.remove(chunk);
                return null;
            }
            if (VALIDATED_CHUNK_DATA.get(chunk) == attached) return attached;
            if (attached.schemaVersion() != SCHEMA_VERSION || attached.positions().stream().anyMatch(packed -> {
                BlockPos pos = BlockPos.of(packed);
                return !new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4).equals(chunk.getPos());
            })) {
                target.removeAttached(NATURAL_TREE_LOGS);
                VALIDATED_CHUNK_DATA.remove(chunk);
                return null;
            }
            VALIDATED_CHUNK_DATA.put(chunk, attached);
            return attached;
        }
    }
}
