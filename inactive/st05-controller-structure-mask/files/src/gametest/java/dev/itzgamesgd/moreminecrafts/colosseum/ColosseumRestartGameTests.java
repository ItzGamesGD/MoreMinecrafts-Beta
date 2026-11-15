package dev.itzgamesgd.moreminecrafts.colosseum;

import static dev.itzgamesgd.moreminecrafts.colosseum.ColosseumFixtures.bossVanishedEvents;
import static dev.itzgamesgd.moreminecrafts.colosseum.ColosseumFixtures.bosses;
import static dev.itzgamesgd.moreminecrafts.colosseum.ColosseumFixtures.buildFloor;
import static dev.itzgamesgd.moreminecrafts.colosseum.ColosseumFixtures.crystalEndings;
import static dev.itzgamesgd.moreminecrafts.colosseum.ColosseumFixtures.driveToBoss;
import static dev.itzgamesgd.moreminecrafts.colosseum.ColosseumFixtures.forceChunks;
import static dev.itzgamesgd.moreminecrafts.colosseum.ColosseumFixtures.keyCount;
import static dev.itzgamesgd.moreminecrafts.colosseum.ColosseumFixtures.keyItems;
import static dev.itzgamesgd.moreminecrafts.colosseum.ColosseumFixtures.killAll;
import static dev.itzgamesgd.moreminecrafts.colosseum.ColosseumFixtures.player;
import static dev.itzgamesgd.moreminecrafts.colosseum.ColosseumFixtures.shelter;
import static dev.itzgamesgd.moreminecrafts.colosseum.ColosseumFixtures.trialMobs;
import static dev.itzgamesgd.moreminecrafts.colosseum.ColosseumFixtures.waves;

import dev.itzgamesgd.moreminecrafts.colosseum.ColosseumTrialState.Allocation;
import dev.itzgamesgd.moreminecrafts.colosseum.ColosseumTrialState.AllocationStatus;
import dev.itzgamesgd.moreminecrafts.colosseum.ColosseumTrialState.Outcome;
import dev.itzgamesgd.moreminecrafts.colosseum.ColosseumTrialState.Phase;
import dev.itzgamesgd.moreminecrafts.colosseum.WaveRecord.KeyState;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Process-restart persistence, run in three server processes by the ST-05 restart workflow
 * ({@code -Dmoreminecrafts.st05.restart.phase=write|reload|verify -Dmoreminecrafts.st05.hold=true}). Inert in the
 * ordinary GameTest run.
 * <p>
 * Determinism: the controller is held from server start, so nothing ticks until a test releases it. Values saved before
 * the shutdown are written to a file in the world folder and compared <em>exactly</em> after the restart; no assertion
 * accepts "either outcome" or depends on how quickly the runner ticks.
 */
public final class ColosseumRestartGameTests {
    private static final UUID MAIN_PLAYER = UUID.fromString("5f05c0de-0000-4000-8000-000000000005");
    private static final UUID WARN_PLAYER = UUID.fromString("5f05c0de-0000-4000-8000-000000000006");
    private static final UUID BLOCK_PLAYER = UUID.fromString("5f05c0de-0000-4000-8000-000000000007");
    private static final UUID BOSS_PLAYER = UUID.fromString("5f05c0de-0000-4000-8000-000000000008");
    private static final UUID MOB_PLAYER = UUID.fromString("5f05c0de-0000-4000-8000-000000000009");

    private static final BlockPos MAIN = new BlockPos(-30_200, 200, -30_200);
    private static final BlockPos BLOCKED = new BlockPos(-30_600, 200, -30_200);
    private static final BlockPos WARNING = new BlockPos(-31_000, 200, -30_200);
    private static final BlockPos BOSS_ARENA = new BlockPos(-31_400, 200, -30_200);
    private static final BlockPos MOB_ARENA = new BlockPos(-31_800, 200, -30_200);
    private static final BlockPos BOSS_FAR = BOSS_ARENA.offset(0, 0, 4096);
    private static final BlockPos MOB_FAR = MOB_ARENA.offset(0, 0, 4096);

    // Helpers.

    private static String phase() {
        String phase = System.getProperty("moreminecrafts.st05.restart.phase");
        return phase == null || phase.isBlank() ? null : phase;
    }

