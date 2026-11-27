package dev.itzgamesgd.moreminecrafts.pillagerhq.worldgen;

import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01FastTerrain;
import java.util.function.IntBinaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** Chunk-clipped cut/fill before the unchanged template assembly; never writes outside the 112x112 footprint. */
public final class St03TerrainCorrectionPiece extends StructurePiece {
    private final BlockPos origin;
    public St03TerrainCorrectionPiece(BlockPos origin,int minY,int maxY) {
        super(St03Worldgen.TERRAIN_PIECE,0,new BoundingBox(origin.getX(),minY,origin.getZ(),origin.getX()+111,maxY,origin.getZ()+111));
        this.origin=origin.immutable();
    }
    public St03TerrainCorrectionPiece(StructurePieceSerializationContext context,CompoundTag tag) {
        super(St03Worldgen.TERRAIN_PIECE,tag);
        origin=new BlockPos(tag.getIntOr("OX",0),tag.getIntOr("FY",0),tag.getIntOr("OZ",0));
    }
    @Override protected void addAdditionalSaveData(StructurePieceSerializationContext context,CompoundTag tag) {
        tag.putInt("OX",origin.getX());tag.putInt("FY",origin.getY());tag.putInt("OZ",origin.getZ());
    }
    public BlockPos origin() { return origin; }
    @Override public void postProcess(WorldGenLevel level,StructureManager structures,ChunkGenerator generator,
            RandomSource random,BoundingBox chunkBox,ChunkPos chunkPos,BlockPos pivot) {
        var state=level.getLevel().getChunkSource().randomState();
        IntBinaryOperator height=(x,z)->generator.getBaseHeight(x,z,Heightmap.Types.WORLD_SURFACE_WG,level,state);
        if(generator instanceof NoiseBasedChunkGenerator noise && St01FastTerrain.batchedApplies(generator)) {
            int[] heights=St01FastTerrain.fillChunk(noise,level,state,chunkPos.getMinBlockX(),chunkPos.getMinBlockZ());
            int sx=Math.max(origin.getX(),chunkPos.getMinBlockX()),sz=Math.max(origin.getZ(),chunkPos.getMinBlockZ());
            // Same existing exact vanilla batching as ST-01, with a local per-chunk reference check/fallback.
            if(heights[(sx&15)*16+(sz&15)]==height.applyAsInt(sx,sz))
                height=(x,z)->heights[(x&15)*16+(z&15)];
        }
        correct(level,chunkBox,origin,boundingBox.minY(),boundingBox.maxY(),height);
    }
    public record Changes(int cleared,int filled) {}
    /** Bounds are explicit so eligibility policy and permitted correction depth need not be conflated. */
    public static Changes correct(WorldGenLevel level,BoundingBox chunkBox,BlockPos origin,int minY,int maxY,IntBinaryOperator baseHeight) {
        int x0=Math.max(chunkBox.minX(),origin.getX()),x1=Math.min(chunkBox.maxX(),origin.getX()+111);
        int z0=Math.max(chunkBox.minZ(),origin.getZ()),z1=Math.min(chunkBox.maxZ(),origin.getZ()+111);
        int floor=Math.max(Math.max(minY,chunkBox.minY()),level.getMinY());
        int ceiling=Math.min(Math.min(maxY,chunkBox.maxY()),level.getMaxY());
        int fy=origin.getY(),cleared=0,filled=0;
        BlockPos.MutableBlockPos pos=new BlockPos.MutableBlockPos();
        for(int x=x0;x<=x1;x++)for(int z=z0;z<=z1;z++) {
            // Pure generator height avoids clearing unrelated structures higher in the same column.
            int top=Math.min(ceiling,baseHeight.applyAsInt(x,z)-1);
            for(int y=fy+1;y<=top;y++) {
                if(!level.getBlockState(pos.set(x,y,z)).isAir()) {level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);cleared++;}
            }
            if(fy>=floor && fy<=ceiling && !level.getBlockState(pos.set(x,fy,z)).isSolidRender()) {
                level.setBlock(pos,Blocks.COBBLESTONE.defaultBlockState(),2);filled++;
            }
            for(int y=fy-1;y>=floor;y--) {
                if(level.getBlockState(pos.set(x,y,z)).isSolidRender())break;
                // Use the approved ground palette's masonry: vanilla springs can replace dirt after structures.
                level.setBlock(pos,Blocks.COBBLESTONE.defaultBlockState(),2);filled++;
            }
        }
        return new Changes(cleared,filled);
    }
}
