package dev.itzgamesgd.moreminecrafts.colosseum;

import com.mojang.authlib.GameProfile;
import dev.itzgamesgd.moreminecrafts.colosseum.ArenaAttempt.*;
import io.netty.channel.embedded.EmbeddedChannel;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/**
 * Stage 1-A integration on the real Minecraft 26.3 server runtime (the production tick, events and SavedData are exercised; players are
 * server-side {@link ServerPlayer}s with an embedded channel, not a real client). Each test owns a distinct far-away arena site.
 */
public final class ColosseumGameTests {
    private static final int FLOOR_Y = 150;

    /** A constructed-arena stand-in with the Stage 0-F interior geometry: flat floor radius 29, roofed so sunlight never burns trial mobs. */
    private static final class Arena {
        final ServerLevel level; final String id; final int cx, cz; final ColosseumData.Site site; final ArenaController ctl;
        final List<ServerPlayer> players = new ArrayList<>();

        Arena(GameTestHelper ctx, int idx, RoundPlan.Timing timing) {
            level = ctx.getLevel();
            cx = 6000 + idx * 200; cz = 6000; id = "gt" + idx;
            for (int x = (cx - 34) >> 4; x <= (cx + 34) >> 4; x++) for (int z = (cz - 34) >> 4; z <= (cz + 34) >> 4; z++) level.setChunkForced(x, z, true);
            for (int dx = -33; dx <= 33; dx++) for (int dz = -33; dz <= 33; dz++) {
                if (dx * dx + dz * dz > 33 * 33) continue;
                BlockPos f = new BlockPos(cx + dx, FLOOR_Y - 1, cz + dz);
                level.setBlock(f, Blocks.DEEPSLATE_BRICKS.defaultBlockState(), 18);
                for (int y = 0; y < 4; y++) level.setBlock(f.above(1 + y), Blocks.AIR.defaultBlockState(), 18);
                level.setBlock(new BlockPos(cx + dx, FLOOR_Y + 4, cz + dz), Blocks.DEEPSLATE_BRICKS.defaultBlockState(), 18);
            }
            ColosseumRuntime.registerArena(level.getServer(), id, cx, cz, FLOOR_Y);
            site = ColosseumRuntime.data(level.getServer()).sites().get(id);
            ctl = ColosseumRuntime.data(level.getServer()).controller(id);
            ctl.setTiming(timing);
        }

        ServerPlayer player(UUID uuid, String name) {
            var server = level.getServer();
            var cookie = CommonListenerCookie.createInitial(new GameProfile(uuid, name), false);
            ServerPlayer p = new ServerPlayer(server, level, cookie.gameProfile(), cookie.clientInformation());
            Connection connection = new Connection(PacketFlow.SERVERBOUND);
            new EmbeddedChannel(connection);
            server.getPlayerList().placeNewPlayer(connection, p, cookie);
            p.setGameMode(GameType.SURVIVAL);
            p.setPermanentlyInvulnerable(true);
            players.add(p);
            at(p, 60, 0);   // outside until a test moves it
            return p;
        }
        ServerPlayer player(String name) { return player(UUID.randomUUID(), name); }

        /** Teleports to horizontal offset (dx, dz) from the arena centre, standing on the floor. */
        void at(ServerPlayer p, double dx, double dz) { p.teleportTo(cx + 0.5 + dx, FLOOR_Y, cz + 0.5 + dz); }

        List<Mob> mobs() { return level.getEntitiesOfClass(Mob.class, new AABB(cx - 40, FLOOR_Y - 5, cz - 40, cx + 40, FLOOR_Y + 30, cz + 40),
                m -> m.entityTags().stream().anyMatch(t -> t.startsWith(ColosseumRuntime.TAG_ALLOC))); }

        void killMobs() { for (Mob m : mobs()) if (m.isAlive()) m.kill(level); }

        int keyItems() { return level.getEntitiesOfClass(ItemEntity.class, new AABB(cx - 3, FLOOR_Y - 1, cz - 3, cx + 3, FLOOR_Y + 3, cz + 3),
                i -> i.getItem().is(Items.TRIAL_KEY)).stream().mapToInt(i -> i.getItem().getCount()).sum(); }

        void cleanup() {
            for (ServerPlayer p : players) level.getServer().getPlayerList().remove(p);
            for (Mob m : mobs()) m.discard();
            for (int x = (cx - 34) >> 4; x <= (cx + 34) >> 4; x++) for (int z = (cz - 34) >> 4; z <= (cz + 34) >> 4; z++) level.setChunkForced(x, z, false);
        }
        ArenaAttempt attempt() { return ctl.attempt(); }
    }

