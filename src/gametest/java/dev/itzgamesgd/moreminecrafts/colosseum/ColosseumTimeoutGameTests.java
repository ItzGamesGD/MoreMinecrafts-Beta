package dev.itzgamesgd.moreminecrafts.colosseum;

import static dev.itzgamesgd.moreminecrafts.colosseum.ColosseumFixtures.arena;
import static dev.itzgamesgd.moreminecrafts.colosseum.ColosseumFixtures.bossVanishedEvents;
import static dev.itzgamesgd.moreminecrafts.colosseum.ColosseumFixtures.bosses;
import static dev.itzgamesgd.moreminecrafts.colosseum.ColosseumFixtures.crystalEndings;
import static dev.itzgamesgd.moreminecrafts.colosseum.ColosseumFixtures.driveToBoss;
import static dev.itzgamesgd.moreminecrafts.colosseum.ColosseumFixtures.finish;
import static dev.itzgamesgd.moreminecrafts.colosseum.ColosseumFixtures.forceChunks;
import static dev.itzgamesgd.moreminecrafts.colosseum.ColosseumFixtures.freshCenter;
import static dev.itzgamesgd.moreminecrafts.colosseum.ColosseumFixtures.items;
import static dev.itzgamesgd.moreminecrafts.colosseum.ColosseumFixtures.keyCount;
import static dev.itzgamesgd.moreminecrafts.colosseum.ColosseumFixtures.keyItems;
import static dev.itzgamesgd.moreminecrafts.colosseum.ColosseumFixtures.killAll;
import static dev.itzgamesgd.moreminecrafts.colosseum.ColosseumFixtures.moveTo;
import static dev.itzgamesgd.moreminecrafts.colosseum.ColosseumFixtures.playerAtCenter;
import static dev.itzgamesgd.moreminecrafts.colosseum.ColosseumFixtures.removals;
import static dev.itzgamesgd.moreminecrafts.colosseum.ColosseumFixtures.shelter;
import static dev.itzgamesgd.moreminecrafts.colosseum.ColosseumFixtures.trialMobs;
import static dev.itzgamesgd.moreminecrafts.colosseum.ColosseumFixtures.waves;

import dev.itzgamesgd.moreminecrafts.colosseum.ColosseumTrialState.AllocationStatus;
import dev.itzgamesgd.moreminecrafts.colosseum.ColosseumTrialState.Outcome;
import dev.itzgamesgd.moreminecrafts.colosseum.ColosseumTrialState.Phase;
import dev.itzgamesgd.moreminecrafts.colosseum.WaveRecord.KeyState;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/**
 * Live-world coverage of the shared key, the Q116 time-out, the Q117 vanishing boss (a tagged stub zombie stands in for
 * the Immortal Gladiator, which does not exist yet) and the Q91/Q118-A restore journal, on real entities, chunks and
 * blocks. The boss' own block breaking is simulated by the test calling the journal API and then removing the block.
 */
public final class ColosseumTimeoutGameTests {

    private static ServerPlayer presentPlayer(GameTestHelper ctx, BlockPos center, String name) {
        return playerAtCenter(ctx, center, name);
    }

    // Shared key.