    private static void requireHold(GameTestHelper ctx) {
        ctx.assertTrue(ColosseumRuntime.controllerHeld(),
                "the restart tests need the controller held from server start: run with -Dmoreminecrafts.st05.hold=true");
    }

    private static Path file(ServerLevel level, String name) {
        return level.getServer().getWorldPath(LevelResource.ROOT).resolve("st05-restart-" + name + ".properties");
    }

    private static Properties load(GameTestHelper ctx, String name) {
        Properties expect = new Properties();
        try (InputStream in = Files.newInputStream(file(ctx.getLevel(), name))) {
            expect.load(in);
        } catch (IOException e) {
            ctx.fail("missing restart expectations written by the previous phase: " + e);
        }
        return expect;
    }

    private static void save(GameTestHelper ctx, String name, Properties expect) {
        try (OutputStream out = Files.newOutputStream(file(ctx.getLevel(), name))) {
            expect.store(out, "ST-05 restart expectations");
        } catch (IOException e) {
            ctx.fail("cannot write restart expectations: " + e);
        }
    }

    /** Every persisted controller field that must come back unchanged, as one comparable string. */
    private static String exact(ColosseumTrialState s) {
        StringBuilder b = new StringBuilder();
        b.append(s.phase()).append('|').append(s.lastOutcome()).append('|').append(s.trialId()).append('|')
                .append(s.lastTrialId()).append('|').append(s.round()).append('|').append(s.trialRemaining()).append('|')
                .append(s.absenceTicks()).append('|').append(s.blockedTicks()).append('|').append(s.spawnBlocked()).append('|')
                .append(s.intermissionRemaining()).append('|').append(s.cooldownRemaining()).append('|')
                .append(s.lockedParticipants()).append('|').append(s.boss().started()).append('|').append(s.boss().entity()).append('|');
        for (Allocation a : s.allocations())
            b.append(a.index()).append(':').append(a.mob()).append(':').append(a.status()).append(':').append(a.entity())
                    .append(':').append(a.warningTicks()).append(':').append(a.requeues()).append(';');
        b.append("|waves=").append(s.ledger().waves().size()).append("|elig=").append(s.ledger().eligibility().size())
                .append("|released=").append(s.ledger().releasedTrials()).append("|vanish=").append(s.bossVanish().size())
                .append("|journal=").append(s.journal().entries().size());
        return b.toString();
    }

    private static ColosseumArena registerFresh(GameTestHelper ctx, BlockPos center, int floorRadius) {
        ServerLevel level = ctx.getLevel();
        ctx.assertTrue(ColosseumIntegration.arena(level, center) == null,
                "the write phase needs a fresh world; delete build/run/gameTest before rerunning locally");
        forceChunks(level, center, true);
        buildFloor(level, center, floorRadius, true);
        ColosseumArena arena = ColosseumIntegration.registerArena(level, ColosseumArenaGeometry.testFixture(center));
        ColosseumIntegration.recordStructureMask(level, arena, List.of()); // fixture has no source-template provenance
        return arena;
    }

    private static ColosseumArena existing(GameTestHelper ctx, BlockPos center) {
        ColosseumArena arena = ColosseumIntegration.arena(ctx.getLevel(), center);
        ctx.assertTrue(arena != null, "arena " + center.toShortString() + " was persisted");
        return arena;
    }

    private static ServerPlayer at(GameTestHelper ctx, UUID id, String name, BlockPos center) {
        return player(ctx, id, name, center.getX() + 0.5D, center.getY(), center.getZ() + 0.5D);
    }

    private static void saveWorld(GameTestHelper ctx, List<ServerPlayer> leaving) {
        ServerLevel level = ctx.getLevel();
        for (ServerPlayer p : leaving) level.getServer().getPlayerList().remove(p);
        level.getServer().saveEverything(true, true, true);
    }

    // 1. R1 state, spawn warnings and the Q110 clock across a restart.

    @GameTest(maxTicks = 1500)
    public void trialStateSurvivesProcessRestart(GameTestHelper ctx) {
        String phase = phase();
        if (phase == null) { ctx.succeed(); return; }
        requireHold(ctx);
        switch (phase) {
            case "write" -> mainWrite(ctx);
            case "reload" -> mainReload(ctx);
            case "verify" -> mainVerify(ctx);
            default -> ctx.fail("unknown phase " + phase);
        }
    }