    private static final RoundPlan.Timing LONG = RoundPlan.Timing.APPROVED;

    @GameTest(maxTicks = 300)
    public void entryIsDetectedOnlyInsideTheArenaInteriorAndStartsRoundOne(GameTestHelper ctx) {
        Arena a = new Arena(ctx, 1, LONG);
        ServerPlayer p = a.player("entry");
        AtomicInteger step = new AtomicInteger();
        ctx.onEachTick(() -> {
            int t = step.incrementAndGet();
            if (t == 3) { a.at(p, 40, 0); }          // corridor / natural cave outside the structure
            if (t == 8) { ctx.assertTrue(a.attempt() == null, "corridor position must not start anything"); a.at(p, 31, 0); }   // wall ring / doorway band
            if (t == 13) { ctx.assertTrue(a.attempt() == null, "wall ring must not start anything"); a.at(p, 0, 0); }          // arena centre (crystal is decoration, never a trigger)
            if (t == 18) {
                ctx.assertTrue(a.attempt() != null && a.attempt().round() == 1, "first entry starts round 1");
                ctx.assertTrue(a.attempt().rosterIds().equals(java.util.Set.of(p.getUUID())), "entrant is on the roster");
            }
            if (t == 120) {
                long spawned = a.attempt().count(AllocStatus.SPAWNED);
                ctx.assertTrue(spawned > 0 && spawned <= 12, "mobs spawned after the 1 s warning, within the 12-alive cap: " + spawned);
                for (Mob m : a.mobs()) {
                    double r = Math.hypot(m.getX() - (a.cx + 0.5), m.getZ() - (a.cz + 0.5));
                    ctx.assertTrue(r >= 11.0 && r <= 25.5, "spawn lies in the 12-24 annulus, was " + r);
                }
                ctx.assertTrue(a.mobs().size() == spawned, "exactly the persisted spawned allocations exist in the world");
                a.cleanup();
                ctx.succeed();
            }
        });
    }

    @GameTest(maxTicks = 120)
    public void twoPlayersEnteringInTheSameTickAreBothInTheFirstRoster(GameTestHelper ctx) {
        Arena a = new Arena(ctx, 2, LONG);
        ServerPlayer p1 = a.player("first"), p2 = a.player("second");
        AtomicInteger step = new AtomicInteger();
        ctx.onEachTick(() -> {
            int t = step.incrementAndGet();
            if (t == 3) { a.at(p2, 5, 5); a.at(p1, -5, 5); }   // same server tick, either order
            if (t == 20) {
                ArenaAttempt at = a.attempt();
                ctx.assertTrue(at != null && at.rosterIds().size() == 2 && at.pendingIds().isEmpty(), "both in the first roster, none pending");
                ctx.assertTrue(at.snapshot().n() == 2 && at.snapshot().budget() == 32, "round 1 scaled for 2 players: " + at.snapshot());
                ctx.assertTrue(at.admissions.size() == 2, "exactly one admission per player");
                a.cleanup();
                ctx.succeed();
            }
        });
    }

