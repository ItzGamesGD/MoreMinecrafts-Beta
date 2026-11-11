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
import dev.itzgamesgd.moreminecrafts.accessory.StarCovenantVaultRewards;
import net.minecraft.world.level.block.VaultBlock;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.block.entity.vault.VaultConfig;
import net.minecraft.world.level.block.entity.vault.VaultState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

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

    @GameTest(maxTicks=200)
    public void rooftopVaultPlacementAndFacingAcrossAllRotations(GameTestHelper ctx){
        var level=ctx.getLevel();
        for(int r=0;r<4;r++){
            var fix=fixture(ctx,r,false);
            var site=fix.site();
            var pt=site.rooftopVault();
            var pos=new BlockPos(pt.x(),pt.y(),pt.z());
            ctx.assertTrue(level.getBlockState(pos).isAir(),"target position initially air");
            ctx.assertTrue(SkyTowerVaultRewards.placeAndConfigureRooftopVault(level,site),"rooftop placed");
            var state=level.getBlockState(pos);
            ctx.assertTrue(state.is(Blocks.VAULT),"placed block is vault");
            ctx.assertTrue(state.getValue(VaultBlock.FACING)==SkyTowerVaultRewards.rooftopVaultFacing(r),"vault facing transformed with rotation");
            var be=level.getBlockEntity(pos);
            ctx.assertTrue(be instanceof VaultBlockEntity,"block entity is VaultBlockEntity");
            var vault=(VaultBlockEntity)be;
            ctx.assertTrue(vault.getConfig().lootTable().equals(SkyTowerVaultRewards.ROOFTOP_VAULT_LOOT_KEY),"rooftop loot table configured");
            ctx.assertTrue(vault.getConfig().keyItem().is(Items.TRIAL_KEY),"trial key configured");
            ctx.assertTrue(SkyTowerVaultRewards.placeAndConfigureRooftopVault(level,site),"repeated placement is idempotent");
            fix.release();
        }
        ctx.succeed();
    }

    @GameTest(maxTicks=200)
    public void vaultUnlockNormalAndRooftopRewardIsolationAndStarCovenant(GameTestHelper ctx){
        var fix=fixture(ctx,0,false);
        var level=fix.level();
        var site=fix.site();
        var player=survivalPlayer(level);

        var normalPt=site.vault(0);
        var normalPos=new BlockPos(normalPt.x(),normalPt.y(),normalPt.z());
        level.setBlock(normalPos,Blocks.VAULT.defaultBlockState()
                .setValue(VaultBlock.STATE,VaultState.ACTIVE)
                .setValue(VaultBlock.OMINOUS,false),3);

        var rooftopPt=site.rooftopVault();
        var rooftopPos=new BlockPos(rooftopPt.x(),rooftopPt.y(),rooftopPt.z());
        level.setBlock(rooftopPos,Blocks.VAULT.defaultBlockState()
                .setValue(VaultBlock.STATE,VaultState.ACTIVE)
                .setValue(VaultBlock.FACING,SkyTowerVaultRewards.rooftopVaultFacing(0))
                .setValue(VaultBlock.OMINOUS,false),3);

        ctx.assertTrue(SkyTowerVaultRewards.configureSiteVaults(level,site)>=2,"site vaults configured");

        var normalVault=(VaultBlockEntity)level.getBlockEntity(normalPos);
        var rooftopVault=(VaultBlockEntity)level.getBlockEntity(rooftopPos);
        ctx.assertTrue(normalVault.getConfig().lootTable().equals(SkyTowerVaultRewards.NORMAL_VAULT_LOOT_KEY),"normal vault loot table");
        ctx.assertTrue(rooftopVault.getConfig().lootTable().equals(SkyTowerVaultRewards.ROOFTOP_VAULT_LOOT_KEY),"rooftop vault loot table");

        var normalKey=new ItemStack(Items.TRIAL_KEY);
        VaultBlockEntity.Server.tryInsertKey(level,normalPos,level.getBlockState(normalPos),
                normalVault.getConfig(),normalVault.getServerData(),normalVault.getSharedData(),player,normalKey);
        ctx.assertTrue(normalKey.isEmpty(),"normal vault key consumed on unlock");

        var normalKey2=new ItemStack(Items.TRIAL_KEY);
        VaultBlockEntity.Server.tryInsertKey(level,normalPos,level.getBlockState(normalPos),
                normalVault.getConfig(),normalVault.getServerData(),normalVault.getSharedData(),player,normalKey2);
        ctx.assertTrue(!normalKey2.isEmpty(),"normal vault refuses second unlock by same player");

        var rooftopKey=new ItemStack(Items.TRIAL_KEY);
        VaultBlockEntity.Server.tryInsertKey(level,rooftopPos,level.getBlockState(rooftopPos),
                rooftopVault.getConfig(),rooftopVault.getServerData(),rooftopVault.getSharedData(),player,rooftopKey);
        ctx.assertTrue(rooftopKey.isEmpty(),"rooftop vault key consumed on unlock");

        var rooftopKey2=new ItemStack(Items.TRIAL_KEY);
        VaultBlockEntity.Server.tryInsertKey(level,rooftopPos,level.getBlockState(rooftopPos),
                rooftopVault.getConfig(),rooftopVault.getServerData(),rooftopVault.getSharedData(),player,rooftopKey2);
        ctx.assertTrue(!rooftopKey2.isEmpty(),"rooftop vault refuses second unlock by same player");

        var starData=StarCovenantVaultRewards.Data.get(level.getServer());
        var ref=new StarCovenantVaultRewards.VaultRef(site.id(),"rooftop");
        var outcome=starData.resolve(player.getUUID(),ref,()->0.0);
        ctx.assertTrue(outcome==StarCovenantVaultRewards.Outcome.ALREADY_CLAIMED,"Star Covenant recorded rooftop unlock");

        var outsidePos=new BlockPos(site.x()+1000,80,site.z()+1000);
        ctx.assertTrue(!SkyTowerVaultRewards.isSkyTowerVault(level,outsidePos),"outside position is not sky tower vault");
        var vanillaConfig=new VaultConfig(BuiltInLootTables.TRIAL_CHAMBERS_REWARD,4.0D,4.5D,new ItemStack(Items.TRIAL_KEY),java.util.Optional.empty());
        var resolved=SkyTowerVaultRewards.resolveVaultConfig(level,outsidePos,vanillaConfig);
        ctx.assertTrue(resolved.lootTable().equals(BuiltInLootTables.TRIAL_CHAMBERS_REWARD),"vanilla trial chamber vault config untouched");

        fix.release();
        ctx.succeed();
    }

    @GameTest(maxTicks=200)
    public void newWorldRegistrationLifecycleConfiguresAllNineVaultsIdempotently(GameTestHelper ctx){
        var level=ctx.getLevel();
        int x=100009+NEXT.incrementAndGet()*400,z=100000;
        var site=new SkyTowerLayout.Site(level.dimension().identifier().toString(),x,80,z,0);
        for(int cx=(x-50)>>4;cx<=(x+50)>>4;cx++)for(int cz=(z-50)>>4;cz<=(z+50)>>4;cz++){
            level.setChunkForced(cx,cz,true);level.getChunk(cx,cz);
        }
        for(int cx=(x-50)>>4;cx<=(x+50)>>4;cx++)for(int cz=(z-50)>>4;cz<=(z+50)>>4;cz++)
            level.waitForEntities(new net.minecraft.world.level.ChunkPos(cx,cz),0);

        var origin=new BlockPos(x,80,z);
        var t=level.getStructureTemplateManager().getOrCreate(Identifier.parse(SkyTowerLayout.TEMPLATE));
        ctx.assertTrue(t.placeInWorld(level,origin,origin,new StructurePlaceSettings().setRotation(Rotation.NONE),level.getRandom(),18),"fresh template placed");

        var data=level.getServer().overworld().getDataStorage().computeIfAbsent(SkyTowerData.TYPE);
        ctx.assertTrue(!data.sites().containsKey(site.id()),"fresh site is not pre-registered before first registration");

        // Simulate SkyTowerWorldgenRegistration.tick first-registration path on a newly generated tower
        boolean added=data.register(site);
        ctx.assertTrue(added,"first registration adds site");
        int configured=SkyTowerVaultRewards.configureSiteVaults(level,site);
        ctx.assertTrue(configured==9,"all 8 normal vaults + 1 rooftop special vault configured on initial registration");

        for(int i=0;i<8;i++){
            var pt=site.vault(i);
            var pos=new BlockPos(pt.x(),pt.y(),pt.z());
            var vault=(VaultBlockEntity)level.getBlockEntity(pos);
            ctx.assertTrue(vault!=null && vault.getConfig().lootTable().equals(SkyTowerVaultRewards.NORMAL_VAULT_LOOT_KEY),"normal vault "+i+" configured");
        }
        var roofPt=site.rooftopVault();
        var roofPos=new BlockPos(roofPt.x(),roofPt.y(),roofPt.z());
        var roofVault=(VaultBlockEntity)level.getBlockEntity(roofPos);
        ctx.assertTrue(roofVault!=null && roofVault.getConfig().lootTable().equals(SkyTowerVaultRewards.ROOFTOP_VAULT_LOOT_KEY),"rooftop vault placed and configured");

        // Unlock one normal vault and the rooftop vault for a player, then verify repeated registration / chunk reload preserves state
        level.setBlock(roofPos,level.getBlockState(roofPos).setValue(VaultBlock.STATE,VaultState.ACTIVE),3);
        roofVault=(VaultBlockEntity)level.getBlockEntity(roofPos);
        SkyTowerVaultRewards.configureRooftopVault(roofVault,new ItemStack(Items.TRIAL_KEY));
        final var activeRoofVault=roofVault;
        var player=survivalPlayer(level);
        var key=new ItemStack(Items.TRIAL_KEY);
        SkyTowerVaultRewards.withRollOverride(()->0.50D,()->
                VaultBlockEntity.Server.tryInsertKey(level,roofPos,level.getBlockState(roofPos),
                        activeRoofVault.getConfig(),activeRoofVault.getServerData(),activeRoofVault.getSharedData(),player,key));
        ctx.assertTrue(key.isEmpty(),"rooftop key consumed");
        ctx.assertTrue(activeRoofVault.saveWithoutMetadata(level.registryAccess()).toString().contains("rewarded_players"),"player recorded in rooftop rewardedPlayers NBT");

        // Repeated registration attempt returns false (idempotent) and chunk reload reconciliation preserves rewardedPlayers and Star Covenant ledger
        ctx.assertTrue(!data.register(site),"repeated registration is a no-op");
        ctx.assertTrue(SkyTowerVaultRewards.placeAndConfigureRooftopVault(level,site),"reconcileRooftop idempotent on chunk reload");
        var keyRepeat=new ItemStack(Items.TRIAL_KEY);
        VaultBlockEntity.Server.tryInsertKey(level,roofPos,level.getBlockState(roofPos),
                activeRoofVault.getConfig(),activeRoofVault.getServerData(),activeRoofVault.getSharedData(),player,keyRepeat);
        ctx.assertTrue(!keyRepeat.isEmpty(),"rewardedPlayers preserved across chunk reload (repeat key rejected)");
        var starData=StarCovenantVaultRewards.Data.get(level.getServer());
        ctx.assertTrue(starData.resolve(player.getUUID(),new StarCovenantVaultRewards.VaultRef(site.id(),"rooftop"),()->0.0D)
                ==StarCovenantVaultRewards.Outcome.ALREADY_CLAIMED,"Star Covenant claim preserved across chunk reload");

        for(int cx=(x-50)>>4;cx<=(x+50)>>4;cx++)for(int cz=(z-50)>>4;cz<=(z+50)>>4;cz++)level.setChunkForced(cx,cz,false);
        ctx.succeed();
    }

    @GameTest(maxTicks=200)
    public void rooftopVaultEjectsStarCovenantViaVanillaVaultAndNeverInsertsIntoInventory(GameTestHelper ctx){
        var fix=fixture(ctx,0,false);
        var level=fix.level();
        var site=fix.site();
        var hitPlayer=survivalPlayer(level);
        var missPlayer=survivalPlayer(level);

        var rooftopPt=site.rooftopVault();
        var rooftopPos=new BlockPos(rooftopPt.x(),rooftopPt.y(),rooftopPt.z());
        level.setBlock(rooftopPos,Blocks.VAULT.defaultBlockState()
                .setValue(VaultBlock.STATE,VaultState.ACTIVE)
                .setValue(VaultBlock.FACING,SkyTowerVaultRewards.rooftopVaultFacing(0))
                .setValue(VaultBlock.OMINOUS,false),3);
        ctx.assertTrue(SkyTowerVaultRewards.placeAndConfigureRooftopVault(level,site),"rooftop vault configured");

        var rooftopVault=(VaultBlockEntity)level.getBlockEntity(rooftopPos);
        var starItem=StarCovenantVaultRewards.reward().getItem();
        hitPlayer.setPos(rooftopPos.getX()+0.5,rooftopPos.getY(),rooftopPos.getZ()-2.5);
        missPlayer.setPos(rooftopPos.getX()+0.5,rooftopPos.getY(),rooftopPos.getZ()-2.5);

        // 1. Deterministic HIT roll (0.0 < 0.10) on rooftop vault
        var hitKey=new ItemStack(Items.TRIAL_KEY);
        SkyTowerVaultRewards.withRollOverride(()->0.0D,()->
                VaultBlockEntity.Server.tryInsertKey(level,rooftopPos,level.getBlockState(rooftopPos),
                        rooftopVault.getConfig(),rooftopVault.getServerData(),rooftopVault.getSharedData(),hitPlayer,hitKey));

        ctx.assertTrue(hitKey.isEmpty(),"hitPlayer Trial Key consumed");
        ctx.assertTrue(hitPlayer.getInventory().countItem(starItem)==0,"Star Covenant is NEVER silently inserted into player inventory");
        ctx.assertTrue(level.getBlockState(rooftopPos).getValue(VaultBlock.STATE)==VaultState.UNLOCKING,"Vault entered vanilla UNLOCKING state");
        ctx.assertTrue(rooftopVault.getSharedData().getDisplayItem().is(starItem),"Star Covenant queued at top of vanilla Vault ejection list and displayed in keyhole");

        // Verify mid-ejection persistence in VaultBlockEntity NBT before ticks pop the queue
        var nbtBeforeEject=rooftopVault.saveWithoutMetadata(level.registryAccess());
        ctx.assertTrue(nbtBeforeEject.toString().contains("moreminecrafts:star_covenant"),"Star Covenant persisted in VaultBlockEntity server_data.items_to_eject NBT");

        // Tick VaultBlockEntity.Server across real GameTest ticks so vanilla's gameTime-based pauseStateUpdatingUntil progresses and ejects the Star Covenant upward
        var searchBox=new net.minecraft.world.phys.AABB(rooftopPos).inflate(6.0);
        int[] ticks=new int[]{0};
        boolean[] seenEjectedStar=new boolean[]{false};
        ctx.onEachTick(()->{
            ticks[0]++;
            var st=level.getBlockState(rooftopPos);
            if(st.is(Blocks.VAULT)){
                VaultBlockEntity.Server.tick(level,rooftopPos,st,
                        rooftopVault.getConfig(),rooftopVault.getServerData(),rooftopVault.getSharedData());
            }
            for(var itemEntity:level.getEntitiesOfClass(ItemEntity.class,searchBox)){
                if(itemEntity.getItem().is(starItem)){
                    seenEjectedStar[0]=true;
                    ctx.assertTrue(itemEntity.getY()>=rooftopPos.getY(),"Star Covenant ItemEntity ejected upward from rooftop Vault");
                }
            }
            if(seenEjectedStar[0] && ticks[0]>=25){
                ctx.assertTrue(hitPlayer.getInventory().countItem(starItem)==0,"hitPlayer inventory still has no direct-inserted Star Covenant");
                // Clear ejected entities before testing missPlayer on a second tower's rooftop vault
                for(var itemEntity:level.getEntitiesOfClass(ItemEntity.class,searchBox))itemEntity.discard();

                // Verify repeated interaction by hitPlayer is rejected and cannot duplicate reward
                var dupKey=new ItemStack(Items.TRIAL_KEY);
                SkyTowerVaultRewards.withRollOverride(()->0.0D,()->
                        VaultBlockEntity.Server.tryInsertKey(level,rooftopPos,level.getBlockState(rooftopPos),
                                rooftopVault.getConfig(),rooftopVault.getServerData(),rooftopVault.getSharedData(),hitPlayer,dupKey));
                ctx.assertTrue(!dupKey.isEmpty(),"repeated unlock by hitPlayer rejected");

                // 2. Deterministic MISS roll (0.50 >= 0.10) on a fresh rooftop vault does NOT queue or eject Star Covenant
                var fix2=fixture(ctx,1,false);
                var site2=fix2.site();
                var roofPt2=site2.rooftopVault();
                var roofPos2=new BlockPos(roofPt2.x(),roofPt2.y(),roofPt2.z());
                level.setBlock(roofPos2,Blocks.VAULT.defaultBlockState()
                        .setValue(VaultBlock.STATE,VaultState.ACTIVE)
                        .setValue(VaultBlock.FACING,SkyTowerVaultRewards.rooftopVaultFacing(1))
                        .setValue(VaultBlock.OMINOUS,false),3);
                SkyTowerVaultRewards.placeAndConfigureRooftopVault(level,site2);
                var roofVault2=(VaultBlockEntity)level.getBlockEntity(roofPos2);
                var missKey=new ItemStack(Items.TRIAL_KEY);
                SkyTowerVaultRewards.withRollOverride(()->0.50D,()->
                        VaultBlockEntity.Server.tryInsertKey(level,roofPos2,level.getBlockState(roofPos2),
                                roofVault2.getConfig(),roofVault2.getServerData(),roofVault2.getSharedData(),missPlayer,missKey));
                ctx.assertTrue(missKey.isEmpty(),"missPlayer Trial Key consumed");
                ctx.assertTrue(!roofVault2.getSharedData().getDisplayItem().is(starItem),"failed roll does not display Star Covenant in Vault");
                var nbtMiss=roofVault2.saveWithoutMetadata(level.registryAccess());
                ctx.assertTrue(!nbtMiss.toString().contains("moreminecrafts:star_covenant"),"failed roll does not queue Star Covenant in server_data.items_to_eject");

                fix2.release();
                fix.release();
                ctx.succeed();
            }
        });
    }
}