    private static void mainWrite(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        ColosseumArena main = registerFresh(ctx, MAIN, 27);
        ColosseumArena blocked = registerFresh(ctx, BLOCKED, 10); // no annulus floor: every spawn position is invalid
        ColosseumArena warning = registerFresh(ctx, WARNING, 27);
        List<ServerPlayer> players = List.of(at(ctx, MAIN_PLAYER, "st05-main", MAIN), at(ctx, BLOCK_PLAYER, "st05-block", BLOCKED),
                at(ctx, WARN_PLAYER, "st05-warn", WARNING));
        AtomicInteger step = new AtomicInteger();
        ctx.onEachTick(() -> {
            if (step.get() != 0) return;
            step.set(1);
            // Main: R1 running with live mobs and exactly one valid kill.
            ColosseumIntegration.advanceController(level, MAIN, 1);
            ColosseumIntegration.advanceController(level, MAIN, 1);
            ColosseumIntegration.advanceController(level, MAIN, ColosseumRules.SPAWN_WARNING_TICKS + 1);
            ColosseumTrialState m = main.state();
            ctx.assertTrue(m.phase() == Phase.ROUND && m.count(AllocationStatus.WARNING) == 0 && m.count(AllocationStatus.ALIVE) >= 2,
                    "R1 is running with live mobs");
            trialMobs(level, main).get(0).kill(level);
            ctx.assertTrue(m.count(AllocationStatus.KILLED) == 1, "one valid kill");
            // Blocked: the Q110 clock at exactly 400 ticks.
            ColosseumIntegration.advanceController(level, BLOCKED, 1);
            ColosseumIntegration.advanceController(level, BLOCKED, 400);
            ctx.assertTrue(blocked.state().blockedTicks() == 400 && blocked.state().spawnBlocked(), "Q110 clock at 400, got " + blocked.state().blockedTicks());
            // Warning: spawn warnings interrupted at 15 ticks left.
            ColosseumIntegration.advanceController(level, WARNING, 1);
            ColosseumIntegration.advanceController(level, WARNING, 1);
            ColosseumIntegration.advanceController(level, WARNING, 5);
            ColosseumTrialState w = warning.state();
            ctx.assertTrue(w.count(AllocationStatus.WARNING) > 0, "warnings in progress");
            for (Allocation a : w.allocations())
                if (a.status() == AllocationStatus.WARNING) ctx.assertTrue(a.warningTicks() == ColosseumRules.SPAWN_WARNING_TICKS - 5,
                        "each warning has exactly 15 ticks left, got " + a.warningTicks());
            Properties expect = new Properties();
            expect.setProperty("main.exact", exact(m));
            expect.setProperty("blocked.exact", exact(blocked.state()));
            expect.setProperty("warning.exact", exact(w));
            expect.setProperty("warning.warned", String.valueOf(w.count(AllocationStatus.WARNING)));
            expect.setProperty("warning.alive", String.valueOf(w.count(AllocationStatus.ALIVE)));
            expect.setProperty("main.alive", String.valueOf(m.count(AllocationStatus.ALIVE)));
            expect.setProperty("main.trial", String.valueOf(m.trialId()));
            save(ctx, "main", expect);
            saveWorld(ctx, players);
            System.out.println("ST05_RESTART_WRITE");
            ctx.succeed();
        });
    }

