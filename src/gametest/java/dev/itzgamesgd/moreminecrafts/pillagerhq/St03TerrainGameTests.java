package dev.itzgamesgd.moreminecrafts.pillagerhq;

import dev.itzgamesgd.moreminecrafts.pillagerhq.worldgen.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public final class St03TerrainGameTests {
    @GameTest public void chunkClippedCutFillWaterAndOutsideSentinels(GameTestHelper helper) {
        verifyCorrectionFixture(helper.getLevel(),new BlockPos(400000,80,400000));helper.succeed();
    }
    /** The normal dedicated-server probe also runs this same GameTest fixture in its NoiseBased world. */
    public static java.util.Map<String,Object> verifyCorrectionFixture(net.minecraft.server.level.ServerLevel level,BlockPos origin) {
        int fy=origin.getY();
        // Finish neighboring feature generation before installing the disposable fixture/sentinels.
        for(int x=(origin.getX()-1)>>4;x<=(origin.getX()+112)>>4;x++)
            for(int z=(origin.getZ()-1)>>4;z<=(origin.getZ()+112)>>4;z++)level.getChunk(x,z);
        for(int x=-1;x<=112;x++)for(int z=-1;z<=112;z++) {
            for(int dy=-17;dy<=9;dy++) {
                var state=dy==-17||x<40?Blocks.STONE.defaultBlockState():Blocks.WATER.defaultBlockState();
                level.setBlock(origin.offset(x,dy,z),state,2);
            }
        }
        level.setBlock(origin.offset(111,-17,111),Blocks.DIAMOND_BLOCK.defaultBlockState(),2);
        level.setBlock(origin.offset(0,11,0),Blocks.EMERALD_BLOCK.defaultBlockState(),2);
        List<ChunkPos> chunks=new ArrayList<>();
        for(int x=origin.getX()>>4;x<=(origin.getX()+111)>>4;x++)for(int z=origin.getZ()>>4;z<=(origin.getZ()+111)>>4;z++)chunks.add(new ChunkPos(x,z));
        Collections.reverse(chunks);
        for(var cp:chunks) {
            BoundingBox box=new BoundingBox(cp.getMinBlockX(),level.getMinY(),cp.getMinBlockZ(),cp.getMaxBlockX(),level.getMaxY(),cp.getMaxBlockZ());
            St03TerrainCorrectionPiece.correct(level,box,origin,fy-16,fy+10,(x,z)->fy+10);
        }
        for(int x=0;x<112;x++)for(int z=0;z<112;z++) {
            require(level.getBlockState(origin.offset(x,0,z)).isSolidRender(),"Foundation missing");
            for(int dy=1;dy<=16;dy++)require(level.getBlockState(origin.offset(x,-dy,z)).isSolidRender(),"Water/air in allowed foundation fill");
            for(int dy=1;dy<=9;dy++)require(level.getBlockState(origin.offset(x,dy,z)).isAir(),"Natural hill or water left above foundation");
        }
        int checked=0;
        for(int i=-1;i<=112;i++)for(int dy=-16;dy<=9;dy++) {
            require(level.getBlockState(origin.offset(-1,dy,i)).is(Blocks.STONE),"West footprint boundary modified");
            require(level.getBlockState(origin.offset(112,dy,i)).is(Blocks.WATER),"East footprint boundary modified");
            var expected=i<40?Blocks.STONE:Blocks.WATER;
            require(level.getBlockState(origin.offset(i,dy,-1)).is(expected),"North footprint boundary modified");
            require(level.getBlockState(origin.offset(i,dy,112)).is(expected),"South footprint boundary modified");checked+=4;
        }
        require(level.getBlockState(origin.offset(111,-17,111)).is(Blocks.DIAMOND_BLOCK),"Fill exceeded 16 blocks");
        require(level.getBlockState(origin.offset(0,11,0)).is(Blocks.EMERALD_BLOCK),"Cut exceeded 10 blocks");
        return java.util.Map.of("status","PASS","generator",level.getChunkSource().getGenerator().getClass().getName(),
                "origin",List.of(origin.getX(),fy,origin.getZ()),"columns",12544,"outside_checks",checked,"cut_limit",10,"fill_limit",16);
    }
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    @GameTest public void approvedTemplateBoxesStayInsideFootprintInEveryRotation(GameTestHelper helper) {
        var templates=helper.getLevel().getStructureTemplateManager();
        ChunkPos start=new ChunkPos(-25003,25007);int farthest=0;
        for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++) {
        BlockPos origin=new BlockPos(start.getMinBlockX()-48+dx*16,80,start.getMinBlockZ()-48+dz*16);
        for(var rotation:net.minecraft.world.level.block.Rotation.values()) {
            var builder=new net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder();
            St03HeadquartersStructure.addPieces(builder,templates,origin,rotation);
            var pieces=builder.build().pieces();
            helper.assertTrue(pieces.size()==34,"34 unchanged modules required");
            for(var piece:pieces) {
                var box=piece.getBoundingBox();
                helper.assertTrue(box.minX()>=origin.getX()&&box.maxX()<=origin.getX()+111
                        &&box.minZ()>=origin.getZ()&&box.maxZ()<=origin.getZ()+111,
                        "Template extends outside approved footprint: "+rotation+" "+box);
                int reach=Math.max(Math.max(Math.abs((box.minX()>>4)-start.x()),Math.abs((box.maxX()>>4)-start.x())),
                        Math.max(Math.abs((box.minZ()>>4)-start.z()),Math.abs((box.maxZ()>>4)-start.z())));
                farthest=Math.max(farthest,reach);
                helper.assertTrue(reach<=5,"N=2 template exceeds the proven five-chunk reach: "+box);
            }
        }
        }
        helper.assertTrue(farthest==5,"All 25 offsets and four rotations must exercise the five-chunk boundary");
        helper.succeed();
    }
}
