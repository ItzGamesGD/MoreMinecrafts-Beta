package dev.itzgamesgd.moreminecrafts.skytower;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.TrialSpawnerBlock;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;

/** Real mod-server blocks/hooks. Synthetic cooldown is explicitly not a player combat test. */
public final class SkyTowerGameTests {
    private static final AtomicInteger NEXT=new AtomicInteger();
    private record Fixture(ServerLevel level,SkyTowerData data,SkyTowerLayout.Site site){
        BlockPos at(int floor,int cell){return SkyTowerBarriers.pos(site.barrier(floor,cell));}
        void release(){for(int x=(site.x()-50)>>4;x<=(site.x()+50)>>4;x++)for(int z=(site.z()-50)>>4;z<=(site.z()+50)>>4;z++)level.setChunkForced(x,z,false);}
    }
    private Fixture fixture(GameTestHelper ctx,int rotation,boolean template){
        var level=ctx.getLevel();int x=100009+NEXT.incrementAndGet()*400,z=100000;
        var site=new SkyTowerLayout.Site(level.dimension().identifier().toString(),x,80,z,rotation);
        for(int cx=(x-50)>>4;cx<=(x+50)>>4;cx++)for(int cz=(z-50)>>4;cz<=(z+50)>>4;cz++){
            level.setChunkForced(cx,cz,true);level.getChunk(cx,cz);
        }
        for(int cx=(x-50)>>4;cx<=(x+50)>>4;cx++)for(int cz=(z-50)>>4;cz<=(z+50)>>4;cz++)
            level.waitForEntities(new net.minecraft.world.level.ChunkPos(cx,cz),0);
        if(template){
            var rotations=new Rotation[]{Rotation.NONE,Rotation.CLOCKWISE_90,Rotation.CLOCKWISE_180,Rotation.COUNTERCLOCKWISE_90};
            var origin=new BlockPos(x,80,z);
            var t=level.getStructureTemplateManager().getOrCreate(Identifier.parse(SkyTowerLayout.TEMPLATE));
            ctx.assertTrue(t.placeInWorld(level,origin,origin,new StructurePlaceSettings().setRotation(rotations[rotation]),level.getRandom(),18),"approved template placed");
        }
        var data=level.getServer().overworld().getDataStorage().computeIfAbsent(SkyTowerData.TYPE);data.register(site);
        return new Fixture(level,data,site);
    }
    @GameTest(maxTicks=300)
    public void fourRotationsPlaceAnchorsAndReleaseOnlyCompletedFloors(GameTestHelper ctx){
        for(int r=0;r<4;r++){
            var a=fixture(ctx,r,true);
            for(int i=0;i<16;i++)ctx.assertTrue(a.level.getBlockState(SkyTowerBarriers.pos(a.site.spawner(i))).is(Blocks.TRIAL_SPAWNER),"rotated spawner anchor");
            for(int i=0;i<8;i++)ctx.assertTrue(a.level.getBlockState(SkyTowerBarriers.pos(a.site.vault(i))).is(Blocks.VAULT),"rotated vanilla vault anchor");
            for(int f=0;f<4;f++){
                ctx.assertTrue(SkyTowerBarriers.reconcile(a.level,a.data,a.site,f),"floor installs");
                for(int i=0;i<18;i++)ctx.assertTrue(a.level.getBlockState(a.at(f,i)).is(Blocks.BARRIER) && a.data.ownsBarrier(a.site.id(),f,i),"all72 cells installed and owned");
            }
            for(int i=0;i<4;i++)a.data.recordVanillaCompletion(a.site.id(),a.site.spawner(i));
            SkyTowerBarriers.reconcile(a.level,a.data,a.site,0);
            for(int i=0;i<18;i++)ctx.assertTrue(a.level.getBlockState(a.at(0,i)).isAir(),"completed floor opens");
            ctx.assertTrue(a.level.getBlockState(a.at(1,0)).is(Blocks.BARRIER),"next incomplete floor remains blocked");
            SkyTowerBarriers.reconcile(a.level,a.data,a.site,0);
            ctx.assertTrue(a.level.getBlockState(a.at(0,0)).isAir(),"completed floor never reinstalls");a.release();
        }
        ctx.succeed();
    }
    @GameTest(maxTicks=160)
    public void foreignBlocksEntitiesAndReplacedBarriersAreNeverOverwrittenOrAdopted(GameTestHelper ctx){
        var a=fixture(ctx,0,false);var p=a.at(0,0);
        a.level.setBlock(p,Blocks.STONE.defaultBlockState(),3);
        ctx.assertTrue(!SkyTowerBarriers.reconcile(a.level,a.data,a.site,0),"stone defers floor");
        ctx.assertTrue(a.level.getBlockState(p).is(Blocks.STONE) && a.level.getBlockState(a.at(0,1)).isAir(),"no overwrite or partial wall");
        ctx.runAfterDelay(5,()->{
        a.level.setBlock(p,Blocks.AIR.defaultBlockState(),3);
        var item=new ItemEntity(a.level,p.getX()-.05,p.getY()+.1,p.getZ()+.5,new ItemStack(Items.STONE));ctx.assertTrue(a.level.addFreshEntity(item),"real entity inserted");
        ctx.assertTrue(!SkyTowerBarriers.reconcile(a.level,a.data,a.site,0),"entity centred in adjacent chunk with overlapping body defers installation");item.discard();
        ctx.assertTrue(SkyTowerBarriers.reconcile(a.level,a.data,a.site,0),"vacant air installs");
        a.level.setBlock(p,Blocks.AIR.defaultBlockState(),3);
        ctx.assertTrue(!a.data.ownsBarrier(a.site.id(),0,0),"committed removal revokes ownership");
        a.level.setBlock(p,Blocks.BARRIER.defaultBlockState(),3);
        for(int i=0;i<4;i++)a.data.recordDestruction(a.site.id(),a.site.spawner(i));
        SkyTowerBarriers.reconcile(a.level,a.data,a.site,0);
        ctx.assertTrue(a.level.getBlockState(p).is(Blocks.BARRIER),"foreign barrier survives completion");
        ctx.assertTrue(a.level.getBlockState(a.at(0,1)).isAir(),"owned barriers removed");a.release();ctx.succeed();
        });
    }
    @GameTest(maxTicks=160)
    public void committedSpawnerRemovalAndVanillaCooldownShareQuorumWithoutCrossInstanceProgress(GameTestHelper ctx){
        var a=fixture(ctx,0,false);var b=fixture(ctx,1,false);
        for(int i=0;i<4;i++)a.level.setBlock(SkyTowerBarriers.pos(a.site.spawner(i)),Blocks.TRIAL_SPAWNER.defaultBlockState(),3);
        SkyTowerBarriers.reconcile(a.level,a.data,a.site,0);
        for(int i=0;i<3;i++)a.level.destroyBlock(SkyTowerBarriers.pos(a.site.spawner(i)),false);
        ctx.assertTrue(a.data.completedMask(a.site.id())==7 && a.data.destroyedMask(a.site.id())==7,"actual successful removals count");
        var last=SkyTowerBarriers.pos(a.site.spawner(3));
        a.level.setBlock(last,Blocks.TRIAL_SPAWNER.defaultBlockState().setValue(TrialSpawnerBlock.STATE,TrialSpawnerState.COOLDOWN),3);
        ctx.runAfterDelay(25,()->{
            ctx.assertTrue(a.data.completedMask(a.site.id())==15 && a.data.destroyedMask(a.site.id())==7,"real vanilla tick cooldown supplies final normal completion");
            a.level.destroyBlock(last,false);
            ctx.assertTrue(a.data.destroyedMask(a.site.id())==7,"normal-first cause survives later destruction");
            a.level.setBlock(SkyTowerBarriers.pos(a.site.spawner(0)),Blocks.TRIAL_SPAWNER.defaultBlockState(),3);
            ctx.assertTrue(a.data.completedMask(a.site.id())==15,"reinstalled spawner never resets progress");
            ctx.assertTrue(a.data.completedMask(b.site.id())==0,"other instance isolated");
            SkyTowerBarriers.reconcile(a.level,a.data,a.site,0);
            ctx.assertTrue(a.level.getBlockState(a.at(0,0)).isAir(),"mixed quorum opens barrier");a.release();b.release();ctx.succeed();
        });
    }
    @GameTest(maxTicks=160)
    public void waterloggedOwnedBarrierIsForeignAndSurvivesCompletionAndStaleOwnership(GameTestHelper ctx){
        var a=fixture(ctx,0,false);var p=a.at(0,0);
        ctx.assertTrue(SkyTowerBarriers.reconcile(a.level,a.data,a.site,0),"dry barriers installed");
        var wet=Blocks.BARRIER.defaultBlockState().setValue(net.minecraft.world.level.block.BarrierBlock.WATERLOGGED,true);
        a.level.setBlock(p,wet,3);
        ctx.assertTrue(!a.data.ownsBarrier(a.site.id(),0,0),"external state change revokes ownership even within the same block type");
        for(int i=0;i<4;i++)a.data.recordDestruction(a.site.id(),a.site.spawner(i));
        SkyTowerBarriers.reconcile(a.level,a.data,a.site,0);
        ctx.assertTrue(a.level.getBlockState(p).equals(wet),"foreign waterlogged barrier not removed");
        ctx.assertTrue(a.level.getBlockState(a.at(0,1)).isAir(),"remaining owned dry barrier removed");
        a.data.setBarrierOwned(a.site.id(),0,0,true); // Deliberate stale saved-ownership fault.
        SkyTowerBarriers.reconcile(a.level,a.data,a.site,0);
        ctx.assertTrue(!a.data.ownsBarrier(a.site.id(),0,0) && a.level.getBlockState(p).equals(wet),"stale ownership is relinquished without deleting changed state");
        a.release();ctx.succeed();
    }