    private static void mainReload(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        Properties expect = load(ctx, "main");
        ColosseumArena main = existing(ctx, MAIN), blocked = existing(ctx, BLOCKED), warning = existing(ctx, WARNING);
        // Nothing has ticked since the restart (held), so every value must equal what was saved, exactly.
        ctx.assertTrue(exact(main.state()).equals(expect.getProperty("main.exact")), "main state restored exactly:\n" + exact(main.state())
                + "\n" + expect.getProperty("main.exact"));
        ctx.assertTrue(exact(blocked.state()).equals(expect.getProperty("blocked.exact")), "blocked state restored exactly (Q110 clock 400)");
        ctx.assertTrue(exact(warning.state()).equals(expect.getProperty("warning.exact")), "interrupted warnings restored exactly");
        ctx.assertTrue(main.state().absenceTicks() == 0 && main.state().trialRemaining() > 0, "no offline time was consumed");
        for (BlockPos c : List.of(MAIN, BLOCKED, WARNING)) forceChunks(level, c, true);
        int expectedAlive = Integer.parseInt(expect.getProperty("main.alive"));
        int warned = Integer.parseInt(expect.getProperty("warning.warned"));
        int warnAlive = Integer.parseInt(expect.getProperty("warning.alive"));
        AtomicInteger step = new AtomicInteger();
        List<ServerPlayer> players = new ArrayList<>();
        ctx.onEachTick(() -> {
            switch (step.get()) {
                case 0 -> {
                    boolean loaded = level.areEntitiesLoaded(ChunkPos.pack(MAIN)) && level.areEntitiesLoaded(ChunkPos.pack(WARNING));
                    if (!loaded) return;
                    for (Allocation a : main.state().allocations())
                        if (a.status() == AllocationStatus.ALIVE && level.getEntity(a.entity()) == null) return; // still loading
                    ctx.assertTrue(trialMobs(level, main).size() == expectedAlive, "the same mobs came back and no duplicate, got "
                            + trialMobs(level, main).size() + " vs " + expectedAlive);
                    ctx.assertTrue(exact(main.state()).equals(expect.getProperty("main.exact")), "loading mobs changed no state");
                    // Everyone is back (same ids) BEFORE the controller is released: the rejoin is explicit, not a race.
                    players.add(at(ctx, MAIN_PLAYER, "st05-main", MAIN));
                    players.add(at(ctx, BLOCK_PLAYER, "st05-block", BLOCKED));
                    players.add(at(ctx, WARN_PLAYER, "st05-warn", WARNING));
                    ColosseumRuntime.setControllerHeld(false);
                    step.set(1);
                }
                case 1 -> {
                    ColosseumTrialState w = warning.state();
                    if (w.count(AllocationStatus.WARNING) > 0) return;
                    ctx.assertTrue(w.count(AllocationStatus.ALIVE) == warnAlive + warned && trialMobs(level, warning).size() == w.count(AllocationStatus.ALIVE),
                            "each interrupted warning spawned exactly once: " + w.count(AllocationStatus.ALIVE) + " alive vs " + (warnAlive + warned));
                    ctx.assertTrue(main.state().absenceTicks() == 0, "rejoined before the first tick: no absence was ever counted");
                    ctx.assertTrue(blocked.state().blockedTicks() >= 400 && blocked.state().phase() == Phase.ROUND, "the Q110 clock continued from 400");
                    killAll(level, main);
                    step.set(2);
                }
                case 2 -> {
                    ColosseumTrialState m = main.state();
                    if (m.phase() == Phase.ROUND) { killAll(level, main); return; }
                    ColosseumRuntime.setControllerHeld(true); // freeze every value for the final restart
                    UUID trial = m.trialId();
                    ctx.assertTrue(m.phase() == Phase.INTERMISSION && m.waveCleared(trial, 1), "R1 completed after the restart");
                    ctx.assertTrue(waves(main) == 1 && m.ledger().waves().size() == 1, "exactly once");
                    WaveRecord wave = m.ledger().wave(trial, 1);
                    ctx.assertTrue(wave.key().state() == KeyState.SPAWNED && keyItems(level, main).size() == 1, "one shared key issued once");
                    ctx.assertTrue(m.ledger().isEligible(trial, 1, MAIN_PLAYER), "Q114-C eligibility recorded for the returned participant");
                    Properties next = new Properties();
                    next.setProperty("main.exact", exact(m));
                    next.setProperty("blocked.exact", exact(blocked.state()));
                    next.setProperty("warning.exact", exact(warning.state()));
                    next.setProperty("main.trial", String.valueOf(trial));
                    next.setProperty("main.key", String.valueOf(wave.key().entity()));
                    save(ctx, "main", next);
                    saveWorld(ctx, players);
                    System.out.println("ST05_RESTART_RELOAD");
                    ctx.succeed();
                }
                default -> {}
            }
        });
    }

