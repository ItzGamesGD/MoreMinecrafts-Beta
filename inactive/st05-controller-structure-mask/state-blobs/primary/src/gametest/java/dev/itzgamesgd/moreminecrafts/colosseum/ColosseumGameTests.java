package dev.itzgamesgd.moreminecrafts.colosseum;

import static dev.itzgamesgd.moreminecrafts.colosseum.ColosseumFixtures.arena;
import static dev.itzgamesgd.moreminecrafts.colosseum.ColosseumFixtures.finish;
import static dev.itzgamesgd.moreminecrafts.colosseum.ColosseumFixtures.freshCenter;
import static dev.itzgamesgd.moreminecrafts.colosseum.ColosseumFixtures.items;
import static dev.itzgamesgd.moreminecrafts.colosseum.ColosseumFixtures.killAll;
import static dev.itzgamesgd.moreminecrafts.colosseum.ColosseumFixtures.moveTo;
import static dev.itzgamesgd.moreminecrafts.colosseum.ColosseumFixtures.player;
import static dev.itzgamesgd.moreminecrafts.colosseum.ColosseumFixtures.playerAtCenter;
import static dev.itzgamesgd.moreminecrafts.colosseum.ColosseumFixtures.trialMobs;
import static dev.itzgamesgd.moreminecrafts.colosseum.ColosseumFixtures.waves;
import static dev.itzgamesgd.moreminecrafts.colosseum.ColosseumFixtures.buildFloor;
import static dev.itzgamesgd.moreminecrafts.colosseum.ColosseumFixtures.forceChunks;

import com.mojang.authlib.GameProfile;
import dev.itzgamesgd.moreminecrafts.colosseum.ColosseumTrialState.AllocationStatus;
import dev.itzgamesgd.moreminecrafts.colosseum.ColosseumTrialState.Outcome;
import dev.itzgamesgd.moreminecrafts.colosseum.ColosseumTrialState.Phase;
import dev.itzgamesgd.moreminecrafts.zombie.ZombieVariant;
import dev.itzgamesgd.moreminecrafts.zombie.ZombieVariantState;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;

/**
 * ST-05 controller against real entities, players, chunks and saved data, on flat roofed test floors built in the
 * sky (no natural colosseum structure). Each test owns an arena far from every other test.
 */
public final class ColosseumGameTests {
    // Tests.

    @GameTest(maxTicks = 400)
    public void centerApproachSpawnsExactTrialOwnedRosterOnTheAnnulus(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos center = freshCenter();
        ColosseumArena arena = arena(ctx, center, 27);
        ServerPlayer outside = player(ctx, "st05-out", center.getX() + 0.5D, center.getY(), center.getZ() + 6.5D);
        java.util.concurrent.atomic.AtomicBoolean once = new java.util.concurrent.atomic.AtomicBoolean();
        ctx.runAfterDelay(20, () -> {
            if (!once.compareAndSet(false, true)) return;
            ctx.assertTrue(arena.state().phase() == Phase.READY, "being in the zone but not the center does not start, got "
                    + arena.state().phase() + "/" + arena.state().lastOutcome() + " player at " + outside.position());
            ServerPlayer inside = playerAtCenter(ctx, center, "st05-center");
            ctx.onEachTick(() -> {
                ColosseumTrialState s = arena.state();
                if (s.phase() != Phase.ROUND || s.count(AllocationStatus.PENDING) > 0 || s.count(AllocationStatus.WARNING) > 0) return;
                ctx.assertTrue(s.lockedCount() == 2, "both zone players locked, got " + s.lockedCount());
                List<Mob> mobs = trialMobs(level, arena);
                ctx.assertTrue(mobs.size() == s.count(AllocationStatus.ALIVE) && mobs.size() == s.allocations().size(),
                        "every allocation materialized exactly once: " + mobs.size() + " vs " + s.allocations().size());
                Set<UUID> recorded = new HashSet<>();
                for (var a : s.allocations()) recorded.add(a.entity());
                for (Mob mob : mobs) {
                    ColosseumOwnership.Tag tag = ColosseumOwnership.tag(mob);
                    var allocation = s.allocation(tag.allocation());
                    ctx.assertTrue(recorded.contains(mob.getUUID()) && tag.trial().equals(s.trialId()), "owned by this trial");
                    ctx.assertTrue(mob.getType() == ColosseumServerPort.entityType(allocation.mob()),
                            "type matches allocation " + allocation.mob());
                    ctx.assertTrue(mob.isPersistenceRequired(), "trial mobs never despawn by distance");
                    double d = Math.sqrt(ColosseumArenaGeometry.horizontalDistanceSqr(center, mob.getX(), mob.getZ()));
                    ctx.assertTrue(mob.tickCount > 5 || (d >= 11.5D && d <= 24.6D), "spawned in the 12-24 annulus, d=" + d);
                    if (mob instanceof Zombie zombie) {
                        ctx.assertTrue(zombie.isBaby() == (allocation.mob() == ColosseumMob.BABY_ZOMBIE), "baby roll is exact");
                        ctx.assertTrue(((ZombieVariantState) zombie).moreminecrafts$getVariant() == ZombieVariant.NORMAL,
                                "conditional zombie variants stay inactive");
                        ctx.assertTrue(zombie.getVehicle() == null, "no jockeys");
                    }
                }
                finish(ctx, center, List.of(outside, inside));
            });
        });
    }