    @GameTest(maxTicks = 500)
    public void sharedKeyIsOneVanillaCompatibleItemAtTheCenterAndPickupConsumesIt(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos center = freshCenter();
        ColosseumArena arena = arena(ctx, center, 27);
        ServerPlayer player = presentPlayer(ctx, center, "st05-key");
        AtomicInteger step = new AtomicInteger();
        ctx.onEachTick(() -> {
            ColosseumTrialState s = arena.state();
            switch (step.get()) {
                case 0 -> {
                    if (s.phase() != Phase.ROUND || s.count(AllocationStatus.ALIVE) == 0) return;
                    killAll(level, arena);
                    step.set(1);
                }
                case 1 -> {
                    if (s.phase() == Phase.ROUND) { killAll(level, arena); return; }
                    List<ItemEntity> keys = keyItems(level, arena);
                    ctx.assertTrue(keys.size() == 1, "exactly one shared key item, got " + keys.size());
                    ItemEntity key = keys.get(0);
                    ctx.assertTrue(ItemStack.isSameItemSameComponents(key.getItem(), new ItemStack(Items.TRIAL_KEY))
                            && key.getItem().getCount() == 1, "an unmarked vanilla Trial Key (so vanilla Vaults accept it)");
                    ctx.assertTrue(Math.abs(key.getX() - (center.getX() + 0.5D)) < 1.0D && Math.abs(key.getZ() - (center.getZ() + 0.5D)) < 1.0D,
                            "at the exact arena center");
                    WaveRecord wave = s.ledger().wave(s.trialId(), 1);
                    ctx.assertTrue(wave.key().state() == KeyState.SPAWNED && key.getUUID().equals(wave.key().entity())
                            && wave.key().issueCount() == 1, "recorded as issued once");
                    ctx.assertTrue(items(level, center) >= 1, "present in the world");
                    ColosseumIntegration.advanceController(level, center, 100);
                    ctx.assertTrue(keyItems(level, arena).size() == 1 && wave.key().issueCount() == 1, "idle time never adds a key");
                    key.discard(); // picked up (or merged, or despawned)
                    ctx.assertTrue(wave.key().state() == KeyState.CONSUMED, "a normal removal consumes the key, got " + wave.key().state());
                    step.set(2);
                }
                case 2 -> {
                    ColosseumIntegration.advanceController(level, center, 100);
                    ctx.assertTrue(keyItems(level, arena).isEmpty(), "a consumed key is never reissued");
                    ctx.assertTrue(s.ledger().wave(s.trialId(), 1).key().issueCount() == 1, "still issued exactly once");
                    finish(ctx, center, List.of(player));
                }
                default -> {}
            }
        });
    }

    @GameTest(maxTicks = 1800)
    public void theSharedKeySurvivesAChunkUnloadAsTheSameItemAndIsNotDuplicated(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos center = freshCenter();
        ColosseumArena arena = arena(ctx, center, 27);
        ServerPlayer player = presentPlayer(ctx, center, "st05-keyunload");
        long chunk = ChunkPos.pack(center);
        AtomicInteger step = new AtomicInteger();
        AtomicReference<UUID> keyId = new AtomicReference<>();
        ctx.onEachTick(() -> {
            ColosseumTrialState s = arena.state();
            switch (step.get()) {
                case 0 -> {
                    if (s.phase() != Phase.ROUND || s.count(AllocationStatus.ALIVE) == 0) return;
                    killAll(level, arena);
                    step.set(1);
                }
                case 1 -> {
                    if (s.phase() == Phase.ROUND) { killAll(level, arena); return; }
                    ctx.assertTrue(keyItems(level, arena).size() == 1, "key issued");
                    keyId.set(s.ledger().wave(s.trialId(), 1).key().entity());
                    moveTo(player, center.getX() + 6000.5D, center.getY(), center.getZ() + 0.5D);
                    forceChunks(level, center, false);
                    step.set(2);
                }
                case 2 -> {
                    if (level.areEntitiesLoaded(chunk)) return;
                    WaveRecord wave = s.ledger().wave(s.trialId(), 1);
                    ctx.assertTrue(wave.key().state() == KeyState.CONFIRMED, "stored to a chunk: confirmed, got " + wave.key().state());
                    forceChunks(level, center, true);
                    moveTo(player, center.getX() + 0.5D, center.getY(), center.getZ() + 0.5D);
                    step.set(3);
                }
                case 3 -> {
                    if (!level.areEntitiesLoaded(chunk) || level.getEntity(keyId.get()) == null) return;
                    ctx.assertTrue(keyItems(level, arena).size() == 1, "exactly one key after the reload (no duplicate)");
                    WaveRecord wave = s.ledger().wave(s.trialId(), 1);
                    ctx.assertTrue(wave.key().state() == KeyState.CONFIRMED && wave.key().issueCount() == 1,
                            "same key, never reissued, got " + wave.key().state() + "/" + wave.key().issueCount());
                    ColosseumIntegration.advanceController(level, center, 100);
                    ctx.assertTrue(keyItems(level, arena).size() == 1 && wave.key().issueCount() == 1, "still one key, issued once");
                    finish(ctx, center, List.of(player));
                }
                default -> {}
            }
        });
    }