    private static void mainVerify(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        Properties expect = load(ctx, "main");
        ColosseumArena main = existing(ctx, MAIN), blocked = existing(ctx, BLOCKED), warning = existing(ctx, WARNING);
        ctx.assertTrue(exact(main.state()).equals(expect.getProperty("main.exact")), "main restored exactly:\n" + exact(main.state())
                + "\n" + expect.getProperty("main.exact"));
        ctx.assertTrue(exact(blocked.state()).equals(expect.getProperty("blocked.exact")), "blocked restored exactly");
        ctx.assertTrue(exact(warning.state()).equals(expect.getProperty("warning.exact")), "warning arena restored exactly");
        UUID trial = main.state().trialId();
        ctx.assertTrue(String.valueOf(trial).equals(expect.getProperty("main.trial")), "same trial");
        ctx.assertTrue(main.state().ledger().waves().size() == 1 && main.state().waveCleared(trial, 1), "the single payout record persisted");
        ctx.assertTrue(main.state().ledger().isEligible(trial, 1, MAIN_PLAYER), "eligibility persisted");
        ctx.assertTrue(waves(main) == 0, "no wave was announced again after the restart");
        WaveRecord wave = main.state().ledger().wave(trial, 1);
        ctx.assertTrue(String.valueOf(wave.key().entity()).equals(expect.getProperty("main.key")) && wave.key().issueCount() == 1,
                "the same key, issued once");
        forceChunks(level, MAIN, true);
        ctx.onEachTick(() -> {
            if (!level.areEntitiesLoaded(ChunkPos.pack(MAIN))) return;
            List<ItemEntity> keys = keyItems(level, main);
            if (keys.isEmpty() && level.getEntity(wave.key().entity()) == null) return;
            ctx.assertTrue(keys.size() == 1 && keys.get(0).getUUID().equals(wave.key().entity()), "exactly one key entity after two restarts, got " + keys.size());
            ctx.assertTrue(main.state().ledger().wave(trial, 1).key().state() == KeyState.CONFIRMED
                    || main.state().ledger().wave(trial, 1).key().state() == KeyState.SPAWNED, "key state kept");
            for (BlockPos c : List.of(MAIN, BLOCKED, WARNING)) {
                ColosseumIntegration.archiveAndUnregister(level, c);
                forceChunks(level, c, false);
            }
            System.out.println("ST05_RESTART_VERIFY");
            ctx.succeed();
        });
    }

    // 2. Q116/Q117/Q118-A: a timed-out boss and ordinary mob in unloaded chunks, and an unfinished restore, across restarts.

    @GameTest(maxTicks = 3000)
    public void timeoutCleanupSurvivesProcessRestart(GameTestHelper ctx) {
        String phase = phase();
        if (phase == null) { ctx.succeed(); return; }
        requireHold(ctx);
        switch (phase) {
            case "write" -> timeoutWrite(ctx);
            case "reload" -> timeoutReload(ctx);
            case "verify" -> timeoutVerify(ctx);
            default -> ctx.fail("unknown phase " + phase);
        }
    }

    private static final BlockPos[] JOURNAL = {BOSS_FAR.offset(1, 0, 0), BOSS_FAR.offset(2, 0, 0), BOSS_FAR.offset(3, 0, 0), BOSS_FAR.offset(4, 0, 0)};

