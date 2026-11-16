package dev.itzgamesgd.moreminecrafts.pillagerhq.worldgen;

import com.mojang.serialization.MapCodec;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * ST-03 Pillager Headquarters natural structure.
 * Searches the owner-approved N=2 neighborhood using the measured V2 terrain rules
 * and assembles the 34 fixed placements in manifest order.
 */
public final class St03HeadquartersStructure extends Structure {
    public static final MapCodec<St03HeadquartersStructure> CODEC = simpleCodec(St03HeadquartersStructure::new);

    public static final Map<St03Eligibility.Verdict, AtomicLong> VERDICT_COUNTS = new EnumMap<>(St03Eligibility.Verdict.class);

    static {
        for (St03Eligibility.Verdict v : St03Eligibility.Verdict.values()) {
            VERDICT_COUNTS.put(v, new AtomicLong());
        }
    }

    public St03HeadquartersStructure(StructureSettings settings) {
        super(settings);
    }

    public static boolean isEligibleBiome(Holder<Biome> biome) {
        return biome.is(Biomes.PLAINS)
                || biome.is(Biomes.SUNFLOWER_PLAINS)
                || biome.is(Biomes.MEADOW)
                || biome.is(Biomes.FOREST)
                || biome.is(Biomes.FLOWER_FOREST)
                || biome.is(Biomes.BIRCH_FOREST)
                || biome.is(Biomes.OLD_GROWTH_BIRCH_FOREST)
                || biome.is(Biomes.DARK_FOREST);
    }

    public static boolean isEdgeBiome(Holder<Biome> biome) {
        return biome.is(Biomes.BEACH) || biome.is(Biomes.SNOWY_BEACH) || biome.is(Biomes.STONY_SHORE)
                || biome.is(Biomes.RIVER) || biome.is(Biomes.FROZEN_RIVER);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int originX = context.chunkPos().getMinBlockX() - St03PlacementConfig.BOX_SIZE / 2 + 8;
        int originZ = context.chunkPos().getMinBlockZ() - St03PlacementConfig.BOX_SIZE / 2 + 8;
        Rotation rotation = Rotation.getRandom(context.random());

        GeneratorTerrain terrain = new GeneratorTerrain(context);
        var selected = St03SiteSearch.select(terrain, originX, originZ);
        if (selected.isEmpty()) return Optional.empty();
        var site = selected.get();
        VERDICT_COUNTS.get(site.result().verdict()).incrementAndGet();
        BlockPos origin = new BlockPos(site.originX(), site.result().foundationY(), site.originZ());
        // The engine validates the stub's biome. Use the approved *surface center*, not a footprint corner
        // or a different vertical biome at the optimised foundation Y. Pieces retain their foundation Y.
        int cx = origin.getX() + 56, cz = origin.getZ() + 56;
        BlockPos biomeAnchor = new BlockPos(cx, terrain.surfaceHeight(cx, cz) - 1, cz);
        return Optional.of(new GenerationStub(biomeAnchor,
                builder -> addPieces(builder, context.structureTemplateManager(), origin, rotation)));
    }

    public static void addPieces(StructurePiecesBuilder builder, StructureTemplateManager templates,
                                 BlockPos compoundOrigin, Rotation compoundRotation) {
        for (St03PlacementManifest.Placement placement : St03PlacementManifest.placements()) {
            builder.addPiece(new St03AssemblyPiece(templates, placement, compoundOrigin, compoundRotation));
        }
    }

    @Override
    public StructureType<?> type() {
        return St03Worldgen.STRUCTURE_TYPE;
    }

    public static final class GeneratorTerrain implements St03Eligibility.Terrain {
        private final GenerationContext context;
        private final Map<Long, Integer> surfaceHeights = new HashMap<>();
        private final Map<Long, Integer> floorHeights = new HashMap<>();

        public GeneratorTerrain(GenerationContext context) {
            this.context = context;
        }

        private static long key(int x, int z) {
            return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
        }

        @Override
        public int surfaceHeight(int x, int z) {
            return surfaceHeights.computeIfAbsent(key(x, z), k -> context.chunkGenerator().getBaseHeight(
                    x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState()));
        }

        @Override
        public int oceanFloorHeight(int x, int z) {
            return floorHeights.computeIfAbsent(key(x, z), k -> context.chunkGenerator().getBaseHeight(
                    x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState()));
        }

        @Override
        public boolean isEdgeBiomeCell(int quartX, int quartY, int quartZ) {
            return isEdgeBiome(context.biomeResolver().getNoiseBiome(quartX, quartY, quartZ));
        }

        @Override
        public boolean isEligibleBiomeCell(int quartX, int quartY, int quartZ) {
            return isEligibleBiome(context.biomeResolver().getNoiseBiome(quartX, quartY, quartZ));
        }
    }
}