    // Q116: ordinary mobs stay.

    @GameTest(maxTicks = 500)
    public void timeOutLeavesLiveMobsAndSuppliesAndRewardsNothing(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos center = freshCenter();
        ColosseumArena arena = arena(ctx, center, 27);
        ServerPlayer player = presentPlayer(ctx, center, "st05-timeout");
        AtomicInteger step = new AtomicInteger();
        AtomicReference<List<UUID>> mobIds = new AtomicReference<>();
        ctx.onEachTick(() -> {
            ColosseumTrialState s = arena.state();
            if (step.get() != 0 || s.phase() != Phase.ROUND || s.count(AllocationStatus.ALIVE) == 0 || s.count(AllocationStatus.WARNING) > 0) return;
            step.set(1);
            UUID trial = s.trialId();
            List<UUID> ids = trialMobs(level, arena).stream().map(Entity::getUUID).toList();
            mobIds.set(ids);
            int itemsBefore = items(level, center);
            s.trialRemaining = 2;
            ColosseumIntegration.advanceController(level, center, 2);
            ctx.assertTrue(s.phase() == Phase.COOLDOWN && s.lastOutcome() == Outcome.TIMED_OUT, "timed out, got " + s.lastOutcome());
            ctx.assertTrue(s.cooldownRemaining() == ColosseumRules.COOLDOWN_TICKS && s.trialRemaining() == 0, "separate timers");
            ctx.assertTrue(s.ledger().isReleased(trial), "released");
            for (UUID id : ids) {
                Entity mob = level.getEntity(id);
                ctx.assertTrue(mob != null && mob.isAlive(), "ordinary mob " + id + " is still alive after the time-out");
                ctx.assertTrue(ColosseumOwnership.tag(mob) == null, "and is now a plain external mob");
            }
            ctx.assertTrue(items(level, center) == itemsBefore, "nothing was dropped or cleaned up");
            ctx.assertTrue(s.bossVanish().isEmpty() && crystalEndings(arena) == 0, "no boss: no vanishing, no ending presentation");
            // Killing a leftover mob: vanilla result only, never a trial kill or reward.
            Mob victim = (Mob) level.getEntity(ids.get(0));
            victim.kill(level);
            ctx.assertTrue(waves(arena) == 0 && s.ledger().waves().isEmpty(), "no wave progress");
            ctx.assertTrue(keyItems(level, arena).isEmpty(), "no Trial Key");
            ctx.assertTrue(s.ledger().eligibility().isEmpty() && s.ledger().claims().isEmpty(), "no personal records");
            // The next trial does not count the leftovers.
            ColosseumIntegration.clearCooldown(level, center);
            ColosseumIntegration.advanceController(level, center, 2);
            ctx.assertTrue(s.phase() == Phase.ROUND && s.trialId() != null && !s.trialId().equals(trial), "a new trial can start");
            ctx.assertTrue(s.count(AllocationStatus.ALIVE) == 0, "leftovers are not this trial's mobs");
            ColosseumIntegration.forceReset(level, center);
            for (UUID id : ids) {
                Entity mob = level.getEntity(id);
                if (mob != null && mob != victim) mob.discard();
            }
            finish(ctx, center, List.of(player));
        });
    }

    @GameTest(maxTicks = 1800)
    public void aReleasedMobInAnUnloadedChunkLoadsLaterAsAnExternalMob(GameTestHelper ctx) {
        releasedMobInAnUnloadedChunk(ctx, false);
    }

    @GameTest(maxTicks = 1800)
    public void anArchivedArenaStillReleasesItsTimedOutMobsWhenTheyLoad(GameTestHelper ctx) {
        releasedMobInAnUnloadedChunk(ctx, true);
    }

