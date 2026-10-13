package dev.itzgamesgd.moreminecrafts.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    public record ChunkData(int schemaVersion, Set<Long> positions) {
        public ChunkData { positions = Set.copyOf(positions); }
    }

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
    private static final ThreadLocal<ArrayDeque<Identifier>> ACTIVE_PLACED_FEATURE_IDS =
            ThreadLocal.withInitial(ArrayDeque::new);

    /** Optional observer for runtime diagnostics. It does not participate in eligibility. */
    public interface DiagnosticListener {
        void naturalDecorationEntered();
        void trunkCallback(Identifier featureId, BlockPos position);
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

    public static boolean isKnownVanillaTreeFeatureId(Identifier key) {
        return key != null && VANILLA_TREE_FEATURE_IDS.contains(key);
    }

    public static boolean isEligibleTreeIdentity(Identifier placedKey, Identifier featureKey, boolean treeFeature) {
        return isVanillaNamespace(placedKey) && isKnownVanillaTreeFeatureId(featureKey) && treeFeature;
    }

    private static boolean isVanillaLog(BlockState state) {
        return state.is(BlockTags.LOGS) && isVanillaNamespace(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
    }

    public static void recordTrunkWrite(Feature source, WorldGenLevel level, BlockPos pos, BlockState state) {
        RegistryAccess registries = ACTIVE_REGISTRIES.get();
        Identifier featureKey = registries == null ? null
                : registries.lookupOrThrow(Registries.FEATURE).getKey(source);
        Identifier placedKey = ACTIVE_PLACED_FEATURE_IDS.get().peek();
        DiagnosticListener observer = diagnosticListener;
        if (observer != null && isNaturalBiomeDecoration() && placedKey != null && source instanceof TreeFeature)
            observer.trunkCallback(featureKey, pos);
        if (!isNaturalBiomeDecoration() || !isEligibleTreeIdentity(placedKey, featureKey, source instanceof TreeFeature)
                || !isVanillaLog(state) || !level.getBlockState(pos).equals(state)) return;
        ChunkAccess chunk = level.getChunk(pos);
        if (!(chunk instanceof AttachmentTarget target)) return;
        ChunkData previous = data(chunk);
        if (previous == null) previous = new ChunkData(SCHEMA_VERSION, Set.of());
        Set<Long> positions = new HashSet<>(previous.positions());
        if (positions.add(pos.asLong())) {
            target.setAttached(NATURAL_TREE_LOGS, new ChunkData(SCHEMA_VERSION, positions));
            if (observer != null) observer.eligibleTrunkRecorded();
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
        ChunkData previous = data(chunk);
        if (previous == null) return false;
        Set<Long> positions = new HashSet<>(previous.positions());
        if (!positions.remove(pos.asLong())) return false;
        if (positions.isEmpty()) target.removeAttached(NATURAL_TREE_LOGS);
        else target.setAttached(NATURAL_TREE_LOGS, new ChunkData(SCHEMA_VERSION, positions));
        return true;
    }

    public static ChunkData data(ChunkAccess chunk) {
        if (!(chunk instanceof AttachmentTarget target)) return null;
        ChunkData attached = target.getAttached(NATURAL_TREE_LOGS);
        if (attached == null) return null;
        if (attached.schemaVersion() != SCHEMA_VERSION || attached.positions().stream().anyMatch(packed -> {
            BlockPos pos = BlockPos.of(packed);
            return !new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4).equals(chunk.getPos());
        })) {
            target.removeAttached(NATURAL_TREE_LOGS);
            return null;
        }
        return attached;
    }
}