    @GameTest(maxTicks = 600)
    public void onlyTrialKillsCountAndEachWaveCompletesOnce(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos center = freshCenter();
        ColosseumArena arena = arena(ctx, center, 27);
        ServerPlayer player = playerAtCenter(ctx, center, "st05-kill");
        Zombie external = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
        external.snapTo(center.getX() + 4.5D, center.getY(), center.getZ() + 0.5D, 0.0F, 0.0F);
        external.setPersistenceRequired();
        level.addFreshEntity(external);
        AtomicInteger step = new AtomicInteger();
        ctx.onEachTick(() -> {
            ColosseumTrialState s = arena.state();
            switch (step.get()) {
                case 0 -> {
                    if (s.phase() != Phase.ROUND || s.count(AllocationStatus.ALIVE) == 0) return;
                    external.kill(level);
                    ctx.assertTrue(s.count(AllocationStatus.KILLED) == 0, "an external kill never counts");
                    killAll(level, arena);
                    step.set(1);
                }
                case 1 -> {
                    if (s.phase() == Phase.ROUND) { killAll(level, arena); return; }
                    ctx.assertTrue(s.phase() == Phase.INTERMISSION && s.waveCleared(s.trialId(), 1), "R1 cleared");
                    ctx.assertTrue(waves(arena) == 1, "WAVE_CLEARED fired once, got " + waves(arena));
                    ctx.assertTrue(s.ledger().waves().get(0).lockedParticipants().equals(List.of(player.getUUID())),
                            "payout record names the locked participant");
                    step.set(2);
                }
                case 2 -> {
                    if (s.intermissionRemaining() > 100) return;
                    ctx.assertTrue(waves(arena) == 1, "no second payout during the interval");
                    finish(ctx, center, List.of(player));
                }
                default -> {}
            }
        });
    }

    @GameTest(maxTicks = 1200)
    public void fourPlayersNeverExceedTwelveAliveAndRefillAtSix(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos center = freshCenter();
        ColosseumArena arena = arena(ctx, center, 27);
        List<ServerPlayer> players = new ArrayList<>();
        for (int i = 0; i < 4; i++)
            players.add(player(ctx, "st05-p" + i, center.getX() + 0.5D + (i % 2), center.getY(), center.getZ() + 0.5D + i / 2));
        AtomicInteger step = new AtomicInteger();
        AtomicInteger peak = new AtomicInteger();
        ctx.onEachTick(() -> {
            ColosseumTrialState s = arena.state();
            int alive = trialMobs(level, arena).size();
            peak.set(Math.max(peak.get(), alive));
            ctx.assertTrue(alive <= ColosseumRules.MAX_ALIVE, "never more than 12 alive, got " + alive);
            switch (step.get()) {
                case 0 -> {
                    if (s.phase() != Phase.ROUND || s.count(AllocationStatus.ALIVE) < 12) return;
                    ctx.assertTrue(s.lockedCount() == 4 && s.allocations().size() > 12,
                            "4 players: allocation " + s.allocations().size() + " exceeds the cap");
                    ctx.assertTrue(s.allocatedThreat() <= 56 && s.allocatedThreat() >= 48, "R1 x4 budget 56");
                    List<Mob> mobs = trialMobs(level, arena);
                    for (int i = 0; i < 5; i++) mobs.get(i).kill(level);
                    step.set(1);
                }
                case 1 -> {
                    if (s.count(AllocationStatus.KILLED) < 5) return;
                    ctx.runAfterDelay(50, () -> {
                        if (step.get() != 2) return;
                        ctx.assertTrue(arena.state().count(AllocationStatus.ALIVE) == 7
                                && arena.state().count(AllocationStatus.WARNING) == 0, "7 alive: no refill");
                        trialMobs(level, arena).get(0).kill(level);
                        step.set(3);
                    });
                    step.set(2);
                }
                case 3 -> {
                    if (s.count(AllocationStatus.ALIVE) + s.count(AllocationStatus.WARNING) < Math.min(12, s.allocations().size() - 6)) return;
                    ctx.assertTrue(s.count(AllocationStatus.KILLED) == 6, "six valid kills recorded");
                    ctx.assertTrue(peak.get() == 12, "peak 12");
                    finish(ctx, center, players);
                }
                default -> {}
            }
        });
    }

