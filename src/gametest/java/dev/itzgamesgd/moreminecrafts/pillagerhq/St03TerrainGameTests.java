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
        var level=helper.getLevel();BlockPos origin=new BlockPos(400000,80,400000);
        for(int x=-1;x<=112;x++)for(int z=-1;z<=112;z++) {
            level.getChunk((origin.getX()+x)>>4,(origin.getZ()+z)>>4);
            for(int y=74;y<=85;y++) {
                var state=y<=75?Blocks.STONE.defaultBlockState():x<40?Blocks.STONE.defaultBlockState():Blocks.WATER.defaultBlockState();
                level.setBlock(origin.offset(x,y-80,z),state,2);
            }
        }
        List<ChunkPos> chunks=new ArrayList<>();
        for(int x=origin.getX()>>4;x<=(origin.getX()+111)>>4;x++)for(int z=origin.getZ()>>4;z<=(origin.getZ()+111)>>4;z++)chunks.add(new ChunkPos(x,z));
        Collections.reverse(chunks);
        for(var cp:chunks) {
            BoundingBox box=new BoundingBox(cp.getMinBlockX(),level.getMinY(),cp.getMinBlockZ(),cp.getMaxBlockX(),level.getMaxY(),cp.getMaxBlockZ());
            St03TerrainCorrectionPiece.correct(level,box,origin,64,90,(x,z)->86);
        }
        for(int x=0;x<112;x++)for(int z=0;z<112;z++) {
            helper.assertTrue(level.getBlockState(origin.offset(x,0,z)).isSolidRender(),"Foundation missing");
            helper.assertTrue(level.getBlockState(origin.offset(x,-1,z)).isSolidRender(),"Water/air below foundation");
            helper.assertTrue(level.getBlockState(origin.offset(x,1,z)).isAir(),"Natural hill or water left above foundation");
        }
        for(int i=-1;i<=112;i++) {
            helper.assertTrue(level.getBlockState(origin.offset(-1,1,i)).is(Blocks.STONE),"West footprint boundary modified");
            helper.assertTrue(level.getBlockState(origin.offset(112,1,i)).is(Blocks.WATER),"East footprint boundary modified");
            helper.assertTrue(!level.getBlockState(origin.offset(i,1,-1)).isAir(),"North footprint boundary modified");
            helper.assertTrue(!level.getBlockState(origin.offset(i,1,112)).isAir(),"South footprint boundary modified");
        }
        helper.succeed();
    }
    @GameTest public void approvedTemplateBoxesStayInsideFootprintInEveryRotation(GameTestHelper helper) {
        var templates=helper.getLevel().getStructureTemplateManager();
        BlockPos origin=new BlockPos(400320,80,400320);
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
            }
        }
        helper.succeed();
    }
}
