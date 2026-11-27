package dev.itzgamesgd.moreminecrafts.worldgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.EmptyPoolElement;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;

/**
 * Terrain-adaptive variant of vanilla {@code minecraft:jigsaw} for the three mountain Sky Island structures (design B-01..B-05).
 *
 * <p>Everything except the start offset is delegated unchanged to {@link JigsawPlacement#addPieces}. The start offset is the
 * terrain-relative {@code start_height} of the vanilla structure; it is sampled so that the element the engine will really select
 * (same seed, same draw order) fits under the padded build ceiling while keeping {@code minimum_offset}.
 *
 * <p>Random-number contract (identical to vanilla for every candidate that needs no adaptation):
 * <ol>
 *   <li>draw 1 on the structure random = {@code nextInt(max-min+1)+min} (exactly what {@code UniformHeight} does);</li>
 *   <li>{@code JigsawPlacement.addPieces} then draws rotation and pool element as before;</li>
 *   <li>if draw 1 exceeds the feasible maximum it is <b>not clamped</b>: a replacement is drawn uniformly from
 *       {@code [min, feasibleMax]} from a separate look-ahead stream (a copy of the structure random advanced past the three
 *       vanilla draws). Rejection sampling keeps the distribution exactly uniform on the feasible range.</li>
 * </ol>
 * If {@code feasibleMax < minimum_offset} the candidate is rejected (no clip, no relocation, no re-selection, no retry).
 */
public final class AdaptiveJigsawStructure extends Structure {
    public static final MapCodec<AdaptiveJigsawStructure> CODEC = RecordCodecBuilder.<AdaptiveJigsawStructure>mapCodec(i -> i.group(
            settingsCodec(i),
            StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(s -> s.startPool),
            Codec.intRange(0, 20).fieldOf("size").forGetter(s -> s.maxDepth),
            Codec.INT.fieldOf("minimum_offset").forGetter(s -> s.minOffset),
            Codec.INT.fieldOf("maximum_offset").forGetter(s -> s.maxOffset),
            Types.CODEC.fieldOf("project_start_to_heightmap").forGetter(s -> s.heightmap),
            Codec.BOOL.fieldOf("use_expansion_hack").forGetter(s -> s.useExpansionHack),
            JigsawStructure.MaxDistance.CODEC.fieldOf("max_distance_from_center").forGetter(s -> s.maxDistance),
            DimensionPadding.CODEC.optionalFieldOf("dimension_padding", JigsawStructure.DEFAULT_DIMENSION_PADDING).forGetter(s -> s.padding),
            LiquidSettings.CODEC.optionalFieldOf("liquid_settings", JigsawStructure.DEFAULT_LIQUID_SETTINGS).forGetter(s -> s.liquid)
    ).apply(i, AdaptiveJigsawStructure::new)).validate(s -> s.minOffset > s.maxOffset
            ? com.mojang.serialization.DataResult.error(() -> "minimum_offset must be <= maximum_offset") : com.mojang.serialization.DataResult.success(s));

    public static final StructureType<AdaptiveJigsawStructure> TYPE = Registry.register(BuiltInRegistries.STRUCTURE_TYPE,
            Identifier.fromNamespaceAndPath(MoreMinecrafts.MOD_ID, "adaptive_jigsaw"), () -> CODEC);

    private final Holder<StructureTemplatePool> startPool;
    private final int maxDepth, minOffset, maxOffset;
    private final Types heightmap;
    private final boolean useExpansionHack;
    private final JigsawStructure.MaxDistance maxDistance;
    private final DimensionPadding padding;
    private final LiquidSettings liquid;

    public AdaptiveJigsawStructure(StructureSettings settings, Holder<StructureTemplatePool> startPool, int maxDepth, int minOffset, int maxOffset,
                                   Types heightmap, boolean useExpansionHack, JigsawStructure.MaxDistance maxDistance, DimensionPadding padding, LiquidSettings liquid) {
        super(settings);
        this.startPool = startPool; this.maxDepth = maxDepth; this.minOffset = minOffset; this.maxOffset = maxOffset; this.heightmap = heightmap;
        this.useExpansionHack = useExpansionHack; this.maxDistance = maxDistance; this.padding = padding; this.liquid = liquid;
    }

    /** Forces class initialisation (registers the structure type). */
    public static void initialize() {}

    /** Highest legal offset for an element of {@code sizeY} blocks on terrain whose top block is {@code topY}. */
    public static int feasibleMaxOffset(int levelMaxYInclusive, int paddingTop, int topY, int sizeY) {
        return AdaptivePlacementMath.feasibleMaxOffset(levelMaxYInclusive, paddingTop, topY, sizeY);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        ChunkPos cp = ctx.chunkPos();
        // draw 1: identical to vanilla UniformHeight.sample (Mth.randomBetweenInclusive)
        int roll = ctx.random().nextInt(maxOffset - minOffset + 1) + minOffset;
        // look-ahead on a copy of the stream: draw 1, rotation, element (the three draws vanilla makes), then spare draws for resampling
        WorldgenRandom look = new WorldgenRandom(new LegacyRandomSource(0L));
        look.setLargeFeatureSeed(ctx.seed(), cp.x(), cp.z());
        look.nextInt(maxOffset - minOffset + 1);
        Rotation rotation = Rotation.getRandom(look);
        StructurePoolElement element = startPool.value().getRandomTemplate(look);
        if (element == EmptyPoolElement.INSTANCE) return Optional.empty();
        BoundingBox box = element.getBoundingBox(ctx.structureTemplateManager(), new BlockPos(cp.getMinBlockX(), roll, cp.getMinBlockZ()), rotation);
        int cx = (box.maxX() + box.minX()) / 2, cz = (box.maxZ() + box.minZ()) / 2;
        // same early-out as JigsawPlacement.addPieces (no extra cost for biome-ineligible candidates)
        if (!ctx.couldStructureExistInColumn(cx, cz, ctx.heightAccessor().getMinY(), ctx.heightAccessor().getMaxY())) return Optional.empty();
        int top = ctx.chunkGenerator().getFirstOccupiedHeight(cx, cz, heightmap, ctx.heightAccessor(), ctx.randomState()); // top block T = firstFree - 1
        int feasibleMax = feasibleMaxOffset(ctx.heightAccessor().getMaxY(), padding.top(), top, box.getYSpan());
        int effectiveMax = AdaptivePlacementMath.effectiveMaxOffset(maxOffset, feasibleMax);
        if (!AdaptivePlacementMath.feasible(minOffset, maxOffset, feasibleMax)) return Optional.empty(); // B-04: reject
        if (roll > effectiveMax) roll = minOffset + look.nextInt(effectiveMax - minOffset + 1); // uniform on [min, effectiveMax]; never clamped
        ChunkPos c = cp;
        return JigsawPlacement.addPieces(ctx, startPool, Optional.empty(), maxDepth, new BlockPos(c.getMinBlockX(), roll, c.getMinBlockZ()), useExpansionHack,
                Optional.of(heightmap), maxDistance, PoolAliasLookup.EMPTY, padding, liquid);
    }

    @Override
    public StructureType<?> type() { return TYPE; }
}