    @GameTest(maxTicks = 400)
    public void q110BlockedAnnulusFailsAfterSixtyActiveSecondsAndRecoveryResetsTheClock(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos center = freshCenter();
        ColosseumArena arena = arena(ctx, center, 10); // floor only inside radius 10: no annulus position is valid
        ServerPlayer player = playerAtCenter(ctx, center, "st05-q110");
        java.util.concurrent.atomic.AtomicBoolean once = new java.util.concurrent.atomic.AtomicBoolean();
        ctx.runAfterDelay(5, () -> {
            if (!once.compareAndSet(false, true)) return;
            ColosseumTrialState s = arena.state();
            ctx.assertTrue(s.phase() == Phase.ROUND && s.spawnBlocked(), "R1 started and is blocked");
            ColosseumIntegration.advanceController(level, center, ColosseumRules.SPAWN_BLOCKED_FAIL_TICKS - 40);
            ctx.assertTrue(s.phase() == Phase.ROUND, "not failed before 60 seconds");
            // Recovery: a valid floor appears; the clock resets on the next batch cycle.
            buildFloor(level, center, 27, false);
            ColosseumIntegration.advanceController(level, center, ColosseumRules.BATCH_CYCLE_TICKS + 1);
            ctx.assertTrue(s.blockedTicks() == 0 && !s.spawnBlocked() && s.count(AllocationStatus.WARNING) > 0,
                    "valid position resets the clock, blocked=" + s.blockedTicks());
            // Block again (warned mobs cannot materialize either) and let 60 active seconds pass.
            buildFloor(level, center, 10, false);
            ColosseumIntegration.advanceController(level, center, ColosseumRules.SPAWN_BLOCKED_FAIL_TICKS + 60);
            ctx.assertTrue(s.phase() == Phase.COOLDOWN && s.lastOutcome() == Outcome.FAILED_SPAWN_BLOCKED,
                    "Q110 failure, got " + s.phase() + "/" + s.lastOutcome());
            ctx.assertTrue(s.cooldownRemaining() > ColosseumRules.COOLDOWN_TICKS - 100, "78,000-tick cooldown applied");
            ctx.assertTrue(trialMobs(level, arena).isEmpty(), "no trial mobs remain");
            finish(ctx, center, List.of(player));
        });
    }

    @GameTest(maxTicks = 400)
    public void absenceGraceExpiryRemovesOnlyTrialMobsWithoutLoot(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos center = freshCenter();
        ColosseumArena arena = arena(ctx, center, 27);
        ServerPlayer player = playerAtCenter(ctx, center, "st05-away");
        Zombie external = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
        external.snapTo(center.getX() + 15.5D, center.getY(), center.getZ() + 0.5D, 0.0F, 0.0F);
        external.setPersistenceRequired();
        level.addFreshEntity(external);
        java.util.concurrent.atomic.AtomicBoolean done = new java.util.concurrent.atomic.AtomicBoolean();
        ctx.onEachTick(() -> {
            ColosseumTrialState s = arena.state();
            if (done.get() || s.phase() != Phase.ROUND || s.count(AllocationStatus.ALIVE) == 0 || s.count(AllocationStatus.WARNING) > 0) return;
            done.set(true);
            int alive = trialMobs(level, arena).size();
            int itemsBefore = items(level, center);
            moveTo(player, center.getX() + 60.5D, center.getY(), center.getZ() + 0.5D);
            ColosseumIntegration.advanceController(level, center, ColosseumRules.ABSENCE_GRACE_TICKS - 2);
            ctx.assertTrue(s.phase() == Phase.ROUND && s.paused(), "paused inside the grace period");
            ctx.assertTrue(trialMobs(level, arena).size() == alive, "existing mobs are kept during grace");
            ColosseumIntegration.advanceController(level, center, 2);
            ctx.assertTrue(s.phase() == Phase.COOLDOWN && s.lastOutcome() == Outcome.FAILED_ABSENCE_RESET, "grace expiry resets");
            ctx.assertTrue(trialMobs(level, arena).isEmpty(), "trial mobs removed");
            ctx.assertTrue(external.isAlive() && !external.isRemoved(), "external mob preserved");
            ctx.assertTrue(items(level, center) == itemsBefore, "removal drops no loot");
            ctx.assertTrue(s.cooldownRemaining() == ColosseumRules.COOLDOWN_TICKS, "cooldown starts at reset");
            ColosseumIntegration.advanceController(level, center, ColosseumRules.COOLDOWN_TICKS);
            ctx.assertTrue(s.phase() == Phase.READY, "READY after 78,000 ticks");
            external.discard();
            finish(ctx, center, List.of(player));
        });
    }

