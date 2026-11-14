package dev.itzgamesgd.moreminecrafts.spidercolony;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.Attributes;
public final class SpiderColonyGameTests {
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
  ctx.runAfterDelay(310,()-> {
   int count=SpiderColonyEntities.data(level).childCount(mother.getUUID());
   ctx.assertTrue(count>0 && count<=2,"first cycle must create at most two juveniles; got "+count);
   ctx.runAfterDelay(320,()-> {
    int later=SpiderColonyEntities.data(level).childCount(mother.getUUID());
    ctx.assertTrue(later<=4,"simultaneous summon cap exceeded: "+later);
    mother.discard(); target.discard(); ctx.succeed();
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
  level.setChunkForced(chunk.x(),chunk.z(),true);
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
     level.setChunkForced(chunk.x(),chunk.z(),false);phase.set(1);
    }
    case 1 -> {
     if(level.areEntitiesLoaded(chunk.pack()))return;
     ctx.assertTrue(SpiderColonyEntities.data(level).childCount(motherId.get())==4,"unloaded juveniles must reserve all four slots");
     ctx.assertTrue(SummonPolicy.allowed(true,SpiderColonyEntities.data(level).childCount(motherId.get()))==0,"unload must not bypass cap");
     level.setChunkForced(chunk.x(),chunk.z(),true); phase.set(2);
    }
    case 2 -> {
     if(!level.areEntitiesLoaded(chunk.pack()))return;
     var loaded=level.getEntity(motherId.get());
     if(!(loaded instanceof MotherSpider mother))return;
     for(var id:children)if(level.getEntity(id)==null)return;
     ctx.assertTrue(mother.getHealth()==17.0F,"mother damage must survive chunk reload");
     ctx.assertTrue(SpiderColonyEntities.data(level).childCount(motherId.get())==4,"reload must retain cap");
     for(var id:children)level.getEntity(id).discard(); mother.discard();
     level.setChunkForced(chunk.x(),chunk.z(),false);phase.set(3);ctx.succeed();
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
}
