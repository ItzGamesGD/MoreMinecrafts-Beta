package dev.itzgamesgd.moreminecrafts.pillagerhq.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * Places one of the 34 ST-03 modules and extends solid bottom footing blocks downward
 * to natural solid terrain (up to {@link St03PlacementConfig#MAX_FOOTING_FILL} blocks).
 */
public final class St03AssemblyPiece extends TemplateStructurePiece {
    private final String placementId;
    private final boolean includeAir;
    private final BlockPos compoundOrigin;
    private final Rotation compoundRotation;

    public St03AssemblyPiece(StructureTemplateManager templates, St03PlacementManifest.Placement placement,
                             BlockPos compoundOrigin, Rotation compoundRotation) {
        super(
                St03Worldgen.ASSEMBLY_PIECE,
                placement.module().order(),
                templates,
                placement.module().templateId(),
                placement.module().templateId().toString(),
                createSettings(placement.effectiveRotation(compoundRotation), placement.module().includeAir()),
                placement.worldPlacePos(compoundOrigin, compoundRotation)
        );
        this.placementId = placement.placementId();
        this.includeAir = placement.module().includeAir();
        this.compoundOrigin = compoundOrigin.immutable();
        this.compoundRotation = compoundRotation;
    }

    public St03AssemblyPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(
                St03Worldgen.ASSEMBLY_PIECE,
                tag,
                ctx.structureTemplateManager(),
                id -> createSettings(
                        Rotation.valueOf(tag.getStringOr("St03EffRot", "NONE")),
                        tag.getBooleanOr("St03Air", true)
                )
        );
        this.placementId = tag.getStringOr("St03Placement", "");
        this.includeAir = tag.getBooleanOr("St03Air", true);
        this.compoundOrigin = new BlockPos(
                tag.getIntOr("St03OX", 0),
                tag.getIntOr("St03OY", 0),
                tag.getIntOr("St03OZ", 0)
        );
        this.compoundRotation = Rotation.valueOf(tag.getStringOr("St03CompRot", "NONE"));
    }

    public static StructurePlaceSettings createSettings(Rotation effectiveRotation, boolean includeAir) {
        return new StructurePlaceSettings()
                .setRotation(effectiveRotation)
                .setKnownShape(true)
                .setLiquidSettings(LiquidSettings.IGNORE_WATERLOGGING)
                .addProcessor(includeAir ? BlockIgnoreProcessor.STRUCTURE_BLOCK : BlockIgnoreProcessor.STRUCTURE_AND_AIR);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super.addAdditionalSaveData(ctx, tag);
        tag.putString("St03Placement", placementId);
        tag.putBoolean("St03Air", includeAir);
        tag.putString("St03EffRot", placeSettings.getRotation().name());
        tag.putInt("St03OX", compoundOrigin.getX());
        tag.putInt("St03OY", compoundOrigin.getY());
        tag.putInt("St03OZ", compoundOrigin.getZ());
        tag.putString("St03CompRot", compoundRotation.name());
    }

    public String placementId() {
        return placementId;
    }

    public BlockPos compoundOrigin() {
        return compoundOrigin;
    }

    public Rotation compoundRotation() {
        return compoundRotation;
    }

    @Override
    public Rotation getRotation() {
        return placeSettings.getRotation();
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator,
                            RandomSource random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos reference) {
        super.postProcess(level, structures, generator, random, chunkBox, chunkPos, reference);
        fillVerticalFooting(level, chunkBox);
    }

    private void fillVerticalFooting(WorldGenLevel level, BoundingBox chunkBox) {
        BoundingBox pieceBox = this.boundingBox;
        int minX = Math.max(pieceBox.minX(), chunkBox.minX());
        int maxX = Math.min(pieceBox.maxX(), chunkBox.maxX());
        int minZ = Math.max(pieceBox.minZ(), chunkBox.minZ());
        int maxZ = Math.min(pieceBox.maxZ(), chunkBox.maxZ());
        int baseY = compoundOrigin.getY();

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                cursor.set(x, baseY, z);
                BlockState footing = level.getBlockState(cursor);
                if (footing.isAir() || !footing.isSolidRender()) {
                    continue;
                }
                for (int dy = 1; dy <= St03PlacementConfig.MAX_FOOTING_FILL; dy++) {
                    int y = baseY - dy;
                    if (y <= level.getMinY()) {
                        break;
                    }
                    cursor.set(x, y, z);
                    BlockState below = level.getBlockState(cursor);
                    if (!below.isAir() && below.isSolidRender()) {
                        break;
                    }
                    level.setBlock(cursor, footing, 2);
                }
            }
        }
    }

    @Override
    protected void handleDataMarker(String marker, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox box) {
        // ST-03 modules carry no structure data markers; loot/spawns are managed by St03HeadquartersCompletion.
    }
}