    private static void releasedMobInAnUnloadedChunk(GameTestHelper ctx, boolean archiveBeforeReload) {
        ServerLevel level = ctx.getLevel();
        BlockPos center = freshCenter();
        ColosseumArena arena = arena(ctx, center, 27);
        ServerPlayer player = presentPlayer(ctx, center, "st05-released");
        BlockPos far = center.offset(0, 0, 4096);
        long farChunk = ChunkPos.pack(far);
        forceChunks(level, far, true);
        shelter(level, far);
        AtomicInteger step = new AtomicInteger();
        AtomicReference<UUID> parked = new AtomicReference<>();
        ctx.onEachTick(() -> {
            ColosseumTrialState s = arena.state();
            switch (step.get()) {
                case 0 -> {
                    if (s.phase() != Phase.ROUND || s.count(AllocationStatus.ALIVE) == 0 || s.count(AllocationStatus.WARNING) > 0) return;
                    Mob mob = trialMobs(level, arena).get(0);
                    parked.set(mob.getUUID());
                    mob.setPos(far.getX() + 0.5D, far.getY(), far.getZ() + 0.5D);
                    step.set(1);
                }
                case 1 -> {
                    var allocation = s.allocations().stream().filter(a -> parked.get().equals(a.entity())).findFirst().orElse(null);
                    if (allocation == null || allocation.chunk() != farChunk) return; // the controller noted the new chunk
                    forceChunks(level, far, false);
                    step.set(2);
                }
                case 2 -> {
                    if (level.areEntitiesLoaded(farChunk)) return;
                    var allocation = s.allocations().stream().filter(a -> parked.get().equals(a.entity())).findFirst().orElseThrow();
                    ctx.assertTrue(allocation.status() == AllocationStatus.ALIVE, "an unloaded mob is not lost (Q111), got " + allocation.status());
                    s.trialRemaining = 2;
                    ColosseumIntegration.advanceController(level, center, 2);
                    ctx.assertTrue(s.lastOutcome() == Outcome.TIMED_OUT && s.ledger().isReleased(s.lastTrialId()), "timed out and released");
                    if (archiveBeforeReload) {
                        // The operator archives the arena while the released mob is still in storage.
                        ctx.assertTrue(ColosseumIntegration.archiveAndUnregister(level, center) == ColosseumIntegration.UnregisterResult.REMOVED,
                                "archived");
                        ctx.assertTrue(ColosseumIntegration.arena(level, center) == null, "the arena is gone, only its archived ledger remains");
                    }
                    forceChunks(level, far, true);
                    step.set(3);
                }
                case 3 -> {
                    Entity mob = level.getEntity(parked.get());
                    if (!level.areEntitiesLoaded(farChunk) || mob == null) return;
                    ctx.assertTrue(mob.isAlive(), "the released mob loaded and lives");
                    ctx.assertTrue(ColosseumOwnership.tag(mob) == null, "as a plain external mob: its ownership tag was removed");
                    mob.discard();
                    forceChunks(level, far, false);
                    if (archiveBeforeReload) {
                        level.getServer().getPlayerList().remove(player);
                        forceChunks(level, center, false);
                        ctx.succeed();
                    } else {
                        finish(ctx, center, List.of(player));
                    }
                }
                default -> {}
            }
        });
    }

    // Q111: a converted mob is external.

    @GameTest(maxTicks = 500)
    public void aConvertedMobIsExternalAndItsAllocationGoesBackToPending(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos center = freshCenter();
        ColosseumArena arena = arena(ctx, center, 27);
        ServerPlayer player = presentPlayer(ctx, center, "st05-convert");
        AtomicInteger step = new AtomicInteger();
        ctx.onEachTick(() -> {
            ColosseumTrialState s = arena.state();
            if (step.get() != 0 || s.phase() != Phase.ROUND || s.count(AllocationStatus.ALIVE) == 0 || s.count(AllocationStatus.WARNING) > 0) return;
            step.set(1);
            Mob original = trialMobs(level, arena).get(0);
            var allocation = s.allocations().stream().filter(a -> original.getUUID().equals(a.entity())).findFirst().orElseThrow();
            int index = allocation.index();
            Mob converted = original.convertTo(net.minecraft.world.entity.EntityTypes.SILVERFISH,
                    net.minecraft.world.entity.ConversionParams.single(original, false, false), c -> { });
            ctx.assertTrue(converted != null && original.isRemoved(), "the original was replaced by the converted mob");
            ctx.assertTrue(ColosseumOwnership.tag(converted) == null, "the converted mob carries no ownership tag: it is external");
            var after = s.allocation(index);
            ctx.assertTrue(after.status() != AllocationStatus.ALIVE && after.status() != AllocationStatus.KILLED && after.requeues() == 1,
                    "the allocation returned to pending (Q111-A), got " + after.status() + " requeues=" + after.requeues());
            int killedBefore = s.count(AllocationStatus.KILLED);
            converted.kill(level);
            ctx.assertTrue(s.count(AllocationStatus.KILLED) == killedBefore, "killing the converted mob is not a trial kill");
            ctx.assertTrue(s.count(AllocationStatus.ALIVE) + s.count(AllocationStatus.WARNING) + s.count(AllocationStatus.PENDING)
                    + s.count(AllocationStatus.KILLED) == s.allocations().size(), "no allocation was lost or duplicated");
            finish(ctx, center, List.of(player));
        });
    }