    @GameTest(maxTicks = 900)
    public void entryDuringARoundIsPendingAndAdmittedAtTheNextRoundBoundary(GameTestHelper ctx) {
        Arena a = new Arena(ctx, 3, LONG);
        ServerPlayer p1 = a.player("early"), p2 = a.player("late");
        AtomicInteger step = new AtomicInteger();
        int[] r1Budget = new int[1];
        ctx.onEachTick(() -> {
            int t = step.incrementAndGet();
            if (t == 3) a.at(p1, 3, 3);
            if (t == 60) {
                ArenaAttempt at = a.attempt();
                ctx.assertTrue(at.round() == 1 && at.snapshot().n() == 1, "round 1 started with one participant");
                r1Budget[0] = at.snapshot().budget();
                a.at(p2, -4, 4);                                  // enters after round 1 began
            }
            if (t == 70) {
                ArenaAttempt at = a.attempt();
                ctx.assertTrue(at.pendingIds().contains(p2.getUUID()) && !at.rosterIds().contains(p2.getUUID()), "late entrant is pending only");
                ctx.assertTrue(at.snapshot().n() == 1 && at.snapshot().budget() == r1Budget[0] && at.snapshot().budget() == 20, "round 1 parameters are not changed retroactively");
            }
            if (t > 70 && t < 400 && a.attempt().round() == 1) a.killMobs();
            if (t == 420) {
                ArenaAttempt at = a.attempt();
                ctx.assertTrue(at.round() == 2 || at.phase() == Phase.INTERMISSION, "round 1 cleared, got round " + at.round() + " " + at.phase());
                ctx.assertTrue(at.keysAwarded().contains(1) && a.keyItems() == 1, "exactly one shared key for wave 1: " + a.keyItems());
                ctx.assertTrue(at.entitlements().stream().noneMatch(e -> e.contains(p2.getUUID().toString())), "no retroactive reward for the pending player");
            }
            if (t == 700) {
                ArenaAttempt at = a.attempt();
                ctx.assertTrue(at.round() == 2 && at.rosterIds().contains(p2.getUUID()), "admitted at the round 1 -> 2 boundary");
                ctx.assertTrue(at.member(p2.getUUID()).admittedRound == 2, "admission round recorded");
                ctx.assertTrue(at.snapshot().n() == 2 && at.snapshot().budget() == 32 && at.snapshot().multiplierMilli() == 1600, "round 2 recalculated: " + at.snapshot());
                ctx.assertTrue(at.pendingIds().isEmpty(), "pending list consumed");
                long spawnedAndPending = at.allocations().size();
                ctx.assertTrue(spawnedAndPending > 0, "round 2 spawn plan exists");
                a.cleanup();
                ctx.succeed();
            }
        });
    }

    @GameTest(maxTicks = 120)
    public void reenteringAnAdmittedParticipantIsAReturnNotANewAdmission(GameTestHelper ctx) {
        Arena a = new Arena(ctx, 4, LONG);
        ServerPlayer p1 = a.player("stay"), p2 = a.player("leaver");
        AtomicInteger step = new AtomicInteger();
        ctx.onEachTick(() -> {
            int t = step.incrementAndGet();
            if (t == 3) { a.at(p1, 2, 2); a.at(p2, -2, 2); }
            if (t == 20) a.at(p2, 45, 0);
            if (t == 30) a.at(p2, 8, 8);
            if (t == 50) {
                ArenaAttempt at = a.attempt();
                ctx.assertTrue(at.rosterIds().size() == 2 && at.pendingIds().isEmpty(), "still two members, nobody pending");
                ctx.assertTrue(at.member(p2.getUUID()).admittedRound == 1 && at.admissions.size() == 2, "return did not create another admission");
                ctx.assertTrue(at.snapshot().n() == 2, "scaling unchanged by exit and return");
                a.cleanup();
                ctx.succeed();
            }
        });
    }

    @GameTest(maxTicks = 160)
    public void wipeOfTheWholeRosterFailsImmediatelyAndRemovesOnlyTrialMobs(GameTestHelper ctx) {
        Arena a = new Arena(ctx, 5, LONG);
        ServerPlayer p1 = a.player("w1"), p2 = a.player("w2"), p3 = a.player("w3");
        AtomicInteger step = new AtomicInteger();
        ctx.onEachTick(() -> {
            int t = step.incrementAndGet();
            if (t == 3) { a.at(p1, 2, 2); a.at(p2, -2, 2); }
            if (t == 60) a.at(p3, 6, -6);   // pending, must not be able to rescue the attempt
            if (t == 70) { ctx.assertTrue(!a.mobs().isEmpty(), "trial mobs exist before the wipe"); p1.kill(a.level); p2.kill(a.level); }
            if (t == 90) {
                ctx.assertTrue(a.attempt().outcome() == Outcome.WIPE, "wipe ends the attempt, got " + a.attempt().outcome());
                ctx.assertTrue(a.mobs().isEmpty(), "this trial's mobs removed");
                ctx.assertTrue(a.ctl.cooldownLeft() > 77_000, "78,000-tick cooldown started");
                a.cleanup();
                ctx.succeed();
            }
        });
    }