    @GameTest(maxTicks = 200)
    public void soloDeathIsTotalPartyDefeat(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos center = freshCenter();
        ColosseumArena arena = arena(ctx, center, 27);
        ServerPlayer player = playerAtCenter(ctx, center, "st05-dies");
        java.util.concurrent.atomic.AtomicBoolean done = new java.util.concurrent.atomic.AtomicBoolean();
        ctx.onEachTick(() -> {
            ColosseumTrialState s = arena.state();
            if (done.get() || s.phase() != Phase.ROUND || s.count(AllocationStatus.ALIVE) == 0) return;
            done.set(true);
            player.getAbilities().invulnerable = false;
            player.setHealth(0.0F);
            player.die(level.damageSources().genericKill()); // bypasses the new-player spawn invulnerability
            ctx.assertTrue(player.isDeadOrDying(), "the player died");
            java.util.concurrent.atomic.AtomicBoolean checked = new java.util.concurrent.atomic.AtomicBoolean();
            ctx.runAfterDelay(2, () -> {
                if (!checked.compareAndSet(false, true)) return;
                ctx.assertTrue(s.phase() == Phase.COOLDOWN && s.lastOutcome() == Outcome.FAILED_PARTY_DEFEAT,
                        "solo death fails the run, got " + s.lastOutcome());
                ctx.assertTrue(trialMobs(level, arena).isEmpty(), "trial mobs removed");
                finish(ctx, center, List.of(player));
            });
        });
    }