    private static void timeoutWrite(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        ColosseumArena bossArena = registerFresh(ctx, BOSS_ARENA, 27);
        ColosseumArena mobArena = registerFresh(ctx, MOB_ARENA, 27);
        forceChunks(level, BOSS_FAR, true);
        forceChunks(level, MOB_FAR, true);
        shelter(level, BOSS_FAR);
        shelter(level, MOB_FAR);
        List<ServerPlayer> players = List.of(at(ctx, BOSS_PLAYER, "st05-boss", BOSS_ARENA), at(ctx, MOB_PLAYER, "st05-mob", MOB_ARENA));
        AtomicInteger step = new AtomicInteger();
        UUID[] parked = new UUID[1];
        UUID[] bossId = new UUID[1];
        // Ordinary-mob arena: R1 with live mobs, one of them parked in a far chunk.
        ColosseumIntegration.advanceController(level, MOB_ARENA, 1);
        ColosseumIntegration.advanceController(level, MOB_ARENA, 1);
        ColosseumIntegration.advanceController(level, MOB_ARENA, ColosseumRules.SPAWN_WARNING_TICKS + 1);
        driveToBoss(ctx, bossArena, () -> step.set(1));
        ctx.onEachTick(() -> {
            ColosseumTrialState bs = bossArena.state(), ms = mobArena.state();
            switch (step.get()) {
                case 0 -> {
                    // wait: the boss arena is still playing R1-R6
                }
                case 1 -> {
                    ctx.assertTrue(ms.phase() == Phase.ROUND && ms.count(AllocationStatus.ALIVE) >= 2, "mob arena has live mobs");
                    Mob mob = trialMobs(level, mobArena).get(0);
                    parked[0] = mob.getUUID();
                    mob.setPos(MOB_FAR.getX() + 0.5D, MOB_FAR.getY(), MOB_FAR.getZ() + 0.5D);
                    Mob boss = bosses(level, bossArena).get(0);
                    bossId[0] = boss.getUUID();
                    for (BlockPos p : JOURNAL) {
                        level.setBlockAndUpdate(p, Blocks.STONE.defaultBlockState());
                        ctx.assertTrue(ColosseumIntegration.journalBreak(level, bossArena, bs.trialId(), p, Blocks.STONE.defaultBlockState(), null)
                                == ColosseumIntegration.JournalResult.RECORDED, "journaled " + p);
                        level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
                    }
                    level.setBlockAndUpdate(JOURNAL[3], Blocks.OAK_PLANKS.defaultBlockState()); // a player builds on one broken spot
                    boss.setPos(BOSS_FAR.getX() + 0.5D, BOSS_FAR.getY(), BOSS_FAR.getZ() + 0.5D);
                    step.set(2);
                }
                case 2 -> {
                    ColosseumIntegration.advanceController(level, BOSS_ARENA, 1); // reconcile notes the new chunks
                    ColosseumIntegration.advanceController(level, MOB_ARENA, 1);
                    var parkedAlloc = ms.allocations().stream().filter(a -> parked[0].equals(a.entity())).findFirst().orElse(null);
                    if (bs.boss().chunk() != ChunkPos.pack(BOSS_FAR) || parkedAlloc == null || parkedAlloc.chunk() != ChunkPos.pack(MOB_FAR)) return;
                    forceChunks(level, BOSS_FAR, false);
                    forceChunks(level, MOB_FAR, false);
                    step.set(3);
                }
                case 3 -> {
                    if (level.areEntitiesLoaded(ChunkPos.pack(BOSS_FAR)) || level.areEntitiesLoaded(ChunkPos.pack(MOB_FAR))
                            || level.isLoaded(JOURNAL[0])) return;
                    bs.trialRemaining = 1;
                    ms.trialRemaining = 1;
                    ColosseumIntegration.advanceController(level, BOSS_ARENA, 1);
                    ColosseumIntegration.advanceController(level, MOB_ARENA, 1);
                    ctx.assertTrue(bs.lastOutcome() == Outcome.TIMED_OUT && ms.lastOutcome() == Outcome.TIMED_OUT, "both timed out");
                    ctx.assertTrue(bs.bossVanish().size() == 1 && bs.bossVanish().get(0).entity().equals(bossId[0]), "boss vanish pending in the unloaded chunk");
                    ctx.assertTrue(crystalEndings(bossArena) == 1 && bossVanishedEvents(bossArena) == 0, "ending played, boss not gone yet");
                    ctx.assertTrue(bs.journal().entries().size() == 4, "four unfinished restore entries");
                    ctx.assertTrue(bs.cooldownRemaining() == ColosseumRules.COOLDOWN_TICKS && ms.cooldownRemaining() == ColosseumRules.COOLDOWN_TICKS, "cooldowns");
                    ctx.assertTrue(ms.ledger().isReleased(ms.lastTrialId()) && bs.ledger().isReleased(bs.lastTrialId()), "both trials released");
                    Properties expect = new Properties();
                    expect.setProperty("boss.exact", exact(bs));
                    expect.setProperty("mob.exact", exact(ms));
                    expect.setProperty("boss.id", bossId[0].toString());
                    expect.setProperty("boss.trial", String.valueOf(bs.lastTrialId()));
                    expect.setProperty("mob.parked", parked[0].toString());
                    StringBuilder others = new StringBuilder();
                    for (UUID id : ms.ledger().releasedTrials().isEmpty() ? List.<UUID>of() : List.<UUID>of()) others.append(id);
                    expect.setProperty("keys", String.valueOf(bs.ledger().waves().stream().map(w -> String.valueOf(w.key().entity())).toList()));
                    save(ctx, "timeout", expect);
                    saveWorld(ctx, players);
                    System.out.println("ST05_RESTART_WRITE");
                    ctx.succeed();
                }
                default -> {}
            }
        });
    }