    @GameTest(maxTicks = 6000)
    public void absenceGraceResumesOnReturnAndExpiresOtherwise(GameTestHelper ctx) {
        Arena a = new Arena(ctx, 6, new RoundPlan.Timing(RoundPlan.LIMIT_MILLIS, 1_500));
        ServerPlayer p = a.player("grace");
        AtomicInteger step = new AtomicInteger();
        UUID[] id = new UUID[1];
        ctx.onEachTick(() -> {
            int t = step.incrementAndGet();
            if (t == 3) a.at(p, 4, 4);
            if (t == 40) { id[0] = a.attempt().attemptId; a.at(p, 50, 0); }
            if (t == 50) ctx.assertTrue(a.attempt().graceExpiryMillis() > 0 && !a.attempt().ended(), "grace running");
            if (t == 60) a.at(p, 4, 4);             // returns within the grace
            if (t == 75) {
                ctx.assertTrue(a.attempt().graceExpiryMillis() == 0 && a.attempt().attemptId.equals(id[0]) && !a.attempt().ended(), "return resumes the same attempt");
                a.at(p, 50, 0);
            }
            if (t == 76) { ctx.assertTrue(a.attempt().graceExpiryMillis() > 0 || !a.attempt().ended(), "second absence"); }
            if (t > 80 && a.attempt().ended()) {
                ctx.assertTrue(a.attempt().outcome() == Outcome.ABSENCE_EXPIRED, "grace expired, got " + a.attempt().outcome());
                ctx.assertTrue(a.mobs().isEmpty(), "trial mobs removed on failure");
                a.cleanup();
                ctx.succeed();
            }
        });
    }

    @GameTest(maxTicks = 6000)
    public void wallClockLimitTimesOutAndLeavesOrdinaryMobs(GameTestHelper ctx) {
        Arena a = new Arena(ctx, 7, new RoundPlan.Timing(5_000, RoundPlan.GRACE_MILLIS));
        ServerPlayer p = a.player("limit");
        AtomicInteger step = new AtomicInteger();
        ctx.onEachTick(() -> {
            int t = step.incrementAndGet();
            if (t == 3) a.at(p, 4, 4);
            if (t == 40) ctx.assertTrue(!a.mobs().isEmpty(), "mobs exist");
            if (t > 40 && a.attempt().ended()) {
                ctx.assertTrue(a.attempt().outcome() == Outcome.TIMED_OUT, "time-out after the limit, got " + a.attempt().outcome());
                ctx.assertTrue(!a.mobs().isEmpty(), "ordinary trial mobs are left in place on time-out (Q116)");
                ctx.assertTrue(a.ctl.cooldownLeft() > 77_000, "cooldown started");
                a.cleanup();
                ctx.succeed();
            }
        });
    }

    /** Process-restart persistence in three server processes: -Dmoreminecrafts.st05.restart.phase=write|reload|verify (inert otherwise). */
    private static final UUID RESTART_PLAYER = UUID.fromString("5a05a05a-0000-4000-8000-0000000000a1");