    @GameTest(maxTicks = 300)
    public void staleSavedCopyOfATrialMobIsRejectedOnLoad(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos center = freshCenter();
        ColosseumArena arena = arena(ctx, center, 27);
        ServerPlayer player = playerAtCenter(ctx, center, "st05-stale");
        java.util.concurrent.atomic.AtomicBoolean done = new java.util.concurrent.atomic.AtomicBoolean();
        ctx.onEachTick(() -> {
            ColosseumTrialState s = arena.state();
            if (done.get() || s.phase() != Phase.ROUND || s.count(AllocationStatus.ALIVE) == 0) return;
            done.set(true);
            Mob original = trialMobs(level, arena).get(0);
            TagValueOutput saved = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
            original.saveWithoutId(saved);
            EntityType<?> type = original.getType();
            int index = ColosseumOwnership.tag(original).allocation();
            original.kill(level); // counted
            ctx.assertTrue(s.allocation(index).status() == AllocationStatus.KILLED, "the original's kill counted");
            original.discard(); // let the stale copy reuse the id
            Entity copy = EntityType.create(type, TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(),
                    saved.buildResult()), level, EntitySpawnReason.LOAD).orElseThrow();
            ctx.assertTrue(ColosseumOwnership.tag(copy) != null, "the copy carries the ownership tag");
            level.addFreshEntity(copy);
            java.util.concurrent.atomic.AtomicBoolean checked = new java.util.concurrent.atomic.AtomicBoolean();
            ctx.runAfterDelay(2, () -> {
                if (!checked.compareAndSet(false, true)) return;
                ctx.assertTrue(level.getEntity(copy.getUUID()) == null, "the stale copy never enters the world");
                ctx.assertTrue(arena.state().allocation(index).status() == AllocationStatus.KILLED,
                        "and the allocation stays a single valid kill");
                finish(ctx, center, List.of(player));
            });
        });
    }

    @GameTest(maxTicks = 1600)
    public void chunkUnloadDuringARoundKeepsOwnershipWithoutDuplicates(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos center = freshCenter();
        ColosseumArena arena = arena(ctx, center, 27);
        ServerPlayer player = playerAtCenter(ctx, center, "st05-unload");
        AtomicInteger step = new AtomicInteger();
        AtomicReference<Set<UUID>> ids = new AtomicReference<>();
        ctx.onEachTick(() -> {
            ColosseumTrialState s = arena.state();
            switch (step.get()) {
                case 0 -> {
                    if (s.phase() != Phase.ROUND || s.count(AllocationStatus.ALIVE) == 0 || s.count(AllocationStatus.WARNING) > 0) return;
                    Set<UUID> alive = new HashSet<>();
                    for (var a : s.allocations()) if (a.status() == AllocationStatus.ALIVE) alive.add(a.entity());
                    ids.set(alive);
                    moveTo(player, center.getX() + 4000.5D, center.getY(), center.getZ() + 0.5D);
                    forceChunks(level, center, false);
                    step.set(1);
                }
                case 1 -> {
                    if (level.areEntitiesLoaded(ChunkPos.pack(center))) return;
                    ctx.assertTrue(s.paused() && s.phase() == Phase.ROUND, "empty arena pauses the round");
                    ctx.assertTrue(s.count(AllocationStatus.ALIVE) == ids.get().size(),
                            "unloaded mobs stay allocated ALIVE, never lost or respawned");
                    ctx.assertTrue(s.count(AllocationStatus.PENDING) + s.count(AllocationStatus.WARNING) == 0
                            || s.count(AllocationStatus.WARNING) == 0, "no new warnings while paused");
                    moveTo(player, center.getX() + 0.5D, center.getY(), center.getZ() + 0.5D);
                    forceChunks(level, center, true);
                    step.set(2);
                }
                case 2 -> {
                    if (!level.areEntitiesLoaded(ChunkPos.pack(center)) || trialMobs(level, arena).size() < ids.get().size()) return;
                    Set<UUID> reloaded = new HashSet<>();
                    for (Mob mob : trialMobs(level, arena)) reloaded.add(mob.getUUID());
                    ctx.assertTrue(reloaded.equals(ids.get()), "the same mobs came back, no duplicates");
                    ctx.assertTrue(!s.paused() && s.phase() == Phase.ROUND, "the round resumed within grace");
                    step.set(3);
                }
                case 3 -> {
                    if (s.phase() == Phase.ROUND) { killAll(level, arena); return; }
                    ctx.assertTrue(s.waveCleared(s.trialId(), 1) && waves(arena) == 1, "the resumed round completes exactly once");
                    finish(ctx, center, List.of(player));
                }
                default -> {}
            }
        });
    }

    @GameTest(maxTicks = 600)
    public void lateJoinerIsLockedOnlyFromTheNextRound(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos center = freshCenter();
        ColosseumArena arena = arena(ctx, center, 27);
        ServerPlayer first = playerAtCenter(ctx, center, "st05-first");
        AtomicReference<ServerPlayer> late = new AtomicReference<>();
        AtomicInteger step = new AtomicInteger();
        ctx.onEachTick(() -> {
            ColosseumTrialState s = arena.state();
            switch (step.get()) {
                case 0 -> {
                    if (s.phase() != Phase.ROUND || s.count(AllocationStatus.ALIVE) == 0) return;
                    late.set(player(ctx, "st05-late", center.getX() + 3.5D, center.getY(), center.getZ() + 0.5D));
                    step.set(1);
                }
                case 1 -> {
                    ctx.assertTrue(s.round() != 1 || s.lockedCount() == 1, "the R1 lock never changes mid-round");
                    if (s.phase() == Phase.ROUND) { killAll(level, arena); return; }
                    ColosseumIntegration.advanceController(level, center, s.intermissionRemaining());
                    ctx.assertTrue(s.round() == 2 && s.lockedCount() == 2, "R2 locks both players");
                    int budget = ColosseumRules.budget(2, 2);
                    ctx.assertTrue(s.allocatedThreat() <= budget && s.allocatedThreat() >= ColosseumRules.normalFillMin(budget),
                            "R2 uses the two-player budget");
                    finish(ctx, center, List.of(first, late.get()));
                }
                default -> {}
            }
        });
    }
}