    private static void timeoutReload(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        Properties expect = load(ctx, "timeout");
        ColosseumArena bossArena = existing(ctx, BOSS_ARENA), mobArena = existing(ctx, MOB_ARENA);
        ColosseumTrialState bs = bossArena.state(), ms = mobArena.state();
        ctx.assertTrue(exact(bs).equals(expect.getProperty("boss.exact")), "boss arena restored exactly:\n" + exact(bs) + "\n" + expect.getProperty("boss.exact"));
        ctx.assertTrue(exact(ms).equals(expect.getProperty("mob.exact")), "mob arena restored exactly");
        ctx.assertTrue(bs.cooldownRemaining() == ColosseumRules.COOLDOWN_TICKS && bs.trialRemaining() == 0,
                "no offline time was consumed: cooldown still exactly 78,000");
        ctx.assertTrue(bs.bossVanish().size() == 1 && bs.journal().entries().size() == 4, "unfinished vanish and restore were persisted");
        UUID bossId = UUID.fromString(expect.getProperty("boss.id"));
        UUID parked = UUID.fromString(expect.getProperty("mob.parked"));
        UUID bossTrial = UUID.fromString(expect.getProperty("boss.trial"));
        for (BlockPos c : List.of(BOSS_ARENA, MOB_ARENA, BOSS_FAR, MOB_FAR)) forceChunks(level, c, true);
        AtomicInteger step = new AtomicInteger();
        ctx.onEachTick(() -> {
            switch (step.get()) {
                case 0 -> {
                    if (!level.areEntitiesLoaded(ChunkPos.pack(BOSS_FAR)) || !level.areEntitiesLoaded(ChunkPos.pack(MOB_FAR))
                            || !level.areEntitiesLoaded(ChunkPos.pack(BOSS_ARENA)) || !level.isLoaded(JOURNAL[0])) return;
                    Entity mob = level.getEntity(parked);
                    if (mob == null) return; // the released mob is loading
                    ctx.assertTrue(level.getEntity(bossId) == null, "the boss' stored copy was rejected on load");
                    ctx.assertTrue(bs.bossVanish().isEmpty() && bossVanishedEvents(bossArena) == 1, "the vanishing completed once");
                    ctx.assertTrue(ColosseumOwnership.tag(mob) == null && mob.isAlive(), "the released ordinary mob loaded as a plain external mob");
                    ctx.assertTrue(bs.journal().entries().size() == 4, "held: nothing restored yet");
                    ctx.assertTrue(keyCount(level, bossArena) == 6, "the six issued keys are all still there, no duplicates, got " + keyCount(level, bossArena));
                    for (WaveRecord w : bs.ledger().waves())
                        ctx.assertTrue(w.key().state() != KeyState.PENDING && w.key().state() != KeyState.NONE, "key state " + w.key().state());
                    ctx.assertTrue(crystalEndings(bossArena) == 0, "the ending is not replayed after a restart");
                    ColosseumRuntime.setControllerHeld(false);
                    step.set(1);
                }
                case 1 -> {
                    if (bs.journal().entries().size() != 1) return;
                    for (int i = 0; i < 3; i++) ctx.assertTrue(level.getBlockState(JOURNAL[i]).is(Blocks.STONE), "restored " + JOURNAL[i]);
                    ctx.assertTrue(level.getBlockState(JOURNAL[3]).is(Blocks.OAK_PLANKS), "Q118-A: the player's building survives the restart");
                    ctx.assertTrue(bs.journal().at(JOURNAL[3].asLong()).status() == RestoreJournal.Status.SKIPPED_CONFLICT,
                            "and the terminal conflict is recorded");
                    level.setBlockAndUpdate(JOURNAL[3], Blocks.AIR.defaultBlockState());
                    step.set(2);
                }
                case 2 -> {
                    if (bs.hasOpenWork()) return;
                    ColosseumRuntime.setControllerHeld(true); // freeze for the final restart
                    ctx.assertTrue(level.getBlockState(JOURNAL[3]).is(Blocks.AIR)
                            && bs.journal().at(JOURNAL[3].asLong()).status() == RestoreJournal.Status.SKIPPED_CONFLICT,
                            "after conflict, an emptied spot remains skipped and the history remains stored");
                    ctx.assertTrue(bs.cooldownRemaining() <= ColosseumRules.COOLDOWN_TICKS && bs.cooldownRemaining() > 0, "the cooldown is counting down");
                    Properties next = new Properties();
                    next.setProperty("boss.exact", exact(bs));
                    next.setProperty("mob.exact", exact(ms));
                    next.setProperty("boss.id", bossId.toString());
                    next.setProperty("mob.parked", parked.toString());
                    next.setProperty("boss.trial", bossTrial.toString());
                    save(ctx, "timeout", next);
                    saveWorld(ctx, List.of());
                    System.out.println("ST05_RESTART_RELOAD");
                    ctx.succeed();
                }
                default -> {}
            }
        });
    }

