package dev.itzgamesgd.moreminecrafts.spidercolony;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.Attributes;
public final class SpiderColonyGameTests {
 static void forceArea(ServerLevel level,net.minecraft.world.level.ChunkPos center,boolean forced) {
  for(int dx=-1;dx<=1;dx++) for(int dz=-1;dz<=1;dz++) level.setChunkForced(center.x()+dx,center.z()+dz,forced);
 }
 @GameTest public void motherIsSummonableAndHasApprovedHealth(GameTestHelper ctx) {
  var mother=SpiderColonyEntities.MOTHER.create(ctx.getLevel(),EntitySpawnReason.COMMAND);
  ctx.assertTrue(mother!=null && mother.getAttributeValue(Attributes.MAX_HEALTH)==60.0,"mother must instantiate with 60 maximum health");
  mother.discard(); ctx.succeed();
 }
 @GameTest public void placedEggSacIsNotNaturallyReactive(GameTestHelper ctx) {
  var state=dev.itzgamesgd.moreminecrafts.registry.ModBlocks.SPIDER_EGG_SAC.defaultBlockState();
  ctx.assertTrue(!state.getValue(EggSacBlock.REACTIVE),"player placed default must be inert");
  ctx.succeed();
 }
 @GameTest(maxTicks=700) public void combatSummonsRespectCap(GameTestHelper ctx) {
  var level=ctx.getLevel();
  for(int x=-5;x<=10;x++) for(int z=-5;z<=10;z++) for(int y=2;y<=7;y++)
   level.setBlock(ctx.absolutePos(new net.minecraft.core.BlockPos(x,y,z)),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
  for(int x=-5;x<=10;x++) for(int z=-5;z<=10;z++)
   level.setBlock(ctx.absolutePos(new net.minecraft.core.BlockPos(x,2,z)),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
  var mother=ctx.spawn(SpiderColonyEntities.MOTHER,new net.minecraft.core.BlockPos(2,3,2));
  var target=ctx.spawn(net.minecraft.world.entity.EntityTypes.ZOMBIE,new net.minecraft.core.BlockPos(7,3,2));
  mother.setNoAi(true); mother.setNoGravity(true); target.setNoAi(true); target.setNoGravity(true); mother.setTarget(target);
  // A durable dummy survives both cooldowns, including daylight, like the Tomb Guardian fixture.
  target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000.0D); target.setHealth(1000.0F);
  // Juveniles are ordinary monsters: a nearby player prevents vanilla hard distance despawn,
  // matching the already-fixed Tomb Guardian summon fixture without changing production rules.
  var cookie=net.minecraft.server.network.CommonListenerCookie.createInitial(
    new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"spider-cap-anchor"),false);
  var anchor=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,cookie.gameProfile(),cookie.clientInformation());
  var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
  new io.netty.channel.embedded.EmbeddedChannel(connection);
  level.getServer().getPlayerList().placeNewPlayer(connection,anchor,cookie);
  anchor.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
  var position=ctx.absoluteVec(new net.minecraft.world.phys.Vec3(8.5,3,8.5));
  anchor.teleportTo(position.x,position.y,position.z);
  ctx.runAfterDelay(310,()-> {
   int count=SpiderColonyEntities.data(level).childCount(mother.getUUID());
   ctx.assertTrue(count>0 && count<=2,"first cycle must create at most two juveniles; got "+count);
   ctx.runAfterDelay(320,()-> {
    int later=SpiderColonyEntities.data(level).childCount(mother.getUUID());
    ctx.assertTrue(later<=4,"simultaneous summon cap exceeded: "+later);
    mother.discard(); target.discard(); level.getServer().getPlayerList().remove(anchor); ctx.succeed();
   });
  });
 }
 @GameTest public void motherHealthAndIdentitySurviveEntitySerialization(GameTestHelper ctx) {
  var first=SpiderColonyEntities.MOTHER.create(ctx.getLevel(),EntitySpawnReason.COMMAND);
  first.setHealth(17.0F);
  var out=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,ctx.getLevel().registryAccess());
  first.saveWithoutId(out);
  var restored=SpiderColonyEntities.MOTHER.create(ctx.getLevel(),EntitySpawnReason.COMMAND);
  restored.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,ctx.getLevel().registryAccess(),out.buildResult()));
  ctx.assertTrue(restored.getUUID().equals(first.getUUID()) && restored.getHealth()==17.0F,"identity and damage must survive serialization");
  first.discard(); restored.discard(); ctx.succeed();
 }
 @GameTest(maxTicks=30) public void reactiveEggSacOnlyHatchesOnBreak(GameTestHelper ctx) {
  var level=ctx.getLevel(); var pos=ctx.absolutePos(new net.minecraft.core.BlockPos(2,2,2));
  for(int x=-2;x<=2;x++) for(int z=-2;z<=2;z++) for(int y=0;y<=3;y++)
   level.setBlock(pos.offset(x,y,z),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
  var block=dev.itzgamesgd.moreminecrafts.registry.ModBlocks.SPIDER_EGG_SAC;
  var reactive=block.defaultBlockState().setValue(EggSacBlock.REACTIVE,true); level.setBlock(pos,reactive,3);
  var box=new net.minecraft.world.phys.AABB(pos).inflate(4);
  ctx.runAfterDelay(3,()-> {
   ctx.assertTrue(level.getEntitiesOfClass(JuvenileSpider.class,box).isEmpty(),"proximity alone must not hatch");
   var player=ctx.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
   var silk=level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH);
   var tool=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE); tool.enchant(silk,1);
   player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,tool);
   block.playerWillDestroy(level,pos,reactive,player);
   ctx.assertTrue(level.getEntitiesOfClass(JuvenileSpider.class,box).isEmpty(),"silk touch must not hatch");
   player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,net.minecraft.world.item.ItemStack.EMPTY);
   block.playerWillDestroy(level,pos,reactive,player); level.removeBlock(pos,false);
   int count=level.getEntitiesOfClass(JuvenileSpider.class,box).size();
   ctx.assertTrue(count>=1 && count<=2,"reactive natural sac must hatch one or two, got "+count); ctx.succeed();
  });
 }
 @GameTest public void killingBoundMotherPreventsAnotherForSameNest(GameTestHelper ctx) {
  var mother=ctx.spawn(SpiderColonyEntities.MOTHER,new net.minecraft.core.BlockPos(2,2,2));
  var id="gametest-"+mother.getUUID(); var level=ctx.getLevel();
  ctx.assertTrue(mother.bindNest(level,id),"first mother must bind");
  mother.hurtServer(level,level.damageSources().genericKill(),10000.0F);
  ctx.assertTrue(SpiderColonyEntities.data(level).defeated(id),"death must mark nest defeated");
  var replacement=SpiderColonyEntities.MOTHER.create(level,EntitySpawnReason.COMMAND);
  ctx.assertTrue(!replacement.bindNest(level,id),"defeated nest must reject replacement");
  replacement.discard();
  var p=mother.blockPosition();
  ctx.runAfterDelay(1,()-> {
   var drops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(p).inflate(4));
   int crystals=drops.stream().filter(e->e.getItem().is(dev.itzgamesgd.moreminecrafts.registry.ModItems.COLONY_CRYSTAL)).mapToInt(e->e.getItem().getCount()).sum();
   ctx.assertTrue(crystals==1,"mother death must drop exactly one colony crystal, got "+crystals);
   var normal=ctx.spawn(net.minecraft.world.entity.EntityTypes.SPIDER,new net.minecraft.core.BlockPos(5,2,2));
   ctx.assertTrue(normal.isAlive(),"ordinary spider remains spawnable after nest defeat"); normal.discard(); ctx.succeed();
  });
 }
 @GameTest(maxTicks=350) public void noTargetMeansNoSummons(GameTestHelper ctx) {
  var mother=ctx.spawn(SpiderColonyEntities.MOTHER,new net.minecraft.core.BlockPos(2,3,2));
  mother.setNoAi(true); mother.setNoGravity(true);
  ctx.runAfterDelay(310,()-> {
   ctx.assertTrue(SpiderColonyEntities.data(ctx.getLevel()).childCount(mother.getUUID())==0,"mother without combat target must not summon");
   mother.discard(); ctx.succeed();
  });
 }
 @GameTest(maxTicks=900) public void unloadedChunkKeepsMotherHealthAndFourReservations(GameTestHelper ctx) {
  var level=ctx.getLevel();
  var base=ctx.absolutePos(net.minecraft.core.BlockPos.ZERO).offset(4096,0,4096);
  var chunk=net.minecraft.world.level.ChunkPos.containing(base);
  forceArea(level,chunk,true);
  var spawn=level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,base);
  var phase=new java.util.concurrent.atomic.AtomicInteger();
  var motherId=new java.util.concurrent.atomic.AtomicReference<java.util.UUID>();
  var children=new java.util.ArrayList<java.util.UUID>();
  ctx.onEachTick(()-> {
   switch(phase.get()) {
    case 0 -> {
     if(!level.areEntitiesLoaded(chunk.pack()))return;
     var mother=SpiderColonyEntities.MOTHER.create(level,EntitySpawnReason.COMMAND);
     mother.setPos(spawn.getX()+0.5,spawn.getY()+2,spawn.getZ()+0.5); mother.setNoAi(true); mother.setNoGravity(true); mother.setHealth(17);
     level.addFreshEntity(mother); motherId.set(mother.getUUID());
     for(int i=0;i<4;i++) {
      var baby=SpiderColonyEntities.JUVENILE.create(level,EntitySpawnReason.MOB_SUMMONED);
      baby.setPos(mother.getX(),mother.getY(),mother.getZ()); baby.setNoAi(true); baby.setNoGravity(true);
      baby.addTag("mm_spider_owner_"+mother.getUUID()); SpiderColonyEntities.data(level).addChild(mother.getUUID(),baby.getUUID());
      children.add(baby.getUUID()); level.addFreshEntity(baby);
     }
     forceArea(level,chunk,false);phase.set(1);
    }
    case 1 -> {
     if(level.areEntitiesLoaded(chunk.pack()))return;
     ctx.assertTrue(SpiderColonyEntities.data(level).childCount(motherId.get())==4,"unloaded juveniles must reserve all four slots");
     ctx.assertTrue(SummonPolicy.allowed(true,SpiderColonyEntities.data(level).childCount(motherId.get()))==0,"unload must not bypass cap");
     forceArea(level,chunk,true); phase.set(2);
    }
    case 2 -> {
     if(!level.areEntitiesLoaded(chunk.pack()))return;
     var loaded=level.getEntity(motherId.get());
     if(!(loaded instanceof MotherSpider mother))return;
     for(var id:children)if(level.getEntity(id)==null)return;
     ctx.assertTrue(mother.getHealth()==17.0F,"mother damage must survive chunk reload");
     ctx.assertTrue(SpiderColonyEntities.data(level).childCount(motherId.get())==4,"reload must retain cap");
     for(var id:children)level.getEntity(id).discard(); mother.discard();
     forceArea(level,chunk,false);phase.set(3);ctx.succeed();
    }
    default -> { }
   }
  });
 }
 @GameTest(maxTicks=600) public void stateSurvivesActualServerRestart(GameTestHelper ctx) {
  String phase=System.getProperty("mm.spider.restart.phase","");
  if(phase.isBlank()) {ctx.succeed();return;}
  var level=ctx.getLevel(); var pos=new net.minecraft.core.BlockPos(-27992,300,-27992);
  var chunk=net.minecraft.world.level.ChunkPos.containing(pos); level.setChunkForced(chunk.x(),chunk.z(),true);
  String fixture=System.getProperty("mm.spider.restart.fixture","default");
  String liveNest="restart-live-"+fixture, deadNest="restart-defeated-"+fixture;
  var motherId=java.util.UUID.nameUUIDFromBytes(("spider-restart-"+fixture).getBytes(java.nio.charset.StandardCharsets.UTF_8));
  var done=new java.util.concurrent.atomic.AtomicBoolean();
  ctx.onEachTick(()-> {
   if(done.get() || !level.areEntitiesLoaded(chunk.pack()))return;
   if(phase.equals("write")) {
    var mother=SpiderColonyEntities.MOTHER.create(level,EntitySpawnReason.COMMAND);mother.setUUID(motherId);
    mother.setPos(pos.getX()+0.5,pos.getY(),pos.getZ()+0.5);mother.setNoAi(true);mother.setNoGravity(true);mother.setHealth(17);
    ctx.assertTrue(mother.bindNest(level,liveNest),"fresh live nest must bind");
    ctx.assertTrue(level.addFreshEntity(mother),"mother must enter world");
    for(int i=0;i<4;i++) {
     var baby=SpiderColonyEntities.JUVENILE.create(level,EntitySpawnReason.MOB_SUMMONED);
     baby.setPos(mother.getX(),mother.getY(),mother.getZ());baby.setNoAi(true);baby.setNoGravity(true);
     baby.addTag("mm_spider_owner_"+motherId); SpiderColonyEntities.data(level).addChild(motherId,baby.getUUID());
     ctx.assertTrue(level.addFreshEntity(baby),"baby must enter world");
    }
    var killed=SpiderColonyEntities.MOTHER.create(level,EntitySpawnReason.COMMAND);
    killed.setPos(mother.getX()+4,mother.getY(),mother.getZ());
    ctx.assertTrue(killed.bindNest(level,deadNest),"fresh defeated fixture must bind");
    ctx.assertTrue(level.addFreshEntity(killed),"defeat fixture must enter world");
    killed.hurtServer(level,level.damageSources().genericKill(),10000);
    ctx.assertTrue(SpiderColonyEntities.data(level).defeated(deadNest),"defeat must record before restart");
    level.getServer().saveEverything(true,true,true);
    done.set(true);System.out.println("SPIDER_RESTART_WRITE_OK");ctx.succeed();
   } else if(phase.equals("reload")) {
    var loaded=level.getEntity(motherId);
    if(!(loaded instanceof MotherSpider mother))return;
    ctx.assertTrue(mother.getHealth()==17.0F,"damage must survive process restart");
    ctx.assertTrue(SpiderColonyEntities.data(level).childCount(motherId)==4,"four juvenile reservations must survive restart");
    var box=new net.minecraft.world.phys.AABB(pos).inflate(16);
    int live=level.getEntitiesOfClass(JuvenileSpider.class,box,e->e.entityTags().contains("mm_spider_owner_"+motherId)).size();
    ctx.assertTrue(live==4,"all four actual juveniles must survive restart, got "+live);
    ctx.assertTrue(SummonPolicy.allowed(true,SpiderColonyEntities.data(level).childCount(motherId))==0,"restart cannot bypass summon cap");
    ctx.assertTrue(SpiderColonyEntities.data(level).defeated(deadNest),"defeated nest must survive restart");
    var duplicate=SpiderColonyEntities.MOTHER.create(level,EntitySpawnReason.COMMAND);
    ctx.assertTrue(!duplicate.bindNest(level,deadNest),"defeated nest must reject respawn after restart");duplicate.discard();
    done.set(true);System.out.println("SPIDER_RESTART_RELOAD_OK");ctx.succeed();
   } else ctx.fail("unknown restart phase");
  });
 }
 @GameTest public void juvenileIsWeakerRegularSpiderVariant(GameTestHelper ctx) {
  var baby=SpiderColonyEntities.JUVENILE.create(ctx.getLevel(),EntitySpawnReason.COMMAND);
  net.minecraft.world.entity.monster.spider.Spider spider=baby;
  ctx.assertTrue(!(spider instanceof net.minecraft.world.entity.monster.spider.CaveSpider),"juvenile must use regular spider, not cave spider");
  ctx.assertTrue(baby.getAttributeValue(Attributes.MAX_HEALTH)<net.minecraft.world.entity.monster.spider.Spider.createAttributes().build().getValue(Attributes.MAX_HEALTH),"juvenile health must be lower than a regular spider");
  ctx.assertTrue(baby.getBaseExperienceReward(ctx.getLevel())==2,"juvenile XP is two");
  baby.discard();ctx.succeed();
 }
 @GameTest public void juvenileDeathReleasesItsOwnersSlot(GameTestHelper ctx) {
  var level=ctx.getLevel(); var owner=java.util.UUID.randomUUID();
  var baby=ctx.spawn(SpiderColonyEntities.JUVENILE,new net.minecraft.core.BlockPos(2,2,2));
  baby.addTag("mm_spider_owner_"+owner); SpiderColonyEntities.data(level).addChild(owner,baby.getUUID());
  ctx.assertTrue(SpiderColonyEntities.data(level).childCount(owner)==1,"juvenile must reserve a slot");
  baby.hurtServer(level,level.damageSources().genericKill(),10000);
  ctx.assertTrue(SpiderColonyEntities.data(level).childCount(owner)==0,"death must release the slot");
  ctx.succeed();
 }
 @GameTest public void hangingCocoonRequiresCeilingSupport(GameTestHelper ctx) {
  var ceiling=new net.minecraft.core.BlockPos(3,5,3);
  var pos=ceiling.below();
  ctx.setBlock(ceiling,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
  ctx.setBlock(pos,dev.itzgamesgd.moreminecrafts.registry.ModBlocks.HANGING_COCOON.defaultBlockState());
  ctx.assertBlockPresent(dev.itzgamesgd.moreminecrafts.registry.ModBlocks.HANGING_COCOON,pos);
  ctx.setBlock(ceiling,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
  ctx.assertBlockPresent(net.minecraft.world.level.block.Blocks.AIR,pos);
  ctx.succeed();
 }
 @GameTest public void hangingCocoonRewardDropsString(GameTestHelper ctx) {
  var level=ctx.getLevel();
  var ceiling=new net.minecraft.core.BlockPos(3,5,3);
  var pos=ceiling.below();
  ctx.setBlock(ceiling,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
  ctx.setBlock(pos,dev.itzgamesgd.moreminecrafts.registry.ModBlocks.HANGING_COCOON.defaultBlockState().setValue(HangingCocoonBlock.REWARD,true));
  var player=ctx.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
  var absPos=ctx.absolutePos(pos);
  var state=level.getBlockState(absPos);
  state.getBlock().playerWillDestroy(level,absPos,state,player);
  level.removeBlock(absPos,false);
  var items=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(absPos).inflate(4),e->e.getItem().is(net.minecraft.world.item.Items.STRING));
  int totalString=items.stream().mapToInt(e->e.getItem().getCount()).sum();
  ctx.assertTrue(totalString>=1 && totalString<=2,"reward cocoon must drop 1-2 string, got "+totalString);
  var spiders=level.getEntitiesOfClass(net.minecraft.world.entity.monster.spider.Spider.class,new net.minecraft.world.phys.AABB(absPos).inflate(6));
  ctx.assertTrue(spiders.isEmpty(),"reward cocoon must not spawn spiders");
  ctx.succeed();
 }
 @GameTest public void cocoonSpawnVolumeClearsIntactCocoon(GameTestHelper ctx) {
  var level=ctx.getLevel(); var pos=ctx.absolutePos(new net.minecraft.core.BlockPos(3,4,3));
  level.setBlock(pos.above(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
  level.setBlock(pos,dev.itzgamesgd.moreminecrafts.registry.ModBlocks.HANGING_COCOON.defaultBlockState().setValue(HangingCocoonBlock.REWARD,false),3);
  var spider=net.minecraft.world.entity.EntityTypes.SPIDER.create(level,EntitySpawnReason.TRIGGERED);
  spider.setPos(pos.getX()+0.5,pos.getY()-0.5,pos.getZ()+0.5);
  boolean occupied=level.noCollision(spider);
  ctx.assertTrue(!occupied,"old spawn height collides with the intact cocoon");
  for(int x=-10;x<=10;x++) for(int z=-10;z<=10;z++) {
   spider.setPos(pos.getX()+0.5+x/10.0,pos.getY()-spider.getBbHeight(),pos.getZ()+0.5+z/10.0);
   ctx.assertTrue(level.noCollision(spider),"spawn volume below the cocoon must be clear at "+x+","+z);
  }
  System.out.println("COCOON_SPAWN_VOLUME old_collides=true lower_candidates_clear=441");
  spider.discard(); ctx.succeed();
 }
 @GameTest public void hangingCocoonHazardSpawnsVanillaSpiders(GameTestHelper ctx) {
  var level=ctx.getLevel();
  var ceiling=new net.minecraft.core.BlockPos(3,5,3);
  var pos=ceiling.below();
  ctx.setBlock(ceiling,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
  ctx.setBlock(pos,dev.itzgamesgd.moreminecrafts.registry.ModBlocks.HANGING_COCOON.defaultBlockState().setValue(HangingCocoonBlock.REWARD,false));
  var player=ctx.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
  var absPos=ctx.absolutePos(pos);
  var state=level.getBlockState(absPos);
  state.getBlock().playerWillDestroy(level,absPos,state,player);
  level.removeBlock(absPos,false);
  var vanillaSpider=net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("spider"));
  var spiders=level.getEntitiesOfClass(net.minecraft.world.entity.monster.spider.Spider.class,new net.minecraft.world.phys.AABB(absPos).inflate(6),e->e.getType()==vanillaSpider);
  ctx.assertTrue(spiders.size()>=1 && spiders.size()<=2,"hazard cocoon must spawn 1-2 vanilla spiders, got "+spiders.size());
  ctx.succeed();
 }
 @GameTest public void worldgenPlacementRulesAndLedgerPersistColony(GameTestHelper ctx) {
  var ledger=SpiderColonyEntities.data(ctx.getLevel());
  int rx=SpiderColonyPlacementRules.regionX(1000), rz=SpiderColonyPlacementRules.regionZ(1000);
  int target=SpiderColonyPlacementRules.targetColoniesForRegion(ctx.getLevel().getSeed(),rx,rz);
  ctx.assertTrue(target>=1 && target<=3,"1.5 km^2 region target must be 1..3, got "+target);
  int radius=SpiderColonyPlacementRules.nestRadiusForSite(ctx.getLevel().getSeed(),16000,-12,16000);
  ctx.assertTrue(radius>=128 && radius<=256,"nest radius must be 128..256, got "+radius);
  ledger.recordColony("gt-worldgen-colony",16000,-12,16000,radius,rx,rz);
  ctx.assertTrue(ledger.regionColonyCount(rx,rz)>=1,"region count must increment");
  ctx.assertTrue(SpiderColonyPlacementRules.overlapsAny(16050,16050,128,ledger.recordedColonies()),"overlapping nest must be rejected");
  ctx.assertTrue(!SpiderColonyPlacementRules.overlapsAny(16600,16600,128,ledger.recordedColonies()),"distant nest must be allowed");
  ctx.succeed();
 }
}
