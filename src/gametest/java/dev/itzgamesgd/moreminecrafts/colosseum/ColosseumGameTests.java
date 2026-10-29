package dev.itzgamesgd.moreminecrafts.colosseum;

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
    private static final Map<Long, AtomicInteger> WAVES = new ConcurrentHashMap<>();
    private static final AtomicInteger NEXT_SLOT = new AtomicInteger();
    /** The GameTest world persists between runs; a per-run base keeps every run on untouched chunks. */
    private static final int RUN_BASE_X = 100_000 + java.util.concurrent.ThreadLocalRandom.current().nextInt(0, 20_000) * 1_000;

    static {
        ColosseumIntegration.WAVE_CLEARED.register((level, arena, record) ->
                WAVES.computeIfAbsent(arena.key(), key -> new AtomicInteger()).incrementAndGet());
    }

    // Fixture.

    /** A fresh arena center, unique per test, high above terrain. */
    private static BlockPos freshCenter() {
        int slot = NEXT_SLOT.getAndIncrement();
        return new BlockPos(RUN_BASE_X + slot * 160, 200, 24_000);
    }

    private static void forceChunks(ServerLevel level, BlockPos center, boolean forced) {
        for (int cx = (center.getX() - 32) >> 4; cx <= (center.getX() + 32) >> 4; cx++)
            for (int cz = (center.getZ() - 32) >> 4; cz <= (center.getZ() + 32) >> 4; cz++)
                level.setChunkForced(cx, cz, forced);
    }

    /** Smooth-stone floor (radius {@code floorRadius}, optionally only inside {@code innerOnly}), stone roof 6 up. */
    private static void buildFloor(ServerLevel level, BlockPos center, int floorRadius, boolean roof) {
        for (int dx = -27; dx <= 27; dx++)
            for (int dz = -27; dz <= 27; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 > 27 * 27) continue;
                level.setBlockAndUpdate(center.offset(dx, -1, dz), d2 <= floorRadius * floorRadius
                        ? Blocks.SMOOTH_STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
                if (roof) level.setBlockAndUpdate(center.offset(dx, 6, dz), Blocks.STONE.defaultBlockState());
                if (roof && d2 > 26 * 26) // perimeter wall keeps mobs on the platform (falls would count as kills)
                    for (int dy = 0; dy < 6; dy++) level.setBlockAndUpdate(center.offset(dx, dy, dz), Blocks.STONE.defaultBlockState());
            }
    }

    private static ColosseumArena arena(GameTestHelper ctx, BlockPos center, int floorRadius) {
        ServerLevel level = ctx.getLevel();
        forceChunks(level, center, true);
        buildFloor(level, center, floorRadius, true);
        ColosseumIntegration.unregisterArena(level, center);
        return ColosseumIntegration.registerArena(level, ColosseumArenaGeometry.testFixture(center));
    }

    private static ServerPlayer player(GameTestHelper ctx, String name, double x, double y, double z) {
        return player(ctx, UUID.randomUUID(), name, x, y, z);
    }

    private static ServerPlayer player(GameTestHelper ctx, UUID id, String name, double x, double y, double z) {
        var server = ctx.getLevel().getServer();
        var cookie = CommonListenerCookie.createInitial(new GameProfile(id, name), false);
        ServerPlayer player = new ServerPlayer(server, ctx.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        server.getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(GameType.SURVIVAL);
        player.getAbilities().invulnerable = true; // trial mobs fight; tests decide deaths explicitly
        player.teleportTo(ctx.getLevel(), x, y, z, Set.of(), 0.0F, 0.0F, false);
        return player;
    }

    private static ServerPlayer playerAtCenter(GameTestHelper ctx, BlockPos center, String name) {
        return player(ctx, name, center.getX() + 0.5D, center.getY(), center.getZ() + 0.5D);
    }

    private static void moveTo(ServerPlayer player, double x, double y, double z) {
        player.teleportTo((ServerLevel) player.level(), x, y, z, Set.of(), 0.0F, 0.0F, false);
    }

    private static void finish(GameTestHelper ctx, BlockPos center, List<ServerPlayer> players) {
        ServerLevel level = ctx.getLevel();
        ColosseumIntegration.unregisterArena(level, center);
        for (ServerPlayer player : players) level.getServer().getPlayerList().remove(player);
        forceChunks(level, center, false);
        ctx.succeed();
    }

    private static List<Mob> trialMobs(ServerLevel level, ColosseumArena arena) {
        BlockPos c = arena.geometry().center();
        return level.getEntitiesOfClass(Mob.class, new AABB(c).inflate(40.0D), mob -> {
            ColosseumOwnership.Tag tag = ColosseumOwnership.tag(mob);
            return mob.isAlive() && tag != null && tag.arena() == arena.key();
        });
    }

    private static void killAll(ServerLevel level, ColosseumArena arena) {
        for (Mob mob : trialMobs(level, arena)) mob.kill(level);
    }

    private static int items(ServerLevel level, BlockPos center) {
        return level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new AABB(center).inflate(30.0D)).size();
    }

    private static int waves(ColosseumArena arena) {
        AtomicInteger count = WAVES.get(arena.key());
        return count == null ? 0 : count.get();
    }

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
                    ctx.assertTrue(s.phase() == Phase.INTERMISSION && s.waveCleared(1), "R1 cleared");
                    ctx.assertTrue(waves(arena) == 1, "WAVE_CLEARED fired once, got " + waves(arena));
                    ctx.assertTrue(s.clearedWaves().get(0).lockedParticipants().equals(List.of(player.getUUID())),
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
                    ctx.assertTrue(s.waveCleared(1) && waves(arena) == 1, "the resumed round completes exactly once");
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

    /**
     * Process restart, run in three server processes by the ST-05 restart workflow
     * (-Dmoreminecrafts.st05.restart.phase=write|reload|verify). Inert in the ordinary GameTest run.
     */
    @GameTest(maxTicks = 1200)
    public void trialStateSurvivesProcessRestart(GameTestHelper ctx) {
        String phase = System.getProperty("moreminecrafts.st05.restart.phase");
        if (phase == null || phase.isBlank()) { ctx.succeed(); return; }
        ServerLevel level = ctx.getLevel();
        BlockPos center = new BlockPos(-30_200, 200, -30_200);
        BlockPos blockedCenter = new BlockPos(-30_600, 200, -30_200);
        BlockPos warningCenter = new BlockPos(-31_000, 200, -30_200);
        forceChunks(level, center, true);
        forceChunks(level, blockedCenter, true);
        forceChunks(level, warningCenter, true);
        switch (phase) {
            case "write" -> restartWrite(ctx, level, center, blockedCenter, warningCenter);
            case "reload" -> restartReload(ctx, level, center, blockedCenter, warningCenter);
            case "verify" -> restartVerify(ctx, level, center, blockedCenter, warningCenter);
            default -> ctx.fail("unknown phase " + phase);
        }
    }

    /** The same participant reconnects after the restart (a real player keeps their UUID). */
    private static final UUID RESTART_PLAYER = UUID.fromString("5f05c0de-0000-4000-8000-000000000005");

    private static final UUID RESTART_WARNING_PLAYER = UUID.fromString("5f05c0de-0000-4000-8000-000000000006");

    private static void restartWrite(GameTestHelper ctx, ServerLevel level, BlockPos center, BlockPos blockedCenter,
                                     BlockPos warningCenter) {
        ColosseumIntegration.unregisterArena(level, center); // a fresh start even if an earlier attempt left state
        ColosseumIntegration.unregisterArena(level, blockedCenter);
        ColosseumIntegration.unregisterArena(level, warningCenter);
        buildFloor(level, center, 27, true);
        buildFloor(level, blockedCenter, 10, true);
        buildFloor(level, warningCenter, 27, true);
        ColosseumArena arena = ColosseumIntegration.registerArena(level, ColosseumArenaGeometry.testFixture(center));
        ColosseumArena blocked = ColosseumIntegration.registerArena(level, ColosseumArenaGeometry.testFixture(blockedCenter));
        ColosseumArena warning = ColosseumIntegration.registerArena(level, ColosseumArenaGeometry.testFixture(warningCenter));
        ServerPlayer player = player(ctx, RESTART_PLAYER, "st05-restart", center.getX() + 0.5D, center.getY(), center.getZ() + 0.5D);
        ServerPlayer blockedPlayer = playerAtCenter(ctx, blockedCenter, "st05-restart-blocked");
        java.util.concurrent.atomic.AtomicReference<ServerPlayer> warningPlayer = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicBoolean done = new java.util.concurrent.atomic.AtomicBoolean();
        ctx.onEachTick(() -> {
            if (done.get()) return;
            ColosseumTrialState s = arena.state();
            if (s.phase() != Phase.ROUND || s.count(AllocationStatus.ALIVE) < 2 || s.count(AllocationStatus.WARNING) > 0) return;
            if (blocked.state().phase() != Phase.ROUND) return;
            if (warningPlayer.get() == null) { // start the third arena now so it is mid-warning at shutdown
                warningPlayer.set(player(ctx, RESTART_WARNING_PLAYER, "st05-restart-warn",
                        warningCenter.getX() + 0.5D, warningCenter.getY(), warningCenter.getZ() + 0.5D));
                return;
            }
            ColosseumTrialState w = warning.state();
            if (w.phase() != Phase.ROUND || w.count(AllocationStatus.WARNING) == 0) return;
            done.set(true);
            trialMobs(level, arena).get(0).kill(level); // a death just before shutdown
            ColosseumIntegration.advanceController(level, blockedCenter, 400); // 20 s of Q110 blockage
            System.out.println("ST05_RESTART_STATE trial=" + s.trialId() + " killed=" + s.count(AllocationStatus.KILLED)
                    + " alive=" + s.count(AllocationStatus.ALIVE) + " blocked=" + blocked.state().blockedTicks()
                    + " warning=" + w.count(AllocationStatus.WARNING) + " warningAlive=" + w.count(AllocationStatus.ALIVE));
            level.getServer().getPlayerList().remove(player);
            level.getServer().getPlayerList().remove(blockedPlayer);
            level.getServer().getPlayerList().remove(warningPlayer.get());
            level.getServer().saveEverything(true, true, true);
            System.out.println("ST05_RESTART_WRITE");
            ctx.succeed();
        });
    }

    private static void restartReload(GameTestHelper ctx, ServerLevel level, BlockPos center, BlockPos blockedCenter,
                                      BlockPos warningCenter) {
        ColosseumArena arena = ColosseumIntegration.arena(level, center);
        ColosseumArena blocked = ColosseumIntegration.arena(level, blockedCenter);
        ColosseumArena warning = ColosseumIntegration.arena(level, warningCenter);
        ctx.assertTrue(arena != null && blocked != null && warning != null, "all arenas persisted");
        ColosseumTrialState w = warning.state();
        int warned = w.count(AllocationStatus.WARNING), warnedAlive = w.count(AllocationStatus.ALIVE);
        ctx.assertTrue(w.phase() == Phase.ROUND && warned > 0, "the spawn warning persisted, got " + warned);
        ServerPlayer warnPlayer = player(ctx, RESTART_WARNING_PLAYER, "st05-restart-warn",
                warningCenter.getX() + 0.5D, warningCenter.getY(), warningCenter.getZ() + 0.5D);
        ColosseumTrialState s = arena.state();
        ctx.assertTrue(s.phase() == Phase.ROUND && s.trialId() != null && s.round() == 1,
                "R1 resumes after restart, got " + s.phase() + "/" + s.lastOutcome() + " absent=" + s.absenceTicks());
        ctx.assertTrue(s.count(AllocationStatus.KILLED) >= 1, "the pre-shutdown kill persisted");
        ctx.assertTrue(blocked.state().blockedTicks() >= 300 && blocked.state().spawnBlocked(),
                "the Q110 clock persisted, got " + blocked.state().blockedTicks());
        Set<UUID> expected = new HashSet<>();
        for (var a : s.allocations()) if (a.status() == AllocationStatus.ALIVE) expected.add(a.entity());
        int allocations = s.allocations().size();
        ctx.assertTrue(s.lockedParticipants().equals(List.of(RESTART_PLAYER)), "the locked participant persisted");
        ServerPlayer player = player(ctx, RESTART_PLAYER, "st05-restart", center.getX() + 0.5D, center.getY(), center.getZ() + 0.5D);
        AtomicInteger step = new AtomicInteger();
        ctx.onEachTick(() -> {
            switch (step.get()) {
                case 0 -> {
                    if (!level.areEntitiesLoaded(ChunkPos.pack(center))) return;
                    Set<UUID> loaded = new HashSet<>();
                    for (Mob mob : trialMobs(level, arena)) loaded.add(mob.getUUID());
                    if (!loaded.containsAll(expected)) return;
                    ctx.assertTrue(s.allocations().size() == allocations, "no allocation added or lost");
                    ctx.assertTrue(trialMobs(level, arena).size() <= s.count(AllocationStatus.ALIVE),
                            "no duplicate trial mob after restart");
                    step.set(1);
                }
                case 1 -> {
                    if (w.count(AllocationStatus.WARNING) > 0 || !level.areEntitiesLoaded(ChunkPos.pack(warningCenter))) return;
                    ctx.assertTrue(w.count(AllocationStatus.ALIVE) >= warnedAlive + warned,
                            "the warnings interrupted by the restart completed after it");
                    ctx.assertTrue(trialMobs(level, warning).size() == w.count(AllocationStatus.ALIVE),
                            "the interrupted warning spawned exactly once: " + trialMobs(level, warning).size()
                                    + " vs " + w.count(AllocationStatus.ALIVE));
                    ColosseumIntegration.unregisterArena(level, warningCenter);
                    level.getServer().getPlayerList().remove(warnPlayer);
                    step.set(2);
                }
                case 2 -> {
                    if (s.phase() == Phase.ROUND) { killAll(level, arena); return; }
                    ctx.assertTrue(s.waveCleared(1) && s.clearedWaves().size() == 1 && waves(arena) == 1,
                            "R1 completes once after the restart");
                    ColosseumIntegration.unregisterArena(level, blockedCenter);
                    level.getServer().getPlayerList().remove(player);
                    level.getServer().saveEverything(true, true, true);
                    System.out.println("ST05_RESTART_RELOAD");
                    ctx.succeed();
                }
                default -> {}
            }
        });
    }

    private static void restartVerify(GameTestHelper ctx, ServerLevel level, BlockPos center, BlockPos blockedCenter,
                                      BlockPos warningCenter) {
        ColosseumArena arena = ColosseumIntegration.arena(level, center);
        ctx.assertTrue(arena != null && ColosseumIntegration.arena(level, blockedCenter) == null
                && ColosseumIntegration.arena(level, warningCenter) == null, "registry persisted");
        ColosseumTrialState s = arena.state();
        ctx.assertTrue(s.waveCleared(1) && s.clearedWaves().size() == 1, "the R1 payout record persisted exactly once");
        // Nobody is present in this phase: the GameTest server runs ticks unpaced, so the 60 s absence grace may
        // legitimately expire before the test starts. Either the trial is still waiting, or it was reset by absence.
        ctx.assertTrue(s.phase() == Phase.INTERMISSION || s.phase() == Phase.ROUND
                        || (s.phase() == Phase.COOLDOWN && s.lastOutcome() == Outcome.FAILED_ABSENCE_RESET),
                "the trial continues or was reset by absence, got " + s.phase() + "/" + s.lastOutcome());
        System.out.println("ST05_RESTART_VERIFY_STATE phase=" + s.phase() + " outcome=" + s.lastOutcome()
                + " absent=" + s.absenceTicks());
        ctx.assertTrue(waves(arena) == 0, "no wave is re-announced after restart");
        ColosseumIntegration.unregisterArena(level, center);
        level.getServer().saveEverything(true, true, true);
        System.out.println("ST05_RESTART_VERIFY");
        ctx.succeed();
    }
}