    @GameTest(maxTicks = 1200)
    public void attemptStateSurvivesRealServerRestart(GameTestHelper ctx) throws IOException {
        String phase = System.getProperty("moreminecrafts.st05.restart.phase");
        if (phase == null || phase.isBlank()) { ctx.succeed(); return; }
        Path evidence = Path.of(System.getProperty("moreminecrafts.st05.restart.file", "build/st05-restart-state.txt"));
        boolean fresh = phase.equals("write");
        Arena a = new Arena(ctx, 9, LONG);   // idempotent registration: reloaded phases find the persisted controller
        AtomicInteger step = new AtomicInteger();
        switch (phase) {
            case "write" -> {
                ServerPlayer p = a.player(RESTART_PLAYER, "restart");
                ctx.onEachTick(() -> {
                    int t = step.incrementAndGet();
                    if (t == 3) a.at(p, 5, 5);
                    if (t == 120) {
                        ArenaAttempt at = a.attempt();
                        ctx.assertTrue(at.count(AllocStatus.SPAWNED) >= 3, "mobs spawned before shutdown");
                        ctx.assertTrue(a.mobs().size() == at.count(AllocStatus.SPAWNED), "world and plan agree");
                        List<String> lines = new ArrayList<>();
                        lines.add("attempt=" + at.attemptId);
                        lines.add("round=" + at.round());
                        lines.add("spawned=" + at.count(AllocStatus.SPAWNED));
                        lines.add("allocs=" + at.allocations().size());
                        lines.add("n=" + at.snapshot().n());
                        try { Files.createDirectories(evidence.toAbsolutePath().getParent()); Files.write(evidence, lines); } catch (IOException e) { throw new RuntimeException(e); }
                        a.level.getServer().getPlayerList().remove(p);
                        a.level.getServer().saveEverything(true, true, true);
                        System.out.println("ST05_RESTART_WRITE " + String.join(" ", lines));
                        ctx.succeed();
                    }
                });
            }
            case "reload" -> {
                List<String> saved = Files.readAllLines(evidence);
                String attemptId = saved.get(0).substring(8);
                int spawned = Integer.parseInt(saved.get(2).substring(8)), allocs = Integer.parseInt(saved.get(3).substring(7));
                ctx.assertTrue(!a.ctl.quarantined() && a.attempt() != null, "restored, not quarantined: " + a.ctl.quarantineReason());
                ctx.assertTrue(a.attempt().attemptId.toString().equals(attemptId), "same attempt id after restart");
                ctx.assertTrue(a.attempt().rosterIds().contains(RESTART_PLAYER) && a.attempt().allocations().size() == allocs, "roster and spawn plan restored");
                AtomicInteger roundStarts = new AtomicInteger();
                ServerPlayer[] pl = new ServerPlayer[1];
                ctx.onEachTick(() -> {
                    int t = step.incrementAndGet();
                    ctx.assertTrue(a.attempt().round() == 1 || t > 60, "restart did not restart or advance the round before the participant returned");
                    if (t == 40) {
                        ctx.assertTrue(a.mobs().size() <= allocs, "no duplicate spawn after restart");
                        ctx.assertTrue(a.attempt().count(AllocStatus.SPAWNED) == spawned && a.mobs().size() == spawned, "spawned allocations still bound to the surviving entities: " + a.attempt().count(AllocStatus.SPAWNED) + "/" + a.mobs().size());
                        ctx.assertTrue(a.attempt().graceExpiryMillis() > 0, "nobody present: the absence grace runs after recovery");
                        pl[0] = a.player(RESTART_PLAYER, "restart");
                        a.at(pl[0], 5, 5);                      // the admitted participant returns
                    }
                    if (t == 60) {
                        ctx.assertTrue(a.attempt().admissions.size() == 1 && a.attempt().pendingIds().isEmpty(), "return after restart is not a new admission");
                        ctx.assertTrue(a.attempt().graceExpiryMillis() == 0, "return resumed the encounter");
                    }
                    if (t > 60 && t < 500 && a.attempt().round() == 1 && a.attempt().phase() == Phase.ROUND) a.killMobs();
                    if (t == 520) {
                        ArenaAttempt at = a.attempt();
                        ctx.assertTrue(at.keysAwarded().equals(java.util.Set.of(1)) && a.keyItems() == 1, "one wave-1 key after recovery, not duplicated: " + a.keyItems());
                        ctx.assertTrue(at.attemptId.toString().equals(attemptId), "still the same attempt");
                        String line = "keys=" + at.keysAwarded() + " round=" + at.round() + " phase=" + at.phase() + " entitlements=" + at.entitlements().size();
                        try { Files.writeString(evidence, String.join("\n", saved) + "\nreload " + line + "\n"); } catch (IOException e) { throw new RuntimeException(e); }
                        a.level.getServer().getPlayerList().remove(pl[0]);
                        a.level.getServer().saveEverything(true, true, true);
                        System.out.println("ST05_RESTART_RELOAD " + line);
                        ctx.succeed();
                    }
                });
            }
            case "verify" -> {
                String attemptId = Files.readAllLines(evidence).get(0).substring(8);
                ArenaAttempt at = a.attempt();
                ctx.assertTrue(at != null && !a.ctl.quarantined(), "restored again");
                ctx.assertTrue(at.attemptId.toString().equals(attemptId), "second restart: same attempt");
                ctx.assertTrue(at.keysAwarded().equals(java.util.Set.of(1)), "wave 1 key record survives and is not re-awarded: " + at.keysAwarded());
                ctx.assertTrue(at.entitlements().stream().anyMatch(e -> e.endsWith(":1:" + RESTART_PLAYER)), "personal eligibility survived");
                ctx.assertTrue(a.ctl.entitlements().containsAll(at.entitlements()), "arena-level eligibility records kept");
                ctx.onEachTick(() -> {
                    int t = step.incrementAndGet();
                    if (t == 60) {
                        ctx.assertTrue(at.keysAwarded().equals(java.util.Set.of(1)), "no second key for wave 1 after another restart");
                        ctx.assertTrue(a.keyItems() <= 1, "key item not duplicated in the world: " + a.keyItems());
                        System.out.println("ST05_RESTART_VERIFY keys=" + at.keysAwarded() + " round=" + at.round() + " phase=" + at.phase());
                        a.cleanup();
                        ctx.succeed();
                    }
                });
            }
            default -> ctx.fail("unknown phase " + phase);
        }
    }
}
