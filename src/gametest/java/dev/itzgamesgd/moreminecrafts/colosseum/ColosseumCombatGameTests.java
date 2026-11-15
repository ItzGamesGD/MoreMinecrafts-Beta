package dev.itzgamesgd.moreminecrafts.colosseum;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/** Real 26.3 server entities, tickets, combat and persistence; embedded players are not live clients. */
public final class ColosseumCombatGameTests {
    @GameTest(maxTicks=4200)
    public void sixRoundsLeadToSingleBoundBossAndNormalDeathEndsTheAttempt(GameTestHelper ctx) {
        var a=new ColosseumGameTests.Arena(ctx,21,RoundPlan.Timing.APPROVED);
        ServerPlayer p=a.player("gladiator"); AtomicInteger ticks=new AtomicInteger();
        UUID[] bossId={null};
        ctx.onEachTick(()->{
            int t=ticks.incrementAndGet();
            if(t==3) a.at(p,8,8);
            ArenaAttempt at=a.attempt(); if(at==null) return;
            if(at.round()<7) { a.killMobs(); return; }
            if(at.ended()) {
                ctx.assertTrue(at.outcome()==ArenaAttempt.Outcome.SUCCESS,"normal boss death succeeds");
                ctx.assertTrue(at.keysAwarded().size()==7,"one key record per completed round");
                ctx.assertTrue(at.supplyChests.isEmpty(),"success cleans supplies");
                var keys=a.level.getEntitiesOfClass(ItemEntity.class,new AABB(a.cx-40,145,a.cz-40,a.cx+40,180,a.cz+40),i->i.getItem().is(Items.OMINOUS_TRIAL_KEY));
                ctx.assertTrue(keys.stream().mapToInt(i->i.getItem().getCount()).sum()==1,"one shared boss-foot key");
                a.cleanup(); ctx.succeed(); return;
            }
            if(at.bossEntity==null) return;
            var boss=a.level.getEntity(at.bossEntity);
            ctx.assertTrue(boss instanceof ImmortalGladiator,"R7 custom boss exists");
            if(bossId[0]==null) {
                bossId[0]=boss.getUUID();
                var gladiator=(ImmortalGladiator)boss;
                ctx.assertTrue(gladiator.getMaxHealth()==300,"solo HP");
                var restored=ArenaController.restore(a.id,a.ctl.toJson(),null,ColosseumData.clock.getAsLong());
                ctx.assertTrue(restored.attempt().bossEntity.equals(bossId[0]),"boss binding survives serialization");
                ColosseumBossHook.provider().start(a.level,a.site,at,300);
                ctx.assertTrue(a.level.getEntitiesOfClass(ImmortalGladiator.class,boss.getBoundingBox().inflate(40)).size()==1,"provider retry must not duplicate boss");
                boss.kill(a.level);
            }
        });
    }
    @GameTest(maxTicks=250)
    public void occupiedTerrainIsNeverOverwrittenAndConflictPersists(GameTestHelper ctx) {
        var a=new ColosseumGameTests.Arena(ctx,22,RoundPlan.Timing.APPROVED);
        var p=a.player("restore"); AtomicInteger ticks=new AtomicInteger();
        ctx.onEachTick(()->{
            int t=ticks.incrementAndGet(); if(t==3)a.at(p,8,8);
            if(t==40) {
                var at=a.attempt(); BlockPos empty=new BlockPos(a.cx+3,150,a.cz+3),occupied=empty.east();
                at.terrain.record(empty.asLong(),"minecraft:oak_log[axis=x]");
                at.terrain.record(occupied.asLong(),"minecraft:stone");
                a.level.setBlock(occupied,Blocks.DIAMOND_BLOCK.defaultBlockState(),18);
                at.cleanupPending=true; at.onPlayerDeath(p.getUUID());
            }
            if(t==70) {
                var at=a.attempt(); var empty=new BlockPos(a.cx+3,150,a.cz+3);
                ctx.assertTrue(a.level.getBlockState(empty).is(Blocks.OAK_LOG),"exact original restored");
                ctx.assertTrue(a.level.getBlockState(empty.east()).is(Blocks.DIAMOND_BLOCK),"player block preserved");
                ctx.assertTrue(at.terrain.conflicts.contains(empty.east().asLong()),"conflict recorded");
                var restored=ArenaController.restore(a.id,a.ctl.toJson(),null,ColosseumData.clock.getAsLong());
                ctx.assertTrue(restored.attempt().terrain.conflicts.contains(empty.east().asLong()),"conflict persists");
                a.cleanup();ctx.succeed();
            }
        });
    }
    @GameTest(maxTicks=1800)
    public void ownedTicketsHoldCombatThenUnloadReloadDoesNotReplenish(GameTestHelper ctx) {
        var a=new ColosseumGameTests.Arena(ctx,23,RoundPlan.Timing.APPROVED);
        ServerPlayer p=a.player("chunks");AtomicInteger ticks=new AtomicInteger();
        Set<UUID> ids=new HashSet<>();int[] phase={0};long[] chunk={0};
        ctx.onEachTick(()->{
            int t=ticks.incrementAndGet();if(t==3)a.at(p,5,5);
            if(t==60) {
                ctx.assertTrue(!a.mobs().isEmpty(),"spawned before unload test");
                for(var m:a.mobs())ids.add(m.getUUID());
                chunk[0]=net.minecraft.world.level.ChunkPos.pack(a.cx>>4,a.cz>>4);
                a.level.getServer().getPlayerList().remove(p);
                for(long packed:ArenaChunkTickets.region(a.site.geometry())) {var cp=net.minecraft.world.level.ChunkPos.unpack(packed);a.level.setChunkForced(cp.x(),cp.z(),false);}
            }
            if(t==120) {
                ctx.assertTrue(a.level.areEntitiesLoaded(chunk[0]),"production ticket holds entities with no nearby player/forced chunk");
                ctx.assertTrue(a.mobs().stream().allMatch(m->ColosseumRuntime.combatFrozen(m)),"absence freezes owned combat");
                a.level.getServer().saveEverything(true,true,true);
                ColosseumRuntime.data(a.level.getServer()).sites().remove(a.id); // Test harness withdraws runtime ownership to permit an actual unload.
                phase[0]=1;
            }
            if(phase[0]==1 && !a.level.areEntitiesLoaded(chunk[0])) {
                ctx.assertTrue(a.attempt().count(ArenaAttempt.AllocStatus.SPAWNED)==ids.size(),"unload is not a loss");
                ColosseumRuntime.data(a.level.getServer()).sites().put(a.id,a.site);phase[0]=2;
            } else if(phase[0]==2 && ColosseumRuntime.entitiesLoaded(a.level,a.site.geometry())) {
                ctx.assertTrue(!a.ctl.quarantined(),"consistent reload resumes");
                Set<UUID> after=new HashSet<>();for(var m:a.mobs())after.add(m.getUUID());
                ctx.assertTrue(after.equals(ids),"same entities, no duplicate replenishment after reload");
                a.cleanup();ctx.succeed();phase[0]=3;
            }
        });
    }
    @GameTest(maxTicks=200)
    public void duplicateWorldAllocationQuarantinesWithoutSpawnOrReward(GameTestHelper ctx) {
        var a=new ColosseumGameTests.Arena(ctx,24,RoundPlan.Timing.APPROVED);
        var p=a.player("duplicate");AtomicInteger ticks=new AtomicInteger();
        ctx.onEachTick(()->{
            int t=ticks.incrementAndGet();if(t==3)a.at(p,5,5);
            if(t==50) {
                var original=a.mobs().getFirst();
                var duplicate=original.getType().create(a.level,net.minecraft.world.entity.EntitySpawnReason.EVENT);
                duplicate.snapTo(original.getX()+2,original.getY(),original.getZ(),0,0);
                for(String tag:original.entityTags())duplicate.addTag(tag);
                a.level.addFreshEntity(duplicate);
            }
            if(t==70) {
                ctx.assertTrue(a.ctl.quarantined(),"duplicate must quarantine");
                ctx.assertTrue(a.attempt().keysAwarded().isEmpty(),"no payout for uncertain state");
                a.cleanup();ctx.succeed();
            }
        });
    }