    @GameTest(maxTicks=300)
    public void allSixCompositeIslandTemplatesPlaceApprovedTowerAnchorsAcrossFourRotations(GameTestHelper ctx){
        String[] styles={"snowy","meadow","forest","badlands","desert","rocky"};
        Rotation[] rotations={Rotation.NONE,Rotation.CLOCKWISE_90,Rotation.CLOCKWISE_180,Rotation.COUNTERCLOCKWISE_90};
        var level=ctx.getLevel();
        int base=NEXT.incrementAndGet()*1000+200000;
        for(int index=0;index<styles.length;index++){
            int rotation=index%4;
            int x=base+(index%3)*120,z=base+(index/3)*120,y=100;
            var compositeOrigin=new BlockPos(x,y,z);
            for(int cx=(x-52)>>4;cx<=(x+60)>>4;cx++)for(int cz=(z-52)>>4;cz<=(z+60)>>4;cz++){
                level.setChunkForced(cx,cz,true);level.getChunk(cx,cz);
            }
            for(int cx=(x-52)>>4;cx<=(x+60)>>4;cx++)for(int cz=(z-52)>>4;cz<=(z+60)>>4;cz++)
                level.waitForEntities(new net.minecraft.world.level.ChunkPos(cx,cz),0);
            var id=Identifier.parse("moreminecrafts:sky_islands/"+styles[index]+"/tower");
            var template=level.getStructureTemplateManager().getOrCreate(id);
            ctx.assertTrue(template.placeInWorld(level,compositeOrigin,compositeOrigin,
                    new StructurePlaceSettings().setRotation(rotations[rotation]),level.getRandom(),18),styles[index]+" composite places");
            var towerOrigin=compositeOrigin.offset(net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.transform(
                    new BlockPos(0,17,0),net.minecraft.world.level.block.Mirror.NONE,rotations[rotation],BlockPos.ZERO));
            var site=new SkyTowerLayout.Site(level.dimension().identifier().toString(),towerOrigin.getX(),towerOrigin.getY(),towerOrigin.getZ(),rotation);
            for(int i=0;i<16;i++){
                var p=site.spawner(i);
                ctx.assertTrue(level.getBlockState(new BlockPos(p.x(),p.y(),p.z())).is(Blocks.TRIAL_SPAWNER),styles[index]+" approved spawner "+i);
            }
            for(int i=0;i<8;i++){
                var p=site.vault(i);
                ctx.assertTrue(level.getBlockState(new BlockPos(p.x(),p.y(),p.z())).is(Blocks.VAULT),styles[index]+" approved vault "+i);
            }
            for(int floor=0;floor<4;floor++)for(int cell=0;cell<18;cell++){
                var p=site.barrier(floor,cell);
                ctx.assertTrue(level.getBlockState(new BlockPos(p.x(),p.y(),p.z())).isAir(),styles[index]+" reserved barrier cell remains clear");
            }
        }
        ctx.succeed();
    }
}
