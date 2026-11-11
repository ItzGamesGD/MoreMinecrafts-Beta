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
    /** GameTest's stock in-level mock reports CREATIVE, which vanilla Trial Spawners deliberately ignore.
     * This one is a plain survival ServerPlayer with a loopback connection, placed through the vanilla PlayerList. */
    private static net.minecraft.server.level.ServerPlayer survivalPlayer(ServerLevel level){
        var profile=new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"c4-survivor");
        var cookie=net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false);
        var player=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,profile,cookie.clientInformation());
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection,player,cookie);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        return player;
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
    /** C-4: real vanilla lifecycle. No completion marker, cooldown block write or manual reconcile is used for the
     * subject tower: survival players are detected by the four floor-0 Trial Spawners, vanilla spawns real zombies,
     * player-attributed damage kills them, and vanilla's own tick walks ACTIVE -> reward ejection -> COOLDOWN.
     * The mod hook and the 20-tick barrier tick must then record 4/4 and open the floor-0 wall.
     * mode: 0 normal; 1 the last spawner is really removed while ACTIVE (destroyed-during-combat). */
    private void runRealCombat(GameTestHelper ctx,int rotation,int players,int mode,String tag){
        var a=fixture(ctx,rotation,true);var other=fixture(ctx,(rotation+1)%4,true);var level=a.level;
        level.getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL,true);
        var ps=new java.util.ArrayList<net.minecraft.server.level.ServerPlayer>();
        var mid=a.site.at(24,11,24);
        for(int i=0;i<players;i++){var p=survivalPlayer(level);p.setPos(mid.x()+.5+i,mid.y(),mid.z()+.5);ps.add(p);}
        var order=new java.util.ArrayList<String>();
        var last=new TrialSpawnerState[4];var maskAtCooldown=new int[]{-1,-1,-1,-1};var killed=new int[]{0};var t=new int[]{0};
        var barrierSeen=new boolean[]{false};var openedAt=new int[]{-1};var ghost=new int[]{0};
        var box=new net.minecraft.world.phys.AABB(mid.x()-30,mid.y()-4,mid.z()-30,mid.x()+30,mid.y()+10,mid.z()+30);
        ctx.onEachTick(()->{
            t[0]++;
            if(!barrierSeen[0] && level.getBlockState(a.at(0,0)).is(Blocks.BARRIER))barrierSeen[0]=true;
            for(int i=0;i<4;i++){
                var pos=SkyTowerBarriers.pos(a.site.spawner(i));var st=level.getBlockState(pos);
                if(!st.is(Blocks.TRIAL_SPAWNER))continue;
                var s=st.getValue(TrialSpawnerBlock.STATE);
                if(s!=last[i]){order.add("t"+t[0]+" sp"+i+" "+s);last[i]=s;
                    if(s==TrialSpawnerState.COOLDOWN)maskAtCooldown[i]=a.data.completedMask(a.site.id());
                    if(openedAt[0]<0 && s!=TrialSpawnerState.COOLDOWN && s!=TrialSpawnerState.INACTIVE && s!=TrialSpawnerState.WAITING_FOR_PLAYERS)
                        ctx.assertTrue((a.data.completedMask(a.site.id())&(1<<i))==0,"no completion before vanilla COOLDOWN (sp"+i+" "+s+")");
                    if(mode==1 && i==3 && s==TrialSpawnerState.ACTIVE){
                        ctx.assertTrue((a.data.completedMask(a.site.id())&8)==0,"sp3 incomplete when removed mid-combat");
                        level.destroyBlock(pos,false);order.add("t"+t[0]+" sp3 DESTROYED_WHILE_ACTIVE");
                    }
                }
            }
            for(var z:level.getEntitiesOfClass(net.minecraft.world.entity.monster.zombie.Zombie.class,box))
                if(z.isAlive()){z.hurtServer(level,level.damageSources().playerAttack(ps.get(0)),1000f);killed[0]++;}
            if(openedAt[0]<0 && a.data.floorCleared(a.site.id(),0)){
                openedAt[0]=t[0];
                ctx.assertTrue(barrierSeen[0],"barrier wall was installed before combat completed");
                ctx.assertTrue(a.data.completedMask(a.site.id())==15,"exactly floor-0 mask "+a.data.completedMask(a.site.id()));
                if(mode==0){
                    ctx.assertTrue(killed[0]>=4*players,"real zombies killed "+killed[0]);
                    ctx.assertTrue(a.data.destroyedMask(a.site.id())==0,"completion is normal, not destruction");
                    for(int i=0;i<4;i++)ctx.assertTrue(maskAtCooldown[i]>=0,"sp"+i+" reached vanilla COOLDOWN");
                }else{
                    ctx.assertTrue(a.data.destroyedMask(a.site.id())==8,"only the removed spawner is a destruction: "+a.data.destroyedMask(a.site.id()));
                    for(int i=0;i<3;i++)ctx.assertTrue(maskAtCooldown[i]>=0,"sp"+i+" reached vanilla COOLDOWN");
                }
                System.out.println("[C4-combat:"+tag+"] cleared at t="+t[0]+" killed="+killed[0]+" order="+order);
            }
            if(openedAt[0]>=0 && t[0]==openedAt[0]+45){
                for(int c=0;c<18;c++)ctx.assertTrue(level.getBlockState(a.at(0,c)).isAir(),"floor-0 wall removed by the real barrier tick");
                ctx.assertTrue(level.getBlockState(a.at(1,0)).is(Blocks.BARRIER),"floor 1 wall still present");
                ctx.assertTrue(other.data.completedMask(other.site.id())==0 && level.getBlockState(other.at(0,0)).is(Blocks.BARRIER),"second tower untouched and still walled");
                // Persistence of real-combat-derived state through the production codec (NBT, as SavedData writes it).
                var ops=level.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
                var nbt=SkyTowerData.CODEC.encodeStart(ops,a.data).getOrThrow();
                var back=SkyTowerData.CODEC.parse(ops,nbt).getOrThrow();
                ctx.assertTrue(back.completedMask(a.site.id())==15 && back.destroyedMask(a.site.id())==a.data.destroyedMask(a.site.id())
                    && back.completedMask(other.site.id())==0 && !back.quarantined(),"NBT round-trip keeps masks");
                for(int c=0;c<18;c++)ctx.assertTrue(!back.ownsBarrier(a.site.id(),0,c) && back.ownsBarrier(a.site.id(),1,c),"ownership round-trip: floor 0 released, floor 1 owned");
                // Player-placed block in a released cell and next to the wall must survive; completed wall never re-installs.
                level.setBlock(a.at(0,0),Blocks.STONE.defaultBlockState(),3);
                level.setBlock(a.at(0,2).above(),Blocks.OAK_PLANKS.defaultBlockState(),3);
                // A completed spawner "reactivating" (synthetic block-state write; vanilla cooldown is 30 min) must not re-wall or reset.
                var sp0=SkyTowerBarriers.pos(a.site.spawner(0));
                if(level.getBlockState(sp0).is(Blocks.TRIAL_SPAWNER))
                    level.setBlock(sp0,level.getBlockState(sp0).setValue(TrialSpawnerBlock.STATE,TrialSpawnerState.ACTIVE),3);
            }
            if(openedAt[0]>=0 && t[0]==openedAt[0]+110){
                ctx.assertTrue(level.getBlockState(a.at(0,0)).is(Blocks.STONE) && level.getBlockState(a.at(0,2).above()).is(Blocks.OAK_PLANKS),"player blocks preserved");
                for(int c=1;c<18;c++)ctx.assertTrue(level.getBlockState(a.at(0,c)).isAir(),"no re-wall after reactivation");
                ctx.assertTrue(a.data.completedMask(a.site.id())==15,"mask unchanged by reactivation");
                a.release();other.release();ctx.succeed();
            }
        });
    }
    @GameTest(maxTicks=1500)
    public void realVanillaTrialCombatCompletesFloorZeroAndOpensBarrier(GameTestHelper ctx){runRealCombat(ctx,0,1,0,"rot0-1p");}
    @GameTest(maxTicks=1500)
    public void realVanillaTrialCombatTwoPlayersRotatedHalfTurn(GameTestHelper ctx){runRealCombat(ctx,2,2,0,"rot2-2p");}
    @GameTest(maxTicks=1500)
    public void realVanillaTrialCombatSpawnerRemovedWhileActiveStillOpensFloor(GameTestHelper ctx){runRealCombat(ctx,3,1,1,"rot3-destroy-active");}

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
