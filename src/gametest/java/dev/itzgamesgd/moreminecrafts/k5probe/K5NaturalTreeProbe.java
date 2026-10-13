package dev.itzgamesgd.moreminecrafts.k5probe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicReference;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.chunk.ChunkAccess;

/** GameTest-only diagnostic storage. It never updates K5 or any player progression. */
public final class K5NaturalTreeProbe {
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
    private static final ThreadLocal<RegistryAccess> ACTIVE_REGISTRIES = new ThreadLocal<>();
    private static final ThreadLocal<ArrayDeque<Identifier>> ACTIVE_PLACED_FEATURE_IDS =
            ThreadLocal.withInitial(ArrayDeque::new);
    private static final AtomicLong BIOME_DECORATION_CALLS = new AtomicLong();
    private static final AtomicLong NATURAL_TRUNK_WRITES = new AtomicLong();
    private static final AtomicLong NATURAL_TREE_TRUNK_CALLBACKS = new AtomicLong();
    private static final ThreadLocal<Set<Identifier>> LAST_NATURAL_TREE_FEATURE_IDS =
            ThreadLocal.withInitial(HashSet::new);
    private static final ThreadLocal<Set<Long>> LAST_NATURAL_TREE_CALLBACK_POSITIONS =
            ThreadLocal.withInitial(HashSet::new);
    private static final AtomicLong SAVED_CHUNKS_WITH_MARKERS = new AtomicLong();
    private static final AtomicReference<ChunkUnloadObservation> LAST_MARKED_CHUNK_UNLOAD = new AtomicReference<>();
    private static final ThreadLocal<List<String>> TREE_GROWER_FEATURE_ATTEMPTS =
            ThreadLocal.withInitial(ArrayList::new);

    public static void beginTreeGrowerTrace() {
        TREE_GROWER_FEATURE_ATTEMPTS.get().clear();
    }

    /** Test-only trace of the actual registered Feature.place calls made by TreeGrower.growTree. */
    public static void recordTreeGrowerFeatureAttempt(WorldGenLevel level, Feature feature, BlockPos pos,
            boolean succeeded) {
        Identifier featureKey = level.registryAccess().lookupOrThrow(Registries.FEATURE).getKey(feature);
        TREE_GROWER_FEATURE_ATTEMPTS.get().add(featureKey + "@" + pos + " succeeded=" + succeeded);
    }

    public static List<String> treeGrowerFeatureAttempts() {
        return List.copyOf(TREE_GROWER_FEATURE_ATTEMPTS.get());
    }

    private K5NaturalTreeProbe() {}

    public static void enterNaturalBiomeDecoration(RegistryAccess registries) {
        BIOME_DECORATION_CALLS.incrementAndGet();
        if (BIOME_DECORATION_DEPTH.get() == 0) {
            ACTIVE_REGISTRIES.set(registries);
            LAST_NATURAL_TREE_FEATURE_IDS.get().clear();
            LAST_NATURAL_TREE_CALLBACK_POSITIONS.get().clear();
        }
        BIOME_DECORATION_DEPTH.set(BIOME_DECORATION_DEPTH.get() + 1);
    }

    /** Opens attribution only for registered vanilla placed-feature identities. */
    public static boolean enterPlacedFeature(PlacedFeature placedFeature) {
        if (!isNaturalBiomeDecoration()) return false;
        RegistryAccess registries = ACTIVE_REGISTRIES.get();
        if (registries == null) return false;
        Identifier key = registries.lookupOrThrow(Registries.PLACED_FEATURE).getKey(placedFeature);
        boolean eligible = isVanillaNamespace(key);
        if (eligible) ACTIVE_PLACED_FEATURE_IDS.get().push(key);
        return eligible;
    }

    public static void exitPlacedFeature(boolean entered) {
        if (!entered) return;
        ArrayDeque<Identifier> active = ACTIVE_PLACED_FEATURE_IDS.get();
        if (!active.isEmpty()) active.pop();
        if (active.isEmpty()) ACTIVE_PLACED_FEATURE_IDS.remove();
    }

    private static boolean isVanillaNamespace(Identifier key) {
        return key != null && key.getNamespace().equals("minecraft");
    }

    public static boolean isKnownVanillaTreeFeatureId(Identifier treeFeatureKey) {
        return treeFeatureKey != null && VANILLA_TREE_FEATURE_IDS.contains(treeFeatureKey);
    }

    public static boolean isEligibleTreeIdentity(Identifier placedFeatureKey, Identifier treeFeatureKey,
            boolean isTreeFeature) {
        return isVanillaNamespace(placedFeatureKey)
                && isKnownVanillaTreeFeatureId(treeFeatureKey)
                && isTreeFeature;
    }

    public static boolean hasEligiblePlacedFeatureScope() {
        return !ACTIVE_PLACED_FEATURE_IDS.get().isEmpty();
    }

    public static long biomeDecorationCalls() {
        return BIOME_DECORATION_CALLS.get();
    }

    public static void exitNaturalBiomeDecoration() {
        int depth = BIOME_DECORATION_DEPTH.get() - 1;
        if (depth <= 0) {
            BIOME_DECORATION_DEPTH.remove();
            ACTIVE_REGISTRIES.remove();
            ACTIVE_PLACED_FEATURE_IDS.remove();
        } else {
            BIOME_DECORATION_DEPTH.set(depth);
        }
    }