    // Q117 + Q91 + Q118-A.

    @GameTest(maxTicks = 3000)
    public void aTimedOutBossVanishesWithoutAKillAndItsTerrainIsRestoredWithoutOverwritingPlayerBuilding(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos center = freshCenter();
        ColosseumArena arena = arena(ctx, center, 27);
        ServerPlayer player = presentPlayer(ctx, center, "st05-boss");
        BlockPos far = center.offset(0, 0, 4096);
        long farChunk = ChunkPos.pack(far);
        forceChunks(level, far, true);
        shelter(level, far);
        BlockPos b1 = far.offset(5, 0, 0), b2 = far.offset(6, 0, 0), b3 = far.offset(7, 0, 0), built = far.offset(8, 0, 0);
        AtomicInteger step = new AtomicInteger();
        AtomicReference<UUID> bossId = new AtomicReference<>();
        AtomicReference<Entity> bossRef = new AtomicReference<>();
        AtomicReference<UUID> trialRef = new AtomicReference<>();
        driveToBoss(ctx, arena, () -> step.set(1));
        ctx.onEachTick(() -> {
            ColosseumTrialState s = arena.state();
            switch (step.get()) {
                case 1 -> {
                    trialRef.set(s.trialId());
                    List<Mob> bs = bosses(level, arena);
                    ctx.assertTrue(bs.size() == 1, "the stub boss exists, got " + bs.size());
                    Mob boss = bs.get(0);
                    bossId.set(boss.getUUID());
                    bossRef.set(boss);
                    ctx.assertTrue(boss.getUUID().equals(s.boss().entity()), "bound to the boss record before it was added");
                    // The boss "breaks" four natural blocks: record first, then break (the order the boss must use).
                    for (BlockPos p : List.of(b1, b2, b3, built)) {
                        level.setBlockAndUpdate(p, Blocks.STONE.defaultBlockState());
                        var recorded = ColosseumIntegration.journalBreak(level, arena, trialRef.get(), p,
                                Blocks.STONE.defaultBlockState(), null);
                        ctx.assertTrue(recorded == ColosseumIntegration.JournalResult.RECORDED, "recorded " + p + ": " + recorded);
                        level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
                    }
                    level.setBlockAndUpdate(built, Blocks.OAK_PLANKS.defaultBlockState()); // a player builds on one broken spot
                    boss.setPos(far.getX() + 0.5D, far.getY(), far.getZ() + 0.5D);
                    step.set(2);
                }
                case 2 -> {
                    if (s.boss().chunk() != farChunk) return; // the controller followed the boss
                    forceChunks(level, far, false);
                    step.set(3);
                }
                case 3 -> {
                    if (level.areEntitiesLoaded(farChunk)) return;
                    step.set(30); // run this block once: a failed assertion must not be masked by a second pass
                    ctx.assertTrue(s.phase() == Phase.BOSS, "still in the boss phase before the limit runs out, got " + s.phase() + "/" + s.lastOutcome()
                            + " round=" + s.round() + " boss=" + s.boss().started() + " locked=" + s.lockedCount());
                    int keysBefore = keyCount(level, arena);
                    s.trialRemaining = 2;
                    ColosseumIntegration.advanceController(level, center, 2);
                    ctx.assertTrue(s.lastOutcome() == Outcome.TIMED_OUT && s.phase() == Phase.COOLDOWN, "timed out, got " + s.lastOutcome());
                    ctx.assertTrue(s.bossVanish().size() == 1, "vanishing is pending: the boss sits in an unloaded chunk");
                    ctx.assertTrue(crystalEndings(arena) == 1, "the server-side crystal ending fired once");
                    ctx.assertTrue(bossVanishedEvents(arena) == 0, "but the boss is not gone yet");
                    ctx.assertTrue(s.journal().entries().size() == 4, "four journal entries wait for their chunk");
                    ctx.assertTrue(keyCount(level, arena) == keysBefore && keysBefore == 6, "the six issued keys stay (stacks merge), got " + keysBefore);
                    ctx.assertTrue(waves(arena) == 6, "no seventh wave and no reward for the vanishing");
                    ctx.assertTrue(s.ledger().isReleased(trialRef.get()), "the trial was released");
                    forceChunks(level, far, true);
                    step.set(4);
                }
                case 4 -> {
                    if (!level.areEntitiesLoaded(farChunk) || !level.isLoaded(b1)) return;
                    ctx.assertTrue(level.getEntity(bossId.get()) == null, "the boss never loaded back: it was rejected");
                    ctx.assertTrue(bossRef.get().getRemovalReason() != Entity.RemovalReason.KILLED, "and did not die");
                    ctx.assertTrue(s.bossVanish().isEmpty() && bossVanishedEvents(arena) == 1, "the vanishing completed once");
                    ctx.assertTrue(removals(arena, "VANISHED") == 1, "the handler was told: vanished");
                    ctx.assertTrue(level.getEntitiesOfClass(ItemEntity.class, new AABB(far).inflate(20.0D)).isEmpty(), "no loot");
                    ctx.assertTrue(level.getEntitiesOfClass(ExperienceOrb.class, new AABB(far).inflate(20.0D)).isEmpty(), "no XP");
                    step.set(5);
                }
                case 5 -> {
                    ColosseumIntegration.advanceController(level, center, ColosseumRules.JOURNAL_PERIOD_TICKS + 1);
                    ctx.assertTrue(level.getBlockState(b1).is(Blocks.STONE) && level.getBlockState(b2).is(Blocks.STONE)
                            && level.getBlockState(b3).is(Blocks.STONE), "empty broken spots are restored to their exact prior state");
                    ctx.assertTrue(level.getBlockState(built).is(Blocks.OAK_PLANKS), "Q118-A: the player's building is untouched");
                    RestoreJournal.Entry conflict = s.journal().at(built.asLong());
                    ctx.assertTrue(s.journal().entries().size() == 1 && conflict != null
                            && conflict.status() == RestoreJournal.Status.SKIPPED_CONFLICT, "the terminal conflict is recorded");
                    ctx.assertTrue(ColosseumIntegration.unregisterArena(level, center)
                            == ColosseumIntegration.UnregisterResult.REFUSED_HAS_RECORDS, "the ledger, not a terminal conflict, blocks removal");
                    level.setBlockAndUpdate(built, Blocks.AIR.defaultBlockState()); // the player removes it
                    step.set(6);
                }
                case 6 -> {
                    ColosseumIntegration.advanceController(level, center, ColosseumRules.JOURNAL_PERIOD_TICKS + 1);
                    ctx.assertTrue(level.getBlockState(built).is(Blocks.AIR)
                            && s.journal().at(built.asLong()).status() == RestoreJournal.Status.SKIPPED_CONFLICT,
                            "a terminal conflict remains in history and is not re-evaluated");
                    for (ItemEntity key : level.getEntitiesOfClass(ItemEntity.class, new AABB(center).inflate(8.0D))) key.discard();
                    forceChunks(level, far, false);
                    finish(ctx, center, List.of(player));
                }
                default -> {}
            }
        });
    }

