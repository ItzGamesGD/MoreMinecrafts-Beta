package dev.itzgamesgd.moreminecrafts.colosseum;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.IOException;
import java.nio.file.Files;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

/** Real-world-entity reconciliation for the independently saved reward journal. */
public final class ColosseumRewardGameTests {
    private static RewardDelivery plan(ColosseumGameTests.Arena a, int round, boolean ominous) {
        ArenaAttempt attempt=a.attempt();
        attempt.keysAwarded.add(round);
        RewardDelivery delivery=RewardDelivery.sharedKey(attempt.attemptId,round,ominous);
        attempt.rewardDeliveries.put(delivery.rewardId,delivery);
        return delivery;
    }

    private static ItemEntity item(ColosseumGameTests.Arena a,RewardDelivery delivery,boolean boundUuid) {
        boolean ominous=delivery.itemId.equals("minecraft:ominous_trial_key");
        ItemEntity entity=new ItemEntity(a.level,a.cx+.5,150.0,a.cz+.5,new ItemStack(ominous?Items.OMINOUS_TRIAL_KEY:Items.TRIAL_KEY,1));
        if(boundUuid)entity.setUUID(RewardDelivery.entityUuid(delivery.rewardId));
        entity.addTag(ColosseumRuntime.TAG_REWARD+delivery.rewardId);
        entity.addTag(ColosseumRuntime.TAG_ATTEMPT+a.attempt().attemptId);
        a.level.addFreshEntity(entity);
        return entity;
    }

    @GameTest(maxTicks=120)
    public void plannedSharedKeyIsInsertedOnceAndReceiptSurvivesRepeatReconcile(GameTestHelper ctx) {
        var a=new ColosseumGameTests.Arena(ctx,31,RoundPlan.Timing.APPROVED);var p=a.player("reward-plan");
        AtomicInteger ticks=new AtomicInteger();
        ctx.onEachTick(()->{
            if(ticks.incrementAndGet()==3)a.at(p,8,8);
            if(ticks.get()==50) {
                var reward=plan(a,1,false);
                ctx.assertTrue(ColosseumRuntime.reconcileRewards(a.level,a.site,a.ctl),"planned delivery is safely replayed");
                ctx.assertTrue(reward.state==RewardDelivery.State.DELIVERED,"receipt is separate from eligibility");
                ctx.assertTrue(!a.attempt().entitlements().contains(reward.rewardId) && !a.ctl.claims.contains(reward.rewardId),"shared payment receipt does not become personal eligibility or claim completion");
                int count=keys(a).size();
                ctx.assertTrue(count==1,"exactly one shared Trial Key entity exists");
                ColosseumRuntime.reconcileRewards(a.level,a.site,a.ctl);
                ctx.assertTrue(keys(a).size()==1,"same reward ID is idempotent");
                cleanup(a);ctx.succeed();
            }
        });
    }

    @GameTest(maxTicks=120)
    public void savedEntityCompletesAnInterruptedInsertWithoutCreatingAnother(GameTestHelper ctx) {
        var a=new ColosseumGameTests.Arena(ctx,32,RoundPlan.Timing.APPROVED);var p=a.player("reward-reload");
        AtomicInteger ticks=new AtomicInteger();
        ctx.onEachTick(()->{
            if(ticks.incrementAndGet()==3)a.at(p,8,8);
            if(ticks.get()==50) {
                var reward=plan(a,1,false);reward.begin(RewardDelivery.entityUuid(reward.rewardId));item(a,reward,true);
                ArenaRecovery.save(a.level.getServer(),a.ctl,false);
                a.level.getServer().saveEverything(true,true,true);
                ctx.assertTrue(ColosseumRuntime.reconcileRewards(a.level,a.site,a.ctl),"entity proves the prior insert completed");
                ctx.assertTrue(reward.state==RewardDelivery.State.DELIVERED,"controller receipt reconciled from world entity");
                ctx.assertTrue(keys(a).size()==1,"existing entity adopted, never duplicated");
                cleanup(a);ctx.succeed();
            }
        });
    }

    @GameTest(maxTicks=120)
    public void uncertainInsertWithoutEntityIsQuarantinedAndNotReissued(GameTestHelper ctx) {
        var a=new ColosseumGameTests.Arena(ctx,33,RoundPlan.Timing.APPROVED);var p=a.player("reward-uncertain");
        AtomicInteger ticks=new AtomicInteger();
        ctx.onEachTick(()->{
            if(ticks.incrementAndGet()==3)a.at(p,8,8);
            if(ticks.get()==50) {
                var reward=plan(a,1,false);reward.begin(RewardDelivery.entityUuid(reward.rewardId));
                ctx.assertTrue(!ColosseumRuntime.reconcileRewards(a.level,a.site,a.ctl),"missing entity after insertion began is uncertain");
                ctx.assertTrue(a.ctl.quarantined(),"ambiguous payment is isolated");
                ctx.assertTrue(keys(a).isEmpty(),"no unsupported regrant occurs");
                cleanup(a);ctx.succeed();
            }
        });
    }