    public static boolean isNaturalBiomeDecoration() {
        return BIOME_DECORATION_DEPTH.get() > 0;
    }

    public static long naturalTrunkWrites() {
        return NATURAL_TRUNK_WRITES.get();
    }

    /** Counts real TreeFeature trunk-setter invocations before the still-unverified feature-identity filter. */
    public static long naturalTreeTrunkCallbacks() {
        return NATURAL_TREE_TRUNK_CALLBACKS.get();
    }

    public static Set<Identifier> lastNaturalTreeFeatureIds() {
        return Set.copyOf(LAST_NATURAL_TREE_FEATURE_IDS.get());
    }

    public static Set<Long> lastNaturalTreeCallbackPositions() {
        return Set.copyOf(LAST_NATURAL_TREE_CALLBACK_POSITIONS.get());
    }

    public static long savedChunksWithMarkers() {
        return SAVED_CHUNKS_WITH_MARKERS.get();
    }

    public static void recordSuccessfulChunkSaveWithMarkers() {
        SAVED_CHUNKS_WITH_MARKERS.incrementAndGet();
    }

    public static void clearLastMarkedChunkUnload() {
        LAST_MARKED_CHUNK_UNLOAD.set(null);
    }

    public static void recordMarkedChunkUnload(ServerLevel level, LevelChunk chunk) {
        LAST_MARKED_CHUNK_UNLOAD.set(new ChunkUnloadObservation(
                level.dimension().identifier(), chunk.getPos(), new WeakReference<>(chunk)));
    }

    public static boolean observedMarkedChunkUnload(ServerLevel level, ChunkPos pos, LevelChunk expectedInstance) {
        ChunkUnloadObservation event = LAST_MARKED_CHUNK_UNLOAD.get();
        return event != null
                && event.dimension().equals(level.dimension().identifier())
                && event.chunkPos().equals(pos)
                && event.chunk().get() == expectedInstance;
    }

    /** Called only by TreeFeature's actual trunk setter while natural biome decoration is active. */
    public static void recordTrunkWrite(Feature source, WorldGenLevel level, BlockPos pos,
            net.minecraft.world.level.block.state.BlockState state) {
        RegistryAccess registries = ACTIVE_REGISTRIES.get();
        Identifier treeFeatureKey = registries == null ? null
                : registries.lookupOrThrow(Registries.FEATURE).getKey(source);
        Identifier placedFeatureKey = ACTIVE_PLACED_FEATURE_IDS.get().peek();
        if (isNaturalBiomeDecoration() && hasEligiblePlacedFeatureScope() && source instanceof TreeFeature) {
            NATURAL_TREE_TRUNK_CALLBACKS.incrementAndGet();
            if (treeFeatureKey != null) LAST_NATURAL_TREE_FEATURE_IDS.get().add(treeFeatureKey);
            LAST_NATURAL_TREE_CALLBACK_POSITIONS.get().add(pos.asLong());
        }
        if (!isNaturalBiomeDecoration() || !hasEligiblePlacedFeatureScope()
                || !isEligibleTreeIdentity(placedFeatureKey, treeFeatureKey, source instanceof TreeFeature)
                || !state.is(BlockTags.LOGS) || !level.getBlockState(pos).equals(state)) return;
        NATURAL_TRUNK_WRITES.incrementAndGet();
        ChunkAccess chunk = level.getChunk(pos);
        if (!(chunk instanceof AttachmentTarget target)) return;
        ChunkData previous = data(chunk);
        if (previous == null) previous = new ChunkData(SCHEMA_VERSION, Set.of());
        Set<Long> positions = new HashSet<>(previous.positions());
        if (positions.add(pos.asLong()))
            target.setAttached(NATURAL_TREE_LOGS, new ChunkData(SCHEMA_VERSION, Set.copyOf(positions)));
    }

    /** Removes stale provenance when the referenced block is replaced or removed. */
    public static void invalidatePosition(Level level, BlockPos pos) {
        ChunkAccess chunk = level.getChunkAt(pos);
        if (!(chunk instanceof AttachmentTarget target)) return;
        ChunkData previous = data(chunk);
        if (previous == null) return;
        Set<Long> positions = new HashSet<>(previous.positions());
        if (!positions.remove(pos.asLong())) return;
        if (positions.isEmpty()) target.removeAttached(NATURAL_TREE_LOGS);
        else target.setAttached(NATURAL_TREE_LOGS, new ChunkData(SCHEMA_VERSION, Set.copyOf(positions)));
    }

    public static ChunkData data(ChunkAccess chunk) {
        if (!(chunk instanceof AttachmentTarget target)) return null;
        ChunkData attached = target.getAttached(NATURAL_TREE_LOGS);
        if (attached == null) return null;
        if (attached.schemaVersion() != SCHEMA_VERSION
                || attached.positions().stream().anyMatch(packed -> {
                    BlockPos pos = BlockPos.of(packed);
                    return !new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4).equals(chunk.getPos());
                })) {
            target.removeAttached(NATURAL_TREE_LOGS);
            return null;
        }
        return attached;
    }

    public record ChunkData(int schemaVersion, Set<Long> positions) {
        public ChunkData {
            positions = Set.copyOf(positions);
        }
    }

    private record ChunkUnloadObservation(Identifier dimension, ChunkPos chunkPos,
            WeakReference<LevelChunk> chunk) {}
}