    private static void timeoutVerify(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        Properties expect = load(ctx, "timeout");
        ColosseumArena bossArena = existing(ctx, BOSS_ARENA), mobArena = existing(ctx, MOB_ARENA);
        ColosseumTrialState bs = bossArena.state();
        ctx.assertTrue(exact(bs).equals(expect.getProperty("boss.exact")), "boss arena restored exactly:\n" + exact(bs) + "\n" + expect.getProperty("boss.exact"));
        ctx.assertTrue(exact(mobArena.state()).equals(expect.getProperty("mob.exact")), "mob arena restored exactly");
        ctx.assertTrue(bs.bossVanish().isEmpty() && bs.journal().entries().size() == 1 && !bs.hasOpenWork()
                && bs.journal().at(JOURNAL[3].asLong()).status() == RestoreJournal.Status.SKIPPED_CONFLICT,
                "the terminal conflict stays stored without remaining open work");
        UUID bossId = UUID.fromString(expect.getProperty("boss.id"));
        UUID parked = UUID.fromString(expect.getProperty("mob.parked"));
        ctx.assertTrue(bs.ledger().isReleased(UUID.fromString(expect.getProperty("boss.trial"))), "the released-trial record is kept");
        ctx.assertTrue(bs.ledger().waves().size() == 6, "the six wave records are kept");
        for (BlockPos c : List.of(BOSS_ARENA, MOB_ARENA, BOSS_FAR, MOB_FAR)) forceChunks(level, c, true);
        ctx.onEachTick(() -> {
            if (!level.areEntitiesLoaded(ChunkPos.pack(BOSS_FAR)) || !level.areEntitiesLoaded(ChunkPos.pack(MOB_FAR))
                    || !level.areEntitiesLoaded(ChunkPos.pack(BOSS_ARENA)) || !level.isLoaded(JOURNAL[0])) return;
            Entity mob = level.getEntity(parked);
            if (mob == null) return;
            ctx.assertTrue(level.getEntity(bossId) == null && bosses(level, bossArena).isEmpty(), "the boss is gone for good");
            ctx.assertTrue(mob.isAlive() && ColosseumOwnership.tag(mob) == null, "the released mob is still a plain external mob");
            for (int i = 0; i < 3; i++) ctx.assertTrue(level.getBlockState(JOURNAL[i]).is(Blocks.STONE), "terrain stays restored at " + JOURNAL[i]);
            ctx.assertTrue(level.getBlockState(JOURNAL[3]).is(Blocks.AIR), "the terminal conflict is not revisited after restart");
            ctx.assertTrue(keyCount(level, bossArena) == 6, "six keys, no duplicates after two restarts, got " + keyCount(level, bossArena));
            ctx.assertTrue(bossVanishedEvents(bossArena) == 0 && crystalEndings(bossArena) == 0, "nothing was re-announced");
            mob.discard();
            for (ItemEntity key : level.getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(BOSS_ARENA).inflate(8.0D))) key.discard();
            for (BlockPos c : List.of(BOSS_ARENA, MOB_ARENA)) {
                ColosseumIntegration.archiveAndUnregister(level, c);
                forceChunks(level, c, false);
            }
            forceChunks(level, BOSS_FAR, false);
            forceChunks(level, MOB_FAR, false);
            System.out.println("ST05_RESTART_VERIFY");
            ctx.succeed();
        });
    }
}