    @GameTest(maxTicks=120)
    public void duplicateWorldEntitiesForOneRewardAreQuarantined(GameTestHelper ctx) {
        var a=new ColosseumGameTests.Arena(ctx,34,RoundPlan.Timing.APPROVED);var p=a.player("reward-duplicate");
        AtomicInteger ticks=new AtomicInteger();
        ctx.onEachTick(()->{
            if(ticks.incrementAndGet()==3)a.at(p,8,8);
            if(ticks.get()==50) {
                var reward=plan(a,7,true);reward.begin(RewardDelivery.entityUuid(reward.rewardId));
                item(a,reward,true);item(a,reward,false);
                ctx.assertTrue(!ColosseumRuntime.reconcileRewards(a.level,a.site,a.ctl),"duplicate entity evidence is ambiguous");
                ctx.assertTrue(a.ctl.quarantined(),"duplicate shared key identities quarantine the arena");
                ctx.assertTrue(keys(a).stream().mapToInt(e->e.getItem().getCount()).sum()==2,"reconciliation does not create a third key");
                cleanup(a);ctx.succeed();
            }
        });
    }

    @GameTest(maxTicks=160)
    public void sharedRewardPlanAndEntitySurviveThreeIndependentJvmProcesses(GameTestHelper ctx) throws Exception {
        String phase=System.getProperty("moreminecrafts.st05c.restart.phase","");
        if(phase.isEmpty()){ctx.succeed();return;}
        var a=new ColosseumGameTests.Arena(ctx,35,RoundPlan.Timing.APPROVED);
        java.nio.file.Path fixture=java.nio.file.Path.of(System.getProperty("moreminecrafts.st05c.restart.file",
                a.level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("st05c-reward-fixture.txt").toString()));
        AtomicInteger ticks=new AtomicInteger();
        ctx.onEachTick(()->{
            int t=ticks.incrementAndGet();
            if(phase.equals("write") && t==20) {
                UUID challenge=UUID.fromString("5a05c05b-0000-4000-8000-0000000000c1");
                ArenaAttempt attempt=new ArenaAttempt(challenge,77);attempt.phase=ArenaAttempt.Phase.ENDED;attempt.outcome=ArenaAttempt.Outcome.SUCCESS;
                attempt.round=1;attempt.keysAwarded.add(1);
                RewardDelivery delivery=RewardDelivery.sharedKey(challenge,1,false);attempt.rewardDeliveries.put(delivery.rewardId,delivery);
                a.ctl.attempt=attempt;a.ctl.lastOutcome=ArenaAttempt.Outcome.SUCCESS.name();
                if(!ArenaRecovery.save(a.level.getServer(),a.ctl,false))throw new AssertionError("planned reward journal not saved");
                ColosseumRuntime.data(a.level.getServer()).setDirty();
                try {Files.createDirectories(fixture.toAbsolutePath().getParent());Files.writeString(fixture,challenge+"\n"+delivery.rewardId+"\n");}
                catch(IOException e){throw new RuntimeException(e);}
                a.level.getServer().saveEverything(true,true,true);
                ctx.assertTrue(keys(a).isEmpty(),"first process leaves a durable plan without creating its entity");
                System.out.println("ST05C_REWARD_WRITE challenge="+challenge+" reward="+delivery.rewardId);
                ctx.succeed();
            } else if((phase.equals("reconcile") || phase.equals("verify")) && t==40) {
                try {
                    var lines=Files.readAllLines(fixture);UUID challenge=UUID.fromString(lines.get(0));String rewardId=lines.get(1);
                    ArenaAttempt attempt=a.ctl.attempt();
                    ctx.assertTrue(attempt!=null && attempt.attemptId.equals(challenge),"same challenge ID after process restart");
                    ctx.assertTrue(attempt.keysAwarded.contains(1),"existing key history remains intact");
                    ctx.assertTrue(attempt.outcome()==ArenaAttempt.Outcome.SUCCESS && a.ctl.lastOutcome().equals("SUCCESS"),"successful terminal state is preserved");
                    RewardDelivery delivery=attempt.rewardDeliveries.get(rewardId);
                    ctx.assertTrue(delivery!=null,"same reward ID survives restart");
                    if(phase.equals("reconcile")) {
                        ctx.assertTrue(delivery.state==RewardDelivery.State.DELIVERED,"planned payment reconciles after restart");
                        ctx.assertTrue(keys(a).size()==1,"recovery inserts exactly one entity");
                        a.level.getServer().saveEverything(true,true,true);
                        System.out.println("ST05C_REWARD_RECONCILE challenge="+challenge+" reward="+rewardId);
                    } else {
                        ctx.assertTrue(delivery.state==RewardDelivery.State.DELIVERED,"receipt survives a second restart");
                        ctx.assertTrue(keys(a).size()==1,"world contains exactly one shared key after second restart");
                        ctx.assertTrue(ColosseumRuntime.reconcileRewards(a.level,a.site,a.ctl),"repeat reconciliation is safe");
                        ctx.assertTrue(keys(a).size()==1,"second reconciliation does not duplicate");
                        System.out.println("ST05C_REWARD_VERIFY challenge="+challenge+" reward="+rewardId);
                    }
                    if(phase.equals("verify"))a.cleanup();
                    ctx.succeed();
                } catch(IOException e){throw new RuntimeException(e);}
            }
        });
    }

    private static java.util.List<ItemEntity> keys(ColosseumGameTests.Arena a) {
        return a.level.getEntitiesOfClass(ItemEntity.class,new AABB(a.cx-4,145,a.cz-4,a.cx+4,160,a.cz+4),
                e->e.entityTags().stream().anyMatch(t->t.startsWith(ColosseumRuntime.TAG_REWARD)));
    }

    private static void cleanup(ColosseumGameTests.Arena a) {
        for(ItemEntity entity:keys(a))entity.discard();
        a.cleanup();
    }
}