    @GameTest(maxTicks=1600)
    public void bossAndCompletionSurviveThreeRealServerProcesses(GameTestHelper ctx) throws Exception {
        String phase=System.getProperty("moreminecrafts.st05b.restart.phase","");
        if(phase.isEmpty()) {ctx.succeed();return;}
        var a=new ColosseumGameTests.Arena(ctx,25,RoundPlan.Timing.APPROVED);
        UUID player=UUID.fromString("5a05b05b-0000-4000-8000-0000000000b1");
        java.nio.file.Path file=java.nio.file.Path.of(System.getProperty("moreminecrafts.st05b.restart.file",a.level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("st05b-restart-evidence.txt").toString()));
        AtomicInteger ticks=new AtomicInteger();int[] bossTicks={0};
        if(phase.equals("write")) {
            ServerPlayer p=a.player(player,"bossrestart");
            ctx.onEachTick(()->{
                int t=ticks.incrementAndGet();if(t==3)a.at(p,8,8);
                var at=a.attempt();if(at==null)return;
                if(at.round()<7) {a.killMobs();if(at.phase()==ArenaAttempt.Phase.INTERMISSION)at.intermissionLeft=1;return;}
                if(at.bossEntity==null)return;
                var boss=(ImmortalGladiator)a.level.getEntity(at.bossEntity);
                if(++bossTicks[0]==2)boss.setHealth(100);
                if(bossTicks[0]==80) {
                    ctx.assertTrue(boss.phaseTwo(),"phase two entered before shutdown");
                    BlockPos restore=new BlockPos(a.cx+4,150,a.cz+4);
                    at.terrain.record(restore.asLong(),"minecraft:oak_log[axis=z]");
                    at.terrain.record(restore.east().asLong(),"minecraft:stone");
                    a.level.setBlock(restore.east(),Blocks.DIAMOND_BLOCK.defaultBlockState(),18);
                    ArenaRecovery.save(a.level.getServer(),a.ctl,false);
                    var pursuit=boss.pursuitState();
                    ctx.assertTrue(pursuit.valid(),"pursuit auxiliaries are in range before restart");
                    try {java.nio.file.Files.createDirectories(file.toAbsolutePath().getParent());java.nio.file.Files.writeString(file,at.attemptId+"\n"+at.bossEntity+"\n"+boss.getHealth()+"\n"+pursuit.sample()+"\n"+Double.toHexString(pursuit.lastDistance())+"\n");}catch(Exception e){throw new RuntimeException(e);}
                    a.level.getServer().getPlayerList().remove(p);
                    a.level.getServer().saveEverything(true,true,true);
                    System.out.println("ST05B_RESTART_WRITE boss="+at.bossEntity+" hp="+boss.getHealth());ctx.succeed();
                }
            });
        } else {
            var lines=java.nio.file.Files.readAllLines(file);UUID attempt=UUID.fromString(lines.get(0)),bossId=UUID.fromString(lines.get(1));
            ServerPlayer[] p={null};
            ctx.onEachTick(()->{
                int t=ticks.incrementAndGet();var c=ColosseumRuntime.data(a.level.getServer()).controller(a.id);var at=c.attempt();
                ctx.assertTrue(!c.quarantined(),"restart not quarantined: "+c.quarantineReason());
                ctx.assertTrue(at!=null && at.attemptId.equals(attempt),"same attempt after real process restart");
                if(phase.equals("reload")) {
                    if(t==50) {
                        var boss=a.level.getEntity(bossId);
                        ctx.assertTrue(boss instanceof ImmortalGladiator,"saved custom boss reloaded");
                        var b=(ImmortalGladiator)boss;
                        ctx.assertTrue(b.getHealth()==Float.parseFloat(lines.get(2)) && b.phaseTwo(),"HP/phase preserved");
                        ctx.assertTrue(b.pursuitState().equals(new GladiatorPursuitState(Integer.parseInt(lines.get(3)),Double.valueOf(lines.get(4)))),"pursuit sample/distance persisted in NBT");
                        ctx.assertTrue(at.bossState.equals(b.combatSnapshot()),"controller snapshot and restored boss auxiliaries agree");
                        ctx.assertTrue(a.level.getEntitiesOfClass(ImmortalGladiator.class,b.getBoundingBox().inflate(64)).size()==1,"exactly one recovered boss");
                        p[0]=a.player(player,"bossrestart");a.at(p[0],8,8);
                    }
                    if(t==70)a.level.getEntity(bossId).kill(a.level);
                    if(t==90) {
                        ctx.assertTrue(at.outcome()==ArenaAttempt.Outcome.SUCCESS,"recovered boss death succeeds");
                        ctx.assertTrue(at.keysAwarded().size()==7,"one record per round after recovery");
                        BlockPos restore=new BlockPos(a.cx+4,150,a.cz+4);
                        ctx.assertTrue(a.level.getBlockState(restore).is(Blocks.OAK_LOG) && a.level.getBlockState(restore).getValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS)==net.minecraft.core.Direction.Axis.Z,"exact terrain state restored after restart");
                        ctx.assertTrue(a.level.getBlockState(restore.east()).is(Blocks.DIAMOND_BLOCK) && at.terrain.conflicts.contains(restore.east().asLong()),"player block protected and conflict persisted");
                        a.level.getServer().getPlayerList().remove(p[0]);a.level.getServer().saveEverything(true,true,true);
                        System.out.println("ST05B_RESTART_RELOAD success keys="+at.keysAwarded());ctx.succeed();
                    }
                } else if(phase.equals("verify") && t==60) {
                    ctx.assertTrue(at.outcome()==ArenaAttempt.Outcome.SUCCESS && at.keysAwarded().size()==7,"success/reward receipt survives second restart");
                    ctx.assertTrue(a.level.getEntity(bossId)==null,"defeated boss is never recreated");
                    BlockPos restore=new BlockPos(a.cx+4,150,a.cz+4);
                    ctx.assertTrue(a.level.getBlockState(restore).is(Blocks.OAK_LOG) && a.level.getBlockState(restore.east()).is(Blocks.DIAMOND_BLOCK),"restoration and player change survive second restart");
                    ctx.assertTrue(at.terrain.conflicts.contains(restore.east().asLong()),"terrain conflict survives second restart");
                    var keys=a.level.getEntitiesOfClass(ItemEntity.class,new AABB(a.cx-40,145,a.cz-40,a.cx+40,180,a.cz+40),e->e.getItem().is(Items.OMINOUS_TRIAL_KEY));
                    ctx.assertTrue(keys.stream().mapToInt(e->e.getItem().getCount()).sum()==1,"boss key is not duplicated by restart");
                    System.out.println("ST05B_RESTART_VERIFY success one boss key");a.cleanup();ctx.succeed();
                }
            });
        }
    }

    @GameTest(maxTicks=220)
    public void bossTimeoutVanishesWithoutSuccessOrKey(GameTestHelper ctx) {bossTermination(ctx,26,true);}
    @GameTest(maxTicks=220)
    public void bossWipeCleansWithoutSuccessOrKey(GameTestHelper ctx) {bossTermination(ctx,27,false);}
    private void bossTermination(GameTestHelper ctx,int index,boolean timeout) {
        var a=new ColosseumGameTests.Arena(ctx,index,RoundPlan.Timing.APPROVED);var clock=a.manualClock();var p=a.player("end"+index);
        AtomicInteger ticks=new AtomicInteger();UUID[] boss={null};
        ctx.onEachTick(()->{
            int t=ticks.incrementAndGet();if(t==3)a.at(p,8,8);
            if(t==40) {
                var at=a.attempt();for(var m:a.mobs())m.discard();at.allocs.clear();at.round=7;
                at.snap=new ArenaAttempt.Snapshot(7,1,1000,0,1,new ArrayList<>(at.rosterIds()));
            }
            if(t==80) {
                var at=a.attempt();boss[0]=at.bossEntity;
                ctx.assertTrue(boss[0]!=null && a.level.getEntity(boss[0])!=null,"boss exists before termination");
                a.assertOwnClock(ctx);
                if(timeout){a.ctl.setTiming(new RoundPlan.Timing(1,RoundPlan.GRACE_MILLIS));clock.advance(1);}else p.kill(a.level);
            }
            if(t==110) {
                ctx.assertTrue(a.attempt().outcome()==(timeout?ArenaAttempt.Outcome.TIMED_OUT:ArenaAttempt.Outcome.WIPE),"distinct terminal outcome");
                ctx.assertTrue(a.level.getEntity(boss[0])==null,"boss removed without death");
                ctx.assertTrue(a.attempt().keysAwarded().isEmpty(),"no success reward");
                ctx.assertTrue(!a.attempt().cleanupPending && a.ctl.cooldownLeft()>77000,"cleanup finished; cooldown active");
                a.cleanup();ctx.succeed();
            }
        });
    }

    @GameTest(maxTicks=180)
    public void inFlightOwnedArrowFreezesAndCannotDamageDuringAbsence(GameTestHelper ctx) {
        var a=new ColosseumGameTests.Arena(ctx,28,RoundPlan.Timing.APPROVED);var p=a.player("arrowpause");
        AtomicInteger ticks=new AtomicInteger();net.minecraft.world.entity.projectile.arrow.Arrow[] arrow={null};
        net.minecraft.world.entity.Mob[] owner={null};double[] x={0};int[] age={0};
        ctx.onEachTick(()->{
            int t=ticks.incrementAndGet();if(t==3)a.at(p,8,8);
            if(t==50) {
                owner[0]=a.mobs().getFirst();
                arrow[0]=new net.minecraft.world.entity.projectile.arrow.Arrow(a.level,owner[0],new net.minecraft.world.item.ItemStack(Items.ARROW),new net.minecraft.world.item.ItemStack(Items.BOW));
                arrow[0].snapTo(a.cx+8,153,a.cz+8,0,0);arrow[0].setNoGravity(true);arrow[0].setDeltaMovement(.02,0,0);a.level.addFreshEntity(arrow[0]);
                a.at(p,50,0);x[0]=arrow[0].getX();age[0]=arrow[0].tickCount;
            }
            if(t==75) {
                ctx.assertTrue(arrow[0].getX()==x[0] && arrow[0].tickCount==age[0],"in-flight arrow movement AND lifetime freeze");
                p.setPermanentlyInvulnerable(false);float hp=p.getHealth();
                boolean hurt=p.hurtServer(a.level,a.level.damageSources().arrow(arrow[0],owner[0]),4);
                ctx.assertTrue(!hurt && p.getHealth()==hp,"arena projectile cannot hurt participant during pause");
                a.at(p,8,8);
            }
            if(t==100) {
                ctx.assertTrue(arrow[0].tickCount>age[0] && arrow[0].getX()!=x[0],"flight resumes with remaining state");
                arrow[0].discard();a.cleanup();ctx.succeed();
            }
        });
    }

    @GameTest(maxTicks = 160)
    public void structureMaskDistinguishesPlayerPlacementsAndProtectsOriginal(GameTestHelper ctx) {
        var a = new ColosseumGameTests.Arena(ctx, 29, RoundPlan.Timing.APPROVED);
        BlockPos templatePos = new BlockPos(a.cx + 5, 150, a.cz + 5);
        BlockPos playerPos = new BlockPos(a.cx + 6, 150, a.cz + 6);
        a.level.setBlock(templatePos, Blocks.STONE_BRICKS.defaultBlockState(), 3);
        a.ctl.recordStructureMask(List.of(templatePos));

        ColosseumRuntime.onPlayerPlaced(a.level, playerPos);
        a.level.setBlock(playerPos, Blocks.COBBLESTONE.defaultBlockState(), 3);

        ctx.assertTrue(a.ctl.structureMask().complete(), "template occupancy recorded");
        ctx.assertTrue(a.ctl.structureMask().protects(templatePos), "template block protected");
        ctx.assertTrue(!a.ctl.structureMask().playerPlaced(templatePos), "template block not marked as player placed");
        ctx.assertTrue(a.ctl.structureMask().protects(playerPos), "player placed block protected");
        ctx.assertTrue(a.ctl.structureMask().playerPlaced(playerPos), "player placed block marked as player placed");

        // Player block break releases only player placement
        ColosseumRuntime.onPlayerBlockRemoved(a.level, playerPos);
        ctx.assertTrue(!a.ctl.structureMask().protects(playerPos), "player placement released upon break");
        ctx.assertTrue(a.ctl.structureMask().protects(templatePos), "template block remains protected");

        a.cleanup();
        ctx.succeed();
    }
}