    @GameTest(maxTicks = 600)
    public void journalBreakRefusesProtectedBlocksAndWrongTrials(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos center = freshCenter();
        ColosseumArena arena = arena(ctx, center, 27);
        ServerPlayer player = presentPlayer(ctx, center, "st05-protect");
        AtomicInteger step = new AtomicInteger();
        ctx.onEachTick(() -> {
            ColosseumTrialState s = arena.state();
            if (step.get() != 0 || s.phase() != Phase.ROUND) return;
            step.set(1);
            UUID trial = s.trialId();
            BlockPos ok = center.offset(3, 1, 0), masked = center.offset(4, 1, 0), vault = center.offset(5, 1, 0),
                    chest = center.offset(6, 1, 0), bedrock = center.offset(7, 1, 0),
                    sourceBlock = center.offset(8, 1, 0), playerBlock = center.offset(9, 1, 0);
            level.setBlockAndUpdate(ok, Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(masked, Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(sourceBlock, Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(playerBlock, Blocks.STONE.defaultBlockState());
            ColosseumIntegration.recordStructureMask(level, arena, List.of(sourceBlock));
            ctx.assertTrue(ColosseumIntegration.protectArenaBlock(level, arena, playerBlock),
                    "a confirmed player placement is added to the durable protection mask");
            level.setBlockAndUpdate(vault, Blocks.VAULT.defaultBlockState());
            level.setBlockAndUpdate(chest, Blocks.CHEST.defaultBlockState());
            level.setBlockAndUpdate(bedrock, Blocks.BEDROCK.defaultBlockState());
            ColosseumFixtures.PROTECTED.add(masked.asLong());
            var stone = Blocks.STONE.defaultBlockState();
            ctx.assertTrue(ColosseumIntegration.journalBreak(level, arena, trial, ok, stone, null) == ColosseumIntegration.JournalResult.RECORDED, "an allowed block is recorded");
            ctx.assertTrue(ColosseumIntegration.journalBreak(level, arena, trial, ok, stone, null) == ColosseumIntegration.JournalResult.ALREADY_RECORDED,
                    "one open entry per position");
            ctx.assertTrue(ColosseumIntegration.journalBreak(level, arena, trial, masked, stone, null) == ColosseumIntegration.JournalResult.PROTECTED, "masked (structure) block");
            ctx.assertTrue(ColosseumIntegration.journalBreak(level, arena, trial, sourceBlock, stone, null)
                            == ColosseumIntegration.JournalResult.PROTECTED,
                    "original structure occupancy is protected by the persisted source mask");
            ctx.assertTrue(ColosseumIntegration.journalBreak(level, arena, trial, playerBlock, stone, null)
                            == ColosseumIntegration.JournalResult.PROTECTED,
                    "player-installed occupancy is protected by the persisted modification mask");
            ctx.assertTrue(ColosseumIntegration.journalBreak(level, arena, trial, vault, Blocks.VAULT.defaultBlockState(), null) == ColosseumIntegration.JournalResult.PROTECTED, "Vault");
            ctx.assertTrue(ColosseumIntegration.journalBreak(level, arena, trial, chest, Blocks.CHEST.defaultBlockState(), null) == ColosseumIntegration.JournalResult.PROTECTED, "block entity");
            ctx.assertTrue(ColosseumIntegration.journalBreak(level, arena, trial, bedrock, Blocks.BEDROCK.defaultBlockState(), null) == ColosseumIntegration.JournalResult.PROTECTED, "unbreakable");
            ctx.assertTrue(ColosseumIntegration.journalBreak(level, arena, UUID.randomUUID(), center.offset(8, 1, 0), stone, null) == ColosseumIntegration.JournalResult.NOT_ACTIVE, "another trial");
            ctx.assertTrue(s.journal().entries().size() == 1, "only the allowed block was recorded");
            ColosseumFixtures.PROTECTED.remove(masked.asLong());
            // An incomplete legacy/test registration cannot classify source positions by block type alone.
            ColosseumIntegration.setStructureMask(null);
            try {
                ColosseumArena incomplete = new ColosseumArena("incomplete", arena.geometry(), new ColosseumTrialState());
                ColosseumIntegration.journalBreak(level, incomplete, trial, ok, stone, null);
                ctx.fail("journalBreak must refuse to work without a structure mask");
            } catch (IllegalStateException expected) {
                // required behavior
            } finally {
                ColosseumIntegration.setStructureMask((l, a, p, st) -> ColosseumFixtures.PROTECTED.contains(p.asLong()));
            }
            ColosseumIntegration.forceReset(level, center);
            ColosseumIntegration.advanceController(level, center, ColosseumRules.JOURNAL_PERIOD_TICKS + 1);
            ctx.assertTrue(s.journal().isEmpty() || level.getBlockState(ok).is(Blocks.STONE), "the allowed entry resolved");
            finish(ctx, center, List.of(player));
        });
    }

    // D3.

    @GameTest(maxTicks = 500)
    public void unregisteringAnArenaWithRecordsOrOpenWorkIsRefusedAndArchivingKeepsTheHistory(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos center = freshCenter();
        ColosseumArena arena = arena(ctx, center, 27);
        ServerPlayer player = presentPlayer(ctx, center, "st05-unreg");
        String instance = arena.instanceId();
        AtomicInteger step = new AtomicInteger();
        ctx.onEachTick(() -> {
            ColosseumTrialState s = arena.state();
            switch (step.get()) {
                case 0 -> {
                    if (s.phase() != Phase.ROUND || s.count(AllocationStatus.ALIVE) == 0) return;
                    killAll(level, arena);
                    step.set(1);
                }
                case 1 -> {
                    if (s.phase() == Phase.ROUND) { killAll(level, arena); return; }
                    UUID trial = s.trialId();
                    ctx.assertTrue(ColosseumIntegration.unregisterArena(level, center) == ColosseumIntegration.UnregisterResult.REFUSED_HAS_RECORDS,
                            "an arena with ledger records cannot be unregistered");
                    ctx.assertTrue(ColosseumIntegration.arena(level, center) == arena && s.ledger().waves().size() == 1
                            && s.phase() == Phase.INTERMISSION, "and was left completely untouched (no reset either)");
                    ctx.assertTrue(ColosseumIntegration.archiveAndUnregister(level, center) == ColosseumIntegration.UnregisterResult.REMOVED, "archived and removed");
                    ctx.assertTrue(ColosseumIntegration.arena(level, center) == null, "removed");
                    ctx.assertTrue(ColosseumIntegration.data(level).archived().get(instance).hasWave(trial, 1), "its ledger was archived");
                    ColosseumArena again = ColosseumIntegration.registerArena(level, ColosseumArenaGeometry.testFixture(center), instance);
                    ctx.assertTrue(again.state().waveCleared(trial, 1) && ColosseumIntegration.data(level).archived().get(instance) == null,
                            "registering the same instance restores the history");
                    // Unfinished work blocks even the archive path.
                    again.state().journal().add(UUID.randomUUID(), 1L, new net.minecraft.nbt.CompoundTag(), null);
                    ctx.assertTrue(ColosseumIntegration.archiveAndUnregister(level, center) == ColosseumIntegration.UnregisterResult.REFUSED_OPEN_WORK, "an unfinished restore blocks archiving");
                    ctx.assertTrue(ColosseumIntegration.unregisterArena(level, center) == ColosseumIntegration.UnregisterResult.REFUSED_OPEN_WORK, "an unfinished restore blocks unregistering");
                    again.state().journal().remove(again.state().journal().at(1L));
                    BlockPos conflictCenter = center.offset(20, 0, 0);
                    ColosseumArena conflictOnly = ColosseumIntegration.registerArena(level,
                            ColosseumArenaGeometry.testFixture(conflictCenter));
                    conflictOnly.state().journal().add(UUID.randomUUID(), 2L, new net.minecraft.nbt.CompoundTag(), null);
                    conflictOnly.state().journal().at(2L).markSkippedConflict(123L, new net.minecraft.nbt.CompoundTag());
                    ctx.assertTrue(!conflictOnly.state().hasOpenWork(), "a retained terminal conflict is not unfinished work");
                    ctx.assertTrue(ColosseumIntegration.unregisterArena(level, conflictCenter)
                            == ColosseumIntegration.UnregisterResult.REMOVED,
                            "a conflict alone does not permanently block arena unregistering");
                    finish(ctx, center, List.of(player));
                }
                default -> {}
            }
        });
    }
}
